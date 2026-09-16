package com.example.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds the pages of the product listing, newest listing first.
 */
public class ProductCatalog {

    /**
     * 出品日の新しい順。下書きはまだ出品日を持たないため、出品済みの商品より後ろに並べる。
     *
     * <p>nullsLast は comparing の内側に置く。外側の comparator に reversed() を掛けると
     * null の扱いまで反転し、下書きが先頭に来てしまうため。
     *
     * <p>同順位は SKU で解消する。これが無いと出品日が同じ商品はリポジトリが返した順のままになり、
     * ページごとに呼び出すたび順序が変わって、どの商品がどのページにいるか食い違う。
     */
    private static final Comparator<Product> NEWEST_FIRST =
            Comparator.comparing(Product::listedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Product::sku);

    private final ProductRepository repository;

    public ProductCatalog(ProductRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * @param page     1-based page number
     * @param pageSize maximum number of products on a page
     */
    public CatalogPage getPage(int page, int pageSize) {
        List<Product> products = new ArrayList<>(repository.findAll());
        products.sort(NEWEST_FIRST);

        int from = (page - 1) * pageSize;
        if (from >= products.size()) {
            return CatalogPage.empty(page, pageSize);
        }
        int to = Math.min(from + pageSize, products.size());
        return new CatalogPage(List.copyOf(products.subList(from, to)), page, pageSize, to < products.size());
    }
}
