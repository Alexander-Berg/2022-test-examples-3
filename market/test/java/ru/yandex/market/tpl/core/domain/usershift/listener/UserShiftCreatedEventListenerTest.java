package ru.yandex.market.tpl.core.domain.usershift.listener;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftCreatedEventListenerTest extends TplAbstractTest {

    private static final Long UID = 1234L;

    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final UserRepository userRepository;
    private final UserShiftRepository userShiftRepository;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;

    private User user;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(UID);
    }

    @Test
    void testCreateAndAssignVehicle() {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleColor = vehicleGenerateService.generateVehicleColor("White");
        var vehicleColorId = vehicleColor.getId();

        String registrationNumber = "A001MP";
        String vehicleRegion = "777";
        var userVehicle =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .users(List.of(user))
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(vehicleRegion)
                        .color(vehicleColor)
                        .vehicle(vehicle)
                        .build());
        var userVehicleId = userVehicle.getId();

        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        var additionalData = userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();
        assertThat(additionalData.getVehicle().getId()).isEqualTo(vehicle.getId());
        assertThat(additionalData.getVehicleColor().getId()).isEqualTo(vehicleColorId);
        assertThat(additionalData.getVehicleRegisterNumber()).isEqualTo(registrationNumber + vehicleRegion);

        user = userRepository.findByIdWithVehicles(user.getId()).orElseThrow();
        assertThat(user.getVehicleInstances()).isNotEmpty();
        assertThat(user.getVehicleInstances().get(0).getId()).isEqualTo(userVehicleId);
        assertThat(user.getVehicleInstances().get(0).getColor().getId()).isEqualTo(vehicleColorId);

    }

}
