package ru.yandex.market.ff.dbqueue.consumer.functional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ShopRequestStatusService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

@ParametersAreNonnullByDefault
public class GetRequestDetailsQueueProducerTest extends IntegrationTest {

    @Autowired
    private ShopRequestStatusService shopRequestStatusService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;


    @Test
    @DatabaseSetup("classpath:db-queue/producer/get-details/before-for-ff-supply.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/producer/get-details/after-for-ff-supply.xml",
            assertionMode = NON_STRICT)
    void produceSuccessfullyWhenSupplyRequestMovesToStatusProcessed() {
        ShopRequest requestInSentToServiceStatus = shopRequestRepository.findById(1L);

        shopRequestStatusService.updateStatus(
                requestInSentToServiceStatus,
                RequestStatus.PROCESSED
        );
    }

    @Test
    @DatabaseSetup("classpath:db-queue/producer/get-details-supplier-not-in-test/before-for-ff-supply.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/producer/get-details-supplier-not-in-test/after-for-ff-supply.xml",
            assertionMode = NON_STRICT)
    void produceSuccessfulWhenRequestIsNotInListAndSupplierInTest() {
        ShopRequest requestInSentToServiceStatus = shopRequestRepository.findById(1L);

        shopRequestStatusService.updateStatus(
                requestInSentToServiceStatus,
                RequestStatus.PROCESSED
        );
    }
}
