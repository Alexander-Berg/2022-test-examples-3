package ru.yandex.direct.core.entity.banner.type.vcard;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.type.Helpers;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.vcardNotFound;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithVcardValidatorProviderTest {

    @Autowired
    public BannerWithVcardValidatorProvider provider;

    @Autowired
    public Helpers bannerHelpers;

    @Autowired
    public Steps steps;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private VcardInfo vcardInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        campaignInfo = adGroupInfo.getCampaignInfo();
        vcardInfo = steps.vcardSteps().createVcard(campaignInfo);
    }

    @Test
    public void validVcard() {
        BannerWithVcard banner = clientBannerWithVcard()
                .withVcardId(vcardInfo.getVcardId());

        ValidationResult<List<BannerWithVcard>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noVcard() {
        BannerWithVcard banner = clientBannerWithVcard()
                .withVcardId(null);

        ValidationResult<List<BannerWithVcard>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void nonExistingVcard() {
        BannerWithVcard banner = clientBannerWithVcard()
                .withVcardId(Long.MAX_VALUE);

        ValidationResult<List<BannerWithVcard>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithVcard.VCARD_ID)),
                vcardNotFound()))));
    }

    private BannerWithVcard clientBannerWithVcard() {
        return clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(campaignInfo.getCampaignId())
                .withVcardId(vcardInfo.getVcardId());
    }

    private ValidationResult<List<BannerWithVcard>, Defect> validate(BannerWithVcard banner) {
        return validate(List.of(banner));
    }

    private ValidationResult<List<BannerWithVcard>, Defect> validate(List<BannerWithVcard> banners) {
        BannersAddOperationContainer validationContainer = bannerHelpers.createValidationContainer(clientInfo, banners);
        return ListValidationBuilder.<BannerWithVcard, Defect>of(banners)
                .checkEachBy(provider.bannerWithVcardValidator(validationContainer))
                .getResult();
    }
}
