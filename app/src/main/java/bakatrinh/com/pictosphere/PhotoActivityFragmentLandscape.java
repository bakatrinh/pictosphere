package bakatrinh.com.pictosphere;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class PhotoActivityFragmentLandscape extends Fragment implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        OnMapReadyCallback {
    PhotoActivity mContext;
    LandscapeUIHandler mHandler;
    GoogleMap mMap;

    static final int UPDATE_MAP = 1;

    public PhotoActivityFragmentLandscape() {
    }

    public static PhotoActivityFragmentLandscape newInstance() {
        PhotoActivityFragmentLandscape fragment = new PhotoActivityFragmentLandscape();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mContext = (PhotoActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must be PhotoActivity Class");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_landscape, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHandler = new LandscapeUIHandler();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        SupportMapFragment supportMapFragment = SupportMapFragment.newInstance();
        fm.beginTransaction().replace(R.id.map, supportMapFragment).commit();
        supportMapFragment.getMapAsync(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void redrawGoogleMaps() {
        Message msg = mHandler.obtainMessage(PhotoActivityFragmentLandscape.UPDATE_MAP);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mContext.enableMyLocation();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    class LandscapeUIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            }
        }
    }
}