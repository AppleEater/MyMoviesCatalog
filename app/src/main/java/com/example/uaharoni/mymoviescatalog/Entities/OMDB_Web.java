package com.example.uaharoni.mymoviescatalog.Entities;

public final class OMDB_Web {
    public final static String URL_TITLESEARCH_PARAM = "?s=";
    public final static int SEARCH_TITLE = 1;

    public final static String URL_INFOSEARCH_PARAM = "?i=";
    public final static int SEARCH_INFO = 2;


    public final static String URLPOSTFIX = "&type=movie&r=json";
    public final static String URL_PAGE = "&page=";
    public final static String JSON_RESPNOSE = "Response";
    public final static String JSON_RESPONSE_TYPE_TRUE = "True";
    public final static String JSON_RESPONSE_TYPE_FALSE = "False";
    public final static String JSON_TOTALRESULTS = "totalResults";
    public final int MAX_ITEMS_IN_PAGE = 10;
    public final static String JSON_SEARCH_ARRAY = "Search";
    public final static String JSON_TITLE = "Title";
    public final static String JSON_YEAR = "Year";
    public final static  String JSON_IMDBID = "imdbID";
    public final static String JSON_POSTER = "Poster";
    public final static String JSON_PLOT = "Plot";
    public final static String JSON_RATING = "imdbRating";



    public final  static String URL_PROTOCOL = "http";
    public final  static String AUTHORITY = "www.omdbapi.com";

}
