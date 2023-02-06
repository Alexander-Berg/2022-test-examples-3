package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithGeo;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithGeoAddOperationSupportTranslocalityTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public CampaignWithGeoAddOperationSupport campaignWithGeoAddOperationSupport;

    @Autowired
    Steps steps;

    // Американские Виргинские острова, отсутствуют в нашем гео-дереве
    private static final long UNKNOWN_REGION_ID = 21553L;

    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {UNKNOWN_REGION_ID, (int) Region.RUSSIA_REGION_ID, (int) Region.CRIMEA_REGION_ID},
                {Region.KAZAKHSTAN_REGION_ID, (int) Region.KAZAKHSTAN_REGION_ID},
                {Region.RUSSIA_REGION_ID, (int) Region.RUSSIA_REGION_ID, (int) Region.CRIMEA_REGION_ID},
                {Region.UKRAINE_REGION_ID, (int) Region.UKRAINE_REGION_ID, (int) Region.CRIMEA_REGION_ID},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("Client region = {0}")
    public void textCampaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(Long region,
                                                                                Integer... expectedGeo) {
        campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(new TextCampaign(), region, expectedGeo);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("Client region = {0}")
    public void dynamicCampaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(Long region,
                                                                                   Integer... expectedGeo) {
        campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(new DynamicCampaign(), region, expectedGeo);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("Client region = {0}")
    public void smartCampaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(Long region,
                                                                                 Integer... expectedGeo) {
        campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(new SmartCampaign(), region, expectedGeo);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("Client region = {0}")
    public void mcBannerCampaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(Long region,
                                                                                    Integer... expectedGeo) {
        campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(new McBannerCampaign(), region, expectedGeo);
    }

    private void campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo(CampaignWithGeo campaign,
                                                                             Long region,
                                                                             Integer... expectedGeo) {
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withCountryRegionId(region));

        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                RestrictedCampaignsAddOperationContainer.create(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid());
        campaignWithGeoAddOperationSupport.onPreValidated(addCampaignParametersContainer, List.of(campaign));

        assertThat(campaign.getGeo())
                .containsExactlyInAnyOrder(expectedGeo);
    }

}
