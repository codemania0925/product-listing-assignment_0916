package com.example.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the pages of the product listing, newest listing first.
 */
public class ProductCatalog {

    private static final Logger LOG = Logger.getLogger(ProductCatalog.class.getName());

    private final ProductRepository repository;

    public ProductCatalog(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * @param page     1-based page number
     * @param pageSize maximum number of products on a page
     */
    public CatalogPage getPage(int page, int pageSize) {
        try {
            List<Product> products = new ArrayList<>(repository.findAll());
            products.sort(Comparator.comparing(Product::listedAt).reversed());

            int from = (page - 1) * pageSize;
            if (from >= products.size()) {
                return CatalogPage.empty(page, pageSize);
            }
            int to = Math.min(from + pageSize, products.size());
            return new CatalogPage(List.copyOf(products.subList(from, to)), page, pageSize, to < products.size());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Could not build the product listing", e);
            return CatalogPage.empty(page, pageSize);
        }
    }
}
