package ru.yandex.market.tpl.core.service.vehicle;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDataDto;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class VehicleQueryServiceTest extends TplAbstractTest {
    private final VehicleGenerateService vehicleGenerateService;
    private final VehicleQueryService vehicleQueryService;


    @BeforeEach
    void init() {
        vehicleGenerateService.generateVehicle("Lada", "Granta");
        vehicleGenerateService.generateVehicle("Lada", "X-Ray");
        vehicleGenerateService.generateVehicle("Audi", "A6");
        vehicleGenerateService.generateVehicleColor("Красный");
    }

    @DisplayName("Успешный поиск Vehicle по названию бренда")
    @Test
    void shouldFindVehiclesByBrandName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Lada", pageable))
                .hasSize(2)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("Granta", "X-Ray");
    }

    @DisplayName("Успешный поиск Vehicle по частичному названию бренда")
    @Test
    void shouldFindVehiclesByPartOfBrandName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Aud", pageable))
                .hasSize(1)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("A6");
    }

    @DisplayName("Успешный поиск Vehicle по модели")
    @Test
    void shouldFindVehiclesByModelName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("X-Ray", pageable))
                .hasSize(1)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("X-Ray");
    }

    @DisplayName("Успешный поиск Vehicle по частичному названию модели")
    @Test
    void shouldFindVehiclesByPartOfModelName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Ray", pageable))
                .hasSize(1)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("X-Ray");
    }

    @DisplayName("Успешный поиск Vehicle по совместному названию бренда и модели")
    @Test
    void shouldFindVehiclesByBrandAndModelName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Lada X-Ray", pageable))
                .hasSize(1)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("X-Ray");
    }

    @DisplayName("Успешный поиск Vehicle по частичному названию бренда и модели")
    @Test
    void shouldFindVehiclesByPartOfBrandAndModelName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("ada X-Ra", pageable))
                .hasSize(1)
                .extracting(PartnerUserVehicleDataDto::getName)
                .containsExactlyInAnyOrder("X-Ray");
    }

    @DisplayName("Неуспешный поиск Vehicle")
    @Test
    void shouldNotFindVehicles() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Lada A6", pageable))
                .hasSize(0);
    }

    @DisplayName("Неуспешный поиск Vehicle")
    @Test
    void shouldNotFindVehicleUnknownBrandName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicles("Tesla", pageable))
                .hasSize(0);
    }

    @DisplayName("Успешный поиск VehicleColor")
    @Test
    void shouldFindVehicleColor() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicleColors("Красный", pageable))
                .hasSize(1);
    }

    @DisplayName("Успешный поиск VehicleColor по частичному совпадению")
    @Test
    void shouldFindVehicleColorByPartOfName() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicleColors("Крас", pageable))
                .hasSize(1);
    }

    @DisplayName("Неуспешный поиск VehicleColor")
    @Test
    void shouldNotFindVehicleColor() {
        PageRequest pageable = PageRequest.of(0, 2);
        assertThat(vehicleQueryService.findVehicleColors("Черный", pageable))
                .hasSize(0);
    }

}
