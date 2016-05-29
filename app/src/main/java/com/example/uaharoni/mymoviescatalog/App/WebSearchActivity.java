package com.example.uaharoni.mymoviescatalog.App;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.uaharoni.mymoviescatalog.Entities.Movie;
import com.example.uaharoni.mymoviescatalog.Entities.OMDB_Web;
import com.example.uaharoni.mymoviescatalog.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class WebSearchActivity extends MenuActivity implements View.OnClickListener, AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {

    private SearchView etxtSearch;
    private ListView listViewTitles;
    private ArrayAdapter<Movie> listViewTitlesAdapter;
    private Button btnCancel;
    private Movie selectedMovie;
    private ProgressBar pbGetTitle;
    private ProgressBar pbSimple;
    private GetMovieTitlesFromWeb getMovieTitlesFromWeb;


    public static Intent createIntent(Context context) {
        return( new Intent(context, WebSearchActivity.class));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_search);

        initializeViews();
        initializeListeners();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if((requestCode==Intent.FILL_IN_ACTION)){
            switch (resultCode){
                case MainActivity.MOVIE_ADDED_RETURNCODE:
                    setResult(MainActivity.MOVIE_ADDED_RETURNCODE);
                    this.finish();
                case MainActivity.APP_EXIT_RETURNCODE:
                    // returning to the calling activity with result code to finish
                    setResult(MainActivity.APP_EXIT_RETURNCODE);
                    this.finish();
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }

        }

    }
    private void initializeViews(){
        etxtSearch = (SearchView) findViewById(R.id.searchView);
        btnCancel = (Button) findViewById(R.id.btnCancelAddFromWeb);
        listViewTitles = (ListView) findViewById(R.id.listView_WebSearch);
        listViewTitlesAdapter = new ArrayAdapter<Movie>(WebSearchActivity.this,android.R.layout.simple_list_item_1,android.R.id.text1);
        // Connecting the listView of the movies list adapter
        listViewTitles.setAdapter(listViewTitlesAdapter);
        pbGetTitle = ((ProgressBar) findViewById(R.id.pb_getTitles_WebSearch));
        if(pbGetTitle != null){
            pbGetTitle.setVisibility(View.INVISIBLE);
        }
        pbSimple = (ProgressBar)findViewById(R.id.pbSimple_WebSearch);
        if(pbSimple != null) {pbSimple.setVisibility(ProgressBar.INVISIBLE);}
    }
    private void initializeListeners(){
        // Connecting the Cancel button to the listener
        btnCancel.setOnClickListener(this);
        listViewTitles.setOnItemClickListener(this);
        etxtSearch.setOnQueryTextListener(this);
    }

    @Override
    public void onClick(View buttonClicked) {
        switch (buttonClicked.getId()){
            case R.id.btnCancelAddFromWeb:
                finish();
                break;
            default:
                // The search button is checked in the onQueryTextSubmit method
                break;
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Here we use a minimised movie object, with only title and imdbId
        // Later, we will bring all the movie details from the web
        selectedMovie = listViewTitlesAdapter.getItem(position);
           GetMovieInfoByImdb taskGetMovieInfo = new GetMovieInfoByImdb();
           taskGetMovieInfo.execute(selectedMovie.getImdbId());
    }
    @Override
    public boolean onQueryTextSubmit(String query) {
        if(query.isEmpty()) {
            Toast.makeText(WebSearchActivity.this, "Please type movie title to search!", Toast.LENGTH_LONG).show();
            return false;
        }
            String searchString = query.trim().replace(" ","%20");
            getMovieTitlesFromWeb = new GetMovieTitlesFromWeb();
            getMovieTitlesFromWeb.execute(searchString);
        Toast.makeText(WebSearchActivity.this, "Searching for " + searchString + " . Please wait...", Toast.LENGTH_LONG).show();
            return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // This method runs after every text change,
        return false;
    }

    public class GetMovieInfoByImdb extends AsyncTask<String,Void,Movie>{

        @Override
        protected Movie doInBackground(String... params) {
            String inputLine;   // Single line from the streamBuffer
            String resultResponse = "";  // The complete text of the response
            //Movie updatedMovie = params[0];
            String movieImdb = params[0];

            String urlDefaultPrefixURL = OMDB_Web.URL_PROTOCOL + OMDB_Web.URL_HOST_OMDB;
            try {
                URL url = new URL((urlDefaultPrefixURL + OMDB_Web.URL_INFOSEARCH_PARAM + movieImdb + OMDB_Web.URLPOSTFIX ));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if(!this.isCancelled()){
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return null;
                    }
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((inputLine = reader.readLine()) != null && !this.isCancelled()) {
                    resultResponse += inputLine;
                }
                // convert the json response into a big JSON object
                JSONObject movieJSONObject = new JSONObject(resultResponse);
                String moviePlot = movieJSONObject.getString(OMDB_Web.JSON_PLOT);
                selectedMovie.setPlot(moviePlot);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return selectedMovie;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbSimple.setBackgroundColor(Color.TRANSPARENT);
            pbSimple.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onPostExecute(Movie returnedMovie) {
            super.onPostExecute(returnedMovie);
            pbSimple.setVisibility(ProgressBar.GONE);

            Intent newMovieActivity = new Intent(getApplicationContext(),NewEditMovieActivity.class);

            newMovieActivity.putExtra("Movie",returnedMovie);
            newMovieActivity.setAction(Intent.ACTION_INSERT);
            startActivityForResult(newMovieActivity,Intent.FILL_IN_ACTION);
            }
        }

    public class GetMovieTitlesFromWeb extends AsyncTask<String,Integer,ArrayList<Movie>> {

        @Override
        protected ArrayList<Movie> doInBackground(String... params) {
            ArrayList<Movie> tmpArray = new ArrayList<Movie>();
            String searchTitle = params[0];
            int pageNumber = 0;
            int totalResults = 0;
            int countItems = 0;

            String response = OMDB_Web.JSON_RESPONSE_TYPE_TRUE;
            while (response.equalsIgnoreCase(OMDB_Web.JSON_RESPONSE_TYPE_TRUE)) {
                try{
                    pageNumber++;

                    String myUrl = constructURL(searchTitle, pageNumber);
                   HttpURLConnection urlConnection = getConnection(myUrl);

                    JSONObject resultObject = getJsonFromConnection(urlConnection);
                    if (resultObject==null){
                        tmpArray=null;
                    } else {
                        response = resultObject.getString(OMDB_Web.JSON_RESPNOSE);
                        if (response.equals(OMDB_Web.JSON_RESPONSE_TYPE_TRUE)) {
                            if (totalResults == 0) {
                                totalResults = resultObject.getInt(OMDB_Web.JSON_TOTALRESULTS);
                            } else {
                                publishProgress((int) (countItems / totalResults) * 100);
                            }
                            JSONArray json_movieTitles = resultObject.getJSONArray(OMDB_Web.JSON_SEARCH_ARRAY);
                            for (int i = 0; i < json_movieTitles.length(); i++) {
                                JSONObject movie = json_movieTitles.getJSONObject(i);
                                Movie miniMovie = getMovieFromJSON(movie);
                                assert tmpArray != null;
                                tmpArray.add(miniMovie);
                                countItems++;
                            }
                        }
                    }

                } catch (JSONException e) {
                    Log.e("doInBackground:", "Error parsing JSON Array... " + e.getMessage());
                }
            }

            return tmpArray;
        }

        private JSONObject getJsonFromConnection(HttpURLConnection connection){

            if(connection == null){
                Log.e("getJsonFromConnection:", "No connection was found ");
                return null;
            }
            String inputLine;   // Single line from the streamBuffer
            String resultResponse = "";  // The complete text of the response
            JSONObject resultObject = null;
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((inputLine = reader.readLine()) != null) {
                    resultResponse += inputLine;
                }
                // convert the json response into a big JSON object
                resultObject = new JSONObject(resultResponse);
            } catch (IOException eIO) {
                Log.e("getJsonFromConnection:", "Can't get JSON object... " + eIO.getMessage());
            }
          catch (JSONException eJSON) {
              Log.e("getJsonFromConnection:", "Error Parsing JSON response.." + eJSON.getMessage());
            }
            return resultObject;
        }
        private HttpURLConnection getConnection(String urlString){
            URL url = null;
            HttpURLConnection connection = null;
            try {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(2000);
                connection.setConnectTimeout(5000);
                int responseCode = connection.getResponseCode();

            } catch (MalformedURLException eUrl) {
                Log.e("getConnection:", "Bad URL format... " + eUrl.getMessage());
            } catch (SecurityException en){
                Log.e("getConnection:", "No permissions to connect: " + en.getMessage());
            } catch (IOException e) {
                Log.e("getConnection:", "Failed to create connection: " + e.getMessage());
            }
            return connection;
        }

        private Movie getMovieFromJSON(JSONObject jsonMovieObject){

            String movieTitle = null;
            String moviePoster = null;
            String movieImdbId = null;
            String movieYear = null;
            try {
                movieTitle = jsonMovieObject.getString(OMDB_Web.JSON_TITLE);
                moviePoster = jsonMovieObject.getString(OMDB_Web.JSON_POSTER);
                movieImdbId= jsonMovieObject.getString(OMDB_Web.JSON_IMDBID);
                movieYear = jsonMovieObject.getString(OMDB_Web.JSON_YEAR);
            } catch (JSONException eJSON) {
                Log.e("getMovieFromJSON:", "Error Parsing JSON elements.." + eJSON.getMessage());
            }
            return(new Movie(movieTitle+" ("+movieYear+")",movieImdbId,(moviePoster=="N/A")?null:moviePoster));
        }
        private String constructURL(String searchTitle, int pageNumber) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("www.omdbapi.com")
                    .appendPath("")
                    .appendQueryParameter("s",searchTitle)
                    .appendQueryParameter("type","movie")
                    .appendQueryParameter("r","json");
            if(pageNumber !=0){
                builder.appendQueryParameter("page", String.valueOf(pageNumber));
            }
            return builder.build().toString();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            listViewTitlesAdapter.clear();
            pbGetTitle.setMax(100);
            pbGetTitle.setProgress(0);
            pbGetTitle.setVisibility(View.VISIBLE);
            pbGetTitle.incrementProgressBy(4);
            pbGetTitle.bringToFront();
            pbGetTitle.setBackgroundColor(Color.DKGRAY);
            pbGetTitle.setDrawingCacheBackgroundColor(Color.MAGENTA);

        }

        @Override
        protected void onPostExecute(ArrayList<Movie> resultArray) {
            super.onPostExecute(resultArray);

            if(resultArray != null) {
               // Toast.makeText(WebSearchActivity.this, "Finished searching.  Found " + resultArray.size() + " items", Toast.LENGTH_LONG).show();
                listViewTitlesAdapter.clear();
                listViewTitlesAdapter.addAll(resultArray);
            }
            pbGetTitle.setVisibility(View.GONE);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pbGetTitle.setProgress(values[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pbGetTitle.setVisibility(ProgressBar.GONE);
        }
    }


}
