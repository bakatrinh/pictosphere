package bakatrinh.com.pictosphere;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);
        imageView = findViewById(R.id.image_bitmap);
        mHandler = new UIHandler();
        imageBitmap = null;
        imageData = (ArrayList<String>) getIntent().getSerializableExtra(MainActivity.BUNDLE_IMAGE_DATA);
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
                    }
                    else {
                        textView.setText(message);
                        textView.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }
}
