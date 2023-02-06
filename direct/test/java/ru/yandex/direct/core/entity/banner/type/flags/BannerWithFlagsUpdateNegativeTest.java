package ru.yandex.direct.core.entity.banner.type.flags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Path;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFlags.FLAGS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotChangeBannerFlagsFromAgeToOtherTypes;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.insufficientRights;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.age;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.alcohol;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.babyFood;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.empty;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithFlagsUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase {
    private static final Path PATH = path(field(FLAGS));

    @Test
    public void addImmutableFlag() {
        Long bannerId = createBanner(activeMobileAppBanner().withFlags(empty()));

        var changes = new ModelChanges<>(bannerId, MobileAppBanner.class).process(alcohol(), FLAGS);
        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, insufficientRights())
        ));
    }

    @Test
    public void removeImmutableFlag() {
        Long bannerId = createBanner(activeMobileAppBanner().withFlags(alcohol()));

        var changes = new ModelChanges<>(bannerId, MobileAppBanner.class).process(empty(), FLAGS);
        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, insufficientRights())
        ));
    }

    @Test
    public void replaceAgeByBabyFoodInTextBanner() {
        Long bannerId = createBanner(activeTextBanner().withFlags(age(6)));

        var changes = new ModelChanges<>(bannerId, TextBanner.class).process(babyFood(6), FLAGS);
        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, cannotChangeBannerFlagsFromAgeToOtherTypes())
        ));
    }

    private Long createBanner(OldBanner banner) {
        bannerInfo = steps.bannerSteps().createBanner(banner);
        return bannerInfo.getBannerId();
    }
}
