package ru.yandex.direct.core.entity.banner.type.titleextension;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtension;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstants.MAX_NUMBER_OF_NARROW_CHARACTERS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.absentValueInField;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxLengthWordTemplateMarker;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxNumberOfNarrowCharacters;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_TITLE_WORD;
import static ru.yandex.direct.core.entity.banner.type.titleextension.BannerWithTitleExtensionConstants.MAX_LENGTH_TITLE_EXTENSION;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithTitleExtensionValidatorProviderTest {

    private static final String MAX_LENGTH_TITLE_EXTENSION_STR = "long title extension long titl";
    private static final String MAX_WORD_LENGTH_STR = "TheTwentyTwoLetterWord";
    private static final String MAX_NARROW_CHARACTERS_STR = ".,!:;\".,!:;\".,!";
    private static final Path PATH = path(index(0), field(BannerWithTitleExtension.TITLE_EXTENSION));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithTitleExtension banner;

    @Parameterized.Parameter(2)
    public Defect<String> expectedDefect;

    private BannerWithTitleExtensionValidatorProvider provider =
            new BannerWithTitleExtensionValidatorProvider();

    @SuppressWarnings("ConstantConditions")
    public BannerWithTitleExtensionValidatorProviderTest() {
        checkState(MAX_LENGTH_TITLE_EXTENSION_STR.length() == MAX_LENGTH_TITLE_EXTENSION,
                "fix test data please (MAX_LENGTH_TITLE_EXTENSION_STR)");
        checkState(MAX_NARROW_CHARACTERS_STR.length() == MAX_NUMBER_OF_NARROW_CHARACTERS,
                "fix test data please (MAX_NARROW_CHARACTERS_STR)");
        checkState(MAX_WORD_LENGTH_STR.length() == MAX_LENGTH_TITLE_WORD,
                "fix test data please (MAX_WORD_LENGTH_STR)");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "короткий заголовок2",
                        createBanner("продать товар"),
                        null
                },
                {
                        "заголовок2 максимальной длины с максимальным количеством узких символов",
                        createBanner(MAX_LENGTH_TITLE_EXTENSION_STR + MAX_NARROW_CHARACTERS_STR),
                        null
                },
                {
                        "превышено количество обычных символов",
                        createBanner(MAX_LENGTH_TITLE_EXTENSION_STR + "A"),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE_EXTENSION)
                },
                {
                        "узкие символы не учитываются при подсчете длины",
                        createBanner(MAX_LENGTH_TITLE_EXTENSION_STR + "."),
                        null
                },
                {
                        "максимальное количество узких символов",
                        createBanner("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },
                {
                        "заголовок2 не является обязательным",
                        createBanner(null),
                        null
                },
                {
                        "нельзя передавать пустую строку",
                        createBanner(""),
                        absentValueInField()
                },
                {
                        "максимальная длина слова",
                        createBanner(MAX_WORD_LENGTH_STR + " word2"),
                        null
                },
                {
                        "превышена максимальная длина слова",
                        createBanner(MAX_WORD_LENGTH_STR + "А"),
                        maxLengthWordTemplateMarker(MAX_LENGTH_TITLE_WORD)
                },
                {
                        "метки шаблонов не учитываются при подсчете длины",
                        createBanner(MAX_LENGTH_TITLE_EXTENSION_STR.replaceFirst("title", "#title#")),
                        null
                },
        });
    }

    private static BannerWithTitleExtension createBanner(String titleExtension) {
        return new TextBanner().withTitleExtension(titleExtension);
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithTitleExtension>, Defect> vr =
                ListValidationBuilder.<BannerWithTitleExtension, Defect>of(singletonList(banner))
                        .checkEachBy(provider.bannerWithTitleExtensionValidator())
                        .getResult();

        if (expectedDefect != null) {
            assertThat(vr, hasDefectWithDefinition(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

}
