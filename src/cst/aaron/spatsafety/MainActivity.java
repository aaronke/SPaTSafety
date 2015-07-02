package cst.aaron.spatsafety;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements LocationListener,SensorEventListener{

	private static BluetoothAdapter mBluetoothAdapter;
	private static final UUID MY_UUID=UUID.fromString("66841278-c3d1-11df-ab31-001de000a903");
//	private static OutputStream mOutputStream;
//	private static InputStream mInputStream;
	private final static int 	REQUEST_CODE_BT=1;
	private static final String MY_NAME="Bluetooth_Test_Aaron";
	private AcceptThread acceptThread;
	private ConnectedThread mConnectedThread;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECTED=1;
	public static final int MESSAGE_DISCONNECTE=3;
	private final static int DISCURABLE_TIME=300;
	private final static String KEEP_CURRENT_SPEED="keep current speed";
	private final static String SPEED_UP="Speed up to";
	private final static String SLOW_DOWN="Slow down to";
	private final static String TURN_RED="Signal will turn red ahead";
	private final static String RED_SIGNAL_AHEAD="Red light ahead";
	private final static int SIGNAL_GREEN=3, SIGNAL_RED=1, SIGNAL_YELLOW=2;
	public static TextView count_TextView;
	public static ImageView signal_ImageView;
	public static TextView connecttion_TextView;
	public static ImageView left_ImageView;
	public static ImageView straight_ImageView;
	public static ImageView right_ImageView;
	public static TextToSpeech textToSpeech;
	public static TextView acc_display;
	private LocationManager locationManager;
	private static double current_speed=0, current_distance=200;
	private final static double RSU_LATI=53.522902, RSU_LONG=-113.517852;
	private static SpeedometerView speedometerView;
	private static boolean voice_message_flag=true,connected_flag=false;
	//private static ArrayList<Double> distance_arrayList=new ArrayList<Double>();
	private SensorManager sensorManager;
	private Sensor accelerationSensor;
	private static Record_entity record_entity=new Record_entity();
	private String device_id,file_name;
	private final String FILE_PATH_STRING=Environment.getExternalStorageDirectory().getPath();
	private CheckBox record_data_checkbox;
	private static double acceleration=0;
	//private static String messageString="";
	private static int color_label;
	private static String voice_messageString;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // setup UI view;
        count_TextView=(TextView)findViewById(R.id.signal_count);
        connecttion_TextView=(TextView)findViewById(R.id.connect_label);
        speedometerView=(SpeedometerView)findViewById(R.id.speedometerview);
        record_data_checkbox=(CheckBox)findViewById(R.id.record_data_button);
        acc_display=(TextView)findViewById(R.id.acceleration_display);
        speedometerView.setLabelConverter(new SpeedometerView.LabelConverter() {
			
			@Override
			public String getLabelFor(double progress, double maxProgress) {
				// TODO Auto-generated method stub
				return String.valueOf((int)Math.round(progress));
			}
		});
        
        speedometerView.setMaxSpeed(200);
        speedometerView.setMajorTickStep(20);
        speedometerView.setMinorTicks(1);
        speedometerView.setSpeed((int)(Math.random()*10+60));
        
       /* speedometerView.addColoredRange(20, 80, Color.GREEN);
        speedometerView.addColoredRange(80, 100, Color.YELLOW);
        speedometerView.addColoredRange(100, 200, Color.RED);*/
        sensorManager=(SensorManager)this.getSystemService(SENSOR_SERVICE);
        accelerationSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerationSensor,SensorManager.SENSOR_DELAY_NORMAL);
        
        
        TelephonyManager tManager=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		device_id=tManager.getDeviceId();
		file_name=createDir(FILE_PATH_STRING+"/EcoGLOSA/");
		 
		 
        locationManager=(LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {			
			@Override
			public void onInit(int status) {
				// TODO Auto-generated method stub
			
				if (status==TextToSpeech.SUCCESS) {
					textToSpeech.setLanguage(Locale.UK);	
					speakToText("Hello, Welcome to CST Connected Vehicle Lab!");
				}
			}
		});
        
        
        // setup the BlueTooth 
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter==null) {
			Toast.makeText(getApplicationContext(), "Your device doesn't support bluetooth~", Toast.LENGTH_SHORT).show();		
		}
        
       /* if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_CODE_BT);
		}*/
        
        
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCURABLE_TIME);
                startActivityForResult(discoverableIntent,REQUEST_CODE_BT);
         }else{
            if(acceptThread==null){
               acceptThread=new AcceptThread();
           	   acceptThread.start();
             }
         }
    }
    
    private String createDir(String path){
  		Log.v("test", "create fold");
  		File file=new File(path);
  		if (!file.exists()) {
  			file.mkdir();
  		}
  		return path;
  	}
    @SuppressWarnings("deprecation")
	public static void speakToText(String string){
    	
    	textToSpeech.speak(string, TextToSpeech.QUEUE_FLUSH, null);
    }
    protected void onActivityResult( int requestCode,  int resultCode, Intent data){
    	
    	switch (requestCode) {
		case REQUEST_CODE_BT:
			if (resultCode==DISCURABLE_TIME) {
				if (acceptThread==null) {
					acceptThread=new AcceptThread();
					acceptThread.start();
				}
			}
			break;
		default:
			break;
		}
    }
    
    private class AcceptThread extends Thread{
    	private BluetoothServerSocket mServerSocket=null;
    	
    	public AcceptThread(){
    		
    		BluetoothServerSocket tmp=null;
    		try {
    			
    			tmp=mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(MY_NAME, MY_UUID);
    			Log.v("bluetooth", "serversocket create success");
    			
			} catch (Exception e) {
				// TODO: handle exception	
				
				Log.v("bluetooth", "serversocket create fail");
			}
    		mServerSocket=tmp;
    	}
    	
    	public void run(){
    		BluetoothSocket mBluetoothSocket=null;
    		while(true){
    			try {
					mBluetoothSocket=mServerSocket.accept();
					
				} catch (Exception e) {
					// TODO: handle exception
			//		Toast.makeText(getApplicationContext(), "unable to connect!", Toast.LENGTH_SHORT).show();
					Log.v("bluetooth", "unable to connect");
					break;
				}
    			if (mBluetoothSocket!=null) {
					// do something about the socket;
    			
    			connected(mBluetoothSocket);
    				Log.v("bluetooth", "bluetooth connected");
    				try {
						mServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				break;
				}
    		}
    	}
    	public void cancel(){
    		try {
				mServerSocket.close();
				mHandler.obtainMessage(MESSAGE_DISCONNECTE).sendToTarget();
			} catch (Exception e) {
				// TODO: handle exception
			}
    	}
    }
    public synchronized void connected(BluetoothSocket socket){
    	if (acceptThread!=null) {
			acceptThread.cancel();acceptThread=null;
		}
    	if (mConnectedThread!=null) {
			mConnectedThread.cancel();mConnectedThread=null;
		}
    	mConnectedThread=new ConnectedThread(socket);
    	mConnectedThread.start();
    }
    
    private class ConnectedThread extends Thread{
    	private final BluetoothSocket mmSocket;
    	private  final InputStream mmInputStream;
    	//private  final OutputStream mmOutputStream;
    	
    	public ConnectedThread(BluetoothSocket socket){
    		
    		mmSocket=socket;
    		InputStream tmpInputStream=null;
    		//OutputStream tmpOutputStream=null;
    		try {
				tmpInputStream=socket.getInputStream();
				//tmpOutputStream=socket.getOutputStream();
			} catch (Exception e) {
				// TODO: handle exception
				mHandler.obtainMessage(MESSAGE_DISCONNECTE);
				Log.v("bluetooth", "tmp sockets not created");
				
			}
    		mmInputStream=tmpInputStream;
    	//	mmOutputStream=tmpOutputStream;
    		
    	}
    	public void run(){
    		byte[] buffer=new byte[1024];
    		int flag=0;
    		//String messageString="Hello, I am Android";
    	//	byte[] sent_message=messageString.getBytes();
    		while (true) {
				try {
					
				//	bytes=mmInputStream.read(buffer);
					//mmOutputStream.write(sent_message);
					int  result = mmInputStream.read(buffer, 0, mmInputStream.available()); 
					
					if (result>0) {
					//	Log.v("STTest", "before result:"+result);
						connected_flag=true;
						mHandler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
						mHandler.obtainMessage(MESSAGE_READ, result, -1, buffer).sendToTarget();
						flag=0;
						
					}else {
						flag++;
						if (flag>100000) {
							Log.v("STTest", "inside result:"+result);
							mHandler.obtainMessage(MESSAGE_DISCONNECTE).sendToTarget();
						}
						
					}
					
				} catch (Exception e) {
					// TODO: handle exception
					mHandler.obtainMessage(MESSAGE_DISCONNECTE).sendToTarget();
					Log.v("bluetooth", "disconnected");
				}
			}
    	}
    	public void cancel(){
    		try {
				mmSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
				Log.v("bluetooth", "close socket failed");
			}
    	}
    	
    }
    
    private static final Handler mHandler=new Handler(){
    	public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_READ:
				byte[] readbuf=(byte[]) msg.obj;
				String readMessageString=new String(readbuf,0,msg.arg1);
			//	String message_getString=new BigInteger(0,readbuf).toString(16);
			//	String message_getString=byteArrayToHex(readbuf);
				/*int size= readbuf.length;
				int dataint[]= new int[size];
				StringBuilder bStringBuilder=new StringBuilder();
				for (int i = 0; i < msg.arg1; i++) {
					dataint[i]=readbuf[i];
					bStringBuilder.append(Integer.toHexString(dataint[i]));
				}
				String message_getString=bStringBuilder.toString();*/
				readMessageString=readMessageString.substring(5);
				Log.v("bluetooth", "read:"+readMessageString);
			//	Log.v("bluetooth", "read:"+message_getString);
				InfoEntity temp_infoEntity=new InfoEntity();
					temp_infoEntity=MessageParse(readMessageString);
				UpdateUI(temp_infoEntity);
				
				if (voice_message_flag && connected_flag) {
					speakToText(GLOSAUpdate(temp_infoEntity));
					voice_message_flag=false;
				}
				
				break;
			case MESSAGE_CONNECTED:
				connecttion_TextView.setText("Connected");
				
				break;
			case MESSAGE_DISCONNECTE:
				connecttion_TextView.setText("not Connected");
				voice_message_flag=true;
				connected_flag=false;
				disconnected_UI();
				 break;
			default:
				break;
			}
		}
    };

    public static InfoEntity MessageParse(String msg){
    	
    	InfoEntity infoEntity=new InfoEntity();
    	
    	//int  count=Character.getNumericValue(msg.charAt(4));
    		int count=Integer.parseInt(msg.substring(3, 5));
    		Log.v("bluetooth", msg.substring(3, 5));
    	infoEntity.setSignal_time(count);
    	record_entity.setSignal_time(count);
    	if (msg.contains("GSB")) {
    		
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_RL);
			infoEntity.setSignal_color_code(InfoEntity.SINGAL_GREEN);
			color_label=3;
		}
    	if (msg.contains("RSB")) {
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_LEFT);
			infoEntity.setSignal_color_code(InfoEntity.SIGNAL_RED);
			color_label=1;
		}
    	if (msg.contains("YSB")) {
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_STRAIGHT);
			infoEntity.setSignal_color_code(InfoEntity.SIGNAL_YELLOW);
			color_label=2;
		}
    	if (current_speed!=-1) {
    		infoEntity.setSpeed(current_speed);
    		Log.v("STTest", current_speed+"speed");
		}
    	if (current_distance!=-1) {
    		infoEntity.setDistance(current_distance);
    		Log.v("STTest", current_distance+"distance");
		}
    	infoEntity.setAcceleration(acceleration);
    	return infoEntity;
    	
    }
   private static void disconnected_UI(){
		count_TextView.setVisibility(View.INVISIBLE);
		connecttion_TextView.setText("not Connected");
		speedometerView.clearColoredRanges();
		speedometerView.setSpeed(0);
		voice_messageString=null;
   }
   
 private static String GLOSAUpdate(InfoEntity infoEntity){
	   
	   String advice_mesageString = null;
	   double v_0=infoEntity.getSpeed();
	   double x=infoEntity.getDistance();
	  
	  
	   double v_max=infoEntity.getMax_speed();
	   double t1=x/v_0;
	   int t_m=infoEntity.getSignal_time();
	   
	   if (infoEntity.getSignal_color_code()==SIGNAL_GREEN) {
		   
		   double a_r=1.70*Math.exp(-0.04*v_0);
		   double t2=(x-(Math.pow(v_max, 2)-Math.pow(v_0, 2))/(2*a_r))/v_max
				   +(v_max-v_0)/a_r;
		   if (t1<t_m) {
			advice_mesageString=KEEP_CURRENT_SPEED;
		   }else if (t2<=t_m && t1>t_m) {
			double v_s;
			v_s=v_0+a_r*t_m+Math.pow((a_r*(a_r*t_m*t_m+2*t_m*v_0-2*x)), 0.5);
			if (v_s>v_max) {
				advice_mesageString=TURN_RED;
			}else {
				advice_mesageString=SPEED_UP+(int)v_s;
			}
			
		   }else if (t2>t_m) {
			advice_mesageString=TURN_RED;
		   }else {
			// do nothing;
		   }
		   
	   }else if (infoEntity.getSignal_color_code()==SIGNAL_YELLOW) {
		   advice_mesageString=TURN_RED;
		   
	   }else if (infoEntity.getSignal_color_code()==SIGNAL_RED){
		   double a_d=-0.005*Math.pow(v_0, 3)+0.154*v_0+0.493;
		   double v_min=0.5*v_0;
		   double t2=(x-(v_0*v_0-v_min*v_min)/(2*a_d))/v_min+(v_0-v_min)/a_d;
		   if (t1>t_m) {
			   advice_mesageString=KEEP_CURRENT_SPEED;
		   }else if (t1<t_m && t2>t_m) {
			   double v_s=v_0-a_d*t_m+Math.pow((a_d*(a_d*t_m*t_m-2*t_m*v_0+2*x)), 0.5);
			   if (v_s<v_max && v_s >v_min) {
				   advice_mesageString=SLOW_DOWN+(int)v_s;
			   }else {
				   advice_mesageString=RED_SIGNAL_AHEAD;
			   }
			   
		   }else if (t2<=t_m) {
			   advice_mesageString=RED_SIGNAL_AHEAD;
		   }else {
			   // do nothing;
		   }
			
	   }else {
		   // do nothing;
	   }
	   
	   voice_messageString=advice_mesageString;
	   return advice_mesageString;
	   
   }

 /* public static  String SafetyMsg(int color_label, int remaining_time, double dact, double Vveh, double aveh){

		// implement SafetyMsg..
		// waiting for the specific values 
		// if color_label==1 , then it green, if color_label==0, then red, if color_label==2, then yellow
		
		int Yellow;
		double  dcal, tMsg = 0, Vvehfinal;
		String MsgG, MsgS, Msgf = null;
		int t, deltaG, deltaR ;
		
		if ((color_label==3)){
			deltaG=remaining_time;
			Yellow=3;
			deltaR=0;
		}else{ 
			if ((color_label==2)){
			deltaG=0;
			Yellow=remaining_time;
			deltaR=0;
			}else{ 
			deltaG=0; 
			Yellow=0;
			deltaR=remaining_time;
			}
		}

				
		MsgG="Clear Go!";
		MsgS="Prepare to STOP!";
		if (Vveh>50){
			 Vvehfinal=50;
		}else{ Vvehfinal=Vveh-Vveh%5;				 
   	}
		String MsgD= "Drive below " + Vvehfinal+"" + " kilometer per hour";
		
		if((deltaG>0)|(Yellow>0)){
			t=deltaG+Yellow;
			dcal=(Vveh*t/3.6)+0.5*aveh* Math.pow(t, 2);
			if (dcal>=dact){
				if (Vveh>50){
					Msgf=MsgD;
				}else{
					Msgf=MsgG; tMsg=1.5;
				}
			}else{
				Msgf=MsgS; tMsg=2;
			}

		}else{  if (deltaR>0){
				t=deltaR; 
				dcal=(Vveh*t/3.6)+0.5*aveh* Math.pow(t, 2);
					
					if(dcal>=dact){
						Msgf=MsgS;tMsg=1.5;
					}else{
						Msgf=MsgD; tMsg=3;
					}
		}
	}
		double f, dmsg=Vveh*tMsg/3.6; 
		if (Vveh>40){
			f=0.36;
		}else{ 
			f=0.38;
		}

		double SD=Vveh/3.6+Math.pow(Vveh, 2)/(254*f);
		if ((dact-Vveh*1/3.6)<=(dmsg+SD)){
			return Msgf;
		}else{ 
			return  null;
		}	
}
   */
   // get the advice message type;
   /*private static String GLOSAUpdate(InfoEntity infoEntity){
	   
	   String advice_mesageString = null;
	   double v_0=infoEntity.getSpeed();
	   double x=infoEntity.getDistance();
	   double d=infoEntity.getAcceleration();
	   int t_m=infoEntity.getSignal_time();
	   
	   advice_mesageString=SafetyMsg(infoEntity.getSignal_color_code(),t_m, x,v_0,d);
	   voice_messageString=advice_mesageString;
	   Log.v("STTest", "Message:"+voice_messageString);
	   return advice_mesageString;
	   
   }*/
   
    private static void UpdateUI(InfoEntity infoentity){
       count_TextView.setVisibility(View.VISIBLE);
      /* distance_arrayList.add(infoentity.getDistance());
  	   int array_size=distance_arrayList.size();
  	   if (array_size>4) {
  		   if (distance_arrayList.get(array_size-1)>distance_arrayList.get(array_size-2) && distance_arrayList.get(array_size-2)>distance_arrayList.get(array_size-3)) {
  				mHandler.obtainMessage(MESSAGE_DISCONNECTE);
  			   }
  	   }*/
    	double critical_speed=3.6*infoentity.getDistance()/infoentity.getSignal_time();
    	boolean critical_flag=true;
    	if (critical_speed>=200) {
    		critical_speed=200;
			critical_flag=false;
		}
    	speedometerView.clearColoredRanges();
    	switch (infoentity.getSignal_color_code()) {
		case InfoEntity.SIGNAL_RED:
			count_TextView.setTextColor(Color.RED);
			if (critical_flag) {
				speedometerView.addColoredRange(critical_speed, 200, Color.RED);
			}
			speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+60), critical_speed, Color.GREEN);
			speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+63), 3.6*infoentity.getDistance()/(infoentity.getSignal_time()+60), Color.YELLOW);
			break;
		case InfoEntity.SIGNAL_YELLOW:
			count_TextView.setTextColor(Color.YELLOW);
			if (critical_flag) {
				speedometerView.addColoredRange(critical_speed, 200, Color.YELLOW);
			}
			
			speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+60), critical_speed, Color.GREEN);
			speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+120), 3.6*infoentity.getDistance()/(infoentity.getSignal_time()+60), Color.RED);
			break;
		case InfoEntity.SINGAL_GREEN:
			count_TextView.setTextColor(Color.GREEN);
			if (critical_flag) {
				speedometerView.addColoredRange(critical_speed, 200, Color.GREEN);
			}
			if (3.6*infoentity.getDistance()/(infoentity.getSignal_time()+3)<200) {
				speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+3), critical_speed, Color.YELLOW);
			}
			speedometerView.addColoredRange(3.6*infoentity.getDistance()/(infoentity.getSignal_time()+63), 3.6*infoentity.getDistance()/(infoentity.getSignal_time()+3), Color.RED);
			break;
		default: 
			count_TextView.setVisibility(TextView.GONE);
			connecttion_TextView.setText("not Connected");
			break;
		}
    	speedometerView.setSpeed(infoentity.getSpeed());
    	count_TextView.setText(""+infoentity.getSignal_time());
    	
    }
    @Override
    public void onPause(){
    	if (textToSpeech!=null) {
			textToSpeech.stop();
			textToSpeech.shutdown();
		}
    	super.onPause();
    }
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (acceptThread!=null) {
			acceptThread.cancel();
		}
		if (mConnectedThread!=null) {
			mConnectedThread.cancel();
		}
		
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		current_speed=location.getSpeed();
		Location temp_location=new Location("test");
		temp_location.setLatitude(RSU_LATI);
		temp_location.setLongitude(RSU_LONG);
		current_distance=location.distanceTo(temp_location);
		Log.v("STTest", "speed:"+current_speed+"distance:"+current_distance);
		record_entity.setLatitude(location.getLatitude());
		record_entity.setLongitude(location.getLongitude());
		record_entity.setSpeed(location.getSpeed());
		record_entity.setDistance(current_distance);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		Sensor sensor=event.sensor;
		switch (sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			record_entity.setAcc_x(event.values[0]);
			record_entity.setAcc_y(event.values[1]);
			record_entity.setAcc_z(event.values[2]);
			record_entity.setTime(System.currentTimeMillis());
			record_entity.setColor_label(color_label);
			record_entity.setMessage(voice_messageString);
			acceleration=event.values[1];
			acc_display.setText("ACC_Y:"+record_entity.getAcc_y());
			break;
		default:
			break;
		}
		if (record_data_checkbox.isChecked()) {
			writeToFile(record_entity);
		}
		
	}
	
