package ru.yandex.market.monitoring.db;

import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.juggler.JugglerClient;
import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class DbMonitoringUpdaterExecutorTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate template;

    @Mock
    private JugglerClient jugglerClient;

    @Captor
    ArgumentCaptor<List<JugglerEvent>> jugglerEventCaptor;

    private DbMonitoringUpdaterExecutor executor;

    AutoCloseable autoclosable;

    @BeforeEach
    public void init() throws Exception {
        autoclosable = MockitoAnnotations.openMocks(this);
        executor = new DbMonitoringUpdaterExecutor(template, jugglerClient, "");
    }

    @Test
    @DbUnitDataSet(before = "DbMonitoringUpdaterExecutorTest.before.csv")
    public void testMonitor() throws IOException {
        executor.doJob(null);
        var expected = List.of(
                new JugglerEvent("test1", "test1", JugglerEvent.Status.OK, "OK"),
                new JugglerEvent("test2", "test2", JugglerEvent.Status.OK, "OK"),
                new JugglerEvent("test3", "test3", JugglerEvent.Status.OK, "OK")
        );
        List<String> result = template.queryForList("select host " +
                "from monitor.monitoring where last_run < current_timestamp + make_interval(mins := 5)", String.class);
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test3"));
        assertEquals(2, result.size());
        verify(jugglerClient).sendEvents(jugglerEventCaptor.capture());
        List<JugglerEvent> capturedJugglerEvents = jugglerEventCaptor.getValue();
        Assertions
                .assertThat(capturedJugglerEvents)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testAll() throws IOException {
        executor.doJob(null);
        verify(jugglerClient).sendEvents(jugglerEventCaptor.capture());
        List<JugglerEvent> capturedJugglerEvents = jugglerEventCaptor.getValue();
        assertTrue(capturedJugglerEvents.size() > 30);
    }

    @AfterEach
    public void destroy() throws Exception {
        autoclosable.close();
    }
}
