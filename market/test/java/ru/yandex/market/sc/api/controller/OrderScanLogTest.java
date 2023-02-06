package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.ScanLogService;
import ru.yandex.market.sc.core.domain.scan_log.model.PartnerOrderScanLogEntryDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogOperation;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogResult;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScApiControllerTest
class OrderScanLogTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    ScanLogService scanLogService;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    TestControllerCaller caller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        caller = TestControllerCaller.createCaller(mockMvc);
    }


    @Test
    void scanLogGetOrder() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        scanOrder(order.getExternalId(), testFactory.orderPlace(order).getMainPartnerCode(),
                ScanLogContext.INITIAL_ACCEPTANCE);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId())).isEqualTo(
                List.of(scanLogEntryScan(order.getExternalId(), ScanLogResult.OK, ScanLogContext.INITIAL_ACCEPTANCE)));
    }

    @Test
    void scanLogGetOrderMultiplace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        scanOrder(order.getExternalId(), "1", null);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntryScan(order.getExternalId(), "1")));
    }

    @Test
    void scanLogGetOrderNoContext() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        scanOrder(order.getExternalId(), testFactory.orderPlace(order).getMainPartnerCode(), null);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntryScan(order.getExternalId(), ScanLogResult.OK, null)));
    }

    @Test
    void scanLogGetOrderError() {
        String notExisting = "not_existing";
        scanOrder(notExisting, notExisting, ScanLogContext.COURIER_SHIP);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, notExisting))
                .isEqualTo(List.of(scanLogEntryScan(notExisting, ScanLogResult.ERROR,
                        ScanLogContext.COURIER_SHIP)));
    }

    @Test
    void scanLogGetOrderWithLotsError() {
        var notExisting = "not_existing";
        getOrderWithLots(notExisting, notExisting);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, notExisting))
                .isEqualTo(List.of(scanLogEntryScan(notExisting, ScanLogResult.ERROR, ScanLogContext.SORT)));
    }

    @Test
    void scanLogAcceptOrder() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        acceptOrder(order.getExternalId(), null, ScanLogContext.INITIAL_ACCEPTANCE);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId())).isEqualTo(
                List.of(scanLogEntryScan(order.getExternalId(), ScanLogResult.OK, ScanLogContext.INITIAL_ACCEPTANCE)));
    }

    @Test
    void scanLogAcceptOrderMultiplace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        acceptOrder(order.getExternalId(), "1", null);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntryScan(order.getExternalId(), "1")));
    }

    @Test
    void scanLogAcceptOrderNoContext() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        acceptOrder(order.getExternalId(), null, null);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntryScan(order.getExternalId(), ScanLogResult.OK, null)));
    }

    @Test
    void scanLogAcceptOrderError() {
        String notExisting = "not_existing";
        acceptOrder(notExisting, notExisting, ScanLogContext.COURIER_SHIP);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, notExisting))
                .isEqualTo(List.of(scanLogEntryScan(notExisting, ScanLogResult.ERROR,
                        ScanLogContext.COURIER_SHIP)));
    }

    @Test
    @Disabled
