package ru.yandex.direct.core.entity.banner.type.phone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone.PHONE_ID;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.hasNoAccessToOrganization;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAndPhoneUpdateNegativeTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Test
    public void prepareAndApply_BannerWithOrganization_NoRightsToOrg() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var clientId = clientInfo.getClientId();
        var organization = defaultActiveOrganization(clientId);
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withPermalinkId(organization.getPermalinkId())
                        .withPreferVCardOverPermalink(false))
                .withAdGroupInfo(adGroupInfo));

        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long phoneId = clientPhone.getId();

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(phoneId, PHONE_ID);

        var validationResult = prepareAndApplyInvalid(modelChanges);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(PHONE_ID)), hasNoAccessToOrganization())
        ));
        assertThat(validationResult.flattenErrors(), hasSize(1));
    }

}
