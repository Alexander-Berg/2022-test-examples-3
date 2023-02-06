package ru.yandex.market.delivery.rupostintegrationapp.service.implementation;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.entities.request.ds.DsGetOrderHistoryRequest;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.exception.RussianPostIntegrationAppException;
import ru.yandex.market.delivery.russianposttracker.RussianPostOperationHistoryClient;

import static org.mockito.Mockito.verify;

/**
 * Тест для {@link RussianGetOrderHistoryService}.
 */
@ExtendWith(MockitoExtension.class)
class GetOrderHistoryServiceTest extends BaseTest {

    private static final String YANDEX_ID = "Y505";

    @Mock
    private RussianPostOperationHistoryClient client;
    @InjectMocks
    private RussianGetOrderHistoryService russianGetOrderHistoryService;
    @InjectMocks
    private InternationalGetOrderHistoryService internationalGetOrderHistoryService;
    @InjectMocks
    private FfRussianGetOrderHistoryService ffRussianGetOrderHistoryService;

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("deliveryId", null, null, "deliveryId", null),
            Arguments.of("deliveryId", "fulfillmentId", null, "deliveryId", null),
            Arguments.of("deliveryId", null, "partnerId", "deliveryId", null),
            Arguments.of("deliveryId", "fulfillmentId", "partnerId", "deliveryId", null),
            Arguments.of(null, "fulfillmentId", null, "fulfillmentId", null),
            Arguments.of(null, "fulfillmentId", "partnerId", "fulfillmentId", null),
            Arguments.of(null, null, "partnerId", "partnerId", null),
            Arguments.of(null, null, null, null, RussianPostIntegrationAppException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void testRussianGetOrderHistory(
        String deliveryId,
        String fulfillmentId,
        String partnerId,
        String expectedId,
        Class<? extends Exception> expectedException
    ) {
        DsGetOrderHistoryRequest dsGetOrderHistoryRequest = prepareRequest(
            deliveryId,
            fulfillmentId,
            partnerId
        );
        if (expectedException != null) {
            softly.assertThatThrownBy(() -> russianGetOrderHistoryService.doJob(dsGetOrderHistoryRequest))
                .isInstanceOf(expectedException)
                .hasMessageContaining(YANDEX_ID);
        } else {
            russianGetOrderHistoryService.doJob(dsGetOrderHistoryRequest);
            verify(client).getOrderHistory(expectedId);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void testInternationalGetOrderHistory(
        String deliveryId,
        String fulfillmentId,
        String partnerId,
        String expectedId,
        Class<? extends Exception> expectedException
    ) {
        DsGetOrderHistoryRequest dsGetOrderHistoryRequest = prepareRequest(
            deliveryId,
            fulfillmentId,
            partnerId
        );
        if (expectedException != null) {
            softly.assertThatThrownBy(() -> internationalGetOrderHistoryService.doJob(dsGetOrderHistoryRequest))
                .isInstanceOf(expectedException)
                .hasMessageContaining(YANDEX_ID);
        } else {
            internationalGetOrderHistoryService.doJob(dsGetOrderHistoryRequest);
            verify(client).getOrderHistory(expectedId);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void testFfRussianGetOrderHistory(
        String deliveryId,
        String fulfillmentId,
        String partnerId,
        String expectedId,
        Class<? extends Exception> expectedException
    ) {
        DsGetOrderHistoryRequest dsGetOrderHistoryRequest = prepareRequest(
            deliveryId,
            fulfillmentId,
            partnerId
        );
        if (expectedException != null) {
            softly.assertThatThrownBy(() -> ffRussianGetOrderHistoryService.doJob(dsGetOrderHistoryRequest))
                .isInstanceOf(expectedException)
                .hasMessageContaining(YANDEX_ID);
        } else {
            ffRussianGetOrderHistoryService.doJob(dsGetOrderHistoryRequest);
            verify(client).getOrderHistory(expectedId);
        }
    }

    private DsGetOrderHistoryRequest prepareRequest(
        String deliveryId,
        String fulfillmentId,
        String partnerId
    ) {
        ResourceId orderId = new ResourceId();
        orderId.setYandexId(YANDEX_ID);
        orderId.setDeliveryId(deliveryId);
        orderId.setFulfillmentId(fulfillmentId);
        orderId.setPartnerId(partnerId);

        DsGetOrderHistoryRequest dsGetOrderHistoryRequest = new DsGetOrderHistoryRequest();
        DsGetOrderHistoryRequest.RequestContent requestContent = dsGetOrderHistoryRequest.new RequestContent();
        requestContent.setOrderId(orderId);
        dsGetOrderHistoryRequest.setRequestContent(requestContent);

        return dsGetOrderHistoryRequest;
    }
}
