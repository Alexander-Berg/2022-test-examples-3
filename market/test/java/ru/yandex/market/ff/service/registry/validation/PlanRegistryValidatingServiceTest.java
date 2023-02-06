package ru.yandex.market.ff.service.registry.validation;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.LogisticsPoint;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.ShopRequestStatusService;
import ru.yandex.market.ff.service.registry.PlanRegistryValidatingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class PlanRegistryValidatingServiceTest extends IntegrationTest {

    @Autowired
    private PlanRegistryValidatingService planRegistryValidatingService;
    @Autowired
    private ShopRequestStatusService shopRequestStatusService;

    @Test
    @DatabaseSetup("classpath:service/registry/35/before.xml")
    public void checkNormalUpdateStatusFlow() {
        ShopRequest shopRequest = baseShopRequest();
        shopRequest.setType(RequestType.ORDERS_SUPPLY);
        shopRequest.setStatus(RequestStatus.ACCEPTED_BY_SERVICE);
        var newStatus = RequestStatus.PLAN_REGISTRY_CREATED;
        planRegistryValidatingService.updateShopRequestStatusIfNeeded(shopRequest, newStatus);
        Mockito.verify(shopRequestModificationService).updateStatus(shopRequest, newStatus);
        Mockito.verifyNoMoreInteractions(publishCalendarShopRequestChangeProducerService);
    }

    @Test
    @DatabaseSetup("classpath:service/registry/35/before.xml")
    public void checkSendToLogbrokerWithNoShopRequestStatusChange() {
        ShopRequest shopRequest = baseShopRequest();
        shopRequest.setType(RequestType.ORDERS_SUPPLY);
        shopRequest.setStatus(RequestStatus.PROCESSED);
        var newStatus = RequestStatus.PLAN_REGISTRY_CREATED;
        planRegistryValidatingService.updateShopRequestStatusIfNeeded(shopRequest, newStatus);
        Mockito.verifyNoMoreInteractions(publishCalendarShopRequestChangeProducerService);
    }

    @Test
    @DatabaseSetup("classpath:service/registry/35/before.xml")
    public void checkSendToLogbrokerWithAcceptedStatus() {
        ShopRequest shopRequest = baseShopRequest();
        shopRequest.setType(RequestType.ORDERS_SUPPLY);
        shopRequest.setStatus(RequestStatus.PLAN_REGISTRY_ACCEPTED);
        var newStatus = RequestStatus.PLAN_REGISTRY_ACCEPTED;
        planRegistryValidatingService.updateShopRequestStatusIfNeeded(shopRequest, newStatus);
        Mockito.verify(shopRequestStatusService)
                .publishUncheckedStatusChangeEventForShopRequest(eq(shopRequest), eq(shopRequest.getStatus()), any());
    }

    private ShopRequest baseShopRequest() {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        shopRequest.setCreatedAt(LocalDateTime.now().minusDays(2));
        shopRequest.setUpdatedAt(LocalDateTime.now().minusDays(1));
        shopRequest.setLogisticsPoint(new LogisticsPoint(222L));
        shopRequest.setServiceRequestId("1");
        shopRequest.setExternalRequestId("1");
        shopRequest.setServiceId(100L);
        return shopRequest;
    }
}
