package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.wrap.infor.service.solomon.SolomonPushService;
import ru.yandex.market.wrap.infor.service.stocks.StockChangeEventsMonitoringService;
import ru.yandex.market.wrap.infor.util.ContextHolderUtil;

import static org.mockito.Mockito.verify;


public class StockChangeEventsMonitoringServiceTest extends AbstractFunctionalTest {


    @Autowired
    SolomonPushService solomonPushService;

    @Autowired
    StockChangeEventsMonitoringService service;

    @Autowired
    TokenContextHolder tokenContextHolder;

    @After
    public void tearDown() {
        tokenContextHolder.clearToken();
    }

    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/change_events_to_solomon/transmitlog_with_unprocessed_events.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @Test
    public void happyPath() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ContextHolderUtil.runWithToken(
            tokenContextHolder,
            "xxxxxxxxxxxxxxxxxxxxxxxxxTestTokenxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            service::run);
        verify(solomonPushService).push(captor.capture());
        String encodedParameter = captor.getValue();
        softly.assertThat(encodedParameter).isNotBlank();
        String expected = readResource("fixtures/functional/change_events_to_solomon/encoded_happy_path.json");
        softly.assertThat(encodedParameter).contains(expected);
    }

    @Test
    public void noUnprocessedEvents() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ContextHolderUtil.runWithToken(
            tokenContextHolder,
            "xxxxxxxxxxxxxxxxxxxxxxxxxTestTokenxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            service::run);
        verify(solomonPushService).push(captor.capture());
        String encodedParameter = captor.getValue();
        softly.assertThat(encodedParameter).containsPattern("\\{\"ts\":[0-9]*}");
    }
}
