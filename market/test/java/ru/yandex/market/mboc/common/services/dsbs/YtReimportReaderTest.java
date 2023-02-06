package ru.yandex.market.mboc.common.services.dsbs;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author apluhin
 * @created 2/10/22
 */
public class YtReimportReaderTest extends BaseDbTestClass {

    private JdbcTemplate yqlJdbcTemplate;

    YtReimportReader ytReimportReader;

    private static final String EXPORT_PATH = "//test";
    @Before
    public void setUp() throws Exception {
        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        ytReimportReader = new YtReimportReader(
            yqlJdbcTemplate
        );
    }

    @Test
    public void testNextReimportTask() {
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        Mockito.doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            for (ResultSet rs : generateIds()) {
                handler.processRow(rs);
            }
            return null;
        }).when(yqlJdbcTemplate).query(query.capture(), Mockito.any(RowCallbackHandler.class));
        LocalDate now = LocalDate.now();
        ReimportTask reimportTask = ytReimportReader.nextReimportTask(EXPORT_PATH, now);
        Assertions.assertThat(reimportTask.getOfferIds()).isNotEmpty();

        Assertions.assertThat(query.getValue()).isEqualTo(
            String.format("select id, offer_id from `//test/%s` order by id", now)
        );
    }

    private List<ResultSet> generateIds() {
        return LongStream.range(0, 100).boxed().map(mockResultSet()).collect(Collectors.toList());
    }

    private Function<Long, ResultSet> mockResultSet() {
        return id -> {
            ResultSet mock = Mockito.mock(ResultSet.class);
            try {
                Mockito.when(mock.getLong(Mockito.eq("offer_id"))).thenReturn(id);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            return mock;
        };
    }
}
