package ru.yandex.direct.core.entity.banner.type.body;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithBody;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.data.TestBannerValidationContainers;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstants.MAX_NUMBER_OF_NARROW_CHARACTERS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotHaveTemplate;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxNumberOfNarrowCharacters;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants.MAX_LENGTH_BODY;
import static ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants.MAX_LENGTH_CONTENT_PROMOTION_BODY;
import static ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants.MAX_LENGTH_MOBILE_BODY;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithBodyValidatorProviderTest {

    private static final String DEFAULT_MAX_BODY_LENGTH_STR =
            "long body long body long body long body long body long body long body long body 1";
    private static final String MOBILE_APP_MAX_BODY_LENGTH_STR =
            "long body long body long body long body long body long body long body long1";
    private static final String CONTENT_PROMO_MAX_BODY_LENGTH_STR =
            "long body long body long body long body long body " +
                    "long body long body long body long body long body " +
                    "long body long body long body long body long body " +
                    "long body long body long body long body long body1";
    private static final String MAX_NARROW_CHARACTERS_STR = ".,!:;\".,!:;\".,!";
    private static final String TEMPLATE_BODY_STR = "#sometemplate#";

    private static final Path PATH = path(index(0), field(BannerWithBody.BODY));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithBody banner;

    @Parameterized.Parameter(2)
    public TestBannerValidationContainers.Builder validationContainer;

    @Parameterized.Parameter(3)
    public Defect<String> expectedDefect;

    private BannerWithBodyValidatorProvider provider = new BannerWithBodyValidatorProvider();

    @SuppressWarnings("ConstantConditions")
    public BannerWithBodyValidatorProviderTest() {
        checkState(DEFAULT_MAX_BODY_LENGTH_STR.length() == MAX_LENGTH_BODY,
                "fix test data please");
        checkState(MOBILE_APP_MAX_BODY_LENGTH_STR.length() == MAX_LENGTH_MOBILE_BODY,
                "fix test data please");
        checkState(CONTENT_PROMO_MAX_BODY_LENGTH_STR.length() == MAX_LENGTH_CONTENT_PROMOTION_BODY,
                "fix test data please");
        checkState(MAX_NARROW_CHARACTERS_STR.length() == MAX_NUMBER_OF_NARROW_CHARACTERS,
                "fix test data please");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: пустое тело не разрешено",
                        new TextBanner().withBody(null),
                        newBannerValidationContainer(),
                        notNull()
                },
                {
                        "Текстовый баннер: тело максимальной длины",
                        new TextBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Текстовый баннер: превышено количество обычных символов в теле",
                        new TextBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_BODY)
                },
                {
                        "Текстовый баннер: узкие символы не учитываются при подсчете длины тела",
                        new TextBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Текстовый баннер: проверяется макс. количество узких символов",
                        new TextBanner().withBody("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },

                {
                        "Охватный баннер: пустое тело разрешено",
                        new CpmBanner().withBody(null),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Охватный баннер: пустая строка в теле не разрешена",
                        new CpmBanner().withBody(""),
                        newBannerValidationContainer(),
                        stringShouldNotBeBlank()
                },
                {
                        "Охватный баннер: тело максимальной длины",
                        new CpmBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Охватный баннер: превышено количество обычных символов в теле",
                        new CpmBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_BODY)
                },
                {
                        "Охватный баннер: узкие символы не учитываются при подсчете длины тела",
                        new CpmBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Охватный баннер: проверяется макс. количество узких символов",
                        new CpmBanner().withBody("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },

                {
                        "Графический баннер: пустое тело разрешено",
                        new ImageBanner().withBody(null),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Графический баннер: пустая строка в теле не разрешена",
                        new ImageBanner().withBody(""),
                        newBannerValidationContainer(),
                        stringShouldNotBeBlank()
                },
                {
                        "Графический баннер: тело максимальной длины",
                        new ImageBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Графический баннер: превышено количество обычных символов в теле",
                        new ImageBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_BODY)
                },
                {
                        "Графический баннер: узкие символы не учитываются при подсчете длины тела",
                        new ImageBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Графический баннер: проверяется макс. количество узких символов",
                        new ImageBanner().withBody("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },

                {
                        "Динамический баннер: тело максимальной длины",
                        new DynamicBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Динамический баннер: превышено количество обычных символов в теле",
                        new DynamicBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_BODY)
                },
                {
                        "Динамический баннер: узкие символы не учитываются при подсчете длины тела",
                        new DynamicBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Динамический баннер: проверяется макс. количество узких символов",
                        new DynamicBanner().withBody("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },
                {
                        "Динамический баннер: проверяется запрет на использование шаблонов",
                        new DynamicBanner().withBody("короткий" + TEMPLATE_BODY_STR + "."),
                        newBannerValidationContainer(),
                        cannotHaveTemplate()
                },

                {
                        "РМП: тело максимальной длины",
                        new MobileAppBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "РМП: узкие символы учитываются наравне с остальными при проверке длины тела",
                        new MobileAppBanner().withBody(DEFAULT_MAX_BODY_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_BODY)
                },
                {
                        "РМП: максимальное число узких символов не проверяется",
                        new MobileAppBanner().withBody(MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        null
                },

                {
                        "Продвижение контента - видео: максимальная длина тела",
                        new ContentPromotionBanner()
                                .withBody(CONTENT_PROMO_MAX_BODY_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        null
                },
                {
                        "Продвижение контента - видео: превышено количество обычных символов в теле",
                        new ContentPromotionBanner().withBody(CONTENT_PROMO_MAX_BODY_LENGTH_STR + "A"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_CONTENT_PROMOTION_BODY)
                },
                {
                        "Продвижение контента - видео: узкие символы не учитываются при подсчете длины тела",
                        new ContentPromotionBanner().withBody(CONTENT_PROMO_MAX_BODY_LENGTH_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        null
                },
                {
                        "Продвижение контента - видео: проверяется макс. количество узких символов",
                        new ContentPromotionBanner().withBody("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS)
                },

                {
                        "Продвижение контента - услуги: тело запрещено",
                        new ContentPromotionBanner().withBody("короткий"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.SERVICE),
                        isNull()
                },

                {
                        "Продвижение контента - коллекции: тело запрещено",
                        new ContentPromotionBanner().withBody("короткий"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.COLLECTION),
                        isNull()
                },

                {
                        "Продвижение контента - неизвестный тип: null в теле разрешен",
                        new ContentPromotionBanner().withBody(null),
                        newBannerValidationContainer(),
                        null
                },
                {
                        "Продвижение контента - неизвестный тип: не-null в теле разрешен",
                        new ContentPromotionBanner().withBody("короткий"),
                        newBannerValidationContainer(),
                        null
                },
        });
    }

    @Test
    public void testValidationProvider() {
        validationContainer.withBannerToIndexMap(Map.of(banner, 0));
        ValidationResult<List<BannerWithBody>, Defect> vr = validate(banner, validationContainer.build());
        if (expectedDefect != null) {
            assertThat(vr, hasDefectWithDefinition(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithBody>, Defect> validate(
            BannerWithBody banner, BannersAddOperationContainer validationContainer) {
        return ListValidationBuilder.<BannerWithBody, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithBodyValidator(validationContainer))
                .getResult();
    }

    private static TestBannerValidationContainers.Builder validationContainerWithContentPromotionBanner(
            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return newBannerValidationContainer()
                .withIndexToContentPromotionAdgroupTypeMap(Map.of(0, contentPromotionAdgroupType));
    }
}
