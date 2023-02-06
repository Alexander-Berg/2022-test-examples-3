package ru.yandex.market.ff.repository.replica;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static org.assertj.core.api.Assertions.assertThat;

public class ShopRequestReplicaRepositoryTest extends IntegrationTest {

    @Autowired
    private ShopRequestReplicaRepository requestRepository;

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_without_withdraw_act.xml")
    void testFindRequestsWithoutWithdrawAct() {
        final Collection<ShopRequest> requests = requestRepository.findRequestsWithoutWithdrawAct();
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(4).contains(1L, 2L, 6L, 20L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_withdraw_with_cises_and_without_act.xml")
    void testFindRequestsWithdrawWithCisesAndWithoutAct() {
        final Collection<ShopRequest> requests = requestRepository.findRequestsWithdrawWithCisesWithoutReport();
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(2).contains(6L, 30L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup(
            "classpath:repository/shop-request/find-utilization-transfers-with-cises-and-without-report.xml")
    void testFindUtilizationTransfersWithCisesAndWithoutReport() {
        Collection<Long> requestIds = requestRepository.findUtilizationTransfersWithCisesAndWithoutReport()
                .stream()
                .map(ShopRequest::getId)
                .collect(Collectors.toList());
        assertions.assertThat(requestIds).containsExactlyInAnyOrder(5L, 7L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup(
            "classpath:repository/shop-request/find_supply_requests_with_cises_and_without_report.xml")
    void testFindSupplyRequestsWithCisesWithoutReport() {
        Collection<Long> requestIds = requestRepository.findSupplyRequestsWithCisesWithoutReport()
                .stream()
                .map(ShopRequest::getId)
                .collect(Collectors.toList());
        assertions.assertThat(requestIds).containsExactlyInAnyOrder(1L, 6L, 30L);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-transfers-by-supplier-and-inbound-id.xml")
    void findShopReqByInboundIdAndSupplierId() {
        Collection<ShopRequest> allByInboundIdAndSupplierId =
                requestRepository.findAllByInboundIdAndSupplierId(10, 1,
                        Arrays.asList(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED,
                                RequestStatus.INVALID));
        assertThat(allByInboundIdAndSupplierId).hasSize(2);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_without_surplus_additional_act.xml")
    void testFindRequestsWithoutAdditionalActOfReception() {
        final Collection<ShopRequest> requests = requestRepository.findRequestsWithoutAdditionalActOfReception(List.of(
                RequestType.SUPPLY,
                RequestType.X_DOC_PARTNER_SUPPLY_TO_FF,
                RequestType.CROSSDOCK));
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(1).contains(1L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/act_reception_transfer.xml")
    void findRequestsWithoutReceptionTransferAct() {
        final Collection<ShopRequest> requests = requestRepository.findRequestsWithoutReceptionTransferAct(List.of(
                RequestType.SUPPLY,
                RequestType.X_DOC_PARTNER_SUPPLY_TO_FF,
                RequestType.CROSSDOCK));
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(4).contains(1L, 2L, 41L, 42L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_without_act_of_withdraw_from_storage.xml")
    void testFindUtilizationWithdrawsWithoutActOfWithdrawFromStorage() {
        Collection<Long> requestIds = requestRepository.findUtilizationWithdrawsWithoutActOfWithdrawFromStorage()
                .stream()
                .map(ShopRequest::getId)
                .collect(Collectors.toList());
        assertions.assertThat(requestIds).containsExactlyInAnyOrder(1L, 2L, 14L, 21L, 25L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_without_act_of_withdraw_from_storage.xml")
    void testFindUtilizationTransfersWithoutActOfWithdrawFromStorage() {
        Collection<Long> requestIds = requestRepository.findUtilizationTransfersWithoutActOfWithdrawFromStorage()
                .stream()
                .map(ShopRequest::getId)
                .collect(Collectors.toList());
        assertions.assertThat(requestIds).containsExactlyInAnyOrder(15L, 23L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_requests_without_discrepancy_act.xml")
    void testFindRequestsWithoutDiscrepancyAct() {
        Collection<Long> requestIds = requestRepository.findRequestsWithoutDiscrepancyAct(List.of(
                RequestType.SUPPLY,
                RequestType.X_DOC_PARTNER_SUPPLY_TO_FF,
                RequestType.CROSSDOCK))
                .stream()
                .map(ShopRequest::getId)
                .collect(Collectors.toList());
        assertions.assertThat(requestIds).containsExactlyInAnyOrder(1L, 2L, 7L, 8L, 9L, 10L, 11L);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-hung-demand-bbxd.xml")
    void findHungDemandBBXD() {
        LocalDateTime dateTime = LocalDateTime.of(2000, 1, 1, 10, 1);
        Set<ShopRequest> shopRequests = requestRepository.findHungDemandBBXD(dateTime);
        assertions.assertThat(shopRequests.stream().map(ShopRequest::getId).collect(Collectors.toSet()))
                .hasSize(3)
                .contains(2L, 4L, 7L);
    }
}
