package ru.yandex.direct.core.entity.banner.type.turbogallery;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboGallery;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.emptyHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.type.turbogallery.BannerTurboGalleryConstraints.MAX_LENGTH_TURBO_GALLERY_HREF;
import static ru.yandex.direct.core.entity.banner.type.turbogallery.BannerWithTurboGalleryValidatorProvider.turboGalleryHrefValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

@RunWith(Parameterized.class)
public class BannerWithTurboGalleryValidatorProviderTest {

    private static final String VALID_TURBO_GALLERY_HREF = "http://yandex.ru/turbo?text=lpc/42";
    private static final String TURBO_GALLERY_HREF_MAX_LENGTH = StringUtils.rightPad(VALID_TURBO_GALLERY_HREF,
            MAX_LENGTH_TURBO_GALLERY_HREF, "abcdef");

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String turboGalleryHref;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: невалидная ссылка",
                        "invalid href", invalidHref()
                },
                {
                        "Текстовый баннер: пустая ссылка",
                        "", emptyHref()
                },
                {
                        "Текстовый баннер: превышение максимальной длины ссылки",
                        TURBO_GALLERY_HREF_MAX_LENGTH + "a", maxStringLength(MAX_LENGTH_TURBO_GALLERY_HREF)
                },
                {
                        "Текстовый баннер: валидная ссылка",
                        TURBO_GALLERY_HREF_MAX_LENGTH, null
                }
        });
    }

    @Test
    public void validationText() {
        BannerWithTurboGallery banner = createBanner(turboGalleryHref);

        ValidationResult<String, Defect> vr = turboGalleryHrefValidator().apply(banner.getTurboGalleryHref());

        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(emptyPath(), expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private BannerWithTurboGallery createBanner(String turboGalleryHref) {
        return new TextBanner().withTurboGalleryHref(turboGalleryHref);
    }
}
