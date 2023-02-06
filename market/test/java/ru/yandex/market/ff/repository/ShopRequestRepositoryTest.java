package ru.yandex.market.ff.repository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.enums.ConsolidatedShippingStatus;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.time.LocalDateTime.of;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kotovdv 04/08/2017.
 */
class ShopRequestRepositoryTest extends IntegrationTest {

    private static final long SHOP_ID = 2L;
    private static final long X_DOC_SERVICE_ID = 123;

    @Autowired
    private ShopRequestRepository requestRepository;

    @Test
    @JpaQueriesCount(4)
    @DatabaseSetup("classpath:repository/shop-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/save.xml", assertionMode = NON_STRICT)
    void testSaveFullyFilledShopRequest() {
        ShopRequest request = createFullyFilledShopRequest();

        requestRepository.save(request);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_by_status.xml")
    void testFindNotInternalByStatus() {
        Collection<ShopRequest> requests =
                requestRepository.findNotInternalByStatus(RequestStatus.VALIDATED);

        assertThat(requests)
                .as("Asserting that exactly 1 request was extracted by status")
                .allMatch(elem -> Objects.equals(elem.getSupplier().getId(), SHOP_ID))
                .allMatch(elem -> elem.getStatus().equals(RequestStatus.VALIDATED))
                .hasSize(1);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_by_id.xml")
    void testFindByIdWithFetchedSupplier() {
        ShopRequest request = requestRepository.findByIdWithFetchedSupplier(1);
        assertions.assertThat(request).isNotNull();
        Supplier supplier = request.getSupplier();
        assertions.assertThat(supplier.getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
        assertions.assertThat(supplier.getName()).isEqualTo("supplier1");
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find_by_id_with_consolidated_shipping.xml")
    @Transactional
    void testFindByIdWithFetchedConsolidatedShipping() {
        ShopRequest request = requestRepository.findByIdWithFetchedSupplier(1);
        assertions.assertThat(request).isNotNull();
        Supplier supplier = request.getSupplier();
        assertions.assertThat(supplier.getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
        assertions.assertThat(supplier.getName()).isEqualTo("supplier1");
        assertions.assertThat(request.getConsolidatedShipping().getId()).isEqualTo(666L);
        assertions.assertThat(request.getConsolidatedShipping().getStatus()).isEqualTo(
                ConsolidatedShippingStatus.ACTIVE);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find_xdoc_by_status.xml")
    void testFindXDocByStatus() {
        Collection<ShopRequest> requests = requestRepository.findXDocByStatus(RequestStatus.ACCEPTED_BY_XDOC_SERVICE);

        assertThat(requests)
                .as("Asserting that exactly 1 request was extracted by status")
                .allMatch(elem -> Objects.equals(elem.getxDocServiceId(), X_DOC_SERVICE_ID))
                .allMatch(elem -> elem.getStatus() == RequestStatus.ACCEPTED_BY_XDOC_SERVICE)
                .hasSize(1);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find_by_status_and_types.xml")
    void findRequestsByStatusAndTypeIn() {
        Collection<ShopRequest> requests = requestRepository.findNotInternalByStatusAndType(RequestStatus.CREATED,
                ImmutableList.of(RequestType.CROSSDOCK, RequestType.SUPPLY));

        assertThat(requests).hasSize(2)
                .anyMatch(elem -> elem.getId().equals(1L))
                .anyMatch(elem -> elem.getId().equals(2L));
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/finished_requests_within_period.xml")
    void findFinishedRequestsUpdatedInPeriod() {
        Collection<ShopRequest> requests = requestRepository.findInboundSuppliesWereArrivedInPeriod(
                100L,
                SupplierType.FIRST_PARTY,
                LocalDate.of(2022, 3, 23).atStartOfDay().toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2022, 3, 24).atStartOfDay().toInstant(ZoneOffset.ofHours(3)),
                Set.of(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF, RequestType.SUPPLY));

        assertThat(requests).hasSize(1);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/finished_x_dock_requests_within_period.xml")
    void findXDockWereArrivedToSortingInPeriod() {
        Collection<ShopRequest> requests = requestRepository.findXDockWereArrivedToSortingInPeriod(
                100L,
                SupplierType.FIRST_PARTY,
                LocalDate.of(2022, 3, 23).atStartOfDay().toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2022, 3, 24).atStartOfDay().toInstant(ZoneOffset.ofHours(3)),
                Set.of(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF));

        assertThat(requests).hasSize(1);
    }

    @Test
    void testFindNotInternalByStatusWithEmptyDatabase() {
        assertThat(requestRepository.findNotInternalByStatus(RequestStatus.REJECTED_BY_SERVICE))
                .as("Asserting that returned value is empty collection")
                .isEmpty();
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/find_by_status.xml")
    void testFindByStatuses() {
        Collection<ShopRequest> requests =
                requestRepository.findByStatusIn(RequestStatus.VALIDATED, RequestStatus.PROCESSED);

        final Condition<ShopRequest> validatedCondition =
                new Condition<>((r) -> r.getStatus() == RequestStatus.VALIDATED, "Only one validated");
        final Condition<ShopRequest> doneCondition =
                new Condition<>((r) -> r.getStatus() == RequestStatus.PROCESSED, "Only one done");

        assertThat(requests)
                .as("Asserting that exactly 2 request was extracted by status")
                .haveExactly(2, validatedCondition)
                .haveExactly(1, doneCondition)
                .allMatch(elem -> Objects.equals(elem.getSupplier().getId(), SHOP_ID))
                .hasSize(3);
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    void testFindByStatusesWithEmptyDatabase() {
        assertThat(requestRepository.findByStatusIn(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CREATED))
                .as("Asserting that returned value is empty collection")
                .isEmpty();
    }

    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:repository/shop-request/get_with_documents.xml")
    void testFindIds() {
        long requestId = 1;
        assertThat(requestRepository.findIds(Collections.singleton(requestId), SHOP_ID))
                .containsExactlyInAnyOrder(requestId);
        assertThat(requestRepository.findIds(Collections.singleton(999L), SHOP_ID))
                .isEmpty();
        assertThat(requestRepository.findIds(Arrays.asList(requestId, 999L, requestId, 123L), SHOP_ID))
                .containsExactlyInAnyOrder(requestId);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/moved_to_status_in_period.xml")
    void findCrossdockRequestsMovedToStatusInPeriod() {
        final Collection<ShopRequest> requests = requestRepository.findCrossdockRequestsMovedToStatusInPeriod(
                Arrays.asList(RequestStatus.ARRIVED_TO_SERVICE, RequestStatus.REJECTED_BY_SERVICE),
                DateTimeTestConfig.FIXED_NOW.minusYears(100),
                DateTimeTestConfig.FIXED_NOW.minusDays(1)
        );
        assertThat(requests).isNotNull().hasSize(1);

        final List<Long> ids = requests.stream().map(ShopRequest::getId).collect(Collectors.toList());
        assertThat(ids).containsExactly(8L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/shop-request/non-cancelled-crossdock-requests-after.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/non-cancelled-crossdock-requests-after.xml",
            assertionMode = NON_STRICT)
    void findCountOfNonCancelledCrossdockRequestsCreatedAfter() {
        LocalDateTime time = LocalDateTime.of(2019, 10, 7, 11, 17, 30);
        long count = requestRepository.findCountOfNonCancelledCrossdockRequestsCreatedAfter(1, time);
        assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/not_arrived_to_warehouse_crossdock_requests.xml")
    @JpaQueriesCount(1)
    @ExpectedDatabase(value = "classpath:repository/shop-request/not_arrived_to_warehouse_crossdock_requests.xml",
            assertionMode = NON_STRICT)
    void findNotArrivedToWarehouseCrossdockRequests() {
        Collection<ShopRequest> requests = requestRepository.findNotArrivedToWarehouseCrossdockRequests();
        List<Long> ids = requests.stream().map(ShopRequest::getId).collect(Collectors.toList());
        assertions.assertThat(ids).containsExactly(6L);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-with-request-creation-sla-exceeded.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/find-with-request-creation-sla-exceeded.xml",
            assertionMode = NON_STRICT)
    void findSuppliesWithRequestCreationSlaExceeded() {
        LocalDateTime now = of(2000, 1, 1, 2, 0);
        LocalDateTime checkPeriodStart = now.minusHours(1);
        LocalDateTime slaExceededStart = now.minusMinutes(15);

        List<BigInteger> suppliesWithExceededCreationInterval =
                requestRepository.findRequestsWithExceededCreationInterval(checkPeriodStart, slaExceededStart,
                        Set.of(RequestType.SUPPLY.getId()));

        assertions.assertThat(suppliesWithExceededCreationInterval).containsExactly(BigInteger.valueOf(4L));
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-with-request-creation-sla-exceeded-with-slot-waiting.xml")
    @ExpectedDatabase(
            value = "classpath:repository/shop-request/find-with-request-creation-sla-exceeded-with-slot-waiting.xml",
            assertionMode = NON_STRICT)
    void findSuppliesWithRequestCreationSlaExceededWithSlotWaiting() {
        LocalDateTime now = of(2000, 1, 1, 2, 0);
        LocalDateTime checkPeriodStart = now.minusHours(1);
        LocalDateTime slaExceededStart = now.minusMinutes(15);

        List<BigInteger> suppliesWithExceededCreationInterval =
                requestRepository.findRequestsWithExceededCreationInterval(checkPeriodStart, slaExceededStart,
                        Set.of(RequestType.SUPPLY.getId()));

        assertions.assertThat(suppliesWithExceededCreationInterval).isEmpty();
    }

    @Test
    @DatabaseSetup(
            "classpath:repository/shop-request/" +
                    "find-with-request-creation-sla-exceeded-without-slot-waiting-for-it.xml")
    @ExpectedDatabase(
            value = "classpath:repository/shop-request/" +
                    "find-with-request-creation-sla-exceeded-without-slot-waiting-for-it.xml",
            assertionMode = NON_STRICT)
    void findSuppliesWithRequestCreationSlaExceededWithoutSlotWaitingForIt() {
        LocalDateTime now = of(2000, 1, 1, 2, 0);
        LocalDateTime checkPeriodStart = now.minusHours(1);
        LocalDateTime slaExceededStart = now.minusMinutes(15);

        List<BigInteger> suppliesWithExceededCreationInterval =
                requestRepository.findRequestsWithExceededCreationInterval(checkPeriodStart, slaExceededStart,
                        Set.of(RequestType.SUPPLY.getId()));

        assertions.assertThat(suppliesWithExceededCreationInterval).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-with-request-creation-sla-exceeded-not-zero-attempt.xml")
    void findSuppliesWithRequestCreationSlaExceededNotZeroAttempt() {
        LocalDateTime now = of(2000, 1, 1, 2, 0);
        LocalDateTime checkPeriodStart = now.minusHours(1);
        LocalDateTime slaExceededStart = now.minusMinutes(15);

        List<BigInteger> suppliesWithExceededCreationInterval =
                requestRepository.findRequestsWithExceededCreationInterval(checkPeriodStart, slaExceededStart,
                        Set.of(RequestType.SUPPLY.getId()));

        assertions.assertThat(suppliesWithExceededCreationInterval).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-with-request-validation-sla-exceeded.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/find-with-request-validation-sla-exceeded.xml",
            assertionMode = NON_STRICT)
    void findSuppliesWithRequestValidationSlaExceeded() {
        LocalDateTime now = of(2000, 1, 1, 2, 0);
        LocalDateTime checkPeriodStart = now.minusHours(1);
        LocalDateTime slaExceededStart = now.minusSeconds(60);

        List<Long> suppliesWithExceededCreationInterval =
                requestRepository.findRequestsWithExceededValidationInterval(
                        EnumSet.of(RequestType.SUPPLY), checkPeriodStart, slaExceededStart)
                        .stream()
                        .map(ShopRequest::getId)
                        .collect(Collectors.toList());

        assertions.assertThat(suppliesWithExceededCreationInterval).containsExactly(3L);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find_requests_with_valid_calendaring.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/find_requests_with_valid_calendaring.xml",
            assertionMode = NON_STRICT)
    void findByStatusAndTypeInJoinFetchDocumentsWithValidCalendaringTest() {
        Collection<ShopRequest> requests =
                requestRepository.findNotInternalByStatusAndTypeWithValidCalendaringAndLogisticsPointFetched(
                        RequestStatus.CREATED,
                        Arrays.asList(RequestType.values())
                );
        Collection<Long> ids = requests.stream().map(ShopRequest::getId).collect(Collectors.toList());
        assertions.assertThat(ids).containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/find-utilization-transfers.xml")
    void findUtilizationTransferWithOutboundsNotInStatus() {
        Collection<ShopRequest> requests = requestRepository.findUtilizationTransfersWithOutboundsNotInStatus(
                RequestStatus.PROCESSED, EnumSet.of(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.INVALID,
                        RequestStatus.CANCELLED), 100
        );

        assertThat(requests).hasSize(3)
                .anyMatch(elem -> elem.getId().equals(4L))
                .anyMatch(elem -> elem.getId().equals(5L))
                .anyMatch(elem -> elem.getId().equals(7L));
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/count_by_status_and_update_at_less_then.xml")
    void getCountByLastStatusAndHistoryUpdateAtLessThenTest() {
        long count = requestRepository.countByStatusAndUpdateAtLessThen(RequestStatus.PROCESSED,
                LocalDateTime.of(2020, 7, 1, 12, 33, 11));
        Assertions.assertEquals(2, count);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request/update-is-registry-shop-request-before.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request/update-is-registry-shop-request-after.xml",
            assertionMode = NON_STRICT)
    void updateIsRegistryTest() {
        requestRepository.updateIsRegistry(1, true);
    }

    private ShopRequest createFullyFilledShopRequest() {
        ShopRequest shopRequest = new ShopRequest();

        final Supplier supplier = new Supplier();
        supplier.setId(SHOP_ID);
        shopRequest.setSupplier(supplier);
        shopRequest.setServiceId(100L);

        shopRequest.setStatus(RequestStatus.CREATED);
        shopRequest.setType(RequestType.VALID_UNREDEEMED);

        LocalDateTime timestamp = of(1999, 9, 9, 9, 9, 9);
        shopRequest.setCreatedAt(timestamp);
        shopRequest.setUpdatedAt(timestamp);
        shopRequest.setRequestedDate(timestamp);
        shopRequest.setItemsTotalCount(1L);
        shopRequest.setCalendaringMode(CalendaringMode.AUTO);

        shopRequest.setConsignor("Consignor");
        shopRequest.setConsignorId(190L);
        shopRequest.setConsignorRequestId("АПП №5");

        return shopRequest;
    }
}
