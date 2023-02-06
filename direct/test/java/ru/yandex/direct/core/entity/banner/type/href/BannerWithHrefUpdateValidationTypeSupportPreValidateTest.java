package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.repository.BannerRepositoryConstants.BANNER_CLASS_TO_TYPE;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithHrefUpdateValidationTypeSupportPreValidateTest {

    private static final Path PATH_HREF = path(index(0), field(BannerWithHref.HREF));
    private static final Path PATH_DOMAIN = path(index(0), field(BannerWithHref.DOMAIN));
    private static final String DEFAULT_DOMAIN = "ya.ru";
    public static final String DEFAULT_HREF = "http://ya.ru";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public ModelChanges<BannerWithHref> oneModelChanges;

    @Parameterized.Parameter(2)
    public boolean isOperatorInternal;

    @Parameterized.Parameter(3)
    public Defect<String> expectedDefect;

    @Parameterized.Parameter(4)
    public Path path;

    private BannerWithHrefUpdateValidationTypeSupport typeSupportUnderTest;

    @Before
    public void before() {
        typeSupportUnderTest = new BannerWithHrefUpdateValidationTypeSupport(null);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: без изменений",
                        emptyModelChanges(),
                        false,
                        null,
                        null
                },
                {
                        "Текстовый баннер: href изменен",
                        getModelChanges(BannerWithHref.HREF, DEFAULT_HREF, TextBanner.class),
                        false,
                        null,
                        null
                },
                {
                        "Текстовый баннер: domain задан",
                        getModelChanges(BannerWithHref.DOMAIN, DEFAULT_DOMAIN, TextBanner.class),
                        false,
                        noRights(),
                        PATH_DOMAIN
                },
                {
                        "Текстовый баннер: domain задан оператором",
                        getModelChanges(BannerWithHref.DOMAIN, DEFAULT_DOMAIN, TextBanner.class),
                        true,
                        null,
                        null
                },
                {
                        "ContentPromotion баннер: domain задан",
                        getModelChanges(BannerWithHref.DOMAIN, DEFAULT_DOMAIN, ContentPromotionBanner.class),
                        false,
                        forbiddenToChange(),
                        PATH_DOMAIN
                },
                {
                        "ContentPromotion баннер: domain задан оператором",
                        getModelChanges(BannerWithHref.DOMAIN, DEFAULT_DOMAIN, ContentPromotionBanner.class),
                        true,
                        forbiddenToChange(),
                        PATH_DOMAIN
                },
                {
                        "ContentPromotion баннер: href задан",
                        getModelChanges(BannerWithHref.HREF, DEFAULT_HREF, ContentPromotionBanner.class),
                        false,
                        forbiddenToChange(),
                        PATH_HREF
                },
                {
                        "РМП баннер: href изменен",
                        getModelChanges(BannerWithHref.HREF, DEFAULT_HREF, MobileAppBanner.class),
                        false,
                        null,
                        null
                },
        });
    }

    @Test
    public void testTypeSupportPreValidation() {
        ValidationResult<List<ModelChanges<BannerWithHref>>, Defect> vr = validate(oneModelChanges,
                isOperatorInternal);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<ModelChanges<BannerWithHref>>, Defect> validate(
            ModelChanges<BannerWithHref> oneModelChanges, boolean isOperatorInternal) {
        BannersUpdateOperationContainer container = newBannerValidationContainer()
                .withOperatorInternal(isOperatorInternal)
                .withBannerType(oneModelChanges.getId(), BANNER_CLASS_TO_TYPE.get(oneModelChanges.getModelType()))
                .buildUpdate();
        return typeSupportUnderTest.preValidate(container, ValidationResult.success(singletonList(oneModelChanges)));
    }

    private static ModelChanges<BannerWithHref> emptyModelChanges() {
        return new ModelChanges<>(1L, (Class<? extends BannerWithHref>) TextBanner.class)
                .castModelUp(BannerWithHref.class);
    }

    private static ModelChanges<BannerWithHref> getModelChanges(ModelProperty<? super BannerWithHref, String> modelProperty,
                                                                String value,
                                                                Class<? extends BannerWithHref> tClass) {
        return new ModelChanges<>(1L, tClass)
                .process(value, modelProperty)
                .castModelUp(BannerWithHref.class);
    }
}
