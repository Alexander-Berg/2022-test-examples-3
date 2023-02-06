package ru.yandex.market.mbo.export.modelstorage;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.TitlePipePart;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.01.2017
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:methodlength"})
public class TitlePipePartTest {
    private static final String GURU_TITLE_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";
    private static final String SKU_TITLE_TEMPLATE =
        "{\"delimiter\":\" \",\"before\":\"test \",\"values\":[[(1 ),(t0 ),null,(true)],[(1 ),(v14871214 )]]}";
    public static final String SHORT_SKU_TEMPLATE =
        "{\"delimiter\":\" \",\"before\":\"short title \",\"values\":"
                + "[[(1 ),(t0 ),null,(true)],[(1 ),(v14871214 )]]}";


    private final CategoryParam nameParam = new Parameter();
    private final CategoryParam vendorParam = new Parameter();
    private final CategoryParam colorVendorParam = new Parameter();
    private final CategoryParam boolSkuParam = new Parameter();
    private final CategoryParam strSkuParam = new Parameter();
    private final CategoryParam strSkuParam2 = new Parameter();
    private final CategoryParam numericSkuParam = new Parameter();
    private final CategoryParam isSkuParam = new Parameter();

    private final List<CategoryParam> params =
        Arrays.asList(nameParam, vendorParam, colorVendorParam, boolSkuParam, strSkuParam, numericSkuParam,
            isSkuParam, strSkuParam2);

    private CommonModelBuilder<CommonModel> modelBuilder = CommonModelBuilder.newBuilder();
    private TitlePipePart titlePipePart;

    @Before
    public void init() {
        CategoryInfo categoryInfo = createCategoryInfo();
        titlePipePart = TitlePipePart.create(categoryInfo);
    }

