package ru.yandex.market.logistics.lms.client.controller.yt;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.lms.model.PartnerRelationLightModel;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DisplayName("Получение связок партнеров")
class LmsLomYtControllerGetPartnerRelationTest extends LmsLomYtControllerAbstractTest {

    private static final PartnerRelationFilter CUTOFFS_FILTER = PartnerRelationFilter.newBuilder()
        .fromPartnersIds(Set.of(1L))
        .toPartnersIds(Set.of(2L))
        .build();

    private static final PartnerRelationFilter RETURN_PARTNERS_FILTER = PartnerRelationFilter.newBuilder()
        .fromPartnersIds(Set.of(1L))
        .build();

    private static final PartnerRelationFilter INVALID_FILTER = PartnerRelationFilter.newBuilder().build();

    private static final String SEARCH_CUTOFFS_PATH =
        "/lms/test-yt/partner-relation/cutoffs/get-by-filter";

    private static final String SEARCH_RETURN_PARTNERS_PATH =
        "/lms/test-yt/partner-relation/return-partners/get-by-filter";

    private static final String CUTOFFS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_dyn] "
        + "WHERE partner_from = 1 AND partner_to = 2";

    private static final String RETURN_PARTNERS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_to_dyn] "
        + "WHERE partner_from IN (1)";

    @Test
    @DisplayName("Получение связок с катоффами - успех")
    void testGetPartnerRelationWithCutoffs() throws Exception {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            getPartnerRelationWithCutoffs(),
            CUTOFFS_QUERY
        );

        getCutoffs(CUTOFFS_FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partner_relation_with_cutoffs.json"));

        verifyYtCalling(CUTOFFS_QUERY);
    }

    @Test
    @DisplayName("Получение связок с катоффами - нет данных")
    void testGetPartnerRelationWithCutoffsNoData() throws Exception {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            CUTOFFS_QUERY
        );

        getCutoffs(CUTOFFS_FILTER)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyYtCalling(CUTOFFS_QUERY);
    }

    @Test
    @DisplayName("Получение связок с катоффами - некорректный фильтр")
    void testGetPartnerRelationWithCutoffsBadFilter() {
        softly.assertThatThrownBy(
                () -> getCutoffs(INVALID_FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; "
                + "nested exception is java.lang.IllegalArgumentException: "
                + "Filter must contain exactly one 'fromPartnerId' and 'toPartnerId'");
    }

    @Test
    @DisplayName("Получение связок с катоффами - ошибка в yt")
    void testGetPartnerRelationWithCutoffsYtError() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            CUTOFFS_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatThrownBy(
                () -> getCutoffs(CUTOFFS_FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(CUTOFFS_QUERY);
    }

    @Test
    @DisplayName("Получение связок с возвратными партнерами - успех")
    void testGetPartnerRelationWithReturnPartners() throws Exception {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            getPartnerRelationWithReturnPartners(),
            RETURN_PARTNERS_QUERY
        );

        getReturnPartners(RETURN_PARTNERS_FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partner_relation_with_return_partners.json"));

        verifyYtCalling(RETURN_PARTNERS_QUERY);
    }

    @Test
    @DisplayName("Получение связок с возвратными партнерами - нет данных")
    void testGetPartnerRelationWithReturnPartnersNoData() throws Exception {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            RETURN_PARTNERS_QUERY
        );

        getReturnPartners(RETURN_PARTNERS_FILTER)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyYtCalling(RETURN_PARTNERS_QUERY);
    }

    @Test
    @DisplayName("Получение связок с возвратными партнерами - некорректный фильтр")
    void testGetPartnerRelationWithReturnPartnersInvalidFilter() {
        softly.assertThatThrownBy(
                () -> getReturnPartners(INVALID_FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; "
                + "nested exception is java.lang.IllegalArgumentException: "
                + "Field 'fromPartnerIds' in filter must not be null");
    }

    @Test
    @DisplayName("Получение связок с возвратными партнерами - ошибка в yt")
    void testGetPartnerRelationWithReturnPartnersYtError() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            RETURN_PARTNERS_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatThrownBy(
                () -> getReturnPartners(RETURN_PARTNERS_FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(RETURN_PARTNERS_QUERY);
    }

    @Override
    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ytTables, hahnYt);
    }

    @NotNull
    private List<PartnerRelationLightModel> getPartnerRelationWithCutoffs() {
        return List.of(
            PartnerRelationLightModel.build(
                PartnerRelationLightModel.newBuilder()
                    .fromPartnerId(1L)
                    .toPartnerId(2L)
                    .cutoffs(Set.of(
                        CutoffResponse.newBuilder()
                            .locationId(12332190)
                            .cutoffTime(LocalTime.of(18, 1, 1))
                            .build())
                    )
                    .build()
            )
        );
    }

    @NotNull
    private List<PartnerRelationLightModel> getPartnerRelationWithReturnPartners() {
        return List.of(
            PartnerRelationLightModel.build(
                PartnerRelationLightModel.newBuilder()
                    .fromPartnerId(9884122L)
                    .toPartnerId(9884123L)
                    .returnPartnerId(9884124L)
                    .build()
            )
        );
    }

    @Nonnull
    private ResultActions getCutoffs(PartnerRelationFilter filter) throws Exception {
        return mockMvc.perform(
            post(SEARCH_CUTOFFS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    @Nonnull
    private ResultActions getReturnPartners(PartnerRelationFilter filter) throws Exception {
        return mockMvc.perform(
            post(SEARCH_RETURN_PARTNERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    private void verifyYtCalling(String queryToVerify) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            queryToVerify
        );
        verify(hahnYt, times(2)).tables();
    }
}
