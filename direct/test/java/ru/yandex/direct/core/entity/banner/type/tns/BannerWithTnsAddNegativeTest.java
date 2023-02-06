package ru.yandex.direct.core.entity.banner.type.tns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTns.TNS_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTnsAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String CORRECT_TNS_ID = "abc";
    private static final String BLANK_TNS_ID = " ";
    private static final String RESTRICTED_TNS_ID = "#$*-";

    @Before
    public void setUp() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
    }

    @Test
    public void blankTnsIdForCpmBanner_NotEmptyStringError() {
        CpmBanner banner = createCpmBanner(BLANK_TNS_ID);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TNS_ID)), notEmptyString())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void tnsIdWithIncorrectCharactersForCpmBanner_RestrictedCharsInFieldError() {
        CpmBanner banner = createCpmBanner(RESTRICTED_TNS_ID);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TNS_ID)), restrictedCharsInField())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void notNullTnsIdForPriceCpmBanner_IsNullError() {

        CpmBanner banner = createCpmBannerForPriceSales();

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TNS_ID)), isNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private CpmBanner createCpmBannerForPriceSales() {
        var clientInfo = adGroupInfo.getClientInfo();
        var priceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        var priceAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(priceCampaign, clientInfo);
        var creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, priceCampaign);

        return clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(priceAdGroup.getId())
                .withCampaignId(priceCampaign.getId())
                .withTnsId(CORRECT_TNS_ID);
    }

    private CpmBanner createCpmBanner(String tnsId) {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(adGroupInfo.getClientInfo());

        return clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withTnsId(tnsId);
    }
}
