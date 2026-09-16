package com.example.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProductCatalogTest {

    @Test
    void newestListingComesFirst() {
        ProductCatalog catalog = catalogOf(
                product("older", LocalDate.of(2026, 1, 10)),
                product("newest", LocalDate.of(2026, 3, 1)),
                product("middle", LocalDate.of(2026, 2, 5)));

        CatalogPage page = catalog.getPage(1, 10);

        assertEquals(List.of("newest", "middle", "older"), skus(page));
        assertFalse(page.hasNext());
    }

    @Test
    void splitsProductsIntoPages() {
        ProductCatalog catalog = catalogOf(
                product("a", LocalDate.of(2026, 1, 1)),
                product("b", LocalDate.of(2026, 1, 2)),
                product("c", LocalDate.of(2026, 1, 3)));

        CatalogPage first = catalog.getPage(1, 2);
        CatalogPage second = catalog.getPage(2, 2);

        assertEquals(List.of("c", "b"), skus(first));
        assertTrue(first.hasNext());
        assertEquals(List.of("a"), skus(second));
        assertFalse(second.hasNext());
    }

    /** バグ1: 出品日を持たない下書きが 1 件あるだけで、一覧全体が空になっていた。 */
    @Test
    void listsDraftsWithoutAListingDateAfterTheListedProducts() {
        ProductCatalog catalog = catalogOf(
                product("listed-older", LocalDate.of(2026, 1, 10)),
                draft("draft"),
                product("listed-newer", LocalDate.of(2026, 3, 1)));

        CatalogPage page = catalog.getPage(1, 10);

        assertEquals(List.of("listed-newer", "listed-older", "draft"), skus(page));
    }

    /** バグ1: 空ページは正常な応答なので、障害の通知として使ってはいけない。 */
    @Test
    void doesNotHideRepositoryFailuresBehindAnEmptyPage() {
        ProductCatalog catalog = new ProductCatalog(() -> {
            throw new IllegalStateException("database is down");
        });

        assertThrows(IllegalStateException.class, () -> catalog.getPage(1, 10));
    }

    private static Product product(String sku, LocalDate listedAt) {
        return new Product(sku, "Product " + sku, "cables", new BigDecimal("10.00"), listedAt);
    }

    private static Product draft(String sku) {
        return new Product(sku, null, "cables", null, null);
    }

    private static ProductCatalog catalogOf(Product... products) {
        return new ProductCatalog(new InMemoryProductRepository(List.of(products)));
    }

    private static List<String> skus(CatalogPage page) {
        return page.products().stream().map(Product::sku).toList();
    }
}
