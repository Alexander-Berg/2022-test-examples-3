package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicNumber")
public class NewModePreprocessorTest extends BasePreprocessorTest {
    private NewModelPreprocessor newModelPreprocessor;

    @Before
    public void before() {
        super.before();
        newModelPreprocessor = new NewModelPreprocessor();
    }

    @Test
    public void testNewModelIdChanged() {
        CommonModel model = model(1);
        CommonModel model1 = model(0);
        CommonModel model2 = model(0);
        CommonModel model3 = model(0);
        CommonModel sku1 = sku(0, model);
        CommonModel sku2 = sku(0, model);
        CommonModel sku3 = sku(0, model);
        CommonModel sku4 = sku(0, model);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(ImmutableList.of(model, model1, model2, model3,
            sku1, sku2, sku3, sku4));
        modelSaveGroup.addBeforeModels(ImmutableList.of(model));
        newModelPreprocessor.preprocess(modelSaveGroup, modelSaveContext);
        List<Long> newModelIds = Arrays.asList(-1L, -2L, -3L, -4L, -5L, -6L, -7L);
        assertThat(newModelIds)
            .containsExactlyInAnyOrder(model1.getId(), model2.getId(), model3.getId(), sku1.getId(),
                sku2.getId(), sku3.getId(), sku4.getId());
    }
    @Test
    public void testNewModelIdChanged2() {
        CommonModel model = model(1);
        CommonModel model1 = model(0);
        CommonModel model2 = model(0);
        CommonModel model3 = model(-8);
        CommonModel sku1 = sku(0, model);
        CommonModel sku2 = sku(0, model);
        CommonModel sku3 = sku(0, model);
        CommonModel sku4 = sku(0, model);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(ImmutableList.of(model, model1, model2, model3,
            sku1, sku2, sku3, sku4));
        modelSaveGroup.addBeforeModels(ImmutableList.of(model));
        newModelPreprocessor.preprocess(modelSaveGroup, modelSaveContext);
        List<Long> newModelIds = Arrays.asList(-8L, -9L, -10L, -11L, -12L, -13L, -14L);
        assertThat(newModelIds)
            .containsExactlyInAnyOrder(model1.getId(), model2.getId(), model3.getId(), sku1.getId(),
                sku2.getId(), sku3.getId(), sku4.getId());
    }
}
