package ru.yandex.market.api.controller.v2.category;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.category.CategoriesSort;
import ru.yandex.market.api.category.CategoryUtils;
import ru.yandex.market.api.category.FilterService;
import ru.yandex.market.api.category.FilterSetType;
import ru.yandex.market.api.controller.v2.CategoryControllerV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.CategoryV2;
import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.domain.v2.FiltersResult;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.option.Statistic;
import ru.yandex.market.api.domain.v2.toloka.TolokaCategories;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.matchers.CategoryMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.YandexUid;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.FiltersMatcher.filter;
import static ru.yandex.market.api.matchers.FiltersMatcher.id;
import static ru.yandex.market.api.matchers.FiltersMatcher.name;
import static ru.yandex.market.api.matchers.FiltersMatcher.type;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.option;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.optionHow;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.optionId;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.options;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.sort;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.text;
import static ru.yandex.market.api.matchers.StatisticMatcher.count;
import static ru.yandex.market.api.matchers.StatisticMatcher.cpaCount;
import static ru.yandex.market.api.matchers.StatisticMatcher.modelCount;
import static ru.yandex.market.api.matchers.StatisticMatcher.offerCount;
import static ru.yandex.market.api.matchers.StatisticMatcher.statistic;

public class CategoryControllerV2Test extends BaseTest {
    @Inject
    private CategoryControllerV2 categoryController;

    @Inject
    private ReportTestClient reportTestClient;

    private static final int CATEGORY_ID = 91491;

    @Test
    public void shouldQueryToPrimeLite_notNeedSortsAndStatistics_version2_1_3() {
        reportTestClient.categorySearchLite(CATEGORY_ID, "category-91491-prime-lite.json");

        FiltersResult filtersResult = filtersResult(
            CATEGORY_ID,
            Collections.emptyList()
        );

        assertThat(
            filtersResult.getFilters(),
            hasItems(
                filter(
                    id("4940921"),
                    type("ENUM"),
                    name("Тип")
                ),
                filter(
                    id("12782797"),
                    type("ENUM"),
                    name("Линейка")
                )
            )
        );
    }

    @Test
    public void shouldQueryToPrimeIfNeedSorts() {
        reportTestClient.categorySearch(IntLists.singleton(CATEGORY_ID), "category-91491-prime.json");

        FiltersResult filtersResult = filtersResult(
            CATEGORY_ID,
            Collections.singleton(FilterField.SORTS)
        );

        assertThat(
            filtersResult.getFilters(),
            hasItems(
                filter(
                    id("4940921"),
                    type("ENUM"),
                    name("Тип")
                )
                ,
                filter(
                    id("12782797"),
                    type("ENUM"),
                    name("Линейка")
                )
            )
        );

        assertThat(
            filtersResult.getSorts(),
            hasItems(
                sort(
                    text("по популярности")
                ),
                sort(
                    text("по цене"),
                    options(
                        option(
                            optionId("aprice"),
                            optionHow(SortOrder.ASC)
                        ),
                        option(
                            optionId("dprice"),
                            optionHow(SortOrder.DESC)
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldQueryToPrimeIfNeedStatistics() {
        reportTestClient.categorySearch(IntLists.singleton(CATEGORY_ID), "category-91491-prime.json");

        FiltersResult filtersResult = filtersResult(
            CATEGORY_ID,
            Collections.singleton(FilterField.STATISTICS)
        );

        assertThat(
            filtersResult.getFilters(),
            hasItems(
                filter(
                    id("4940921"),
                    type("ENUM"),
                    name("Тип")
                )
                ,
                filter(
                    id("12782797"),
                    type("ENUM"),
                    name("Линейка")
                )
            )
        );

        assertThat(
            getStatistic(filtersResult),
            statistic(
                count(42868),
                modelCount(9299),
                offerCount(33569),
                cpaCount(10616)
            )
        );
    }

    @Test
    public void categoriesChildrenWithPaging() {
        List<CategoryV2> categories = (List<CategoryV2>) categoryController.getChildrenCategories(
            CategoryUtils.ROOT_CATEGORY_ID,
            1,
            Collections.emptyList(),
            CategoriesSort.NONE,
            new PageInfo(1, 3)
        ).getCategories();

        Assert.assertThat(
            categories,
            Matchers.containsInAnyOrder(
                CategoryMatcher.id(90509),
                CategoryMatcher.id(90666),
                CategoryMatcher.id(91512)
            )
        );
    }

    @Test
    public void categoriesChildrenWithDepthTwo() {
        List<CategoryV2> categories = (List<CategoryV2>) categoryController.getChildrenCategories(
            CategoryUtils.ROOT_CATEGORY_ID,
            2,
            Collections.emptyList(),
            CategoriesSort.NONE,
            new PageInfo(1, 4)
        ).getCategories();

        Assert.assertThat(
            categories,
            Matchers.containsInAnyOrder(
                CategoryMatcher.id(90509),
                CategoryMatcher.id(91157),
                CategoryMatcher.id(10562873),
                CategoryMatcher.id(90530)
            )
        );
    }

    @Test
    public void categoriesAllChildren() {
        List<CategoryV2> categories = (List<CategoryV2>) categoryController.getChildrenCategories(
            CategoryUtils.ROOT_CATEGORY_ID,
            -1,
            Collections.emptyList(),
                CategoriesSort.NONE,
                PageInfo.ALL_ITEMS
        ).getCategories();

        Assert.assertThat(
                categories,
                Matchers.hasSize(2675)
        );
    }

    @Test
    public void categoriesToloka() {
        User user = new User(null, null, new Uuid("517040088"), new YandexUid("7432517151606405785"));
        ContextHolder.update(ctx -> {
            ctx.setUser(user);
            ctx.setPpList(IntLists.singleton(18));
        });

        reportTestClient.doRequest("dj_links", builder ->
                builder.param("dj-place", "market_thematics_prod")
                        .param("pp", "18")
                        .param("uuid", "517040088")
                        .param("yandexuid", "7432517151606405785"))
                .ok().body("categories-toloka.json");

        TolokaCategories categories = categoryController.searchToloka(
                "market_thematics_prod",
                PageInfo.ALL_ITEMS,
                GenericParams.DEFAULT
        ).waitResult().getResult();

        Assert.assertThat(
                categories.getCategories(),
                Matchers.hasSize(6)
        );
    }


    private FiltersResult filtersResult(int categoryId,
                                        Collection<? extends Field> fields) {
        return categoryController.getCategoryFilters(
                categoryId,
                fields,
                FilterSetType.POPULAR,
                FilterService.FilterSort.NONE,
                Collections.emptyMap(),
            null,
            false,
            false,
            null,
            null,
            genericParams,
            new ValidationErrors()
        ).waitResult();
    }

    private static Statistic getStatistic(FiltersResult filtersResult) {
        return ((Statistic) ((ResultContextV2) filtersResult.getContext()).getProcessingOptions());
    }
}
