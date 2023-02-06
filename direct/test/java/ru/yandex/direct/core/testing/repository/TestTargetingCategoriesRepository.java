package ru.yandex.direct.core.testing.repository;

import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.dbschema.ppcdict.enums.TargetingCategoriesState;
import ru.yandex.direct.dbschema.ppcdict.enums.TargetingCategoriesTargetingType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.TargetingCategories.TARGETING_CATEGORIES;

/**
 * Работа с категориями таргетинга в тестах
 */
@Repository
public class TestTargetingCategoriesRepository {
    private final DslContextProvider dslContextProvider;
    private final TargetingCategoriesCache targetingCategoriesCache;

    public TestTargetingCategoriesRepository(DslContextProvider dslContextProvider,
                                             TargetingCategoriesCache targetingCategoriesCache) {
        this.dslContextProvider = dslContextProvider;
        this.targetingCategoriesCache = targetingCategoriesCache;
    }

    /**
     * Добавить новую категорию таргетинга в базу
     *
     * @param targetingCategory Новая категория таргетинга
     */
    public void addTargetingCategory(TargetingCategory targetingCategory) {
        dslContextProvider.ppcdict()
                .insertInto(TARGETING_CATEGORIES, TARGETING_CATEGORIES.CATEGORY_ID, TARGETING_CATEGORIES.IMPORT_ID,
                        TARGETING_CATEGORIES.TARGETING_TYPE, TARGETING_CATEGORIES.NAME, TARGETING_CATEGORIES.STATE,
                        TARGETING_CATEGORIES.PARENT_CATEGORY_ID)
                .values(targetingCategory.getTargetingCategoryId(), ULong.valueOf(targetingCategory.getImportId()),
                        TargetingCategoriesTargetingType.rmp_interest, "Test", TargetingCategoriesState.Submitted,
                        targetingCategory.getParentId())
                .onDuplicateKeyIgnore()
                .execute();
        targetingCategoriesCache.invalidate();
    }
}
