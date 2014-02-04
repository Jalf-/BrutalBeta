package com.company22.brutalbeta;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public class MainActivity extends Activity implements SensorEventListener
{	
	// TODO Remove graph
	// TODO Time to turn on
	// TODO UI
	// TODO Status indicator
	// TODO Sound or vibration
	// TODO SMS
	
	// Make variables.
	private float beta;
	private float lambda;
	private float temperature;
	private ArrayList<Float> temperatureList;
	private float sensorTemperature;
	private float humidity;
	private float sensorHumidity;
	private SeekBar roomTempSlider;
	private TextView roomTempNumber;
	private TextView roomTempText;
	private TextView messageText;
	private TextView dewPointText;
	private boolean hasSensor = false;
	private Formatter formatter;
	protected static final String TAG = "brutalbeta";
	
	// Bluetooth.
	private Bluetooth bluetooth;
	protected static String bluetoothData = ""; 
	
	// Sensor variables.
	private SensorManager mSensorManager;
	private Sensor mTemperature;
	private Sensor mHumidity;
	
	// Graph variables.
	private XYPlot plot;
	private static ArrayList<Float> dewPointList;
	private static ArrayList<Float> dewPointLongList;
	private static XYSeries dewPointSeries;
	private LineAndPointFormatter dewpointFormat;
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		
		// Initialize variables.
		beta = 17.502f;
		lambda = 240.97f;
		dewPointText = (TextView) findViewById(R.id.dewPointTextView);
		roomTempSlider = (SeekBar) findViewById(R.id.roomTempSlider);
		roomTempNumber = (TextView) findViewById(R.id.roomTempNumber);
		roomTempText = (TextView) findViewById(R.id.roomTempText);
		messageText = (TextView) findViewById(R.id.messageText);
		temperatureList = new ArrayList<Float>();
		
		// Bluetooth
		bluetooth = new Bluetooth(getApplicationContext(), getContentResolver());
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
		plot = (XYPlot) findViewById(R.id.xyPloy);
		plot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
		plot.setTicksPerRangeLabel(3);
		plot.setTicksPerDomainLabel(5);
		plot.getGraphWidget().setDomainLabelOrientation(-45);
		plot.setRangeBoundaries(-20, 50, BoundaryMode.FIXED);
		dewPointList = new ArrayList<Float>();
		dewPointLongList = new ArrayList<Float>();
		dewpointFormat = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);
		dewpointFormat.getFillPaint().setAlpha(64);
		
		// Start listeners.
		// Slider listener.
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
			// Try to get room temperature from the slider.
//			try
//			{			
//				temperature = (Float.parseFloat(String.valueOf(roomTempSlider.getProgress())) / 10) + 10;
//			}
//			catch (Exception e)
//			{
//				temperature = 20;
//			}
			temperature += new Random().nextDouble() - 0.25;
				
			
			temperatureList.add(temperature);
			
			float temperatureAverage = 0;
			ArrayList<Float> temperatureAverageList = new ArrayList<Float>();
			
			for (int i = 0; i < temperatureList.size() - 1; i++)
			{
				temperatureAverageList.add(temperatureList.get(i + 1) - temperatureList.get(i));
			}
			for (Float temperatureAverageValue : temperatureAverageList) temperatureAverage += temperatureAverageValue;
			temperatureAverage /= temperatureAverageList.size();
			
			
			
			// Set humidity value.
			if (!hasSensor) humidity = 30;
			else humidity = sensorHumidity;
			
			// Dew point calculation.
			float parentheses = (float) (Math.log(humidity / 100) + (getBeta() * temperature) / (getLambda() + temperature));
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
			
			// Calculate when it is possible to turn it on.
//			ArrayList<Float> xValues = new ArrayList<Float>();
//			for (int i = 1; i < dewPointList.size() + 1; i++)
//			{
//				xValues.add((float) i);
//			}
//			float[] regression = getSimpleLinearRegression(xValues, dewPointList);
//			xValues = null;
//			System.out.println("A: " + regression[1] + " B: " + regression);
		
			// Output.
			if (hasSensor)
			{
				formatter = new Formatter();
				roomTempNumber.setText(formatter.format("%.1f", sensorTemperature).out().toString());
				formatter.close();
			}
			
			dewPointText.setText("Dew point: " + dewPointString);

			// Check if it is safe to turn on electronics.
			if (sensorTemperature - 0.5 <= dewPoint)
			{
				messageText.setTextColor(Color.argb(255, 255, 0, 0));
				messageText.setText("Don't turn on your electronics!");
			}
			else
			{
				messageText.setTextColor(Color.argb(255, 0, 255, 0));
				messageText.setText("It's now safe to turn on your electronics.");
			}
			
			// Update graph.
			dewPointSeries = new SimpleXYSeries(dewPointList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Dew Point");
			plot.clear();
			plot.addSeries(dewPointSeries, dewpointFormat);
			plot.redraw();
			
			
			
			
			String[] strings = bluetoothData.split(",");
			System.out.println(strings[0]);
			System.out.println(strings[1]);
			roomTempNumber.setText(bluetoothData);
			
			
			
			
			// Debug data.
			
			System.out.println(temperature + " " + temperatureAverage);
//			System.out.println("Temperature: " + temperature + " Humidity: " + humidity + " Dew point: " + dewPoint);
//			System.out.println("Sensor Temperature: " + sensorTemperature);
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
		if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) sensorTemperature = event.values[0];
		else if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)) sensorHumidity = event.values[0];
	}
	/**
	 * Calculate the simple linear regression for two value sets.
	 * @param xValues X values.
	 * @param yValues Y values.
	 * @return Float array containing two values, first values is th slope value and the second is the y-intercept value.
	 */
	public float[] getSimpleLinearRegression(ArrayList<Float> xValues, ArrayList<Float> yValues)
	{
		// Check if both arrays are the same size.
		if (xValues.size() != yValues.size()) return null;
		
		// Initialize variables.
		float xAverage = 0;
		float yAverage = 0;
		float xSubAverageRaisedSum = 0;
		float xySubAverageMultiSum = 0;
		float[] regression = new float[2];
		
		// Calculate average of the x and y values.
		// Sum x y values.
		for (int i = 0; i < xValues.size(); i++)
		{
			xAverage += xValues.get(i);
			yAverage += yValues.get(i);
		}
		// Divide by the size to get average.
		xAverage /= xValues.size();
		yAverage /= yValues.size();

		// Magic.
		for (Float xValue : xValues) xSubAverageRaisedSum += Math.pow(xValue - xAverage, 2);
		for (int i = 0; i < xValues.size(); i++) xySubAverageMultiSum += (xValues.get(i) - xAverage) * (yValues.get(i) - yAverage);
		
		// Calculate slope and y-intercept.
		regression[0] = xSubAverageRaisedSum / xySubAverageMultiSum;
		regression[1] = yAverage - regression[0] * xAverage;
		
		return regression;
	}
}
