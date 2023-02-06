package ru.yandex.market.delivery.mdbapp.components.logging.json;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import ru.yandex.market.delivery.mdbapp.components.logging.AbstractLogger;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;
import ru.yandex.market.delivery.mdbclient.model.delivery.OrderDeliveryDate;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateResult;

import static org.mockito.Mockito.spy;

public class GetOrdersDeliveryDateResultLoggerTest {
    public static final long DELIVERY_SERVICE_ID = 1234L;
    final Long orderId1 = 1001L;
    final Long orderId2 = 1002L;
    final ResourceId r1 = new ResourceId(orderId1.toString(), "11");
    final ResourceId r2 = new ResourceId(orderId2.toString(), "22");
    final ZoneOffset offset = ZoneOffset.ofHours(3);
    final OrderDeliveryDate dd1 = new OrderDeliveryDate(r1,
        OffsetDateTime.of(2020, 7, 15, 0, 0, 0, 0, offset),
        OffsetTime.of(10, 0, 0, 0, offset),
        OffsetTime.of(13, 0, 0, 0, offset),
        "Message 1");
    final OrderDeliveryDate dd2 = new OrderDeliveryDate(r2,
        OffsetDateTime.of(2020, 7, 16, 0, 0, 0, 0, offset),
        OffsetTime.of(11, 30, 0, 0, offset),
        OffsetTime.of(15, 15, 0, 0, offset),
        "Message 2");

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private GetOrdersDeliveryDateResultLogger logger;
    private Logger dataLoggerMock;


    @Before
    public void setUp() throws Exception {
        logger = spy(new GetOrdersDeliveryDateResultLogger());

        dataLoggerMock = Mockito.mock(Logger.class);

        Field dataLogger = AbstractLogger.class.getDeclaredField("dataLogger");
        dataLogger.setAccessible(true);
        dataLogger.set(logger, dataLoggerMock);
    }

    @Test
    public void logNoRequests() {
        var result = new GetOrdersDeliveryDateResult("p", DELIVERY_SERVICE_ID, List.of(dd1, dd2));
        logger.logDeliveryDateResult(result, Map.of(
            orderId1, List.of(),
            orderId2, List.of()
        ));

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd1, "p", DELIVERY_SERVICE_ID, 0L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd2, "p", DELIVERY_SERVICE_ID, 0L)));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }


    @Test
    public void logEmptyRequests() {
        var result = new GetOrdersDeliveryDateResult("p", DELIVERY_SERVICE_ID, List.of(dd1, dd2));
        logger.logDeliveryDateResult(result, Map.of(
            orderId1, List.of(),
            orderId2, List.of()
        ));

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd1, "p", DELIVERY_SERVICE_ID, 0L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd2, "p", DELIVERY_SERVICE_ID, 0L)));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }

    @Test
    public void logHasRequests() {
        var result = new GetOrdersDeliveryDateResult("p", DELIVERY_SERVICE_ID, List.of(dd1, dd2));
        logger.logDeliveryDateResult(result, Map.of(
            orderId1, List.of(
                createOrderRequest(1L),
                createOrderRequest(2L),
                createOrderRequest(3L)
            ),
            orderId2, List.of(
                createOrderRequest(4L),
                createOrderRequest(5L)
            )
        ));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd1, "p", DELIVERY_SERVICE_ID, 1L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd1, "p", DELIVERY_SERVICE_ID, 2L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd1, "p", DELIVERY_SERVICE_ID, 3L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd2, "p", DELIVERY_SERVICE_ID, 4L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(dd2, "p", DELIVERY_SERVICE_ID, 5L)));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }

    private OrderRequest createOrderRequest(long id) {
        return new OrderRequest().setRequestCondition(new RequestCondition().setId(id));
    }

    @RequiredArgsConstructor
    @Slf4j
    @Ignore
    static class ArgumentMatcher implements org.mockito.ArgumentMatcher<String> {
        private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        private final OrderDeliveryDate orderDeliveryDate;
        private final String processId;
        private final Long deliveryServiceId;
        private final Long requestConditionId;

        @Override
        public boolean matches(String argument) {
            try {
                Map<String, Object> data = om.readValue(argument, new TypeReference<>() {
                });
                return Objects.equals(data.get("processId"), processId)
                    && Objects.equals(data.get("deliveryServiceId"), deliveryServiceId.intValue())
                    && Objects.equals(
                        data.get("orderId"),
                        Long.valueOf(orderDeliveryDate.getOrderIdAsLong()).intValue()
                    )
                    && Objects.equals(data.get("deliveryId"), orderDeliveryDate.getOrderId().getDeliveryId())
                    && Objects.equals(data.get("requestConditionId"), requestConditionId.intValue())
                    && Objects.equals(
                        data.get("deliveryDate"),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(orderDeliveryDate.getDeliveryDate())
                    )
                    && Objects.equals(data.get("deliveryFromTime"), orderDeliveryDate.getDeliveryFromTime().toString())
                    && Objects.equals(data.get("deliveryToTime"), orderDeliveryDate.getDeliveryToTime().toString())
                    && Objects.equals(data.get("message"), orderDeliveryDate.getMessage());
            } catch (JsonProcessingException e) {
                log.error("Invalid json {}: ", argument, e);
                return false;
            }
        }
    }
}
