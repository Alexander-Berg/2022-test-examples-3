package ru.yandex.market.mboc.common.services.modelstorage;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.randomizers.ModelRandomizer;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

import static java.util.Collections.emptySet;

/**
 * @author s-ermakov
 */
public class ModelConverterTest {
    private static final long SEED = "MBO-15933".hashCode();

    private ModelRandomizer modelRandomizer;

    @Before
    public void setUp() throws Exception {
        modelRandomizer = new ModelRandomizer(SEED);
    }

    @Test
    public void testDoubleConvention() {
        for (int i = 0; i < 100; i++) {
            Model model = modelRandomizer.getRandomValue();
            if (model.isGuruOrPartner()) {
                model.setSkuParentModelId(model.getId());
            } else if (model.isFastSku()) {
                model.setSkuParentModelId(0);
            }

            ModelStorage.Model proto = ModelConverter.reverseConvert(model);
            Model converted = ModelConverter.convert(proto, emptySet(), emptySet());

            // Don't compare quality and parameterValues
            converted.setModelQuality(model.getModelQuality());
            converted.setCategoryQuality(model.getCategoryQuality());
            converted.setParameterValues(model.getParameterValues());
            Assertions.assertThat(converted)
                .usingRecursiveComparison()
                .isEqualTo(model);
        }
    }

    @Test
    public void testVariousSkuTypes() {
        Model model = modelRandomizer.getRandomValue();

        // MSKU
        assertSkuIsConvertedCorrectly(model, Model.ModelType.SKU, Model.ModelQuality.OPERATOR, null,
            Model.ModelType.SKU, Model.ModelType.SKU);

        // PSKU1.0
        assertSkuIsConvertedCorrectly(model, Model.ModelType.PARTNER_SKU, Model.ModelQuality.PARTNER, null,
            Model.ModelType.PARTNER_SKU, Model.ModelType.PARTNER_SKU);

        // PSKU2.0
        assertSkuIsConvertedCorrectly(model, Model.ModelType.SKU, Model.ModelQuality.PARTNER, null,
            Model.ModelType.SKU, Model.ModelType.PARTNER_SKU);

        // FAST SKU
        assertSkuIsConvertedCorrectly(model, Model.ModelType.FAST_SKU, Model.ModelQuality.PARTNER, null,
            Model.ModelType.FAST_SKU, Model.ModelType.PARTNER_SKU);
    }

    private void assertSkuIsConvertedCorrectly(Model model, Model.ModelType modelType,
                                               Model.ModelQuality modelQuality,
                                               SimpleModel.CategoryQuality categoryQuality,
                                               Model.ModelType protoCurrentType,
                                               Model.ModelType protoSourceType) {
        model.setModelType(modelType);
        model.setModelQuality(modelQuality);
        model.setCategoryQuality(categoryQuality);
        //No parent model for fast sku
        if (model.isFastSku()) {
            model.setSkuParentModelId(0);
        }
        //converter is setting relations based on model type, so needs to be used after type is set
        ModelStorage.Model.Builder proto = ModelConverter.reverseConvert(model).toBuilder();
        proto.setCurrentType(protoCurrentType.name());
        proto.setSourceType(protoSourceType.name());
        proto.addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(1L)
            .setXslName(XslNames.MODEL_QUALITY)
            .setOptionId((int) modelQuality.getOptionId())
            .build());
        Model converted = ModelConverter.convert(proto.build(), emptySet(), emptySet());
        converted.setParameterValues(model.getParameterValues());
        Assertions.assertThat(converted)
            .usingRecursiveComparison()
            .isEqualTo(model);
    }
}
