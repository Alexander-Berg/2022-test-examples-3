package ru.yandex.market.mboc.common.yt;

import java.sql.ResultSet;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class YqlReaderTest {

    private JdbcTemplate yqlJdbcTemplate;
    private StorageKeyValueServiceMock keyValueService;

    @Test
    public void testStoreFinalSavePointOffset() {
        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        keyValueService = new StorageKeyValueServiceMock();

        var yqlReaderConfig = YqlReader.Config.builder()
            .clearSavePointOffsetOnFinish(false)
            .storeFinalSavePointOffset(true)
            .batchSavePoint(5)
            .build();

        int rowCount = 12;
        mockYqlJdbcTemplate(rowCount);

        var mockYqlReader = new MockYqlReader(yqlJdbcTemplate, keyValueService, yqlReaderConfig);

        mockYqlReader.handleTable("");

        long storedSavePointOffset = mockYqlReader.savePointOffset(mockYqlReader.readerName());
        assertThat(storedSavePointOffset).isEqualTo(rowCount);
    }

    private void mockYqlJdbcTemplate(int rowCount) {
        var resultSetMock = mock(ResultSet.class);
        doAnswer(invocation -> {
            RowCallbackHandler rowCallbackHandler = invocation.getArgument(2);
            for (int i = 0; i < rowCount; i++) {
                rowCallbackHandler.processRow(resultSetMock);
            }
            return null;
        })
            .when(yqlJdbcTemplate).query(
                anyString(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class)
            );
    }

    private static class MockYqlReader extends YqlReader<Object> {

        protected MockYqlReader(JdbcTemplate yqlJdbcTemplate,
                                StorageKeyValueService storageKeyValueService,
                                Config config) {
            super(yqlJdbcTemplate, storageKeyValueService, config);
        }

        @Override
        public String selectSql(Long offset, String table) {
            return "";
        }

        @Override
        public void handleBatch(List<Object> batch) {

        }

        @Override
        public Object extractRow(ResultSet rs) {
            return new Object();
        }
    }
}
