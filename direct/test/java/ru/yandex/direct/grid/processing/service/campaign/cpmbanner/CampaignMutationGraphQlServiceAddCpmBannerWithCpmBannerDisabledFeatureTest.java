package ru.yandex.direct.grid.processing.service.campaign.cpmbanner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCpmYndxFrontpageCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultCpmPriceCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultCpmYndxFrontpageCampaign;
import static ru.yandex.direct.grid.processing.service.campaign.cpmbanner.DefaultGdAddCpmBannerCampaign.defaultGdAddCpmBannerCampaign;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceAddCpmBannerWithCpmBannerDisabledFeatureTest {
    private static final String MUTATION_NAME = "addCampaigns";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    private static CampaignAttributionModel defaultAttributionModel;
    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.DISABLE_BILLING_AGGREGATES, true);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @Test
    public void addCpmBannerCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        GdAddCmpBannerCampaign gdAddCpmBannerCampaign =
                defaultGdAddCpmBannerCampaign(campaignConstantsService.getDefaultAttributionModel());

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withCpmBannerCampaign(gdAddCpmBannerCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input,
                operator);

        GdDefect defect = new GdDefect()
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED")
                .withPath("campaignAddItems[0]");
        assertThat(gdAddCampaignPayload.getValidationResult()).isNotNull();
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }


    @Test
    public void addCpmPriceCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 1, 1, 1);
        var endDate = LocalDate.of(currentYear + 1, 12, 1);

        var pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withPrice(BigDecimal.valueOf(13L))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom())
                .withDateStart(startDate)
                .withDateEnd(endDate)
                .withClients(List.of(allowedPricePackageClient(clientInfo))));
        var cpmPriceCampaign =
                defaultCpmPriceCampaign(startDate, endDate, pricePackage.getPricePackageId(), defaultAttributionModel)
                        .withIsAllowedOnAdultContent(true);
        var gdAddCampaignUnion = new GdAddCampaignUnion().withCpmPriceCampaign(cpmPriceCampaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input,
                operator);

        GdDefect defect = new GdDefect()
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED")
                .withPath("campaignAddItems[0]");
        assertThat(gdAddCampaignPayload.getValidationResult()).isNotNull();
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }


    @Test
    public void addCpmYndxFrontPageCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 1, 1, 1);
        var endDate = LocalDate.of(currentYear + 1, 12, 1);

        GdAddCpmYndxFrontpageCampaign cpmYndxFrontpageCampaign =
                defaultCpmYndxFrontpageCampaign(startDate, endDate, defaultAttributionModel);

        var gdAddCampaignUnion = new GdAddCampaignUnion().withCpmYndxFrontpageCampaign(cpmYndxFrontpageCampaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input,
                operator);

        GdDefect defect = new GdDefect()
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED")
                .withPath("campaignAddItems[0]");
        assertThat(gdAddCampaignPayload.getValidationResult()).isNotNull();
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }
}
