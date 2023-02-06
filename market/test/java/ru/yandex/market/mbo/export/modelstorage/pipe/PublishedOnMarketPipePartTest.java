package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.helper.SizeChartExportHelper;
import ru.yandex.market.mbo.export.helper.SizeMeasureExportHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class PublishedOnMarketPipePartTest extends BasePipePartTestClass {

    @Test
    public void testGuru() {
        ModelStorage.Model model = createModel(true);

        ModelPipeContext ctx = process(model);

        checkPublished(ctx, model);
    }

    @Test
    public void testGuruDummy() {
        ModelStorage.Model model = createModel(true).toBuilder()
            .setCurrentType(CommonModel.Source.GURU_DUMMY.name())
            .build();

        ModelPipeContext ctx = process(model);

        checkPublished(ctx, model);
    }

    @Test
    public void testNotPublishedGuru() {
        ModelStorage.Model model = createModel(false);

        ModelPipeContext ctx = process(model);

        checkPublished(ctx);
    }

    @Test
    public void testGuruWithModification() {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modif = createModification(model, true);

        ModelPipeContext ctx = process(model, Collections.singletonList(modif));

        checkPublished(ctx, model, modif);
    }

    @Test
    public void testPublishedGuruWithNotPublishedModif() {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modif = createModification(model, false);

        ModelPipeContext ctx = process(model, Collections.singletonList(modif));

        // Everithing will be unpublished, because model should contain at least one published modification
        checkPublished(ctx);
    }

    @Test
    public void testNotPublishedGuruWithPublishedModif() {
        ModelStorage.Model model = createModel(false);
        ModelStorage.Model modif = createModification(model, true);

        ModelPipeContext ctx = process(model, Collections.singletonList(modif));

        checkPublished(ctx);
    }

    @Test
    public void testGuruWithSku() {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model sku = createSkuFor(model, true);

        ModelPipeContext ctx = process(model, Collections.emptyList(), Collections.singleton(sku));

        checkPublished(ctx, model, sku);
    }

    @Test
    public void testPublishedGuruWithNotPublishedSku() {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model sku = createSkuFor(model, false);

        ModelPipeContext ctx = process(model, Collections.emptyList(), Collections.singleton(sku));

        checkPublished(ctx, model);
    }

    @Test
    public void testNotPublishedGuruWithPublishedSku() {
        ModelStorage.Model model = createModel(false);
        ModelStorage.Model sku = createSkuFor(model, true);

        ModelPipeContext ctx = process(model, Collections.emptyList(), Collections.singleton(sku));

        checkPublished(ctx);
    }

    @Test
    public void testComplex() {
        ModelStorage.Model model = createModel(true); //10

        ModelStorage.Model modif1 = createModification(model, false); //11
        ModelStorage.Model modif2 = createModification(model, true); //12
        ModelStorage.Model modif3 = createModification(model, true); //13

        ModelStorage.Model modelSKU = createSkuFor(model, true); //14
        ModelStorage.Model modelSKU2 = createSkuFor(model, false); //15

        ModelStorage.Model modif1SKU = createSkuFor(modif1, true); //16
        ModelStorage.Model modif1SKU2 = createSkuFor(modif1, false); //17

        ModelStorage.Model modif2SKU = createSkuFor(modif2, true); //18
        ModelStorage.Model modif2SKU2 = createSkuFor(modif2, true); //19
        ModelStorage.Model modif2SKU3 = createSkuFor(modif2, false); //20

        ModelPipeContext ctx = process(model,
            Arrays.asList(modif1, modif2, modif3),
            Arrays.asList(modelSKU, modelSKU2,
                modif1SKU, modif1SKU2,
                modif2SKU, modif2SKU2, modif2SKU3)
        );

        checkPublished(ctx, model, modif2, modif3, modelSKU, modif2SKU, modif2SKU2);
    }

    @Test
    public void testPartnerWithPartnerSkuNotPublishedOnMarket() {
        ModelStorage.Model model = createModel(true).toBuilder()
            .setCurrentType(CommonModel.Source.PARTNER.name()).build();
        ModelStorage.Model sku = createSkuFor(model, true).toBuilder()
            .setCurrentType(CommonModel.Source.PARTNER_SKU.name()).build();

        ModelPipeContext ctx = process(model, Collections.emptyList(), Collections.singleton(sku));

        checkPublished(ctx);
    }

    @Test
    public void testAlwaysPublished() {
        ModelStorage.Model model = makePartner(createModel(true)); //10

        ModelStorage.Model modelSKU = makePartner(createSkuFor(model, true)); //11
        ModelStorage.Model modelSKU2 = makePartner(createSkuFor(model, false)); //12

        ModelPipeContext ctx = process(model,
            Collections.emptyList(),
            Arrays.asList(modelSKU, modelSKU2),
            Collections.emptyList()
        );

        checkPublished(ctx, model, modelSKU);
    }

    @Test
    public void testBrokenPublished() {
        ModelStorage.Model model = createModel(true, true, false);

        ModelPipeContext ctx = process(model);

        checkPublished(ctx);
    }

    @Test
    public void testStrictChecksRequiredPublished() {
        ModelStorage.Model model = createModel(true, false, true);

        ModelPipeContext ctx = process(model);

        checkPublished(ctx);
    }

    @Test
    public void testBrokenAndStrictChecksRequiredPublished() {
        ModelStorage.Model model = createModel(true, true, true);

        ModelPipeContext ctx = process(model);

        checkPublished(ctx);
    }

    @Test
    public void testBrokenAndStrictInheritance() {
        ModelStorage.Model model = createModel(true, true, true); //10

        ModelStorage.Model modif1 = createModification(model, true); //11

        ModelStorage.Model modelSKU = createSkuFor(model, true); //12

        ModelStorage.Model modif1SKU = createSkuFor(modif1, true); //13

        ModelPipeContext ctx = process(model,
            Arrays.asList(modif1),
            Arrays.asList(modelSKU, modif1SKU)
        );

        checkPublished(ctx);
    }

    private ModelStorage.Model makePartner(ModelStorage.Model model) {
        return model.toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setValueType(MboParameters.ValueType.ENUM)
                .setParamId(KnownIds.MODEL_QUALITY_PARAM_ID)
                .setOptionId((int) KnownIds.MODEL_QUALITY_OFFER)
                .setXslName(XslNames.MODEL_QUALITY)
                .build())
            .build();
    }

    protected ModelPipeContext process(ModelStorage.Model model) {
        return process(model, Collections.emptyList());
    }

    protected ModelPipeContext process(ModelStorage.Model model,
                                       Collection<ModelStorage.Model> modifs) {
        return process(model, modifs, Collections.emptyList());
    }

    protected ModelPipeContext process(ModelStorage.Model model,
                                       Collection<ModelStorage.Model> modifs,
                                       Collection<ModelStorage.Model> skus) {
        return process(model, modifs, skus, Arrays.asList(CommonModel.Source.values()));
    }

    protected ModelPipeContext process(ModelStorage.Model model,
                                       Collection<ModelStorage.Model> modifs,
                                       Collection<ModelStorage.Model> skus,
                                       List<CommonModel.Source> showModelTypes) {
        CategoryInfo categoryInfo = CategoryInfo.create(
            0L, Collections.singleton(0L), showModelTypes, true,
            Collections.emptyList(),
            SizeMeasureExportHelper.create(Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()),
            null,
            SizeChartExportHelper.create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap())
        );
        ModelPipeContext ctx = new ModelPipeContext(model, modifs, skus);
        Pipe pipe = Pipe.start()
            .then(new PublishedOnMarketPipePart(categoryInfo))
            .build();
        try {
            pipe.acceptModelsGroup(ctx);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }

    private void checkPublished(ModelPipeContext ctx, ModelStorage.Model... publishedModels) {
        Set<Long> publishedModelIds = Arrays.stream(publishedModels)
            .map(ModelStorage.Model::getId).collect(Collectors.toSet());

        List<Long> expectedPublishedModels = ctx.getModels().stream()
            .filter(model -> publishedModelIds.contains(model.getId()))
            .filter(model -> !model.getPublishedOnMarket())
            .map(model -> model.getId())
            .collect(Collectors.toList());
        List<Long> expectedNotPublishedModels = ctx.getModels().stream()
            .filter(model -> !publishedModelIds.contains(model.getId()))
            .filter(model -> model.getPublishedOnMarket())
            .map(model -> model.getId())
            .collect(Collectors.toList());

        if (!expectedPublishedModels.isEmpty()) {
            Assert.fail("Expected models with ids: " + expectedPublishedModels + " to be published. Actual is false.");
        }
        if (!expectedNotPublishedModels.isEmpty()) {
            Assert.fail("Expected models with ids: " + expectedNotPublishedModels + " to be not published. " +
                "Actual is true.");
        }
    }
}
