package ru.yandex.market.tpl.core.domain.order.partner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.partner.order.BasePartnerReportOrder;
import ru.yandex.market.tpl.core.domain.usershift.partner.order.PartnerReportOrder;
import ru.yandex.market.tpl.core.domain.usershift.partner.order.PartnerReportOrderRepository;
import ru.yandex.market.tpl.core.domain.usershift.partner.order.PartnerReportOrderSpecification;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author a-bryukhov
 */
@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerReportOrderRepositoryTest {

    private static final long UID = 1231L;
    private static final String TEST_ADDRESS = "testAddress city street";
    private static final String GENERALIZED_TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private final EntityManager entityManager;
    private final PartnerReportOrderRepository partnerReportOrderRepository;
    private final Environment environment;

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;

    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;


    private long userShiftId;

    @BeforeEach
    void setUp() {
        this.userShiftId = createUserWithTasks();
    }

    @Test
    void findWithNullFilters() {
        Page<PartnerReportOrder> orders = partnerReportOrderRepository.findAll(
                new PartnerReportOrderSpecification<>(null, null, environment),
                PageRequest.of(0, 10)
        );

        assertThat(orders.getContent()).isNotEmpty();
    }

    @Test
    void findWithFilters() {
        PartnerReportOrderParamsDto params = getParams();
        params.setGeneralizedTaskStatus(GENERALIZED_TASK_STATUS_IN_PROGRESS);
        log.info("Search with filters " + params);

        List<PartnerReportOrder> orders = partnerReportOrderRepository.findAll();
        log.info("Orders " + orders);
        long ordersSize = orders.size();
        Page<PartnerReportOrder> filteredOrders = partnerReportOrderRepository.findAll(
                new PartnerReportOrderSpecification<>(params, null, environment),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "deliveryTime"))
        );

        assertThat(filteredOrders.getContent())
                .describedAs("Всего заказов=%s, timeZone=%s", ordersSize, TimeZone.getDefault().toZoneId())
                .hasSize(3);
        assertThat(filteredOrders.getContent()).haveAtLeast(
                2, new Condition<>(order -> order.getCourierUid() == UID, "order with specified courier uid")
        );
        assertEqualsGeneralizedTaskStatusInProgress(filteredOrders);
    }

    @Test
    void findWithAddressFilter() {
        PartnerReportOrderParamsDto params = getParams();
        params.setAddress(TEST_ADDRESS);

        Page<PartnerReportOrder> filteredOrders = partnerReportOrderRepository.findAll(
                new PartnerReportOrderSpecification<>(params, null, environment),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "deliveryTime"))
        );

        assertEquals(filteredOrders.getContent().size(), 1);
    }

    @Test
    void findLockerOrder() {
        testDataFactory.addLockerDeliveryTask(userShiftId);

        PartnerReportOrderParamsDto params = new PartnerReportOrderParamsDto();
        params.setOrderTypes(Set.of(PartnerOrderType.LOCKER));
        log.info("Search with filters " + params);

        List<PartnerReportOrder> orders = partnerReportOrderRepository.findAll();
        log.info("Orders " + orders);
        long ordersSize = orders.size();
        Page<PartnerReportOrder> filteredOrders = partnerReportOrderRepository.findAll(
                new PartnerReportOrderSpecification<>(params, null, environment),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "deliveryTime"))
        );

        assertThat(filteredOrders.getContent())
                .describedAs("Всего заказов=%s, timeZone=%s", ordersSize, TimeZone.getDefault().toZoneId())
                .hasSize(1);
        assertThat(filteredOrders.getContent()).haveAtLeast(
                1, new Condition<>(order -> order.getOrderType() == PartnerOrderType.LOCKER, "order with locker type")
        );
        assertEqualsGeneralizedTaskStatusInProgress(filteredOrders);
    }

    @Test
    void findDropship() {
        testDataFactory.addDropshipTask(userShiftId);

        PartnerReportOrderParamsDto params = new PartnerReportOrderParamsDto();
        params.setOrderTypes(Set.of(PartnerOrderType.DROPSHIP));
        log.info("Search with filters " + params);

        List<PartnerReportOrder> orders = partnerReportOrderRepository.findAll();
        log.info("Orders " + orders);
        long ordersSize = orders.size();
        Page<PartnerReportOrder> filteredOrders = partnerReportOrderRepository.findAll(
                new PartnerReportOrderSpecification<>(params, null, environment),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "deliveryTime"))
        );

        assertThat(filteredOrders.getContent())
                .describedAs("Всего заказов=%s, timeZone=%s", ordersSize, TimeZone.getDefault().toZoneId())
                .hasSize(1);
        assertThat(filteredOrders.getContent()).haveAtLeast(
                1, new Condition<>(order -> order.getOrderType() == PartnerOrderType.DROPSHIP,
                        "not an order but rather a dropship request, but who cares")
        );
        assertEqualsGeneralizedTaskStatusInProgress(filteredOrders);
    }

    private void assertEqualsGeneralizedTaskStatusInProgress(Page<PartnerReportOrder> filteredOrders) {
        filteredOrders.getContent()
                .stream()
                .map(BasePartnerReportOrder::getGeneralizedTaskStatus)
                .forEach(s ->
                        assertEquals(s, GENERALIZED_TASK_STATUS_IN_PROGRESS)
                );
    }

    private PartnerReportOrderParamsDto getParams() {
        PartnerReportOrderParamsDto params = new PartnerReportOrderParamsDto();

        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        params.setCourierUid(userShift.getUser().getUid());
        LocalDate expectedDate = userShift.streamRoutePoints().findFirst().get().getExpectedDateTime()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        params.setArrivalDateFrom(expectedDate);
        params.setArrivalDateTo(expectedDate);
        List<Long> orderIds = userShift.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Order> orders = orderRepository.findAllById(orderIds);
        params.setDeliveryIntervalFrom(orders.stream()
                .map(Order::getDelivery)
                .map(od -> {
                    ZoneOffset offset =
                            dsZoneOffsetCachingService.getOffsetForDs(od.getOrder().getDeliveryServiceId());
                    return od.getDeliveryIntervalFrom().atZone(offset).toLocalTime();
                })
                .min(LocalTime::compareTo).get());
        params.setDeliveryIntervalTo(orders.stream()
                .map(Order::getDelivery)
                .map(od -> {
                    ZoneOffset offset =
                            dsZoneOffsetCachingService.getOffsetForDs(od.getOrder().getDeliveryServiceId());
                    return od.getDeliveryIntervalTo().atZone(offset).toLocalTime();
                })
                .max(LocalTime::compareTo).get());
        return params;
    }

    private long createUserWithTasks() {
        LocalDate date = LocalDate.now();
        User user = testUserHelper.findOrCreateUser(UID, date);
        Shift shift = testUserHelper.findOrCreateOpenShift(date);
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId);
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .build());
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .paymentType(OrderPaymentType.PREPAID)
                        .addressGenerateParam(
                                AddressGenerator.AddressGenerateParam.builder()
                                        .street(TEST_ADDRESS)
                                        .build())
                        .build());
        entityManager.flush();
        return userShiftId;
    }

}
