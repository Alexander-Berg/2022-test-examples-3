package ru.yandex.market.delivery.mdbapp.controller.admin.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.mdbapp.components.logging.OrderEventAction;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.controller.admin.enums.AdminEventAction;
import ru.yandex.market.delivery.mdbapp.controller.admin.enums.AdminFailCauseType;
import ru.yandex.market.delivery.mdbapp.integration.converter.EnumConverter;

@DisplayName("Конвертация перечислений из внутренних представлений во внешние")
public class AdminEnumConverterTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(OrderEventAction.class)
    @DisplayName("OrderEventAction -> AdminEventAction")
    public void orderEventActionFromModel(OrderEventAction value) {
        Assertions.assertNotNull(enumConverter.convert(value, AdminEventAction.class));
    }

    @ParameterizedTest
    @EnumSource(AdminEventAction.class)
    @DisplayName("AdminEventAction -> OrderEventAction")
    public void orderEventActionFromAdmin(AdminEventAction value) {
        Assertions.assertNotNull(enumConverter.convert(value, OrderEventAction.class));
    }

    @ParameterizedTest
    @EnumSource(FailCauseType.class)
    @DisplayName("FailCauseType -> AdminFailCauseType")
    public void failCauseTypeFromModel(FailCauseType value) {
        Assertions.assertNotNull(enumConverter.convert(value, AdminFailCauseType.class));
    }

    @ParameterizedTest
    @EnumSource(AdminFailCauseType.class)
    @DisplayName("AdminFailCauseType -> FailCauseType")
    public void failCauseTypeFromAdmin(AdminFailCauseType value) {
        Assertions.assertNotNull(enumConverter.convert(value, FailCauseType.class));
    }
}
