package ru.yandex.market.checkout.checkouter.degradation;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.degradation.strategy.yaLavka.YaLavkaDeliveryCheckingDegradationStrategy;
import ru.yandex.market.checkout.checkouter.metrics.CheckouterRestTemplateBuilder;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryServiceServiceImpl;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaRequestBuilder;
import ru.yandex.market.checkout.checkouter.service.yalavka.client.YaLavkaDeliveryServiceClientImpl;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.common.taxi.model.DeliveryOptionsCheckRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;

@ContextConfiguration(classes = {
        YaLavkaDeliveryServiceServiceImpl.class,
        YaLavkaDeliveryServiceClientImpl.class,
        YaLavkaDeliveryCheckingDegradationStrategy.class
})
@MockBean(classes = {
        YaLavkaRequestBuilder.class,
        CheckouterRestTemplateBuilder.class
})
public class YaLavkaDegradationTest extends AbstractDegradationTest {

    @Autowired
    private YaLavkaDeliveryServiceServiceImpl yaLavkaDeliveryService;
    @Autowired
    private YaLavkaDeliveryCheckingDegradationStrategy strategy;
    @Autowired
    private YaLavkaRequestBuilder requestBuilder;
    @Autowired
    private CheckouterProperties properties;
    @Autowired
    private RestTemplate yaLavkaRestTemplate;
    @MockBean
    @Qualifier("taxiDispatcherTvmTicketProvider")
    private TvmTicketProvider tvmTicketProvider;

    @BeforeEach
    void init() {
        log.addAppender(appender);
        appender.clear();
        appender.start();
        when(properties.getEnableLavkaDeliveryCheckDegradationStrategy()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void onErrorTest() {

        when(requestBuilder.validateAndBuildOptionCheckRequest(any(), any(), any(), any()))
                .thenReturn(new DeliveryOptionsCheckRequest(List.of(), List.of(), null));
        when(yaLavkaRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new RuntimeException("Some exception"));

        DegradationContextHolder.setStage(CHECKOUT);

        yaLavkaDeliveryService.getAvailableDeliveryIntervals(null, null, "lat:0;lon:0;", null);

        assertOnErrorLog(strategy.getCallName());
    }

}
