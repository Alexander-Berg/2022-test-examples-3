package ru.yandex.direct.internaltools.tools.pricesales;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.pricesales.model.PriceSalesCampaignsListAndApproveParameter;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class PriceSalesCampaignsListAndApproveToolApproveTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private PriceSalesCampaignsListAndApproveTool tool;

    private ClientInfo client;
    private UserInfo operator;
    private PricePackage pricePackage;

    @Spy
    private PricePackage pricePackageSpy;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();

        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).getChiefUserInfo();
        steps.featureSteps().addClientFeature(operator.getClientId(), CPM_PRICE_CAMPAIGN, true);
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(client).getPricePackage();

        pricePackageSpy = spy(pricePackage);
        doReturn(false).when(pricePackageSpy).isFrontpagePackage();
    }

    @Test
    public void success() {
        CpmPriceCampaign campaign = createCampaign(defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withBrandSurveyId("EZ36ppWatcxFSeFiSZa1ZJ")
        );
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);

        setCampaignStatusApprove(campaign.getId(), PriceFlightStatusApprove.YES);

        CpmPriceCampaign campaignFromDb = (CpmPriceCampaign) campaignTypedRepository
                .getTypedCampaigns(client.getShard(), List.of(campaign.getId())).get(0);
        assertThat(campaignFromDb.getFlightStatusApprove()).isEqualTo(PriceFlightStatusApprove.YES);
    }

    @Test
    public void autoProlongationNotChanged() {

        PricePackage pr = approvedPricePackage()
                .withIsFrontpage(false)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withCurrency(client.getClient().getWorkCurrency());

        steps.pricePackageSteps().createPricePackage(pr);

        var cpmCampaign = defaultCpmPriceCampaignWithSystemFields(client, pr)
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withBrandSurveyId("EZ36ppWatcxFSeFiSZa1ZJ");
        cpmCampaign.getStrategy().getStrategyData().setAutoProlongation(1L);
        CpmPriceCampaign campaign = createCampaign(cpmCampaign);

        steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, client);

        setCampaignStatusApprove(campaign.getId(), PriceFlightStatusApprove.YES);

        CpmPriceCampaign campaignFromDb = (CpmPriceCampaign) campaignTypedRepository
                .getTypedCampaigns(client.getShard(), List.of(campaign.getId())).get(0);
        assertThat(campaignFromDb.getStrategy().getStrategyData().getAutoProlongation()).isEqualTo(1L);
    }

    @Test(expected = InternalToolValidationException.class)
    public void nonValidCampaign() {
        CpmPriceCampaign campaign = createCampaign(defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                .withStartDate(LocalDate.of(1970, 1, 1))
                .withEndDate(LocalDate.of(2000, 1, 1))
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW));
        setCampaignStatusApprove(campaign.getId(), PriceFlightStatusApprove.YES);
    }

    @Test(expected = InternalToolValidationException.class)
    public void nonExistingCampaign() {
        Long nonExistingCampaignId = Long.MAX_VALUE;
        setCampaignStatusApprove(nonExistingCampaignId, PriceFlightStatusApprove.YES);
    }

    @Test(expected = InternalToolValidationException.class)
    public void nonCpmPriceCampaign() {
        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign();
        setCampaignStatusApprove(campaign.getCampaignId(), PriceFlightStatusApprove.YES);
    }

    private CpmPriceCampaign createCampaign(CpmPriceCampaign campaign) {
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, campaign);
        return campaign;
    }

    private void setCampaignStatusApprove(Long campaignId, PriceFlightStatusApprove statusApprove) {
        PriceSalesCampaignsListAndApproveParameter parameter = new PriceSalesCampaignsListAndApproveParameter()
                .withCampaignId(campaignId)
                .withStatusApprove(statusApprove);
        parameter.setOperator(operator.getUser());
        tool.process(parameter);
    }
}
