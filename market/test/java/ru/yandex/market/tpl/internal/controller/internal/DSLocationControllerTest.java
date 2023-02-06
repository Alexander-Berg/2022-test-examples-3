package ru.yandex.market.tpl.internal.controller.internal;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.core.domain.region.TplRegionService;
import ru.yandex.market.tpl.core.domain.usershift.location.DeliveryRegionInfoService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.precise.PreciseGeoPointService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.DSLocationService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(DSLocationController.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DSLocationControllerTest extends BaseShallowTest {

    @MockBean
    private DSLocationService dsLocationService;
    @MockBean
    private TplRegionService tplRegionService;
    @MockBean
    private PreciseGeoPointService preciseGeoPointService;
    private final MockMvc mockMvc;


    @Test
    void getDsByLocation() throws Exception {
        GeoPoint geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.733969"), new BigDecimal("37.587093"));
        given(dsLocationService.getDsByRegionId(120542))
                .willReturn(Optional.of(239L));
        given(tplRegionService.getRegionId(geoPoint))
                .willReturn(120542);
        mockMvc.perform(
                post("/internal/ds-by-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"address\" : \"Москва, Льва Толстого 16\",\n" +
                                "  \"geo\" : {\n" +
                                "      \"latitude\" : 55.733969,\n" +
                                "      \"longitude\" : 37.587093\n" +
                                "  }\n" +
                                "}\n")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{\n" +
                        "   \"deliveryServiceId\" : 239,\n" +
                        "   \"regionId\" : 120542\n" +
                        "}"
                ));
    }

    @Test
    void getDsByLocationNoGeo() throws Exception {
        GeoPoint geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.733969"), new BigDecimal("37.587093"));
        given(preciseGeoPointService.getByAddress("Москва, Льва Толстого 16"))
                .willReturn(Optional.of(geoPoint));
        given(dsLocationService.getDsByRegionId(120542))
                .willReturn(Optional.of(239L));
        given(tplRegionService.getRegionId(geoPoint))
                .willReturn(120542);
        mockMvc.perform(
                post("/internal/ds-by-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"address\" : \"Москва, Льва Толстого 16\"\n" +
                                "}")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{\n" +
                        "   \"deliveryServiceId\" : 239,\n" +
                        "   \"regionId\" : 120542\n" +
                        "}"
                ));
    }

    @Test
    void getDsByLocationFallbackToDefault() throws Exception {
        GeoPoint geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.733969"), new BigDecimal("37.587093"));
        given(dsLocationService.getDefaultDsId())
                .willReturn(Optional.of(239L));
        given(tplRegionService.getRegionId(geoPoint))
                .willReturn(120542);
        mockMvc.perform(
                post("/internal/ds-by-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"geo\" : {\n" +
                                "      \"latitude\" : 55.733969,\n" +
                                "      \"longitude\" : 37.587093\n" +
                                "  }\n" +
                                "}\n")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{\n" +
                        "   \"deliveryServiceId\" : 239,\n" +
                        "   \"regionId\" : 120542\n" +
                        "}"
                ));
    }

    @Test
    void getDsByLocationFails() throws Exception {
        GeoPoint geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.733969"), new BigDecimal("37.587093"));
        given(preciseGeoPointService.getByAddress("Москва, Льва Толстого 16"))
                .willReturn(Optional.of(geoPoint));
        given(dsLocationService.getDsByRegionId(120542))
                .willReturn(Optional.empty());
        given(dsLocationService.getDefaultDsId())
                .willReturn(Optional.empty());
        given(tplRegionService.getRegionId(geoPoint))
                .willReturn(120542);
        mockMvc.perform(
                post("/internal/ds-by-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"address\" : \"Москва, Льва Толстого 16\"\n" +
                                "}")
        )
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getDsByLocationBadRequest() throws Exception {
        mockMvc.perform(
                post("/internal/ds-by-location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
                .andExpect(status().is4xxClientError());
    }
}