private void writeToFile(Record_entity entity){
		
		SimpleDateFormat dFormat=new SimpleDateFormat("MM_dd_yyyy");
		String nowdataString=dFormat.format(new Date());
		File sensorsdatafile=new File(file_name+nowdataString+device_id+"SPaTSafety.txt");
		boolean sensorsdata=true;
	
		try {
			if (!sensorsdatafile.exists()) {
				sensorsdatafile.createNewFile();
				sensorsdata=false;
			}
			
			OutputStream myOutputStream=new FileOutputStream(sensorsdatafile,true);
			OutputStreamWriter myOutputStreamWriter=new OutputStreamWriter(myOutputStream);
			if (!sensorsdata) {
				myOutputStreamWriter.append("Time,Signal_time,Speed,Distance,Acc_x,Acc_y,Acc_z,Longitude,Latitude,message,color_label\n");
			}
		
			String sensor_tmp=entity.getTime()+","+entity.getSignal_time()+","+entity.getSpeed()+","+entity.getDistance()+","+entity.getAcc_x()+","+entity.getAcc_y()+","+entity.getAcc_z()+","
			+entity.getLongitude()+","+entity.getLatitude()+","+entity.getMessage()+","+entity.getColor_label()+"\n";
			
			myOutputStreamWriter.append(sensor_tmp);
			
			myOutputStreamWriter.close();
			myOutputStream.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		
	}
}
