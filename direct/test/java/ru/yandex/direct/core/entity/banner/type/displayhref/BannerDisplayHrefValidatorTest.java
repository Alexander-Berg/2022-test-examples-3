package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.duplicateSpecCharsInDisplayHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxLengthDisplayHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInDisplayHref;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.MAX_LENGTH_DISPLAY_HREF;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefValidatorProvider.displayHrefValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

@RunWith(Parameterized.class)
public class BannerDisplayHrefValidatorTest {
    private static final String VALID_HREF = "http://ya.ru";
    private static final String MAX_LENGTH_DISPLAY_URL = "long/display/url/lon";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String href;

    @Parameterized.Parameter(2)
    public String displayHref;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: валидная отображаемая ссылка максимальной длины",
                        VALID_HREF, MAX_LENGTH_DISPLAY_URL,
                        null
                },
                {
                        "Текстовый баннер: превышение максимальной длины отображаемой ссылки",
                        VALID_HREF, MAX_LENGTH_DISPLAY_URL + "a",
                        maxLengthDisplayHref(MAX_LENGTH_DISPLAY_HREF)
                },
                {
                        "Текстовый баннер: отображаемая ссылка содержит недопустимые символы",
                        VALID_HREF, "https://www.ya.ru",
                        restrictedCharsInDisplayHref(":..")
                },
                {
                        "Текстовый баннер: отображаемая ссылка содержит двойные спецсимволы",
                        VALID_HREF, "site//page",
                        duplicateSpecCharsInDisplayHref()
                }
        });
    }

    @Test
    public void validationText() {
        BannerWithDisplayHref banner = createBanner(href, displayHref);

        ValidationResult<String, Defect> vr = displayHrefValidator().apply(banner.getDisplayHref());

        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(emptyPath(), expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private BannerWithDisplayHref createBanner(String href, String displayHref) {
        return new TextBanner().withHref(href).withDisplayHref(displayHref);
    }
}
