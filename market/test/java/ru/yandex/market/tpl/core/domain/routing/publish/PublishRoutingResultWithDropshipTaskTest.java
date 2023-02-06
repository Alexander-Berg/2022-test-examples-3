package ru.yandex.market.tpl.core.domain.routing.publish;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.RoutingResultPublisher;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;
import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PublishRoutingResultWithDropshipTaskTest extends TplAbstractTest {

    private String processingId = "549a4c6f-77ed132a-9ac024ad-9eb768ea";
    private final Long order2Id = 100500L;

    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;

    private final TestUserHelper testUserHelper;

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final UserShiftRepository userShiftRepository;
    private final RoutingLogDao routingLogDao;
    private final PickupPointRepository pickupPointRepository;


    private final RoutingResultPublisher<Shift> publishRoutingResultManager;
    private final TestTplRoutingFactory testTplRoutingFactory;


    private Order order1;
    private Movement movement;
    private User courier1;
    private User courier2;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void publishResult() throws Exception {
        processingId = "549a4c6f-77ed132a-9ac024ad-9eb768ea";
        LocalDate shiftDate = LocalDate.of(2021, 2, 3);

        courier1 = testUserHelper.findOrCreateUser(12345601L, shiftDate);
        courier2 = testUserHelper.findOrCreateUser(12345602L, shiftDate);
        Shift shift = createShift(shiftDate);
        var pp = pickupPointRepository.save(generatePickupPoint(100500L));
        this.clearAfterTest(pp);

        order1 = createOrder("20210202001");
        // Сохраняем order в обход hibernate, для того что бы избежать кэширования entity
        nativeCreateOrder(order2Id, "20210202002", pp.getId());


        movement = createMovement("M202102001");

        createRoutingLogRecord("/publishRoutingResult/withDropships/request.json", "/publishRoutingResult" +
                        "/withDropships/result.json",
                order1.getId(),
                order2Id,
                courier1.getId(),
                courier2.getId(),
                movement.getId());

        RoutingResultWithShiftDate routingResult =
                routingLogDao.findResultByProcessingId(processingId).orElseThrow();

        publishRoutingResultManager.publishRouting(routingResult);

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).isNotEmpty().hasSize(2);
    }


    @Test
    void publishResult2() throws Exception {
        LocalDate shiftDate = LocalDate.of(2021, 2, 3);

        courier1 = testUserHelper.findOrCreateUser(12345602L, shiftDate);
        Shift shift = createShift(shiftDate);
        var pp = pickupPointRepository.save(generatePickupPoint(100501L));
        this.clearAfterTest(pp);

        order1 = createOrder("20210202002");

        Order order2 = createOrder("20210202003");

        movement = createMovement("M202102002");

        processingId = createRoutingLogRecord("/publishRoutingResult/withDropships/request.json",
                "/publishRoutingResult" +
                        "/withDropships/result_mix_dropship_delivery.json",
                courier1.getId(),
                order2.getId(),
                movement.getId());

        RoutingResultWithShiftDate routingResult =
                routingLogDao.findResultByProcessingId(processingId).orElseThrow();

        publishRoutingResultManager.publishRouting(routingResult);

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).isNotEmpty().hasSize(1);
    }

    Shift createShift(LocalDate shiftDate) {

        return testUserHelper.findOrCreateShiftForScWithStatus(
                shiftDate,
                SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);
    }

    private String createRoutingLogRecord(String requestPath, String resultPath, Object... args) throws Exception {
        String result = String.format(
                TplCoreTestUtils.readResourceAsString(resultPath),
                args
        );

        var routingRequest = TplCoreTestUtils.mapFromResource(requestPath, RoutingRequest.class);
        var routingResult = TplObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(result, RoutingResult.class);


        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);

        return routingResult.getProcessingId();
    }

    private Movement createMovement(String externalId) {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .externalId(externalId)
                .build()
        );
    }

    private Order createOrder(String externalOrderId) {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build()
        );
    }

    private void nativeCreateOrder(Long orderId, String externalOrderId, Long pickPointId) {

        var oc = orderGenerateService.generateOrderCommand(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build());

        jdbcTemplate.update("INSERT INTO orders " +
                        "(id, external_order_id, payment_type, payment_status, created_at, updated_at, " +
                        "delivery_status, delivery_service_id, order_flow_status, order_flow_status_updated_at, " +
                        "pickup, sender_id, warehouse_id, weight, length, width, height, warehouse_return_id, " +
                        "ds_api_checkpoint, pickup_point_id, buyer_yandex_uid) " +
                        "VALUES (:orderId, :external_order_id, 'PREPAID', 'PAID', now(), " +
                        "now(), 'NOT_DELIVERED', -1, 'SORTING_CENTER_ARRIVED', " +
                        "now(), true, null, null, 1.2, 10, 20, 30,null,30,:pickPointId, null)",
                Map.of("external_order_id", externalOrderId,
                        "orderId", orderId,
                        "pickPointId", pickPointId)
        );

        nativeCreateDeliveryOrder(orderId, oc.getOrderDelivery());
        nativeCreatePlace(orderId);
    }

    private void nativeCreateDeliveryOrder(Long orderId, OrderDelivery od) {
        jdbcTemplate.update("INSERT INTO order_delivery (id, order_id, recipient_fio, recipient_phone, " +
                        "recipient_email, recipient_notes, longitude, latitude, city, street, house," +
                        " apartment, floor, entry_phone, address, created_at, updated_at, delivery_interval_from, " +
                        "delivery_interval_to, courier_notes, region_id, building, housing, entrance, " +
                        "precise_longitude, precise_latitude, original_delivery_date, original_region_id, clarified) " +
                        "VALUES (100500, :orderId, :recipient_fio, '+79051234567', 'ivan@yandex.ru', 'Не опаздывайте," +
                        " пожалуйста!', 37.434201, 55.762958, 'Москва', 'Ильи Толстого', '10', '1', '3', null, 'Ильи " +
                        "Толстого д. 10, кв. 1', now(), now(), :delivery_interval_from, " +
                        ":delivery_interval_to, null, null, null, null, null, null, null, :original_delivery_date, " +
                        "null, null);",
                Map.of("orderId", orderId,
                        "recipient_fio", od.getRecipientFio(),
                        "delivery_interval_from", Timestamp.from(od.getDeliveryIntervalFrom()),
                        "delivery_interval_to", Timestamp.from(od.getDeliveryIntervalTo()),
                        "original_delivery_date", Timestamp.from(od.getOriginalDeliveryDate())));
    }

    private void nativeCreatePlace(Long orderId) {
        jdbcTemplate.update("INSERT INTO order_places (id, order_id, barcode, weight, length, width, height, " +
                "yandex_id, warehouse_id) " +
                "VALUES (100500, :orderId, '8502054', 1.2, 10, 20, 30, null, '20210202002')", Map.of("orderId",
                orderId));
    }

    private PickupPoint generatePickupPoint(Long logisticPointId) {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode("test" + logisticPointId);
        pickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        pickupPoint.setType(PickupPointType.LOCKER);
        pickupPoint.setLogisticPointId(logisticPointId);
        return pickupPoint;
    }
}
