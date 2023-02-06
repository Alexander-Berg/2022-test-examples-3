package ru.yandex.market.mboc.app.managers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.app.managers.web.DisplayManagerCategory;
import ru.yandex.market.mboc.app.managers.web.UpdateAcceptanceStatusRequest;
import ru.yandex.market.mboc.app.managers.web.UpdateManagerCategoryRequest;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.exceptions.BadUserRequestException;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.ManagerCategory;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingService;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 20.03.19
 */
public class ManagerCategoriesControllerTest extends BaseMbocAppTest {
    @Autowired
    private CategoryManagerRepository categoryManagerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Resource
    private CatteamRepository catteamRepository;

    private ManagerCategoriesController controller;
    private CategoryCachingServiceMock categoryCachingService;
    private StaffServiceMock staffService;
    private CategoryManagerServiceImpl managersService;

    @Before
    public void setup() {
        categoryCachingService = new CategoryCachingServiceMock();
        UserCachingService userCachingService = new UserCachingServiceImpl(userRepository);
        staffService = new StaffServiceMock();
        managersService = new CategoryManagerServiceImpl(categoryCachingService, categoryManagerRepository,
            userCachingService, transactionHelper, staffService, namedParameterJdbcTemplate, categoryInfoRepository,
            catteamRepository);
        controller = new ManagerCategoriesController(managersService, categoryCachingService,
            userCachingService, "http://mbo");

        categoryInfoRepository.deleteAll();

        categoryInfoRepository.insert(infoWithId(1L));
        categoryInfoRepository.insert(infoWithId(2L));
    }

    private CategoryInfo infoWithId(long id) {
        var info = new CategoryInfo();
        info.setCategoryId(id);
        return info;
    }


    @Test
    public void testDisplayCategoriesOutput() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryCachingService.addCategory(2, "Cat #1-2", 1);
        categoryManagerRepository.storeManagerCategories(Collections.singletonList(new ManagerCategory("test", 2,
            ManagerRole.OTHER)));

        List<DisplayManagerCategory> categories = controller.all();
        assertThat(categories).hasSize(1); // Only one with manager is displayed now
        assertThat(categories.get(0).getCategoryId()).isEqualTo(2);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("Cat #1-2");
        assertThat(categories.get(0).getCategoryUrl()).isEqualTo("http://mbo/gwt/#tovarTree/hyperId=2");
        assertThat(categories.get(0).getLogin()).isEqualTo("test");
    }

    @Test
    public void testCatmanLoginValidation() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryCachingService.addCategory(2, "Cat #1-2", 1);
        staffService.addApiUser("test_user");

        controller.update(List.of(new UpdateManagerCategoryRequest()
            .setCategoryId(1)
            .setManagers(List.of(
                new UpdateManagerCategoryRequest.ManagerCategoryRole()
                    .setLogin("test_user")
                    .setRole(ManagerRole.CATMAN)
            ))
        ));
    }

    @Test(expected = BadUserRequestException.class)
    public void testCatmanLoginValidationException() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryCachingService.addCategory(2, "Cat #1-2", 1);

        controller.update(List.of(new UpdateManagerCategoryRequest()
            .setCategoryId(1)
            .setManagers(List.of(
                new UpdateManagerCategoryRequest.ManagerCategoryRole()
                    .setLogin("test_user")
                    .setRole(ManagerRole.CATMAN)
            ))
        ));
    }

    @Test
    public void testSaveCatteam() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryCachingService.addCategory(2, "Cat #1-2", 1);

        controller.update(List.of(new UpdateManagerCategoryRequest()
            .setCategoryId(1)
            .setManagers(List.of())
            .setCatteam("test_catteam")
        ));

        assertThat(controller.all())
            .usingElementComparatorOnFields("categoryId", "catteam")
            .containsExactly(
                displayManagerCategory(1, "test_catteam")
            );
    }

    @Test
    public void testDeleteCatteam() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryCachingService.addCategory(2, "Cat #1-2", 1);

        controller.update(List.of(new UpdateManagerCategoryRequest()
            .setCategoryId(1)
            .setManagers(List.of())
            .setCatteam(null)
        ));

        assertThat(controller.all()).isEmpty();
    }

    @Test
    public void testSaveCatteamWithManagerBindings() {
        categoryCachingService.addCategory(1, "Cat #1", 0);
        categoryManagerRepository.updateManagerCategories(
            Map.of(1L, List.of(new ManagerCategory("test_login", 1, ManagerRole.CATMAN)))
        );
        staffService.addApiUser("test_login_222");

        controller.update(List.of(new UpdateManagerCategoryRequest()
            .setCategoryId(1)
            .setManagers(List.of(new UpdateManagerCategoryRequest.ManagerCategoryRole()
                .setLogin("test_login_222")
                .setRole(ManagerRole.CATMAN)
            ))
            .setCatteam("test_catteam_2")
        ));

        assertThat(controller.all())
            .usingElementComparatorIgnoringFields("userInfo")
            .containsExactly(new DisplayManagerCategory(
                1,
                new ManagerCategory("test_login_222", 1, ManagerRole.CATMAN),
                "Cat #1",
                "http://mbo/gwt/#tovarTree/hyperId=1",
                null,
                "test_catteam_2",
                infoWithId(1L)
            ));
    }

    @Test
    public void testUpdateCategoryAutoAcceptanceParameters() {
        categoryCachingService.addCategory(1, "cat1", 0);
        categoryManagerRepository.updateManagerCategories(
            Map.of(1L, List.of(new ManagerCategory("test_login", 1, ManagerRole.CATMAN)))
        );
        staffService.addApiUser("test_login_222");
        categoryInfoRepository.insertOrUpdate(new CategoryInfo().setCategoryId(1L));

        controller.updateAcceptance(List.of(
            new UpdateAcceptanceStatusRequest()
                .setCategoryId(1L)
                .setFbyAcceptanceMode("AUTO_ACCEPT")
                .setFbyPlusAcceptanceMode("MANUAL")
                .setFbsAcceptanceMode("AUTO_REJECT")
                .setExpressAcceptanceMode("AUTO_REJECT")
        ));

        var info = categoryInfoRepository.findById(1L);
        assertThat(info.getFbyAcceptanceMode()).isEqualTo(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        assertThat(info.getFbyPlusAcceptanceMode()).isEqualTo(CategoryInfo.AcceptanceMode.MANUAL);
        assertThat(info.getFbsAcceptanceMode()).isEqualTo(CategoryInfo.AcceptanceMode.AUTO_REJECT);
        assertThat(info.getExpressAcceptanceMode()).isEqualTo(CategoryInfo.AcceptanceMode.AUTO_REJECT);
    }

    private DisplayManagerCategory displayManagerCategory(long categoryId, String catteam) {
        return new DisplayManagerCategory(categoryId, null,
            null, null, null, catteam, new CategoryInfo());
    }

}
