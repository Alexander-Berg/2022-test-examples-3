package ru.yandex.market.wrap.infor.repository;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.repository.stock.StockChangeEventMonitoringRepository;

import java.util.Map;

public class StockChangeEventMonitoringRepositoryTest extends AbstractContextualTest {

    @Autowired
    private StockChangeEventMonitoringRepository repository;

    @Autowired
    private TokenContextHolder tokenContextHolder;

    @Before
    public void setUp() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    @After
    public void tearDown() {
        tokenContextHolder.clearToken();
    }

    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/integration/change_events/transmitlog_with_unprocessed_events.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @Test
    public void happyPath() {
        Map<String, Long> unprocessedEvents = repository.findUnprocessedEvents();
        softly.assertThat(unprocessedEvents).isNotNull();
        softly.assertThat(unprocessedEvents.keySet()).containsExactlyInAnyOrder("DEPOSIT", "ADJUSTMENT");
        softly.assertThat(unprocessedEvents.get("ADJUSTMENT")).isEqualTo(1);
        softly.assertThat(unprocessedEvents.get("DEPOSIT")).isEqualTo(2);
    }
}
