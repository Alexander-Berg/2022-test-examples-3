package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationWeatherTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather.WEATHER_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationWeatherTypeSupportTest {
    private static final Path errorPath = path(field(WEATHER_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private CpmVideoAdGroup adGroupCpmVideo;
    private BidModifierWeather modifier;
    private BidModifierValidationWeatherTypeSupport service;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        adGroupCpmVideo = new CpmVideoAdGroup().withType(AdGroupType.CPM_VIDEO);
        modifier = new BidModifierWeather().withWeatherAdjustments(singletonList(
                new BidModifierWeatherAdjustment().withPercent(120).withExpression(
                        singletonList(singletonList(
                                new BidModifierWeatherLiteral()
                                        .withParameter(WeatherType.CLOUDNESS)
                                        .withOperation(OperationType.EQ)
                                        .withValue(50)
                        ))
                )));
        service = new BidModifierValidationWeatherTypeSupport();
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithKeywords_passes() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithoutKeywords_passes() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.USER_PROFILE), clientId,
                null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupVideo_passes() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmVideo, clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_performanceCampaign_errorIsGenerated() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.PERFORMANCE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaignCpmAdGroup_passes() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_DEALS, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_errorIsGenerated() {
        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }
}
