package ru.yandex.direct.core.entity.banner.type.href;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.not;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.emptyHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyHrefOrTurbolandingId;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithHrefAddCpmPriceNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void validate_nullHrefInCpmPrice_DefectOnlyOnHrefProperty() {
        validate_hrefInCpmPriceCampaign(null, notNull());
    }

    @Test
    public void validate_emptyHrefInCpmPrice_DefectOnlyOnHrefProperty() {
        // В старых баннерах был дефект notEmptyString, а теперь сделали emptyHref. Это ок, прочекано с фронтом,
        // смотри коменты в тикете.
        validate_hrefInCpmPriceCampaign("", emptyHref());
    }

    private void validate_hrefInCpmPriceCampaign(String href, Defect expectedDefect) {
        var userInfo = steps.userSteps().createDefaultUser();
        var clientInfo = userInfo.getClientInfo();
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        adGroupInfo = new AdGroupInfo().withAdGroup(adGroup)
                .withCampaignInfo(new CampaignInfo().withClientInfo(clientInfo));
        CreativeInfo creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);

        var banner = clientCpmBanner(creative.getCreativeId())
                .withCampaignId(campaign.getId())
                .withAdGroupId(adGroup.getId())
                .withTurboLandingId(null)
                .withHref(href);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithHref.HREF)),
                expectedDefect)));
        AssertionsForClassTypes.assertThat(vr).is(not(matchedBy(hasDefectDefinitionWith(validationError(path(),
                requiredButEmptyHrefOrTurbolandingId())))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
