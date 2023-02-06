package ru.yandex.market.delivery.transport_manager.admin;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRegisterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminTransportationUnitTypeTest {

    @Test
    void adminEnumEqualToServiceEnum() {
        for (UnitType value : UnitType.values()) {
            assertThat(AdminRegisterUnitType.valueOf(value.name()));
        }
    }

}
