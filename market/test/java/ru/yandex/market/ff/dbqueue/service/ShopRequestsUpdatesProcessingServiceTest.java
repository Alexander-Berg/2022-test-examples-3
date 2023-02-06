package ru.yandex.market.ff.dbqueue.service;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.dbqueue.UpdateRequestPayload;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.LgwRequestService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShopRequestsUpdatesProcessingServiceTest {

    private static final long REQUEST_ID = 123;

    private LgwRequestService lgwRequestService;
    private ShopRequestFetchingService shopRequestFetchingService;
    private ShopRequestsUpdatesProcessingService shopRequestsUpdatesProcessingService;
    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        shopRequestFetchingService = mock(ShopRequestFetchingService.class);
        lgwRequestService = mock(LgwRequestService.class);
        shopRequestsUpdatesProcessingService =
                new ShopRequestsUpdatesProcessingService(lgwRequestService,
                        shopRequestFetchingService);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    void processPayloadForCorrectOrder() {
        ShopRequest request = createShopRequest();
        when(shopRequestFetchingService.getRequestOrThrow(REQUEST_ID)).thenReturn(request);

        shopRequestsUpdatesProcessingService.processPayload(createPayload());

        verify(shopRequestFetchingService).getRequestOrThrow(REQUEST_ID);
        verify(lgwRequestService).updateRequest(request);
    }

    @Nonnull
    private UpdateRequestPayload createPayload() {
        return new UpdateRequestPayload(REQUEST_ID);
    }

    @Nonnull
    private ShopRequest createShopRequest() {
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        return request;
    }
}
