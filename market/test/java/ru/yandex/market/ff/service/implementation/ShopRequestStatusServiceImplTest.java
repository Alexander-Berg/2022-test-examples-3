package ru.yandex.market.ff.service.implementation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ShopRequestStatusService;

class ShopRequestStatusServiceImplTest extends IntegrationTest {

    @Autowired
    private ShopRequestStatusService shopRequestStatusService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Test
    @DatabaseSetup("classpath:/service/shop-request/produce-task-create-doc-ticket-before.xml")
    @ExpectedDatabase(
            value = "classpath:/service/shop-request/produce-task-create-doc-ticket-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void produceTaskCreateDocTicketWhenStatusIsArrivedToServiceAndTypeSupply() {
        ShopRequest requestInSentToServiceStatus = shopRequestRepository.findById(1L);

        shopRequestStatusService.updateStatus(
                requestInSentToServiceStatus,
                RequestStatus.ARRIVED_TO_SERVICE
        );
    }

    /**
    * Тест для типа 21 X_DOC_PARTNER_SUPPLY_TO_FF
    */
    @Test
    @DatabaseSetup("classpath:/service/shop-request/produce-task-create-doc-ticket-for-type-21-before.xml")
    @ExpectedDatabase(
            value = "classpath:/service/shop-request/produce-task-create-doc-ticket-for-type-21-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void produceTaskCreateDocTicketWhenStatusIsArrivedToXDocServiceAndType21() {
        ShopRequest requestInSentToServiceStatus = shopRequestRepository.findById(1L);

        shopRequestStatusService.updateStatus(
                requestInSentToServiceStatus,
                RequestStatus.ARRIVED_TO_XDOC_SERVICE
        );
    }

    @Test
    @DatabaseSetup("classpath:/service/request-status-history/setup.xml")
    void getPriorRequestStatus() {
        RequestStatus priorRequestStatus = shopRequestStatusService.getPriorRequestStatus(1L, RequestStatus.VALIDATED);
        assertions.assertThat(priorRequestStatus).isEqualTo(RequestStatus.SENT_TO_SERVICE);
    }
}
