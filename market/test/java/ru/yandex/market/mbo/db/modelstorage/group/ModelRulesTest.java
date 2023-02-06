package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelRulesExecutorService;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType.SKU_MODEL;

/**
 * @author york
 * @since 03.11.2020
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelRulesTest extends BaseGroupStorageUpdatesTest {
    private List<Long> rulesAppliedIds = new ArrayList<>();
    private CommonModel root1;
    private CommonModel child10;
    private CommonModel child11;
    private CommonModel sku12;
    private CommonModel root2;
    private CommonModel root3;
    private CommonModel child20;
    private CommonModel sku20;
    private Long sku20Id = 22L;
    private Long sku12Id = 12L;
    private Long categoryId = 1L;

    @Before
    public void before() {
        super.before();
        root1 = createGuruModel(1);
        child10 = createGuruModel(10, b -> b.parentModelId(root1.getId()));
        child11 = createGuruModel(11, b -> b.parentModelId(root1.getId())
            .modelRelation(sku12Id, categoryId, SKU_MODEL));
        sku12 = createSku(sku12Id, categoryId, child11.getId());
        root2 = createGuruModel(2, b -> b.modelRelation(sku20Id, categoryId, SKU_MODEL));
        root3 = createGuruModel(3);
        child20 = createGuruModel(20, b -> b.parentModelId(root2.getId()));
        sku20 = createSku(sku20Id, categoryId, root2.getId());
        super.putToStorage(root1, child10, child11, sku12, root2, child20, sku20, root3);
    }

    @Override
    protected ModelRulesExecutorService createRuleService() {
        return model -> {
            rulesAppliedIds.add(model.getId());
            return true;
        };
    }

    @Test
    public void testModelsThenModifications() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(child10, root3);
        storage.saveModels(saveGroup, context);
        assertThat(rulesAppliedIds)
            .containsExactlyInAnyOrder(root1.getId(), root3.getId(), child10.getId(), child11.getId(), sku12.getId());
        assertThat(rulesAppliedIds.size()).isEqualTo(5);
    }

    @Test
    public void testModelsThenSKU() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(root3, sku12, sku20, root2);
        storage.saveModels(saveGroup, context);
        assertThat(rulesAppliedIds)
            .containsExactlyInAnyOrder(root1.getId(), root3.getId(), child10.getId(), child11.getId(),
                child20.getId(), sku20.getId(), sku12.getId(), root2.getId());
        assertThat(rulesAppliedIds.size()).isEqualTo(8);
    }

    @Test
    public void testModelsThen3LevelNesting() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(root1);
        storage.saveModels(saveGroup, context);
        assertThat(rulesAppliedIds)
            .containsExactlyInAnyOrder(root1.getId(), child11.getId(), child10.getId(), sku12.getId());
        assertThat(rulesAppliedIds.size()).isEqualTo(4);
    }
}
