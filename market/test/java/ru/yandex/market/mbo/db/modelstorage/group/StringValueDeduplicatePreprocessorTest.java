package ru.yandex.market.mbo.db.modelstorage.group;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueUtils;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.List;

/**
 * @author shadoff
 * created on 7/3/20
 */
public class StringValueDeduplicatePreprocessorTest extends BaseGroupStorageUpdatesTest {
    private static final long CATEGORY1 = 1L;
    private static final long VENDOR1 = 1L;
    private static final long MODEL1 = 1L;
    private static final long MODEL2 = 2L;
    private static final long MODEL3 = 3L;

    @Test
    public void aliasDeduplicateTest() {
        CommonModel model1 = CommonModelBuilder.newBuilder(MODEL1, CATEGORY1, VENDOR1)
            .startParameterValue()
            .paramId(1L).xslName(XslNames.ALIASES).type(Param.Type.STRING)
            .words("алиас 1", "алиас 1", "алиас 2", "алиас 1")
            .endParameterValue()
            .getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(MODEL2, CATEGORY1, VENDOR1)
            .startParameterValue()
            .paramId(2L).xslName(XslNames.SEARCH_ALIASES).type(Param.Type.STRING)
            .words("алиас 3", "алиас 3", "алиас 2", "алиас 3")
            .endParameterValue()
            .getModel();

        CommonModel model3 = CommonModelBuilder.newBuilder(MODEL3, CATEGORY1, VENDOR1)
            .startParameterValue()
            .paramId(1L).xslName(XslNames.ALIASES).type(Param.Type.STRING)
            .words("алиас 1", "алиас 1", "алиас 2", "алиас 1")
            .endParameterValue()
            .startParameterValue()
            .paramId(2L).xslName(XslNames.SEARCH_ALIASES).type(Param.Type.STRING)
            .words("алиас 3", "алиас 3", "алиас 2", "алиас 3")
            .endParameterValue()
            .getModel();

        putToStorage(model1, model2, model3);

        // ничего не меняем и сохраняем
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1, model2, model3);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        CommonModel model1saved = groupOperationStatus.getRequestedModelStatuses().get(0).getModel();
        List<String> model1AliasValues = ParameterValueUtils
            .getStringValues(model1saved.getSingleParameterValue(XslNames.ALIASES));
        Assertions.assertThat(model1AliasValues).containsExactlyInAnyOrder("алиас 1", "алиас 2");

        CommonModel model2saved = groupOperationStatus.getRequestedModelStatuses().get(1).getModel();
        List<String> model2SearchAliasValues = ParameterValueUtils
            .getStringValues(model2saved.getSingleParameterValue(XslNames.SEARCH_ALIASES));
        Assertions.assertThat(model2SearchAliasValues).containsExactlyInAnyOrder("алиас 3", "алиас 2");

        CommonModel model3saved = groupOperationStatus.getRequestedModelStatuses().get(2).getModel();
        List<String> model3AliasValues = ParameterValueUtils
            .getStringValues(model3saved.getSingleParameterValue(XslNames.ALIASES));
        Assertions.assertThat(model3AliasValues).containsExactlyInAnyOrder("алиас 1", "алиас 2");
        List<String> model3SearchAliasValues = ParameterValueUtils
            .getStringValues(model3saved.getSingleParameterValue(XslNames.SEARCH_ALIASES));
        Assertions.assertThat(model3SearchAliasValues).containsExactlyInAnyOrder("алиас 3", "алиас 2");
    }
}
