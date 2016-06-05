package com.example.uaharoni.mymoviescatalog.Entities;

import java.io.Serializable;

public class Movie implements Serializable{
    private long id;
    private String title;
    private String plot;
    private String imdbId;
    private String coverImageURL;
    private double rating;
    private boolean seen;

    public Movie(String title, String plot, String imdbId, String coverImageURL,long id, double rating) {
        this.title = title;
        this.plot = plot;
        this.imdbId = imdbId;
        this.coverImageURL = coverImageURL;
        this.id = id;
        this.rating = rating;
    }

    public Movie(String title,String imdbId,String coverImageURL) {
        // used to obtain extended movie details from the movies websearch
        this.imdbId = imdbId;
        this.title = title;
        this.coverImageURL = coverImageURL;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public void setCoverImageURL(String coverImageURL) {
        this.coverImageURL = coverImageURL;
    }

    @Override
    public String toString() {
        return title;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPlot() {
        return plot;
    }

    public String getImdbId() {
        return imdbId;
    }

    public String getCoverImageURL() {
        return coverImageURL;
    }
    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public boolean getSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}



