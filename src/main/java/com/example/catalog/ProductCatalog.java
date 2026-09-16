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
     * @return 指定されたページ。最終ページより先を指している場合は空のページ
     * @throws IllegalArgumentException {@code page} または {@code pageSize} が 1 未満の場合
     */
    public CatalogPage getPage(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1, but was " + page);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1, but was " + pageSize);
        }

        List<Product> products = new ArrayList<>(repository.findAll());
        products.sort(NEWEST_FIRST);

        // 大きいページ番号と pageSize の積は int を溢れるため、開始位置は long で計算する。
        long offset = (long) (page - 1) * pageSize;
        if (offset >= products.size()) {
            return CatalogPage.empty(page, pageSize);
        }
        int from = (int) offset;
        int to = (int) Math.min(offset + pageSize, products.size());
        return new CatalogPage(List.copyOf(products.subList(from, to)), page, pageSize, to < products.size());
    }
}
