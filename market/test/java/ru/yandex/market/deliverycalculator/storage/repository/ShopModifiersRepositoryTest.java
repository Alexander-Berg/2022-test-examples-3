package ru.yandex.market.deliverycalculator.storage.repository;

import java.util.ArrayList;
import java.util.Optional;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.ShopModifiersEntity;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Action;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тест для {@link ShopModifiersRepository}.
 */
class ShopModifiersRepositoryTest extends FunctionalTest {

    @Autowired
    private ShopModifiersRepository tested;

    /**
     * Тест на {@link ShopModifiersRepository#save(Object)}.
     * Случай: магазинные модификаторы успешно вставлены.
     */
    @DbUnitDataSet(before = "database/createShopModifiers.before.csv", after = "database/createShopModifiers.after.csv")
    @Test
    void testSave() {
        //модификаторы магазина с идентификатором 1
        ShopModifiersEntity shop1Modifiers = new ShopModifiersEntity();
        shop1Modifiers.setShopId(1L);
        shop1Modifiers.setModifiers(asList(new DeliveryModifier.Builder()
                        .withId(1L)
                        .withTimestamp(1234567L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(51L))
                                .withDeliveryDestinations(Sets.newHashSet(21))
                                .build())
                        .withAction(new Action.Builder()
                                .withIsCarrierTurnedOn(true)
                                .build())
                        .build(),
                new DeliveryModifier.Builder()
                        .withId(2L)
                        .withTimestamp(1234567L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(9L))
                                .withDeliveryDestinations(Sets.newHashSet(250))
                                .build())
                        .withAction(new Action.Builder()
                                .withIsCarrierTurnedOn(true)
                                .build())
                        .build()));
        //модификаторы магазина с идентификатором 2
        ShopModifiersEntity shop2Modifiers = new ShopModifiersEntity();
        shop2Modifiers.setShopId(2L);
        shop2Modifiers.setModifiers(new ArrayList<>());

        tested.saveAll(asList(shop1Modifiers, shop2Modifiers));

        assertThat(tested.findById(2L).orElse(null), new ReflectionAssertMatcher<>(shop2Modifiers));
        assertThat(tested.findById(1L).orElse(null), new ReflectionAssertMatcher<>(shop1Modifiers));
    }

    @DbUnitDataSet(before = "database/createShopModifiers.after.csv")
    @Test
    void testSearchAndNotFound() {
        Optional<ShopModifiersEntity> entityInDatabase = tested.findById(3L);
        assertFalse(entityInDatabase.isPresent());
    }
}
