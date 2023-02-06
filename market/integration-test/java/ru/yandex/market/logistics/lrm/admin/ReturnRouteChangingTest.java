package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.provider.Arguments;

abstract class ReturnRouteChangingTest extends AbstractAdminIntegrationTest {
    @Nonnull
    protected static Stream<Arguments> invalidRoute() {
        return Stream.of(
            Arguments.of(
                "Лишние поля",
                "json/admin/change-route/invalid-route/invalid_excess_fields.json",
                "json/admin/change-route/validation-messages/invalid_excess_fields.json"
            ),
            Arguments.of(
                "Неправильные типы полей",
                "json/admin/change-route/invalid-route/invalid_field_types.json",
                "json/admin/change-route/validation-messages/invalid_field_types.json"
            ),
            Arguments.of(
                "Невалидный enum",
                "json/admin/change-route/invalid-route/invalid_enum.json",
                "json/admin/change-route/validation-messages/invalid_enum.json"
            )
        );
    }
}
