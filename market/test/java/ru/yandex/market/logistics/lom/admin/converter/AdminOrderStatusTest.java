package ru.yandex.market.logistics.lom.admin.converter;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;

class AdminOrderStatusTest extends AbstractTest {
    @DisplayName("Описания статусов начинаются с заглавной буквы")
    @ParameterizedTest
    @MethodSource("statusDescriptions")
    void allStatusDescriptionsStartWithCapitalLetter(String description) {
        softly.assertThat(description).matches(d -> Character.isUpperCase(description.charAt(0)));
    }

    static Stream<Arguments> statusDescriptions() {
        return Stream.of(AdminOrderStatus.values()).map(AdminOrderStatus::getTitle).map(Arguments::of);
    }
}
