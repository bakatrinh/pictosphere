package bakatrinh.com.pictosphere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    static final int RC_SIGN_IN = 200;
    static final String TAG = "PICTOSPHERE_DEBUG";
    static final String BUNDLE_GOOGLE_EMAIL = "google_email";
    static final String BUNDLE_IMAGE_DATA = "image_data";
    static final String BUNDLE_LATITUDE = "latitude";
    static final String BUNDLE_LONGITUDE = "longitude";
    static final String BUNDLE_FINISH_ACTIVITY = "finish_activity";
    private String mGoogleEmail = "";
    GoogleSignInClient mGoogleSignInClient;
    BroadcastReceiver finishActivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleSignInClient.signOut();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, MainActivity.RC_SIGN_IN);
            }
        });

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            account.getEmail();
            mGoogleEmail = account.getEmail();
            Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
            intent.putExtra(MainActivity.BUNDLE_GOOGLE_EMAIL, mGoogleEmail);
            startActivity(intent);
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        finishActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals(MainActivity.BUNDLE_FINISH_ACTIVITY)) {
                    if (mGoogleSignInClient != null) {
                        mGoogleSignInClient.signOut();
                    }
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            account.getEmail();
            mGoogleEmail = account.getEmail();
            Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
            intent.putExtra(MainActivity.BUNDLE_GOOGLE_EMAIL, mGoogleEmail);

            startActivity(intent);
            finish();
        } catch (ApiException e) {
            e.printStackTrace();
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_LONG).show();
        }
    }
}
