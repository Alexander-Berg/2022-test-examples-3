package ru.yandex.market.pvz.internal.controller.pi.legal_partner;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.core.domain.polygon.PolygonCommandService;
import ru.yandex.market.pvz.core.domain.polygon.model.Polygon;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.CABINET_CREATION_ERROR_MESSAGE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.EMPTY_LOGIN_ERROR_MESSAGE;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.generateRandomINN;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PreLegalPartnerControllerTest extends BaseShallowTest {

    private static final long PARTNER_ID = 2000L;

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;

    private final PolygonCommandService polygonCommandService;

    private final MbiApiClient mbiApiClient;

    private final Environment environment;

    @BeforeEach
    void mockMbi() {
        var mbiResponse = new SimpleShopRegistrationResponse();
        mbiResponse.setCampaignId(PARTNER_ID);
        mbiResponse.setDatasourceId(DEFAULT_DATASOURCE_ID);
        mbiResponse.setClientId(RandomUtils.nextLong(0, Long.MAX_VALUE));
        mbiResponse.setOwnerId(RandomUtils.nextLong(0, Long.MAX_VALUE));
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(mbiResponse);
        polygonCommandService.truncateAndSave(createPolygons());
    }

    @Test
    void createLegalPartner() throws Exception {
        String taxpayerNumber = generateRandomINN();

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_create.json"),
                                taxpayerNumber)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pre_partner/response_create.json"),
                        PARTNER_ID,
                        taxpayerNumber)));
    }

    @Test
    void legalFormIsAbsentForLegalPerson() throws Exception {
        String taxpayerNumber = generateRandomINN();

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_legal_form_absent.json"),
                                taxpayerNumber)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTaxpayerNumber() throws Exception {
        String taxpayerNumber = "344309228394";

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_create.json"),
                                taxpayerNumber)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLegalPartnerWithMbiBadRequest() throws Exception {
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any()))
                .thenThrow(new HttpClientErrorException(BAD_REQUEST, EMPTY_LOGIN_ERROR_MESSAGE));
        String taxpayerNumber = generateRandomINN();

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_create.json"),
                                taxpayerNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(String.format(
                        getFileContent("pre_partner/response_create_mbi_bad_request.json"),
                        PARTNER_ID,
                        taxpayerNumber), false));
    }

    @Test
    void createLegalPartnerWithMbiServerError() throws Exception {
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any()))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR, CABINET_CREATION_ERROR_MESSAGE));
        String taxpayerNumber = generateRandomINN();

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_create.json"),
                                taxpayerNumber)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(String.format(
                        getFileContent("pre_partner/response_create_mbi_server_error.json"),
                        PARTNER_ID,
                        taxpayerNumber), false));
    }

    @Test
    void invalidOgrn() throws Exception {
        String taxpayerNumber = generateRandomINN();

        mockMvc.perform(
                post("/v1/pi/pre-partners/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pre_partner/request_invalid_ogrn.json"),
                                taxpayerNumber)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPreLegalPartner() throws Exception {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        long partnerId = preLegalPartner.getPartnerId();
        String taxpayerNumber = preLegalPartner.getTaxpayerNumber();

        mockMvc.perform(
                get("/v1/pi/pre-partners/" + partnerId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pre_partner/response_create.json"),
                        partnerId,
                        taxpayerNumber)));
    }

    @Test
    void preLegalPartnerNotFound() throws Exception {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        long partnerId = preLegalPartner.getPartnerId();

        mockMvc.perform(
                get("/v1/pi/pre-partners/" + (partnerId + 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCities() throws Exception {
        mockMvc.perform(
                get("/v1/pi/pre-partners/cities"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("pre_partner/cities.json")));
    }

    @Test
    void getPolygons() throws Exception {
        mockMvc.perform(
                get("/v1/pi/pre-partners/polygons?locationId=16"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("pre_partner/response_polygon.json")));
    }

    private List<Polygon> createPolygons() {
        return List.of(
                createPolygon("Москва и Московская область", "Акулово", 117114),
                createPolygon("Москва и Московская область", "Андреевка", 109761),
                createPolygon("Ярославская область", "Ярославль", 16)
        );
    }

    private Polygon createPolygon(String region, String city, int locationId) {
        return Polygon.builder()
                .region(region)
                .city(city)
                .locationId(locationId)
                .hexIndex("8910f1325afffff")
                .geometry(List.of(
                        List.of(57.1, 56.1),
                        List.of(57.1, 56.1),
                        List.of(57.1, 56.1),
                        List.of(57.1, 56.1))
                )
                .childLocationId(123)
                .build();
    }
}
