package com.company22.brutalbeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener
{	
	// TODO Remove graph - done
	// TODO Time to turn on
	// TODO UI
	// TODO Status indicator - part
	// TODO Sound or vibration - done
	// TODO SMS - nah
	
	// Make variables.
	private float beta;
	private float lambda;
	private float temperature;
	private ArrayList<Float> temperatureList;
	private ArrayList<Float> temperatureAverageList;
	private float sensorRoomTemperature;
	private float sensorTemperature = 1337;
//	private float humidity;
	private float sensorHumidity;
	private boolean hasSensor = false;
	private Formatter formatter;
	protected static final String TAG = "brutalbeta";
	private boolean isReadyToTurn = false;
	
	// Android variables.
	private SeekBar roomTempSlider;
	private TextView roomTempNumber;
	private TextView roomTempText;
	private TextView messageText;
	private TextView dewPointText;
	private ImageView imageView;
	
	// Notification variables.
	private AudioManager audioManager;
	private Vibrator vibrator;
	private NotificationManager notificationManager;
	private Uri soundUri;
	private NotificationCompat.Builder mBuilder;
	
	// Bluetooth.
	private Bluetooth bluetooth;
	protected static String bluetoothData = "1337,1337";
	private String[] bluetoothDataSplit;
	
	// Sensor variables.
	private SensorManager mSensorManager;
	private Sensor mTemperature;
	private Sensor mHumidity;
	
	// Graph variables.
	private static ArrayList<Float> dewPointList;
	private static ArrayList<Float> dewPointLongList;	// Remove sis------------------------------------------------
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize variables.
		beta = 17.502f;
		lambda = 240.97f;
		dewPointText = (TextView) findViewById(R.id.dewPointTextView);
		roomTempSlider = (SeekBar) findViewById(R.id.roomTempSlider);
		roomTempNumber = (TextView) findViewById(R.id.roomTempNumber);
		roomTempText = (TextView) findViewById(R.id.roomTempText);
		messageText = (TextView) findViewById(R.id.messageText);
		imageView = (ImageView) findViewById(R.id.statusImageView);
		temperatureList = new ArrayList<Float>();
		temperatureAverageList = new ArrayList<Float>();
		
		// Notification variables.
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		// Prepare the build of the notification.
		mBuilder = new NotificationCompat.Builder(getApplicationContext())
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.notificationText));
		
		// Bluetooth
		bluetooth = new Bluetooth(getApplicationContext(), getContentResolver());
		bluetoothDataSplit = new String[2];
		bluetooth.checkBt();
		bluetooth.connect();
		
		// Sensor variables.
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// Check if sensors does exist.
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
				&& mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null)
		{
			hasSensor = true;
			mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
			mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
			// Remove unneeded Views.
			roomTempText.setVisibility(View.GONE);
			roomTempSlider.setVisibility(View.GONE);
		}
		else 
		{
			hasSensor = false;
		}
		
		// Graph variables.
		dewPointList = new ArrayList<Float>();
		dewPointLongList = new ArrayList<Float>();
		
		// Start listeners.
		roomTempSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				// Change the temperature text when slider is moved.
				roomTempNumber.setText(String.valueOf((Float.parseFloat(String.valueOf(progress)) / 10) + 10));
			}
		});

		// Start timer.
		Log.d(TAG, "Starting timer loop.");
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{	
			@Override
			public void run()
			{
				TimerMethod();
			}
		}, 1000, 1000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		// Close Bluetooth connection when app is closed.
		try
		{
			bluetooth.getBtSocket().close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called when application is resumed.
	 */
	protected void onResume()
	{
		super.onResume();
		if (hasSensor)
		{
			// Register sensors.
			mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
			mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	/**
	 * Called when application is paused.
	 */
	protected void onPause()
	{
		super.onPause();
		// Unregister listeners.
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Method to run the timer in the main class.
	 */
	private void TimerMethod()
	{
		this.runOnUiThread(TimerTick);
	}
	
	/**
	 * Timer that will update and calculate the result.
	 */
	private Runnable TimerTick = new Runnable()
	{
		@Override
		public void run()
		{
			// Get input data.
			bluetoothDataSplit = bluetoothData.split(",");			

			// Temperature.
			if (!hasSensor)
			{
				// Slider value.
				temperature = (Float.parseFloat(String.valueOf(roomTempSlider.getProgress())) / 10) + 10;
			}
			else
			{
				temperature = sensorRoomTemperature;
			}
			// Temperature from the Arduino sensor.
			sensorTemperature = Float.parseFloat(bluetoothDataSplit[1]);
			
			float temperatureAverage = 0;
			
			// Check if sensor data has been received.
			if (sensorTemperature != 1337)
			{
				// Adds sensor temperature to a list.
				temperatureList.add(sensorTemperature);
				
				// Builds average list.
				for (int i = 0; i < temperatureList.size() - 1; i++)
				{
					// Adds the difference between each entry. 
					temperatureAverageList.add(temperatureList.get(i + 1) - temperatureList.get(i));
				}
				// Adds the average list to average temperature.
				for (Float temperatureAverageValue : temperatureAverageList) temperatureAverage += temperatureAverageValue;
				// Divide the average value by the list size.
				temperatureAverage /= temperatureAverageList.size();
				// Clear the average list to avoid clutter.
				temperatureAverageList.clear();
			}

			// Set humidity value.
			sensorHumidity = Float.parseFloat(bluetoothDataSplit[0]);
			
			// Dew point calculation.
			float parentheses = (float) (Math.log(sensorHumidity / 100) + (getBeta() * temperature) / (getLambda() + temperature));
			float dewPoint = (getLambda() * parentheses) / (getBeta() - parentheses);
			
			// Format output.
			formatter = new Formatter();
			String dewPointString = formatter.format("%.1f", dewPoint).out().toString();
			formatter.close();
			
			// Insert to list.
			dewPointList.add(dewPoint);
			dewPointLongList.add(dewPoint);
			
			// Remove last value on the list if it is larger than 10.
			if (dewPointList.size() > 10) dewPointList.remove(0);
			
			// Remove last value on the list if it is larger than 3600.
			if (dewPointLongList.size() > 3600) dewPointList.remove(0);
		
			// Output.
			if (hasSensor)
			{
				formatter = new Formatter();
				roomTempNumber.setText(formatter.format("%.1f", sensorRoomTemperature).out().toString());
				formatter.close();
			}
			
			dewPointText.setText("Dew point: " + dewPointString);

			// Check if it is safe to turn on electronics.
			if (sensorTemperature - 0.5 <= dewPoint)
			{
				messageText.setTextColor(Color.argb(255, 255, 0, 0));
				messageText.setText("Don't turn on your electronics!");
				// Change ready image.
				imageView.setImageResource(R.drawable.off);
				isReadyToTurn = false;
			}
			else
			{
				messageText.setTextColor(Color.argb(255, 0, 255, 0));
				messageText.setText("It's now safe to turn on your electronics.");
				// Change ready image.
				imageView.setImageResource(R.drawable.on);
				// Check if it wasn't ready last update.
				if (!isReadyToTurn)
				{
					// Go thought what ringer mode the phone is on.
					switch (audioManager.getRingerMode())
					{
					// Ringer mode is normal and the phone will receive a notification with sound and vibration.
					case AudioManager.RINGER_MODE_NORMAL:
						notificationManager.notify(0, mBuilder.setSound(soundUri).build());
						vibrator.vibrate(1000);
						break;
					// Ringer mode is vibration and the phone will receive a notification and vibration.
					case AudioManager.RINGER_MODE_VIBRATE:
						notificationManager.notify(0, mBuilder.build());
						vibrator.vibrate(1000);
						break;
					// Ringer mode is silent and the phone will receive a notification.
					case AudioManager.RINGER_MODE_SILENT:
						notificationManager.notify(0, mBuilder.build());
						break;
					}
				}
				isReadyToTurn = true;
			}

			// Debug data.
			System.out.println(sensorTemperature + " " + temperatureAverage);
//			System.out.println("Temperature: " + temperature + " Humidity: " + humidity + " Dew point: " + dewPoint);
//			System.out.println("Sensor Room Temperature: " + sensorRoomTemperature);
//			System.out.println("Sensor Temperature: " + sensorTemperature + " Sensor Humidity: " + sensorHumidity);
		}
	};

	/**
	 * @return Beta value.
	 */
	public float getBeta()
	{
		return beta;
	}

	/**
	 * Sets the beta value.
	 * @param beta New beta value.
	 */
	public void setBeta(float beta)
	{
		this.beta = beta;
	}

	/**
	 * @return Lambda value.
	 */
	public float getLambda()
	{
		return lambda;
	}

	/**
	 * Sets the lambda value.
	 * @param lambda New lambda value.
	 */
	public void setLambda(float lambda)
	{
		this.lambda = lambda;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		// Set sensor values.
		if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) sensorRoomTemperature = event.values[0];
//		else if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)) sensorHumidity = event.values[0];
	}
}