    @Test
    public void testTitleGenerationForGuruModelWithModificationsAndSkus() throws Exception {
        ModelPipeContext groupContext = createPipeContextWithModificationsAndSkus();
        titlePipePart.acceptModelsGroup(groupContext);

        assertEquals(1, groupContext.getModel().getTitlesCount());
        assertEquals("Apple MODEL1", groupContext.getModel().getTitles(0).getValue());

        assertThat("has title for modif1", groupContext.getModifications().stream()
            .anyMatch(m -> m.getTitles(0).getValue().equals("Apple MODIF1")));
        assertThat("has title for modif2", groupContext.getModifications().stream()
            .anyMatch(m -> m.getTitles(0).getValue().equals("Socks company MODIF2")));

        assertEquals("Test MODIF1 Blue", groupContext.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Test MODIF2 Red", groupContext.getSkus().get(1).getTitles(0).getValue());
        assertEquals("Test MODIF2 Blue", groupContext.getSkus().get(2).getTitles(0).getValue());
        assertEquals("Test MODIF1 Red", groupContext.getSkus().get(3).getTitles(0).getValue());
        assertEquals("Test MODIF2 Red", groupContext.getSkus().get(4).getTitles(0).getValue());
    }

    @Test
    public void testTitleGenerationForGuruModelWithSkus() {
        ModelPipeContext nonGroupContext = createPipeContextWithSkus();
        titlePipePart.acceptModelsGroup(nonGroupContext);

        assertEquals(1, nonGroupContext.getModel().getTitlesCount());
        assertEquals("Apple MODEL1", nonGroupContext.getModel().getTitles(0).getValue());

        assertEquals("Test MODEL1 Blue", nonGroupContext.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Test MODEL1 Blue", nonGroupContext.getSkus().get(1).getTitles(0).getValue());
        assertEquals("Test MODEL1 Red", nonGroupContext.getSkus().get(2).getTitles(0).getValue());
    }

    @Test
    public void testDefaultSkuTitleGenerationInPreprocessor() throws Exception {
        ModelPipeContext context = createPipeContextWithModificationsAndSkus();

        // create preprocessor without SKU template

        CategoryInfo info = CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate(GURU_TITLE_TEMPLATE, null, null, null, null, null),
            params, Collections.emptyList(), null, null
        );
        titlePipePart = TitlePipePart.create(info);
        titlePipePart.acceptModelsGroup(context);

        assertEquals("Apple MODEL1", context.getModel().getTitles(0).getValue());

        assertEquals("Socks company MODIF1 Blue StrValue1 boolean sku param 15 см",
            context.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Apple MODIF2 Red StrValue2", context.getSkus().get(1).getTitles(0).getValue());
        assertEquals("Socks company MODIF2 Blue StrValue5 boolean sku param",
            context.getSkus().get(2).getTitles(0).getValue());
        assertEquals("Socks company MODIF1 Red StrValue6 boolean sku param",
            context.getSkus().get(3).getTitles(0).getValue());
        assertEquals("Socks company MODIF2 Red StrValue7",
            context.getSkus().get(4).getTitles(0).getValue());
    }

    @Test
    public void testOfferTitleFallback() {
        ModelPipeContext context = createPipeContextWithSkus();

        CategoryInfo info = CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate("qwerty", "v666",
                        null, null, null, null),
            params, Collections.emptyList(), null, null
        );

        assertTrue(context.getSkus().size() >= 2);

        ModelStorage.Model.Builder publishedSku = context.getSkus().get(0);
        ModelStorage.Model.Builder unpublishedSku = context.getSkus().get(1);

        final String fallbackTitle = "SKU fallback TITLE";
        ParameterValue pv = new ParameterValue(0, XslNames.OFFER_TITLE, Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder()
                .setStringValue(WordUtil.defaultWord(fallbackTitle))
        );

        publishedSku.addParameterValues(ModelProtoConverter.convert(pv));
        unpublishedSku.addParameterValues(ModelProtoConverter.convert(pv));
        unpublishedSku.setPublished(false);

        titlePipePart = TitlePipePart.create(info);
        titlePipePart.acceptModelsGroup(context);

        assertEquals(0, publishedSku.getTitlesCount());
        assertEquals(1, unpublishedSku.getTitlesCount());
        assertEquals(fallbackTitle, unpublishedSku.getTitles(0).getValue());

    }

    @Test
    public void testDefaultSkuSizeTitleGenerationInPreprocessor() throws Exception {
        CategoryParam sizeParam = createSizeSkuParam();
        CategoryParam scaleParam = createScaleSkuParam();
        ModelPipeContext context = createPipeContextWithSkusWithSizes(sizeParam, scaleParam);

        List<CategoryParam> origParamsWithSizeParams = new ArrayList<>(params);
        origParamsWithSizeParams.add(sizeParam);
        origParamsWithSizeParams.add(scaleParam);

        // create preprocessor without SKU template
        CategoryInfo categoryInfo = CategoryInfo.forCategory(
                new TovarCategory(-1),
                new TMTemplate(GURU_TITLE_TEMPLATE, null, null, null, null, null),
                origParamsWithSizeParams,
                createTestSizeMeasures(),
                null,
                null);
        titlePipePart = TitlePipePart.create(categoryInfo);
        titlePipePart.acceptModelsGroup(context);

        assertEquals("Apple MODEL1", context.getModel().getTitles(0).getValue());

        assertEquals("Socks company MODEL1 StrValue101 boolean sku param 46 (RU)",
                context.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Socks company MODEL1 StrValue102 XL (INT)", context.getSkus().get(1).getTitles(0).getValue());
    }

    @Test
    public void testShortTitle() throws Exception {
        CategoryParam sizeParam = createSizeSkuParam();
        CategoryParam scaleParam = createScaleSkuParam();
        ModelPipeContext context = createPipeContextWithSkusWithSizes(sizeParam, scaleParam);

        List<CategoryParam> origParamsWithSizeParams = new ArrayList<>(params);
        origParamsWithSizeParams.add(sizeParam);
        origParamsWithSizeParams.add(scaleParam);

        // create preprocessor without SKU template
        CategoryInfo categoryInfo = CategoryInfo.forCategory(
                new TovarCategory(-1),
                new TMTemplate(GURU_TITLE_TEMPLATE, null, null, null, null, SHORT_SKU_TEMPLATE),
                origParamsWithSizeParams,
                createTestSizeMeasures(),
                null,
                null);
        titlePipePart = TitlePipePart.create(categoryInfo);
        titlePipePart.acceptModelsGroup(context);

        assertEquals("Apple MODEL1", context.getModel().getTitles(0).getValue());

        assertEquals("Socks company MODEL1 StrValue101 boolean sku param 46 (RU)",
                context.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Short title MODEL1",
                context.getSkus().get(0).getQualifiedTitles(0).getValue().getValue());
        assertEquals("Socks company MODEL1 StrValue102 XL (INT)", context.getSkus().get(1).getTitles(0).getValue());
    }

    @Test
    public void testShortTitleSwitch() throws Exception {
        CategoryParam sizeParam = createSizeSkuParam();
        CategoryParam scaleParam = createScaleSkuParam();
        ModelPipeContext context = createPipeContextWithSkusWithSizes(sizeParam, scaleParam);

        List<CategoryParam> origParamsWithSizeParams = new ArrayList<>(params);
        origParamsWithSizeParams.add(sizeParam);
        origParamsWithSizeParams.add(scaleParam);

        // create preprocessor without SKU template
        CategoryInfo categoryInfo = CategoryInfo.forCategory(
                new TovarCategory(-1),
                new TMTemplate(GURU_TITLE_TEMPLATE, null, null, null, null, "qwerty"),
                origParamsWithSizeParams,
                createTestSizeMeasures(),
                null,
                null);
        titlePipePart = TitlePipePart.create(categoryInfo);
        titlePipePart.acceptModelsGroup(context);

        assertEquals("Apple MODEL1", context.getModel().getTitles(0).getValue());

        assertEquals("Socks company MODEL1 StrValue101 boolean sku param 46 (RU)",
                context.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Socks company MODEL1 StrValue101 boolean sku param 46 (RU)",
                context.getSkus().get(0).getQualifiedTitles(0).getValue().getValue());
        assertEquals("Socks company MODEL1 StrValue102 XL (INT)", context.getSkus().get(1).getTitles(0).getValue());
    }


    @Test
    public void testShortTitleNone() throws Exception {
        CategoryParam sizeParam = createSizeSkuParam();
        CategoryParam scaleParam = createScaleSkuParam();
        ModelPipeContext context = createPipeContextWithSkusWithSizes(sizeParam, scaleParam);

        List<CategoryParam> origParamsWithSizeParams = new ArrayList<>(params);
        origParamsWithSizeParams.add(sizeParam);
        origParamsWithSizeParams.add(scaleParam);

        // create preprocessor without SKU template
        CategoryInfo categoryInfo = CategoryInfo.forCategory(
                new TovarCategory(-1),
                new TMTemplate(GURU_TITLE_TEMPLATE, null, null, null, null, null),
                origParamsWithSizeParams,
                createTestSizeMeasures(),
                null,
                null);
        titlePipePart = TitlePipePart.create(categoryInfo);
        titlePipePart.acceptModelsGroup(context);

        assertEquals("Apple MODEL1", context.getModel().getTitles(0).getValue());

        assertEquals("Socks company MODEL1 StrValue101 boolean sku param 46 (RU)",
                context.getSkus().get(0).getTitles(0).getValue());
        assertEquals(0,
                context.getSkus().get(0).getQualifiedTitlesCount());
        assertEquals("Socks company MODEL1 StrValue102 XL (INT)", context.getSkus().get(1).getTitles(0).getValue());
    }

    @Test
    public void testFailedTitleModelUnpublished() throws Exception {
        ModelPipeContext context = createPipeContextWithModificationsAndSkus();

        CategoryInfo info = CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate("qwerty", SKU_TITLE_TEMPLATE, null, null, null, null),
            params, Collections.emptyList(), null, null
        );
        titlePipePart = TitlePipePart.create(info);
        titlePipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modif1 = context.getModifications().stream()
            .filter(m -> m.getId() == 2L)
            .findFirst().orElseThrow(() -> new IllegalStateException("Expecting modification with id " + 2L));
        ModelStorage.Model.Builder modif2 = context.getModifications().stream()
            .filter(m -> m.getId() == 3L)
            .findFirst().orElseThrow(() -> new IllegalStateException("Expecting modification with id " + 3L));

        assertEquals(0, context.getModel().getTitlesCount());
        assertEquals(0, modif1.getTitlesCount());
        assertEquals(0, modif2.getTitlesCount());

        assertFalse(context.getModel().getPublishedOnMarket());
        assertFalse(modif1.getPublishedOnMarket());
        assertFalse(modif2.getPublishedOnBlueMarket());
        assertFalse(context.getSkus().get(0).getPublishedOnMarket());
        assertFalse(context.getSkus().get(1).getPublishedOnBlueMarket());
    }

    @Test
    public void testFailedTitleSkuUnpublished() throws Exception {
        ModelPipeContext context = createPipeContextWithModificationsAndSkus();

        CategoryInfo info = CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate(GURU_TITLE_TEMPLATE, "qwerty", null, null, null, null),
            params, Collections.emptyList(), null, null
        );
        titlePipePart = TitlePipePart.create(info);
        titlePipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modif1 = context.getModifications().stream()
            .filter(m -> m.getId() == 2L)
            .findFirst().orElseThrow(() -> new IllegalStateException("Expecting modification with id " + 2L));
        ModelStorage.Model.Builder modif2 = context.getModifications().stream()
            .filter(m -> m.getId() == 3L)
            .findFirst().orElseThrow(() -> new IllegalStateException("Expecting modification with id " + 3L));

        assertEquals(0, context.getSkus().get(0).getTitlesCount());
        assertEquals(0, context.getSkus().get(1).getTitlesCount());

        assertTrue(context.getModel().getPublishedOnMarket());
        assertTrue(modif1.getPublishedOnMarket());
        assertTrue(modif2.getPublishedOnBlueMarket());
        assertFalse(context.getSkus().get(0).getPublishedOnMarket());
        assertFalse(context.getSkus().get(1).getPublishedOnBlueMarket());
    }

