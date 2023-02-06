package ru.yandex.market.deliverycalculator.storage.model.yadelivery.modifier;

import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.deliverycalculator.model.DeliveryModifierPriorityGroup;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Action;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.deliverycalculator.model.DeliveryModifierPriorityGroup.WITHOUT_CARRIER_WITHOUT_REGION;
import static ru.yandex.market.deliverycalculator.model.DeliveryModifierPriorityGroup.WITHOUT_CARRIER_WITH_REGION;
import static ru.yandex.market.deliverycalculator.model.DeliveryModifierPriorityGroup.WITH_CARRIER_WITHOUT_REGION;
import static ru.yandex.market.deliverycalculator.model.DeliveryModifierPriorityGroup.WITH_CARRIER_WITH_REGION;

class DeliveryModifierTest {

    @ParameterizedTest
    @MethodSource("argumentsForGetPriorityGroup")
    void testGetPriorityGroup(DeliveryModifier modifier, DeliveryModifierPriorityGroup expected) {
        assertEquals(expected, modifier.getPriorityGroup());
    }

    static Stream<Arguments> argumentsForGetPriorityGroup() {
        return Stream.of(
                Arguments.of(new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L, 2L))
                                .withDeliveryDestinations(Sets.newHashSet(223, 224))
                                .build()).build(), WITH_CARRIER_WITH_REGION),
                Arguments.of(new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L, 2L))
                                .build()).build(), WITH_CARRIER_WITHOUT_REGION),
                Arguments.of(new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withCondition(new Condition.Builder()
                                .withDeliveryDestinations(Sets.newHashSet(223, 224))
                                .build()).build(), WITHOUT_CARRIER_WITH_REGION),
                Arguments.of(new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withCondition(new Condition.Builder()
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.POST))
                                .build()).build(), WITHOUT_CARRIER_WITHOUT_REGION),
                Arguments.of(new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .build(), WITHOUT_CARRIER_WITHOUT_REGION)
        );
    }
}
