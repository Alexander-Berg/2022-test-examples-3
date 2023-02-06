package ru.yandex.market.tpl.core.domain.user;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstance;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceRepository;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class UserVehicleInstanceTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final TransactionTemplate transactionTemplate;
    private final UserCommandService commandService;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserRepository userRepository;
    private final VehicleInstanceRepository vehicleInstanceRepository;


    private User user;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(3523601L);
    }

    @Test
    void testAddNewVehicleInstance() {
        assertThat(user.getVehicleInstances()).isEmpty();
        var vehicle = vehicleGenerateService.generateVehicle();
        var color = vehicleGenerateService.generateVehicleColor("Black");

        String registrationNumber = "A111AA";
        String registrationNumberRegion = "789";
        var command = UserCommand.AddOrUpdateVehicleInstance.builder()
                .userId(user.getId())
                .vehicleInstanceDto(UserCommand.AddOrUpdateVehicleInstance.UpdateVehicleInstanceDto.builder()
                        .type(VehicleInstanceType.PUBLIC)
                        .vehicle(vehicle)
                        .vehicleColor(color)
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        commandService.addOrUpdateVehicleInstance(command);

        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(user.getVehicleInstances()).isNotEmpty();
            var vehicleInstance = user.getVehicleInstances().get(0);
            assertThat(vehicleInstance.getId()).isNotNull();
            assertThat(vehicleInstance.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(vehicleInstance.getRegistrationNumberRegion()).isEqualTo(registrationNumberRegion);
            assertThat(vehicleInstance.getColor()).isEqualTo(color);
            assertThat(vehicleInstance.getInfoUpdatedAt()).isNull();
            assertThat(vehicleInstance.getRegistrationNumberCountry()).isEqualTo(VehicleRegistrationNumberCountry.RUS);
            return null;
        });
    }

    @DisplayName("Привязываем одно ТС сразу 2 курьерам, заодно обновляем данные")
    @Test
    void testAddExistingVehicleInstance() {
        assertThat(user.getVehicleInstances()).isEmpty();
        var vehicle = vehicleGenerateService.generateVehicle();
        var color = vehicleGenerateService.generateVehicleColor("Black");
        String registrationNumber = "A111AA";
        String registrationNumberRegion = "789";
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .type(VehicleInstanceType.PERSONAL)
                        .users(List.of(user))
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationNumberRegion)
                        .color(color)
                        .vehicle(vehicle)
                        .build());
        user = userRepository.findByIdWithVehicles(user.getId()).orElseThrow();
        assertThat(user.getVehicleInstances()).isNotEmpty();
        assertThat(user.getVehicleInstances().get(0).getId()).isEqualTo(vehicleInstance.getId());
        assertThat(user.getVehicleInstances().get(0).getType()).isEqualTo(VehicleInstanceType.PERSONAL);
        assertThat(user.getVehicleInstances().get(0).getColor().getId()).isEqualTo(color.getId());

        var secondUser = userHelper.findOrCreateUser(123456L);
        var newColor = vehicleGenerateService.generateVehicleColor("White");
        var command = UserCommand.AddOrUpdateVehicleInstance.builder()
                .userId(secondUser.getId())
                .vehicleInstanceDto(UserCommand.AddOrUpdateVehicleInstance.UpdateVehicleInstanceDto.builder()
                        .type(VehicleInstanceType.PUBLIC)
                        .vehicle(vehicle)
                        .vehicleColor(newColor)
                        .vehicleInstanceId(vehicleInstance.getId())
                        .registrationNumber(vehicleInstance.getRegistrationNumber())
                        .registrationNumberRegion(vehicleInstance.getRegistrationNumberRegion())
                        .build())
                .build();
        commandService.addOrUpdateVehicleInstance(command);

        secondUser = userRepository.findByIdWithVehicles(secondUser.getId()).orElseThrow();
        assertThat(secondUser.getVehicleInstances()).isNotEmpty();
        assertThat(secondUser.getVehicleInstances().get(0).getId()).isEqualTo(vehicleInstance.getId());
        assertThat(secondUser.getVehicleInstances().get(0).getType()).isEqualTo(VehicleInstanceType.PUBLIC);
        assertThat(secondUser.getVehicleInstances().get(0).getColor().getId()).isEqualTo(newColor.getId());

        user = userRepository.findByIdWithVehicles(user.getId()).orElseThrow();
        assertThat(user.getVehicleInstances().get(0).getId()).isEqualTo(vehicleInstance.getId());
        assertThat(user.getVehicleInstances().get(0).getType()).isEqualTo(VehicleInstanceType.PUBLIC);
        assertThat(user.getVehicleInstances().get(0).getColor().getId()).isEqualTo(newColor.getId());

        transactionTemplate.execute(ts -> {
            var vehicleInstanceTs = vehicleInstanceRepository.findById(vehicleInstance.getId()).orElseThrow();
            assertThat(vehicleInstanceTs.getUsers()).hasSize(2);
            return null;
        });
    }

    @Test
    void testUpdateVehicleInstance() {
        var vehicleInstance = assignVehicleToUser(VehicleInstanceType.PUBLIC, null);
        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(user.getVehicleInstances()).isNotEmpty();
            assertThat(user.getVehicleInstances().get(0).getRegistrationNumber()).isEqualTo(vehicleInstance.getRegistrationNumber());
            assertThat(vehicleInstance.getRegistrationNumberCountry()).isEqualTo(VehicleRegistrationNumberCountry.RUS);
            return null;
        });

        String registrationNumber = "A111AA";
        String registrationNumberRegion = "789";
        var command = UserCommand.AddOrUpdateVehicleInstance.builder()
                .userId(user.getId())
                .vehicleInstanceDto(UserCommand.AddOrUpdateVehicleInstance.UpdateVehicleInstanceDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .type(VehicleInstanceType.PUBLIC)
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationNumberRegion)
                        .registrationNumberCountry(VehicleRegistrationNumberCountry.RUS)
                        .build())
                .build();
        commandService.addOrUpdateVehicleInstance(command);

        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(user.getVehicleInstances()).isNotEmpty();
            var updatedVehicleInstance = user.getVehicleInstances().get(0);
            assertThat(updatedVehicleInstance.getId()).isNotNull();
            assertThat(updatedVehicleInstance.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(updatedVehicleInstance.getRegistrationNumberRegion()).isEqualTo(registrationNumberRegion);
            assertThat(vehicleInstance.getRegistrationNumberCountry()).isEqualTo(VehicleRegistrationNumberCountry.RUS);
            assertThat(updatedVehicleInstance.getInfoUpdatedAt()).isNotNull();
            return null;
        });
    }

    @Test
    void testAddVehicleInstanceWithoutMandatoryParams() {

        String registrationNumber = "A111AA";
        String registrationNumberRegion = "789";

        // add without vehicle
        var commandWithoutVehicle = UserCommand.AddOrUpdateVehicleInstance.builder()
                .userId(user.getId())
                .vehicleInstanceDto(UserCommand.AddOrUpdateVehicleInstance.UpdateVehicleInstanceDto.builder()
                        .type(VehicleInstanceType.PUBLIC)
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        var exception = assertThrows(CommandFailedException.class, () ->
                commandService.addOrUpdateVehicleInstance(commandWithoutVehicle));

        assertThat(exception.getMessage()).contains("Vehicle and registration number must be present to create a new " +
                "instance of vehicle");

        // add without registerNumber
        var commandWithoutNumber = UserCommand.AddOrUpdateVehicleInstance.builder()
                .userId(user.getId())
                .vehicleInstanceDto(UserCommand.AddOrUpdateVehicleInstance.UpdateVehicleInstanceDto.builder()
                        .vehicle(vehicleGenerateService.generateVehicle())
                        .type(VehicleInstanceType.PUBLIC)
                        .registrationNumberRegion(registrationNumberRegion)
                        .build())
                .build();
        exception = assertThrows(CommandFailedException.class, () ->
                commandService.addOrUpdateVehicleInstance(commandWithoutNumber));

        assertThat(exception.getMessage()).contains("Vehicle and registration number must be present to create a new " +
                "instance of vehicle");
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
