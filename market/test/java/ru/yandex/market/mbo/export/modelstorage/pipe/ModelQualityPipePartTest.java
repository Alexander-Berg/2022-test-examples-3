package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelQualityPipePartTest extends BasePipePartTestClass {
    private static final long UID = 100500L;
    private static final long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "category_name";

    private ParametersBuilder<CommonModelBuilder<CommonModel>> parametersBuilder;
    private CommonModelBuilder<CommonModel> modelBuilder;

    @Before
    public void setUp() {
        parametersBuilder =
            ParametersBuilder.defaultBuilder()
                .startParameter()
                .id(KnownIds.MODEL_QUALITY_PARAM_ID)
                .type(Param.Type.ENUM)
                .xsl(XslNames.MODEL_QUALITY)
                .option(KnownIds.MODEL_QUALITY_OPERATOR, "Оператор")
                .option(KnownIds.MODEL_QUALITY_OFFER, "Оффер")
                .name("Качество")
                .endParameter()
                .startParameter()
                .id(500L)
                .type(Param.Type.BOOLEAN)
                .xsl(XslNames.MATCHING_DISABLED)
                .option(200L, "TRUE")
                .option(201L, "FALSE")
                .name("Не матчер")
                .endParameter();
        modelBuilder = parametersBuilder.endParameters();
    }

    @Test
    public void testPartnerNotChanged() throws IOException {
        ModelStorage.Model model = createQualityModel(1L, CommonModel.Source.PARTNER, KnownIds.MODEL_QUALITY_OFFER);
        ModelPipeContext context = process(createPipe(),
            model);

        assertMatchingDisabledParameterValueNotExists(context.getModel());
    }

    @Test
    public void testOfferGuruChanged() throws IOException {
        ModelStorage.Model model = createQualityModel(1L, CommonModel.Source.GURU, KnownIds.MODEL_QUALITY_OFFER);
        ModelPipeContext context = process(createPipe(),
            model);

        assertMatchingDisabledParameterValueTrue(context.getModel());
    }

    @Test
    public void testOperatorGuruNotChanged() throws IOException {
        ModelStorage.Model model = createQualityModel(1L, CommonModel.Source.GURU, KnownIds.MODEL_QUALITY_OPERATOR);
        ModelPipeContext context = process(createPipe(),
            model);

        assertMatchingDisabledParameterValueNotExists(context.getModel());
    }

    private Pipe createPipe(CategoryInfo categoryInfo) {
        return Pipe.simple(new ModelQualityPipePart(categoryInfo, UID));
    }

    private Pipe createPipe() {
        return createPipe(createCategoryInfo());
    }

    private CategoryInfo createCategoryInfo() {
        return new CategoryInfo(
            CATEGORY_ID,
            false,
            Collections.emptyList(),
            Collections.singletonList(CommonModel.Source.GURU),
            Collections.emptySet(),
            CATEGORY_NAME,
            new TMTemplate(),
            null,
            parametersBuilder.getParameters().stream()
                .map(ForTitleParameter::fromCategoryParam)
                .collect(Collectors.toList()),
            Collections.emptyList(),
            null,
            null);
    }

    private ModelPipeContext process(Pipe pipe,
                                     ModelStorage.Model model) throws IOException {
        ModelPipeContext ctx = new ModelPipeContext(model, Collections.emptyList(), Collections.emptyList());
        pipe.acceptModelsGroup(ctx);
        return ctx;
    }

    private void assertMatchingDisabledParameterValue(ModelStorage.Model model,
                                                      Consumer<ModelStorage.ParameterValue> consumer) {
        ModelStorage.ParameterValue parameterValue = model.getParameterValuesList().stream()
            .filter(pv -> pv.getXslName().equals(XslNames.MATCHING_DISABLED))
            .findFirst()
            .orElse(null);
        consumer.accept(parameterValue);
    }

    private void assertMatchingDisabledParameterValueNotExists(ModelStorage.Model.Builder builder) {
        assertMatchingDisabledParameterValue(builder.build(), pv -> assertThat(pv).isNull());
    }

    private void assertMatchingDisabledParameterValueTrue(ModelStorage.Model.Builder builder) {
        assertMatchingDisabledParameterValue(builder.build(), pv -> {
            assertThat(pv).isNotNull();
            assertThat(pv)
                .extracting(ModelStorage.ParameterValue::getBoolValue)
                .isEqualTo(true);
        });
    }

    private ModelStorage.Model createQualityModel(long id, CommonModel.Source type, Long quality) {
        CommonModelBuilder<CommonModel> result = modelBuilder.startModel()
            .id(id)
            .category(CATEGORY_ID)
            .source(type)
            .currentType(type)
            .published(true)
            .vendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID);

        if (quality != null) {
            result.param(KnownIds.MODEL_QUALITY_PARAM_ID).setOption(quality);
        }

        return result.getRawModel();
    }
}
