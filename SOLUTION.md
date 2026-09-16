# 解答メモ

3件のバグをすべて修正しました。いずれも「先に失敗するテストを書く」→「原因を直す」の順です。`mvn test` は 16 件すべて成功します。

| バグ | 原因 | 修正 | テスト |
|---|---|---|---|
| 1. 一覧が空になる | `Comparator.comparing(Product::listedAt)` が下書きで例外になり、`getPage` の広すぎる `catch (RuntimeException)` がそれを空ページに変えていた | `nullsLast` で並べ、catch を削除。失敗は HTTP 500 に変換 | `ProductCatalogTest.listsDraftsWithoutAListingDateAfterTheListedProducts` / `…doesNotHideRepositoryFailuresBehindAnEmptyPage` / `AppTest.answersWithAServerErrorWhenTheCatalogFails` |
| 2. JSON が壊れる | 商品名をエスケープせずに JSON へ連結していた。加えて `Content-Length` に文字数を渡していた（正しくはバイト数） | `CatalogJsonWriter` で文字列をエスケープ。本文は UTF-8 で送り、`charset=utf-8` を明記 | `CatalogJsonWriterTest.escapesQuotesAndBackslashesInNames` / `…escapesControlCharactersInNames` / `AppTest.servesUtf8JsonForNamesOutsideAscii` |
| 3. 商品がページ間で重複する | ソートキーが一意でなく、出品日が同じ商品はリポジトリの返す順のままだった。その順序は呼び出しごとに変わる | SKU で同順位を解消し、全順序にした | `ProductCatalogTest.pagingReturnsEveryProductExactlyOnceWhenTheRepositoryOrderChanges` |

## バグ1: 下書き 1 件で一覧全体が消える

**見つけ方.** 報告は「リクエストが失敗した」ではなく「一覧が消えた」でした。どこかで例外が握りつぶされている兆候です。
`ProductCatalog.getPage` は 12 行ほどの中に容疑者が 2 つあります。null を取りうるとドキュメントに書かれたフィールドを比較する
comparator と、空ページを返す `catch (RuntimeException)` です。出品済み 2 件＋下書き 1 件のテストを書くと結果は `[]`
になり、ログに出ていたスタックトレースが `Comparator.comparing` の `NullPointerException` を示していました。

**原因.** 2つの欠陥が重なっていました。

1. `Comparator.comparing(Product::listedAt).reversed()` は null のキーに対して `LocalDate.compareTo` を呼ぶため、
   下書きが 1 件あるだけでソート全体が例外になる。
2. その例外を `catch (RuntimeException)` が受け取り、ログを出して `CatalogPage.empty(...)` を返していた。空ページは
   正常な応答なので、障害が見えなくなる。API は `200` と空配列を返し続け、結果として「全部消えた」という症状になり、
   担当者が気づくまで誰も検知できませんでした。

**修正.** `Comparator.comparing(Product::listedAt, Comparator.nullsLast(Comparator.reverseOrder()))` で並べ、
下書きを出品済み商品の後ろに置きます。catch は削除しました。`App` 側で想定外の失敗を 500 に変換してログへ残すので、
壊れているものは壊れているように見えます。

`nullsLast` を `comparing` の **内側** に置いている点が肝です。外側の comparator に `.reversed()` を掛けると null の
扱いまで反転し、下書きが先頭に来てしまいます。

同じ報告の JSON 側は `CatalogJsonWriter` にありました。下書きは `"name":"null"`、`"listedAt":"null"` と、4文字の
文字列として出力されていました。`Product.displayName()` が SKU で代用し、欠損値は JSON のリテラル `null` を書きます。

**検討した代替案.**

- *下書きを一覧から除外する.* 却下。`TASK.md` の期待動作は「最後に並べる」ことであり、行を隠すのはこのバグ自体の挙動です。
- *catch を残して、その中で 500 を返す.* 却下。`ProductCatalog` はドメイン側であり、HTTP のステータスコードを知るべき
  ではありません。例外はそのまま投げ、端（`App`）で 1 回だけ変換すれば知識が 1 か所に収まります。
