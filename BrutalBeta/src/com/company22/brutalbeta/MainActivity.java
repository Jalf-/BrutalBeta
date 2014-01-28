package com.company22.brutalbeta;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.Stack;
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
import android.view.WindowManager;
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
	// Make variables.
	private float beta;
	private float lambda;
	private float temperature;
	private float sensorTemperature;
	private float sensorHumidity;
	private SeekBar roomTempSlider;
	private TextView roomTempNumber;
	private TextView roomTempText;
	private TextView messageText;
	private TextView dewPointText;
	private boolean hasSensor = false;
	private Formatter formatter;
	
	// Sensor variables.
	private SensorManager mSensorManager;
	private Sensor mTemperature;
	private Sensor mHumidity;
	
	// Graph variables.
	private XYPlot plot;
	private static Stack<Float> dewPointStack;
	private static Number[] dewPointNumbers;
	private static XYSeries dewPointSeries;
	private LineAndPointFormatter dewpointFormat;
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		
		// Initialize variables.
		beta = 17.502f;
		lambda = 240.97f;
		dewPointText = (TextView) findViewById(R.id.dewPointTextView);
		roomTempSlider = (SeekBar) findViewById(R.id.roomTempSlider);
		roomTempNumber = (TextView) findViewById(R.id.roomTempNumber);
		roomTempText = (TextView) findViewById(R.id.roomTempText);
		messageText = (TextView) findViewById(R.id.messageText);
		
		// Sensor variables.
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
				&& mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null)
		{
			hasSensor = true;
			mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
			mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
			roomTempText.setVisibility(View.GONE);
			roomTempSlider.setVisibility(View.GONE);
		}
		else 
		{
			hasSensor = false;
			System.out.println("is null");
		}
		
		// Graph variables.
		plot = (XYPlot) findViewById(R.id.xyPloy);
		plot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
		plot.setTicksPerRangeLabel(3);
		plot.setTicksPerDomainLabel(5);
		plot.getGraphWidget().setDomainLabelOrientation(-45);
		plot.setRangeBoundaries(-20, 50, BoundaryMode.FIXED);
		dewPointStack = new Stack<Float>();
		dewPointNumbers = new Number[10];
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
	
	protected void onResume()
	{
		super.onResume();
		mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
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
			try
			{			
				temperature = (Float.parseFloat(String.valueOf(roomTempSlider.getProgress())) / 10) + 10;
			}
			catch (Exception e)
			{
				temperature = 20;
			}
			
//			Random random = new Random();
//			float humidity = (random.nextInt(19) + random.nextFloat()) + 20;
			float humidity = sensorHumidity;
			
			// Dew point calculation.
			float parentheses = (float) (Math.log(humidity / 100) + (getBeta() * temperature) / (getLambda() + temperature));
			float dewPoint = (getLambda() * parentheses) / (getBeta() - parentheses);
			
			// Format output.
			formatter = new Formatter();
			String dewPointString = formatter.format("%.1f", dewPoint).out().toString();
			formatter.close();
			
			// Insert to stack
			dewPointStack.insertElementAt(dewPoint, 0);
			if (dewPointStack.size() > 10) dewPointStack.remove(10);  
			
			// Reverse stack and get it in the array and reverse the stack again.
			Collections.reverse(dewPointStack);
			for (int i = 0; i < dewPointStack.size(); i++)
			{
				dewPointNumbers[i] = dewPointStack.get(i);
			}
			Collections.reverse(dewPointStack);
		
			// Output.
			if (hasSensor)
			{
				formatter = new Formatter();
				roomTempNumber.setText(formatter.format("%.1f", sensorTemperature).out().toString());
				formatter.close();
			}
			else
			{
				
			}
			
			dewPointText.setText(dewPointString);

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
			dewPointSeries = new SimpleXYSeries(Arrays.asList(dewPointNumbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Dew Point");
			plot.clear();
			plot.addSeries(dewPointSeries, dewpointFormat);
			plot.redraw();
			
			// Debug data.
			System.out.println("Temperature: " + temperature + " Humidity: " + humidity + " Dew point: " + dewPoint);
			System.out.println("Sensor Temperature: " + sensorTemperature);
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
}
