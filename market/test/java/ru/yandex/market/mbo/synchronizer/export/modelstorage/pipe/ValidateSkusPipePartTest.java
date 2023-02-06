package ru.yandex.market.mbo.synchronizer.export.modelstorage.pipe;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.Pipe;
import ru.yandex.market.mbo.export.modelstorage.pipe.PublishedOnBlueMarketPipePart;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.CategoryValidationResult;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.SkuDuplicationError;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.ValidationError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author commince
 * @since 27.02.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ValidateSkusPipePartTest {
    private static final long MILLIS_IN_HOUR = TimeUnit.HOURS.toMillis(1);
    private static final int THRESHOLD = 8;

    private ValidateSkusPipePart validateSkusPipePart;
    private CategoryValidationResult result;

    @Before
    public void setUp() {
        result = new CategoryValidationResult(1L, "Category");
        validateSkusPipePart = new ValidateSkusPipePart(result, THRESHOLD);
    }

    @Test
    public void testModelValidationOk() throws IOException, MessagingException, ParseException {
        ModelStorage.Model model = createModel(false);
        ModelStorage.Model modelSKU1 = createSkuFor(100, model, getDateMinusHours(1).getTime());
        ModelStorage.Model modelSKU2 = createSkuFor(101, model, getDateMinusHours(2).getTime());

        ModelPipeContext ctx = process(true, model,
            Collections.emptyList(),
            Arrays.asList(modelSKU1, modelSKU2)
        );

        validateSkusPipePart.acceptModelsGroup(ctx);
        validateSkusPipePart.flush();

        SkuDuplicationError error = result.getError(ValidationError.ErrorType.SKU_DUPLICATION_ERROR);
        Assert.assertNull(error);
    }

    @Test
    public void testModificationValidationOk() throws IOException, MessagingException {
        ModelStorage.Model model = createModel(false);
        ModelStorage.Model modif = createModification(true);
        ModelStorage.Model modelSKU1 = createSkuFor(100, modif, getDateMinusHours(1).getTime());
        ModelStorage.Model modelSKU2 = createSkuFor(101, modif, getDateMinusHours(1).getTime());

        ModelPipeContext ctx = process(true, model,
            Collections.singletonList(modif),
            Arrays.asList(modelSKU1, modelSKU2)
        );

        validateSkusPipePart.acceptModelsGroup(ctx);
        validateSkusPipePart.flush();

        SkuDuplicationError error = result.getError(ValidationError.ErrorType.SKU_DUPLICATION_ERROR);
        Assert.assertNull(error);
    }

    @Test
    public void testModelAndSkuWithSameIdCase() throws IOException, MessagingException {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modelSKU = createSkuFor(1, model, getDateMinusHours(10).getTime());


        ModelPipeContext ctx = process(true, model,
            Collections.emptyList(),
            Collections.singletonList(modelSKU)
        );

        validateSkusPipePart.acceptModelsGroup(ctx);

        SkuDuplicationError error = result.getError(ValidationError.ErrorType.SKU_DUPLICATION_ERROR);
        Assert.assertNull(error);
    }

    @Test
    public void testModelValidationFailed() throws IOException, MessagingException {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modelSKU1 = createSkuFor(100, model, getDateMinusHours(7).getTime());
        ModelStorage.Model modelSKU2 = createSkuFor(101, model, getDateMinusHours(9).getTime());


        ModelPipeContext ctx = process(true, model, Collections.emptyList(),
            Arrays.asList(modelSKU1, modelSKU2)
        );

        validateSkusPipePart.acceptModelsGroup(ctx);
        validateSkusPipePart.flush();

        SkuDuplicationError error = result.getError(ValidationError.ErrorType.SKU_DUPLICATION_ERROR);
        Assert.assertNotNull(error);
        assertThat(error.getSkuIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void testModificationValidationFailed() throws IOException, MessagingException {
        ModelStorage.Model model = createModel(false);
        ModelStorage.Model modif = createModification(true);
        ModelStorage.Model modelSKU1 = createSkuFor(100, modif, getDateMinusHours(10).getTime());
        ModelStorage.Model modelSKU2 = createSkuFor(101, modif, getDateMinusHours(1).getTime());


        ModelPipeContext ctx = process(true, model,
            Collections.singletonList(modif),
            Arrays.asList(modelSKU1, modelSKU2)
        );

        validateSkusPipePart.acceptModelsGroup(ctx);
        validateSkusPipePart.flush();

        SkuDuplicationError error = result.getError(ValidationError.ErrorType.SKU_DUPLICATION_ERROR);
        Assert.assertNotNull(error);
        assertThat(error.getSkuIds()).containsExactlyInAnyOrder(10L);
    }

    private ModelStorage.Model createModification(boolean isSku) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setId(10)
            .setParentId(1)
            .setPublished(true)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("modification10"));

        if (isSku) {
            builder.addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(XslNames.IS_SKU)
                    .setBoolValue(true));
        }

        return builder.build();
    }

    private ModelStorage.Model createModel(boolean isSku) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setId(1)
            .setPublished(true)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                        .setIsoCode("ru")
                        .setValue("model1"));

        if (isSku) {
            builder.addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(XslNames.IS_SKU)
                    .setBoolValue(true));
        }

        return builder.build();
    }

    private ModelStorage.Model createSkuFor(long id, ModelStorage.Model model, long creationDateMillis) {
        return ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.SKU.name())
            .setId(id)
            .setPublished(true)
            .setCreatedDate(creationDateMillis)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("sku" + id))
            .addRelations(
                ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(model.getId()))
            .build();
    }

    private ModelPipeContext process(boolean extGrouped, ModelStorage.Model model,
                                     Collection<ModelStorage.Model> modifs,
                                     Collection<ModelStorage.Model> skus) throws IOException {
        ModelPipeContext ctx = new ModelPipeContext(model, modifs, skus);
        Pipe pipe = Pipe.start()
            .then(new PublishedOnBlueMarketPipePart(extGrouped, Collections.emptyList()))
            .build();
        pipe.acceptModelsGroup(ctx);
        return ctx;
    }

    private Date getDateMinusHours(int hours) {
        Date date = new Date();
        return new Date(date.getTime() - hours * MILLIS_IN_HOUR);
    }
}
