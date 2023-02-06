package ru.yandex.market.tpl.internal.controller.partner;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleColorDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDataDto;
import ru.yandex.market.tpl.core.service.vehicle.VehicleQueryService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@WebLayerTest(PartnerVehicleController.class)
@Slf4j
public class PartnerVehicleControllerTest extends BaseShallowTest {
    @MockBean
    private VehicleQueryService vehicleQueryService;


    @Test
    void shouldFindVehicles() throws Exception {
        when(vehicleQueryService.findVehicles(any(), any())).thenReturn(List.of(
                        createVehicleDto(1L, "BMW", "X3"),
                        createVehicleDto(2L, "Lada", "Granta")
                )
        );

        mockMvc.perform(get("/internal/partner/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .header(COMPANY_HEADER, 1)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_vehicles.json"), true));
    }

    @Test
    void shouldNotFindVehiclesWithQuery() throws Exception {
        when(vehicleQueryService.findVehicles(any(), any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/internal/partner/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .header(COMPANY_HEADER, 1)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect((content()).json("[]", true));
    }


    @Test
    void shouldFindVehicleColors() throws Exception {
        when(vehicleQueryService.findVehicleColors(any(), any())).thenReturn(List.of(
                createVehicleColorDto(1L, "Красный"),
                createVehicleColorDto(2L, "Черный")
                )
        );

        mockMvc.perform(get("/internal/partner/vehicles/colors")
                .contentType(MediaType.APPLICATION_JSON)
                .header(COMPANY_HEADER, 1)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_vehicle_colors.json"), true));
    }

    @Test
    void shouldNotFindVehicleColorsWithQuery() throws Exception {
        when(vehicleQueryService.findVehicleColors(any(), any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/internal/partner/vehicles/colors")
                .contentType(MediaType.APPLICATION_JSON)
                .header(COMPANY_HEADER, 1)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect((content()).json("[]", true));
    }


    private PartnerUserVehicleDataDto createVehicleDto(long id, String brandName, String name) {
        return PartnerUserVehicleDataDto.builder()
                .id(id)
                .brand(brandName)
                .name(name)
                .build();

    }

    private PartnerUserVehicleColorDto createVehicleColorDto(long id, String name) {
        return PartnerUserVehicleColorDto.builder()
                .id(id)
                .name(name)
                .build();

    }
}
