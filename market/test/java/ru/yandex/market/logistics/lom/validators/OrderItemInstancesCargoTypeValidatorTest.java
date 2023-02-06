package ru.yandex.market.logistics.lom.validators;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;

@DisplayName("Тест валидатора соответствия карготипов и кизов внутри товарной позиции")
class OrderItemInstancesCargoTypeValidatorTest extends AbstractTest {

    OrderItemInstancesCargoTypeValidator validator = new OrderItemInstancesCargoTypeValidator(
        new EnumConverter()
    );

    @AfterEach
    void after() {
        softly.assertAll();
    }

    @Test
    @DisplayName("Пустой список и набор айтемов")
    void emptyItems() {
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(List.of()))
            .as("Должно быть тру").isTrue();

        softly.assertThat(validator.validateItemsForCargoTypesAndCis(Set.of()))
            .as("Должно быть тру")
            .isTrue();
    }

    @Test
    @DisplayName("Список без нужных карготипов")
    void listWithoutCargo() {
        List<Item> items = List.of(
          new Item.ItemBuilder("test", 1, null, null, List.of(CargoType.ADULT)).build()
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть тру")
            .isTrue();
    }

    @Test
    @DisplayName("Набор без нужных карготипов")
    void setWithoutCargoTypes() {
        Set<OrderItem> items = Set.of(
            new OrderItem()
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть тру")
            .isTrue();
    }

    @Test
    @DisplayName("Набор c обязательным карготипом, но пустым списком кизов")
    void setWithCargoTypesFailed() {
        Set<OrderItem> items = Set.of(
            new OrderItem().setCargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED))
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть false, ибо нет кизов и кол-ва")
            .isFalse();
    }

    @Test
    @DisplayName("Набор в котором есть нужный карготип, кол-во и киз")
    void setOfOneWithCargoTypesTrue() {
        Set<OrderItem> items = Set.of(
            new OrderItem()
                .setCargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED))
                .setCount(1)
                .setInstances(List.of(Map.of("cis", "123")))
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть true")
            .isTrue();
    }

    @Test
    @DisplayName("Набор с двумя айтемами где, второй неправильный")
    void setOfTwoItemsWithCargoTypesFalse() {
        Set<OrderItem> items = Set.of(
            new OrderItem()
                .setCargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED))
                .setCount(1)
                .setInstances(List.of(Map.of("cis", "123"))),
            new OrderItem()
                .setCargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED))
                .setCount(2)
                .setInstances(List.of(Map.of("cis", "123")))
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть false, ибо кизов меньше, чем кол-во товаров в айтеме")
            .isFalse();
    }

    @Test
    @DisplayName("Набор где, ключевое слово КИЗ неправильно написано")
    void setOfOneWithWrongCisMarkerFalse() {
        Set<OrderItem> items = Set.of(
            new OrderItem()
                .setCargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED))
                .setCount(1)
                .setInstances(List.of(Map.of("cisss", "123")))
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("Должно быть false, нет ключевого слова cis")
            .isFalse();
    }

    @Test
    @DisplayName("CIS карготипы должны быть взаимоисключающие для одного айтема")
    void setOfOneWithWrongWrongSetOfCargoTypesFalse() {
        Set<OrderItem> items = Set.of(
            new OrderItem()
                .setCargoTypes(
                    Set.of(
                        ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_REQUIRED,
                        ru.yandex.market.logistics.lom.model.enums.CargoType.CIS_DISTINCT
                    )
                )
                .setCount(1)
                .setInstances(List.of(Map.of("cis", "123")))
        );
        softly.assertThat(validator.validateItemsForCargoTypesAndCis(items))
            .as("CIS карготип должен быть один для айтема")
            .isFalse();
    }
}
