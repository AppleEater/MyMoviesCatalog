package com.example.uaharoni.mymoviescatalog.App;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
    private GetFromWeb getTitles=null;
    private ArrayList<Movie> lastTitlesList = new ArrayList<>();

    public static Intent createIntent(Context context) {
        return( new Intent(context, WebSearchActivity.class));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_search);
        initializeViews();
        initializeListeners();
        /*
        Log.i("onCreate","Applying ThreadPolicy");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
*/
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

    @Override
    protected void onResume() {
        super.onResume();
        etxtSearch.clearFocus();
    }

    private void initializeViews() {
        etxtSearch = (SearchView) findViewById(R.id.searchView);
        btnCancel = (Button) findViewById(R.id.btnCancelAddFromWeb);
        listViewTitles = (ListView) findViewById(R.id.listView_WebSearch);
        listViewTitlesAdapter = new ArrayAdapter<>(WebSearchActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1);
        // Connecting the listView of the movies list adapter
        listViewTitles.setAdapter(listViewTitlesAdapter);
        pbGetTitle = ((ProgressBar) findViewById(R.id.pb_getTitles_WebSearch));
        if (pbGetTitle != null) {
            pbGetTitle.setVisibility(ProgressBar.INVISIBLE);
        }
        etxtSearch.setIconified(false);

    }
    private void initializeListeners(){
        // Connecting the Cancel button to the listener
        btnCancel.setOnClickListener(this);
        listViewTitles.setOnItemClickListener(this);
        etxtSearch.setSubmitButtonEnabled(true);
        etxtSearch.setOnQueryTextListener(this);
    }

    @Override
    public void onClick(View buttonClicked) {
        switch (buttonClicked.getId()){
            case R.id.btnCancelAddFromWeb:
                if(getTitles != null  && !getTitles.isCancelled()){
                    Log.d("onClick","Cancelling background task in status " + getTitles.getStatus());
                    getTitles.cancel(true);
                    pbGetTitle.setVisibility(ProgressBar.GONE);
                } else {
                    finish();
                }
                break;
            default:
                // The search button is checked in the onQueryTextSubmit method
                break;
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Here we use a minimized movie object, with only title and imdbId
        // Later, we will bring all the movie details from the web
        selectedMovie = listViewTitlesAdapter.getItem(position);
        GetFromWeb getMovieInfo = new GetFromWeb(OMDB_Web.SEARCH_INFO);
        Log.d("onItemClick","About to run background search on " + selectedMovie.getImdbId());
        getMovieInfo.execute(selectedMovie.getImdbId());
    }
    @Override
    public boolean onQueryTextSubmit(String query) {
        if(query.isEmpty()) {
            Toast.makeText(WebSearchActivity.this, "Please type movie title to search!", Toast.LENGTH_LONG).show();
            return false;
        }
        //Toast.makeText(WebSearchActivity.this, "Searching for " + query + " . Please wait...", Toast.LENGTH_LONG).show();
        etxtSearch.clearFocus();
        getTitles = new GetFromWeb(OMDB_Web.SEARCH_TITLE);
        Log.d("onQueryTextSubmit","About to execute background search on " + query.trim());
        getTitles.execute(query.trim());
         return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // This method runs after every text change,
        return false;
    }
public class GetFromWeb extends AsyncTask<String,Integer,ArrayList<Movie>>{

    private final int searchType;

    protected GetFromWeb(int searchType){
        super();
        this.searchType = searchType;
    }

    @Override
    protected void onCancelled(ArrayList<Movie> movies) {
        super.onCancelled(movies);
        if(movies != null) {
            listViewTitlesAdapter.clear();
            listViewTitlesAdapter.addAll(movies);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("onPreExecute","Setting the progressBar");
        pbGetTitle.setProgress(0);
        pbGetTitle.setVisibility(ProgressBar.VISIBLE);
    }
    @Override
    protected void onPostExecute(ArrayList<Movie> movies) {
        super.onPostExecute(movies);
        Log.d("onPostExecute","Hiding the progressBar");
        pbGetTitle.setVisibility(ProgressBar.GONE);
        Log.d("onPostExecute","Checking the ArrayList based on searchType: " + searchType);
        if(searchType==OMDB_Web.SEARCH_TITLE){
            if(!movies.isEmpty()){
                if(movies.size()>0){
                    Log.d("onPostExecute","Refreshing the adapter");
                    listViewTitlesAdapter.clear();
                    listViewTitlesAdapter.addAll(movies);
                    Log.d("onPostExecute","Saving the list in the lastTitlesList");
                    lastTitlesList = movies;
                    Log.d("onPostExecute","Sanity check for lastTitlesList. Items: " + lastTitlesList.size());
                } else {
                    Log.d("onPostExecute","ArrayList is empty");
                    Toast toast = new Toast(WebSearchActivity.this);
                    toast.setText(R.string.movie_not_found);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setMargin(50f,50f);
                    toast.setGravity(Gravity.CENTER_VERTICAL,50,50);
                    toast.show();
                    return;
                }
            }
        }
        if(searchType==OMDB_Web.SEARCH_INFO){
            Log.d("onPostExecute","Loading info of selectedMovie");
            selectedMovie = movies.get(0);
            Log.i("onPostExecute","Sending the movie object (" + selectedMovie.getTitle() + ") to intent for editing.");
            Intent newMovieActivity = new Intent(getApplicationContext(),NewEditMovieActivity.class);
            newMovieActivity.putExtra("Movie",selectedMovie);
            newMovieActivity.setAction(Intent.ACTION_INSERT);
            startActivityForResult(newMovieActivity,Intent.FILL_IN_ACTION);
        }
    }
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        // Log.d("onProgressUpdate","Updating progressBar with " + values[0]);
        pbGetTitle.setProgress(values[0]);
    }
    @Override
    protected ArrayList<Movie> doInBackground(String... params) {
        return (getMoviesList(params[0]));
    }
    private ArrayList<Movie> getMoviesList(String searchTerm) {
        ArrayList<Movie> arrMovieList;

        switch(searchType) {
            case OMDB_Web.SEARCH_TITLE:
                Log.d("getMoviesList","Getting ArrayList of movies that match " + searchTerm);
                arrMovieList = getTitlesList(searchTerm);
                break;
            case OMDB_Web.SEARCH_INFO:
                Log.d("getMoviesList","Getting ArrayList of movies that have this IMDBid: " + searchTerm);
                arrMovieList = getMovieInfoList(searchTerm);
                break;
            default:
                Log.e("GetFromWeb", "Parameter unknown");
                return null;
        }
        return arrMovieList;
    }
    private ArrayList<Movie> getTitlesList (String movieSearch) {
        ArrayList<Movie> arrMovieList = new ArrayList<>();
        URL url = null;

        int pageNumber=0;
        int totalResults = 0;
        int countItems = 0;

        String json_Response = OMDB_Web.JSON_RESPONSE_TYPE_TRUE;

        try {
            while (json_Response.equals(OMDB_Web.JSON_RESPONSE_TYPE_TRUE)) {
                if (isCancelled()) {
                    break;
                }
                pageNumber++;
                //Log.d("getTitlesList", "Loading page number " + pageNumber);
                url = new URL(buildUrl(movieSearch,pageNumber).toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                JSONObject jsResponse = getJSON(connection);
                json_Response = jsResponse.getString(OMDB_Web.JSON_RESPNOSE);
                Log.i("getTitlesList","json_response = " + json_Response);
                if(!(json_Response.equals(OMDB_Web.JSON_RESPONSE_TYPE_TRUE))){
                    Log.d("getTitlesList","No more proper object. response = " + json_Response + ". Error:" + jsResponse.getString("Error"));
                    continue;
                }
                if (totalResults == 0 && jsResponse.has(OMDB_Web.JSON_TOTALRESULTS)) {
                    totalResults = jsResponse.getInt(OMDB_Web.JSON_TOTALRESULTS);
                    Log.i("getTitlesList", "Found " + totalResults + " results.");
                }
                JSONArray json_movieTitles = jsResponse.getJSONArray(OMDB_Web.JSON_SEARCH_ARRAY);
                Log.d("getTitlesList","SearchArray has " + json_movieTitles.length() + " items");
                for (int i = 0; i < json_movieTitles.length(); i++) {
                    JSONObject movie = json_movieTitles.getJSONObject(i);
                    Movie miniMovie = getMovieFromJSON(movie);
                    //Log.d("getTitlesList","Found movie " + miniMovie.getTitle());
                    arrMovieList.add(miniMovie);
                    //Log.d("getTitlesList","Added movie " + miniMovie.getTitle());
                }
                countItems+=json_movieTitles.length();
                //Log.d("getTitlesList","publishing " + (countItems*100/totalResults));
                publishProgress((countItems*100/totalResults));
                Log.i("getTitlesList","So far " + countItems + " items of " + totalResults + ". ArrayList size: " + arrMovieList.size());
            }
            Log.d("getTitlesList","Finished scanning all " + totalResults + " results. response is " + json_Response);
        } catch (MalformedURLException eurl) {
            Log.e("getTitlesList","Malformed URL " + buildUrl(movieSearch,pageNumber).toString());
        } catch (IOException ce) {
            Log.e("getTitlesList","Connection failure  to " + url.toString() + " ." + ce.getMessage());
        } catch (JSONException je) {
            Log.e("getTitlesList","No access to JSON Object attributes. " + je.getMessage());
        } catch (Exception e){
            Log.e("getTitlesList","Error in while loop. " + e.getMessage());
        }

        Log.d("getTitlesList","List has " + arrMovieList.size() + " items");
        return  arrMovieList;
    }
    private ArrayList<Movie> getMovieInfoList(String imdbid) {
        ArrayList<Movie> arrMovieList = new ArrayList<>();
        URL url = null;

        try {
            url = new URL(buildUrl(imdbid,0).toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            JSONObject jsResponse = getJSON(connection);
            if(!(jsResponse.getString(OMDB_Web.JSON_RESPNOSE).equals(OMDB_Web.JSON_RESPONSE_TYPE_TRUE))) {
                Log.e("getMovieInfoList","No valid Movie  object obtained");
                        return null;
            }
            Movie updatedMovie = getMovieFromJSON(jsResponse);
            Log.i("getMovieInfoList","Got movie object " + updatedMovie.getTitle());
            arrMovieList.add(updatedMovie);
            Log.d("getMovieInfoList","arrMovieList has " + arrMovieList.size() + " items");
        } catch (MalformedURLException eurl) {
            Log.e("getMovieInfoList","Malformed URL " + buildUrl(imdbid,0).toString());
        } catch (IOException e1) {
            Log.e("getMovieInfoList","Connection failure  to " + url.toString());
        } catch (JSONException je) {
            Log.e("getMovieInfoList","No access to JSON Object attributes. " + je.getMessage());
        }

        return  arrMovieList;
    }
    private Movie getMovieFromJSON(JSONObject jsonMovieObject){
        Movie returnMovie;
        String movieTitle = null;
        String moviePoster = null;
        String movieImdbId = null;
        String moviePlot = null;
        int movieIMDBRating = 0;
        String movieYear = null;
        try {
            movieTitle = jsonMovieObject.getString(OMDB_Web.JSON_TITLE);
            moviePoster = jsonMovieObject.getString(OMDB_Web.JSON_POSTER);
            movieImdbId= jsonMovieObject.getString(OMDB_Web.JSON_IMDBID);
            movieYear = jsonMovieObject.getString(OMDB_Web.JSON_YEAR);
            if(searchType==OMDB_Web.SEARCH_INFO){
                moviePlot = jsonMovieObject.getString(OMDB_Web.JSON_PLOT);
                movieIMDBRating = jsonMovieObject.getInt(OMDB_Web.JSON_RATING);

            }
        } catch (JSONException eJSON) {
            Log.e("getMovieFromJSON:", "Error Parsing JSON elements.." + eJSON.getMessage());
        }
        if(searchType==OMDB_Web.SEARCH_TITLE) {
            // We obtain a minified Movie object
            returnMovie = new Movie(movieTitle + " (" + movieYear + ")",movieImdbId,moviePoster);
        } else {
            // We bring the complete Movie object
            returnMovie = new Movie(movieTitle, moviePlot, movieImdbId, (moviePoster.equals("N/A")) ? null : moviePoster, 0, movieIMDBRating);
        }
        return  returnMovie;
    }

    private JSONObject getJSON (HttpURLConnection connection) {
        JSONObject jsonResponse=null;
        if (connection == null) {
            Log.e("getJSON:", "No connection was found ");
            return null;
        }
        String inputLine;
        String resultResponse = "";
        BufferedReader reader;
        try {
            Log.d("getJSON","Reading the response from the URL");
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((inputLine = reader.readLine()) != null && !this.isCancelled()) {
                resultResponse += inputLine;
            }
            //Log.d("getJSON","resultResponse is:  " + resultResponse);
            jsonResponse = new JSONObject(resultResponse);
            // Log.d("getJSON","Obtained JSON object: " + jsonResponse.toString());
        } catch (IOException e) {
            Log.e("getJSON", "Error reading from BufferReader. " + e.getMessage());
        } catch (JSONException je) {
            Log.e("getJSON", "Error converting to JSON object. " + je.getMessage());
        }
        return jsonResponse;
    }


    private Uri buildUrl(String term,int pageNumber){
        Uri.Builder urlBuilder = new Uri.Builder().scheme("http")
                .authority(OMDB_Web.AUTHORITY)
                .appendPath("");

        switch (searchType){
            case OMDB_Web.SEARCH_TITLE:
                urlBuilder.appendQueryParameter("s",term)
                        .appendQueryParameter("type","movie");

                if(pageNumber != 0){
                    urlBuilder.appendQueryParameter("page", String.valueOf(pageNumber));
                }
                break;
            case OMDB_Web.SEARCH_INFO:
                urlBuilder.appendQueryParameter("i",term);
                break;
            default:
                urlBuilder.appendQueryParameter("s",term);
                break;
        }
        urlBuilder.appendQueryParameter("r","json");
        Log.i("buildUrl","Loading URI:" + urlBuilder.build().toString());
        return urlBuilder.build();
    }
    }
}
