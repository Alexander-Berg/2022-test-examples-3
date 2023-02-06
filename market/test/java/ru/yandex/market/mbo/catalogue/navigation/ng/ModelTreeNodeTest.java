package ru.yandex.market.mbo.catalogue.navigation.ng;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.db.GuruVendorsReaderStub;
import ru.yandex.market.mbo.db.SkuMappingsCountService;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.util.FacetHelper;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.security.SecManager;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelTreeNodeTest {

    private static final Long MODEL_ID = 1L;
    private static final Long CATEGORY_ID = 2L;
    private static final Long VENDOR_ID = 3L;
    private static final Long MODIFICATIONS_COUNT = 5L;
    private static final Long OPTION_ID = 6L;

    private static final Long EXISTING_MODEL_PARAM_ID = 7L;
    private static final Long NOT_PUBLISHED_PARAM_ID = 9L;
    private static final Long HIDDEN_PARAM_ID = 10L;
    private static final Long SERVICE_PARAM_ID = 11L;
    private static final Long NAME_PARAM_ID = 12L;
    private static final Long OTHER_PARAM_1_ID = 13L;
    private static final Long OTHER_PARAM_2_ID = 14L;

    //  name and one parameter is filled, two other parameters are not
    private static final int FILLED_PERCENT = 50;
    private static final String MODEL_NAME = "Test model";

    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private SkuMappingsCountService mappingsService;
    private ModelsReportable reportable;
    private CategoryEntities categoryEntities;

    @Before
    public void setUp() {
        CategoryInfo categoryInfo = mock(CategoryInfo.class);
        when(categoryInfo.getCategoryId()).thenReturn(CATEGORY_ID);
        categoryEntities = createCategoryEntities();
        when(categoryInfo.getParameters()).thenReturn(categoryEntities.getParameters().stream()
            .map(ForTitleParameter::fromCategoryParam).collect(Collectors.toList()));
        when(categoryInfo.getTitleTemplate()).thenReturn(
            "{\"delimiter\":\" \",\"values\":[[(1 ),(\"GENERATED\")],[(1 ),(v7 )],[(1 ),(t0 )]]}");
        when(mappingsService.getMappingsCount(anyList())).thenAnswer(args -> {
            List<Long> ids = args.getArgument(0);
            return ids.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> new SkuMappingsCountService.MappingsData(id.intValue(), id.intValue() + 1)
            ));
        });
        reportable = new ModelsReportable(
            new ModelsReportableServices()
                .setModelStorageService(mock(ModelStorageService.class))
                .setSecManager(mock(SecManager.class))
                .setGuruVendorsReader(new GuruVendorsReaderStub())
                .setCategoryMappingService(new CategoryMappingServiceMock())
                .setFacetHelper(mock(FacetHelper.class))
                .setGlobalVendorId(VENDOR_ID)
                .setParameterLoaderService(parameterLoaderService)
                .setMappingsService(mappingsService)
                .setCategoryInfo(categoryInfo)
        );
    }

    @Test
    public void testGuruModel() {
        CommonModel model = createModelBuilder()
            .published(true)
            .publishedOnBlue(true)
            .getModel();

        ModelTreeNode node = new ModelTreeNode(model, MODIFICATIONS_COUNT);
        StringBuilder result = new StringBuilder();
        node.toXml(result);
        String expected = new ResultXmlBuilder()
            .title(MODEL_NAME)
            .published(true)
            .publishedOnBlue(true)
            .modifications(MODIFICATIONS_COUNT)
            .generatedName(MODEL_NAME)
            .modelQuality("some")
            .build();

        assertEquals(expected, result.toString());
    }

    @Test
    public void testModification() {
        CommonModel model = createModelBuilder()
            .published(false)
            .startParameterValue()
            .paramId(OPTION_ID).xslName(XslNames.OPERATOR_SIGN).booleanValue(true, OPTION_ID)
            .endParameterValue()
            .startModelRelation()
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .startModelRelation()
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .getModel();

        ModelTreeNode node = new ModelTreeNode(model, 0L);
        StringBuilder result = new StringBuilder();
        node.toXml(result);
        String expected = new ResultXmlBuilder()
            .title(MODEL_NAME)
            .signature(true)
            .skus(2)
            .generatedName(MODEL_NAME)
            .modelQuality("some")
            .build();

        assertEquals(expected, result.toString());
    }

    @Test
    public void testIsSkuModel() {
        CommonModel model = createModelBuilder()
            .startParameterValue()
            .paramId(OPTION_ID).xslName(XslNames.IS_SKU).booleanValue(true, OPTION_ID)
            .endParameterValue()
            .getModel();

        ModelTreeNode node = new ModelTreeNode(model, 0L);
        StringBuilder result = new StringBuilder();
        node.toXml(result);
        String expected = new ResultXmlBuilder()
            .title(MODEL_NAME)
            .generatedName(MODEL_NAME)
            .skus(1)
            .isSku(true)
            .modelQuality("some")
            .build();

        assertEquals(expected, result.toString());
    }

    @Test
    public void testExperimental() {
        CommonModel model = createModelBuilder()
            .currentType(CommonModel.Source.EXPERIMENTAL)
            .experimentFlag("testexp")
            .getModel();

        ModelTreeNode node = new ModelTreeNode(model, MODIFICATIONS_COUNT);
        StringBuilder result = new StringBuilder();
        node.toXml(result);
        String expected = new ResultXmlBuilder()
            .title(MODEL_NAME + " (testexp)")
            .modifications(MODIFICATIONS_COUNT)
            .generatedName(MODEL_NAME + " (testexp)")
            .modelQuality("some")
            .currentType(CommonModel.Source.EXPERIMENTAL)
            .build();

        assertEquals(expected, result.toString());
    }

    @Test
    public void testEnrichModelParameters() {
        CommonModel model = createModelBuilder().getModel();

        ModelStorage.Model proto = ModelStorage.Model.newBuilder()
            .setId(MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(VENDOR_ID)
            .setCurrentType("GURU")
            .setSourceType("GENERATED")
            .addPictures(ModelStorage.Picture.newBuilder()
                .setXslName("XL-Picture")
                .setUrl("test-pic-url"))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(EXISTING_MODEL_PARAM_ID)
                .setXslName("ExistsInModel")
                .setBoolValue(true))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(NAME_PARAM_ID)
                .setXslName(XslNames.NAME)
                .addAllStrValue(Collections.singleton(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru")
                    .setValue(MODEL_NAME).build()))
                .setBoolValue(true)).build();


        ModelTreeNode node = new ModelTreeNode(model, 0L);
        StringBuilder resultBefore = new StringBuilder();
        node.toXml(resultBefore);
        String expected = new ResultXmlBuilder()
            .title(MODEL_NAME)
            .generatedName(MODEL_NAME)
            .modelQuality("some")
            .build();

        assertFalse(node.hasPicture());
        assertEquals(node.getPictureUrl(), "");
        assertEquals(node.getFilledPercent(), 0);
        assertEquals(node.getGeneratedName(), MODEL_NAME);
        assertFalse(node.isEnriched());
        assertEquals(expected, resultBefore.toString());

        when(parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID)).thenReturn(categoryEntities);
        reportable.enrichModelTreeNodes(Collections.singletonList(node), ImmutableMap.of(MODEL_ID, proto));

        expected = new ResultXmlBuilder()
            .title(MODEL_NAME)
            .generatedName("GENERATED true " + MODEL_NAME)
            .picUrl("test-pic-url")
            .filledPercent(FILLED_PERCENT)
            .blueMappings((int) model.getId())
            .whiteMappings((int) model.getId() + 1)
            // Оператор
            .modelQuality("&#1054;&#1087;&#1077;&#1088;&#1072;&#1090;&#1086;&#1088;")
            .build();

        StringBuilder resultAfter = new StringBuilder();
        node.toXml(resultAfter);

        assertTrue(node.hasPicture());
        assertEquals(node.getPictureUrl(), "test-pic-url");
        assertEquals(FILLED_PERCENT, node.getFilledPercent());
        assertEquals("GENERATED true " + MODEL_NAME, node.getGeneratedName());
        assertTrue(node.isEnriched());
        assertEquals(node.getBlueMappings(), model.getId()); //mock method returns modelId as number of mappings
        assertEquals(node.getWhiteMappings(), model.getId() + 1);
        assertEquals(expected, resultAfter.toString());
    }

    private CategoryEntities createCategoryEntities() {
        CategoryEntities result = new CategoryEntities();
        //  parameters in category, checking that only published, !hidden, !service stay
        Parameter hidden = new Parameter();
        hidden.setId(HIDDEN_PARAM_ID);
        hidden.setXslName("Hidden");
        hidden.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Скрытый")));
        hidden.setPublished(true);
        hidden.setHidden(true);
        hidden.setType(Param.Type.BOOLEAN);
        result.addParameter(hidden);

        Parameter service = new Parameter();
        service.setId(SERVICE_PARAM_ID);
        service.setXslName("Service");
        service.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Служебный")));
        service.setPublished(true);
        service.setService(true);
        service.setType(Param.Type.BOOLEAN);
        result.addParameter(service);

        Parameter notPublished = new Parameter();
        notPublished.setId(NOT_PUBLISHED_PARAM_ID);
        notPublished.setXslName("NotPublished");
        notPublished.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID,
            "Не опубликованный")));
        notPublished.setPublished(false);
        notPublished.setType(Param.Type.BOOLEAN);
        result.addParameter(notPublished);

        Parameter existingValue = new Parameter();
        existingValue.setId(EXISTING_MODEL_PARAM_ID);
        existingValue.setXslName("ExistsInModel");
        existingValue.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "В модели")));
        existingValue.setPublished(true);
        existingValue.setType(Param.Type.BOOLEAN);
        result.addParameter(existingValue);

        Parameter otherParameter1 = new Parameter();
        otherParameter1.setId(OTHER_PARAM_1_ID);
        otherParameter1.setXslName("OtherParameter1");
        otherParameter1.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Другой 1")));
        otherParameter1.setPublished(true);
        otherParameter1.setType(Param.Type.BOOLEAN);
        result.addParameter(otherParameter1);

        Parameter otherParameter2 = new Parameter();
        otherParameter2.setId(OTHER_PARAM_2_ID);
        otherParameter2.setXslName("OtherParameter2");
        otherParameter2.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Другой 2")));
        otherParameter2.setPublished(true);
        otherParameter2.setType(Param.Type.BOOLEAN);
        result.addParameter(otherParameter2);

        Parameter name = new Parameter();
        name.setId(NAME_PARAM_ID);
        name.setXslName(XslNames.NAME);
        name.setLocalizedNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Имя")));
        name.setPublished(true);
        name.setType(Param.Type.STRING);
        result.addParameter(name);

        return result;
    }

    private CommonModelBuilder<?> createModelBuilder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(MODEL_ID)
            .category(CATEGORY_ID)
            .title(MODEL_NAME)
            .source(CommonModel.Source.GENERATED)
            .currentType(CommonModel.Source.GURU)
            .vendorId(VENDOR_ID);
    }

    private class ResultXmlBuilder {
        private String template =
            "<tree-node><name>%1$s</name>\n" +
                "<_id>" + MODEL_ID + "</_id>\n" +
                "<_type>10002</_type>\n" +
                "<_root>0</_root>\n" +
                "<_leaf>0</_leaf>\n" +
                "<_actions></_actions>\n" +
                "<publish_level>%2$d</publish_level>\n" +
                "<published-on-blue>%3$b</published-on-blue>\n" +
                "<OperatorComment></OperatorComment>\n" +
                "<modifications>%4$d</modifications>\n" +
                "<skus>%5$d</skus>\n" +
                "<isSku>%6$b</isSku>\n" +
                "<blue-mappings>%7$d</blue-mappings>\n" +
                "<white-mappings>%8$d</white-mappings>\n" +
                "<hid>2</hid>\n" +
                "<global-vendor-id>" + VENDOR_ID + "</global-vendor-id>\n" +
                "<source-type>GENERATED</source-type>\n" +
                "<current-type>%14$s</current-type>\n" +
                "<DBFilledOK>%9$d</DBFilledOK>\n" +
                "<pic-url>%10$s</pic-url>\n" +
                "<generated-name>%11$s</generated-name>\n" +
                "<filled-percent>%12$d</filled-percent>\n" +
                "<model-quality>%13$s</model-quality>\n" +
                "</tree-node>";

        private String title = "";
        private boolean published;
        private boolean publishedOnBlue;
        private boolean signature;
        private long modifications;
        private int skus;
        private int blueMappings;
        private int whiteMappings;
        private boolean isSku;
        private String picUrl = "";
        private String generatedName = "";
        private int filledPercent;
        private String modelQuality;
        private CommonModel.Source currentType = CommonModel.Source.GURU;

        public ResultXmlBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ResultXmlBuilder published(boolean published) {
            this.published = published;
            return this;
        }

        public ResultXmlBuilder publishedOnBlue(boolean publishedOnBlue) {
            this.publishedOnBlue = publishedOnBlue;
            return this;
        }

        public ResultXmlBuilder signature(boolean signature) {
            this.signature = signature;
            return this;
        }

        public ResultXmlBuilder modifications(long modifications) {
            this.modifications = modifications;
            return this;
        }

        public ResultXmlBuilder skus(int skus) {
            this.skus = skus;
            return this;
        }

        public ResultXmlBuilder blueMappings(int mappings) {
            this.blueMappings = mappings;
            return this;
        }

        public ResultXmlBuilder whiteMappings(int mappings) {
            this.whiteMappings = mappings;
            return this;
        }

        public ResultXmlBuilder isSku(boolean isSku) {
            this.isSku = isSku;
            return this;
        }

        public ResultXmlBuilder picUrl(String picUrl) {
            this.picUrl = picUrl;
            return this;
        }

        public ResultXmlBuilder generatedName(String generatedName) {
            this.generatedName = generatedName;
            return this;
        }

        public ResultXmlBuilder filledPercent(int filledPercent) {
            this.filledPercent = filledPercent;
            return this;
        }

        public ResultXmlBuilder modelQuality(String modelQuality) {
            this.modelQuality = modelQuality;
            return this;
        }

        public ResultXmlBuilder currentType(CommonModel.Source currentType) {
            this.currentType = currentType;
            return this;
        }

        public String build() {
            return String.format(template,
                title,
                published ? 2 : 1,
                publishedOnBlue,
                modifications,
                skus,
                isSku,
                blueMappings,
                whiteMappings,
                signature ? 1 : 0,
                picUrl,
                generatedName,
                filledPercent,
                modelQuality,
                currentType.name());
        }
    }

}
