package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.sql.ResultSet;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 1/17/22
 */
public class YqlReaderTest extends BaseTest {

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    private JdbcTemplate jdbcTemplate;
    private YqlTestReader yqlTestReader;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        yqlTestReader = new YqlTestReader(jdbcTemplate, storageKeyValueService);
    }

    @Test
    public void testCorrectStoreSavePointOffsetAfterFail() {
        Mockito.doAnswer((Answer<Object>) invocation -> {
            RowCallbackHandler argument = invocation.getArgument(2);
            for (int i = 0; i < 10500; i++) {
                argument.processRow(Mockito.mock(ResultSet.class));
            }
            throw new RuntimeException();
        }).when(jdbcTemplate).query(
                Mockito.anyString(),
                Mockito.any(PreparedStatementSetter.class),
                Mockito.any(RowCallbackHandler.class)
        );

        try {
            yqlTestReader.handleTable("test");
            Assertions.fail("must be failed");
        } catch (Exception e) {
            Long offset = storageKeyValueService.getLong("save_point_offset_FULL", 0L);
            Assertions.assertThat(offset).isEqualTo(10000L);
            Assertions.assertThat(yqlTestReader.atomicInteger.get()).isEqualTo(10500);
        }
    }

    @Test
    public void testClearOffsetAfterComplete() {
        Mockito.doAnswer((Answer<Object>) invocation -> {
            RowCallbackHandler argument = invocation.getArgument(2);
            for (int i = 0; i < 10500; i++) {
                argument.processRow(Mockito.mock(ResultSet.class));
            }
            return null;
        }).when(jdbcTemplate).query(
                Mockito.anyString(),
                Mockito.any(PreparedStatementSetter.class),
                Mockito.any(RowCallbackHandler.class)
        );
        yqlTestReader.handleTable("test");
        Assertions.assertThat(storageKeyValueService.getLong("save_point_offset_FULL", 0L))
                .isEqualTo(0L);
    }

    @Test
    public void testStartFromOffset() {
        Mockito.doAnswer((Answer<Object>) invocation -> {
            RowCallbackHandler argument = invocation.getArgument(2);
            for (int i = 0; i < 10500; i++) {
                argument.processRow(Mockito.mock(ResultSet.class));
            }
            return null;
        }).when(jdbcTemplate).query(
                Mockito.anyString(),
                Mockito.any(PreparedStatementSetter.class),
                Mockito.any(RowCallbackHandler.class)
        );
        long intOffset = 20000L;
        storageKeyValueService.putValue("save_point_offset_FULL", intOffset);
        yqlTestReader.handleTable("test");
        Assertions.assertThat(yqlTestReader.inputOffset).isEqualTo(intOffset);
    }

}
