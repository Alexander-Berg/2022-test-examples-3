package ru.yandex.market.checkout.checkouter.degradation;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.degradation.strategy.StockStorageDegradationStrategy;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.service.StockStorageServiceImpl;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsCheckouterService;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;

@ContextConfiguration(classes = {StockStorageServiceImpl.class, StockStorageDegradationStrategy.class})
@MockBean(classes = {
        OrderWritingDao.class,
        OrderReadingDao.class,
        EventService.class,
        TransactionTemplate.class,
        Clock.class,
        ColorConfig.class,
        SettingsCheckouterService.class,
        StockStorageClient.class
})
@TestPropertySource(properties = "market.loyalty.tvm.client_id=0")
public class StockStorageDegradationTest extends AbstractDegradationTest {

    @Autowired
    private StockStorageDegradationStrategy strategy;
    @Autowired
    private StockStorageServiceImpl stockStorageService;
    @MockBean
    @Qualifier("stockStorageServiceExecutor")
    private ExecutorService executorService;
    @Autowired
    private CheckouterProperties properties;
    @Autowired
    private RestTemplate stockStorageRestTemplate;

    @BeforeEach
    void init() {
        log.addAppender(appender);
        appender.clear();
        appender.start();
        when(properties.getEnableStockStorageDegradationStrategy()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void onErrorTest() {

        DegradationContextHolder.setStage(CHECKOUT);

        when(stockStorageRestTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                any(Class.class))).thenThrow(new RuntimeException("some exception"));

        assertThrows(RuntimeException.class, () -> stockStorageService.getAvailableAmounts(List.of(new SSItem()),
                false));

        assertOnErrorLog(strategy.getCallName());
    }

}
