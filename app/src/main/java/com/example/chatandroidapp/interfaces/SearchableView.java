package com.example.chatandroidapp.interfaces;

/**
 * Interface for view that support search functionality.
 */
public interface SearchableView {
    /**
     * Filters the data displayed in the view based on the search query.
     *
     * @param query The search query to filter data.
     */
    void filterData(String query);
}