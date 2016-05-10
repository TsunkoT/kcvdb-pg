# kcvdb-pg

APIごとにテーブルを分けてPostgreSQLに突っ込みます。

## 必要環境

- PostgreSQL 9.4 以降(PGDATAの置き場はなるべく早いディスク推奨)
- JDK8

## 事前準備

- PostgreSQL で `CREATE DATABASE kcvdb;` しておく
- `Config.groovy` 内のDB設定、logファイルリスト設定をしておく

## 実行

```sh
$ ./gradlew run
```

エラー出力が多くてうるさいときには

```sh
$ ./gradlew run 2> stderr.out
```

とかするとよい。
