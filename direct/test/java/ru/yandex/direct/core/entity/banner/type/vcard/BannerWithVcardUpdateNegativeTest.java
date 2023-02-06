package ru.yandex.direct.core.entity.banner.type.vcard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithVcard;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithVcard.VCARD_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithVcardUpdateNegativeTest extends
        BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithVcard> {

    @Test
    public void invalidVcardId() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(-1L, VCARD_ID);

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(changes);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(VCARD_ID)),
                validId()))));
    }
}
