package ru.yandex.direct.core.entity.banner.type.titleextension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTitleExtension;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtension.TITLE_EXTENSION;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.absentValueInField;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleExtensionUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTitleExtension> {

    @Test
    public void updateInvalidTitleExtension() {
        bannerInfo = createActiveBanner("продать товар");

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process("", TITLE_EXTENSION);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(changes);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(
                        path(field(TITLE_EXTENSION)),
                        absentValueInField())
        ));
    }

    private TextBannerInfo createActiveBanner(String titleExtension) {
        return steps.bannerSteps().createActiveTextBanner(
                activeTextBanner().withTitleExtension(titleExtension)
        );
    }

}
