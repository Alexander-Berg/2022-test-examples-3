package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 14.06.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class IsPartnerParamPipePartTest extends BasePipePartTestClass {
    private static final long UID = 100500L;
    private static final long TRUE_OPTION_ID = 9999L;
    private static final long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "category_name";

    @Test
    public void testGuruModificationAndSkuNotChanged() throws IOException {
        ModelStorage.Model model = createModel(true);
        ModelStorage.Model modification = createModification(model, true);
        ModelStorage.Model sku = createSkuFor(modification, true);
        ModelPipeContext context = process(createPipe(),
            model,
            Collections.singletonList(modification),
            Collections.singletonList(sku));

        assertIsPartnerParameterValueNotExists(context.getModel());
        context.getModifications().forEach(this::assertIsPartnerParameterValueNotExists);
        context.getSkus().forEach(this::assertIsPartnerParameterValueNotExists);
    }

    @Test
    public void testPartnerGuruChanged() throws IOException {
        ModelStorage.Model model = createModel(true).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.MODEL_QUALITY)
                .setParamId(KnownIds.MODEL_QUALITY_PARAM_ID)
                .setOptionId(1)
                .build())
            .build();
        ModelStorage.Model modification = createModification(model, true).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.MODEL_QUALITY)
                .setParamId(KnownIds.MODEL_QUALITY_PARAM_ID)
                .setOptionId(1)
                .build())
            .build();
        ModelStorage.Model sku = createSkuFor(modification, true);
        ModelPipeContext context = process(createPipe(),
            model,
            Collections.singletonList(modification),
            Collections.singletonList(sku));

        assertIsPartnerParameterValueTrue(context.getModel());
        context.getModifications().forEach(this::assertIsPartnerParameterValueTrue);
        context.getSkus().forEach(this::assertIsPartnerParameterValueNotExists);
    }

    @Test
    public void testIsPartnerAdded() throws IOException {
        ModelStorage.Model model = createPartnerModel(true);
        ModelStorage.Model sku = createPartnerSkuFor(model, true);
        ModelPipeContext context = process(createPipe(),
            model,
            Collections.emptyList(),
            Collections.singletonList(sku));

        assertIsPartnerParameterValueTrue(context.getModel());
        context.getSkus().forEach(this::assertIsPartnerParameterValueTrue);
    }

    @Test(expected = RuntimeException.class)
    public void testIsPartnerParamNotFound() throws IOException {
        ModelStorage.Model model = createPartnerModel(true);
        ModelStorage.Model sku = createPartnerSkuFor(model, true);
        process(createPipe(createNoIsPartnerCategoryInfo()),
            model,
            Collections.emptyList(),
            Collections.singletonList(sku));
    }

    @Test(expected = RuntimeException.class)
    public void testTrueOptionNotFound() throws IOException {
        ModelStorage.Model model = createPartnerModel(true);
        ModelStorage.Model sku = createPartnerSkuFor(model, true);
        process(createPipe(createNoTrueOptionIsPartnerCategoryInfo()),
            model,
            Collections.emptyList(),
            Collections.singletonList(sku));
    }

    private Pipe createPipe(CategoryInfo categoryInfo) {
        return Pipe.simple(new IsPartnerParamPipePart(categoryInfo, UID));
    }

    private Pipe createPipe() {
        return createPipe(createIsPartnerCategoryInfo());
    }

    private CategoryInfo createCategoryInfo(List<ForTitleParameter> parameters) {
        return new CategoryInfo(
            CATEGORY_ID,
            false,
            Collections.emptyList(),
            Collections.singletonList(CommonModel.Source.GURU),
            Collections.emptySet(),
            CATEGORY_NAME,
            new TMTemplate(),
            null,
            parameters,
            Collections.emptyList(),
            null,
            null);
    }

    private CategoryInfo createNoIsPartnerCategoryInfo() {
        return createCategoryInfo(Collections.emptyList());
    }

    private CategoryInfo createIsPartnerCategoryInfo() {
        CategoryParam isPartnerParam = CategoryParamBuilder.newBuilder(KnownIds.IS_PARTNER_PARAM_ID,
            XslNames.IS_PARTNER, Param.Type.BOOLEAN)
            .addOption(OptionBuilder.newBuilder().addName("TRUE").setId(TRUE_OPTION_ID).build())
            .build();
        return createCategoryInfo(Collections.singletonList(ForTitleParameter.fromCategoryParam(isPartnerParam)));
    }

    private CategoryInfo createNoTrueOptionIsPartnerCategoryInfo() {
        CategoryParam isPartnerParam = CategoryParamBuilder.newBuilder(KnownIds.IS_PARTNER_PARAM_ID,
            XslNames.IS_PARTNER, Param.Type.BOOLEAN)
            .addOption(OptionBuilder.newBuilder().addName("FALSE").setId(TRUE_OPTION_ID).build())
            .build();
        return createCategoryInfo(Collections.singletonList(ForTitleParameter.fromCategoryParam(isPartnerParam)));
    }

    private ModelPipeContext process(Pipe pipe,
                                     ModelStorage.Model model,
                                     Collection<ModelStorage.Model> modifs,
                                     Collection<ModelStorage.Model> skus) throws IOException {
        ModelPipeContext ctx = new ModelPipeContext(model, modifs, skus);
        pipe.acceptModelsGroup(ctx);
        return ctx;
    }

    private void assertIsPartnerParameterValue(ModelStorage.Model model,
                                               Consumer<ModelStorage.ParameterValue> consumer) {
        ModelStorage.ParameterValue parameterValue = model.getParameterValuesList().stream()
            .filter(pv -> pv.getParamId() == KnownIds.IS_PARTNER_PARAM_ID)
            .findFirst()
            .orElse(null);
        consumer.accept(parameterValue);
    }

    private void assertIsPartnerParameterValueNotExists(ModelStorage.Model.Builder builder) {
        assertIsPartnerParameterValue(builder.build(), pv -> assertThat(pv).isNull());
    }

    private void assertIsPartnerParameterValueTrue(ModelStorage.Model.Builder builder) {
        assertIsPartnerParameterValue(builder.build(), pv -> {
            assertThat(pv).isNotNull();
            assertThat(pv)
                .extracting(ModelStorage.ParameterValue::getBoolValue)
                .isEqualTo(true);
        });
    }
}
