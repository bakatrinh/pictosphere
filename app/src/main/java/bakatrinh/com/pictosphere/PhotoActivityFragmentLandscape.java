package bakatrinh.com.pictosphere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PhotoActivityFragmentLandscape extends Fragment implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        OnMapReadyCallback {
    PhotoActivity mContext;
    LandscapeUIHandler mHandler;
    GoogleMap mMap;
    LatLng mCurrentLocation;

    public PhotoActivityFragmentLandscape() {
    }

    public static PhotoActivityFragmentLandscape newInstance() {
        PhotoActivityFragmentLandscape fragment = new PhotoActivityFragmentLandscape();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void setCurrentLocation(LatLng location) {
        mCurrentLocation = location;
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
        mCurrentLocation = null;
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
        Message msg = mHandler.obtainMessage(PhotoActivity.UPDATE_MAP_INITIAL);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                final int marker_id = (int) marker.getTag();
                mContext.imageInfo(marker_id);
            }
        });
        mContext.enableMyLocation();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (mCurrentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(4));
            return true;
        }
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
                case PhotoActivity.UPDATE_MAP:
                    mMap.clear();
                    if (mContext.mImagesContainer != null && mContext.mImagesContainer.size() > 0) {
                        int i = 0;
                        for (ArrayList<String> imageData : mContext.mImagesContainer) {
                            LatLng tempLatLong = new LatLng(Double.parseDouble(imageData.get(2)), Double.parseDouble(imageData.get(3)));
                            String snippet = imageData.get(9) + " " + imageData.get(10);
                            if (!imageData.get(7).isEmpty()) {
                                snippet += "\n" + imageData.get(7);
                            }
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                                   .position(tempLatLong)
                                                                   .title(imageData.get(6))
                                                                   .snippet(snippet)
                                                                   .icon(BitmapDescriptorFactory.defaultMarker(i * 360 / mContext.mImagesContainer.size())));
                            marker.setTag(i);
                            i++;
                        }
                    }
                    break;

                case PhotoActivity.UPDATE_MAP_INITIAL:
                    mMap.clear();
                    if (mCurrentLocation != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(3));
                    }
                    if (mContext.mImagesContainer != null && mContext.mImagesContainer.size() > 0) {
                        int i = 0;
                        for (ArrayList<String> imageData : mContext.mImagesContainer) {
                            LatLng tempLatLong = new LatLng(Double.parseDouble(imageData.get(2)), Double.parseDouble(imageData.get(3)));
                            String snippet = imageData.get(9) + " " + imageData.get(10);
                            if (!imageData.get(7).isEmpty()) {
                                snippet += "\n" + imageData.get(7);
                            }
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                                   .position(tempLatLong)
                                                                   .title(imageData.get(6))
                                                                   .snippet(snippet)
                                                                   .icon(BitmapDescriptorFactory.defaultMarker(i * 360 / mContext.mImagesContainer.size())));
                            marker.setTag(i);
                            i++;
                        }
                    }
                    break;
            }
        }
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mWindow;
        private final View mContents;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            final int marker_id = (int) marker.getTag();

            Bitmap tempBitmap = PhotoActivity.getBitmapFromPath(mContext.mImagesContainer.get(marker_id).get(5));
            if (tempBitmap != null) {
                ImageView tempImageVIew = view.findViewById(R.id.badge);
                tempImageVIew.setImageBitmap(tempBitmap);
            }

            String title = marker.getTitle();
            TextView titleUi = view.findViewById(R.id.title);
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = view.findViewById(R.id.snippet);
            if (snippet != null && snippet.length() > 0) {
                String[] snippetArray = marker.getSnippet().split("\n");
                SpannableString snippetText = new SpannableString(snippetArray[0]);
                snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 0, snippetArray[0].length(), 0);
                snippetUi.setText(snippetText);
                if (snippetArray.length == 2) {
                    snippetUi.append("\n");
                    SpannableString snippetText2 = new SpannableString(snippetArray[1]);
                    snippetText2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, snippetArray[1].length(), 0);
                    snippetUi.append(snippetText2);
                }
            } else {
                snippetUi.setText("");
            }
        }
    }
}