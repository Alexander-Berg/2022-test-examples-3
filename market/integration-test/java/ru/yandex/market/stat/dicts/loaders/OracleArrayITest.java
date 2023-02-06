package ru.yandex.market.stat.dicts.loaders;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.loaders.jdbc.FieldMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcLoadConfigFromFile;
import ru.yandex.market.stat.dicts.loaders.jdbc.RowMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;
import ru.yandex.market.stat.dicts.loaders.jdbc.TypelessJdbcLoader;
import ru.yandex.market.stat.dicts.loaders.jdbc.db.SqlTypeDefinition;
import ru.yandex.market.stat.dicts.services.DictionaryStorageUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@Slf4j
@ActiveProfiles("integration-tests")
@ContextConfiguration(classes = {DictionariesITestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class OracleArrayITest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Autowired
    private JdbcTemplate mbiJdbcTemplate;

    @Test
    @Ignore("Access required")
    public void arrayFromOracleMustLoadArray() throws Exception {
        val sql = "" +
                "SELECT SYS.ODCIVARCHAR2LIST('a', 'b') AS string_list, \n" +
                "SYS.ODCINUMBERLIST(81, 12) AS integer_list, \n" +
                "SYS.ODCINUMBERLIST(81.1, 12.3) AS decimal_list, \n" +
                "SYS.ODCIDATELIST(to_DATE('2018-05-09', 'YYYY-MM-DD'), to_timestamp('2018-05-09 00:12:56', 'YYYY-MM-DD HH24:MI:SS')) AS date_list, \n" +
                "SYS.ODCIOBJECTLIST() as object_list \n" +
                "FROM dual";
        val jdbcTaskDefinition = JdbcLoadConfigFromFile.builder().destination("dual").sql(sql).build().getSingleTaskDefinition();
        val resultList = new ArrayList<>();
        val dictionaryStorage = DictionaryStorageUtils.dictionaryStorageForSaveOnly(resultList);
        val rowMetadata = new RowMetadata("dual", Arrays.asList(
                new FieldMetadata("string_list", "string_list", "oracle.jdbc.OracleArray", List.class, new SqlTypeDefinition("SYS.ODCIVARCHAR2LIST", Types.ARRAY, 0, 0, false)),
                new FieldMetadata("integer_list", "integer_list", "oracle.jdbc.OracleArray", List.class, new SqlTypeDefinition("SYS.ODCINUMBERLIST", Types.ARRAY, 0, 0, false)),
                new FieldMetadata("decimal_list", "decimal_list", "oracle.jdbc.OracleArray", List.class, new SqlTypeDefinition("SYS.ODCINUMBERLIST", Types.ARRAY, 0, 0, false)),
                new FieldMetadata("date_list", "date_list", "oracle.jdbc.OracleArray", List.class, new SqlTypeDefinition("SYS.ODCIDATELIST", Types.ARRAY, 0, 0, false)),
                new FieldMetadata("object_list", "object_list", "oracle.jdbc.OracleArray", List.class, new SqlTypeDefinition("SYS.ODCIOBJECTLIST", Types.ARRAY, 0, 0, false))
        ));
        val expectedRecord = new SchemelessDictionaryRecord(rowMetadata,
                ImmutableMap.of("string_list", Arrays.asList("a", "b"),
                        "integer_list", Arrays.asList(new BigDecimal(81), new BigDecimal(12)),
                        "decimal_list", Arrays.asList(new BigDecimal("81.1"), new BigDecimal("12.3")),
                        "date_list", Arrays.asList(Timestamp.valueOf(LocalDate.of(2018, Month.MAY, 9).atStartOfDay()),
                                Timestamp.valueOf(LocalDateTime.of(2018, Month.MAY, 9, 0, 12, 56))),
                        "object_list", Collections.emptyList()
                ));

        TypelessJdbcLoader jdbcLoader = new TypelessJdbcLoader(mbiJdbcTemplate, dictionaryStorage, jdbcTaskDefinition);
        long actualRecordsLoaded = jdbcLoader.load(DEFAULT_CLUSTER, LocalDate.now().atStartOfDay());
        assertThat("Should load exactly one record", actualRecordsLoaded, is(1L));
        assertThat("Not expected values", resultList, containsInAnyOrder(expectedRecord));
    }
}
