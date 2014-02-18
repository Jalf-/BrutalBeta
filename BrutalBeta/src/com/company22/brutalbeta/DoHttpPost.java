package com.company22.brutalbeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Class that handles connection to website to send data.
 * Adepted from source: http://mobiledevtuts.com/android/android-http-with-asynctask-example/
 */
public class DoHttpPost extends AsyncTask<String, Integer, Double>
{
	@Override
	protected Double doInBackground(String... params)
	{
		postData(params[0]);
		return null;
	}
	
	/**
	 * Method to post data to the website.
	 * @param data Not used.
	 */
	protected void postData(String data)
	{
		// Url to connect to.
		String baseUrl = "http://salty.dk/benjamin/index.php";
		
		// Create variables.
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(baseUrl);
		
		try
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			
			// Adding data to post.
			for (int i = 0; i < MainActivity.temperatureList.size(); i++)
			{
				nameValuePairs.add(new BasicNameValuePair("TP[]", Float.toString(MainActivity.temperatureList.get(i))));
				nameValuePairs.add(new BasicNameValuePair("RH[]", Float.toString(MainActivity.humidityList.get(i))));
				nameValuePairs.add(new BasicNameValuePair("TS[]", Long.toString(MainActivity.timeStampList.get(i))));
				nameValuePairs.add(new BasicNameValuePair("DP[]", Float.toString(MainActivity.dewPointList.get(i))));
				nameValuePairs.add(new BasicNameValuePair("RT[]", Float.toString(MainActivity.roomTemperatureList.get(i))));
			}
			// Add data key.
			nameValuePairs.add(new BasicNameValuePair("KEY", MainActivity.KEY));
			
			// Prepare.
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			// Send the request.
			httpClient.execute(httpPost);
			Log.d(MainActivity.TAG, "Data sent!");
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
