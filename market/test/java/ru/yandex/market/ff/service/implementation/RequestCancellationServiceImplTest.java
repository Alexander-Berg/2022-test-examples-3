package ru.yandex.market.ff.service.implementation;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.service.RequestCancellationService;

public class RequestCancellationServiceImplTest extends IntegrationTest {

    @Autowired
    private RequestCancellationService requestCancellationService;

    @Test
    @DatabaseSetup("classpath:service/request-cancellation/2/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-cancellation/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void requestCancellationPlanRegistrySentRequestTest() {
        requestCancellationService.requestCancellation(1L);
    }

    @Test
    @DatabaseSetup("classpath:service/request-cancellation/3/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-cancellation/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void requestCancellationPlanRegistryAcceptedRequestTest() {
        requestCancellationService.requestCancellation(1L);
    }

    @Test
    @DatabaseSetup("classpath:service/request-cancellation/4/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-cancellation/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processCancellationRequestedAfterAcceptByServiceTest() {
        requestCancellationService.processCancellationRequested(getShopRequest());
    }

    private ShopRequest getShopRequest() {
        ShopRequest request = new ShopRequest();
        request.setId(1L);
        request.setType(RequestType.SUPPLY);
        request.setStatus(RequestStatus.ACCEPTED_BY_SERVICE);
        request.setSupplier(new Supplier(1, "supplier1", null, null, null, new SupplierBusinessType()));
        request.setRequestedDate(LocalDateTime.of(2018, 1, 1, 9, 9, 9));
        request.setServiceId(100L);
        request.setCreatedAt(LocalDateTime.of(2018, 1, 1, 9, 9, 9));
        return request;
    }
}
