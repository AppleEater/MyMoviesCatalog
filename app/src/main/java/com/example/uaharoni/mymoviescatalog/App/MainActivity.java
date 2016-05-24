package com.example.uaharoni.mymoviescatalog.App;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.uaharoni.mymoviescatalog.Entities.Movie;
import com.example.uaharoni.mymoviescatalog.Helpers.MoviesDB;
import com.example.uaharoni.mymoviescatalog.R;

public class MainActivity extends MenuActivity implements View.OnClickListener, AdapterView.OnItemClickListener,DialogInterface.OnClickListener {
    private MoviesDB dbHelper;
    private ImageButton btnAddMovie;
    private ListView movieTitlesList;
    private ImageView coverImage;
    private ProgressBar progressBar;
    private static int counterDummyData=1;

    public static SimpleCursorAdapter movieTitlesListCursorAdapter;
    public static final int MENU_OPTION_DELETE_CATALOG = 2;
    public final static int MENU_OPTION_SHARE = 3;
    public static final int APP_EXIT_RETURNCODE = 2;

    private AlertDialog deleteCatalogDialog,addMovieDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeEntities();
        initializeListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetItemsFromDB bgTask = new GetItemsFromDB();
        bgTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if((requestCode==Intent.FILL_IN_ACTION)||(requestCode==Intent.FILL_IN_DATA)){
            if(resultCode==APP_EXIT_RETURNCODE){
                this.finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initializeEntities(){
        dbHelper = new MoviesDB(getApplicationContext());
        btnAddMovie = ((ImageButton) findViewById(R.id.btnAddMovie_Main));
        movieTitlesList = ((ListView)findViewById(R.id.listMoviesMain) );
        coverImage = (ImageView)findViewById(R.id.imageView_main_moviesList);
        progressBar = (ProgressBar)findViewById(R.id.progressBar_Main);

        registerForContextMenu(movieTitlesList);

    }
    private void initializeListeners() {
        // find  AddButton and connect to click event
        btnAddMovie.setOnClickListener(this);

        // Connect the list item to a click listener
        movieTitlesList.setOnItemClickListener(this);

        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }
        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We use the super method defined in the Menu Activity for general activities.
            //Here we specify specific activity-related options
            menu.add(1,MENU_OPTION_DELETE_CATALOG,1,R.string.menu_delete_all_movies);
            //TODO: Check why the removeItem doesn't work for menu option
            menu.removeItem(MENU_OPTION_SHARE);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPTION_DELETE_CATALOG:
                // dialogBox to confirm catalog deletion
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setIcon(android.R.drawable.ic_delete);
                dialogBuilder.setNegativeButton(R.string.dialog_button_delete_catalog_cancel,this);

                dialogBuilder.setPositiveButton(R.string.dialog_button_delete_complete_catalog,this);
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
        addMovieDialog.setButton(AlertDialog.BUTTON_POSITIVE,getResources().getString(R.string.internet_add_movie), this);
        addMovieDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getResources().getString(R.string.manual_add_movie), this);
        addMovieDialog.setCancelable(true);
        addMovieDialog.show();
    }
    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        // Called when clicking a button in the AlertDialog of AddMovie
        if((deleteCatalogDialog != null) && deleteCatalogDialog.equals(dialog)) {
            switch (whichButton){
                case AlertDialog.BUTTON_POSITIVE:
                    dbHelper.deleteDB();
                    MainActivity.movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursor());
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.toast_message_catalog_deleted), Toast.LENGTH_LONG).show();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    dialog.cancel();
                    break;
                default:
                    break;
            }
        }
        if(addMovieDialog != null){
            if(dialog.equals(addMovieDialog)){
                Intent addMovieActivity = null;
                switch (whichButton){
                    case AlertDialog.BUTTON_POSITIVE:
                        //Open the WebSearchActivity
                        addMovieActivity = new Intent(this,WebSearchActivity.class);
                        startActivityForResult(addMovieActivity,Intent.FILL_IN_DATA);
                        break;
                    case AlertDialog.BUTTON_NEGATIVE:
                        //Open the NewEditMovieActivity with no content
                        addMovieActivity = new Intent(this,NewEditMovieActivity.class);
                        addMovieActivity.setAction(Intent.ACTION_INSERT);
                      //  addMovieActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivityForResult(addMovieActivity,Intent.FILL_IN_ACTION);
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
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        switch(item.getItemId()){
            case MENU_CONTEXT_EDIT:
                // Editing an item
                //Toast.makeText(MainActivity.this, "You clicked Edit!", Toast.LENGTH_SHORT).show();
                setEditActivity(menuInfo.position);
                return true;
            case MENU_CONTEXT_DELETE:
                // Deleting an item
                deleteItem(menuInfo.position);
                return true;
            case MENU_CONTEXT_SHARE:
                //Toast.makeText(MainActivity.this, "You clicked share!", Toast.LENGTH_SHORT).show();
                shareMovieItem(menuInfo.position);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return false;
    }
    private void setEditActivity(int position){
        //Toast.makeText(MainActivity.this, "You clicked position" + position , Toast.LENGTH_SHORT).show();
        Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(position);
        long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
        Movie selectedMovie = dbHelper.getMovieById(movieId);
        Intent editMovieActivity = new Intent(getApplicationContext(),NewEditMovieActivity.class);
        //editMovieActivity.addCategory(Intent.CATEGORY_HOME);
        //editMovieActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        editMovieActivity.putExtra("Movie",selectedMovie);
        editMovieActivity.setAction(Intent.ACTION_EDIT);
        startActivityForResult(editMovieActivity,Intent.FILL_IN_ACTION);
        //startActivity(editMovieActivity);
    }
    private void deleteItem(int position){
        Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(position);
        long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
        if(dbHelper.deleteMovie(movieId)){
            Toast.makeText(this, selectedItem.getString(selectedItem.getColumnIndex(MoviesDB.COL_TITLE)) + " " + getResources().getString(R.string.toast_message_deleted_sucessfully), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,selectedItem.getString(selectedItem.getColumnIndex(MoviesDB.COL_TITLE)) + " " + getResources().getString(R.string.toast_message_movie_delete_failed) , Toast.LENGTH_SHORT).show();
        }
        movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursor());
    }
    public class GetItemsFromDB extends AsyncTask<Void,Integer,Cursor>{

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor movieTitlesCursor = dbHelper.getAllMovieTitlesCursor();
            publishProgress(movieTitlesCursor.getCount());
            if(counterDummyData<2){
            //   insertDummyData();
                counterDummyData++;
            }
            return movieTitlesCursor;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onPostExecute(Cursor returnCursor) {
            super.onPostExecute(returnCursor);
            progressBar.setVisibility(ProgressBar.GONE);
            // Create the SimpleCursorAdapter
            String[] listOfFields = new String[]{MoviesDB.COL_TITLE};
            int[] textViewIds = new int[]{R.id.txtMovieListItem_movieTitle};
            movieTitlesListCursorAdapter = new SimpleCursorAdapter(
                    getApplicationContext()
                    ,R.layout.main_movie_list_item
                    ,returnCursor
                    ,listOfFields
                    ,textViewIds
                    ,0);

            // Connecting the list to the adapter
            movieTitlesList.setAdapter(movieTitlesListCursorAdapter);
            movieTitlesListCursorAdapter.changeCursor(dbHelper.getAllMovieTitlesCursor());
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }
    }

private void shareMovieItem(int itemPosition){
    Cursor selectedItem = (Cursor) movieTitlesListCursorAdapter.getItem(itemPosition);
    long movieId = selectedItem.getLong(selectedItem.getColumnIndex(MoviesDB.COL_ID));
    Movie selectedMovie = dbHelper.getMovieById(movieId);
    String txtTShare = selectedMovie.getTitle().trim();
    txtTShare = getString(R.string.movie_subject) + txtTShare;

    if(!(selectedMovie.getImdbId() ==null)){
        txtTShare+="\nhttp://www.imdb.com/title/" + selectedMovie.getImdbId() +"/";
    } else {
        txtTShare +="\nhttp://www.imdb.com/find?q=" + selectedMovie.getTitle().trim().replace(" ","%20") + "&s=movie#tt";
    }
    txtTShare +="\nInformation courtesy of IMDb (http://www.imdb.com). Used with permission.";


    Intent shareMovie = new Intent(Intent.ACTION_SEND);
    shareMovie.setType("text/plain");
    // Add data to the intent, the receiving app will decide what to do with it.
    shareMovie.putExtra(Intent.EXTRA_SUBJECT,"My movie info");
    shareMovie.putExtra(Intent.EXTRA_TEXT, txtTShare);
    startActivity(Intent.createChooser(shareMovie,"Share movie info"));

}


        private void insertDummyData(){
  /*
        String loremIpsumTxt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        for (int i = 0; i < ((int)loremIpsumTxt.length()*Math.random()); i++) {
            Movie movie = new Movie(
                        getResources().getString(R.string.txt_movie)+String.valueOf((int)(Math.random()*1000))
                        ,loremIpsumTxt.substring(((int)(Math.random()*loremIpsumTxt.length())/10),50)
                    ,null
                    ,null
                    );
            if(!dbHelper.insertMovie(movie)){
                Toast.makeText(MainActivity.this, "Movie insert failed", Toast.LENGTH_LONG).show();
            }
        }
        */
            boolean result = dbHelper.insertMovie(new Movie("Mivtza Savta","Mivtza Savta (\"Operation Grandma\") is a satirical Israeli comedy about three very different brothers trying to get around many obstacles to bury their grandmother on her kibbutz.","tt0374053","http://ia.media-imdb.com/images/M/MV5BZjFkZDFjZjEtZDM4OC00Y2E1LTg5ZjgtZjg2NjAwYzQ0YTljXkEyXkFqcGdeQXVyMjMyMzI4MzY@._V1_SX300.jpg",0,3));
    }
}
