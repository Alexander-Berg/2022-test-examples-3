package ru.yandex.market.checkout.checkouter.degradation;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.checkout.checkouter.balance.service.CreateServiceProductCacheDao;
import ru.yandex.market.checkout.checkouter.balance.service.MultiColorBalanceTokenProvider;
import ru.yandex.market.checkout.checkouter.balance.service.TrustServiceImpl;
import ru.yandex.market.checkout.checkouter.balance.trust.CachedTrustAPI;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.degradation.strategy.trust.TrustCashbackDegradationStrategy;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;

@ContextConfiguration(classes = {TrustServiceImpl.class, TrustCashbackDegradationStrategy.class})
@MockBean(classes = {
        CreateServiceProductCacheDao.class,
        CachedTrustAPI.class,
        ColorConfig.class,
        MultiColorBalanceTokenProvider.class
})
@TestPropertySource(properties = {
        "market.checkout.managed-degradation.trust.cashback.timeout=500"
})
public class TrustCashbackDegradationTest extends AbstractDegradationTest {

    @Autowired
    private TrustServiceImpl trustService;
    @Autowired
    private TrustCashbackDegradationStrategy strategy;
    @Autowired
    private CheckouterProperties properties;
    @MockBean
    @Qualifier("trustGatewayTvmTicketProvider")
    private TvmTicketProvider tvmTicketProvider;

    @BeforeEach
    void init() {
        log.addAppender(appender);
        appender.clear();
        appender.start();
        when(properties.getEnableTrustCashbackDegradationStrategy()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void onErrorTest() {
        doThrow(new RuntimeException("Some exception")).when(properties).getEnableTrustGateway();

        DegradationContextHolder.setStage(CHECKOUT);

        var cashbackInfo = trustService.getYandexCashbackBalance(0L, Set.of());

        assertEquals(new BigDecimal(0), cashbackInfo.getBalance());
        assertNull(cashbackInfo.getAccountPaymethodId());
        assertOnErrorLog(strategy.getCallName());
    }

    @Test
    void onTimeoutTest() {
        when(properties.getEnableTrustGateway()).then(invocation -> {
            Thread.sleep(1000);
            throw new RuntimeException("Some exception");
        });

        DegradationContextHolder.setStage(CHECKOUT);

        var cashbackInfo = trustService.getYandexCashbackBalance(0L, Set.of());

        assertEquals(new BigDecimal(0), cashbackInfo.getBalance());
        assertNull(cashbackInfo.getAccountPaymethodId());
        assertOnTimeoutLog(strategy.getCallName());
    }
}
