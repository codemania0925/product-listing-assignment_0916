package com.example.catalog;

import java.util.List;

/**
 * Source of products.
 */
public interface ProductRepository {

    /**
     * Returns all products. Like our database, the order of the returned list is not guaranteed.
     */
    List<Product> findAll();
}
