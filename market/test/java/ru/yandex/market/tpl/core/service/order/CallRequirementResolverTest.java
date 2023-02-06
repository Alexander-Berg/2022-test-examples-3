package ru.yandex.market.tpl.core.service.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.CALL_REQUIRED;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.CONTACTLESS_DELIVERY_PREFIX;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CoreTestV2
public class CallRequirementResolverTest {

    private final CallRequirementResolver callRequirementResolver;
    private final TransferTypeResolver transferTypeResolver;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;

    @BeforeEach
    void init() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.NEAR_THE_DOOR_CONTAINS_CHECK_ENABLED, false);
    }

    @Test
    void shouldHaveOrderNearTheDoor() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes(CONTACTLESS_DELIVERY_PREFIX);
        assertTrue(transferTypeResolver.isNearTheDoor(order));
    }

    @Test
    void shouldHaveOrderDoNotCall() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_DELIVERY_PREFIX);
        assertTrue(callRequirementResolver.hasDoNotCallNotes(order));
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(DO_NOT_CALL);
    }

    @Test
    void shouldHaveOrderDoNotCallWithFlag() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes("");
        order.getDelivery().setCallRequirement(DO_NOT_CALL);
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(DO_NOT_CALL);
    }

    @Test
    void shouldHaveOrderCallRequired() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes("");
        order.getDelivery().setCallRequirement(null);
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(CALL_REQUIRED);
    }

    @Test
    void shouldHaveOrderCallRequiredWithOverride() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_DELIVERY_PREFIX);
        order.getDelivery().setCallRequirement(CALL_REQUIRED);
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(CALL_REQUIRED);
    }

    @Test
    void shouldHaveOrderDoNotCallAndNearTheDoor() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes(CONTACTLESS_DELIVERY_PREFIX + DO_NOT_CALL_DELIVERY_PREFIX);
        assertTrue(callRequirementResolver.hasDoNotCallNotes(order));
        assertTrue(transferTypeResolver.isNearTheDoor(order));
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(DO_NOT_CALL);
    }

    @Test
    void shouldNotHaveOrderDoNotCallAndNearTheDoor() {
        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_DELIVERY_PREFIX + CONTACTLESS_DELIVERY_PREFIX);
        assertTrue(callRequirementResolver.hasDoNotCallNotes(order));
        assertFalse(transferTypeResolver.isNearTheDoor(order));
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(DO_NOT_CALL);
    }

    @Test
    void shouldHaveOrderDoNotCallAndNearTheDoorWithFlag() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.NEAR_THE_DOOR_CONTAINS_CHECK_ENABLED, true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_DELIVERY_PREFIX + CONTACTLESS_DELIVERY_PREFIX);
        assertTrue(callRequirementResolver.hasDoNotCallNotes(order));
        assertTrue(transferTypeResolver.isNearTheDoor(order));
        assertThat(callRequirementResolver.getCallRequirement(List.of(order))).isEqualTo(DO_NOT_CALL);
    }

    private Order newOrder() {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
    }
}
