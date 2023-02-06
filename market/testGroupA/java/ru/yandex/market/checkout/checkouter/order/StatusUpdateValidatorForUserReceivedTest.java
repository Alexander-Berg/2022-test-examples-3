package ru.yandex.market.checkout.checkouter.order;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES;

public class StatusUpdateValidatorForUserReceivedTest extends AbstractWebTestBase {

    @Autowired
    protected CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private StatusUpdateValidator statusUpdateValidator;

    private static Stream<Arguments> deniedFromUserReceivedToDeliverySubstatusesWithRoles() {
        var allRoles = EnumSet.of(ClientRole.SYSTEM, ClientRole.USER);
        return EnumSet.allOf(OrderSubstatus.class)
                .stream()
                .filter(substatus -> substatus.getStatus() == OrderStatus.DELIVERY
                        && substatus != OrderSubstatus.USER_RECEIVED && substatus != OrderSubstatus.READY_FOR_LAST_MILE)
                .flatMap(substatus -> allRoles.stream().map(role -> new Object[]{substatus, role}))
                .map(Arguments::of);
    }

    @BeforeEach
    void setUp() {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, true);
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryFeature.class, mode = EXCLUDE, names = {"ON_DEMAND", "DEFERRED_COURIER"})
    void wrongFromUserReceivedToReadyForLastMile_whenSystemRoleAndLavkaOrder(DeliveryFeature feature) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(feature));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = ClientInfo.SYSTEM;

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
        });
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryFeature.class, mode = EXCLUDE, names = {"ON_DEMAND", "DEFERRED_COURIER"})
    void wrongFromUserReceivedToReadyForLastMile_whenSystemRoleAndLavkaOrderWithoutToggle(DeliveryFeature feature) {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        wrongFromUserReceivedToReadyForLastMile_whenSystemRoleAndLavkaOrder(feature);
    }

    @ParameterizedTest
    @EnumSource(value = ClientRole.class, mode = EXCLUDE, names = {"SYSTEM"})
    void wrongFromUserReceivedToReadyForLastMile_whenNonSystemRoleAndOnDemand(ClientRole role) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.ON_DEMAND));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = new ClientInfo(role, 123L);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
        });
    }

    @ParameterizedTest
    @EnumSource(value = ClientRole.class, mode = EXCLUDE, names = {"SYSTEM"})
    void wrongFromUserReceivedToReadyForLastMile_whenNonSystemRoleAndDeferredCourier(ClientRole role) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = new ClientInfo(role, 123L);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
        });
    }

    @ParameterizedTest
    @EnumSource(value = ClientRole.class, mode = EXCLUDE, names = {"SYSTEM"})
    void wrongFromUserReceivedToReadyForLastMile_whenNonSystemRoleAndDeferredCourierWithoutToggle(ClientRole role) {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        wrongFromUserReceivedToReadyForLastMile_whenNonSystemRoleAndDeferredCourier(role);
    }

    @Test
    void successFromUserReceivedToReadyForLastMile_whenSystemAndOnDemand() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.ON_DEMAND));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = ClientInfo.SYSTEM;

        statusUpdateValidator.validateStatusUpdate(
                order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
    }

    @Test
    void successFromUserReceivedToReadyForLastMile_whenSystemAndDeferredCourier() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = ClientInfo.SYSTEM;

        statusUpdateValidator.validateStatusUpdate(
                order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
    }

    @Test
    void wrongFromUserReceivedToReadyForLastMile_whenSystemAndDeferredCourierWithoutToggle() {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = ClientInfo.SYSTEM;

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE, clientInfo);
        });
    }

    @ParameterizedTest
    @MethodSource("deniedFromUserReceivedToDeliverySubstatusesWithRoles")
    void wrongFromUserReceivedToAnyButReadyForLastMile_whenOnDemand(OrderSubstatus newSubstatus,
                                                                    ClientRole role) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.ON_DEMAND));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = new ClientInfo(role, 123L);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, newSubstatus, clientInfo);
        });
    }

    @ParameterizedTest
    @MethodSource("deniedFromUserReceivedToDeliverySubstatusesWithRoles")
    void wrongFromUserReceivedToAnyButReadyForLastMile_whenDeferredCourier(OrderSubstatus newSubstatus,
                                                                    ClientRole role) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = new ClientInfo(role, 123L);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, newSubstatus, clientInfo);
        });
    }

    @ParameterizedTest
    @MethodSource("deniedFromUserReceivedToDeliverySubstatusesWithRoles")
    void wrongFromUserReceivedToAnyButReadyForLastMile_whenOnDeferredCourierToggle(OrderSubstatus newSubstatus,
                                                                                 ClientRole role) {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        wrongFromUserReceivedToAnyButReadyForLastMile_whenDeferredCourier(newSubstatus, role);
    }

    @ParameterizedTest
    @MethodSource("deniedFromUserReceivedToDeliverySubstatusesWithRoles")
    void wrongFromUserReceivedToAnyDelivery_whenNotOnDemandAndNotDeferredCourier(OrderSubstatus newSubstatus,
                                                            ClientRole role) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.USER_RECEIVED);

        ClientInfo clientInfo = new ClientInfo(role, 123L);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            statusUpdateValidator.validateStatusUpdate(
                    order, OrderStatus.DELIVERY, newSubstatus, clientInfo);
        });
    }

    @ParameterizedTest
    @MethodSource("deniedFromUserReceivedToDeliverySubstatusesWithRoles")
    void wrongFromUserReceivedToAnyDelivery_whenNotDeferredCourierdWithoutToggle(OrderSubstatus newSubstatus,
                                                                         ClientRole role) {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        wrongFromUserReceivedToAnyDelivery_whenNotOnDemandAndNotDeferredCourier(newSubstatus, role);
    }
}