- *SKU の代用を `CatalogJsonWriter` の中で行う.* 動きはしますが、「名前が無いとき購入者に何を見せるか」は JSON 形式の
  都合ではなく商品の性質なので、`Product.displayName()` にすれば他の画面からも再利用できます。
- *下書きの出品日に `LocalDate.MIN` を入れる.* 却下。データに嘘をついてソートを通すだけで、JSON 出力時には結局 null を
  扱う必要が残ります。

## バグ2: 引用符で JSON が壊れ、日本語名は届かない

**見つけ方.** 症状は 2 つで原因も別物だったため、別々に再現しました。`CatalogJsonWriter` は文字列連結で JSON を組み立てて
いるので、`Monitor 27" 4K` は名前の途中で文字列を閉じます。これは writer の単体テストで再現できます。日本語名は別の失敗で、
writer 自体は正しく出力できており、`App` 経由のテストでは応答が完了しませんでした。サーバのログには
`IOException: too many bytes to write to stream` が出ていました。

**原因.**

1. `CatalogJsonWriter` が商品データをそのまま JSON に埋め込んでいた。名前に `"`・`\`・制御文字が含まれると、どのパーサも
   受け付けない出力になります。エスケープせずにデータを構文へ貼り付けるという意味で、SQL インジェクションと同じ構図です。
   商品名は店舗側の入力なので、いずれ必ず「変わった文字」が入ります。
2. `App` が `sendResponseHeaders(200, json.length())`、つまり **文字数** を宣言したうえで、プラットフォーム既定の文字
   コードで `json.getBytes()` を書き込んでいた。`Content-Length` はバイト数であり、ASCII 以外の文字は UTF-8 で 2〜3 バイト
   になります。宣言が短いので書き込み中に例外となり、クライアントは来ないバイトを待ち続けました（「応答が届かない」）。
   `Content-Type` に `charset` が無いことも、クライアントに文字コードを推測させる点で悪材料でした。

**修正.** `appendString` が `"`・`\`・短縮エスケープ・`U+0020` 未満の文字（`\u00xx`）を処理し、それ以外はそのまま通して
UTF-8 で符号化します。`App` は本文を一度だけ符号化し、`json.length`（バイト数）を送り、
`Content-Type: application/json; charset=utf-8` を設定します。

`AppTest` にはクラス単位の `@Timeout` を付けました。このバグはテストを失敗させるのではなく**ハング**させるためで、
止まったままになるテストは残したくありません。

**検討した代替案.**

- *Jackson を main スコープに入れて直列化する.* 本番のサービスであればそうします。手書きの JSON writer は、まさに今回の
  ような不具合を繰り返し生む場所です。ただし `TASK.md` は main を JDK のみと指定しており、Jackson はテスト依存なので、
  エスケープは手書きのまま、全項目が通る 1 メソッドに集約しました。
- *ASCII 以外も `\uXXXX` でエスケープする.* 応答が純粋な ASCII になり文字コードの推測に強くなりますが、日本語名のサイズが
  約 3 倍になり、デバッグ時に読めなくなります。正しい UTF-8 と正しいヘッダを送るほうが良い取引です。
- *チャンク転送（`sendResponseHeaders(200, 0)`）にする.* 長さの計算自体が不要になりますが、`Content-Length` が失われ、
  クライアントの進捗表示や接続の再利用に影響します。ページは小さくすでに全体がメモリ上にあるので、固定長のほうが親切です。

## バグ3: 商品がページをまたいで重複する

**見つけ方.** 「同じ日・毎回ではない」はソートキーが一意でないことを示します。`ProductRepository` は順序を保証しないと
明記され、`InMemoryProductRepository` は呼び出しごとにシャッフルします。一方 `ProductCatalog` は**ページ要求のたびに**
全件取得してソートします。`List.sort` は安定ソートなので、出品日が同じ商品はリポジトリの順序を保ちます。つまり 1 ページ目と
2 ページ目で並びが違うのです。

