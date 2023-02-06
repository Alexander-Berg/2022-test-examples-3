package ru.yandex.direct.core.entity.banner.type.displayhreftexts;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHrefTexts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.type.displayhreftexts.BannerWithDisplayHrefTextsValidatorProvider.MAX_TEXT_LENGTH;
import static ru.yandex.direct.core.entity.banner.type.displayhreftexts.BannerWithDisplayHrefTextsValidatorProvider.bannerWithDisplayHrefTextsValidator;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithDisplayHrefTextsValidatorTest {

    private static final String VALID_TEXT = "Яндекс.Бизнес";
    private static final String OTHER_VALID_TEXT = "ТПК Абсолют";

    private static final String MAX_LENGTH_TEXT = RandomStringUtils.randomAlphanumeric(MAX_TEXT_LENGTH);
    private static final String TOO_LONG_TEXT = RandomStringUtils.randomAlphanumeric(MAX_TEXT_LENGTH + 1);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String displayHrefPrefix;

    @Parameterized.Parameter(2)
    public String displayHrefSuffix;

    @Parameterized.Parameter(3)
    public Defect expectedPrefixDefect;

    @Parameterized.Parameter(4)
    public Defect expectedSuffixDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] parameters() {
        return new Object[][]{
                {"Не заполненные поля",
                        null, null,
                        null, null},
                {"Не заполненный суффикс",
                        VALID_TEXT, null,
                        null, null},
                {"Не заполненный префикс",
                        null, OTHER_VALID_TEXT,
                        null, null},
                {"Заполненные поля",
                        VALID_TEXT, OTHER_VALID_TEXT,
                        null, null},
                {"Пустые поля",
                        "", "",
                        notEmptyString(), notEmptyString()},
                {"Пустой суффикс",
                        VALID_TEXT, "",
                        null, notEmptyString()},
                {"Пустой префикс",
                        "", OTHER_VALID_TEXT,
                        notEmptyString(), null},
                {"Префикс максимальной длины",
                        MAX_LENGTH_TEXT, null,
                        null, null},
                {"Префикс превышающей длины",
                        TOO_LONG_TEXT, null,
                        maxStringLength(MAX_TEXT_LENGTH), null},
                {"Суффикс максимальной длины",
                        null, MAX_LENGTH_TEXT,
                        null, null},
                {"Суффикс превышающей длины",
                        null, TOO_LONG_TEXT,
                        null, maxStringLength(MAX_TEXT_LENGTH)},
        };
    }

    private static BannerWithDisplayHrefTexts createBanner(String displayHrefPrefix, String displayHrefSuffix) {
        return new TextBanner()
                .withDisplayHrefPrefix(displayHrefPrefix)
                .withDisplayHrefSuffix(displayHrefSuffix);
    }

    @Test
    public void validatorTest() {
        BannerWithDisplayHrefTexts banner = createBanner(displayHrefPrefix, displayHrefSuffix);

        ValidationResult<BannerWithDisplayHrefTexts, Defect> vr = bannerWithDisplayHrefTextsValidator().apply(banner);

        if (expectedPrefixDefect == null && expectedSuffixDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            SoftAssertions.assertSoftly(softly -> {
                if (expectedPrefixDefect != null) {
                    softly.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(
                            path(field(BannerWithDisplayHrefTexts.DISPLAY_HREF_PREFIX)),
                            expectedPrefixDefect
                    ))));
                }
                if (expectedSuffixDefect != null) {
                    softly.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(
                            path(field(BannerWithDisplayHrefTexts.DISPLAY_HREF_SUFFIX)),
                            expectedSuffixDefect
                    ))));
                }
            });
        }
    }
}
