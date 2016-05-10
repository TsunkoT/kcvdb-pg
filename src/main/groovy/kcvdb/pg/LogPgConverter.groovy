package kcvdb.pg

import groovy.sql.Sql
import org.postgresql.util.PSQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat

public class LogPgConverter {

    static String TAB = "\t"

    public static void main(String[] args) {
        println new Date().format("yyyy/MM/dd HH:mm:ss") + " START"

        def config = new ConfigSlurper().parse(new File('Config.groovy').toURL())

        String TARGET_DATE = config.kcvdb.TARGET_DATE
        Integer FILES_START_OFFSET = config.kcvdb.FILES_START_OFFSET

        def sql = Sql.newInstance(
                config.kcvdb.jdbcUrl as String,
                config.kcvdb.username as String,
                config.kcvdb.password as String,
                config.kcvdb.driver as String)

        createTable(sql, TARGET_DATE, config.kcvdb.username as String)
        loadLog(sql, TARGET_DATE, FILES_START_OFFSET)

        println new Date().format("yyyy/MM/dd HH:mm:ss") + " END"
    }

    /** テーブル名生成 */
    static def makeTableName(String TARGET_DATE, String kcapiName) {
        "raw_log_${TARGET_DATE}_${kcapiName.replaceAll("/", "_")}"
    }

    /** テーブル作成 */
    static def createTable(Sql sql, String TARGET_DATE, String username) {
        new File("kcapi_list.txt").eachLine { String kcapiName ->
            def tableName = makeTableName(TARGET_DATE, kcapiName)

            sql.withTransaction {
                String queryCreate = """
CREATE TABLE public.${tableName}
(
  client text NOT NULL,
  uuid character varying(36) NOT NULL,
  api_url text NOT NULL,
  api_status integer,
  datetime1 timestamp with time zone,
  datetime2 timestamp with time zone,
  api_svdata jsonb,
  filename text NOT NULL,
  file_line integer NOT NULL,
  api_info text,
  CONSTRAINT ${tableName}_pkey PRIMARY KEY (filename, file_line)
)
WITH (
  OIDS=FALSE
);
"""
                String queryGrant = "ALTER TABLE public.${tableName} OWNER TO ${username};"
                try {
                    sql.execute(queryCreate)
                    sql.execute(queryGrant)
                } catch (PSQLException pex) {
                    println "PSQLException: ${pex.message}"
                }
                println new Date().format("yyyy/MM/dd HH:mm:ss") + " - CREATE TABLE ${tableName}"
            }
        }
    }

    /** データ読み込み */
    static def loadLog(Sql sql, String TARGET_DATE, Integer FILES_START_OFFSET) {
        def dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

        def files = 0
        new File("${TARGET_DATE}.filelist.txt").eachLine { String logPath -> // logファイルリストの各行に対して処理
            files++
            def filename = logPath.trim()
            if (files >= FILES_START_OFFSET) { // 指定された数logファイルの処理を飛ばす
                def lines = 0
                sql.withTransaction {
                    def withoutError = true
                    new File(filename).eachLine { logLine -> // logファイル内の各行に対して処理
                        if (!withoutError) { // エラーが出たファイルは残りすべての行を読み飛ばす
                            return
                        }
                        def tsv = logLine.split(TAB)
                        if (tsv.size() >= 8) { // データが足りない行は読み飛ばす

                            String client = tsv[0]
                            String uuid = tsv[1]
                            String api_url = tsv[2].substring(tsv[2].indexOf("kcsapi"))
                            Integer api_status = tsv[3] as Integer
                            Timestamp datetime1 = tsv[4] ? dateFormat.parse(tsv[4]).toTimestamp() : null
                            Timestamp datetime2 = tsv[5] ? dateFormat.parse(tsv[5]).toTimestamp() : null
                            String api_info = tsv[6]
                            String api_svdata = tsv[7] - "svdata="

                            def tableName = makeTableName(TARGET_DATE, api_url)

                            lines++

                            if (api_svdata) {
                                try {
                                    sql.executeInsert(
                                            "INSERT INTO ${tableName} (filename, file_line, client, uuid, api_url, api_status, datetime1, datetime2, api_info, api_svdata) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST( ? AS jsonb));".toString(),
                                            [filename, lines, client, uuid, api_url, api_status, datetime1, datetime2, api_info, api_svdata])
                                } catch (PSQLException pex) {
                                    println "PSQLException in LINE:${lines.toString().padLeft(5)} in ${filename} ${pex.message}"
//                                    pex.printStackTrace()
                                    withoutError = false
                                } catch (Exception ex) {
                                    println "Exception in LINE:${lines.toString().padLeft(5)} in ${filename} ${ex.message}"
//                                    ex.printStackTrace()
                                    withoutError = false
                                }
                            }
                        }
                    }
                }
                println new Date().format("yyyy/MM/dd HH:mm:ss") + " -${files.toString().padLeft(5)}: $filename"
            }
        }
    }
}
