package com.company22.brutalbeta;

import java.util.Formatter;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity
{
	// Make variables.
	private float beta;
	private float lambda;
	private float temperature;
	private float sensorTemperature;
	private SeekBar roomTempSlider;
	private TextView roomTempNumber;
	private TextView messageText;
	private TextView dewPointText;
	
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
		messageText = (TextView) findViewById(R.id.messageText);
		
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
			
			Random random = new Random();
			float humidity = (random.nextInt(19) + random.nextFloat()) + 20;
			
			// Dew point calculation.
			float parentheses = (float) (Math.log(humidity / 100) + (getBeta() * temperature) / (getLambda() + temperature));
			float dewPoint = (getLambda() * parentheses) / (getBeta() - parentheses);
			
			// Format output.
			Formatter formatter = new Formatter();
			String dewPointString = formatter.format("%.1f", dewPoint).out().toString();
			formatter.close();
			
			// Output.
			dewPointText.setText(dewPointString);
			
			// Default start temperature.
			sensorTemperature = -273.15f;
			
			// Placeholder for sensor temperature
			sensorTemperature = 0f;

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
			
			// Debug data.
			System.out.println("Temperature: " + temperature + " Humidity: " + humidity + " Dew point: " + dewPoint);
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
}
