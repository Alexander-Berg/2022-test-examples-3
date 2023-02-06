package ru.yandex.market.sc.internal.controller.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.service.RequestService;
import ru.yandex.market.logistic.api.utils.UniqService;
import ru.yandex.market.sc.core.dbqueue.ScQueueConfiguration;
import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto;
import ru.yandex.market.sc.core.domain.courier.shift.repository.CourierShiftRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.route.RouteCellsCleanupService;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.core.external.delivery_service.model.TplCouriers;
import ru.yandex.market.sc.core.external.juggler.JugglerNotificationClient;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.SortingCenterParams;
import ru.yandex.market.sc.internal.controller.internal.dto.BatchUpdateOrdersRequest;
import ru.yandex.market.sc.internal.controller.internal.dto.InternalCourierDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@Import(ScQueueConfiguration.Full.class)
@EmbeddedDbIntTest
class InternalOrderServiceTest {

    @Autowired
    InternalOrderService internalOrderService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    DbQueueTestUtil dbQueueTestUtil;
    @Autowired
    Clock clock;

    @MockBean
    RequestService requestService;
    @MockBean
    RestTemplate restTemplate;
    @MockBean
    XmlMapper xmlMapper;
    @MockBean
    UniqService uniqService;
    @MockBean
    JugglerNotificationClient jugglerNotificationClient;
    @MockBean
    TplClient tplClient;
    @MockBean
    RouteCellsCleanupService routeCellsCleanupService;
    @Autowired
    CourierShiftRepository courierShiftRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void sendJugglerNotificationOnRoutingFinish() {
        var sortingCenter = testFactory.storedSortingCenter();
        testFactory.create(order(sortingCenter).externalId("o1").build()).get();
        internalOrderService.batchUpdateOrders(new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                LocalDate.now(clock),
                List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of("o1")),
                null,
                null
        ));
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.START_SORTING);
        verify(jugglerNotificationClient).pushStartSortingEvents(eq(sortingCenter), anyString());
    }

    @Test
    void uploadCourierShiftsOnRoutingFinish() {
        var sortingCenter = testFactory.storedSortingCenter(777);
        testFactory.create(order(sortingCenter).externalId("o1").build()).get();
        long courierUid = 9L;
        LocalDate shiftDate = LocalDate.now(clock);
        internalOrderService.batchUpdateOrders(new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                shiftDate,
                List.of(new InternalCourierDto(courierUid, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of("o1")),
                null,
                null
        ));
        mockTplGetCouriers(sortingCenter.getId(),
                sortingCenter.getToken(),
                courierUid,
                shiftDate,
                LocalTime.now(clock));
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);
        assertThat(jdbcTemplate.queryForList("select * from courier_shift")).isNotEmpty();
    }

    void mockTplGetCouriers(long scId, String token, long courierUid, LocalDate shiftDate, LocalTime shiftStartTime) {
        TplCouriers tplCouriers = new TplCouriers(List.of(
                new TplCouriers.TplCourier(
                        courierUid, "Вася",
                        new PartnerCourierDto.CourierCompany("ООО Мой курьер"),
                        shiftDate, shiftStartTime
                )
        ));
        doReturn(tplCouriers).when(tplClient).getCouriers(scId, token, shiftDate);
    }

    @Test
    void batchUpdateOrdersTokenFromOtherSc() {
        var sortingCenter1 = testFactory.storedSortingCenter(
                SortingCenterParams.builder().id(1).token("token1").yandexId("100").build());
        var sortingCenter2 = testFactory.storedSortingCenter(
                SortingCenterParams.builder().id(2).token("token2").yandexId("200").build());
        var order = testFactory.createOrder(sortingCenter1).get();
        var request = new BatchUpdateOrdersRequest(
                sortingCenter2.getId(),
                LocalDate.now(clock),
                List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of(order.getExternalId())), sortingCenter1.getToken(),
                null
        );
        var response = internalOrderService.batchUpdateOrders(request);
        assertThat(response.getSuccessUpdatedOrders()).containsOnly(order.getExternalId());
        assertThat(response.getFailUpdatedOrders()).isEmpty();
    }

    @Test
    void batchUpdateOrdersWithoutToken() {
        var sortingCenter = testFactory.storedSortingCenter(
                SortingCenterParams.builder().id(1).token("token1").build());
        var order = testFactory.createOrder(sortingCenter).get();
        var request = new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                LocalDate.now(clock),
                List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of(order.getExternalId())),
                null,
                null
        );
        var response = internalOrderService.batchUpdateOrders(request);
        assertThat(response.getSuccessUpdatedOrders()).containsOnly(order.getExternalId());
        assertThat(response.getFailUpdatedOrders()).isEmpty();
    }

    @Test
    void batchUpdateCourierPerformance() {
        String scToken = "token1";
        var sortingCenter = testFactory.storedSortingCenter(
                SortingCenterParams.builder().id(1).token(scToken).build());
        List<ScOrder> orders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            orders.add(testFactory.createOrder(order(sortingCenter).externalId("ext" + i).build()).get());
        }

        Map<Long, List<String>> ordersByCourier = new HashMap<>();
        List<String> extIdList = orders.stream().map(ScOrder::getExternalId).toList();


        InternalCourierDto courierDto = new InternalCourierDto(100L, "Вася", null, null, null, "ООО Мой курьер");
        ordersByCourier.put(courierDto.getId(), extIdList);
        var request = new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                LocalDate.now(clock),
                List.of(courierDto),
                1L,
                0L,
                1L,
                ordersByCourier,
                scToken,
                null
        );

        long start = System.currentTimeMillis();

        var response = internalOrderService.batchUpdateOrders(request);

        long finish = System.currentTimeMillis();

        assertThat(finish - start).isLessThan(25000);
        assertThat(response.getSuccessUpdatedOrders())
                .containsExactlyInAnyOrder(extIdList.toArray(new String[extIdList.size()]));
        assertThat(response.getFailUpdatedOrders()).isEmpty();
    }

}
