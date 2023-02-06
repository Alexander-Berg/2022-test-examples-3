package ru.yandex.market.logistics.lms.client.controller.yt;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение партнеров по id")
class LmsLomYtControllerGetPartnerTest extends LmsLomYtControllerAbstractTest {

    private static final String GET_PARTNER_BY_ID_PATH = "/lms/test-yt/partner/get/%d";
    private static final String GET_PARTNERS_BY_IDS_PATH = "/lms/test-yt/partner/get-by-ids";
    private static final String GET_PARTNER_BY_ID_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id = 1";
    private static final String GET_PARTNERS_BY_IDS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id IN";
    private static final Long PARTNER_ID = 1L;
    private static final Set<Long> PARTNER_IDS = Set.of(1L, 2L, 3L);

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private YtTables ytTables;

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение партнёра")
    @DatabaseSetup("/lms/client/yt/get_partner_api_settings_from_yt_enabled.xml")
    void successGetPartnerById() {
        mockYtGetPartnerQueryResponse(partnerLightModel(1L));

        getPartner(PARTNER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partner.json"));

        verifyYtCalling(GET_PARTNER_BY_ID_QUERY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Партнер не найден")
    void partnerNotFound() {
        mockYtGetPartnerQueryResponse(null);

        getPartner(PARTNER_ID)
            .andExpect(status().isOk())
            .andExpect(content().string("null"));

        verifyYtCalling(GET_PARTNER_BY_ID_QUERY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение партнеров")
    void successGetPartnersByIds() {
        mockYtGetPartnersQueryResponse();

        getPartners(PARTNER_IDS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partners.json"));

        verifyYtCalling(GET_PARTNERS_BY_IDS_QUERY);
    }


    private void verifyYtCalling(String queryToVerify) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            queryToVerify
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getPartner(Long partnerId) {
        return mockMvc.perform(
            get(String.format(GET_PARTNER_BY_ID_PATH, partnerId))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getPartners(Set<Long> partnerIds) {
        return mockMvc.perform(
            put(GET_PARTNERS_BY_IDS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partnerIds))
        );
    }

    private void mockYtGetPartnerQueryResponse(@Nullable PartnerLightModel response) {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            Optional.ofNullable(response),
            GET_PARTNER_BY_ID_QUERY
        );
    }

    private void mockYtGetPartnersQueryResponse() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(partnerLightModel(1L), partnerLightModel(2L), partnerLightModel(3L)),
            GET_PARTNERS_BY_IDS_QUERY
        );
    }

    @Nonnull
    private PartnerLightModel partnerLightModel(Long partnerId) {
        return PartnerLightModel.build(
            PartnerResponse.newBuilder()
                .id(partnerId)
                .partnerType(PartnerType.DELIVERY)
                .readableName("Some partner")
                .params(List.of(new PartnerExternalParam("paramKey", "paramDescription", "paramValue")))
                .marketId(10 + partnerId)
                .billingClientId(100 + partnerId)
                .name("PARTNER_FROM_YT")
                .status(PartnerStatus.ACTIVE)
                .domain("domain.com")
                .subtype(PartnerSubtypeResponse.newBuilder().id(1L).name("Lavka").build())
                .build()
        );
    }
}
