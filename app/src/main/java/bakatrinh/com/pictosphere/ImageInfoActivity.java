package bakatrinh.com.pictosphere;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageInfoActivity extends AppCompatActivity {
    static final int UPDATE_DATA = 1;

    public UIHandler mHandler;

    String id;
    String filepath;
    String filepathThumb;
    String formatted_info;
    String message;
    Bitmap imageBitmap;
    ImageView imageView;
    int imageViewWidth;
    int imageViewHeight;
    ArrayList<String> imageData;
    BroadcastReceiver finishActivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = findViewById(R.id.image_bitmap);
        mHandler = new UIHandler();
        imageBitmap = null;
        imageData = (ArrayList<String>) getIntent().getSerializableExtra(MainActivity.BUNDLE_IMAGE_DATA);

        finishActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals(MainActivity.BUNDLE_FINISH_ACTIVITY)) {
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(finishActivityReceiver, new IntentFilter(MainActivity.BUNDLE_FINISH_ACTIVITY));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(finishActivityReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pictosphere_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                double tempLatitude = 0.0;
                double tempLongitude = 0.0;
                LatLng temp = getCurrentLocation();
                if (temp != null) {
                    tempLatitude = temp.latitude;
                    tempLongitude = temp.longitude;
                }
                intent = new Intent(ImageInfoActivity.this, AppInfoActivity.class);
                intent.putExtra(MainActivity.BUNDLE_LATITUDE, tempLatitude);
                intent.putExtra(MainActivity.BUNDLE_LONGITUDE, tempLongitude);
                startActivity(intent);
                return true;

            case R.id.action_log_off:
                Intent closeActivitySignal = new Intent(MainActivity.BUNDLE_FINISH_ACTIVITY);
                sendBroadcast(closeActivitySignal);

                intent = new Intent(ImageInfoActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            imageViewWidth = imageView.getWidth();
            imageViewHeight = imageView.getHeight();
            if (imageData != null && imageViewHeight > 0 && imageViewHeight > 0) {
                Runnable renderBitmapTask = new Runnable() {
                    @Override
                    public void run() {
                        Bitmap tempBitmap = PhotoActivity.resizeImage(imageData.get(4), imageViewWidth, imageViewHeight);
                        id = imageData.get(0);
                        filepath = imageData.get(4);
                        filepathThumb = imageData.get(5);
                        message = imageData.get(7);
                        formatted_info = imageData.get(11);
                        imageBitmap = tempBitmap;
                        Message msg = mHandler.obtainMessage(ImageInfoActivity.UPDATE_DATA);
                        mHandler.sendMessage(msg);
                    }
                };
                new Thread(renderBitmapTask).start();
            } else {
                finish();
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
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

    public void goBack(View view) {
        finish();
    }

    public void deletePicture(View v) {
        String whereClause = PictosphereStorage.COLUMN_IMAGE_POSTS_ID + "=?";
        String[] whereArgs = {id};
        getContentResolver().delete(PictosphereStorage.URI_IMAGE_POST, whereClause, whereArgs);
        File file = new File(filepath);
        File fileThumb = new File(filepathThumb);
        if (file.delete() && fileThumb.delete()) {
            Toast.makeText(this, "File Deleted", Toast.LENGTH_LONG).show();
        }
        finish();
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ImageInfoActivity.UPDATE_DATA:
                    if (imageBitmap != null) {
                        imageView.setImageBitmap(imageBitmap);
                    }
                    TextView textViewImageInfo = findViewById(R.id.image_info);
                    textViewImageInfo.setText(formatted_info);
                    TextView textView = findViewById(R.id.image_message);
                    View divider = findViewById(R.id.divider);
                    if (message.isEmpty() || message.length() <= 0) {
                        textView.setVisibility(View.GONE);
                        divider.setVisibility(View.GONE);
                    } else {
                        textView.setText(message);
                        textView.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }
}
