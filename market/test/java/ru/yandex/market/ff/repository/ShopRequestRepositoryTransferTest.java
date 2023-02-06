package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.time.LocalDateTime.of;

/**
 * Интеграционные тесты для {@link ShopRequestRepository}.
 */
class ShopRequestRepositoryTransferTest extends IntegrationTest {

    private static final long TRANSFER_ID = 2L;
    private static final long SHOP_ID = 2L;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Test
    @JpaQueriesCount(5)
    @DatabaseSetup("classpath:repository/request-transfer/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request-transfer/save.xml", assertionMode = NON_STRICT)
    void saveFullyFilledTransfer() {
        ShopRequest transfer = createFullyFilledTransfer();
        ShopRequest savedTransfer = shopRequestRepository.save(transfer);
        Assertions.assertThat(savedTransfer.getStatus()).isEqualTo(RequestStatus.CREATED);
    }

    @Test
    @JpaQueriesCount(2)
    @DatabaseSetup("classpath:repository/request-transfer/find_one.xml")
    void getTransferById() {
        ShopRequest transfer = shopRequestRepository.findById(TRANSFER_ID);
        Assertions.assertThat(transfer != null).isTrue();

        Assertions.assertThat(transfer.getId()).isEqualTo(TRANSFER_ID);
        Assertions.assertThat(transfer.getExternalRequestId()).isEqualTo("123");
        Assertions.assertThat(transfer.getSupplier().getId()).isEqualTo(SHOP_ID);
        Assertions.assertThat(transfer.getServiceId()).isEqualTo(100L);
        Assertions.assertThat(transfer.getStatus()).isEqualTo(RequestStatus.VALIDATED);
        Assertions.assertThat(transfer.getInboundId()).isEqualTo(9L);
        Assertions.assertThat(transfer.getStockType()).isEqualTo(StockType.FIT);
        Assertions.assertThat(transfer.getStockTypeTo()).isEqualTo(StockType.SURPLUS);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-transfer/find_by_status.xml")
    void findByStatuses() {
        Collection<ShopRequest> requests =
            shopRequestRepository.findByStatusIn(RequestStatus.ACCEPTED_BY_SERVICE, RequestStatus.FINISHED);

        final Condition<ShopRequest> acceptedCondition =
            new Condition<>((r) -> r.getStatus() == RequestStatus.ACCEPTED_BY_SERVICE, "Only one accepted");
        final Condition<ShopRequest> finishedCondition =
            new Condition<>((r) -> r.getStatus() == RequestStatus.FINISHED, "Only one finished");

        Assertions.assertThat(requests)
            .as("Asserting that exactly 2 transfers was extracted by status each with 2 items")
            .haveExactly(1, acceptedCondition)
            .haveExactly(1, finishedCondition)
            .allMatch(elem -> Objects.equals(elem.getSupplier().getId(), SHOP_ID))
            .hasSize(2);
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    void findByStatusesWithEmptyDatabase() {
        Assertions.assertThat(shopRequestRepository.findByStatusIn(RequestStatus.FINISHED))
            .as("Asserting that returned value is empty collection")
            .isEmpty();
    }


    private ShopRequest createFullyFilledTransfer() {
        ShopRequest transfer = new ShopRequest();

        final Supplier supplier = new Supplier();
        supplier.setId(SHOP_ID);

        transfer.setId(20L);
        transfer.setSupplier(supplier);
        transfer.setServiceId(100L);
        transfer.setServiceRequestId("serviceTransferID");
        transfer.setType(RequestType.TRANSFER);
        transfer.setExternalRequestId("123");

        transfer.setStatus(RequestStatus.CREATED);
        transfer.setInboundId(9L);

        transfer.setStockTypeTo(StockType.SURPLUS);
        transfer.setStockType(StockType.FIT);

        LocalDateTime timestamp = of(1999, 9, 9, 9, 9, 9);
        transfer.setCreatedAt(timestamp);
        transfer.setUpdatedAt(timestamp);
        transfer.setRequestedDate(timestamp);
        return transfer;
    }

}
