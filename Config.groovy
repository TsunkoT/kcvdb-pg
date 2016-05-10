/* DB接続設定 */
kcvdb.jdbcUrl = "jdbc:postgresql://localhost:5432/kcvdb"
kcvdb.username = 'postgres'
kcvdb.password = 'kcvdb'
kcvdb.driver = 'org.postgresql.Driver'

/* logファイルリスト ${TARGET_DATE}.filelist.txt DBのテーブル名にも利用する(文字列の形式は問わないのでtestとかでもよいが英数字アンダースコアに限る) */
kcvdb.TARGET_DATE = "sample" // '20160507'
/* logファイルリストの何番目から読むか(エラー時等再開用) */
kcvdb.FILES_START_OFFSET = 1