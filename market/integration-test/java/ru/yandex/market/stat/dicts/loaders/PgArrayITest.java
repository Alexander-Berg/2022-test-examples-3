package ru.yandex.market.stat.dicts.loaders;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.stat.dicts.config.MetadataConfig;
import ru.yandex.market.stat.dicts.config.PropertiesDictionariesITestConfig;
import ru.yandex.market.stat.dicts.loaders.jdbc.FieldMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcLoadConfigFromFile;
import ru.yandex.market.stat.dicts.loaders.jdbc.RowMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;
import ru.yandex.market.stat.dicts.loaders.jdbc.TypelessJdbcLoader;
import ru.yandex.market.stat.dicts.loaders.jdbc.db.SqlTypeDefinition;
import ru.yandex.market.stat.dicts.services.DictionaryStorageUtils;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@Slf4j
@ActiveProfiles("integration-tests")
@ContextConfiguration(
    classes = {PropertiesDictionariesITestConfig.class, MetadataConfig.class},
    initializers = LocalPostgresInitializer.class
)
@RunWith(SpringJUnit4ClassRunner.class)
public class PgArrayITest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private DataSource metadataDataSource;

    @Test
    public void supportedArraysFromPgMustNotThrow() throws Exception {
        val sql = "" +
                "SELECT \n" +
                "  ARRAY['a','b'] AS string_array, \n" +
                "  ARRAY[81, 12] AS integer_array, \n" +
                "  ARRAY[82::bigint, 13::bigint] AS long_array, \n" +
                "  ARRAY[81.1, 12.3] AS decimal_array, \n" +
                "  ARRAY['2018-05-09'::date, '2018-05-08'::date] AS date_array, \n" +
                "  ARRAY['2018-05-09'::timestamp, '2018-05-09 00:12:56'::timestamp] AS datetime_array";

        val jdbcTaskDefinition = JdbcLoadConfigFromFile.builder().destination("dual").sql(sql).build().getSingleTaskDefinition();
        val resultList = new ArrayList<>();
        val dictionaryStorage = DictionaryStorageUtils.dictionaryStorageForSaveOnly(resultList);
        val rowMetadata = new RowMetadata("dual", Arrays.asList(
                new FieldMetadata("string_array", "string_array", "java.sql.Array", List.class, new SqlTypeDefinition("_TEXT", Types.ARRAY, Integer.MAX_VALUE, 0, false)),
                new FieldMetadata("integer_array", "integer_array", "java.sql.Array", List.class, new SqlTypeDefinition("_INT4", Types.ARRAY, 10, 0, false)),
                new FieldMetadata("long_array", "long_array", "java.sql.Array", List.class, new SqlTypeDefinition("_INT8", Types.ARRAY, 19, 0, false)),
                new FieldMetadata("decimal_array", "decimal_array", "java.sql.Array", List.class, new SqlTypeDefinition("_NUMERIC", Types.ARRAY, 0, 0, false)),
                new FieldMetadata("date_array", "date_array", "java.sql.Array", List.class, new SqlTypeDefinition("_DATE", Types.ARRAY, 13, 0, false)),
                new FieldMetadata("datetime_array", "datetime_array", "java.sql.Array", List.class, new SqlTypeDefinition("_TIMESTAMP", Types.ARRAY, 29, 6, false))
        ));
        val expectedRecord = new SchemelessDictionaryRecord(rowMetadata,
                ImmutableMap.<String, Object>builder()
                        .put("string_array", Arrays.asList("a", "b"))
                        .put("integer_array", Arrays.asList(81, 12))
                        .put("long_array", Arrays.asList(82L, 13L))
                        .put("decimal_array", Arrays.asList(new BigDecimal("81.1"), new BigDecimal("12.3")))
                        .put("date_array", Arrays.asList(Date.valueOf(LocalDate.of(2018, Month.MAY, 9)),
                                Date.valueOf(LocalDate.of(2018, Month.MAY, 8))))
                        .put("datetime_array", Arrays.asList(Timestamp.valueOf(LocalDate.of(2018, Month.MAY, 9).atStartOfDay()),
                                Timestamp.valueOf(LocalDateTime.of(2018, Month.MAY, 9, 0, 12, 56))))
                .build()
        );

        JdbcTemplate metadataJdbcTemplate = new JdbcTemplate(metadataDataSource);
        TypelessJdbcLoader jdbcLoader = new TypelessJdbcLoader(metadataJdbcTemplate, dictionaryStorage, jdbcTaskDefinition);
        long actualRecordsLoaded = jdbcLoader.load(DEFAULT_CLUSTER, LocalDate.now().atStartOfDay());
        assertThat("Should load exactly one record", actualRecordsLoaded, is(1L));
        assertThat("Not expected values", resultList, containsInAnyOrder(expectedRecord));
    }

    @Test
    public void mixedDateAndDatetimeArraysFromPgMustNotThrow() throws Exception {
        val sql = "" +
                "SELECT \n" +
                "  ARRAY['2018-05-09'::date, '2018-05-09 00:12:56'::timestamp] AS datetime_array";

        val jdbcTaskDefinition = JdbcLoadConfigFromFile.builder().destination("dual").sql(sql).build().getSingleTaskDefinition();
        val resultList = new ArrayList<>();
        val dictionaryStorage = DictionaryStorageUtils.dictionaryStorageForSaveOnly(resultList);
        val rowMetadata = new RowMetadata("dual", Collections.singletonList(
                new FieldMetadata("datetime_array", "datetime_array", "java.sql.Array", List.class, new SqlTypeDefinition("_TIMESTAMP", Types.ARRAY, 29, 6, false))
        ));
        val expectedRecord = new SchemelessDictionaryRecord(rowMetadata,
                ImmutableMap.<String, Object>builder()
                        .put("datetime_array", Arrays.asList(Timestamp.valueOf(LocalDate.of(2018, Month.MAY, 9).atStartOfDay()),
                                Timestamp.valueOf(LocalDateTime.of(2018, Month.MAY, 9, 0, 12, 56))))
                .build()
        );

        JdbcTemplate metadataJdbcTemplate = new JdbcTemplate(metadataDataSource);
        TypelessJdbcLoader jdbcLoader = new TypelessJdbcLoader(metadataJdbcTemplate, dictionaryStorage, jdbcTaskDefinition);
        long actualRecordsLoaded = jdbcLoader.load(DEFAULT_CLUSTER, LocalDate.now().atStartOfDay());
        assertThat("Should load exactly one record", actualRecordsLoaded, is(1L));
        assertThat("Not expected values", resultList, containsInAnyOrder(expectedRecord));
    }

    @Test
    public void arrayOfRecordFromPgMustThrow() throws Exception {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Arrays of type record(1111) are not supported yet");
        val sql = "" +
                "SELECT \n" +
                "  ARRAY['a','b'] AS string_array, \n" +
                "  ARRAY[('b', 1), ('a', 12)] AS object_array";

        val jdbcTaskDefinition = JdbcLoadConfigFromFile.builder().destination("dual").sql(sql).build().getSingleTaskDefinition();
        val resultList = new ArrayList<>();
        val dictionaryStorage = DictionaryStorageUtils.dictionaryStorageForSaveOnly(resultList);

        JdbcTemplate metadataJdbcTemplate = new JdbcTemplate(metadataDataSource);
        TypelessJdbcLoader jdbcLoader = new TypelessJdbcLoader(metadataJdbcTemplate, dictionaryStorage, jdbcTaskDefinition);
        jdbcLoader.load(DEFAULT_CLUSTER, LocalDate.now().atStartOfDay());
    }
}
