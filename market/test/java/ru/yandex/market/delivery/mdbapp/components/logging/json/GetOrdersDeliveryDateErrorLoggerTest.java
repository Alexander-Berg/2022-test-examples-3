package ru.yandex.market.delivery.mdbapp.components.logging.json;

import java.lang.reflect.Field;
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
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateError;

public class GetOrdersDeliveryDateErrorLoggerTest {
    static final Long ORDER_ID_1 = 1001L;
    static final Long ORDER_ID_2 = 1002L;

    ResourceId r1 = new ResourceId(ORDER_ID_1.toString(), "d1");
    ResourceId r2 = new ResourceId(ORDER_ID_2.toString(), "d2");

    private GetOrdersDeliveryDateErrorLogger logger;
    private Logger dataLoggerMock;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        logger = new GetOrdersDeliveryDateErrorLogger();

        dataLoggerMock = Mockito.mock(Logger.class);

        Field dataLogger = AbstractLogger.class.getDeclaredField("dataLogger");
        dataLogger.setAccessible(true);
        dataLogger.set(logger, dataLoggerMock);

    }

    @Test
    public void logNoRequest() {
        logger.logDeliveryDateError(
            new GetOrdersDeliveryDateError("p", 1234L, List.of(r1, r2), "Message"),
            Map.of()
        );

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r1, "Message", "p", 0L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r2, "Message", "p", 0L)));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }

    @Test
    public void logEmptyRequestsList() {
        logger.logDeliveryDateError(
            new GetOrdersDeliveryDateError("p", 1234L, List.of(r1, r2), "Message"),
            Map.of(
                ORDER_ID_1, List.of()
            )
        );

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r1, "Message", "p", 0L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r2, "Message", "p", 0L)));
        Mockito.verifyNoMoreInteractions(dataLoggerMock);
    }

    @Test
    public void logMultipleRequest() {
        logger.logDeliveryDateError(
            new GetOrdersDeliveryDateError("p", 1234L, List.of(r1, r2), "Message"),
            Map.of(
                ORDER_ID_1, List.of(
                    createOrderRequest(1L),
                    createOrderRequest(2L),
                    createOrderRequest(3L)
                ),
                ORDER_ID_2, List.of(
                    createOrderRequest(4L),
                    createOrderRequest(5L)
                )
            )
        );

        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r1, "Message", "p", 1L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r1, "Message", "p", 2L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r1, "Message", "p", 3L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r2, "Message", "p", 4L)));
        Mockito.verify(dataLoggerMock).error(Mockito.argThat(new ArgumentMatcher(r2, "Message", "p", 5L)));
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
        private final ResourceId resourceId;
        private final String error;
        private final String processId;
        private final Long requestConditionId;

        @Override
        public boolean matches(String argument) {
            try {
                Map<String, Object> data = om.readValue(argument, new TypeReference<>() {
                });
                return Objects.equals(data.get("deliveryId"), resourceId.getDeliveryId())
                    && Objects.equals(data.get("yandexId"), resourceId.getYandexId())
                    && Objects.equals(data.get("error"), error)
                    && Objects.equals(data.get("processId"), processId)
                    && Objects.equals(data.get("requestConditionId"), requestConditionId.intValue())
                    && !Objects.isNull(data.get("date"));
            } catch (JsonProcessingException e) {
                log.error("Invalid json {}: ", argument, e);
                return false;
            }
        }
    }
}
