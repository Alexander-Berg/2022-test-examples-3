package ru.yandex.direct.core.entity.banner.type.organization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithOrganization;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganization.PERMALINK_ID;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithOrganization> {

    @Test
    public void addNonexistentOrganizationForTextBanner() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        var organization = defaultActiveOrganization(bannerInfo.getClientId());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization.getPermalinkId(), PERMALINK_ID);

        var validationResult = prepareAndApplyInvalid(modelChanges);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(PERMALINK_ID)), organizationNotFound())
        ));
    }

}
