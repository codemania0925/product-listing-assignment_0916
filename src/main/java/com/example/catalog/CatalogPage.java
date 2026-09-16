package com.example.catalog;

import java.util.List;

/**
 * One page of the product listing.
 *
 * @param products the products on this page
 * @param page     1-based page number
 * @param pageSize maximum number of products on a page
 * @param hasNext  whether there is a next page
 */
public record CatalogPage(List<Product> products, int page, int pageSize, boolean hasNext) {

    static CatalogPage empty(int page, int pageSize) {
        return new CatalogPage(List.of(), page, pageSize, false);
    }
}
