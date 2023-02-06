package ru.yandex.market.tpl.carrier.core.domain.duty;

import java.time.Instant;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.commands.DutyCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTestV2
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DutyCommandServiceTest {
    private final DutyCommandService dutyCommandService;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;

    @Test
    void testCreateDuty() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        testUserHelper.deliveryService(123L, Set.of(company));
        OrderWarehouse orderWarehouse = orderWarehouseGenerator.generateWarehouse();
        Duty duty = dutyCommandService.create(
                DutyCommand.Create.builder()
                        .warehouseId(Long.parseLong(orderWarehouse.getYandexId()))
                        .deliveryServiceId(123L)
                        .dutyStartTime(Instant.parse("2021-01-01T10:00:00.00Z"))
                        .dutyEndTime(Instant.parse("2021-01-01T22:00:00.00Z"))
                        .pallets(33)
                        .build()
        );
        assertThat(duty).isNotNull();
        assertThat(duty).extracting("pallets").isEqualTo(33);
        assertThat(duty).extracting("status").isEqualTo(DutyStatus.CREATED);

    }

}