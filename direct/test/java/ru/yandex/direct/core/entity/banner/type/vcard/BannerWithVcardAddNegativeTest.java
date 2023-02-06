package ru.yandex.direct.core.entity.banner.type.vcard;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.core.entity.banner.model.BannerWithVcard.VCARD_ID;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithVcardAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void invalidVcardId() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withVcardId(-1L);

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(banner);
        Assertions.assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(VCARD_ID)),
                validId()))));
    }
}
