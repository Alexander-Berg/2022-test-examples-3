package ru.yandex.market.logistics.lms.client.yt;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Получение партнеров по идентификатору")
class LmsLomYtGetPartnerTest extends LmsLomYtAbstractTest {

    private static final String GET_PARTNER_BY_ID_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id = 1";
    private static final String GET_PARTNERS_BY_IDS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id IN";
    private static final Long PARTNER_ID = 1L;
    private static final Set<Long> PARTNER_IDS = Set.of(1L, 2L, 3L);

    @Test
    @DisplayName("Успешное получение партнера из yt")
    @DatabaseSetup("/lms/client/yt/get_partner_by_id_from_yt_enabled.xml")
    void successGetPartnerById() {
        mockYtGetPartnerQueryResponse();

        softly.assertThat(lmsLomYtClient.getPartner(PARTNER_ID))
            .isEqualTo(Optional.of(partnerLightModel(PARTNER_ID)));

        verifyYtCalling(GET_PARTNER_BY_ID_QUERY);
    }

    @Test
    @DisplayName("Флаг получения партнера выключен, клиент не идет в yt")
    void goingToYtForPartnerDisabled() {
        mockYtGetPartnerQueryResponse();

        softly.assertThat(lmsLomYtClient.getPartner(PARTNER_ID))
            .isEmpty();
    }

    @Test
    @DisplayName("Успешное получение партнеров из yt")
    @DatabaseSetup("/lms/client/yt/get_partners_by_ids_from_yt_enabled.xml")
    void successGetPartnersByIds() {
        mockYtGetPartnersQueryResponse();

        softly.assertThat(lmsLomYtClient.getPartners(PARTNER_IDS))
            .containsExactlyInAnyOrderElementsOf(partnerLightModelsResponse());

        verifyYtCalling(GET_PARTNERS_BY_IDS_QUERY);
    }

    @Test
    @DisplayName("Флаг получения партнеров выключен, клиент не идет в yt")
    void goingToYtForPartnersDisabled() {
        mockYtGetPartnersQueryResponse();

        softly.assertThat(lmsLomYtClient.getPartners(PARTNER_IDS))
            .isEmpty();
    }

    private void verifyYtCalling(String queryToVerify) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            queryToVerify
        );
        verify(hahnYt, times(2)).tables();
    }

    private void mockYtGetPartnerQueryResponse() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            Optional.of(partnerLightModel(1L)),
            GET_PARTNER_BY_ID_QUERY
        );
    }

    private void mockYtGetPartnersQueryResponse() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            partnerLightModelsResponse(),
            GET_PARTNERS_BY_IDS_QUERY
        );
    }

    private List<PartnerLightModel> partnerLightModelsResponse() {
        return List.of(partnerLightModel(1L), partnerLightModel(2L), partnerLightModel(3L));
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
