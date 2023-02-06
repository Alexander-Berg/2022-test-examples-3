package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.generalization.ModelGroupGeneralizationServiceTest;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.tovartree.NameToAliasesSettings;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты проверяют корректную связь между результатом генерализации и методом сохранения модели.
 * Проверка корректности алгоритма генерализации осуществляется в {@link ModelGroupGeneralizationServiceTest}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ModelGeneralizationStorageTest extends BaseGroupStorageUpdatesTest {

    private static final CategoryParam CATEGORY_PARAM = CategoryParamBuilder
        .newBuilder(10, "param1", Param.Type.NUMERIC)
        .build();

    @Override
    public void before() {
        super.before();
        CommonModel parent = createGuruModel(1);
        CommonModel child1 = createGuruModel(2, b -> b.parentModelId(1));
        CommonModel child2 = createGuruModel(3, b -> b.parentModelId(1));
        parent.putParameterValues(new ParameterValues(CATEGORY_PARAM, BigDecimal.TEN));
        child1.putParameterValues(new ParameterValues(CATEGORY_PARAM, BigDecimal.TEN));

        super.putToStorage(parent, child1, child2);

        guruService.addCategory(parent.getCategoryId(), 101L, true);
    }

    /**
     * Тест проверяет, что отгенерализованные модели будут корректно добавлены в группу:
     * - Еще не содержащиеся модели попадут в additional
     * - Содержащиеся обновятся в required.
     */
    @Test
    public void testCorrectSplitOfGeneralizedModels() {
        // фабрикуем так новую модель, чтобы она поменяло свое значение
        CommonModel newChild = createGuruModel(0, b -> b.parentModelId(1));
        newChild.putParameterValues(new ParameterValues(CATEGORY_PARAM, BigDecimal.TEN));

        guruService.addNameToAliasesSettingByCategory(1, NameToAliasesSettings.COPY_NAME_TO_ALIAS);
        context.setSource(AuditAction.Source.MBO);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(newChild);
        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        Assert.assertEquals(OperationStatusType.OK, groupOperationStatus.getStatus());

        assertThat(groupOperationStatus.getRequestedModelIds()).hasSize(1);
        assertThat(groupOperationStatus.getAdditionalModelStatues()).hasSize(2);
        Assert.assertEquals(1, groupOperationStatus.getRequestedModelIds().size());
        assertThat(modelSaveGroup.getAdditionalModels().stream()
                .map(CommonModel::getId)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(1L, 2L, 3L);
    }
}
