package ru.yandex.direct.core.entity.banner.type.language;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.type.BannerBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.steps.banner.ContentPromotionBannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentLanguageWithGeo;
import static ru.yandex.direct.core.entity.banner.type.language.BannerLanguageConverter.convertLanguage;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLanguageUpdateNegativeTest extends BannerBannerInfoUpdateOperationTestBase {

    private static final String TURKISH_TEXT = "İstanbul trafik yoğunluk";

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;
    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    @Test
    public void validLanguageForContentPromotionBanner() {
        ContentPromotionBanner banner = testNewContentPromotionBanners.fullContentPromoBanner(null, null)
                .withTitle("Русский заголовок")
                .withBody("Русский текст");
        bannerInfo = contentPromotionBannerSteps.createBanner(ContentPromotionContentType.VIDEO, banner);

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                        .process(TURKISH_TEXT, ContentPromotionBanner.TITLE)
                        .process(TURKISH_TEXT, ContentPromotionBanner.BODY);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(emptyPath(),
                inconsistentLanguageWithGeo(convertLanguage(Language.TR)))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
