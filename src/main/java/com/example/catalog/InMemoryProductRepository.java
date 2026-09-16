package com.example.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps products in memory. Like our database, it returns them in no particular order, which can change between calls.
 */
public class InMemoryProductRepository implements ProductRepository {

    private final List<Product> products;

    public InMemoryProductRepository(List<Product> products) {
        this.products = List.copyOf(products);
    }

    @Override
    public List<Product> findAll() {
        List<Product> copy = new ArrayList<>(products);
        Collections.shuffle(copy);
        return copy;
    }
}
