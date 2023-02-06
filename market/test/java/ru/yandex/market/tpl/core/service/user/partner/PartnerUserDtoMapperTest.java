package ru.yandex.market.tpl.core.service.user.partner;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.user.UserRegistrationStatus;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierRegistrationStatusDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierStatus;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerPersonalDataDto;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.vehicle.Vehicle;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstance;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class PartnerUserDtoMapperTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final PartnerUserDtoMapper userDtoMapper;
    private final VehicleGenerateService vehicleGenerateService;
    private final TestUserHelper testUserHelper;
    private final TransactionTemplate transactionTemplate;

    private User user;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(3523601L);
    }

    @Test
    void shouldReturnEmptyVehicles() {
        var result = userDtoMapper.toPartnerUser(user);
        assertThat(result.getVehicles()).isEmpty();
    }

    @Test
    void shouldReturnOneVehicle() {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleInstance = assignVehicleToUser(vehicle, VehicleInstanceType.PUBLIC, null, "White");
        var result = userDtoMapper.toPartnerUser(user);
        assertThat(result.getVehicles()).hasSize(1);
        var vehicleDto = result.getVehicles().get(0);
        assertThat(vehicleDto.getVehicleInstanceId()).isEqualTo(vehicleInstance.getId());
        assertThat(vehicleDto.getColor()).isNotNull();
        assertThat(vehicleDto.getColor().getName()).isEqualTo("White");
        assertThat(vehicleDto.getVehicleData()).isNotNull();
        assertThat(vehicleDto.getVehicleData().getBrand()).isEqualTo(vehicle.getVehicleBrand().getName());
    }

    @Test
    void fromLogbrokerCourierDtoToPartnerUserDto() {
        transactionTemplate.execute(status -> {
            User user = testUserHelper.findOrCreateUser(657839433);
            LogbrokerCourierDto logbrokerCourierDto = new LogbrokerCourierDto();
            logbrokerCourierDto.setId("45832456");
            logbrokerCourierDto.setStatus(LogbrokerCourierStatus.FIRED);
            logbrokerCourierDto.setUid("45678765");
            logbrokerCourierDto.setDeleted(true);
            LogbrokerPersonalDataDto logbrokerPersonalDataDto = new LogbrokerPersonalDataDto();
            logbrokerPersonalDataDto.setEmail("test857499@mail.ru");
            logbrokerPersonalDataDto.setName("testName764585890");
            logbrokerPersonalDataDto.setVaccinated(true);
            logbrokerPersonalDataDto.setFirstName("Иван");
            logbrokerPersonalDataDto.setLastName("Иванов");
            logbrokerCourierDto.setPersonalData(logbrokerPersonalDataDto);
            logbrokerCourierDto.setCourierRegistrationStatus(LogbrokerCourierRegistrationStatusDto.REGISTERED);
            logbrokerCourierDto.setCourierType(LogbrokerCourierTypeDto.SELF_EMPLOYED);
            logbrokerCourierDto.setYaProId("74835389830");
            PartnerUserDto partnerUserDto = userDtoMapper.toPartnerUser(logbrokerCourierDto, user, user.getCompany());
            assertThat(partnerUserDto.getDsmExternalId()).isEqualTo(logbrokerCourierDto.getId());
            assertThat(partnerUserDto.getDsmVersion()).isEqualTo(logbrokerCourierDto.getVersion());
            assertThat(partnerUserDto.getStatus()).isEqualTo(userDtoMapper.toStatus(logbrokerCourierDto.getStatus()));
            assertThat(partnerUserDto.getUid()).isEqualTo(Long.parseLong(logbrokerCourierDto.getUid()));
            assertThat(partnerUserDto.isDeleted()).isEqualTo(logbrokerCourierDto.getDeleted());
            assertThat(partnerUserDto.getEmail()).isEqualTo(logbrokerCourierDto.getPersonalData().getEmail());
            assertThat(partnerUserDto.getName()).isEqualTo(logbrokerCourierDto.getPersonalData().getName());
            assertThat(partnerUserDto.getFirstName()).isEqualTo(logbrokerCourierDto.getPersonalData().getFirstName());
            assertThat(partnerUserDto.getLastName()).isEqualTo(logbrokerCourierDto.getPersonalData().getLastName());
            assertThat(partnerUserDto.getHasVaccination())
                    .isEqualTo(logbrokerCourierDto.getPersonalData().getVaccinated());
            assertThat(partnerUserDto.getRegistrationStatus()).isEqualTo(UserRegistrationStatus.SELF_EMPLOYED);
            assertThat(partnerUserDto.getYaProId()).isEqualTo(logbrokerCourierDto.getYaProId());
            return status;
        });
    }


    @Test
    void shouldReturnTwoVehicles() {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicle2 = vehicleGenerateService.generateVehicle("Audi", "A2");
        assignVehicleToUser(vehicle, VehicleInstanceType.PUBLIC, null, "White");
        assignVehicleToUser(vehicle2, VehicleInstanceType.PUBLIC, null, "Black");
        var result = userDtoMapper.toPartnerUser(user);
        assertThat(result.getVehicles()).hasSize(2);
    }

    private VehicleInstance assignVehicleToUser(Vehicle vehicle, VehicleInstanceType vehicleInstanceType,
                                                Instant infoUpdatedAt, String color) {
        return vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .type(vehicleInstanceType)
                .infoUpdatedAt(infoUpdatedAt)
                .users(List.of(user))
                .vehicle(vehicle)
                .color(vehicleGenerateService.generateVehicleColor(color))
                .build());
    }
}