    @Test
    public void testTitleGenerationForVendor() throws Exception {
        ModelPipeContext vendorContext = createPipeContextForVendorModel();
        titlePipePart.acceptModelsGroup(vendorContext);

        assertEquals(1, vendorContext.getModel().getTitlesCount());
        assertEquals("Apple VENDOR", vendorContext.getModel().getTitles(0).getValue());
    }

    @Test
    public void testTitleGenerationForGeneratedModel() throws Exception {
        ModelPipeContext generatedContext = createPipeContextForGeneratedModel();
        titlePipePart.acceptModelsGroup(generatedContext);

        assertEquals(1, generatedContext.getModel().getTitlesCount());
        assertEquals("Apple GENERATED", generatedContext.getModel().getTitles(0).getValue());
    }

    @Test
    public void testTitleGenerationForGeneratedModelWithGeneratedSku() throws Exception {
        ModelPipeContext context = createPipeContextWithGeneratedModelAndGeneratesSkus();
        titlePipePart.acceptModelsGroup(context);

        assertEquals(1, context.getModel().getTitlesCount());
        assertEquals("Apple GENERATED_MODEL_1", context.getModel().getTitles(0).getValue());

        assertEquals("Test GENERATED_MODEL_1 Blue", context.getSkus().get(0).getTitles(0).getValue());
        assertEquals("Test GENERATED_MODEL_1 Blue", context.getSkus().get(1).getTitles(0).getValue());
    }

