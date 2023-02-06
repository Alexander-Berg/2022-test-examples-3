package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithHrefAddValidationTypeSupportPreValidateTest {

    private static final Path PATH_HREF = path(index(0), field(BannerWithHref.HREF));
    private static final Path PATH_DOMAIN = path(index(0), field(BannerWithHref.DOMAIN));
    private static final Path PATH_DOMAIN_ID = path(index(0), field(BannerWithHref.DOMAIN_ID));
    private static final String DEFAULT_DOMAIN = "ya.ru";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithHref banner;

    @Parameterized.Parameter(2)
    public boolean isOperatorInternal;

    @Parameterized.Parameter(3)
    public Defect<String> expectedDefect;

    @Parameterized.Parameter(4)
    public Path path;

    private BannerWithHrefAddValidationTypeSupport typeSupportUnderTest;

    @Before
    public void before() {
        typeSupportUnderTest = new BannerWithHrefAddValidationTypeSupport(null);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: ничего не задано",
                        new TextBanner(),
                        false,
                        null,
                        null
                },
                {
                        "Текстовый баннер: domain задан оператором",
                        new TextBanner().withDomain(DEFAULT_DOMAIN),
                        true,
                        null,
                        null
                },
                {
                        "ContentPromotion баннер: domain задан",
                        new ContentPromotionBanner().withDomain(DEFAULT_DOMAIN),
                        false,
                        isNull(),
                        PATH_DOMAIN
                },
                {
                        "ContentPromotion баннер: domain задан оператором",
                        new ContentPromotionBanner().withDomain(DEFAULT_DOMAIN),
                        true,
                        isNull(),
                        PATH_DOMAIN
                },
                {
                        "ContentPromotion баннер: href задан",
                        new ContentPromotionBanner().withHref("http://ya.ru"),
                        false,
                        isNull(),
                        PATH_HREF
                },
                {
                        "ContentPromotion баннер: domain_id задан",
                        new ContentPromotionBanner().withDomainId(1L),
                        false,
                        isNull(),
                        PATH_DOMAIN_ID
                },
        });
    }

    @Test
    public void testTypeSupportPreValidation() {
        ValidationResult<List<BannerWithHref>, Defect> vr = validate(banner, isOperatorInternal);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithHref>, Defect> validate(
            BannerWithHref banner, boolean isOperatorInternal) {
        BannersAddOperationContainer container = newBannerValidationContainer()
                .withOperatorInternal(isOperatorInternal)
                .build();
        return typeSupportUnderTest.preValidate(container, ValidationResult.success(singletonList(banner)));
    }

}
