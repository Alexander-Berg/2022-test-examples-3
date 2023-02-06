package ru.yandex.market.pvz.core.domain.request;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.request.RequestAsyncType.FIND_PICKUP_POINT_BY_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_NAME;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RequestAsyncHandlerTest {

    private final TestPickupPointFactory pickupPointFactory;

    private final RequestAsyncQueryService requestAsyncQueryService;

    private final RequestAsyncHandler requestAsyncHandler;

    @Test
    void requestSuccess() throws InterruptedException {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        long requestId = requestAsyncHandler.request(FIND_PICKUP_POINT_BY_ID, Map.of("id", pickupPoint.getId()));
        assertThat(requestId).isPositive();

        var requestAsync = requestAsyncQueryService.get(requestId);
        assertThat(requestAsync.getId()).isEqualTo(requestId);
        assertThat(requestAsync.getRequestType()).isEqualTo(FIND_PICKUP_POINT_BY_ID);

        while (true) {
            Thread.sleep(1000);
            requestAsync = requestAsyncQueryService.get(requestId);
            if (requestAsync.getResponseCode() != null) {
                assertThat(requestAsync.getResponseCode()).isEqualTo(200);
                assertThat(requestAsync.getResponseError()).isNull();
                assertThat(requestAsync.getResponsePayload()).isNotNull();
                assertThat(requestAsync.getResponsePayload()).isExactlyInstanceOf(LinkedHashMap.class);
                assertThat(((LinkedHashMap<String, Object>) requestAsync.getResponsePayload()).get("id").toString())
                        .isEqualTo(pickupPoint.getId().toString());
                assertThat(((LinkedHashMap<String, Object>) requestAsync.getResponsePayload()).get("name"))
                        .isEqualTo(DEFAULT_NAME);
                break;
            }
        }
    }

    @Test
    void requestError() throws InterruptedException {
        long requestId = requestAsyncHandler.request(FIND_PICKUP_POINT_BY_ID, Map.of("id", 100L));
        assertThat(requestId).isPositive();

        var requestAsync = requestAsyncQueryService.get(requestId);
        assertThat(requestAsync.getId()).isEqualTo(requestId);
        assertThat(requestAsync.getRequestType()).isEqualTo(FIND_PICKUP_POINT_BY_ID);

        while (requestAsync.getResponseCode() == null) {
            Thread.sleep(1000);
            requestAsync = requestAsyncQueryService.get(requestId);
        }

        assertThat(requestAsync.getResponseCode()).isEqualTo(500);
        assertThat(requestAsync.getResponseError()).isNotNull();
        assertThat(requestAsync.getResponsePayload()).isNull();
    }
}