    private ModelPipeContext createPipeContextWithModificationsAndSkus() {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_4", "http://url13")
            .vendorId(11L)
            .getRawModel().toBuilder()
            .setPublishedOnMarket(true)
            .build();

        ModelStorage.Model contextModif1 = modelBuilder
            .startModel()
            .title("MODIF1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(2L)
            .parentModelId(contextModel.getId())
            .published(true)
            .picture("XL-Picture_2", "http://url5")
            .vendorId(11L)
            .createdDate(new Date(1518685980548L))
            .getRawModel().toBuilder()
            .setPublishedOnMarket(true)
            .build();

        ModelStorage.Model contextModif2 = modelBuilder
            .startModel()
            .title("MODIF2")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(3L)
            .published(false)
            .parentModelId(contextModel.getId())
            .picture("XL-Picture", "http://url5")
            .picture("XL-Picture_2", "http://url7")
            .picture("XL-Picture_3", "http://url12")
            .vendorId(12L)
            .createdDate(new Date(1518685980547L))
            .getRawModel().toBuilder()
            .setPublishedOnBlueMarket(true)
            .build();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url6")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue1")
            .param(numericSkuParam)
            .setNumeric(15)
            .getRawModel().toBuilder()
            .setPublishedOnMarket(true)
            .build();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(5L)
            .vendorId(11L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url5")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue2")
            .getRawModel().toBuilder()
            .setPublishedOnBlueMarket(true)
            .build();

        ModelStorage.Model contextSku3 = modelBuilder
            .startModel()
            .title("SKU3")
            .id(6L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url8")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue5")
            .getRawModel();

        ModelStorage.Model contextSku4 = modelBuilder
            .startModel()
            .title("SKU4")
            .id(7L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url9")
            .picture(null, "http://url10")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue6")
            .getRawModel();

        ModelStorage.Model contextSku5 = modelBuilder
            .startModel()
            .title("SKU5")
            .id(8L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url11")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue7")
            .getRawModel();

        ModelStorage.Model contextSku6 = modelBuilder
            .startModel()
            .title("SKU6")
            .id(9L)
            .vendorId(12L)
            .published(false)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url14")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue8")
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Arrays.asList(contextModif1, contextModif2),
            Arrays.asList(contextSku1, contextSku2, contextSku3, contextSku4, contextSku5, contextSku6));
    }

    private ModelPipeContext createPipeContextForVendorModel() {
        ModelStorage.Model contextVendorModel = modelBuilder
            .startModel()
            .title("VENDOR")
            .currentType(CommonModel.Source.VENDOR)
            .source(CommonModel.Source.VENDOR)
            .id(10L)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_4", "http://url13")
            .vendorId(11L)
            .getRawModel().toBuilder()
            .setPublishedOnMarket(true)
            .build();

        return new ModelPipeContext(contextVendorModel, Lists.emptyList(), Lists.emptyList());
    }

    private ModelPipeContext createPipeContextForGeneratedModel() {
        ModelStorage.Model contextGeneratedModel = modelBuilder
            .startModel()
            .title("GENERATED")
            .currentType(CommonModel.Source.GENERATED)
            .source(CommonModel.Source.GENERATED)
            .id(11L)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_4", "http://url13")
            .vendorId(11L)
            .getRawModel().toBuilder()
            .setPublishedOnMarket(true)
            .build();

        return new ModelPipeContext(contextGeneratedModel, Lists.emptyList(), Lists.emptyList());
    }

    private ModelPipeContext createPipeContextWithSkus() {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_2", "http://url9")
            .picture("XL-Picture_3", "http://url5")
            .picture("XL-Picture_4", "http://url11")
            .vendorId(11L)
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url6")
            .picture(null, "http://url9")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue1")
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(6L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url9")
            .picture(null, "http://url8")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue5")
            .getRawModel();

        ModelStorage.Model contextSku3 = modelBuilder
            .startModel()
            .title("SKU3")
            .id(8L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url11")
            .picture(null, "http://url6")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue7")
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Collections.emptyList(),
            Arrays.asList(contextSku1, contextSku2, contextSku3));
    }

    private ModelPipeContext createPipeContextWithGeneratedModelAndGeneratesSkus() {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .id(1L).vendorId(11L).category(111L)
            .title("GENERATED_MODEL_1")
            .currentType(CommonModel.Source.GENERATED)
            .source(CommonModel.Source.GENERATED)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_2", "http://url9")
            .picture("XL-Picture_3", "http://url5")
            .picture("XL-Picture_4", "http://url11")
            .withSkuRelations(111L, 2L, 3L)
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .id(2L).vendorId(11L).category(111L)
            .title("GENERATED_SKU_1")
            .currentType(CommonModel.Source.GENERATED_SKU)
            .source(CommonModel.Source.GENERATED_SKU)
            .published(true)
            .withSkuParentRelation(contextModel)
            .picture(null, "http://url1")
            .picture(null, "http://url6")
            .picture(null, "http://url9")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue1")
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .id(3L).vendorId(11L).category(111L)
            .title("GENERATED_SKU_2")
            .currentType(CommonModel.Source.GENERATED_SKU)
            .source(CommonModel.Source.GENERATED_SKU)
            .published(true)
            .withSkuParentRelation(contextModel)
            .picture(null, "http://url9")
            .picture(null, "http://url8")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue5")
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Collections.emptyList(),
            Arrays.asList(contextSku1, contextSku2));
    }

    private ModelPipeContext createPipeContextWithSkusWithSizes(CategoryParam sizeParam, CategoryParam scaleParam) {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .vendorId(11L)
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue101")
            .param(sizeParam)
            .setOption(55432L)
            .param(scaleParam)
            .setOption(55430L)
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(6L)
            .vendorId(12L)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue102")
            .param(sizeParam)
            .setOption(55435L)
            .param(scaleParam)
            .setOption(55431L)
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Collections.emptyList(),
            Arrays.asList(contextSku1, contextSku2));
    }

    private CategoryInfo createCategoryInfo() {
        nameParam.setId(KnownIds.NAME_PARAM_ID);
        nameParam.setXslName("name");
        nameParam.addName(WordUtil.defaultWord("Name"));
        nameParam.setType(Param.Type.STRING);

        vendorParam.setId(KnownIds.VENDOR_PARAM_ID);
        vendorParam.setXslName("vendor");
        vendorParam.addName(WordUtil.defaultWord("Vendor"));
        vendorParam.setType(Param.Type.ENUM);

        Option vendorOption1 = new OptionImpl();
        vendorOption1.setId(11L);
        vendorOption1.setNames(WordUtil.defaultWords("Apple"));

        Option vendorOption2 = new OptionImpl();
        vendorOption2.setId(12L);
        vendorOption2.setNames(WordUtil.defaultWords("Socks company"));

        vendorParam.addOption(vendorOption1);
        vendorParam.addOption(vendorOption2);

        colorVendorParam.setId(14871214L);
        colorVendorParam.setXslName("color_vendor");
        colorVendorParam.addName(WordUtil.defaultWord("Color vendor"));
        colorVendorParam.setType(Param.Type.ENUM);
        colorVendorParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        colorVendorParam.setModelFilterIndex(0);

        Option color1 = new OptionImpl();
        color1.setId(101L);
        color1.setNames(WordUtil.defaultWords("Red"));

        Option color2 = new OptionImpl();
        color2.setId(102L);
        color2.setNames(WordUtil.defaultWords("Blue"));

        colorVendorParam.addOption(color1);
        colorVendorParam.addOption(color2);

        boolSkuParam.setId(123632L);
        boolSkuParam.setXslName("bool_sku_param");
        boolSkuParam.addName(WordUtil.defaultWord("Boolean SKU param"));
        boolSkuParam.setType(Param.Type.BOOLEAN);
        boolSkuParam.addOption(new OptionImpl(1, "TRUE"));
        boolSkuParam.addOption(new OptionImpl(2, "FALSE"));
        boolSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        boolSkuParam.setModelFilterIndex(-1);

        strSkuParam.setId(123639L);
        strSkuParam.setXslName("str_sku_param");
        strSkuParam.addName(WordUtil.defaultWord("String SKU param"));
        strSkuParam.setType(Param.Type.STRING);
        strSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        strSkuParam.setModelFilterIndex(5);

        strSkuParam2.setId(7979879L);
        strSkuParam2.setXslName("str_sku_param_2");
        strSkuParam2.addName(WordUtil.defaultWord("String SKU param 2"));
        strSkuParam2.setType(Param.Type.STRING);
        strSkuParam2.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        strSkuParam2.setModelFilterIndex(6);

        numericSkuParam.setId(43759L);
        numericSkuParam.setXslName("numeric_sku_param");
        numericSkuParam.addName(WordUtil.defaultWord("Numeric SKU param"));
        numericSkuParam.setType(Param.Type.NUMERIC);
        numericSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        numericSkuParam.setUnit(new Unit("Сантиметр", "см", BigDecimal.ONE, 1234L, 24898L));
        numericSkuParam.setModelFilterIndex(-1);

        isSkuParam.setId(43770L);
        isSkuParam.setXslName(XslNames.IS_SKU);
        isSkuParam.setType(Param.Type.BOOLEAN);
        isSkuParam.addOption(new OptionImpl(1, "TRUE"));
        isSkuParam.addOption(new OptionImpl(2, "FALSE"));
        isSkuParam.addName(WordUtil.defaultWord("Model is SKU"));

        return CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate(GURU_TITLE_TEMPLATE, SKU_TITLE_TEMPLATE, null, null, null, null),
            params, Collections.emptyList(), null, null
        );
    }

    private CategoryParam createScaleSkuParam() {
        CategoryParam scaleSkuParam = new Parameter();

        scaleSkuParam.setId(43761L);
        scaleSkuParam.setXslName("scale_sku_param");
        scaleSkuParam.addName(WordUtil.defaultWord("Scale SKU param"));
        scaleSkuParam.setType(Param.Type.ENUM);

        Option scale1 = new OptionImpl();
        scale1.setId(55430L);
        scale1.setNames(WordUtil.defaultWords("RU"));

        Option scale2 = new OptionImpl();
        scale2.setId(55431L);
        scale2.setNames(WordUtil.defaultWords("INT"));

        scaleSkuParam.addOption(scale1);
        scaleSkuParam.addOption(scale2);

        return scaleSkuParam;
    }

    private CategoryParam createSizeSkuParam() {
        CategoryParam sizeSkuParam = new Parameter();

        sizeSkuParam.setId(43760L);
        sizeSkuParam.setXslName("size_sku_param");
        sizeSkuParam.addName(WordUtil.defaultWord("Size SKU param"));
        sizeSkuParam.setType(Param.Type.ENUM);
        sizeSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        Option size1 = new OptionImpl();
        size1.setId(55432L);
        size1.setNames(WordUtil.defaultWords("46"));

        Option size2 = new OptionImpl();
        size2.setId(55433L);
        size2.setNames(WordUtil.defaultWords("48"));

        Option size3 = new OptionImpl();
        size3.setId(55434L);
        size3.setNames(WordUtil.defaultWords("L"));

        Option size4 = new OptionImpl();
        size4.setId(55435L);
        size4.setNames(WordUtil.defaultWords("XL"));

        sizeSkuParam.addOption(size1);
        sizeSkuParam.addOption(size2);
        sizeSkuParam.addOption(size3);
        sizeSkuParam.addOption(size4);

        return sizeSkuParam;
    }

    private List<SizeMeasureDto> createTestSizeMeasures() {
        GLMeasure sizeMeasure = new GLMeasure();
        sizeMeasure.setId(660L);
        sizeMeasure.setName("Размер одежды");
        sizeMeasure.setValueParamId(43760L);
        sizeMeasure.setUnitParamId(43761L);
        return Collections.singletonList(
            new SizeMeasureDto(sizeMeasure, null, "", ""));
    }
}
