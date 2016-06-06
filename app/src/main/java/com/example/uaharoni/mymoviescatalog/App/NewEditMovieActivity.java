package com.example.uaharoni.mymoviescatalog.App;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.RatingBar;
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
    RatingBar ratingBar_movieRating;
    Bitmap coverImageBitmap;
    String movieImdbId;
    Button btnSaveUpdate, btnCancel, btnShowImage;
    String intentAction;
    MoviesDB dbHelper;
    ShareActionProvider mShareActionProvider;
    Intent shareMovie;
    public final static int MENU_OPTION_CLEAR_FIELDS = 5;
    public final static int MENU_OPTION_SHARE_MOVIE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_movie);

        initializeViews();
        initializeListeners();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(2,MENU_OPTION_CLEAR_FIELDS,1,getResources().getString(R.string.menu_option_clear_all_fields));

        menu.add(2,MENU_OPTION_SHARE_MOVIE,2,getString(R.string.menu_share)).setIcon(android.R.drawable.ic_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return super.onCreateOptionsMenu(menu);
    }

    private void initializeViews(){
        movieTitleEditText = (EditText) findViewById(R.id.etxtAddMovie_subject);
        moviePlotEditText = (EditText) findViewById(R.id.etxtMoviePlot);
        movieCoverUrlEditText = (EditText)findViewById(R.id.etxtMovieUrl_NewEditMovie);
         coverImage = (ImageView)findViewById(R.id.coverImage_NewEditMovie);
        progressBar = (ProgressBar)findViewById(R.id.progressBar_NewEditActivity);
        ratingBar_movieRating = (RatingBar)findViewById(R.id.rb_movieRating);

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
        ratingBar_movieRating.setRating((float)movie.getRating());

        if(!movieCoverUrlEditText.getText().toString().isEmpty()){
            getCoverImage(movie.getCoverImageURL());
            coverImage.setOnClickListener(this);
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
                Log.d("onClick","Return to calling activity");
                finish();
                return;
            case R.id.btnSave_NewEditMovie:
                // insert movie object into the DB
                Log.i("onClick","Saving and returning and to main activity");
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
            case R.id.coverImage_NewEditMovie:
                Log.d("onClick", "Image was clicked");
                showMovieTrailer();
            default:
                break;
        }
    }
    private void showMovieTrailer(){
        String movieTitle = movieTitleEditText.getText().toString().trim();
        String urlMovieTrailer = String.format("https://www.youtube.com/results?search_query=%s",(movieTitle.trim()+" official trailer").replace(" ","+"));
        Intent openweb = new Intent(Intent.ACTION_VIEW, Uri.parse(urlMovieTrailer));
        startActivity(openweb);
    }
    private boolean updateMovieObject(){
        boolean resultCode;
        // Insert or update the movie object into the DB
        String movieTitle = movieTitleEditText.getText().toString();
        String moviePlot = moviePlotEditText.getText().toString();
        String movieCoverUrl = movieCoverUrlEditText.getText().toString().replace(" ","%20");
        double movieRating_d = (double) ratingBar_movieRating.getRating();

        if(movieTitle.isEmpty()&& moviePlot.isEmpty()){
            resultCode=false;
        } else {
            editableMovie.setTitle(movieTitle);
            editableMovie.setPlot(moviePlot);
            editableMovie.setCoverImageURL(movieCoverUrl);
            editableMovie.setRating(movieRating_d);

            if (intentAction.equals(Intent.ACTION_EDIT)) {
                dbHelper.updateMovie(editableMovie);
            }
            if (intentAction.equals(Intent.ACTION_INSERT)) {
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

            case MENU_OPTION_CLEAR_FIELDS:
                clearAllFields();
                break;

            case MENU_OPTION_SHARE_MOVIE:
                Log.i("onOptionsItemSelected","Share the current movie info");
                shareCurrentMovie();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void shareCurrentMovie() {
        // Shares the current movie partial info  to an implicit intent
        String movieTitle = movieTitleEditText.getText().toString().trim();
        String movieImdbId = editableMovie.getImdbId();
        String txtTShare = movieTitle;

        if (movieImdbId != null) {
            txtTShare += "\nhttp://www.imdb.com/title/" + movieImdbId + "/";
        } else {
            txtTShare += "\nhttp://www.imdb.com/find?q=" + movieTitle.trim().replace(" ","%20") + "&s=movie#tt";
        }
        txtTShare += "\nInformation courtesy of IMDb (http://www.imdb.com). Used with permission."; // required by IMDB sharing permissions

        Intent shareMovie = new Intent(Intent.ACTION_SEND);
        shareMovie.setType("text/plain");
        shareMovie.putExtra(Intent.EXTRA_SUBJECT, "My movie info");
        shareMovie.putExtra(Intent.EXTRA_TEXT, txtTShare);
        startActivity(Intent.createChooser(shareMovie, getString(R.string.menu_option_share_intent_text)));
        }
        private void getCoverImage(String url){
        if(!(url.isEmpty()) && (url.startsWith("http://")|| url.startsWith("https://"))){
            Log.i("getCoverImage", "imageUrl: " + url);
            DownloadImageTask task = new DownloadImageTask();
            task.execute(url);
        } else {
            Log.e("getCoverImage","Seems not a valid URL: " + url);
        }
    }
    public void clearAllFields(){
        movieTitleEditText.setText("");
        moviePlotEditText.setText("");
        movieCoverUrlEditText.setText("");
        coverImage.setImageBitmap(null);
        ratingBar_movieRating.setRating(0);
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
                Log.w("onPostExecute", "No image was provided");
            }
        }

        @Override
        protected void onPreExecute() {
            //Toast.makeText(NewEditMovieActivity.this, "Loading Image....", Toast.LENGTH_LONG).show();
            progressBar.setProgress(0);
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
