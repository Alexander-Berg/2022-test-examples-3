package ru.yandex.market.logistics.nesu.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.nesu.admin.model.enums.AdminDropoffDisablingSubtaskType;

@DisplayName("Проверка соответствия типов подзадач отключения дропоффа админским типам")
class DropoffDisablingSubtaskTypeTest {

    @ParameterizedTest
    @EnumSource(DropoffDisablingSubtaskType.class)
    @DisplayName("Все причины типы подзадач отключения дропофа соответсвуют админским")
    void checkIfAllDropoffDisablingSubtypesConvertToAdminSubtypes(DropoffDisablingSubtaskType subtype) {
        Assertions.assertDoesNotThrow(() -> AdminDropoffDisablingSubtaskType.valueOf(subtype.name()));
    }
}
