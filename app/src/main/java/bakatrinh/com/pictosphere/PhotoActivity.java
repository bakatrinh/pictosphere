package bakatrinh.com.pictosphere;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.maps.model.LatLng;

public class PhotoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    boolean mPermissionDenied;
    PhotoActivityFragmentPortrait fragmentPortrait;
    PhotoActivityFragmentLandscape fragmentLandscape;
    FrameLayout fragmentContainerPortrait;
    FrameLayout fragmentContainerLandscape;
    private PictosphereStorageObserver pictosphereStorageObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);

        fragmentContainerPortrait = findViewById(R.id.main_fragment_container_portrait);
        fragmentContainerLandscape = findViewById(R.id.main_fragment_container_landscape);

        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
        fragmentPortrait = new PhotoActivityFragmentPortrait();
        manager.beginTransaction().add(R.id.main_fragment_container_portrait, fragmentPortrait).commit();
        fragmentLandscape = new PhotoActivityFragmentLandscape();
        manager.beginTransaction().add(R.id.main_fragment_container_landscape, fragmentLandscape).commit();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fragmentContainerLandscape.setVisibility(View.GONE);
            fragmentContainerPortrait.setVisibility(View.VISIBLE);
        } else {
            fragmentContainerPortrait.setVisibility(View.GONE);
            fragmentContainerLandscape.setVisibility(View.VISIBLE);
        }
        pictosphereStorageObserver = new PictosphereStorageObserver(new Handler());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(PictosphereStorage.URI_IMAGE_POST, true, pictosphereStorageObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fragmentContainerLandscape.setVisibility(View.GONE);
            fragmentContainerPortrait.setVisibility(View.VISIBLE);
        } else {
            fragmentContainerPortrait.setVisibility(View.GONE);
            fragmentContainerLandscape.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    public void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, PhotoActivity.LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (fragmentLandscape.mMap != null) {
            // Access to the location has been granted to the app.
            fragmentLandscape.mMap.setMyLocationEnabled(true);
            fragmentLandscape.redrawGoogleMaps();
        }
    }

    LatLng getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) this.getSystemService(PhotoActivity.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String locationProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(locationProvider);
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            return new LatLng(latitude, longitude);
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != PhotoActivity.LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    public void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    class PictosphereStorageObserver extends ContentObserver {

        public PictosphereStorageObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(MainActivity.TAG, "StorageObserver onChange: " + uri.toString());
        }
    }

    public void testInsert(View v) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_USER_ID, "45");
        contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LONGITUDE, "cat");
        contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LATITUDE, "dog");
        contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_IMAGE, "pig");
        getContentResolver().insert(PictosphereStorage.URI_IMAGE_POST, contentValues);
    }

    public void testQuery(View v) {
        Cursor c1 = getContentResolver().query(PictosphereStorage.URI_IMAGE_POST, null, null, null, null);
        if (c1 != null && c1.isBeforeFirst()) {
            while (c1.moveToNext()) {
                Log.d(MainActivity.TAG, c1.getString(0));
                Log.d(MainActivity.TAG, c1.getString(1));
                Log.d(MainActivity.TAG, c1.getString(2));
                Log.d(MainActivity.TAG, c1.getString(3));
                Log.d(MainActivity.TAG, c1.getString(4));
                Log.d(MainActivity.TAG, c1.getString(5));
                Log.d(MainActivity.TAG, "---------------------");
            }
        }
    }

    public void settings(View v) {
        Intent intent = new Intent(PhotoActivity.this, AppInfoActivity.class);
        startActivity(intent);
    }
}
