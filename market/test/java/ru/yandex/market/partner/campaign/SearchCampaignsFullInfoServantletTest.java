package ru.yandex.market.partner.campaign;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.partner.campaign.model.CampaignFullDTO;
import ru.yandex.market.partner.campaign.model.CampaignFullInfoDTO;
import ru.yandex.market.partner.campaign.model.CampaignInfoDTO;
import ru.yandex.market.partner.campaign.model.DatasourceInfoDTO;
import ru.yandex.market.partner.campaign.model.ParamValueDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link SearchCampaignsFullInfoServantlet}.
 */
@DbUnitDataSet(before = "searchCampaignFullInfoServantletTest.csv")
class SearchCampaignsFullInfoServantletTest extends FunctionalTest {
    private static final Set<ParamValueDTO> PARAM_VALUES = Set.of(
            new ParamValueDTO(ParamType.IS_IN_INDEX, "true"),
            new ParamValueDTO(ParamType.IS_CPC_ENABLED, "true")
    );

    private static Stream<Arguments> testShopCampaignsCanLimitBudgetArgs() {
        return Stream.of(Arguments.of(100L), Arguments.of(new Object[]{null}));
    }

    @Autowired
    BalanceService balanceService;

    @Test
    void getByPartAgency() {
        // given
        var datasourceId = 774L;
        var campaignId = 10774L;
        var clientId = 325076L;
        when(balanceService.getClientByUid(anyLong()))
                .thenReturn(new ClientInfo(clientId, ClientType.OOO, true, 100500L));
        var url = getUrl(1248L) + "&part=test";

        // when
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(null, clientId, datasourceId, 1015L, campaignId);
        var datasourceInfoDTO = new DatasourceInfoDTO("testShop", datasourceId, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, true, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    @Test
    void getByPartDatasource() {
        // given
        var datasourceId = 774L;
        var campaignId = 10774L;
        var clientId = 325076L;
        var url = getUrl(1248L) + "&part=test";

        // when
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(null, clientId, datasourceId, 1015L, campaignId);
        var datasourceInfoDTO = new DatasourceInfoDTO("testShop", datasourceId, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, true, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    /**
     * У магазина должен проставиться флажок can-limit-budget.
     */
    @ParameterizedTest
    @MethodSource("testShopCampaignsCanLimitBudgetArgs")
    void testShopCampaignsCanLimitBudget(Long businessId) {
        var url = getUrl(1248L) + (businessId == null ? "" : "&business_id=" + businessId);
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(CampaignType.SHOP, 325076L, 774L, 1015L, 10774L);
        var datasourceInfoDTO = new DatasourceInfoDTO("testShop", 774L, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, true, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    /**
     * Проверяем, что магазин контакта отфильтровывается по бизнесу.
     */
    @Test
    void testNegativeFilterByBusiness() {
        var url = getUrl(1248L) + "&business_id=999";
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    /**
     * У магазина не должен проставиться флажок can-limit-budget.
     */
    @Test
    void testShopCampaignsCantLimitBudget() {
        var response = FunctionalTestHelper.getXml(getUrl(2248L), CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(CampaignType.SHOP, 325077L, 775L, 1015L, 10775L);
        var datasourceInfoDTO = new DatasourceInfoDTO("testShop2", 775L, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, false, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    /**
     * Доставочные магазины не должны отображаться в ручке для белых магазинов.
     */
    @Test
    void testDeliveryShopCampaignsVisibility() {
        var response = FunctionalTestHelper.getXml(getUrl(3248L), CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    /**
     * Суперчек магазины не должны отображаться в ручке для белых магазинов.
     */
    @Test
    void testFmcgCampaignsVisibility() {
        var response = FunctionalTestHelper.getXml(getUrl(4248L), CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    /**
     * Кроссбордер магазины не должны отображаться в ручке для белых магазинов.
     */
    @Test
    void testCrossborderCampaignsVisibility() {
        var response = FunctionalTestHelper.getXml(getUrl(5248L), CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    /**
     * Кроссбордер магазины не должны отображаться в ручке для белых магазинов.
     */
    @Test
    void testShopOnlyCampaignsVisibility() {
        var url = getUrl(5248L) + "&shopOnly=true";
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    /**
     * У SMB не должен проставиться флажок can-limit-budget.
     */
    @Test
    void testSmbCampaignsCantLimitBudget() {
        var response = FunctionalTestHelper.getXml(getUrl(6248L), CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(CampaignType.SHOP, 325081L, 779L, 1015L, 10779L);
        var datasourceInfoDTO = new DatasourceInfoDTO("SMB", 779L, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, false, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    /**
     * Тестирует поиск с флагом transfer_allowed_only
     */
    @Test
    void testTransferAllowed() {
        var url = getUrl(7248) + "&transfer_allowed_only=true";
        var response = FunctionalTestHelper.getXml(url, CampaignFullInfoDTO.class).getBody();

        var campaignInfoDTO = new CampaignInfoDTO(CampaignType.SHOP, 325076L, 774L, 1015L, 10774L);
        var datasourceInfoDTO = new DatasourceInfoDTO("testShop", 774L, 12345L);
        var campaignFullDTOExpected = new CampaignFullDTO(campaignInfoDTO, datasourceInfoDTO, true, PARAM_VALUES);
        campaignFullDTOExpected.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        assertCampaignEquals(response, campaignFullDTOExpected);
    }

    /**
     * Ручка не должна возвращать DBS
     */
    @Test
    void testDropshipBySeller() {
        var response = FunctionalTestHelper.getXml(getUrl(8248), CampaignFullInfoDTO.class).getBody();
        assertCampaignEquals(response, null);
    }

    private String getUrl(long euid) {
        return baseUrl + "/searchCampaignsFullInfo?euid=" + euid;
    }

    private static void assertCampaignEquals(CampaignFullInfoDTO actual, CampaignFullDTO campaign) {
        if (actual == null) {
            assertThat(campaign).isNull();
        } else {
            assertThat(actual.getCampaign()).isEqualTo(campaign);
            if (campaign != null) {
                assertThat(actual.getCampaign().getPlacementTypes()).isEqualTo(campaign.getPlacementTypes());
            }
        }
    }
}
