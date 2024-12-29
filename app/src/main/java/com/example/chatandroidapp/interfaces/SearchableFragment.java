package com.example.chatandroidapp.interfaces;

/**
 * Interface for fragments that support search functionality.
 */
public interface SearchableFragment {
    /**
     * Filters the data displayed in the fragment based on the search query.
     * @param query The search query to filter data.
     */
    void filterData(String query);
}