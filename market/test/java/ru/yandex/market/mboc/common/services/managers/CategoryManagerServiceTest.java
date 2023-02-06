package ru.yandex.market.mboc.common.services.managers;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryManagerServiceTest {
    private static final MboUser USER_1 = new MboUser(14, "Сережка", "s-ermakov");
    private static final MboUser USER_2 = new MboUser(200, "User", "user");

    private ManagersService service;

    @Before
    public void setUp() throws Exception {
        MboUsersRepositoryMock mboUsersRepository = new MboUsersRepositoryMock();
        mboUsersRepository.insert(USER_1);
        mboUsersRepository.insert(USER_2);

        CategoryInfoRepositoryMock categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepository);
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(1).setContentManagerUid(14L));
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(2).setInputManagerUid(200L));

        service = new ManagersServiceImpl(mboUsersRepository, categoryInfoRepository);
    }

    @Test
    public void testIfAllOk() {
        Optional<MboUser> inputManager = service.getInputManager(2);
        Assertions.assertThat(inputManager)
            .isPresent()
            .get()
            .isEqualTo(USER_2);
    }

    @Test
    public void testGetInputManagerIfCategoryNotExist() {
        Optional<MboUser> inputManager = service.getInputManager(100);
        Assertions.assertThat(inputManager)
            .isNotPresent();
    }

    @Test
    public void testGetInputManagerIfManagerNotExist() {
        Optional<MboUser> inputManager = service.getInputManager(1);
        Assertions.assertThat(inputManager)
            .isNotPresent();
    }

    @Test
    public void testGetInputManagersIfManagerNotExist() {
        Map<Long, MboUser> inputManagersByCategory = service.getInputManagersByCategory(Arrays.asList(1L, 2L, 100L));
        Assertions.assertThat(inputManagersByCategory).containsOnlyKeys(2L);
        Assertions.assertThat(inputManagersByCategory).containsValue(USER_2);
    }
}
