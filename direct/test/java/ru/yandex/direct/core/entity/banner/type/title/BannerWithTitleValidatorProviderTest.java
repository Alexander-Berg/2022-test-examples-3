package ru.yandex.direct.core.entity.banner.type.title;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithTitle;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.data.TestBannerValidationContainers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstants.MAX_NUMBER_OF_NARROW_CHARACTERS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxNumberOfNarrowCharacters;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_CONTENT_PROMOTION_TITLE;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_TITLE;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithTitleValidatorProviderTest {

    private static final String DEFAULT_MAX_TITLE_LENGTH_STR = "long title long title long title lo";
    private static final String CONTENT_PROMO_MAX_TITLE_LENGTH_STR =
            "long title long title long title long title long title long title long title long title long title l" +
                    "long title long title long title long title long title long title long title long title long " +
                    "title l";
    private static final String MAX_NARROW_CHARACTERS_STR = ".,!:;\".,!:;\".,!";

    private static final Path PATH = path(index(0), field(BannerWithTitle.TITLE));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithTitle banner;

    @Parameterized.Parameter(2)
    public TestBannerValidationContainers.Builder validationContainer;

    @Parameterized.Parameter(3)
    public Defect<String> expectedDefect;

    @Parameterized.Parameter(4)
    public Boolean useNewLimits;

    @SuppressWarnings("ConstantConditions")
    public BannerWithTitleValidatorProviderTest() {
        checkState(DEFAULT_MAX_TITLE_LENGTH_STR.length() == MAX_LENGTH_TITLE,
                "fix test data please");
        checkState(CONTENT_PROMO_MAX_TITLE_LENGTH_STR.length() == MAX_LENGTH_CONTENT_PROMOTION_TITLE,
                "fix test data please");

        checkState(MAX_NARROW_CHARACTERS_STR.length() == MAX_NUMBER_OF_NARROW_CHARACTERS,
                "fix test data please");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: пустой заголовок не разрешен",
                        new TextBanner().withTitle(null),
                        newBannerValidationContainer(),
                        notNull(),
                        false
                },
                {
                        "Текстовый баннер: заголовок максимальной длины",
                        new TextBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Текстовый баннер: превышено количество обычных символов в заголовке",
                        new TextBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE),
                        false
                },
                {
                        "Текстовый баннер: узкие символы не учитываются при подсчете длины заголовка",
                        new TextBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Текстовый баннер: проверяется макс. количество узких символов",
                        new TextBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS),
                        false
                },

                {
                        "РМП: заголовок максимальной длины",
                        new MobileAppBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "РМП: узкие символы учитываются наравне с остальными при проверке длины заголовка",
                        new MobileAppBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE),
                        false
                },
                {
                        "РМП: максимальное число узких символов не проверяется",
                        new MobileAppBanner().withTitle(MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        null,
                        false
                },

                {
                        "Продвижение контента - видео: максимальная длина заголовка",
                        new ContentPromotionBanner()
                                .withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        null,
                        false
                },
                {
                        "Продвижение контента - видео: превышено количество обычных символов в заголовке",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR + "A"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_CONTENT_PROMOTION_TITLE),
                        false
                },
                {
                        "Продвижение контента - видео: узкие символы не учитываются при подсчете длины заголовка",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        null,
                        false
                },
                {
                        "Продвижение контента - видео: проверяется макс. количество узких символов",
                        new ContentPromotionBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.VIDEO),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS),
                        false
                },

                {
                        "Продвижение контента - услуги: максимальная длина заголовка",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.SERVICE),
                        null,
                        false
                },
                {
                        "Продвижение контента - услуги: превышено количество обычных символов в заголовке",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR + "A"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.SERVICE),
                        maxStringLength(MAX_LENGTH_CONTENT_PROMOTION_TITLE),
                        false
                },
                {
                        "Продвижение контента - услуги: заголовок включает только разрешенные символы",
                        new ContentPromotionBanner().withTitle("короткий" + "Ω"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.SERVICE),
                        restrictedCharsInField(),
                        false
                },
                {
                        "Продвижение контента - услуги: узкие символы не учитываются при подсчете длины заголовка",
                        new ContentPromotionBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.SERVICE),
                        null,
                        false
                },

                {
                        "Продвижение контента - еда: максимальная длина заголовка",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.EDA),
                        null,
                        false
                },
                {
                        "Продвижение контента - еда: превышено количество обычных символов в заголовке",
                        new ContentPromotionBanner().withTitle(CONTENT_PROMO_MAX_TITLE_LENGTH_STR + "A"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.EDA),
                        maxStringLength(MAX_LENGTH_CONTENT_PROMOTION_TITLE),
                        false
                },
                {
                        "Продвижение контента - еда: заголовок включает только разрешенные символы",
                        new ContentPromotionBanner().withTitle("короткий" + "Ω"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.EDA),
                        restrictedCharsInField(),
                        false
                },
                {
                        "Продвижение контента - еда: узкие символы не учитываются при подсчете длины заголовка",
                        new ContentPromotionBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.EDA),
                        null,
                        false
                },

                {
                        "Продвижение контента - коллекции: заголовок запрещен",
                        new ContentPromotionBanner().withTitle("короткий"),
                        validationContainerWithContentPromotionBanner(ContentPromotionAdgroupType.COLLECTION),
                        isNull(),
                        false
                },

                {
                        "Продвижение контента - неизвестный тип: null в заголовке разрешен",
                        new ContentPromotionBanner().withTitle(null),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Продвижение контента - неизвестный тип: не-null в заголовке разрешен",
                        new ContentPromotionBanner().withTitle("короткий"),
                        newBannerValidationContainer(),
                        null,
                        false
                },

                {
                        "Охватный баннер: пустой заголовок разрешен",
                        new CpmBanner().withTitle(null),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Охватный баннер: пустая строка в заголовке не разрешена",
                        new CpmBanner().withTitle(""),
                        newBannerValidationContainer(),
                        stringShouldNotBeBlank(),
                        false
                },
                {
                        "Охватный баннер: заголовок максимальной длины",
                        new CpmBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Охватный баннер: заголовок максимальной длины",
                        new CpmBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE),
                        false
                },
                {
                        "Охватный баннер: узкие символы не учитываются при подсчете длины заголовка",
                        new CpmBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Охватный баннер: проверяется макс. количество узких символов",
                        new CpmBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS),
                        false
                },

                {
                        "Графический баннер: пустой заголовок разрешен",
                        new ImageBanner().withTitle(null),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Графический баннер: пустая строка в заголовке не разрешена",
                        new ImageBanner().withTitle(""),
                        newBannerValidationContainer(),
                        stringShouldNotBeBlank(),
                        false
                },
                {
                        "Графический баннер: заголовок максимальной длины",
                        new ImageBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + MAX_NARROW_CHARACTERS_STR),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Графический баннер: заголовок максимальной длины",
                        new ImageBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "A"),
                        newBannerValidationContainer(),
                        maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE),
                        false
                },
                {
                        "Графический баннер: узкие символы не учитываются при подсчете длины заголовка",
                        new ImageBanner().withTitle(DEFAULT_MAX_TITLE_LENGTH_STR + "."),
                        newBannerValidationContainer(),
                        null,
                        false
                },
                {
                        "Графический баннер: проверяется макс. количество узких символов",
                        new ImageBanner().withTitle("короткий" + MAX_NARROW_CHARACTERS_STR + "."),
                        newBannerValidationContainer(),
                        maxNumberOfNarrowCharacters(MAX_NUMBER_OF_NARROW_CHARACTERS),
                        false
                },
        });
    }

    private BannerWithTitleValidatorProvider provider;

    @Before
    public void before() {
        FeatureService featureService = mock(FeatureService.class);
        doReturn(useNewLimits).when(featureService).isEnabledForClientId(any(ClientId.class), any(FeatureName.class));
        BannerConstantsService bannerConstantsService = new BannerConstantsService(featureService);
        provider = new BannerWithTitleValidatorProvider(bannerConstantsService);
    }

    @Test
    public void testValidationProvider() {
        validationContainer.withBannerToIndexMap(Map.of(banner, 0));
        ValidationResult<List<BannerWithTitle>, Defect> vr = validate(banner, validationContainer.build());
        if (expectedDefect != null) {
            assertThat(vr, hasDefectWithDefinition(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithTitle>, Defect> validate(
            BannerWithTitle banner, BannersAddOperationContainer validationContainer) {
        return ListValidationBuilder.<BannerWithTitle, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithTitleValidator(validationContainer))
                .getResult();
    }

    private static TestBannerValidationContainers.Builder validationContainerWithContentPromotionBanner(
            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return newBannerValidationContainer()
                .withIndexToContentPromotionAdgroupTypeMap(Map.of(0, contentPromotionAdgroupType));
    }
}
