package ru.yandex.market.mboc.monetization.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.GroupingCategory;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerService;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.ManagerCategory;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.ConfigParameterRepository;
import ru.yandex.market.mboc.monetization.repository.ConfigValidationErrorRepository;
import ru.yandex.market.mboc.monetization.repository.GroupingConfigRepository;

import static org.junit.Assert.assertThat;


@SuppressWarnings("checkstyle:magicNumber")
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupingCategoryServiceImplTest extends BaseDbTestClass {

    @Autowired
    private GroupingConfigRepository groupingConfigRepository;
    @Resource
    private CategoryManagerRepository categoryManagerRepository;
    @Autowired
    private ConfigParameterRepository configParameterRepository;
    @Autowired
    private ConfigValidationErrorRepository configValidationErrorRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CatteamRepository catteamRepository;

    private GroupingCategoryService groupingCategoryService;

    private CategoryManagerService categoryManagerService;
    private CategoryCachingServiceMock categoryCachingService;

    private static GroupingCategory getGroupingCategory(
        long categoryId,
        long parentCategoryId,
        String categoryName,
        String department,
        boolean leaf,
        List<String> categoryManagerLogin
    ) {
        return new GroupingCategory(
            getCategory(
                categoryId,
                parentCategoryId,
                categoryName,
                leaf
            ),
            department,
            categoryManagerLogin,
            Collections.emptyList()
        );
    }

    private static Category getCategory(long categoryId, long parentCategoryId, String categoryName, boolean leaf) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setParentCategoryId(parentCategoryId);
        category.setName(categoryName);
        category.setLeaf(leaf);
        return category;
    }

    @Before
    public void setUp() {
        categoryCachingService = new CategoryCachingServiceMock();
        categoryManagerService = new CategoryManagerServiceImpl(categoryCachingService, categoryManagerRepository,
            new UserCachingServiceImpl(userRepository), transactionHelper, new StaffServiceMock().setAllExists(true),
            namedParameterJdbcTemplate, categoryInfoRepository, catteamRepository);

        groupingCategoryService = new GroupingCategoryServiceImpl(
            categoryCachingService,
            groupingConfigRepository,
            categoryManagerService,
            new GroupingConfigServiceImpl(
                groupingConfigRepository,
                configParameterRepository,
                configValidationErrorRepository
            )
        );
    }

    @Test
    public void testGroupingCategoryService() {
        categoryManagerService.updateManagersToCategories(Arrays.asList(
            new ManagerCategory("login1", 1, ManagerRole.CATDIR),
            new ManagerCategory("login2", 1, ManagerRole.CATMAN),
            new ManagerCategory("login3", 2, ManagerRole.CATMAN),
            new ManagerCategory("login3", 3, ManagerRole.CATDIR),
            new ManagerCategory("login4", 3, ManagerRole.CATMAN)
        ));

        categoryCachingService.addCategory(getCategory(90401, -1, "Все товары", false));
        categoryCachingService.addCategory(getCategory(1, 90401, "Электроника", false));
        categoryCachingService.addCategory(getCategory(2, 90401, "Бытовая техника", true));
        categoryCachingService.addCategory(getCategory(3, 1, "Ноутбуки", true));

        List<GroupingCategory> groupingCategories = groupingCategoryService.find(new GroupingCategoryFilter());

        assertThat(
            groupingCategories,
            Matchers.containsInAnyOrder(
                getGroupingCategory(2, 90401, "Бытовая техника", "Бытовая техника", true, List.of("login3")),
                getGroupingCategory(3, 1, "Ноутбуки", "Электроника", true, List.of("login3", "login4")))
        );
    }
}
