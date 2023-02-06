package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author york
 * @since 27.02.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class PublishedOnBlueMarketPipeTest extends BasePipePartTestClass {

    @Test
    public void testMarkClustersFalse() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.CLUSTER.name())
            .build();

        ModelStorage.Model.Builder newMdl = process(false, model).getModel();
        Assert.assertEquals(true, newMdl.hasPublishedOnBlueMarket());
        Assert.assertEquals(false, newMdl.getPublishedOnBlueMarket());
    }

    @Test
    public void testMarkGuruDummyFalse() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU_DUMMY.name())
            .build();

        ModelStorage.Model.Builder newMdl = process(false, model).getModel();
        Assert.assertEquals(true, newMdl.hasPublishedOnBlueMarket());
        Assert.assertEquals(false, newMdl.getPublishedOnBlueMarket());
    }

    @Test
    public void testMarkNoSkuFalse() throws IOException {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modif = createModification(model, true);

        ModelPipeContext ctx = process(true, model, Collections.singletonList(modif));

        checkAllMarked(ctx);
        checkAllPublished(ctx);
    }

    @Test
    public void testMarkComplex() throws IOException {
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

        ModelPipeContext ctx = process(true, model,
            Arrays.asList(modif1, modif2, modif3),
            Arrays.asList(modelSKU, modelSKU2,
                modif1SKU, modif1SKU2,
                modif2SKU, modif2SKU2, modif2SKU3)
        );

        checkAllMarked(ctx);
        checkAllPublished(ctx, model.getId(), modelSKU.getId(),
            modif2.getId(), modif2SKU.getId(), modif2SKU2.getId());

    }

    @Test
    public void testMarkModelAsSKU() throws IOException {
        ModelStorage.Model modelAsSKU = createModel(true).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.IS_SKU)
                .setBoolValue(true)
            )
        .build();

        ModelPipeContext ctx = process(true, modelAsSKU);
        checkAllMarked(ctx);
        checkAllPublished(ctx, modelAsSKU.getId());

        ctx = process(false, modelAsSKU);
        checkAllMarked(ctx);
        checkAllPublished(ctx, modelAsSKU.getId());

        ModelStorage.Model modification = createModification(modelAsSKU, true);
        ctx = process(true, modelAsSKU, Collections.singletonList(modification));
        checkAllMarked(ctx);
        checkAllPublished(ctx);

        ModelStorage.Model model = createModel(true);

        ModelStorage.Model modificationAsSKU = createModification(model, true).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.IS_SKU)
                .setBoolValue(true)
            )
            .build();

        ctx = process(true, model, Collections.singletonList(modificationAsSKU));
        checkAllMarked(ctx);
        checkAllPublished(ctx, model.getId(), modificationAsSKU.getId());
    }

    @Test
    public void testModelWithBluePublishedMarkedAsSKU() throws IOException {
        ModelStorage.Model modelAsSKU = createModel(false).toBuilder()
            .setBluePublished(true)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.IS_SKU)
                .setBoolValue(true)
            )
            .build();

        ModelPipeContext ctx = process(true, modelAsSKU);
        checkAllMarked(ctx);
        checkAllPublished(ctx, modelAsSKU.getId());

        ctx = process(false, modelAsSKU);
        checkAllMarked(ctx);
        checkAllPublished(ctx, modelAsSKU.getId());
    }

    @Test
    public void testBluePublishedFlag() throws IOException {
        ModelStorage.Model model = createModel(false)
            .toBuilder()
            .setBluePublished(true)
            .build(); //10

        ModelStorage.Model modification = createModification(model, false); // 11

        ModelStorage.Model modelSku = createSkuFor(model, true); //12
        ModelStorage.Model modificationSku = createSkuFor(modification, true); //13

        ModelPipeContext ctx = process(true, model, Collections.singletonList(modification),
            Arrays.asList(modelSku, modificationSku));
        checkAllMarked(ctx);
        checkAllPublished(ctx, model.getId(), modelSku.getId());

        ModelStorage.Model modificationAsSKU = createModification(model, true).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.IS_SKU)
                .setBoolValue(true)
            )
            .build();

        ctx = process(false, model, Arrays.asList(modification, modificationAsSKU),
            Arrays.asList(modelSku, modificationSku));

        checkAllMarked(ctx);
        checkAllPublished(ctx, modificationAsSKU.getId());
    }

    protected ModelPipeContext process(boolean extGrouped, ModelStorage.Model model) throws IOException {
        return process(extGrouped, model, Collections.emptyList());
    }

    protected ModelPipeContext process(boolean extGrouped, ModelStorage.Model model,
                                       Collection<ModelStorage.Model> modifs) throws IOException {
        return process(extGrouped, model, modifs, Collections.emptyList());
    }

    protected ModelPipeContext process(boolean extGrouped, ModelStorage.Model model,
                                       Collection<ModelStorage.Model> modifs,
                                       Collection<ModelStorage.Model> skus) throws IOException {
        ModelPipeContext ctx = new ModelPipeContext(model, modifs, skus);
        Pipe pipe = Pipe.start()
            .then(new PublishedOnBlueMarketPipePart(extGrouped, Collections.emptyList()))
            .build();
        pipe.acceptModelsGroup(ctx);
        return ctx;
    }

    private void checkAllMarked(ModelPipeContext context) {
        checkAll(context, m -> m.hasPublishedOnBlueMarket(), "marked");
    }

    private void checkAllPublished(ModelPipeContext ctx, Long... publishedIds) {
        Set<Long> idsSet = new HashSet<>(Arrays.asList(publishedIds));
        checkAll(ctx,
            m -> idsSet.contains(m.getId()) == m.getPublishedOnBlueMarket(), "published");
    }
}
