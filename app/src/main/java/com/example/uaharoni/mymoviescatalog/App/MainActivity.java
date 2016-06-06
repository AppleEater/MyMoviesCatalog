package com.example.uaharoni.mymoviescatalog.App;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uaharoni.mymoviescatalog.Entities.Movie;
import com.example.uaharoni.mymoviescatalog.Entities.OMDB_Web;
import com.example.uaharoni.mymoviescatalog.Helpers.MoviesDB;
import com.example.uaharoni.mymoviescatalog.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends MenuActivity implements View.OnClickListener, AdapterView.OnItemClickListener,DialogInterface.OnClickListener {
    private MoviesDB dbHelper;
    private ImageButton btnAddMovie;
    private ListView movieTitlesList;
    private View listItem;
    private ImageView coverImage;
    private ProgressBar progressBar;
    private TextView txtNotifyDummy;

    public static SimpleCursorAdapter movieTitlesListCursorAdapter;
    public static final int MENU_OPTION_DELETE_CATALOG = 2;
    public final static int MENU_OPTION_SHARE = 3;
    public static final int MENU_OPTION_GENERATE_DUMMY = 4;
    public static final int APP_EXIT_RETURNCODE = 2;
    public static final int MOVIE_ADDED_RETURNCODE = 3;
    private AlertDialog deleteCatalogDialog, addMovieDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeEntities();
        initializeListeners();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        GetItemsFromDB bgTask = new GetItemsFromDB();
        bgTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == Intent.FILL_IN_ACTION) || (requestCode == Intent.FILL_IN_DATA)) {
            if (resultCode == APP_EXIT_RETURNCODE) {
                this.finish();
            }
        }
        progressBar.setVisibility(View.INVISIBLE);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initializeEntities() {

        dbHelper = new MoviesDB(getApplicationContext());
        btnAddMovie = ((ImageButton) findViewById(R.id.btnAddMovie_Main));
        movieTitlesList = ((ListView) findViewById(R.id.listMoviesMain));
        coverImage = (ImageView) findViewById(R.id.imageView_main_moviesList);
        progressBar = (ProgressBar) findViewById(R.id.progressBar_Main);
        txtNotifyDummy = (TextView)findViewById(R.id.txt_please_wait_dummyData);
        listItem = findViewById(R.id.list_item_layout);

        // Create the SimpleCursorAdapter
        Log.d("onPostExecute","Configuring the ListViewAdapter");
        try {
            String[] listOfFields = new String[]{MoviesDB.COL_TITLE,MoviesDB.COL_URL,MoviesDB.COL_RATING};
            int[] ViewIds = new int[]{R.id.txtMovieListItem_movieTitle,R.id.imageView_main_moviesList,R.id.list_item_layout};
            movieTitlesListCursorAdapter = new SimpleCursorAdapter(
                    getApplicationContext()
                    , R.layout.main_movie_list_item
                    , dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE)
                    , listOfFields
                    , ViewIds
                    , 0);
        } catch (Exception e){
            Log.e("onPostExecute", "Error creating the adapter. " + e.getMessage());
        }
        Log.d("onPostExecute","Binding the adapter");
        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex==cursor.getColumnIndex(MoviesDB.COL_URL)){
                    String imgUrl = cursor.getString(columnIndex);
                    //Log.d("onPostExecute", "Loading image url " + imgUrl);
                    DownloadImageTask getImageBitmap = new DownloadImageTask(view);
                    getImageBitmap.execute(imgUrl);
                    return true;
                }
                if(columnIndex==cursor.getColumnIndex(MoviesDB.COL_RATING)){
                    double movieRating = cursor.getDouble(columnIndex);
                    if(movieRating>=2.5){
                        view.setBackgroundColor(Color.GREEN);
                    } else {
                        view.setBackgroundColor(Color.RED);
                    }
                    return true;
                }
                return false;
            }
        };
        Log.d("onPostExecute","Connecting the adapter to the ViewBinder");
        movieTitlesListCursorAdapter.setViewBinder(viewBinder);

        // Connecting the list to the adapter
        movieTitlesList.setAdapter(movieTitlesListCursorAdapter);

    }

    private void initializeListeners() {
        // find  AddButton and connect to click event
        btnAddMovie.setOnClickListener(this);

        // Connect the list item to a click listener
        movieTitlesList.setOnItemClickListener(this);

        registerForContextMenu(movieTitlesList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We use the super method defined in menu_shared in the Menu Activity for general activities.
        //Here we specify specific activity-related options
        menu.add(1, MENU_OPTION_DELETE_CATALOG, 1, R.string.menu_delete_all_movies);
        menu.add(1,MENU_OPTION_GENERATE_DUMMY,2,R.string.menu_option_generate_dummy_data);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPTION_DELETE_CATALOG:
                // dialogBox to confirm catalog deletion
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setIcon(android.R.drawable.ic_delete);
                dialogBuilder.setNegativeButton(R.string.dialog_button_delete_catalog_cancel, this);

                dialogBuilder.setPositiveButton(R.string.dialog_button_delete_complete_catalog, this);
                dialogBuilder.setMessage(R.string.dialog_message_delete_catalog_warning);
                dialogBuilder.setTitle(R.string.dialog_header_delete_all_catalog);
                //dialogBuilder.show();   // this method does create and show

                deleteCatalogDialog = dialogBuilder.create();
                deleteCatalogDialog.show();


                /* Old method, which updates the AlertDialog instance directly, instead of through the builder

                AlertDialog deleteCatalogDialog = new AlertDialog.Builder(this).create();
                deleteCatalogDialog.setIcon(android.R.drawable.ic_delete);
                deleteCatalogDialog.setTitle(getResources().getString(R.string.dialog_header_delete_all_catalog));
                deleteCatalogDialog.setMessage(getResources().getString(R.string.dialog_message_delete_catalog_warning));
                deleteCatalogDialog.setButton(deleteCatalogDialog.BUTTON_POSITIVE,getResources().getString(R.string.dialog_button_delete_complete_catalog), this);

                deleteCatalogDialog.setButton(deleteCatalogDialog.BUTTON_NEGATIVE,getResources().getString(R.string.dialog_button_delete_catalog_cancel), this);
                deleteCatalogDialog.setCancelable(true);
                deleteCatalogDialog.show();
                    */
                break;
            case MENU_OPTION_GENERATE_DUMMY:
                insertDummyData();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        // Create the AddMovie method dialog o choose movie adding method

        addMovieDialog = new AlertDialog.Builder(this).create();
        addMovieDialog.setIcon(android.R.drawable.ic_input_add);
        addMovieDialog.setTitle(getResources().getString(R.string.dialog_add_movie));
        addMovieDialog.setMessage(getResources().getString(R.string.dialog_add_movie_message));
        addMovieDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.internet_add_movie), this);
        addMovieDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.manual_add_movie), this);
        addMovieDialog.setCancelable(true);
        addMovieDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        // Called when clicking a button in the AlertDialog of AddMovie
        if ((deleteCatalogDialog != null) && deleteCatalogDialog.equals(dialog)) {
            switch (whichButton) {
                case AlertDialog.BUTTON_POSITIVE:
                    dbHelper.deleteDB();
                    MainActivity.movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE));
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.toast_message_catalog_deleted), Toast.LENGTH_LONG).show();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    dialog.cancel();
                    break;
                default:
                    break;
            }
        }
        if (addMovieDialog != null) {
            if (dialog.equals(addMovieDialog)) {
                Intent addMovieActivity;
                switch (whichButton) {
                    case AlertDialog.BUTTON_POSITIVE:
                        //Open the WebSearchActivity
                        addMovieActivity = new Intent(this, WebSearchActivity.class);
                        startActivityForResult(addMovieActivity, Intent.FILL_IN_DATA);
                        break;
                    case AlertDialog.BUTTON_NEGATIVE:
                        //Open the NewEditMovieActivity with no content
                        addMovieActivity = new Intent(this, NewEditMovieActivity.class);
                        addMovieActivity.setAction(Intent.ACTION_INSERT);
                        //  addMovieActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivityForResult(addMovieActivity, Intent.FILL_IN_ACTION);
                        break;
                    default:
                        break;
                }
                //startActivity(addMovieActivity);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Clicking an item in the list should launch the NewEditActivity with the movie details
        setEditActivity(position);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Get extra info about list item that was long-pressed
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case MENU_CONTEXT_EDIT:
                // Editing an item
                Log.i("onContextItemSelected","Clicked editing item " + menuInfo.position);
                setEditActivity(menuInfo.position);
                return true;
            case MENU_CONTEXT_DELETE:
                // Deleting an item
                deleteItem(menuInfo.position);
                return true;
            case MENU_CONTEXT_SHARE:
                Log.i("onContextItemSelected","Clicked sharing movie item " + menuInfo.position);
                shareMovieItem(menuInfo.position);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return false;
    }

    private void setEditActivity(int position) {
        //Toast.makeText(MainActivity.this, "You clicked position" + position , Toast.LENGTH_SHORT).show();
        Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(position);
        long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
        Movie selectedMovie = dbHelper.getMovieById(movieId);
        Intent editMovieActivity = new Intent(getApplicationContext(), NewEditMovieActivity.class);
        //editMovieActivity.addCategory(Intent.CATEGORY_HOME);
        //editMovieActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        editMovieActivity.putExtra("Movie", selectedMovie);
        editMovieActivity.setAction(Intent.ACTION_EDIT);
        startActivityForResult(editMovieActivity, Intent.FILL_IN_ACTION);
        //startActivity(editMovieActivity);
    }

    private void deleteItem(int position) {
        Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(position);
        long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
        if (dbHelper.deleteMovie(movieId)) {
            Toast.makeText(this, selectedItem.getString(selectedItem.getColumnIndex(MoviesDB.COL_TITLE)) + " " + getResources().getString(R.string.toast_message_deleted_sucessfully), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, selectedItem.getString(selectedItem.getColumnIndex(MoviesDB.COL_TITLE)) + " " + getResources().getString(R.string.toast_message_movie_delete_failed), Toast.LENGTH_SHORT).show();
        }
        //movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursor());
        movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE));

    }

    public class GetItemsFromDB extends AsyncTask<Void, Integer, Cursor> {

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor movieTitlesCursor = dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE);
            publishProgress(movieTitlesCursor.getCount());

            return movieTitlesCursor;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Cursor returnCursor) {
            super.onPostExecute(returnCursor);


            Log.i("onPostExecute","Loading new cursor to the adapter");
            //movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursor());
            movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE));
            Log.d("onPostExecute","Hiding the progressBar");
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }
    }
    private void shareMovieItem(int itemPosition) {
        Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(itemPosition);
        long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
        Movie selectedMovie = dbHelper.getMovieById(movieId);
        String txtTShare = selectedMovie.getTitle().trim();

        if (selectedMovie.getImdbId() != null) {
            txtTShare += "\nhttp://www.imdb.com/title/" + selectedMovie.getImdbId() + "/";
        } else {
            txtTShare += "\nhttp://www.imdb.com/find?q=" + selectedMovie.getTitle().trim().replace(" ", "%20") + "&s=movie#tt";
        }
        txtTShare += "\nInformation courtesy of IMDb (http://www.imdb.com). Used with permission.";


        Intent shareMovie = new Intent(Intent.ACTION_SEND);
        shareMovie.setType("text/plain");
        // Add data to the intent, the receiving app will decide what to do with it.
        shareMovie.putExtra(Intent.EXTRA_SUBJECT, "My movie info");
        shareMovie.putExtra(Intent.EXTRA_TEXT, txtTShare);
        startActivity(Intent.createChooser(shareMovie, "Share movie info"));

    }

    private void savePreferences(String value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("key", value);
        edit.apply();   // using instead of commit, to perform asynchronous saving
        // To call the sharedpreferences
        String val = sp.getString("key", "");
        if(val.equals("")) {
            Log.i("savePreferences", "no value");
        }
    }


    private void insertDummyData() {
            getFromNet fetchMovieInfo = new getFromNet();
            fetchMovieInfo.execute();
    }
    public class getFromNet extends AsyncTask<Void, Integer, Void> {

        int numDummyMovies=30;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("onPreExecute","Displaying progressBar and notification");
            txtNotifyDummy.setVisibility(TextView.VISIBLE);
            progressBar.setMax(numDummyMovies);
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress((values[0]*100/numDummyMovies));
            Log.d("onProgress","Updated progress bar with " + values[0]*100/numDummyMovies);
            movieTitlesListCursorAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d("onPostExecute","Hiding the progressBar and notification");
            txtNotifyDummy.setVisibility(TextView.INVISIBLE);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            Log.d("onPostExecute","Updating the adapter for dataSetChanged");
            //MainActivity.movieTitlesListCursorAdapter.notifyDataSetChanged();
            MainActivity.movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursorExtended(MoviesDB.COL_TITLE));
        }

        @Override
        protected Void doInBackground(Void... params) {
            Movie newMovie;

            int numtries = 0;
            while(dbHelper.getAllMovieTitlesCursor().getCount()<numDummyMovies) {
                numtries++;

                String dummyID = "tt" + (int) (Math.random() * 10000000);
                String inputLine, resultResponse = "";

                try {
                    URL omdb_url = new URL("http://www.omdbapi.com/?i=" + dummyID + "&r=json&type=movie");
                    Log.d("insertDummyData", "Checking url:" + omdb_url.toString());
                    HttpURLConnection connection = (HttpURLConnection) omdb_url.openConnection();
                    BufferedReader reader;
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((inputLine = reader.readLine()) != null) {
                        resultResponse += inputLine;
                    }
                    JSONObject jsonResponse = new JSONObject(resultResponse);
                    //Log.d("insertDummyData", "JSON object response: " + jsonResponse.getString("Response"));
                    if (jsonResponse.getString("Response").equals("True")
                            && !(jsonResponse.getString(OMDB_Web.JSON_PLOT).equals("N/A"))
                        &&  !(jsonResponse.getString(OMDB_Web.JSON_TITLE).startsWith("Episode"))
                            && !(jsonResponse.getString(OMDB_Web.JSON_POSTER).equals("N/A"))
                            &&  !(jsonResponse.getString(OMDB_Web.JSON_TITLE).startsWith("#"))
                            )
                    {

                        Log.i("insertDummyData", "Loading new movie");
                        newMovie = new Movie(
                                jsonResponse.getString(OMDB_Web.JSON_TITLE)
                                , jsonResponse.getString(OMDB_Web.JSON_PLOT)
                                , jsonResponse.getString(OMDB_Web.JSON_IMDBID)
                                , jsonResponse.getString(OMDB_Web.JSON_POSTER)
                                , 0
                                , 0
                        );
                        if (jsonResponse.getString(OMDB_Web.JSON_RATING).equals("N/A")) {
                            //Log.d("dummyData", "rating unavailable. Faking...");
                            newMovie.setRating( (Math.random() * 10));
                        } else {
                            Log.d("insertDummyData", "Updating movie " + newMovie.getTitle() + " with rating " + jsonResponse.getDouble(OMDB_Web.JSON_RATING));
                            newMovie.setRating(jsonResponse.getDouble(OMDB_Web.JSON_RATING));
                        }

                        boolean movieInsertResult = dbHelper.insertMovie(newMovie);
                        Log.i("dummyData","Loaded " + dbHelper.getAllMovieTitlesCursor().getCount() + " movies after " + numtries + " tries");
                        publishProgress(dbHelper.getAllMovieTitlesCursor().getCount());

                    }
                } catch (MalformedURLException ue) {
                    Log.d("insertDummyData", "Corrupted URL." + ue.getMessage());
                } catch (IOException ec) {
                    Log.d("insertDummyData", "invalid URL." + ec.getMessage());
                } catch (JSONException ej) {
                    Log.d("insertDummyData", "invalid JSON object." + ej.getMessage());
                } catch (Exception eg) {
                    Log.d("insertDummyData", "unknown error: " + eg.getMessage());
                }
            }
            Log.i("insertDummyData","DB now contains " + numDummyMovies + " items. No need to run anymore");
            return null;    // Not used. we return inside the try/catch
        }
        }
    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private View view;

        public DownloadImageTask(View view) {
            this.view = view;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ((ImageView) view).setImageBitmap(bitmap);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bm = null;
            try {
                URL aURL = new URL(params[0]);
                URLConnection conn = aURL.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bm = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();
            } catch (IOException e) {
                Log.e("getImageBitmap", "Error getting bitmap. " +  e.getMessage());
            }
            return bm;
        }

    }
       }