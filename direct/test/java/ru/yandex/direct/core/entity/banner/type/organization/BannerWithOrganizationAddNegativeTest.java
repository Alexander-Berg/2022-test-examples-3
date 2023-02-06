package ru.yandex.direct.core.entity.banner.type.organization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganization.PERMALINK_ID;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void nonexistentOrganizationForTextBanner() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = clientInfo.getClientId();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var organization = defaultActiveOrganization(clientId);
        var banner = clientTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(PERMALINK_ID)), organizationNotFound())));
    }

}
