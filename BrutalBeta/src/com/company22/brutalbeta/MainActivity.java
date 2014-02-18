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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener
{
	// Make variables.
	private float beta;
	private float lambda;
	private float temperature;
	protected static ArrayList<Float> temperatureList;
	protected static ArrayList<Float> humidityList;
	protected static ArrayList<Long> timeStampList;
	protected static ArrayList<Float> dewPointList;
	protected static ArrayList<Float> roomTemperatureList;
	private ArrayList<Float> temperatureAverageList;
	private float sensorRoomTemperature;
	private float sensorTemperature = 1337;
	private float sensorHumidity;
	private boolean hasSensor = false;
	private Formatter formatter;
	protected static final String TAG = "brutalbeta";
	private boolean isReadyToTurn = true;
	private float temperatureAverage;
	private int timeToTurnOn;
	private float temperatureOffset;
	private float humidityOffset;
	private byte dataIncrement;
	private byte buttonSpamIncrement = 10;
	protected static String KEY = "42";
	
	// Android variables.
	private SeekBar roomTempSlider;
	private TextView roomTempNumber;
	private TextView roomTempText;
	private TextView messageText;
	private ImageView imageView;
	private Button sendDataButton;
	private ToggleButton sendDataToggleButton;
	private Button emptyDataButton;
	
	// Notification variables.
	private AudioManager audioManager;
	private Vibrator vibrator;
	private NotificationManager notificationManager;
	private Uri soundUri;
	private NotificationCompat.Builder mBuilder;
	
	// Bluetooth.
	private Bluetooth bluetooth;
	protected static String bluetoothData = "1337.0,1337.0";
	private String[] bluetoothDataSplit;
	
	// Sensor variables.
	private SensorManager mSensorManager;
	private Sensor mTemperature;
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize variables.
		beta = 17.502f;
		lambda = 240.97f;
		temperatureOffset = -0.4f;
		humidityOffset = 2.3f;
		roomTempSlider = (SeekBar) findViewById(R.id.roomTempSlider);
		roomTempNumber = (TextView) findViewById(R.id.roomTempNumber);
		roomTempText = (TextView) findViewById(R.id.roomTempText);
		messageText = (TextView) findViewById(R.id.messageText);
		imageView = (ImageView) findViewById(R.id.statusImageView);
		sendDataButton = (Button) findViewById(R.id.sendDataButton);
		sendDataToggleButton = (ToggleButton) findViewById(R.id.sendDataToggleButton);
		emptyDataButton = (Button) findViewById(R.id.emptyDataButton);
		temperatureList = new ArrayList<Float>();
		temperatureAverageList = new ArrayList<Float>();
		humidityList = new ArrayList<Float>();
		timeStampList = new ArrayList<Long>();
		dewPointList = new ArrayList<Float>();
		roomTemperatureList = new ArrayList<Float>();
		dataIncrement = 0;
		
		// Notification variables.
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		// Prepare the build of the notification.
		mBuilder = new NotificationCompat.Builder(getApplicationContext())
			.setSmallIcon(R.drawable.water_icon_144)
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
			// Remove unnecessary Views.
			roomTempText.setVisibility(View.GONE);
			roomTempSlider.setVisibility(View.GONE);
		}
		else hasSensor = false;
		
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
		
		sendDataButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Send data to website when button is clicked and button timer is down.
				if (buttonSpamIncrement == 0)
				{
					KEY = "42";
					sendData();
					buttonSpamIncrement = 10;
				}
				else
				{
					System.out.println(Byte.toString(buttonSpamIncrement));
					Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.buttonNotReady) +
							Byte.valueOf(buttonSpamIncrement)
							+ getApplicationContext().getString(R.string.seconds), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		emptyDataButton.setOnClickListener(new OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				// Change key to empty every time data is sent.
				KEY = "24";
				sendData();
			}
		});
		
		// Start timer and repeat task every second.
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
		// Check if phone has sensor.
		if (hasSensor)
		{
			// Register sensors.
			mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
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
			// --- To demonstrate the programs function.
//			Random random = new Random();
//			bluetoothData = Float.toString(5 * random.nextFloat() + 30) + "," + Float.toString(5 * random.nextFloat() + 10);
//			random = null;
			// ---
			
			// Get input data.
			bluetoothDataSplit = bluetoothData.split(",");
			
			// Temperature.
			// Check if phone don't have sensor and get the slider value instead.
			if (!hasSensor) temperature = (Float.parseFloat(String.valueOf(roomTempSlider.getProgress())) / 10) + 10;
			else temperature = sensorRoomTemperature;
			
			// Temperature from the Arduino sensor.
			sensorTemperature = Float.parseFloat(bluetoothDataSplit[1]) + getTemperatureOffset();
			
			temperatureAverage = 0;
			float dewPoint = 0;
			
			// Check if sensor data has been received.
			if (sensorTemperature + getTemperatureOffset() != 1337)
			{
				// Adds sensor temperature to a list.
				temperatureList.add(sensorTemperature);
				
				// Have a maximum of 10 elements in the temperature list.
				if (temperatureList.size() > 10) temperatureList.remove(0);
				
				if (temperatureList.size() != 1)
				{
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
				sensorHumidity = Float.parseFloat(bluetoothDataSplit[0]) + getHumidityOffset();
				
				// Adds sensor humidity to a list.
				humidityList.add(sensorHumidity);
				
				// Have a maximum of 10 elements in the humidity list.
				if (humidityList.size() > 10) humidityList.remove(0);
				
				// Timestamp.
				timeStampList.add(System.currentTimeMillis());
				
				// Have a maximum of 10 elements in the timestamp list.
				if (timeStampList.size() > 10) timeStampList.remove(0);

				// Dew point calculation.
				float parentheses = (float) (Math.log(sensorHumidity / 100) + (getBeta() * temperature) / (getLambda() + temperature));
				dewPoint = (getLambda() * parentheses) / (getBeta() - parentheses);
				
				// Adds dew point to a list.
				dewPointList.add(dewPoint);
				
				// Have a maximum of 10 elements in the dew point list.
				if (dewPointList.size() > 10) dewPointList.remove(0);
				
				// Adds room temperature to a list.
				roomTemperatureList.add(temperature);
				
				// Have a maximum of 10 elements in the room temperature list.
				if (roomTemperatureList.size() > 10) roomTemperatureList.remove(0);

				// Calculate the estimated number of seconds for the components the be ready to turn on.
				timeToTurnOn = (int) Math.ceil((dewPoint + 1 - temperatureList.get(temperatureList.size() - 1)) / temperatureAverage);
				// Check if timeToTurnOn is a magic number.
				if (timeToTurnOn == Integer.MAX_VALUE) timeToTurnOn = 0;
				
				// Check if toggle data transfer is true
				// and only send data every 10 time.
				if (sendDataToggleButton.isChecked()
						&& dataIncrement >= 9)
				{
					dataIncrement = 0;
					sendData();
				}
				else dataIncrement++;
				
				// Increment every time data is received.
				if (buttonSpamIncrement > 0) buttonSpamIncrement--;
			}

			// Output.
			if (hasSensor)
			{
				formatter = new Formatter();
				roomTempNumber.setText("Room temperature: " + formatter.format("%.1f", sensorRoomTemperature).out().toString());
				formatter.close();
			}
			
			// Check if it is safe to turn on electronics.
			if (sensorTemperature - 1 <= dewPoint)
			{
				// Check if enough data has been collected.
				if (temperatureList.size() > 1)
				{
					messageText.setTextColor(Color.argb(255, 255, 0, 0));
					messageText.setText("Estimated time to be safe: " + timeToTurnOn);
				}
				
				// Change ready image.
				imageView.setImageResource(R.drawable.off_1024);
				isReadyToTurn = false;
			}
			else
			{
				messageText.setTextColor(Color.argb(255, 0, 255, 0));
				messageText.setText("It's now safe.");
				// Change ready image.
				imageView.setImageResource(R.drawable.on_1024);
				// Check if it wasn't ready last update.
				if (!isReadyToTurn)
				{
					// Go though what ringer mode the phone is on.
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
			Log.d(TAG, "temp: " + sensorTemperature + " humi: " + sensorHumidity);
		}
	};
	
	/**
	 * Method to send data to external website.
	 */
	private void sendData()
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.isConnected())
			new DoHttpPost().execute("1337");
		else 
			Toast.makeText(getApplicationContext(), "Couldn't connect to address!", Toast.LENGTH_LONG).show();
	}

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

	/**
	 * @return Temperature offset.
	 */
	public float getTemperatureOffset()
	{
		return temperatureOffset;
	}

	/**
	 * Sets the temperature offset value.
	 * @param temperatureOffset New offset value.
	 */
	public void setTemperatureOffset(float temperatureOffset)
	{
		this.temperatureOffset = temperatureOffset;
	}

	/**
	 * @return Humidity offset.
	 */
	public float getHumidityOffset()
	{
		return humidityOffset;
	}

	/**
	 * Sets the humidity offset value.
	 * @param humidityOffset New offset value.
	 */
	public void setHumidityOffset(float humidityOffset)
	{
		this.humidityOffset = humidityOffset;
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
	}
}
