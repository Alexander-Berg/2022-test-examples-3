package ru.yandex.market.supercontroller.mbologs.dao.oracle;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * @author amaslak
 */
@Component
public class OracleTestData {

    private static final Logger log = Logger.getLogger(OracleTestData.class);

    private static final String TEST_TABLE_BASE = "sc_test" + "_" + System.currentTimeMillis();

    private static final int TEST_PARTITION_COUT = 5;

    private static final int TEST_SUB_PARTITION_COUT = 20;

    private static final String NOP_SESSION = "30021101_0201";

    private static final String TEST_TABLE = TEST_TABLE_BASE + "_" + NOP_SESSION;

    @Autowired
    private JdbcTemplate siteCatalogJdbcTemplate;

    public String getTestTableBase() {
        return TEST_TABLE_BASE;
    }

    public int getTestPartitionCout() {
        return TEST_PARTITION_COUT;
    }

    public int getTestSubPartitionCout() {
        return TEST_SUB_PARTITION_COUT;
    }

    public String getTestTable() {
        return TEST_TABLE;
    }

    public String getNopSession() {
        return NOP_SESSION;
    }

    public void createTestTable() {
        deleteTestTable();

        StringBuffer sqlCreateTable = new StringBuffer();
        sqlCreateTable.append("CREATE TABLE " + TEST_TABLE_BASE + " (" +
            "  PARTITION_ID NUMBER(2,0)," +
            "  SUBPARTITION_ID NUMBER(2,0)," +
            "  OFFER_ID VARCHAR2(32 CHAR) NOT NULL ENABLE," +
            "  FEED_ID NUMBER(11,0) NOT NULL ENABLE," +
            "  OFFER VARCHAR2(4000 BYTE)," +
            "  DESCR VARCHAR2(4000 BYTE)," +
            "  PARAMS VARCHAR2(4000 BYTE)," +
            "  OFFER_PARAMS CLOB," +
            "  ADULT NUMBER(11,0) DEFAULT NULL," +
            "  BARCODE VARCHAR2(1024 BYTE)," +
            "  SHOP_CATEGORY_ID VARCHAR2(32 CHAR) DEFAULT '-1' NOT NULL ENABLE," +
            "  PRICE NUMBER(22,5) DEFAULT NULL," +
            "  PRICE_CHANGED NUMBER(1,0) DEFAULT NULL," +
            "  DOC_ID NUMBER(11,0) DEFAULT 0," +
            "  ONSTOCK NUMBER(1,0) DEFAULT NULL," +
            "  SHOP_ID NUMBER(11,0) DEFAULT -1 NOT NULL ENABLE," +
            "  CATEGORY_ID NUMBER(11,0) DEFAULT -1 NOT NULL ENABLE," +
            "  MATCHED_ID NUMBER(11,0) DEFAULT -1 NOT NULL ENABLE," +
            "  MAPPED_ID NUMBER(11,0) DEFAULT -1 NOT NULL ENABLE," +
            "  MAPPED_ACTIVE NUMBER(1,0) DEFAULT 0 NOT NULL ENABLE," +
            "  SHOP_CATEGORY_NAME VARCHAR2(4000 BYTE)," +
            "  DATASOURCE VARCHAR2(1000 BYTE) DEFAULT 'null' NOT NULL ENABLE," +
            "  PROBABILITY NUMBER DEFAULT 4 NOT NULL ENABLE," +
            "  CHANGE_TYPE NUMBER(1,0) DEFAULT 4," +
            "  SESSION_ID VARCHAR2(50 CHAR) NOT NULL ENABLE," +
            "  DETAILS VARCHAR2(4000 BYTE) DEFAULT NULL," +
            "  MATCHED_TYPE NUMBER(14,0) DEFAULT NULL," +
            "  MODIFICATION_ID NUMBER(14,0) DEFAULT -1 NOT NULL ENABLE," +
            "  VENDOR_ID NUMBER(14,0) DEFAULT NULL," +
            "  MODEL_ID NUMBER(14,0) DEFAULT -1 NOT NULL ENABLE," +
            "  OLD_CATEGORY_ID NUMBER(14,0) DEFAULT NULL," +
            "  PRICE_CHECK NUMBER DEFAULT 0 NOT NULL ENABLE," +
            "  SECOND_CATEGORY_ID NUMBER(14,0) DEFAULT NULL," +
            "  SECOND_PROBABILITY NUMBER DEFAULT NULL," +
            "  MESSAGE VARCHAR2(4000 BYTE)," +
            "  TOVAR_CATEGORY_ID NUMBER(14,0) DEFAULT 0," +
            "  GURU_CATEGORY_ID NUMBER(14,0) DEFAULT -1 NOT NULL ENABLE," +
            "  CLASSIFIER_CATEGORY_ID NUMBER(14,0) DEFAULT NULL," +
            "  MATCHED_CATEGORY_ID NUMBER(14,0) DEFAULT NULL," +
            "  MAPPER_TOVAR_CATEGORY_ID NUMBER(14,0) DEFAULT 0," +
            "  CONTAINS_BAD_WORDS NUMBER(1,0) DEFAULT 0 NOT NULL ENABLE," +
            "  LIGHT_MATCH_TYPE NUMBER(14,0) DEFAULT NULL," +
            "  LIGHT_MODEL_ID NUMBER(14,0) DEFAULT -1 NOT NULL ENABLE," +
            "  LIGHT_MODIFICATION_ID NUMBER(14,0) DEFAULT -1 NOT NULL ENABLE," +
            "  CLASSIFICATION_TYPE VARCHAR2(99 BYTE)," +
            "  SECOND_CLASSIFICATION_TYPE VARCHAR2(99 BYTE)," +
            "  PIC_URLS VARCHAR2(1500 BYTE) DEFAULT ''," +
            "  OFFER_HASH VARCHAR2(32 CHAR)," +
            "  CLUSTER_ID NUMBER(14,0) DEFAULT NULL," +
            "  MARKET_CATEGORY VARCHAR2(4000 BYTE) DEFAULT NULL," +
            "  GROUP_ID NUMBER DEFAULT NULL," +
            "  CLASSIFIER_GOOD_ID VARCHAR2(32 BYTE)," +
            "  LONG_CLUSTER_ID NUMBER(14,0) DEFAULT -1," +
            "  MATCHED_TARGET NUMBER(3,0)," +
            "  DEEP_MATCH_TRASH_SCORE NUMBER" +
            " )" +
            " PARTITION BY RANGE (partition_id) SUBPARTITION BY LIST (subpartition_id)" +
            " SUBPARTITION TEMPLATE");

        ArrayList<String> li = new ArrayList<>();
        for (int i = 0; i < TEST_SUB_PARTITION_COUT; i++) {
            li.add(String.format("SUBPARTITION sp%02d VALUES (%d)", i, i));
        }

        sqlCreateTable.append(" (");
        sqlCreateTable.append(StringUtils.join(li, ", "));
        sqlCreateTable.append(") ");

        li.clear();
        for (int i = 0; i < TEST_PARTITION_COUT; i++) {
            li.add(String.format("PARTITION %s_p%02d VALUES LESS THAN(%d)", TEST_TABLE_BASE, i, i + 1));
        }
        sqlCreateTable.append(" (");
        sqlCreateTable.append(StringUtils.join(li, ", "));
        sqlCreateTable.append(")");

        log.debug(sqlCreateTable);

        siteCatalogJdbcTemplate.update(sqlCreateTable.toString());
        log.info("Table " + TEST_TABLE_BASE + " have been created");
    }

