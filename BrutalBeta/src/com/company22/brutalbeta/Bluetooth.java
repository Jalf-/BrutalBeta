package com.company22.brutalbeta;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Bluetooth class that handles the Bluetooth connection.
 * Bluetooth source code 
 * http://forum.arduino.cc/index.php?topic=157621.0
 */
public class Bluetooth
{
	private Context context;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	private InputStream inStream = null;
	private boolean stopWorker = false;
	private byte delimiter = 10;
	private int readBufferPosition = 0;
	private byte[] readBuffer = new byte[1024];
	private Handler handler = new Handler();
	private static String address = "XX:XX:XX:XX:XX:XX";
	private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

	public Bluetooth(Context context, ContentResolver contentResolver)
	{
		this.context = context;
	}
	
	public void checkBt()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//		address = mBluetoothAdapter.getAddress();
		// Hardcoded MAC address of the arduino.
		address = "00:12:09:28:09:90";
		if (!mBluetoothAdapter.isEnabled())
		{
			Toast.makeText(context, "Bluetooth Disabled!",
					Toast.LENGTH_LONG).show();
		}
		
		if (mBluetoothAdapter == null)
		{
			Toast.makeText(context, "Bluetooth Null!",
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void connect()
	{	
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		Log.d(MainActivity.TAG, "Connecting to ... " + address);
		mBluetoothAdapter.cancelDiscovery();
		try
		{
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			btSocket.connect();
			Log.d(MainActivity.TAG, "Connection made.");
		}
		catch (IOException e)
		{
			try
			{
				btSocket.close();
			}
			catch (IOException e2)
			{
				Log.d(MainActivity.TAG, "Unable to end the connection.");
			}
			Log.d(MainActivity.TAG, "Socket creation failed.");
		}
		beginListenForData();
	}

	private void beginListenForData()
	{
		try
		{
			inStream = btSocket.getInputStream();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		Thread workerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(!Thread.currentThread().isInterrupted() && !stopWorker )
				{
					try
					{
						int bytesAvaliable = inStream.available();
						if (bytesAvaliable > 0)
						{
							byte[] packetsBytes = new byte[bytesAvaliable];
							inStream.read(packetsBytes);
							for(int i = 0; i < bytesAvaliable; i++)
							{
								byte b = packetsBytes[i];
								if (b == delimiter)
								{
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;
									handler.post(new Runnable()
									{	
										@Override
										public void run()
										{
											MainActivity.bluetoothData = data;
										}
									});
								}
								else
								{
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					}
					catch(IOException ex)
					{
						stopWorker = true;
					} 
					catch(NullPointerException ex)
					{
						stopWorker = true;
					} 
				}
			}
		});	
		workerThread.start();		
	}
	
	/**
	 * @return Bluetooth socket.
	 */
	public BluetoothSocket getBtSocket()
	{
		return btSocket;
	}
}
