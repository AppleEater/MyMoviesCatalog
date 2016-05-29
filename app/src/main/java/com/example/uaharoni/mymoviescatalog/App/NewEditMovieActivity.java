package com.example.uaharoni.mymoviescatalog.App;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.uaharoni.mymoviescatalog.Entities.Movie;
import com.example.uaharoni.mymoviescatalog.Helpers.MoviesDB;
import com.example.uaharoni.mymoviescatalog.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NewEditMovieActivity extends MenuActivity implements View.OnClickListener, ShareActionProvider.OnShareTargetSelectedListener {
    Movie editableMovie;
    EditText movieTitleEditText, moviePlotEditText,movieCoverUrlEditText;
    ImageView coverImage;
    ProgressBar progressBar;
    Bitmap coverImageBitmap;
    String movieImdbId;
    Button btnSaveUpdate, btnCancel, btnShowImage;
    String intentAction;
    MoviesDB dbHelper;
    ShareActionProvider mShareActionProvider;
    Intent shareMovie;
    public final static int MENU_OPTION_CLEAR_FIELDS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_movie);

        initializeViews();
        initializeListeners();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Get the menu item.
        MenuItem menuItem = menu.findItem(R.id.menu_share);
        // Get the provider and hold onto it to set/change the share intent.
         mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        shareMovie = new Intent(Intent.ACTION_SEND);
        shareMovie.setType("text/plain");
        //shareMovie.addCategory(Intent.CATEGORY_ALTERNATIVE);
        shareMovie.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
        // Search and populate the menu with acceptable offering applications.
        menu.addIntentOptions(
                1
                ,MainActivity.MENU_OPTION_SHARE
                ,0
                ,this.getComponentName()
                ,null
                ,shareMovie
                ,0
                ,null
        );
        menu.add(2,MENU_OPTION_CLEAR_FIELDS,1,getResources().getString(R.string.menu_option_clear_all_fields));
        return super.onCreateOptionsMenu(menu);
    }

    private void initializeViews(){
        movieTitleEditText = (EditText) findViewById(R.id.etxtAddMovie_subject);
        moviePlotEditText = (EditText) findViewById(R.id.etxtMoviePlot);
        movieCoverUrlEditText = (EditText)findViewById(R.id.etxtMovieUrl_NewEditMovie);
         coverImage = (ImageView)findViewById(R.id.coverImage_NewEditMovie);
        progressBar = (ProgressBar)findViewById(R.id.progressBar_NewEditActivity);

        btnCancel = (Button)findViewById(R.id.btnCancel_NewEditMovie);
        btnSaveUpdate = (Button)findViewById(R.id.btnSave_NewEditMovie);
        btnShowImage = (Button)findViewById(R.id.btnShowCoverImage_NewEditMovie);

        dbHelper = new MoviesDB(this);

        Intent intentReceived = getIntent();
        intentAction = intentReceived.getAction();
        editableMovie = (Movie) intentReceived.getSerializableExtra("Movie");
        if(editableMovie != null){
            parseObject(editableMovie);
        } else {
            editableMovie = new Movie(null,null,null);
        }

        switch(intentAction){
            case Intent.ACTION_INSERT:
                // The activity was launched empty, so no need to populate anything
                btnSaveUpdate.setText(getResources().getString(R.string.btn_Add));
                break;
            case Intent.ACTION_EDIT:
                // We arrive here to edit an existing movie object. We don't care where we got it from

                break;
            default:
                break;
        }
    }
    private void parseObject(Movie movie){
        // get a movie object, and populate to all the view fields
        movieTitleEditText.setText(movie.getTitle());
        moviePlotEditText.setText(movie.getPlot());
        movieCoverUrlEditText.setText(movie.getCoverImageURL());
        movieImdbId = editableMovie.getImdbId();

        if(!movieCoverUrlEditText.getText().toString().isEmpty()){
            getCoverImage(movie.getCoverImageURL());
        }
    }

    private void initializeListeners() {
        btnSaveUpdate.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnShowImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCancel_NewEditMovie:
                finish();
                break;
            case R.id.btnSave_NewEditMovie:
                // insert movie object into the DB
                if(updateMovieObject()){
                    setResult(MainActivity.MOVIE_ADDED_RETURNCODE);
                    finish();
                } else {
                    Toast.makeText(NewEditMovieActivity.this, "Movie not updated in catalog", Toast.LENGTH_LONG).show();
                    return;
                }
                break;
            case R.id.btnShowCoverImage_NewEditMovie:
                    // show the ImageView with the coverurl
                //Clearing the current imageView
                    coverImage.setImageBitmap(null);
                // Updating image from the coverUrl
                    getCoverImage(movieCoverUrlEditText.getText().toString());
                break;
            default:
                break;
        }
    }
    private boolean updateMovieObject(){
        boolean resultCode=true;
        // Insert or update the movie object into the DB
        String movieTitle = movieTitleEditText.getText().toString();
        String moviePlot = moviePlotEditText.getText().toString();
        String movieCoverUrl = movieCoverUrlEditText.getText().toString().replace(" ","%20");

        if(movieTitle.isEmpty()&& moviePlot.isEmpty()){
            resultCode=false;
        } else {
            editableMovie.setTitle(movieTitle);
            editableMovie.setPlot(moviePlot);
            editableMovie.setCoverImageURL(movieCoverUrl);
            //  editableMovie.setRating(0); // TODO: Future feature for rating

            if (intentAction == Intent.ACTION_EDIT) {
                dbHelper.updateMovie(editableMovie);
            }
            if (intentAction == Intent.ACTION_INSERT) {
                dbHelper.insertMovie(editableMovie);
            }
            resultCode = true;
        }
        return resultCode;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
        switch (item.getItemId()){
            case R.id.menu_share:
                shareCurrentMovie(shareMovie);
                break;
            case MENU_OPTION_CLEAR_FIELDS:
                clearAllFields();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void shareCurrentMovie(Intent intent) {
        String txtTShare = null;
        //TODO: Check why mShareActionProvider is always null
        if (mShareActionProvider != null) {
            if(updateMovieObject()){
                txtTShare = editableMovie.getTitle().trim();
                if(!(editableMovie.getImdbId() ==null)){
                    txtTShare+="\nhttp://www.imdb.com/title/" + editableMovie.getImdbId() +"/";
                } else {
                    txtTShare +="\nhttp://www.imdb.com/find?q=" + editableMovie.getTitle().trim().replace(" ","%20") + "&s=movie#tt";
                }
            }
            intent.putExtra(Intent.EXTRA_TEXT, txtTShare);
            intent.putExtra(Intent.EXTRA_TITLE, editableMovie.getTitle());

            mShareActionProvider.setShareIntent(intent);
            }
    }
    private void getCoverImage(String url){
        if(!(url.isEmpty()) && (url.startsWith("http://")|| url.startsWith("https://"))){
            //String targetUrl = url.replace(" ","%20");
            Log.i("getCoverImage", "imageUrl: " + url);
            DownloadImageTask task = new DownloadImageTask();
            task.execute(url);
        } else {
            //Toast.makeText(this, getResources().getString(R.string.toast_message_invalid_url), Toast.LENGTH_SHORT).show();
        }
    }
    public void clearAllFields(){
        movieTitleEditText.setText("");
        moviePlotEditText.setText("");
        movieCoverUrlEditText.setText("");
        coverImage.setImageBitmap(null);
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        return false;
    }

    // The task to download the image from the web
    public class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap=null;
            Log.d("doInBackground", "starting download of image");
            try {
                URL url = new URL(params[0]);
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                // get the stream of bytes
                InputStream is = httpCon.getInputStream();
                // convert bytes into Bitmap object (an image)
                 bitmap = BitmapFactory.decodeStream(is);
                return bitmap;
            } catch (Exception e){
                Log.e("DownloadImageTask", e.getMessage());
            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(ProgressBar.GONE);
            if (result != null) {
                coverImage.setImageBitmap(result);
            } else {
                //Toast.makeText(NewEditMovieActivity.this, "Image not loaded...", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPreExecute() {
            //Toast.makeText(NewEditMovieActivity.this, "Loading Image....", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(ProgressBar.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }
    }


    }
