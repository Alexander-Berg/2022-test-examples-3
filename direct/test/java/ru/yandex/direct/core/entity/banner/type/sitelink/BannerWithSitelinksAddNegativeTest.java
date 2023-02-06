package ru.yandex.direct.core.entity.banner.type.sitelink;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSitelinks.SITELINKS_SET_ID;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithSitelinksAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void invalidSitelinksSetId() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withSitelinksSetId(-1L);

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(banner);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(SITELINKS_SET_ID)),
                validId()))));
    }
}
