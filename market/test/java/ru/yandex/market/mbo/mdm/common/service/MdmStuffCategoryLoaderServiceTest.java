package ru.yandex.market.mbo.mdm.common.service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Files;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.service.stuff.MdmStuffService;
import ru.yandex.market.mbo.mdm.common.service.stuff.StuffServiceBaseTestClass;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class MdmStuffCategoryLoaderServiceTest extends StuffServiceBaseTestClass {

    private static final int ROOT_ID = 90401;
    private static final long CATEGORY_ID = 6290271;
    private static final long PARENT_CATEGORY_ID = 6091783;

    private MdmStuffCategoryLoaderService service;

    @Before
    public void setup() {
        service = new MdmStuffCategoryLoaderService(Executors.newSingleThreadScheduledExecutor(),
            complexMonitoring, tempFolder.getRoot().getAbsolutePath(), 2, true);
    }

    @Test
    public void testItLoads() {
        copyFromResources("export/tovar-tree.pb", "tovar-tree.pb");

        assertFalse(service.isReady());
        service.init();
        assertTrue(service.isReady());

        assertNull(getCategory(-42));
        Category category = getCategory(CATEGORY_ID);
        Category parent = getCategory(PARENT_CATEGORY_ID);

        assertNotNull(category);
        assertNotNull(parent);

        Assertions.assertThat(category.getCategoryId()).isEqualTo(CATEGORY_ID);
        Assertions.assertThat(category.getParentCategoryId()).isEqualTo(PARENT_CATEGORY_ID);
        Assertions.assertThat(category.getName()).isEqualTo("BDSM атрибутика");

        Assertions.assertThat(parent.isLeaf()).isFalse();
        Assertions.assertThat(category.isLeaf()).isTrue();

        CategoryTree categoryTree = service.getCategoryTree();
        Assertions.assertThat(categoryTree.getAllCategoryIdsInTree(-42)).isEmpty();

        Collection<Long> categoryIds = categoryTree.getAllCategoryIdsInTree(CATEGORY_ID);
        Collection<Long> parentCategoryIds = categoryTree.getAllCategoryIdsInTree(PARENT_CATEGORY_ID);
        Collection<Long> rootCategoryIds = categoryTree.getAllCategoryIdsInTree(ROOT_ID);
        Assertions.assertThat(categoryIds).isNotEmpty();
        Assertions.assertThat(parentCategoryIds).isNotEmpty().containsAll(categoryIds);
        Assertions.assertThat(rootCategoryIds).isNotEmpty().containsAll(parentCategoryIds);

        Assertions.assertThat(getCategoryParameterValues(CATEGORY_ID)).isNotEmpty();
        Assertions.assertThat(getCategoryParameterValues(PARENT_CATEGORY_ID)).isNotEmpty();
        Assertions.assertThat(getCategoryParameterValues(ROOT_ID)).isNotEmpty();
    }

    @Test
    public void testUpdates() {
        service.init();
        assertNull(getCategory(CATEGORY_ID));
        assertFalse(service.checkForUpdate());

        copyFromResources("export/tovar-tree.pb", "tovar-tree.pb");

        // lastmodified can have second precision, so we have to touch and retouch it until it's actually changed
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Files.touch(tempFolder.getRoot());
            return service.checkForUpdate();
        });

        assertNotNull(getCategory(CATEGORY_ID));
        CategoryTree categoryTree = service.getCategoryTree();
        Assertions.assertThat(categoryTree.getAllCategoryIdsInTree(CATEGORY_ID)).containsExactlyInAnyOrder(6290271L);
        Assertions.assertThat(getCategoryParameterValues(CATEGORY_ID)).isNotEmpty();
    }

    @Test
    public void shouldNotReadStuffIfDisabled() {
        service = new MdmStuffCategoryLoaderService(Executors.newSingleThreadScheduledExecutor(),
            Mockito.mock(ComplexMonitoring.class), tempFolder.getRoot().getAbsolutePath(), 2, false);

        service.init();
        Assertions.assertThat(service.isEnabled()).isFalse();
        Assertions.assertThat(service.isReady()).isFalse();
        Assertions.assertThatThrownBy(() -> service.getAllCategories())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(MdmStuffService.DISABLED_MESSAGE);
        Assertions.assertThatThrownBy(() -> service.getCategoryTree())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(MdmStuffService.DISABLED_MESSAGE);
    }

    private Category getCategory(long categoryId) {
        return service.getAllCategories().stream()
            .filter(c -> c.getCategoryId() == categoryId)
            .findFirst()
            .orElse(null);
    }

    private Map<Long, MboParameters.ParameterValue> getCategoryParameterValues(long categoryId) {
        return service.getAllParameters().getOrDefault(categoryId, Map.of());
    }
}
