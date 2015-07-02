package cst.aaron.spatsafety;

public class Record_entity {
	private double latitude;
	private double longitude;
	private long time;
	private float acc_x,acc_y,acc_z;
	private float speed; 
	private double distance;
	private String message;
	private int color_label;
	private int signal_time;
	
	public int getSignal_time() {
		return signal_time;
	}

	public void setSignal_time(int signal_time) {
		this.signal_time = signal_time;
	}

	public Record_entity(){
		
	}

	public int getColor_label() {
		return color_label;
	}

	public void setColor_label(int color_label) {
		this.color_label = color_label;
	}


	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}


	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public float getAcc_x() {
		return acc_x;
	}

	public void setAcc_x(float acc_x) {
		this.acc_x = acc_x;
	}

	public float getAcc_y() {
		return acc_y;
	}

	public void setAcc_y(float acc_y) {
		this.acc_y = acc_y;
	}

	public float getAcc_z() {
		return acc_z;
	}

	public void setAcc_z(float acc_z) {
		this.acc_z = acc_z;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
}
