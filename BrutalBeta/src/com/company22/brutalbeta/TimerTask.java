package com.company22.brutalbeta;

import java.util.Random;


public class TimerTask extends java.util.TimerTask
{
	public double beta;
	public double lambda;
	
	public TimerTask(double beta, double lambda)
	{
		this.beta = beta;
		this.lambda = lambda;
	}
	
	@Override
	public void run()
	{
		// Get input data
		Random random = new Random();
		double temperature = random.nextInt(20) + 10 + random.nextDouble();
		double humidity = random.nextInt(99) + random.nextDouble();
		
		// Dew point calculation
		double parentheses = (Math.log(humidity / 100) + (beta * temperature) / (lambda + temperature));
		double dewPoint = (lambda * parentheses) / (beta - parentheses);
		
		// Output
		System.out.println("Temperature: " + temperature + " Humidity: " + humidity + " Dew point: " + dewPoint);
	
	}

	//Return a value (temp, humid... etc...)
	public static double getValue(){
		return 10; //placeholder
	}


}
