package ru.yandex.market.mbo.synchronizer.export.modelstorage.pipe;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.CategoryValidationResult;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.GuruTitleGenerationError;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.SkuTitleGenerationError;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.validation.ValidationError;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ValidateTitlesPipePartTest {
    private ValidateTitlesPipePart pipePart;
    private CategoryValidationResult result;

    @Before
    public void setUp() {
        result = new CategoryValidationResult(1L, "Category");
        pipePart = new ValidateTitlesPipePart(result);
    }

    @Test
    public void testValidationPasses() throws IOException {
        ModelStorage.Model model = createModel(true)
            .setPublished(true)
            .build();
        ModelStorage.Model modification = createModification(true)
            .setBluePublished(true)
            .build();
        ModelStorage.Model sku1 = createSkuFor(100, model, true)
            .setPublished(true)
            .build();
        ModelStorage.Model sku2 = createSkuFor(101, modification, true)
            .setBluePublished(true)
            .build();

        ModelPipeContext ctx = new ModelPipeContext(
            model,
            Collections.singletonList(modification),
            Arrays.asList(sku1, sku2)
        );

        pipePart.acceptModelsGroup(ctx);
        pipePart.flush();

        GuruTitleGenerationError guruError = result.getError(ValidationError.ErrorType.GURU_TITLE_GENERATION_ERROR);
        Assert.assertNull(guruError);
        SkuTitleGenerationError skuError = result.getError(ValidationError.ErrorType.SKU_TITLE_GENERATION_ERROR);
        Assert.assertNull(skuError);
    }

    @Test
    public void testValidationFails() throws IOException {
        ModelStorage.Model model = createModel(false)
            .setPublished(true)
            .build();
        ModelStorage.Model modification = createModification(false)
            .setBluePublished(true)
            .build();
        ModelStorage.Model sku1 = createSkuFor(100, model, false)
            .setPublished(true)
            .build();
        ModelStorage.Model sku2 = createSkuFor(101, modification, false)
            .setBluePublished(true)
            .build();

        ModelPipeContext ctx = new ModelPipeContext(
            model,
            Collections.singletonList(modification),
            Arrays.asList(sku1, sku2)
        );

        pipePart.acceptModelsGroup(ctx);
        pipePart.flush();

        GuruTitleGenerationError guruError = result.getError(ValidationError.ErrorType.GURU_TITLE_GENERATION_ERROR);
        assertThat(guruError.getModelIds()).containsExactlyInAnyOrder(1L, 2L);
        SkuTitleGenerationError skuError = result.getError(ValidationError.ErrorType.SKU_TITLE_GENERATION_ERROR);
        assertThat(skuError.getModelIds()).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    public void testValidationPassedForUnpublished() throws IOException {
        ModelStorage.Model model = createModel(false)
            .build();
        ModelStorage.Model modification = createModification(false)
            .build();
        ModelStorage.Model sku1 = createSkuFor(100, model, false)
            .build();
        ModelStorage.Model sku2 = createSkuFor(101, modification, false)
            .build();

        ModelPipeContext ctx = new ModelPipeContext(
            model,
            Collections.singletonList(modification),
            Arrays.asList(sku1, sku2)
        );

        pipePart.acceptModelsGroup(ctx);
        pipePart.flush();

        GuruTitleGenerationError guruError = result.getError(ValidationError.ErrorType.GURU_TITLE_GENERATION_ERROR);
        Assert.assertNull(guruError);
        SkuTitleGenerationError skuError = result.getError(ValidationError.ErrorType.SKU_TITLE_GENERATION_ERROR);
        Assert.assertNull(skuError);
    }

    private ModelStorage.Model.Builder createModification(boolean withTitle) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setId(2)
            .setParentId(1);
        if (withTitle) {
            builder.addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("modification10"));
        }

        return builder;
    }

    private ModelStorage.Model.Builder createModel(boolean withTitle) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setId(1);
        if (withTitle) {
            builder.addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("modification10"));
        }

        return builder;
    }

    private ModelStorage.Model.Builder createSkuFor(long id, ModelStorage.Model model, boolean withTitle) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.SKU.name())
            .setId(id);
        if (withTitle) {
            builder.addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("sku" + id));
        }
        builder.addRelations(
            ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(model.getId()));
        return builder;
    }
}
