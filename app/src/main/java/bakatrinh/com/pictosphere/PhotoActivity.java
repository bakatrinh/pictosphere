package bakatrinh.com.pictosphere;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int PICK_USER_PROFILE_IMAGE = 1000;

    static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final int MY_CAMERA_REQUEST_CODE = 100;
    static final String internalPhotoPath = "/Android/data/bakatrinh.com.pictosphere/files/Pictures";

    static final int UPDATE_IMAGES = 1;
    boolean mPermissionDenied;
    PhotoActivityFragmentPortrait fragmentPortrait;
    PhotoActivityFragmentLandscape fragmentLandscape;
    FrameLayout fragmentContainerPortrait;
    FrameLayout fragmentContainerLandscape;
    String mCurrentPhotoPath;
    String mGoogleEmail;
    private PictosphereStorageObserver pictosphereStorageObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);

        mGoogleEmail = getIntent().getStringExtra(MainActivity.GOOGLEEMAIL);

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

        if (requestCode == PhotoActivity.LOCATION_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            } else {
                // Display the missing permission error dialog when the fragments resume.
                mPermissionDenied = true;
            }
        }
        if (requestCode == PhotoActivity.MY_CAMERA_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.CAMERA)) {
                startCameraActivity();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
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
            Message msg = fragmentPortrait.mHandler.obtainMessage(PhotoActivity.UPDATE_IMAGES);
            fragmentPortrait.mHandler.sendMessage(msg);
        }
    }

    public void takePicture(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                               MY_CAMERA_REQUEST_CODE);
        } else {
            startCameraActivity();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void startCameraActivity() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                                                          "bakatrinh.com.pictosphere.fileprovider",
                                                          photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, PICK_USER_PROFILE_IMAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_USER_PROFILE_IMAGE) {
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);

            String address = "";
            double longitude = 0;
            double latitude = 0;
            LatLng tempLatLong = getCurrentLocation();
            if (tempLatLong != null) {
                longitude = tempLatLong.longitude;
                latitude = tempLatLong.latitude;
                try {
                    address = getAddressGeoCoder(latitude, longitude);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_USER_ID, mGoogleEmail);
            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LONGITUDE, Double.toString(longitude));
            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LATITUDE, Double.toString(latitude));
            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_IMAGE, contentUri.toString());
            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_ADDRESS, address);
            getContentResolver().insert(PictosphereStorage.URI_IMAGE_POST, contentValues);
        }
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
                Log.d(MainActivity.TAG, c1.getString(6));
                Log.d(MainActivity.TAG, "---------------------");
            }
        }
    }

    public void settings(View v) {
        Intent intent = new Intent(PhotoActivity.this, AppInfoActivity.class);
        startActivity(intent);
    }

    public Bitmap getBitmapFromPath() {
        if (mCurrentPhotoPath != null) {
            File imgFile = new File(mCurrentPhotoPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                return myBitmap;
            }
        }
        return null;
    }

    public String getAddressGeoCoder(double latitude, double longitude) throws IOException {
        String returnString = "";
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        if (geocoder != null) {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);

            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            if (!city.isEmpty()) {
                returnString += city;
            }
            if (!state.isEmpty()) {
                if (!returnString.isEmpty() && returnString.length() > 0) {
                    returnString += ", ";
                }
                returnString += state;
            }
            if (!country.isEmpty()) {
                if (!returnString.isEmpty() && returnString.length() > 0) {
                    returnString += ", ";
                }
                returnString += country;
            }
        }
        return returnString;
    }

    public static Bitmap resizeImage(String filePath, int targetW, int targetH) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, bmOptions);
        return bitmap;
    }
}
