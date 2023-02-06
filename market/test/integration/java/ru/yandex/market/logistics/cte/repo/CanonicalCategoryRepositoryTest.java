package ru.yandex.market.logistics.cte.repo;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistics.cte.base.IntegrationTest;
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter;
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter;
import ru.yandex.market.logistics.cte.entity.category.CanonicalCategoryEntity;

@ContextConfiguration(classes = {SupplyDtoToSupplyConverter.class, SupplyItemDtoToSupplyItemConverter.class})
class CanonicalCategoryRepositoryTest extends IntegrationTest {

    @Autowired
    CanonicalCategoryRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/canonical_category_before.xml")
    void getPathFromRoot_happyPath() {
        List<CanonicalCategoryEntity> pathFromRoot = repository.getPathFromRoot(111);

        assertions.assertThat(pathFromRoot).containsExactly(
                buildCategoryEntity(10, "Root", null),
                buildCategoryEntity(1, "Category 1", 10),
                buildCategoryEntity(11, "Category 1.1", 1),
                buildCategoryEntity(111, "Category 1.1.1", 11)
        );
    }

    @Test
    @DatabaseSetup("classpath:repository/canonical_category_before.xml")
    void getPathFromRoot_notFound() {
        List<CanonicalCategoryEntity> pathFromRoot = repository.getPathFromRoot(10000);

        assertions.assertThat(pathFromRoot).isEmpty();
    }

    private CanonicalCategoryEntity buildCategoryEntity(int id, String name, Integer parentId) {
        var entity = new CanonicalCategoryEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setParentId(parentId);

        return entity;
    }
}
