package ru.yandex.market.logistics.cte.service;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistics.cte.base.IntegrationTest;
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter;
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter;
import ru.yandex.market.logistics.cte.entity.category.CanonicalCategoryEntity;
import ru.yandex.market.logistics.cte.repo.CanonicalCategoryExternalRepository;
import ru.yandex.market.logistics.cte.repo.CanonicalCategoryFileRepository;
import ru.yandex.market.logistics.cte.service.impl.RepositoryCategoryService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@ContextConfiguration(classes = {SupplyDtoToSupplyConverter.class, SupplyItemDtoToSupplyItemConverter.class})
public class CanonicalCategoryImportServiceTest extends IntegrationTest {

    private CanonicalCategoryImportService canonicalCategoryImportService;
    @Autowired
    private RepositoryCategoryService categoryService;
    private CanonicalCategoryExternalRepository canonicalCategoryExternalRepository;

    @BeforeEach
    public void init() {
        canonicalCategoryExternalRepository = Mockito.mock(CanonicalCategoryFileRepository.class);
        canonicalCategoryImportService =
                new CanonicalCategoryImportService(categoryService, canonicalCategoryExternalRepository);
    }

    /**
     * Обновляем дерево категорий на новое. При этом происходит следующее:
     * <ul>
     *     <li>Добавляется новая категория</li>
     *     <li>Изменяется родитель у одной из существующих категорий</li>
     *     <li>Изменяется название у одной из существующих категорий</li>
     *     <li>Изменяется id у одной из категорий, при этом parent_id и name не изменяются</li>
     *     <li>Часть категорий не заданы в новом файле, но остаются в БД</li>
     * </ul>
     */
    @Test
    @DatabaseSetup("classpath:repository/qattribute.xml")
    @DatabaseSetup("classpath:repository/group.xml")
    @DatabaseSetup("classpath:service/canonical_category_before.xml")
    @ExpectedDatabase(
            value = "classpath:service/canonical_category_after_update.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateCategoryTree() {
        Mockito.when(canonicalCategoryExternalRepository.loadAll()).thenReturn(
                Map.of(
                        2, buildCategoryEntity(2, "Продукты любые", 1),
                        3, buildCategoryEntity(3, "Безалкогольное пиво и вино", 1),
                        9, buildCategoryEntity(9, "Телевизоры", 7),
                        10, buildCategoryEntity(10, "Техника для красоты", 7)
                )
        );
        canonicalCategoryImportService.updateFromExternalSource();
    }

    private CanonicalCategoryEntity buildCategoryEntity(int id, String name, int parentId) {
        var entity = new CanonicalCategoryEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setParentId(parentId);

        return entity;
    }
}
