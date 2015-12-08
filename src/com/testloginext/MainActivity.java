package com.testloginext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity {
	final private static int PICK_IMAGE_REQUEST = 1;
	final private static int REQUEST_IMAGE_CAPTURE = 2;

	final private static String CAMERA = "Camera";
	final private static String GALLERY = "From Gallery";

	final static List<LatLng> latLongList = new ArrayList<LatLng>();

	private ImageView imageView;
	private SupportMapFragment supportMapFragment;
	private GoogleMap myMap;

	private Intent intent;

	private Double mock_latitude = 0.0;
	private Double mock_longitude = 0.0;

	private long last_best_time;
	private Double lastlocationAccuracy;
	private String lastBestProvider;
	/* Mock addition */
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private static final int DATA_POINTS = 20;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		imageView = (ImageView) findViewById(R.id.imageView);
		final Button btnPhoto = (Button) (findViewById(R.id.btnPhoto));
		btnPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				shutDownService();
				supportMapFragment.getView().setVisibility(View.GONE);
				imageView.setVisibility(View.VISIBLE);
				chooseImage();
			}
		});
		final Button btnRealTimeTracking = (Button) (findViewById(R.id.btnRealTimeTracking));
		btnRealTimeTracking.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				supportMapFragment.getView().setVisibility(View.VISIBLE);
				imageView.setVisibility(View.GONE);
				runService();
			}
		});
		FragmentManager myFragmentManager = getSupportFragmentManager();
		supportMapFragment = (SupportMapFragment) myFragmentManager
				.findFragmentById(R.id.map);
		myMap = supportMapFragment.getMap();
	}

	@Override
	public void onResume() {
		super.onResume();
		intent = new Intent(getApplicationContext(), LocationService.class);
		startService(intent);
		registerReceiver(broadcastReceiver, new IntentFilter(
				LocationService.BROADCAST_ACTION));
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
		stopService(intent);
	}

	protected void runService() {
		Intent myIntent = new Intent(getApplicationContext(),
				LocationService.class);
		startService(myIntent);
	}

	protected void shutDownService() {
		Intent myIntent = new Intent(getApplicationContext(),
				LocationService.class);
		stopService(myIntent);
	}

	private void chooseImage() {
		final CharSequence[] items = { CAMERA, GALLERY, "Cancel" };
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Add Photo");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (items[item].equals(CAMERA)) {
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
				} else if (items[item].equals(GALLERY)) {
					Intent intent = new Intent(
							Intent.ACTION_PICK,
							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					intent.setType("image/*");
					startActivityForResult(
							Intent.createChooser(intent, "Select File"),
							PICK_IMAGE_REQUEST);
				} else if (items[item].equals("Cancel")) {
					dialog.dismiss();
				}
			}
		});
		builder.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
				&& data != null && data.getData() != null) {

			Uri uri = data.getData();
			try {
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(
						getContentResolver(), uri);
				ImageView imageView = (ImageView) findViewById(R.id.imageView);
				imageView.setImageBitmap(bitmap);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

			String imageFilePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/name.png";
			Bundle extras = data.getExtras();
			Bitmap imageBitmap = (Bitmap) extras.get("data");
			ImageView imageView = (ImageView) findViewById(R.id.imageView);

			imageView.setImageBitmap(imageBitmap);
			try {
				FileOutputStream out = new FileOutputStream(imageFilePath);
				imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				Toast.makeText(getApplicationContext(),
						"Saved Successfully at " + imageFilePath,
						Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void addLocationData(Double latitude, Double longitude) {
		if (latLongList.size() == DATA_POINTS) {
			latLongList.remove(0);
		}
		latLongList.add(new LatLng(latitude, longitude));
		if (imageView.getVisibility() != View.VISIBLE) {
			Toast.makeText(getApplicationContext(),
					"latitude: " + latitude + ", longitude: " + longitude,
					Toast.LENGTH_SHORT).show();
		}
		updateMap();
	}

	private void updateMap() {
		addLines();
	}

	private void addLines() {
		if (latLongList != null && latLongList.size() != 0) {
			myMap.addPolyline((new PolylineOptions()).addAll(latLongList)
					.width(5).color(Color.BLUE).geodesic(true));
			myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
					latLongList.get(latLongList.size() - 1), 13));

			CircleOptions circleOptions = new CircleOptions()
					.center(latLongList.get(latLongList.size() - 1)).radius(50)
					.fillColor(Color.BLACK); // In
			myMap.addCircle(circleOptions);

		}
		Log.e("Poly Lines", latLongList.size() + "");
	}

	class UpdateUI implements Runnable {
		boolean isAlive = true;
		private Double longitude;
		private Double latitude;

		public UpdateUI(Double latitude, Double longitude, boolean isAlive) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.isAlive = isAlive;
		}

		@Override
		public void run() {
			if (!isAlive) {
				addLocationData(latitude, longitude);
				stopService(new Intent(MainActivity.this, LocationService.class));
			}

		}
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};

	private void updateUI(Intent intent) {
		final boolean isLocationAvailable = intent.getBooleanExtra(
				"isLocationAvailable", true);
		if (isLocationAvailable) {
			Double latitude = intent.getDoubleExtra("latitude", 0.0);
			Double longitude = intent.getDoubleExtra("longitude", 0.0);
			long currentTime = intent.getLongExtra("location_time", 0l);
			Double locationAccuracy = intent.getDoubleExtra(
					"location_accuracy", 0.0);
			String currentProvider = intent.getStringExtra("provider");

			/* Mock addition */
			if (mock_latitude != 0.0d && mock_longitude != 0.0d) {
				// latitude = mock_latitude + 0.001;
				// longitude = mock_longitude + 0.01;
			}
			mock_latitude = latitude;
			mock_longitude = longitude;
			boolean isBestLocation = checkBestLocation(latitude, longitude,
					currentTime, locationAccuracy, currentProvider);
			// Remove Below Line to check for Accuracy of data
			// isBestLocation = true;
			if (isBestLocation) {
				addLocationData(latitude, longitude);
				last_best_time = currentTime;
				lastlocationAccuracy = locationAccuracy;
				lastBestProvider = currentProvider;
			}
		} else {
			Toast.makeText(MainActivity.this, R.string.turn_on_gps,
					Toast.LENGTH_LONG).show();
			Intent myIntent = new Intent(
					Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			this.startActivity(myIntent);
		}
	}

	/** Code to check accuracy of location result */
	private boolean checkBestLocation(Double latitude, Double longitude,
			long currentTime, Double locationAccuracy, String currentProvider) {
		if (latLongList.size() == 0) {
			return true;
		}
		// Time Difference
		long timeDelta = currentTime - last_best_time;
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;
		if (isSignificantlyNewer) {
			return true;
		} else if (isSignificantlyOlder) {
			return false;
		}
		// Accuracy Difference
		int accuracyDelta = (int) (locationAccuracy - lastlocationAccuracy);
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Provider Difference
		boolean isFromSameProvider = isSameProvider(currentProvider,
				lastBestProvider);

		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}

		return false;

	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
