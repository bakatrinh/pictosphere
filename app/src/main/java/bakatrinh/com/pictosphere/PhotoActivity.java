package bakatrinh.com.pictosphere;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int PICK_USER_PROFILE_IMAGE = 1000;

    static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final int MY_CAMERA_REQUEST_CODE = 100;
    static final String internalPhotoPath = "/Android/data/bakatrinh.com.pictosphere/files/Pictures";

    static final int UPDATE_IMAGES = 1;
    static final int UPDATE_MAP = 1;
    static final int UPDATE_MAP_INITIAL = 2;
    boolean mPermissionDenied;
    PhotoActivityFragmentPortrait fragmentPortrait;
    PhotoActivityFragmentLandscape fragmentLandscape;
    FrameLayout fragmentContainerPortrait;
    FrameLayout fragmentContainerLandscape;
    String mCurrentPhotoPath;
    String getmCurrentPhotoPathThumb;
    String mGoogleEmail;
    ArrayList<ArrayList<String>> mImagesContainer;
    private PictosphereStorageObserver pictosphereStorageObserver;
    ListDataAdapter mListDataAdapter;
    int mListImageWidth;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);

        mContext = this;

        mImagesContainer = new ArrayList<>();
        mListDataAdapter = new ListDataAdapter();

        mGoogleEmail = getIntent().getStringExtra(MainActivity.BUNDLE_GOOGLE_EMAIL);

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
        mListImageWidth = dpToPx(80);
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
        rebuildImagesArray();
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
            fragmentLandscape.setCurrentLocation(getCurrentLocation());
            // Access to the location has been granted to the app.
            fragmentLandscape.mMap.setMyLocationEnabled(true);
            fragmentLandscape.redrawGoogleMaps();
        }
    }

    LatLng getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(MainActivity.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }
            if (bestLocation != null) {
                double latitude = bestLocation.getLatitude();
                double longitude = bestLocation.getLongitude();
                return new LatLng(latitude, longitude);
            }
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
            rebuildImagesArray();
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

    private File createImageFileThumb() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                "_thumb.jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        getmCurrentPhotoPathThumb = image.getAbsolutePath();
        return image;
    }

    public void editImage(int position) {
        final ArrayList<String> imageData = mImagesContainer.get(position);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText message = new EditText(this);
        message.setHint("Enter a note about this image");
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        message.setText(imageData.get(7));

        LinearLayout layout2 = new LinearLayout(this);
        layout2.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout layout3 = new LinearLayout(this);
        layout3.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout layout4 = new LinearLayout(this);
        layout4.setOrientation(LinearLayout.HORIZONTAL);

        layout.addView(message);
        layout.addView(layout2);
        layout.addView(layout3);
        layout.addView(layout4);

        final EditText latitude = new EditText(this);
        latitude.setHint("Latitude");
        latitude.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        latitude.setText(imageData.get(2));

        final TextView textView1 = new TextView(this);
        textView1.setText("Latitude: ");

        layout2.addView(textView1);
        layout2.addView(latitude);

        final EditText longitude = new EditText(this);
        longitude.setHint("Longitude");
        longitude.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        longitude.setText(imageData.get(3));

        final TextView textView2 = new TextView(this);
        textView2.setText("Longitude: ");

        layout3.addView(textView2);
        layout3.addView(longitude);

        final Button clearButton = new Button(this);
        clearButton.setText("Clear Coordinates");
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                longitude.setText("");
                latitude.setText("");
            }
        });

        layout4.addView(clearButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Info About This Image")
                .setView(layout)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (longitude.getText().toString().isEmpty() || latitude.getText().toString().isEmpty()) {
                    Toast.makeText(mContext, "Longitude and Latitude cannot be empty", Toast.LENGTH_LONG).show();
                } else {
                    String newMessage = message.getText().toString().trim();
                    double latitudeNumber = Double.parseDouble(latitude.getText().toString());
                    double longitudeNumber = Double.parseDouble(longitude.getText().toString());
                    ContentValues contentValues = new ContentValues();

                    if (Math.abs(Math.abs(Double.parseDouble(imageData.get(2))) - Math.abs(latitudeNumber)) > 0.5 || Math.abs(Math.abs(Double.parseDouble(imageData.get(3))) - Math.abs(longitudeNumber)) > 0.5 || imageData.get(6).isEmpty() || imageData.get(6).length() <= 0) {
                        String newGeoCode = "";
                        try {
                            newGeoCode = getAddressGeoCoder(latitudeNumber, longitudeNumber);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!newGeoCode.isEmpty() && newGeoCode.length() > 0) {
                            contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_ADDRESS, newGeoCode);
                        }
                    }

                    contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_MESSAGE, newMessage);
                    contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LATITUDE, Double.toString(latitudeNumber));
                    contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LONGITUDE, Double.toString(longitudeNumber));
                    String whereClause = PictosphereStorage.COLUMN_IMAGE_POSTS_ID + "=?";
                    String[] whereArgs = {imageData.get(0)};
                    getContentResolver().update(PictosphereStorage.URI_IMAGE_POST, contentValues, whereClause, whereArgs);
                    dialog.dismiss();
                }
            }
        });
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
            insertNewImage();
        }
    }

    public void insertNewImage() {
        Runnable insertNewImageTask = new Runnable() {
            @Override
            public void run() {
                String address = "";
                double longitude = 0;
                double latitude = 0;
                LatLng tempLatLong = getCurrentLocation();
                if (tempLatLong != null) {
                    latitude = tempLatLong.latitude;
                    longitude = tempLatLong.longitude;
                    try {
                        address = getAddressGeoCoder(latitude, longitude);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                File photoFileThumb = null;
                try {
                    photoFileThumb = createImageFileThumb();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (photoFileThumb != null) {
                    Bitmap bitmap = resizeImage(mCurrentPhotoPath, mListImageWidth, mListImageWidth);
                    try {
                        FileOutputStream out = new FileOutputStream(photoFileThumb);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                ContentValues contentValues = new ContentValues();
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_USER_ID, mGoogleEmail);
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LATITUDE, Double.toString(latitude));
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_LONGITUDE, Double.toString(longitude));
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_IMAGE, mCurrentPhotoPath);
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_IMAGE_THUMB, getmCurrentPhotoPathThumb);
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_ADDRESS, address);
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_MESSAGE, "");
                getContentResolver().insert(PictosphereStorage.URI_IMAGE_POST, contentValues);
            }
        };
        new Thread(insertNewImageTask).start();
    }

    public void rebuildImagesArray() {
        final Runnable imageArrayTask = new Runnable() {
            @Override
            public void run() {
                ArrayList<ArrayList<String>> tempImagesContainer = new ArrayList<>();
                String[] projection = {"*", "case strftime('%m', " + PictosphereStorage.COLUMN_IMAGE_POSTS_DATE + ") when '01' then 'January' when '02' then 'Febuary' when '03' then 'March' when '04' then 'April' when '05' then 'May' when '06' then 'June' when '07' then 'July' when '08' then 'August' when '09' then 'September' when '10' then 'October' when '11' then 'November' when '12' then 'December' else '' end as calendar_month", "strftime('%d, %Y', " + PictosphereStorage.COLUMN_IMAGE_POSTS_DATE + ") as formatted_date"};
                String whereClause = PictosphereStorage.COLUMN_IMAGE_POSTS_USER_ID + "=?";
                String[] whereArgs = {mGoogleEmail};
                Cursor c1 = getContentResolver().query(PictosphereStorage.URI_IMAGE_POST, projection, whereClause, whereArgs, null);
                if (c1 != null && c1.isBeforeFirst()) {
                    while (c1.moveToNext()) {
                        String arrayAdapterString = "";

                        ArrayList<String> temp = new ArrayList<>();
                        temp.add(c1.getString(0));
                        temp.add(c1.getString(1));
                        temp.add(c1.getString(2));
                        temp.add(c1.getString(3));
                        temp.add(c1.getString(4));
                        temp.add(c1.getString(5));
                        temp.add(c1.getString(6));
                        temp.add(c1.getString(7));
                        temp.add(c1.getString(8));
                        temp.add(c1.getString(9));
                        temp.add(c1.getString(10));

                        arrayAdapterString = getAdapterTextString(temp);
                        temp.add(arrayAdapterString);

                        tempImagesContainer.add(temp);
                    }
                }
                mImagesContainer = tempImagesContainer;
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (fragmentPortrait.mHandler != null) {
                        Message msg = fragmentPortrait.mHandler.obtainMessage(PhotoActivity.UPDATE_IMAGES);
                        fragmentPortrait.mHandler.sendMessage(msg);
                    }
                } else {
                    if (fragmentLandscape.mHandler != null) {
                        Message msg2 = fragmentLandscape.mHandler.obtainMessage(PhotoActivity.UPDATE_MAP);
                        fragmentLandscape.mHandler.sendMessage(msg2);
                    }
                }
            }
        };
        new Thread(imageArrayTask).start();
    }

    public void deleteAPicture(final String id, final String filepath, final String filepaththumb) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete?")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String whereClause = PictosphereStorage.COLUMN_IMAGE_POSTS_ID + "=?";
                        String[] whereArgs = {id};
                        getContentResolver().delete(PictosphereStorage.URI_IMAGE_POST, whereClause, whereArgs);
                        File file = new File(filepath);
                        File filethumb = new File(filepaththumb);
                        if (file.delete() && filethumb.delete()) {
                            Toast.makeText(mContext, "Image Deleted", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void imageInfo(int position) {
        Intent intent = new Intent(PhotoActivity.this, ImageInfoActivity.class);
        intent.putExtra(MainActivity.BUNDLE_IMAGE_DATA, mImagesContainer.get(position));
        startActivity(intent);
    }

    public void settings(View v) {
        Intent intent = new Intent(PhotoActivity.this, AppInfoActivity.class);
        startActivity(intent);
    }

    public static Bitmap getBitmapFromPath(String path) {
        File imgFile = new File(path);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            return myBitmap;
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
            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();

                if (city != null && !city.isEmpty()) {
                    returnString += city;
                }
                if (state != null && !state.isEmpty()) {
                    if (!returnString.isEmpty() && returnString.length() > 0) {
                        returnString += ", ";
                    }
                    returnString += state;
                }
                if (country != null && !country.isEmpty()) {
                    if (!returnString.isEmpty() && returnString.length() > 0) {
                        returnString += ", ";
                    }
                    returnString += country;
                }
            }
        }
        return returnString;
    }

    public String getAdapterTextString(ArrayList<String> imageItem) {
        String returnString = "";
        if (!imageItem.get(6).isEmpty() && imageItem.get(6).length() > 0) {
            returnString += imageItem.get(6) + "\n";
        } else {
            double latitude = Double.parseDouble(imageItem.get(2));
            double longitude = Double.parseDouble(imageItem.get(3));
            String newGeoCode = "";
            try {
                newGeoCode = getAddressGeoCoder(latitude, longitude);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!newGeoCode.isEmpty() && newGeoCode.length() > 0) {
                returnString += newGeoCode + "\n";
                ContentValues contentValues = new ContentValues();
                contentValues.put(PictosphereStorage.COLUMN_IMAGE_POSTS_ADDRESS, newGeoCode);
                String whereClause = PictosphereStorage.COLUMN_IMAGE_POSTS_ID + "=?";
                String[] whereArgs = {imageItem.get(0)};
                getContentResolver().update(PictosphereStorage.URI_IMAGE_POST, contentValues, whereClause, whereArgs);
            }
        }
        returnString += imageItem.get(9) + " " + imageItem.get(10);
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
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, bmOptions);
        return bitmap;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    class ListDataAdapter extends BaseAdapter {
        ViewHolder holder;

        @Override
        public int getCount() {
            return mImagesContainer.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemViewType(int position) {
            // current menu type
            //return position % 3;
            return 1;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getApplicationContext(), R.layout.images_list_style, null);
                holder = new ViewHolder(convertView);
            }
            holder = (ViewHolder) convertView.getTag();
            Bitmap tempBitmap = getBitmapFromPath(mImagesContainer.get(position).get(5));
            if (tempBitmap != null) {
                holder.mImageview.setImageBitmap(tempBitmap);
            }
            holder.mTextview.setText(mImagesContainer.get(position).get(11));
            return convertView;
        }

        class ViewHolder {
            TextView mTextview;
            ImageView mImageview;

            public ViewHolder(View view) {
                mImageview = view.findViewById(R.id.lblListImageItem);
                mTextview = view.findViewById(R.id.lblListItem);
                view.setTag(this);
            }
        }
    }
}
