package bakatrinh.com.pictosphere;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "PICTOSPHERE_DEBUG";
    static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    boolean mPermissionDenied;
    MainActivityFragmentPortrait fragmentPortrait;
    MainActivityFragmentLandscape fragmentLandscape;
    FrameLayout fragmentContainerPortrait;
    FrameLayout fragmentContainerLandscape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fragmentContainerPortrait = findViewById(R.id.main_fragment_container_portrait);
        fragmentContainerLandscape = findViewById(R.id.main_fragment_container_landscape);

        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
        fragmentPortrait = new MainActivityFragmentPortrait();
        manager.beginTransaction().add(R.id.main_fragment_container_portrait, fragmentPortrait).commit();
        fragmentLandscape = new MainActivityFragmentLandscape();
        manager.beginTransaction().add(R.id.main_fragment_container_landscape, fragmentLandscape).commit();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fragmentContainerLandscape.setVisibility(View.GONE);
            fragmentContainerPortrait.setVisibility(View.VISIBLE);
        } else {
            fragmentContainerPortrait.setVisibility(View.GONE);
            fragmentContainerLandscape.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    public void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
