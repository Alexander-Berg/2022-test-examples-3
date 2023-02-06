package ru.yandex.market.deepmind.common.repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.VCategoriesForLifecycle;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;

import static ru.yandex.market.deepmind.common.category.CategoryTree.ROOT_CATEGORY_ID;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.V_CATEGORIES_FOR_LIFECYCLE;

public class VCategoriesForLifecycleTest extends DeepmindBaseDbTestClass {

    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private CategorySettingsRepository categorySettingsRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;

    private List<Category> categories = new ArrayList<>();

    @Before
    public void setUp() throws Exception {

        category(ROOT_CATEGORY_ID,
            category(1,
                category(1_1,
                    category(1_1_1),
                    category(1_1_2)
                ),
                category(1_2,
                    category(1_2_1),
                    category(1_2_2)
                )
            ),
            category(2,
                category(2_1,
                    category(2_1_1),
                    category(2_1_2)
                )
            )
        );

        categories = deepmindCategoryRepository.insertBatch(categories);
    }

    @Test
    public void inheritSeasonIdToSubcategories() {
        // cat 1 -> season 4
        //    cat 1_1 -> null
        //        cat 1_1_1 -> season 5
        //        cat 1_1_2 -> null
        //    cat 1_2 -> null
        //        cat 1_2_1 -> null
        //        cat 1_2_2 -> null
        // cat 2 -> null
        //    cat 2_1 -> null
        //        cat 2_1_1 -> season 5
        //        cat 2_1_2 -> null
        var today = LocalDate.now();
        var creationTime = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        seasonRepository.save(new Season(4L, "season 4", creationTime));
        seasonRepository.save(new Season(5L, "season 5", creationTime));

        categorySettingsRepository.save(new CategorySettings(1L, 4L, creationTime));
        categorySettingsRepository.save(new CategorySettings(1_1_1L, 5L, creationTime));
        categorySettingsRepository.save(new CategorySettings(2_1_1L, 5L, creationTime));

        //act
        var catSeasons = dsl
            .selectFrom(V_CATEGORIES_FOR_LIFECYCLE)
            .fetchMap(
                V_CATEGORIES_FOR_LIFECYCLE.CATEGORY_ID,
                VCategoriesForLifecycle.class
            );

        //assert
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(catSeasons.get(1L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(1_1L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(1_1_1L).getSeasonId()).isEqualTo(5L);
            s.assertThat(catSeasons.get(1_1_2L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(1_2L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(1_2_1L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(1_2_2L).getSeasonId()).isEqualTo(4L);
            s.assertThat(catSeasons.get(2L).getSeasonId()).isNull();
            s.assertThat(catSeasons.get(2_1L).getSeasonId()).isNull();
            s.assertThat(catSeasons.get(2_1_1L).getSeasonId()).isEqualTo(5L);
            s.assertThat(catSeasons.get(2_1_2L).getSeasonId()).isNull();
        });
    }

    private Category category(long id, Category... children) {
        Category category = new Category()
            .setName(String.valueOf(id))
            .setCategoryId(id)
            .setParentCategoryId(-1L)
            .setPublished(true)
            .setParameterValues(List.of());
        for (Category child : children) {
            child.setParentCategoryId(id);
        }
        categories.add(category);
        return category;
    }
}
