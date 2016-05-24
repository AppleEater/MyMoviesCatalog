package com.example.uaharoni.mymoviescatalog.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.uaharoni.mymoviescatalog.Entities.Movie;

/**
 * Created by udi on 16/05/2016.
 */
public class MoviesDB extends SQLiteOpenHelper implements BaseColumns{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "movies.db";

    private static final String TBL_NAME_MOVIES = "movies";

    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_TITLE = "title";
    private static final String COL_PLOT = "plot";
    private static final String COL_IMDBID = "imdb_Id";
    private static final String COL_URL = "url";
    private static final String COL_RATING = "rating";
    // Logcat tag
    private static final String LOG = "moviesDbHelper";

    public MoviesDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String TEXT_TYPE = " TEXT";
        String INTEGER_TYPE = " INTEGER";
        String DATETIME_TYPE = " DATETIME";
        String COL_NULLABLE = null;
        String PRIMARY_KEY = " PRIMARY KEY AUTOINCREMENT";

        String sqlCreateTable = "CREATE TABLE " +
                    TBL_NAME_MOVIES + "(" +
                COL_ID + INTEGER_TYPE + PRIMARY_KEY +","
                + COL_TITLE + TEXT_TYPE + ","
                + COL_PLOT + TEXT_TYPE + ","
                + COL_IMDBID + TEXT_TYPE + ","
                + COL_URL + TEXT_TYPE + ","
                + COL_RATING + INTEGER_TYPE
                 + ")";
        db.execSQL(sqlCreateTable);
    }
    public void deleteDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        String sqlFormat = String.format("DROP TABLE IF EXISTS %s", TBL_NAME_MOVIES);
        db.execSQL(sqlFormat);

        // Create empty tables in the db
        onCreate(db);
    }
    public boolean deleteMovie(long rowId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TBL_NAME_MOVIES,COL_ID + "=" + rowId, null);
            return true;
        } catch (Exception dbException){
            Log.e("SQLiteDB:",dbException.getMessage());
            return false;
        }
    }
    public Cursor getAllMovieTitlesCursor() {
        // Returns movie name and id to display in the MainActivity
        Cursor allMovieTitles = null;
        // Gets the db in read mode
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            allMovieTitles = db.rawQuery(
                    "SELECT " + COL_TITLE + "," + COL_ID  + "," + COL_RATING + " from " + TBL_NAME_MOVIES + " order by " + COL_TITLE
                    , null);
        } catch (Exception dbException) {
            Log.e("SQLiteDB:",dbException.getMessage());
        }
        return allMovieTitles;
    }
    public Cursor getAllMovieTitlesCursorExtended(String column) {
        // Returns movie name and id to display in the MainActivity
        Cursor allMovieTitles = null;
        // Gets the db in read mode
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            allMovieTitles = db.rawQuery(
                    "SELECT " + COL_TITLE + "," + COL_ID  + "," + COL_RATING + " FROM " + TBL_NAME_MOVIES + " ORDER BY " + column
                    , null);
        } catch (Exception dbException) {
            Log.e("SQLiteDB:",dbException.getMessage());
        }
        return allMovieTitles;
    }
    public Movie getMovieById(long id) {
        Movie movie = null;
        // Gets the db in read mode
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor singleRow = db.rawQuery(
                "SELECT * from " + TBL_NAME_MOVIES + " WHERE " + COL_ID + "=" + id
                , null
                 );
        // making sure there is at least one row returned
        if (singleRow.getCount()>0) {
            int id_index = singleRow.getColumnIndex(COL_ID);
            int id_title = singleRow.getColumnIndex(COL_TITLE);
            int id_plot = singleRow.getColumnIndex(COL_PLOT);
            int id_imdbid = singleRow.getColumnIndex(COL_IMDBID);
            int id_url = singleRow.getColumnIndex(COL_URL);
            int id_rating = singleRow.getColumnIndex(COL_RATING);
            singleRow.moveToFirst();
            long movieId = singleRow.getLong(id_index);
            String movieTitle = singleRow.getString(id_title);
            String moviePlot = singleRow.getString(id_plot);
            String movieImdbId = singleRow.getString(id_imdbid);
            String movieCoverUrl = singleRow.getString(id_url);
            int movieRating = singleRow.getInt(id_rating);
            movie = new Movie(movieTitle,moviePlot,movieImdbId,movieCoverUrl,movieId,movieRating);
        }
        db.close();
        return movie;
    }
    public boolean insertMovie(Movie movie) {
        boolean success = true;
        // Create a new map of values, where column names are the keys
        ContentValues updatedValues = extractMovie(movie);

        // Open the db for writing
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            long resultId = db.insert(TBL_NAME_MOVIES, null, updatedValues);
        } catch (Exception dbException) {
            //TODO: Implement getLocalizedMessage()
            Log.e("SQLiteDB:",dbException.getMessage());
            success = false;
        }

        return success;
    }
    public boolean updateMovie(Movie movie) {
        // Obtaining the movieId
        long movieId = movie.getId();
        boolean resultFlag = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues updatedValues = extractMovie(movie);

           // resultFlag = db.update(TBL_NAME_MOVIES, updatedValues, COL_ID + "=", new String[]{String.valueOf(movieId)}) > 0; // checks if the resultFlag is not 0
            resultFlag =  db.update(TBL_NAME_MOVIES,updatedValues,COL_ID + "=?", new String[]{String.valueOf(movieId)})>0;
        } catch (Exception dbException) {
            Log.e("SQLiteDB:",dbException.getMessage());
            resultFlag = false;
        }
        return resultFlag;
    }
    private ContentValues extractMovie(Movie movie){
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, movie.getTitle());
        values.put(COL_PLOT, movie.getPlot());
        values.put(COL_URL, movie.getCoverImageURL().replace(" ","%20"));
        values.put(COL_IMDBID,movie.getImdbId());
        values.put(COL_RATING,movie.getRating());
        //values.put(COL_ID,movie.getId());  //We don't need the COL_ID, as we use this function only to insert/update movies, when we either have the id, or don't care for it.


        return values;
    }

        @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
