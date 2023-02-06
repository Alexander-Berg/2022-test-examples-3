package ru.yandex.market.ff4shops.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem;
import ru.yandex.market.ff4shops.AbstractTest;
import ru.yandex.market.ff4shops.api.model.auth.ClientRole;
import ru.yandex.market.ff4shops.api.model.order.DisabledRemovingFromOrderReason;
import ru.yandex.market.ff4shops.api.model.order.DisabledRemovingItemReason;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;

@DisplayName("Тест конвертации Enum")
public class EnumConverterTest extends AbstractTest {

    @ParameterizedTest
    @EnumSource(ClientRole.class)
    @DisplayName("Конвертация ClientRole")
    void clientRole(ClientRole innerValue) {
        softly.assertThat(EnumConverter.toEnum(
            innerValue,
            ru.yandex.market.checkout.checkouter.client.ClientRole.class
        )).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ReasonForNotAbleRemoveFromOrder.class)
    @DisplayName("Конвертация ReasonForNotAbleRemoveFromOrder")
    void disabledRemovingFromOrderReason(ReasonForNotAbleRemoveFromOrder innerValue) {
        softly.assertThat(EnumConverter.toEnum(
            innerValue,
            DisabledRemovingFromOrderReason.class
        )).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ReasonForNotAbleRemoveItem.class)
    @DisplayName("Конвертация ReasonForNotAbleRemoveItem")
    void reasonForNotAbleRemoveItem(ReasonForNotAbleRemoveItem innerValue) {
        softly.assertThat(EnumConverter.toEnum(
            innerValue,
            DisabledRemovingItemReason.class
        )).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(PossibleOrderChangeMethod.class)
    @DisplayName("Конвертация PossibleOrderChangeMethod")
    void possibleOrderChangeMethod(PossibleOrderChangeMethod innerValue) {
        softly.assertThat(EnumConverter.toEnum(
            innerValue,
            ru.yandex.market.ff4shops.model.enums.PossibleOrderChangeMethod.class
        )).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(PossibleOrderChangeType.class)
    @DisplayName("Конвертация PossibleOrderChangeType")
    void possibleOrderChangeType(PossibleOrderChangeType innerValue) {
        softly.assertThat(EnumConverter.toEnum(
            innerValue,
            ru.yandex.market.ff4shops.model.enums.PossibleOrderChangeType.class
        )).isNotNull();
    }
}
