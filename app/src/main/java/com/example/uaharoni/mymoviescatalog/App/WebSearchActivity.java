package com.example.uaharoni.mymoviescatalog.App;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

    SearchView etxtSearch;
    ListView listViewWebTitles;
    ArrayAdapter<Movie> listViewWebTitlesAdapter;
    Button btnCancel;
    Movie selectedMovie;

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
            if(resultCode==MainActivity.APP_EXIT_RETURNCODE){
                Toast.makeText(this, "Goodbye from NewEdit after WebSearch Activity", Toast.LENGTH_LONG).show();
                // returning to the calling activity with result code to finish
                setResult(MainActivity.APP_EXIT_RETURNCODE);
                this.finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void initializeViews(){
        etxtSearch = (SearchView) findViewById(R.id.searchView);
        btnCancel = (Button) findViewById(R.id.btnCancelAddFromWeb);
        listViewWebTitles = (ListView) findViewById(R.id.listView_WebSearch);
        listViewWebTitlesAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,android.R.id.text1);
        // Connecting the listview of the movies list
        listViewWebTitles.setAdapter(listViewWebTitlesAdapter);
    }
    private void initializeListeners(){
        // Connecting the Cancel button to the listener
        btnCancel.setOnClickListener(this);
        listViewWebTitles.setOnItemClickListener(this);
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
        // Here we use a minified movie object, with only title and imdbId
        // Later, we will bring all the movie details from the web
        selectedMovie = listViewWebTitlesAdapter.getItem(position);
           //TODO: fetch all the movie details in AsyncTask, based on the IMDBid
           GetMovieInfoByImdb taskGetMovieInfo = new GetMovieInfoByImdb();
           taskGetMovieInfo.execute(selectedMovie.getImdbId());
    }


    public boolean onQueryTextSubmit(String query) {
        if(!query.isEmpty()){
            String searchString = query.trim().replace(" ","%20");
            GetMovieTitlesFromWeb asyncTask = new GetMovieTitlesFromWeb();
            asyncTask.execute(searchString);
            return true;
        }  else {
            return false;
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // This method runs after every text change,
        return false;
    }


    public class GetMovieTitlesFromWeb extends AsyncTask<String,Integer,ArrayList<Movie>>{

        @Override
        protected ArrayList<Movie> doInBackground(String... params) {
            ArrayList<Movie> movieTitlesArray = new ArrayList<Movie>();
            String inputLine;   // Single line from the streamBuffer
            String resultResponse = "";  // The complete text of the response

            String urlDefaultPrefixURL = OMDB_Web.URL_PROTOCOL + OMDB_Web.URL_DOMAIN_OMDB;
            int pageNumber =0;
            int totalResults=0;
            String response=OMDB_Web.JSON_RESPONSE_TYPE_TRUE;
                    while(response.equalsIgnoreCase(OMDB_Web.JSON_RESPONSE_TYPE_TRUE)){
                        try{
                            pageNumber++;
                            URL url = new URL((urlDefaultPrefixURL + OMDB_Web.URL_TITLESEARCH_PARAM + params[0] + OMDB_Web.URLPOSTFIX + OMDB_Web.URL_PAGE + pageNumber ));
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                                return null;
                                //TODO: Verify we check onPostExecute for null object
                            }
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            while ((inputLine = reader.readLine()) != null) {
                                resultResponse += inputLine;
                            }
                            // convert the json response into a big JSON object
                            JSONObject resultObject = new JSONObject(resultResponse);
                            response = resultObject.getString(OMDB_Web.JSON_RESPNOSE);
                            if(response.equals(OMDB_Web.JSON_RESPONSE_TYPE_TRUE)){
                                if(totalResults==0){
                                    totalResults = resultObject.getInt(OMDB_Web.JSON_TOTALRESULTS);
                                }
                                JSONArray json_movieTitles = resultObject.getJSONArray(OMDB_Web.JSON_SEARCH_ARRAY);
                                for (int i = 0; i < json_movieTitles.length(); i++) {
                                    JSONObject movie = json_movieTitles.getJSONObject(i);

                                    String movieTitle = movie.getString(OMDB_Web.JSON_TITLE);
                                    String moviePoster = movie.getString(OMDB_Web.JSON_POSTER);
                                    String movieImdbId= movie.getString(OMDB_Web.JSON_IMDBID);

                                    Movie miniMovie = new Movie(movieTitle,movieImdbId,moviePoster);

                                    movieTitlesArray.add(miniMovie);
                                    }
                                }
                        } catch (MalformedURLException e) {
                            //e.printStackTrace();
                            Log.e("GetMovies:", "Bad URL format... " + e.getMessage());
                        } catch (IOException e) {
                            Log.e("GetMovies:", "OOPS! something bad happened... " + e.getMessage());
                        } catch (JSONException e) {
                            //e.printStackTrace();
                            Log.e("GetMovies:", "Error Parsing JSON response.." + e.getMessage());
                        }
                    }
            return movieTitlesArray;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> moviesArray) {
            Toast.makeText(WebSearchActivity.this, "Finished searching.  found " + moviesArray.size() + " items", Toast.LENGTH_SHORT).show();
            super.onPostExecute(moviesArray);
            if(!moviesArray.isEmpty()){
                listViewWebTitlesAdapter.clear();
                listViewWebTitlesAdapter.addAll(moviesArray);
                }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    public class GetMovieInfoByImdb extends AsyncTask<String,Void,Movie>{

        @Override
        protected Movie doInBackground(String... params) {
            String inputLine;   // Single line from the streamBuffer
            String resultResponse = "";  // The complete text of the response
            //Movie updatedMovie = params[0];
            String movieImdb = params[0];


            String urlDefaultPrefixURL = OMDB_Web.URL_PROTOCOL + OMDB_Web.URL_DOMAIN_OMDB;
            try {
                URL url = new URL((urlDefaultPrefixURL + OMDB_Web.URL_INFOSEARCH_PARAM + movieImdb + OMDB_Web.URLPOSTFIX ));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((inputLine = reader.readLine()) != null) {
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
        protected void onPostExecute(Movie returnedMovie) {
            super.onPostExecute(returnedMovie);
            //Intent newMovieActivity = new Intent(WebSearchActivity.this,NewEditMovieActivity.class);
            Intent newMovieActivity = new Intent(getApplicationContext(),NewEditMovieActivity.class);

            newMovieActivity.putExtra("Movie",returnedMovie);
            newMovieActivity.setAction(Intent.ACTION_INSERT);
            startActivity(newMovieActivity);
        }
    }

    }
