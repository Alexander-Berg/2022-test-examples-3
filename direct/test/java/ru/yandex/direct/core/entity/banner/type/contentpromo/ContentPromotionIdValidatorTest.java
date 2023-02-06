package ru.yandex.direct.core.entity.banner.type.contentpromo;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.EDA;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.contentPromotionCollectionIsInaccessible;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.contentPromotionVideoIsInaccessible;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.contentTypeNotMatchesAdGroupContentType;
import static ru.yandex.direct.core.entity.banner.type.contentpromo.ContentPromotionIdValidator.contentPromotionIdValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class ContentPromotionIdValidatorTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Long contentPromotionId;

    @Parameterized.Parameter(2)
    public ContentPromotionAdgroupType contentPromotionAdgroupType;

    @Parameterized.Parameter(3)
    public ContentPromotionContent content;

    @Parameterized.Parameter(4)
    public List<Defect<Long>> expectedDefects;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // кейсы с валидным id
                {
                        "contentPromotionId валиден и соответствует типу контента группы (video)",
                        1L,
                        VIDEO,
                        content(ContentPromotionContentType.VIDEO),
                        null
                },
                {
                        "contentPromotionId валиден и соответствует типу контента группы (service)",
                        1L,
                        SERVICE,
                        content(ContentPromotionContentType.SERVICE),
                        null
                },
                {
                        "contentPromotionId валиден и соответствует типу контента группы (collection)",
                        1L,
                        COLLECTION,
                        content(ContentPromotionContentType.COLLECTION),
                        null
                },
                {
                        "contentPromotionId валиден и соответствует типу контента группы (eda)",
                        1L,
                        EDA,
                        content(ContentPromotionContentType.EDA),
                        null
                },

                // контент существует и доступен, тип группы не определен
                // такая проблема должна ловиться где-то в другом месте
                {
                        "contentPromotionId указывает на существующий контент, но тип группы не определен",
                        1L,
                        null,
                        content(ContentPromotionContentType.VIDEO),
                        null
                },

                // null
                {
                        "contentPromotionId == null",
                        null,
                        null,
                        null,
                        singletonList(CommonDefects.notNull())
                },

                // несуществующий контент
                {
                        "contentPromotionId указывает на несуществующий контент, при этом тип группы определен",
                        1L,
                        COLLECTION,
                        null,
                        singletonList(objectNotFound())
                },
                {
                        "contentPromotionId указывает на несуществующий контент, при этом тип группы не определен",
                        1L,
                        null,
                        null,
                        singletonList(objectNotFound())
                },

                // контент существует, но недоступен
                {
                        "contentPromotionId указывает на существующий контент, но он недоступен (video)",
                        1L,
                        null,
                        content(ContentPromotionContentType.VIDEO).withIsInaccessible(true),
                        singletonList(contentPromotionVideoIsInaccessible())
                },
                {
                        "contentPromotionId указывает на существующий контент, но он недоступен (collection)",
                        1L,
                        null,
                        content(ContentPromotionContentType.COLLECTION).withIsInaccessible(true),
                        singletonList(contentPromotionCollectionIsInaccessible())
                },
                {
                        "contentPromotionId указывает на существующий контент, но он недоступен (service)",
                        1L,
                        null,
                        content(ContentPromotionContentType.SERVICE).withIsInaccessible(true),
                        null // услуги не бывают недоступны
                },
                {
                        "contentPromotionId указывает на существующий контент, но он недоступен (eda)",
                        1L,
                        null,
                        content(ContentPromotionContentType.EDA).withIsInaccessible(true),
                        null // еда не бывает недоступна
                },
                {
                        "contentPromotionId указывает на существующий контент, но он недоступен (video), " +
                                "при этом тип группы известен",
                        1L,
                        VIDEO,
                        content(ContentPromotionContentType.VIDEO).withIsInaccessible(true),
                        singletonList(contentPromotionVideoIsInaccessible())
                },

                // контент существует и доступен, но не соответствует типу группы
                {
                        "contentPromotionId указывает на существующий контент, но он не соответствует типу группы",
                        1L,
                        COLLECTION,
                        content(ContentPromotionContentType.VIDEO),
                        singletonList(contentTypeNotMatchesAdGroupContentType())
                },

                // контент существует, но он недоступен и не соответствует типу группы
                {
                        "contentPromotionId указывает на существующий (и недоступный) контент, " +
                                "но он не соответствует типу группы",
                        1L,
                        COLLECTION,
                        content(ContentPromotionContentType.VIDEO).withIsInaccessible(true),
                        asList(contentPromotionVideoIsInaccessible(), contentTypeNotMatchesAdGroupContentType())
                },
        });
    }

    private static ContentPromotionContent content(ContentPromotionContentType type) {
        return new ContentPromotionContent()
                .withType(type)
                .withIsInaccessible(false);
    }

    @Test
    public void testContentPromotionIdValidator() {
        ContentPromotionIdValidator validator =
                contentPromotionIdValidator(contentPromotionAdgroupType, content);
        ValidationResult<Long, Defect> validationResult = validator.apply(contentPromotionId);

        if (expectedDefects != null) {
            expectedDefects.forEach(expectedDefect -> {
                assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), expectedDefect)));
            });
            assertThat("validator must add only expected errors",
                    validationResult.flattenErrors(),
                    hasSize(expectedDefects.size()));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }
}
