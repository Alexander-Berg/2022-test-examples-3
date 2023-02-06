package ru.yandex.market.global.partner.api;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.test.TestUtil;
import ru.yandex.market.global.partner.BaseApiTest;
import ru.yandex.market.global.partner.domain.business.BusinessCommandServiceIndexingImpl;
import ru.yandex.market.global.partner.domain.delivery.tariff.DeliveryTariffCommandServiceIndexingImpl;
import ru.yandex.market.global.partner.domain.localstores.LocalstoresService;
import ru.yandex.market.global.partner.domain.request.IdempotentRequestService;
import ru.yandex.market.global.partner.domain.shop.ShopCommandServiceIndexingImpl;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AdminApiServiceTest extends BaseApiTest {
    private final AdminApiService adminApiService;

    private final ShopCommandServiceIndexingImpl shopCommandServiceIndexing;
    private final BusinessCommandServiceIndexingImpl businessCommandServiceIndexing;
    private final DeliveryTariffCommandServiceIndexingImpl deliveryTariffCommandServiceIndexing;
    private final IdempotentRequestService idempotentRequestService;

    @Test
    public void testIdempotency() {
        LocalstoresService localstoresService = Mockito.mock(LocalstoresService.class);

        AdminApiService adminApiService = new AdminApiService(
                shopCommandServiceIndexing,
                businessCommandServiceIndexing,
                deliveryTariffCommandServiceIndexing,
                localstoresService,
                idempotentRequestService
        );

        TestUtil.doParallelCalls(3,
                () -> adminApiService.apiV1AdminLocalstoresProcessPost("LOCALSTORES-123")
        );

        Mockito.verify(localstoresService, Mockito.times(1))
                .processLocalstoresTicket(Mockito.anyString());

        Mockito.reset(localstoresService);
        adminApiService.apiV1AdminLocalstoresProcessPost("LOCALSTORES-123");

        Mockito.verify(localstoresService, Mockito.times(1))
                .processLocalstoresTicket(Mockito.anyString());

    }
}
