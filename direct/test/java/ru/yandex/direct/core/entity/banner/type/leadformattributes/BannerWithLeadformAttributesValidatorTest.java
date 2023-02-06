package ru.yandex.direct.core.entity.banner.type.leadformattributes;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithLeadformAttributes;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.emptyHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentLeadformHrefAndButtonText;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxHrefLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;
import static ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstants.MAX_LENGTH_HREF;
import static ru.yandex.direct.core.entity.banner.type.leadformattributes.BannerWithLeadformAttributesValidatorProvider.MAX_TEXT_LENGTH;
import static ru.yandex.direct.core.entity.banner.type.leadformattributes.BannerWithLeadformAttributesValidatorProvider.bannerWithLeadformAttributesValidator;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithLeadformAttributesValidatorTest {

    private static final String VALID_HREF = "https://yandex.ru/";
    private static final String VALID_BUTTON_TEXT = "Оставить заявку";

    private static final String INVALID_HREF = "Invalid href";
    private static final String INVALID_BUTTON_TEXT = "Button text Ω";

    private static final String MAX_LENGTH_TEXT = RandomStringUtils.randomAlphanumeric(MAX_TEXT_LENGTH);
    private static final String TOO_LONG_TEXT = RandomStringUtils.randomAlphanumeric(MAX_TEXT_LENGTH + 1);

    private static final String MAX_LENGTH_HREF_STR = VALID_HREF + RandomStringUtils.randomAlphabetic(MAX_LENGTH_HREF - VALID_HREF.length());
    private static final String TOO_LONG_HREF = VALID_HREF + RandomStringUtils.randomAlphabetic(MAX_LENGTH_HREF - VALID_HREF.length() + 1);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String leadformHref;

    @Parameterized.Parameter(2)
    public String leadformButtonText;

    @Parameterized.Parameter(3)
    public Defect expectedHrefDefect;

    @Parameterized.Parameter(4)
    public Defect expectedButtonTextDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] parameters() {
        return new Object[][]{
                {"Незаполненные поля", null, null, null, null},
                {"Незаполненный текст кнопки", VALID_HREF, null, inconsistentLeadformHrefAndButtonText(), null},
                {"Незаполненная ссылка", null, VALID_BUTTON_TEXT, null, inconsistentLeadformHrefAndButtonText()},
                {"Заполненные поля", VALID_HREF, VALID_BUTTON_TEXT, null, null},

                {"Пустые поля", "", "", emptyHref(), notEmptyString()},
                {"Пустой текст кнопки", VALID_HREF, "", null, notEmptyString()},
                {"Пустая ссылка", "", VALID_BUTTON_TEXT, emptyHref(), null},

                {"Невалидная ссылка", INVALID_HREF, VALID_BUTTON_TEXT, invalidHref(), null},
                {"Невалидный текст кнопки", VALID_HREF, INVALID_BUTTON_TEXT, null, restrictedCharsInField()},
                {"Ссылка максимальной длины", MAX_LENGTH_HREF_STR, VALID_BUTTON_TEXT, null, null},
                {"Ссылка с длиной больше максимальной", TOO_LONG_HREF, VALID_BUTTON_TEXT, maxHrefLengthWithoutTemplateMarker(MAX_LENGTH_HREF), null},
                {"Текст кнопки максимальной длины", VALID_HREF, MAX_LENGTH_TEXT, null, null},
                {"Текст кнопки превышающей максимум длины", VALID_HREF, TOO_LONG_TEXT, null, maxStringLength(MAX_TEXT_LENGTH)}
        };
    }

    private static BannerWithLeadformAttributes createBanner(String leadformHref, String leadformButtonText) {
        return new TextBanner()
                .withLeadformHref(leadformHref)
                .withLeadformButtonText(leadformButtonText);
    }

    @Test
    public void validatorTest() {
        BannerWithLeadformAttributes banner = createBanner(leadformHref, leadformButtonText);

        ValidationResult<BannerWithLeadformAttributes, Defect> vr = bannerWithLeadformAttributesValidator().apply(banner);

        if (expectedHrefDefect == null && expectedButtonTextDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            SoftAssertions.assertSoftly(softly -> {
                if (expectedHrefDefect != null) {
                    softly.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(
                            path(field(BannerWithLeadformAttributes.LEADFORM_HREF)),
                            expectedHrefDefect
                    ))));
                } else {
                    softly.assertThat(vr.getSubResults().get(field(BannerWithLeadformAttributes.LEADFORM_HREF))).is(
                            matchedBy(hasNoDefectsDefinitions()));
                }
                if (expectedButtonTextDefect != null) {
                    softly.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(
                            path(field(BannerWithLeadformAttributes.LEADFORM_BUTTON_TEXT)),
                            expectedButtonTextDefect
                    ))));
                } else {
                    softly.assertThat(vr.getSubResults().get(field(BannerWithLeadformAttributes.LEADFORM_BUTTON_TEXT))).is(
                            matchedBy(hasNoDefectsDefinitions()));
                }
            });
        }
    }
}
