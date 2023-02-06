package ru.yandex.direct.web.entity.adgroup.service.cpm;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.PixelKind;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidPixelFormat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotLessThan;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.testing.data.TestAdGroups.cpmAdGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class WebCpmAdGroupValidationServiceTest {

    @Autowired
    private WebCpmAdGroupValidationService validationService;
    @Autowired
    private Steps steps;
    @Autowired
    private ClientService clientService;

    private CampaignInfo campaignInfo;
    private long retCondId;

    private WebCpmAdGroupValidationService.ValidationData manualCampaignValidationData;
    private WebCpmAdGroupValidationService.ValidationData autobudgetCampaignValidationData;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        CampaignType campaignType = campaignInfo.getCampaign().getType();
        Currency clientCurrency = clientService.getWorkCurrency(campaignInfo.getClientId());

        manualCampaignValidationData =
                new WebCpmAdGroupValidationService.ValidationData(clientCurrency, false, campaignType, emptyMap());
        autobudgetCampaignValidationData =
                new WebCpmAdGroupValidationService.ValidationData(clientCurrency, true, campaignType, emptyMap());

        RetConditionInfo defaultRetCondition =
                steps.retConditionSteps().createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = defaultRetCondition.getRetConditionId();
    }

    @Test
    public void validate_AdGroupIsNull() {
        List<WebCpmAdGroup> adGroups = singletonList(null);
        ValidationResult<List<WebCpmAdGroup>, Defect> vr =
                validationService.validate(adGroups, manualCampaignValidationData);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), notNull())));
    }

    @Test
    public void validate_AdGroupInAutobudgetCampaign_Successful() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId());

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), autobudgetCampaignValidationData);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_AdGroupInManualStrategyCampaign_Successful() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId());

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_ManualStrategyAndNullAutoPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withAutoPrice(null);

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.AUTO_PRICE)), notNull())));
    }

    @Test
    public void validate_ManualStrategyAndTooLowPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withGeneralPrice(3.0);

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        Currency currency = clientService.getWorkCurrency(campaignInfo.getClientId());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.AUTO_PRICE), field("single_price")),
                        invalidValueCpmNotLessThan(Money.valueOf(currency.getMinCpmPrice(), currency.getCode())))));
    }

    //ad group with retargetings
    @Test
    public void validate_AdGroupInAutobudgetCampaignWithRetargetings_Successful() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, retCondId)));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), autobudgetCampaignValidationData);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_AdGroupInManualStrategyCampaignWithRetargetings_Successful() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, retCondId).withPriceContext(50.0)));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_AdGroupInManualStrategyCampaignWithRetargetingsWithNullPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, retCondId)));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.RETARGETINGS), index(0),
                        field(WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT)),
                        notNull())));
    }

    @Test
    public void validate_AdGroupInManualStrategyCampaignWithRetargetingsWithTooLowPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, retCondId).withPriceContext(3.0)));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        Currency currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.RETARGETINGS), index(0),
                        field(WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT)),
                        invalidValueCpmNotLessThan(Money.valueOf(currency.getMinCpmPrice(), currency.getCode())))));
    }

    //banner pixels
    @Test
    public void validate_NullPixels_Error() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withBanners(singletonList(new WebCpmBanner().withPixels(singletonList(null))));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.BANNERS), index(0),
                        field(WebCpmBanner.Prop.PIXELS), index(0)),
                        notNull())));
    }

    @Test
    public void validate_PixelFromUnknownProvider_Error() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withBanners(singletonList(new WebCpmBanner().withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl("some unknown url")))));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.BANNERS), index(0),
                        field(WebCpmBanner.Prop.PIXELS), index(0)),
                        invalidPixelFormat())));
    }

    @Test
    public void validate_YaAudiencePixelUrlWithKindAudit_Error() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withBanners(singletonList(new WebCpmBanner().withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl(yaAudiencePixelUrl())))));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.BANNERS), index(0),
                        field(WebCpmBanner.Prop.PIXELS), index(0)),
                        invalidPixelFormat())));
    }

    @Test
    public void validate_AuditPixelUrlWithKindAudience_Error() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withBanners(singletonList(new WebCpmBanner().withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIENCE)
                        .withUrl(adfoxPixelUrl())))));

        ValidationResult<List<WebCpmAdGroup>, Defect> vr = validationService
                .validate(singletonList(webCpmAdGroup), manualCampaignValidationData);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebCpmAdGroup.Prop.BANNERS), index(0),
                        field(WebCpmBanner.Prop.PIXELS), index(0)),
                        invalidPixelFormat())));
    }

}
