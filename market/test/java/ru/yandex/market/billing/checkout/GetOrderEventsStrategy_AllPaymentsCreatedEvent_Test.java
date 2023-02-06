package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.buildPartiallyMockedStrategy;

/**
 * Обработка события {@link HistoryEventType#ALL_PAYMENTS_CREATED}.
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/environmentZeroDelay.csv"})
public class GetOrderEventsStrategy_AllPaymentsCreatedEvent_Test extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/all_payments_created/";
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;
    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void before() {
        strategy = buildPartiallyMockedStrategy(eventProcessorSupportFactory.createSupport());
    }

    @DisplayName("Тест обновления поля cpa_order.all_payments_created на событие HistoryEventType.ALL_PAYMENTS_CREATED.")
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_allPaymentsCreated.before.csv",
            after = "db/GetOrderEventsStrategy_allPaymentsCreated.after.csv"
    )
    void test_process_AllPaymentsCreated_should_updateOrderAllPaymentsCreated() throws IOException {
        List<OrderHistoryEvent> events = events(
                "processingToProcessing-allPaymentsCreated.json"
        );

        strategy.process(events, DATE_2017_01_01);
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCE_PREFIX;
    }
}
