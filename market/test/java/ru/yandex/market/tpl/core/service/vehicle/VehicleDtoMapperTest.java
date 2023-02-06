package ru.yandex.market.tpl.core.service.vehicle;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.api.model.vehicle.VehicleDataDto;
import ru.yandex.market.tpl.api.model.vehicle.VehicleInstanceTypeDto;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


@RequiredArgsConstructor
public class VehicleDtoMapperTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final VehicleGenerateService vehicleGenerateService;
    private final VehicleDtoMapper mapper;

    private User user;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(3523601L);
    }

    @DisplayName("Успешный маппинг VehicleDataDto")
    @Test
    void shouldMapVehicleDataDtoWithoutException() {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .users(List.of(user))
                        .type(VehicleInstanceType.PERSONAL)
                        .vehicle(vehicle)
                        .build());
        VehicleDataDto vehicleDataDto = new VehicleDataDto(new VehicleDataDto.VehicleInstanceDataDto(
                vehicleInstance.getId(),
                "A000AA",
                "001"
        ));
        var res = assertDoesNotThrow(() -> mapper.map(vehicleDataDto, null));
        assertThat(res.getVehicleDataDto().getType()).isEqualTo(vehicleInstance.getType());
    }

    @DisplayName("Успешный маппинг PartnerUserVehicleDto")
    @ParameterizedTest
    @EnumSource(PartnerUserVehicleRegistrationNumberCountry.class)
    void shouldMapPartnerUserVehicleDtoWithoutException(PartnerUserVehicleRegistrationNumberCountry country) {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .users(List.of(user))
                        .vehicle(vehicle)
                        .build());
        PartnerUserVehicleDto vehicleDataDto = PartnerUserVehicleDto.builder()
                .vehicleInstanceId(vehicleInstance.getId())
                .registrationNumber("A001AA")
                .registrationNumberCountry(country)
                .build();
        assertDoesNotThrow(() -> mapper.map(vehicleDataDto, null));
    }

    @Test
    void shouldThrowMapPartnerUserVehicleDto() {
        PartnerUserVehicleDto vehicleDataDto = PartnerUserVehicleDto.builder()
                .registrationNumber("A001AA")
                .type(VehicleInstanceTypeDto.PUBLIC)
                .registrationNumberRegion("000")
                .build();

        var exception = assertThrows(
                TplIllegalArgumentException.class,
                () -> mapper.map(vehicleDataDto, null)
        );

        assertThat(exception.getMessage()).contains("Необходимо выбрать марку и модель ТС");
    }


    @ParameterizedTest
    @EnumSource(VehicleInstanceType.class)
    void testMapVehicleInstances(VehicleInstanceType vehicleInstanceType) {
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .type(vehicleInstanceType)
                        .infoUpdatedAt(Instant.now())
                        .users(List.of(user))
                        .vehicle(vehicle)
                        .build());

        assertDoesNotThrow(() -> mapper.mapVehicleInstanceType(vehicleInstance));
    }

    @ParameterizedTest
    @EnumSource(VehicleRegistrationNumberCountry.class)
    void testMapVehicleRegistrationNumberCountry(VehicleRegistrationNumberCountry country) {
        var result = mapper.mapRegistrationNumberCountry(country);
        assertThat(country).isEqualTo(mapper.mapRegistrationNumberCountry(result));
    }

    @ParameterizedTest
    @DisplayName("Маппим русские буквы в английские")
    @MethodSource("mappingRussianCharsToEnglishTestData")
    void testMapRussianCharsToEnglish(String actual, String expected) {
        assertThat(mapper.mapRussianCharsToEnglish(actual)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Маппим английские буквы в русские")
    @MethodSource("mappingEnglishCharsToRussianTestData")
    void testMapEnglishCharsToRussian(String actual, String expected) {
        assertThat(mapper.mapEnglishCharsToRussian(actual)).isEqualTo(expected);
    }

    private static List<Arguments> mappingRussianCharsToEnglishTestData() {
        return List.of(
                Arguments.of("т 565 рH", "T565PH"),
                Arguments.of("Ш 565 Ус.", "Ш565YC."),
                Arguments.of("АВЕКМНОРСТУХ", "ABEKMHOPCTYX")
        );
    }

    private static List<Arguments> mappingEnglishCharsToRussianTestData() {
        return List.of(
                Arguments.of("t 565 pH", "Т565РН"),
                Arguments.of("q 565 Yc.", "Q565УС."),
                Arguments.of("ABEKMHOPCTYX", "АВЕКМНОРСТУХ")
        );
    }

}
