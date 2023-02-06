package ru.yandex.direct.core.entity.banner.type.contentpromo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.EDA;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.banner.type.contentpromo.VisitUrlValidator.visitUrlValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

@RunWith(Parameterized.class)
public class VisitUrlValidatorTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public ContentPromotionAdgroupType contentPromotionAdgroupType;

    @Parameterized.Parameter(2)
    public String visitUrl;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Группа - video: валидный url",
                        VIDEO,
                        "https://www.yandex.ru",
                        null
                },
                {
                        "Группа - video: url не задан",
                        VIDEO,
                        null,
                        null
                },
                {
                        "Группа - video: url пустой",
                        VIDEO,
                        "",
                        notEmptyString()
                },
                {
                        "Группа - video: url из пробельных символов",
                        VIDEO,
                        " \t \t",
                        notEmptyString()
                },
                {
                        "Группа - video: невалидный url",
                        VIDEO,
                        "https://ya",
                        invalidValue()
                },

                {
                        "Группа - collection: валидный url",
                        COLLECTION,
                        "https://www.yandex.ru",
                        null
                },
                {
                        "Группа - collection: url не задан",
                        COLLECTION,
                        null,
                        null
                },
                {
                        "Группа - collection: url пустой",
                        COLLECTION,
                        "",
                        notEmptyString()
                },
                {
                        "Группа - collection: невалидный url",
                        COLLECTION,
                        "https://ya",
                        invalidValue()
                },

                {
                        "Группа - service: валидный url",
                        SERVICE,
                        "https://www.yandex.ru",
                        isNull()
                },
                {
                        "Группа - service: url не задан",
                        SERVICE,
                        null,
                        null
                },
                {
                        "Группа - service: url из пробельных символов",
                        SERVICE,
                        " ",
                        isNull()
                },

                {
                        "Группа - eda: валидный url",
                        EDA,
                        "https://www.yandex.ru",
                        isNull()
                },
                {
                        "Группа - eda: url не задан",
                        EDA,
                        null,
                        null
                },
                {
                        "Группа - eda: url из пробельных символов",
                        EDA,
                        " ",
                        isNull()
                },
        });
    }

    @Test
    public void testVisitUrlValidator() {
        VisitUrlValidator visitUrlValidator = visitUrlValidator(contentPromotionAdgroupType);
        ValidationResult<String, Defect> validationResult = visitUrlValidator.apply(visitUrl);

        if (expectedDefect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(emptyPath(), expectedDefect)));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }
}
