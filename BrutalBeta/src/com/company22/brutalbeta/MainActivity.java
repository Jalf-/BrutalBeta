package com.company22.brutalbeta;

import java.util.Timer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		double beta = 17.502;
		double lambda = 240.97;
		
		//Get current value and display as string
		TextView textView = (TextView) findViewById(R.id.textView1);
		textView.setText(String.valueOf(19)); //Output Value/variable
		
		Timer t = new Timer();
		t.schedule(new com.company22.brutalbeta.TimerTask(beta, lambda), 10000, 10000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
