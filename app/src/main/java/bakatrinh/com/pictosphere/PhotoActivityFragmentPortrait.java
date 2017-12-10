package bakatrinh.com.pictosphere;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

public class PhotoActivityFragmentPortrait extends Fragment {

    private PhotoActivity mContext;
    public PortraitUIHandler mHandler;
    SwipeMenuListView mSwipeMenuListView;

    public PhotoActivityFragmentPortrait() {
    }

    public static PhotoActivityFragmentPortrait newInstance() {
        PhotoActivityFragmentPortrait fragment = new PhotoActivityFragmentPortrait();
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
        View view = inflater.inflate(R.layout.fragment_photo_portrait, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHandler = new PortraitUIHandler();

        mSwipeMenuListView = getView().findViewById(R.id.image_list);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem subtractItem = new SwipeMenuItem(getContext());
                subtractItem.setBackground(new ColorDrawable(Color.parseColor("#d84329")));
                subtractItem.setWidth(dp2px(60));
                subtractItem.setIcon(R.drawable.minus_sm);
                menu.addMenuItem(subtractItem);

                SwipeMenuItem editItem = new SwipeMenuItem(getContext());
                editItem.setBackground(new ColorDrawable(Color.parseColor("#5292d1")));
                editItem.setWidth(dp2px(60));
                editItem.setIcon(R.drawable.pencil_sm);
                menu.addMenuItem(editItem);
            }
        };

        mSwipeMenuListView.setMenuCreator(creator);

        mSwipeMenuListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                String id;
                String filePath;
                String filePathThumb;
                switch (index) {
                    case 0:
                        id = mContext.mImagesContainer.get(position).get(0);
                        filePath = mContext.mImagesContainer.get(position).get(4);
                        filePathThumb = mContext.mImagesContainer.get(position).get(5);
                        mContext.deleteAPicture(id, filePath, filePathThumb);
                        break;

                    case 1:
                        mContext.editImage(position);
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });

        mSwipeMenuListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mContext.imageInfo(i);
            }
        });

        mSwipeMenuListView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
        mSwipeMenuListView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        mContext.rebuildImagesArray();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    class PortraitUIHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PhotoActivity.UPDATE_IMAGES:
                    mSwipeMenuListView.setAdapter(mContext.mListDataAdapter);
                    break;
            }
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
