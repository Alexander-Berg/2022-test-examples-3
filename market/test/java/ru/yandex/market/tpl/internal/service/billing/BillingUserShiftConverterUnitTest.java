package ru.yandex.market.tpl.internal.service.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.client.billing.dto.BillingShiftType;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.vehicle.brand.VehicleType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

class BillingUserShiftConverterUnitTest {

    private final BillingUserShiftConverter converter = new BillingUserShiftConverter();

    @Test
    void convert_for_VeloShift() {
        //given
        var billingUserShift = OBJECT_GENERATOR.nextObject(BillingUserShiftTest.class);
        billingUserShift.setVehicleType(VehicleType.BICYCLE);

        //when
        var userShiftDto = converter.convert(billingUserShift, Map.of(
                billingUserShift.getId(),
                BigDecimal.TEN
        ));

        //then
        asserts(userShiftDto, billingUserShift);
        assertEquals(BillingShiftType.VELO, userShiftDto.getShiftType());
    }

    @Test
    void convert_for_AvtoShift() {
        //given
        var billingUserShift = OBJECT_GENERATOR.nextObject(BillingUserShiftTest.class);
        billingUserShift.setVehicleType(VehicleType.CAR);

        //when
        var userShiftDto = converter.convert(billingUserShift, Map.of(
                billingUserShift.getId(),
                BigDecimal.TEN
        ));

        //then
        asserts(userShiftDto, billingUserShift);
        assertEquals(BillingShiftType.AVTO, userShiftDto.getShiftType());
    }

    private void asserts(BillingUserShiftDto dto, UserShiftRepository.BillingUserShift src) {
        assertEquals(src.getId(), dto.getId());
        //custom assert shiftType depends on Vehicle type
        assertEquals(src.getShiftDate(), dto.getShiftDate());
        assertEquals(src.getSortingCenterId(), dto.getSortingCenterId());
        //todo append another field checks
    }

    @Getter
    @Setter
    private static class BillingUserShiftTest implements UserShiftRepository.BillingUserShift {
        private Long id;
        private LocalDate shiftDate;
        private Long sortingCenterId;
        private Long userId;
        private String userDsmId;
        private Long companyId;
        private String companyDsmId;
        private RoutingVehicleType routingVehicleType;
        private VehicleType vehicleType;
        private Long transportTypeId;
        private String vehicleNumber;
        private Integer takenOrderCount;
        private Integer takenFashionOrderCount;
        private BigDecimal transitDistance;
    }
}
