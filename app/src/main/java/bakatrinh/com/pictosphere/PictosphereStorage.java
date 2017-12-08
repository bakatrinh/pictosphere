package bakatrinh.com.pictosphere;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.content.ContentUris;
import java.util.HashMap;

public class PictosphereStorage extends ContentProvider {
    private static final int DB_VERSION = 3;
    private static HashMap<String, String> DEVICES_PROJECT_MAP;
    private static SQLiteDatabase db;
    private PictosphereStorageHelper mOpenHelper;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final String PROVIDER = "bakatrinh.com.pictosphere";
    static final String DATABASE_NAME = "pictosphere";

    static final String PICTOSPHERE_IMAGE_POST = "pictosphere_retrieve_image";
    static final String URL_IMAGE_POST = "content://" + PROVIDER + "/" + PICTOSPHERE_IMAGE_POST;
    static final Uri URI_IMAGE_POST = Uri.parse(URL_IMAGE_POST);

    static final String PICTOSPHERE_IMAGE_POSTS_TABLE = "image_posts";
    static final String COLUMN_IMAGE_POSTS_ID = "_ID";
    static final String COLUMN_IMAGE_POSTS_USER_ID = "user_id";
    static final String COLUMN_IMAGE_POSTS_LONGITUDE = "longitude";
    static final String COLUMN_IMAGE_POSTS_LATITUDE = "latitude";
    static final String COLUMN_IMAGE_POSTS_IMAGE = "image_path";
    static final String COLUMN_IMAGE_POSTS_ADDRESS = "address";
    static final String COLUMN_IMAGE_POSTS_DATE = "date_created";

    private static final String CREATE_DB_TABLE = "CREATE TABLE " + PICTOSPHERE_IMAGE_POSTS_TABLE + "( "+COLUMN_IMAGE_POSTS_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+COLUMN_IMAGE_POSTS_USER_ID+" TEXT NOT NULL, "+COLUMN_IMAGE_POSTS_LONGITUDE+" TEXT NOT NULL, "+COLUMN_IMAGE_POSTS_LATITUDE+" TEXT NOT NULL, "+COLUMN_IMAGE_POSTS_IMAGE+" TEXT NOT NULL, "+COLUMN_IMAGE_POSTS_ADDRESS+" TEXT NOT NULL, "+COLUMN_IMAGE_POSTS_DATE+" DATETIME DEFAULT (datetime('now','localtime')))";

    private Context mContext;

    static {
        sUriMatcher.addURI(PROVIDER, PICTOSPHERE_IMAGE_POST, 1);
        sUriMatcher.addURI(PROVIDER, PICTOSPHERE_IMAGE_POST + "/#", 2);
    }

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mOpenHelper = new PictosphereStorageHelper(mContext, DB_VERSION);
        return true;
    }

    public PictosphereStorage() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        db = mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case 1:
                count = db.delete(PICTOSPHERE_IMAGE_POSTS_TABLE, selection, selectionArgs);
                break;

            case 2:
                count = db.delete(PICTOSPHERE_IMAGE_POSTS_TABLE, selection, selectionArgs);
                break;
        }
        notifyChange(uri);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db = mOpenHelper.getWritableDatabase();
        long row = db.insert(PICTOSPHERE_IMAGE_POSTS_TABLE, "", values);
        if (row > 0) {
            Uri _uri = ContentUris.withAppendedId(uri, row);
            notifyChange(_uri);
            return _uri;
        }
        throw new SQLException("Failed to add a record into" + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        sqLiteQueryBuilder.setProjectionMap(DEVICES_PROJECT_MAP);
        switch (sUriMatcher.match(uri)) {
            case 1:
                sqLiteQueryBuilder.setTables(PICTOSPHERE_IMAGE_POSTS_TABLE);
                break;

            case 2:
                sqLiteQueryBuilder.setTables(PICTOSPHERE_IMAGE_POSTS_TABLE);
                sqLiteQueryBuilder.appendWhere(uri.getLastPathSegment());
                break;
        }
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = "date_created DESC";
        }
        db = mOpenHelper.getWritableDatabase();
        Cursor c = sqLiteQueryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void notifyChange(Uri uri) {
        ContentResolver resolver = mContext.getContentResolver();
        if (resolver != null) {
            resolver.notifyChange(uri, null);
        }
    }

    protected static final class PictosphereStorageHelper extends SQLiteOpenHelper {

        PictosphereStorageHelper(Context context, int currentVersion) {
            super(context, DATABASE_NAME, null, currentVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            if (oldVersion < newVersion) {
                Log.d(MainActivity.TAG, "Upgrading " + PICTOSPHERE_IMAGE_POSTS_TABLE + " old version (" + oldVersion + ") to new version (" + newVersion + ")");
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PICTOSPHERE_IMAGE_POSTS_TABLE);
                onCreate((sqLiteDatabase));
            }
        }
    }
}
