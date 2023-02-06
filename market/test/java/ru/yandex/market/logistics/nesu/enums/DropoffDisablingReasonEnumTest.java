package ru.yandex.market.logistics.nesu.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.pvz.client.logistics.model.DisableDropoffReason;

class DropoffDisablingReasonEnumTest {
    @ParameterizedTest
    @EnumSource(DropoffDisablingReason.class)
    @DisplayName("Все причины отключения дропофа соответсвуют причинам смены активности в market-tpl-pvz")
    void checkIfAllValuesConvertToPVZChangeActiveReasonEnum(DropoffDisablingReason reason) {
        Assertions.assertDoesNotThrow(() -> DisableDropoffReason.valueOf(reason.name()));
    }
}