    public void deleteTestTable() {
        siteCatalogJdbcTemplate.execute("DECLARE" +
                "    PROCEDURE drop_if_exists (in_table VARCHAR2) IS" +
                "        table_does_not_exist EXCEPTION;" +
                "        PRAGMA EXCEPTION_INIT(table_does_not_exist, -942);" +
                "    BEGIN" +
                "        EXECUTE IMMEDIATE 'DROP TABLE ' || in_table ;" +
                "        dbms_output.put_line('Table ' || in_table || ' have been dropped');" +
                "        exception when table_does_not_exist then null;" +
                "    END drop_if_exists;" +
                " BEGIN" +
                "  drop_if_exists(?);" +
                " END;",
            (CallableStatementCallback<Void>) cs -> {
                cs.setString(1, TEST_TABLE_BASE);
                cs.execute();
                return null;
            });

        log.info("Table " + TEST_TABLE_BASE + " have been dropped");
    }

    public void fillScLogPartitions() {
        siteCatalogJdbcTemplate.update("declare\n" +
                "  i number;\n" +
                "  j number;\n" +
                "  max_partitions number := ?;\n" +
                "  max_subpartitions number := ?;\n" +
                "  test_table_name varchar2(90) := ?;\n" +
                "  test_session varchar2(16) := ?;\n" +
                "  tmp_session_id varchar2(255);\n" +
                "  tmp_status number := 0;\n" +
                "begin\n" +
                "  delete from sc_log_partitions\n" +
                "    where table_name = test_table_name;\n" +
                "\n" +
                "  for i in 0..max_partitions - 1 loop\n" +
                "    for j in 0..max_subpartitions - 1 loop\n" +
                "      tmp_session_id := '20021101_' || '0' || (i mod 10) || '0' || (j mod 10);\n" +
                "      tmp_status := j mod 5;\n" +
                "      insert into sc_log_partitions (table_name, partition_id," +
                "                                       subpartition_id, session_id, status, updated)\n" +
                "      values (\n" +
                "        test_table_name,\n" +
                "        i,\n" +
                "        j,\n" +
                "        tmp_session_id,\n" +
                "        tmp_status,\n" +
                "        sysdate - dbms_random.value(0, 10)\n" +
                "      );\n" +
                "    end loop;\n" +
                "  end loop;\n" +
                "\n" +
                "  update sc_log_partitions\n" +
                "    set session_id = test_session, status = 4\n" +
                "    where table_name = test_table_name and partition_id = 0 and subpartition_id = 0;\n" +
                "\n" +
                "  commit;\n" +
                "end;\n",
            TEST_PARTITION_COUT, TEST_SUB_PARTITION_COUT, TEST_TABLE_BASE, NOP_SESSION
        );
    }

    public void cleanScLogPartitions() {
        siteCatalogJdbcTemplate.update("delete from sc_log_partitions where table_name = ?", TEST_TABLE_BASE);
    }
}
