package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author Nitin
 *
 */
public class GroupMessengerActivity extends Activity {
	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	private int conn [] = {11108,11112,11116,11120,11124};
	static final int SERVER_PORT = 10000;
	static int lseqNum=0;
	static int gseqNum=0;
	Message m; 
	ServerSocket serverSocket;
	private Uri mUri;

	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(serverSocket!=null){
			if(serverSocket.isBound()){
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		/*TextView to display messages.*/
		try {
			serverSocket=new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		/*
		 * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
		 * OnPTestClickListener demonstrates how to access a ContentProvider.
		 */
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		/*Method to get the message from the input box (EditText) and send it to other AVDs 
		 * in a total-causal order.*/
		findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final EditText editText = (EditText) findViewById(R.id.editText1);
				String msg = editText.getText().toString();
				editText.setText(""); // This is one way to reset the input box.
				Message m=new Message(msg,0,myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m);
			}
		});		
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			/*server code that receives messages and passes them to onProgressUpdate()*/
			try{
				while(true){
					serverSocket = sockets[0];
					Socket sock=serverSocket.accept();
					ObjectInputStream obj1 = new ObjectInputStream(sock.getInputStream());
					m = (Message)obj1.readObject();
					if(m.seqNum==0){
						gseqNum+=1;
						m.seqNum=gseqNum;
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, m);
					}
					else {	
						publishProgress(m.msg);
						ContentValues values = new ContentValues();
						values.put("key", m.seqNum-1);
						values.put("value", m.msg.trim());
						getContentResolver().insert(mUri, values);						
					}
				}
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(String...strings) {
			/*The following code displays what is received in doInBackground().*/
			String strReceived = strings[0].trim();
			TextView textView = (TextView) findViewById(R.id.textView1);
			textView.append(strReceived+"\n");
			return;
		}
	}
	/***
	 * ClientTask is an AsyncTask that should send a string over the network.
	 * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
	 * an enter key press event.
	 */
	private class ClientTask extends AsyncTask<Message, Void, Void> {

		@Override
		protected Void doInBackground(Message... msgs) {
			try {
				Message msgToBeSend = msgs[0];

				if(msgToBeSend.seqNum==0){
					Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),11108);
					ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream()); // send the message to server
					os.writeObject(msgToBeSend);
					os.close();
					sock.close();
				}
				else{
					for(int i=0;i<conn.length;i++){
						Socket sock1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),conn[i]);
						ObjectOutputStream os1= new ObjectOutputStream(sock1.getOutputStream()); // multicast the message
						os1.writeObject(msgToBeSend);
						os1.close();
						sock1.close();
					}
				}
			} catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "ClientTask socket IOException");
			}
			return null;
		}
	}

	private static class Message implements Serializable{
		private String msg=null;
		int seqNum;
		Message(String msg,int seqNum,String myPort){
			this.msg=msg;
			this.seqNum=seqNum;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
}