**原因.** ソートが半順序でした。同じ日付の相対順序が未定義なので、リクエストごとに実質的に別の列をページ分割していました。
ある商品が 1 回目は 2 番目、次は 4 番目に来ると、あるページでは重複し別のページでは欠落します。

**修正.** `thenComparing(Product::sku)` で全順序にしました。SKU は一意なので、リポジトリが何を返しても順序は決まります。
テストではシャッフルではなく回転するリポジトリを使い、「たいてい失敗する」ではなく決定的に再現するようにしました。

**検討した代替案.**

- *`ProductRepository` 側でソートする.* この実装では隠せますが、実際のデータベースでは同じ問題が残ります。ページ分割は
  カタログの仕事であり、取得元の順序に依存すべきではありません。
- *ソート済みリストを `ProductCatalog` にキャッシュしてページを切る.* 連続したリクエストの一貫性は得られますが、全件を
  メモリに抱え込み、しかも古くなります。どちらの問題も全順序にすれば解決します。
- *キーセットページング（`WHERE (listed_at, sku) < (:lastListedAt, :lastSku)`）.* 実データベースに対してはこれを採ります。
  そして同じ全順序が前提になります。公開 API がカーソル方式に変わるため今回は対象外ですが、次の一手はこれです。

## 併せて修正: ページ計算

`(page - 1) * pageSize` は大きなページ番号で int を溢れ、`getPage(0, 10)` では負の開始位置になります。どちらも
`IndexOutOfBoundsException` になり、バグ1の catch があった頃は静かに空ページへ変えられていました。開始位置を `long` で
計算し、最終ページより先は空ページ、1 未満の引数は `IllegalArgumentException` にしました。`App` はすでにそれらを `400` で
弾いているので、API の振る舞いは変わりません。

## 気づいたが直していない点

- **リクエストのたびに一覧を組み直している.** `getPage` は 1 ページのために全商品を読み込んでソートします。デモ用の
  リポジトリなら問題ありませんが、実データでは重くなります。データベースなら
  `ORDER BY listed_at DESC, sku ASC LIMIT :pageSize OFFSET :offset` と `(listed_at DESC, sku)` のインデックス、
  `hasNext` は `pageSize + 1` 件取得して判定（追加クエリ不要）。深いページでは `OFFSET` はスキップ分を走査するため、
  キーセットページングが有利です。
- **`GET /products/anything` も一覧を返す.** `HttpServer` のコンテキストは前方一致なので、一致しない下位パスは `404`
  にすべきです。
- **エラー応答に本文が無い.** `400`/`405`/`500` はステータスのみで理由がありません。`{"error":"..."}` のような小さな本文が
  あるとフロント側が原因を判断できます。JSON 形状が採点対象のため、今回は変更していません。
- **サーバが既定の executor で動いている.** 交換を 1 件ずつ処理します。実トラフィックを流す前に、まず
  `server.setExecutor(...)` で上限付きのプールを設定します。`ProductCatalog` と `CatalogJsonWriter` は可変状態を持たない
  ので、共有しても安全です。
- **`pageSize > 50` を `400` で弾いている.** 上限に丸めるほうが親切で、多くの API はそうしています。ただしこれはバグでは
  なくプロダクト判断です。
- **`Product` が自身の不変条件を強制していない.** Javadoc は `sku` と `category` が null にならないと書いていますが、
  コンパクトコンストラクタで `Objects.requireNonNull` を入れれば「祈る」代わりに保証できます。採点対象の公開 API なので
  record は触らず、代わりに `CatalogJsonWriter` を全項目 null 安全にしました。
- **サンプルデータを対象にしたテストが無い.** `SampleData` には引用符・日本語・下書きが揃っており、3件のバグを同時に通す
  スモークテストとして優秀です。デモデータに対する assert を増やす代わりに、実際にサーバを起動して手で確認しました。

## 実行方法

```bash
mvn test                                        # 16 件
mvn -q compile && java -cp target/classes com.example.catalog.App
curl "http://localhost:8080/products?page=1&pageSize=3"
```

所要時間: この解答メモを含めておよそ 2 時間です。