// утилизация будет сделана иначе - текущую реализацию выключили
    void scanLogAcceptUtilizationError() {
        String notExisting = "not_existing";
        acceptUtilizationOrder(notExisting, notExisting);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, notExisting))
                .isEqualTo(List.of(scanLogEntryScan(notExisting, ScanLogResult.ERROR, ScanLogContext.UTILIZATION)));
    }

    @Test
    void scanLogSortOrder() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        sortPlace(order.getExternalId(), order.getExternalId(), cell.getId());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId())).isEqualTo(List.of(
                scanLogEntrySort(order.getExternalId(), ScanLogResult.OK, null, cell, ScanLogContext.SORT)
        ));
    }

    @Test
    void scanLogSortOrderHasCellBefore() {
        var place = testFactory.createOrder(sortingCenter).accept().keep().getPlace();
        var cellBefore = place.getCell();
        testFactory.updateForTodayDelivery(place.getOrder());

        place = testFactory.updated(place);
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));
        sortPlace(place.getExternalId(), place.getMainPartnerCode(), cell.getId());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place.getExternalId()))
                .isEqualTo(List.of(scanLogEntrySort(place.getExternalId(), ScanLogResult.OK, cellBefore,
                        cell, ScanLogContext.SORT)));
    }

    @Test
    void scanLogSortOrderMultiplace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .accept()
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        sortPlace(order.getExternalId(), "1", cell.getId());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntrySort(order.getExternalId(), "1", cell)));
    }

    @Test
    @SneakyThrows
    void scanLogSortOrderError() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);
        sortPlace(order.getExternalId(), order.getExternalId(), cell.getId());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntrySort(order.getExternalId(), ScanLogResult.ERROR,
                        null, null, ScanLogContext.SORT)));
    }

    @Test
    void scanLogShipOrder() {
        var place = testFactory.createOrderForToday(sortingCenter).accept().sort().getPlace();
        var cellBefore = place.getCell();
        shipRoute(place);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place.getExternalId()))
                .isEqualTo(List.of(scanLogEntryShip(place.getExternalId(), ScanLogResult.OK,
                        cellBefore, ScanLogContext.COURIER_SHIP)));
    }

    @Test
    void scanLogShipOrderMultiplace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").get();
        Place place = testFactory.anyOrderPlace(order);
        var cellBefore = place.getCell();

        shipRoute(place);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place.getExternalId()))
                .isEqualTo(List.of(
                        scanLogEntryShip(place.getExternalId(), "1", cellBefore),
                        scanLogEntryShip(place.getExternalId(), "2", cellBefore)
                ));
    }

    @Test
    void scanLogShipOrderMultiplaceNotFull() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").get();
        Place place = testFactory.anyOrderPlace(order);
        var cellBefore = place.getCell();

        shipRoute(place);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place.getExternalId()))
                .isEqualTo(List.of(
                        scanLogEntryShip(place.getExternalId(), "1", cellBefore)
                ));
    }

    @Test
    void scanLogShipOrderMultiplaceAndSinglePlace() {
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").places("3").build())
                .accept().sort().get();

        Place place1 = testFactory.orderPlace(order1, "1");
        Place place3 = testFactory.orderPlace(order2, "3");

        var cellBefore = place1.getCell();
        shipRoute(place1);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place1.getExternalId()))
                .isEqualTo(List.of(
                        scanLogEntryShip(place1.getExternalId(), "1", cellBefore),
                        scanLogEntryShip(place1.getExternalId(), "2", cellBefore)
                ));
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place3.getExternalId()))
                .isEqualTo(List.of(
                        scanLogEntryShip(place3.getExternalId(), place3.getMainPartnerCode(), cellBefore)
                ));
    }

    @Test
    void scanLogShipOrderNoContext() {
        var place = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().getPlace();
        var cellBefore = place.getCell();
        shipRoute(place);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, place.getExternalId()))
                .isEqualTo(List.of(
                        scanLogEntryShip(place.getExternalId(), place.getMainPartnerCode(), cellBefore)
                ));
    }

    @Test
    void scanLogShipOrderError() {
        var order = testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("1", "2").build()
                )
                .acceptPlaces("1").sortPlaces("1").get();
        Place place1 = testFactory.orderPlaces(order.getId()).stream()
                .filter(p -> p.getMainPartnerCode().equals("1"))
                .findFirst().orElseThrow();
        shipRoute(order);
        assertThat(scanLogService.getOrderScanLog(sortingCenter, order.getExternalId()))
                .isEqualTo(List.of(scanLogEntryShip("o1", "1", ScanLogResult.ERROR,
                        place1.getCell(), ScanLogContext.COURIER_SHIP)));
    }

    @SneakyThrows
    private void getOrderWithLots(String externalOrderId, String placeExternalId) {
        var requestBuilder = MockMvcRequestBuilders.get("/api/orders/withLots")
                .param("externalId", externalOrderId)
                .param("placeExternalId", placeExternalId)
                .header("Authorization", "OAuth uid-" + user.getUid())
                .header("SC-Application-Context", ScanLogContext.SORT);
        mockMvc.perform(requestBuilder);
    }

    @SneakyThrows
    private void acceptUtilizationOrder(String orderExternalId, String placeExternalId) {
        var utilizationUser = testFactory.storedUser(sortingCenter, 777, UserRole.SENIOR_STOCKMAN);
        var requestBuilder = MockMvcRequestBuilders.put("/api/orders/acceptUtilization")
                .header("Authorization", "OAuth uid-" + utilizationUser.getUid())
                .header("SC-Application-Context", ScanLogContext.UTILIZATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalId\":\"" + orderExternalId + "\"," +
                        "\"placeExternalId\":\"" + placeExternalId + "\"}");
        mockMvc.perform(requestBuilder);
    }

    @SneakyThrows
    private void shipRoute(OrderLike order) {
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .or(() -> testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)))
                .or(() -> testFactory.findPossibleOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)))
                .orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        var requestBuilder = MockMvcRequestBuilders.put("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                .header("Authorization", "OAuth uid-" + user.getUid())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cellId\":" + cell.getId() + "}");
        mockMvc.perform(requestBuilder);
    }

    @SneakyThrows
    private void scanOrder(String externalOrderId,
                           @Nullable String externalPlaceId,
                           @Nullable ScanLogContext context) {
        var requestBuilder = MockMvcRequestBuilders.get("/api/orders?externalId=" +
                        externalOrderId + (externalPlaceId == null ? "" : "&placeExternalId=" + externalPlaceId))
                .header("Authorization", "OAuth uid-" + user.getUid());
        if (context != null) {
            requestBuilder = requestBuilder.header("SC-Application-Context", context);
        }
        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }

    @SneakyThrows
    private void acceptOrder(String externalOrderId,
                             @Nullable String externalPlaceId,
                             @Nullable ScanLogContext context) {
        var requestBuilder = MockMvcRequestBuilders.put("/api/orders/accept")
                .header("Authorization", "OAuth uid-" + user.getUid())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalId\":\"" + externalOrderId + "\"" +
                        (externalPlaceId == null ? "" : ",\"placeExternalId\":\"" + externalPlaceId + "\"") +
                        "}");
        if (context != null) {
            requestBuilder = requestBuilder.header("SC-Application-Context", context);
        }
        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }

    @SneakyThrows
    private void sortPlace(
            String orderExternalId,
            @Nullable String externalPlaceId,
            long cellId
    ) {
        caller.sortableBetaSort(new SortableSortRequestDto(
                orderExternalId,
                externalPlaceId,
                String.valueOf(cellId)));
    }

    private PartnerOrderScanLogEntryDto scanLogEntryScan(String externalId, ScanLogResult result,
                                                         @Nullable ScanLogContext context) {
        // в новом флоу у одноместного заказа создается коробка с тем же external_id
        return scanLogEntry(externalId, result, ScanLogOperation.SCAN,
                null, null, externalId, context);
    }

    private PartnerOrderScanLogEntryDto scanLogEntryScan(
            String externalId,
            @SuppressWarnings("SameParameterValue") String externalPlaceId
    ) {
        return scanLogEntry(externalId, ScanLogResult.OK, ScanLogOperation.SCAN,
                null, null, externalPlaceId, null);
    }

    private PartnerOrderScanLogEntryDto scanLogEntrySort(String externalId, ScanLogResult result,
                                                         @Nullable Cell cellBefore, Cell cellAfter,
                                                         @Nullable ScanLogContext context) {
        return scanLogEntry(externalId, result, ScanLogOperation.SORT,
                cellBefore, cellAfter, externalId, context);
    }

    private PartnerOrderScanLogEntryDto scanLogEntrySort(
            String externalId,
            @SuppressWarnings("SameParameterValue") String externalPlaceId,
            Cell cellAfter
    ) {
        return scanLogEntry(externalId, ScanLogResult.OK, ScanLogOperation.SORT,
                null, cellAfter, externalPlaceId, ScanLogContext.SORT);
    }

    @SuppressWarnings("SameParameterValue")
    private PartnerOrderScanLogEntryDto scanLogEntryShip(String externalId, ScanLogResult result,
                                                         Cell cellBefore,
                                                         @Nullable ScanLogContext context) {
        return scanLogEntryShip(externalId, externalId, result, cellBefore, context);
    }

    private PartnerOrderScanLogEntryDto scanLogEntryShip(String externalId, String externalPlaceId,
                                                         ScanLogResult result, @Nullable Cell cellBefore,
                                                         @Nullable ScanLogContext context) {
        return scanLogEntry(externalId, result, ScanLogOperation.SHIP,
                cellBefore, result != ScanLogResult.OK ? cellBefore : null,
                externalPlaceId, context);
    }

    private PartnerOrderScanLogEntryDto scanLogEntryShip(
            String externalId,
            @SuppressWarnings("SameParameterValue") String externalPlaceId,
            Cell cellBefore
    ) {
        return scanLogEntry(externalId, ScanLogResult.OK, ScanLogOperation.SHIP,
                cellBefore, null, externalPlaceId,
                cellBefore.getType().isCourier() ? ScanLogContext.COURIER_SHIP : ScanLogContext.RETURN_SHIP);
    }

    @SuppressWarnings({"SameParameterValue", "checkstyle:ParameterNumber"})
    private PartnerOrderScanLogEntryDto scanLogEntry(String externalId, ScanLogResult result,
                                                     ScanLogOperation operation,
                                                     @Nullable Cell cellBefore,
                                                     @Nullable Cell cellAfter,
                                                     @Nullable String externalPlaceId,
                                                     @Nullable ScanLogContext context) {
        return new PartnerOrderScanLogEntryDto(
                LocalDateTime.now(clock),
                null,
                null,
                externalId,
                externalPlaceId,
                user.getName(),
                operation,
                context,
                cellReportName(cellBefore),
                null,
                cellReportName(cellAfter),
                null,
                null,
                result
        );
    }

    @Nullable
    private String cellReportName(@Nullable Cell cell) {
        return cell == null ? null : (cell.getScNumber() == null ? String.valueOf(cell.getId()) : cell.getScNumber());
    }

}
