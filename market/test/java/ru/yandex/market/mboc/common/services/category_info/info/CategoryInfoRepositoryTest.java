package ru.yandex.market.mboc.common.services.category_info.info;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 17.07.2019
 */
public class CategoryInfoRepositoryTest extends BaseDbTestClass {

    private static final long CATEGORY_ID_1 = 1539818248;
    private static final long CATEGORY_ID_2 = 1681120427;
    private static final long SEED = 1088927734L;

    @Autowired
    private CategoryInfoRepository repository;

    @Autowired
    private MboUsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .build();
    }

    @Test
    public void insertManagers() {
        CategoryManagers managers = new CategoryManagers(CATEGORY_ID_1);
        managers.setContentManagerUid(creatUser().getUid());
        managers.setInputManagerUid(creatUser().getUid());

        repository.resetAndSaveAllCategoriesManagers(Collections.singleton(managers));

        CategoryInfo inserted = repository.findById(CATEGORY_ID_1);

        assertThat(inserted.getContentManagerUid()).isEqualTo(managers.getContentManagerUid());
        assertThat(inserted.getInputManagerUid()).isEqualTo(managers.getInputManagerUid());
    }

    @Test
    public void updateResetsAllOtherManagers() {
        CategoryManagers first = new CategoryManagers(CATEGORY_ID_1);
        first.setContentManagerUid(creatUser().getUid());
        first.setInputManagerUid(creatUser().getUid());

        repository.resetAndSaveAllCategoriesManagers(Collections.singleton(first));

        CategoryManagers second = new CategoryManagers(CATEGORY_ID_2);
        second.setContentManagerUid(creatUser().getUid());
        second.setInputManagerUid(creatUser().getUid());

        repository.resetAndSaveAllCategoriesManagers(Collections.singleton(second));

        CategoryInfo firstUpdated = repository.findById(first.getCategoryId());

        assertThat(firstUpdated.getInputManagerUid()).isNull();
        assertThat(firstUpdated.getContentManagerUid()).isNull();
    }

    @Test
    public void updateManagersDoesntAffectAnotherFields() {
        long idWithFalse = CATEGORY_ID_1;
        long idWithTrue = CATEGORY_ID_2;

        repository.insert(new CategoryInfo(idWithFalse).setManualAcceptance(false));
        repository.insert(new CategoryInfo(idWithTrue).setManualAcceptance(true));

        List<CategoryManagers> managers = Stream.of(idWithFalse, idWithTrue)
            .map(id -> new CategoryManagers(id)
                .setInputManagerUid(creatUser().getUid())
                .setContentManagerUid(creatUser().getUid())
            )
            .collect(Collectors.toList());

        repository.resetAndSaveAllCategoriesManagers(managers);

        CategoryInfo withFalse = repository.findById(idWithFalse);
        assertThat(withFalse.isManualAcceptance()).isFalse();

        CategoryInfo withTrue = repository.findById(idWithTrue);
        assertThat(withTrue.isManualAcceptance()).isTrue();

        // check actually update
        for (CategoryManagers manager : managers) {
            CategoryInfo categoryInfo = repository.findById(manager.getCategoryId());

            assertThat(categoryInfo.getContentManagerUid()).isEqualTo(manager.getContentManagerUid());
            assertThat(categoryInfo.getInputManagerUid()).isEqualTo(manager.getInputManagerUid());
        }
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void addCategoryWithNoManagers() {
        long catId = 123456789L;
        Category cat = new Category().setCategoryId(catId).setName("cat").setPublished(true);
        categoryRepository.insert(cat);

        repository.resetAndSaveAllCategoriesManagers(Collections.emptyList());

        List<CategoryInfo> all = repository.findAll();
        assertThat(all.size()).isEqualTo(1);
        CategoryInfo categoryInfo = all.get(0);
        assertThat(categoryInfo.getCategoryId()).isEqualTo(catId);
        assertThat(categoryInfo.getContentManagerUid()).isNull();
        assertThat(categoryInfo.getInputManagerUid()).isNull();
        assertThat(categoryInfo.isManualAcceptance()).isFalse();
        assertThat(categoryInfo.isModerationInYang()).isTrue();
    }

    private MboUser creatUser() {
        MboUser mboUser = random.nextObject(MboUser.class);
        usersRepository.insertOrUpdate(mboUser);
        return mboUser;
    }
}
