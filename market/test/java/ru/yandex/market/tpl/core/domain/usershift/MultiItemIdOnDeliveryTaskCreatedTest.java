package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.common.model.delivery.Transaction;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class MultiItemIdOnDeliveryTaskCreatedTest extends TplAbstractTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private UserShift userShift;


    @BeforeEach
    @Transactional
    public void init() {
        transactionTemplate.executeWithoutResult(ts -> {
            user = testUserHelper.findOrCreateUser(1);
            userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });
    }

    @Test
    void createOrderDeliveryTaskWithOneClientReturn() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        assertThat(tod).isNotNull();
        assertThat(tod.getClientReturnId()).isEqualTo(clientReturn.getId());

        assertThat(tod.getMultiOrderId()).isEqualTo(clientReturn.getId().toString());
    }

    @Test
    void createOrderDeliveryTaskWithTwoSimilarClientReturns() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var clientReturn2 = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn2.getId(), deliveryTime
                )
        );
        assertThat(tod).isNotNull();
        assertThat(tod.getMultiOrderId()).isEqualTo(String.format("m_%d_%d", clientReturn.getId(),
                clientReturn2.getId()));
    }

    @Test
    void createOrderDeliveryTaskWithSimilarClientReturnAndOrder() {
        var geoPoint = GeoPointGenerator.generateLonLat();
        var cr = clientReturnGenerator.generateReturnFromClient();

        cr.getLogisticRequestPointFrom().setOriginalLatitude(geoPoint.getLatitude());
        cr.getLogisticRequestPointFrom().setOriginalLongitude(geoPoint.getLongitude());
        cr.getClient().getClientData().setPhone("phone1");
        clientReturnRepository.save(cr);

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);

        var clientReturnDeliveryTask = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, cr.getId(), deliveryTime
                )
        );

        var orderDeliveryTask = testDataFactory.addDeliveryTaskManual(user, userShift.getId(), routePointId,
                createOrderGenerateParam(geoPoint, "phone1", "10:00-14:00", 1));

        assertThat(orderDeliveryTask).isNotNull();
        assertThat(orderDeliveryTask.getMultiOrderId()).isEqualTo(
                String.format("m_%d_%d", orderDeliveryTask.getOrderId(), cr.getId())
        );
    }

    @Test
    void createOrderDeliveryTaskWithDifferentClientReturnAndOrder() {
        var geoPoint = GeoPointGenerator.generateLonLat();
        var cr = clientReturnGenerator.generateReturnFromClient();

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);

        var clientReturnDeliveryTask = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, cr.getId(), deliveryTime
                )
        );

        var orderDeliveryTask = testDataFactory.addDeliveryTaskManual(user, userShift.getId(), routePointId,
                createOrderGenerateParam(geoPoint, "phone1", "10:00-14:00", 1));

        assertThat(orderDeliveryTask).isNotNull();
        assertThat(orderDeliveryTask.getMultiOrderId()).isEqualTo(orderDeliveryTask.getOrderId().toString());
        clientReturnDeliveryTask = orderDeliveryTaskRepository.findByIdOrThrow(clientReturnDeliveryTask.getId());
        assertThat(clientReturnDeliveryTask.getMultiOrderId()).isEqualTo(clientReturnDeliveryTask.getClientReturnId().toString());

    }

    private OrderGenerateService.OrderGenerateParam createOrderGenerateParam(GeoPoint geoPoint,
                                                                             String clientPhone,
                                                                             String timeInterval,
                                                                             int volumeInCubicMeters) {
        return OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone(clientPhone)
                .deliveryInterval(LocalTimeInterval.valueOf(timeInterval))
                .dimensions(new Dimensions(BigDecimal.ONE, 100, 100, volumeInCubicMeters * 100))
                .build();
    }
}
