package com.testloginext;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service implements
		android.location.LocationListener {
	public static final String BROADCAST_ACTION = "com.testloginext";

	private boolean isGPSEnabled = false;
	private boolean isNetworkEnabled = false;
	private double latitude;
	private double longitude;

	private static final long DISTANCE_CHANGE_UPDATES = 10;
	private static final long NEXT_RUN = 1000 * 60 * 2;

	private Location location;

	protected LocationManager locationManager;
	private final Handler handler = new Handler();
	Intent intent;

	@Override
	public void onCreate() {
		super.onCreate();
		intent = new Intent(BROADCAST_ACTION);
		Log.e("created service", "service");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getLocation();
		handler.removeCallbacks(sendUpdatesToUI);
		Log.e("service destroyed", "service destroyed");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("onStartCommand", "onStartCommand");

		handler.removeCallbacks(sendUpdatesToUI);
		handler.postDelayed(sendUpdatesToUI, 1000); // first run
		return START_STICKY;
	}

	private Runnable sendUpdatesToUI = new Runnable() {
		public void run() {
			DisplayLoggingInfo();
			handler.postDelayed(this, NEXT_RUN); // 2minutes
		}
	};

	private void DisplayLoggingInfo() {
		Location location = getLocation();
		if (location != null) {
			intent.putExtra("latitude", latitude);
			intent.putExtra("longitude", longitude);
			intent.putExtra("location_time", location.getTime());
			intent.putExtra("location_accuracy", location.getAccuracy());
			intent.putExtra("provider", location.getProvider());
			sendBroadcast(intent);
		} else {
			intent.putExtra("isLocationAvailable", false);
			sendBroadcast(intent);
		}
	}

	public Location getLocation() {
		try {
			locationManager = (LocationManager) getApplicationContext()
					.getSystemService(LOCATION_SERVICE);

			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (isNetworkEnabled) {
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, NEXT_RUN,
						DISTANCE_CHANGE_UPDATES, this);
				Log.e("Network", "Network");
				if (locationManager != null) {
					location = locationManager
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if (location != null) {
						latitude = location.getLatitude();
						longitude = location.getLongitude();
					}
				}
			}
			if (isGPSEnabled) {
				if (location == null) {
					locationManager.requestLocationUpdates(
							LocationManager.GPS_PROVIDER, NEXT_RUN,
							DISTANCE_CHANGE_UPDATES, this);
					Log.d("GPS Enabled", "GPS Enabled");
					if (locationManager != null) {
						location = locationManager
								.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						if (location != null) {
							latitude = location.getLatitude();
							longitude = location.getLongitude();
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.e(latitude + "", longitude + "");
		return location;
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
