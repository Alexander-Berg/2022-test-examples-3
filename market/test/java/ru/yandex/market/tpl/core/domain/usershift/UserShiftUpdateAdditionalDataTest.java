package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstance;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.AdditionalVehicleDataService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.UpdateAdditionalVehicleDataDto;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftUpdateAdditionalDataTest extends TplAbstractTest {

    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TransactionTemplate transactionTemplate;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final VehicleGenerateService vehicleGenerateService;
    private final AdditionalVehicleDataService vehicleDataService;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;
    private final UserRepository userRepository;


    private User user;
    private Order order;
    private UserShift userShift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(3523601L);
        initTasks();
    }

    @Test
    void testAddVehicleAdditionalData() {
        var vehicleInstance = assignVehicleToUser(VehicleInstanceType.PUBLIC, null);
        assertThat(userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId())).isEmpty();

        String registrationNumber = "A098BC";
        String registrationNumberRegion = "000";
        var dto = UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .registrationNumber(registrationNumber)
                        .vehicle(vehicleInstance.getVehicle())
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        vehicleDataService.updateVehicleData(user, dto);

        assertVehicleDataIsUpdated(vehicleInstance, registrationNumber, registrationNumberRegion);
    }

    @Test
    void testUpdateVehicleAdditionalData() {
        var vehicleInstance = assignVehicleToUser(VehicleInstanceType.PUBLIC, null);
        assertThat(userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId())).isEmpty();

        var dto = UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .registrationNumber(vehicleInstance.getRegistrationNumber())
                        .vehicle(vehicleInstance.getVehicle())
                        .registrationNumberRegion(vehicleInstance.getRegistrationNumberRegion())
                        .build())
                .build();
        vehicleDataService.updateVehicleData(user, dto);

        String registrationNumber = "A098BC";
        String registrationNumberRegion = "000";

        transactionTemplate.execute(ts -> {
            var additionalData = userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            user = userShift.getUser();
            assertThat(additionalData).isNotNull();
            assertThat(additionalData.getVehicleRegisterNumber()).isNotEqualTo(registrationNumber + registrationNumberRegion);
            VehicleInstance userVehicleInstance = user.getVehicleInstances().get(0);
            assertThat(userVehicleInstance.getRegistrationNumber()).isNotEqualTo(registrationNumber);
            assertThat(userVehicleInstance.getRegistrationNumberRegion()).isNotEqualTo(registrationNumberRegion);
            return null;
        });

        dto = UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .registrationNumber(registrationNumber)
                        .vehicle(vehicleInstance.getVehicle())
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        vehicleDataService.updateVehicleData(user, dto);

        assertVehicleDataIsUpdated(vehicleInstance, registrationNumber, registrationNumberRegion);
    }

    private void assertVehicleDataIsUpdated(VehicleInstance vehicleInstance, String registrationNumber,
                                            String registrationNumberRegion) {

        transactionTemplate.execute(ts -> {
            var additionalData = userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(additionalData).isNotNull();
            assertThat(additionalData.getVehicle().getId()).isEqualTo(vehicleInstance.getVehicle().getId());
            assertThat(additionalData.getVehicleRegisterNumber()).isEqualTo(registrationNumber + registrationNumberRegion);
            assertThat(additionalData.getVehicleColor()).isEqualTo(vehicleInstance.getColor());
            VehicleInstance userVehicleInstance = user.getVehicleInstances().get(0);
            assertThat(userVehicleInstance.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(userVehicleInstance.getRegistrationNumberRegion()).isEqualTo(registrationNumberRegion);
            assertThat(userVehicleInstance.getInfoUpdatedAt()).isNotNull();
            return null;
        });
    }

    void initTasks() {
        transactionTemplate.execute(ts -> {
            Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

            order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .paymentType(OrderPaymentType.CASH)
                    .paymentStatus(OrderPaymentStatus.UNPAID)
                    .build());

            var createCommand = UserShiftCommand.Create.builder()
                    .userId(user.getId())
                    .shiftId(shift.getId())
                    .routePoint(helper.taskOrderPickup(clock.instant()))
                    .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                    .mergeStrategy(SimpleStrategies.NO_MERGE)
                    .build();

            userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            return null;
        });
    }

    private VehicleInstance assignVehicleToUser(VehicleInstanceType vehicleInstanceType, Instant infoUpdatedAt) {
        var vehicle = vehicleGenerateService.generateVehicle();
        return vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .type(vehicleInstanceType)
                .infoUpdatedAt(infoUpdatedAt)
                .users(List.of(user))
                .vehicle(vehicle)
                .build());
    }
}
