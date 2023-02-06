package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.common.util.date.DateUtil.asDate;

@ActiveProfiles("goe-processing")
class ReceiptOrderDelayTest extends FunctionalTest
        implements ResourceHttpUtilitiesMixin, ResourceUtilitiesMixin {

    public static final String RESOURCES_PREFIX = "resources/checkouter_response/events/";

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void setup() {
        this.strategy = Mockito.spy(new GetOrderEventsStrategy(eventProcessorSupportFactory.createSupport()));
    }

    @Test
    @DbUnitDataSet(
            before = {"db/environmentDelay.csv", "db/newReceiptEvent.before.csv"}
    )
    @DisplayName("Перезабор чека без задержки.")
    void readReceiptWithoutDelay() throws IOException {
        List<OrderHistoryEvent> events = events("receipt/events.delay.json");
        strategy.process(events, asDate(LocalDate.of(2017, 1, 2)));
        Mockito.verify(strategy, Mockito.times(0)).sleep(Mockito.anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = {"db/environmentDelay.csv", "db/newReceiptEvent.before.csv"}
    )
    @DisplayName("Перезабор чека с задержкой.")
    void readReceiptWithDelay() throws IOException {
        List<OrderHistoryEvent> events = events("receipt/events.delay.json");

        // Чтобы не было реальной задержки в тестах
        Mockito.doNothing().when(strategy).sleep(Mockito.anyLong());

        strategy.process(events, asDate(LocalDateTime.of(2017, 1, 1, 0, 0, 10)));
        Mockito.verify(strategy, Mockito.times(1)).sleep(Mockito.anyLong());
    }

    @Override
    public RestTemplate getRestTemplate() {
        return checkouterRestTemplate;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCES_PREFIX;
    }
}
