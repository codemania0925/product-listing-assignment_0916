# Take-home: product listing API

Thank you for taking the time. This should take **about 2 hours**. Please stop around then, even if you are not done,
and write down what is left. You may use any tools you normally use, including AI assistants. In the interview we will
walk through your solution together and make a small change to it live.

## What you get

A small Java 17 HTTP API that lists the products of an online shop, newest listing first, in pages. It uses only the
JDK. The tests use JUnit 5 and Jackson.

```
GET /products?page=1&pageSize=10

{"page":1,"pageSize":10,"hasNext":false,"products":[
  {"sku":"CAB-USBC-2M","name":"USB-C cable 2 m","category":"cables","price":12.50,"listedAt":"2026-09-01"}
]}
```

| Class | Role |
|---|---|
| `Product` | One product. Drafts may not have a name, a price, or a listing date yet |
| `ProductRepository` | Returns all products. Like our database, it does not guarantee any order |
| `ProductCatalog` | Sorts the products (newest listing first) and splits them into pages |
| `CatalogJsonWriter` | Writes a page as JSON |
| `App` | The HTTP server |

Run the tests:

```bash
mvn test
```

Run the server with sample data, then open http://localhost:8080/products:

```bash
mvn -q compile
java -cp target/classes com.example.catalog.App
```

## Bug 1: the listing becomes empty

> A merchandiser reports: "I added one draft product and the whole product list disappeared."
> The draft has no name, no price, and no listing date yet.

Expected behavior:

- Products without a listing date are listed after all listed products.
- A product without a name uses its SKU as the name in the JSON.
- `listedAt` and `price` are `null` in the JSON when the product does not have them.
- One incomplete product never hides the other products.

## Bug 2: the JSON is broken for some product names

> The front-end team reports: "When a name contains a double quote, such as `Monitor 27" 4K`, the response cannot be
> parsed. Responses with Japanese names do not arrive at all."

Expected behavior:

- The response is valid JSON for any product name.
- The response has the header `Content-Type: application/json; charset=utf-8`.

## Bonus bug 3 (only if you have time): products repeat across pages

> "Some products show up on both page 1 and page 2, and some never show up at all.
> It happens with products listed on the same day, and not every time."

Expected behavior:

- Going through all the pages returns every product exactly once.

## What to do

For each bug:

1. Reproduce it with a failing unit test **first**.
2. Fix the cause, not only the symptom.
3. Run `mvn test`.

Commit in small steps. We read the history.

Then write `SOLUTION.md` with:

- the cause of each bug, and how you found it,
- how you fixed it, and which alternatives you considered,
- anything else you noticed in the code (you do not need to fix it).

## Please keep

- The public API, because our grading uses it: `Product`, `ProductRepository`, `ProductCatalog(ProductRepository)`,
  `ProductCatalog.getPage(int, int)`, `CatalogPage`, `App.start(int, ProductRepository)`, and the JSON shape.
- The existing tests passing.

## In the interview

We will ask you to walk through your solution and to add a small feature live, for example filtering by category.
We will also talk about how this listing would work on top of a real database with many products: filtering,
counts per category, and indexes. There is nothing to prepare beyond your own experience.

## How to submit

Send us your repository **with its git history**, either as a zip file or by pushing it to a private repository of
your own and giving us access. Please do not open a fork or pull request on the public assignment repository, because
other candidates could see your solution.
