package ru.yandex.market.mbo.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageInternalService;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ModelTitle;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplateValidationResult;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplateView;
import ru.yandex.market.mbo.gwt.models.titlemaker.TemplateType;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.toolkit.shared.models.JavaScriptTemplate;
import ru.yandex.market.mbo.toolkit.shared.models.OutputBlock;
import ru.yandex.market.mbo.user.AutoUser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author dmserebr
 * @date 19.11.18
 */
@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TitleMakerTemplateValidationServiceTest {

    private static final long CATEGORY_HID = 1;
    private static final String MODEL_NAME_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";
    private static final String MODEL_NAME_BAD_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 ),null,(true],[(1 ),(t0 ),null,(true)]]";
    private static final String INVALID_CODE_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(v13728051 ),(v13728051)],[(v7893318 ),(v7893318 ),null,(true)]," +
            "[(t0 ),(t0 ),null,(true)],[(1 ),(true = v15108462 ),null,(true)]]}";

    private static final String SIMPLE_GURU_TEMPLATE = "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 )," +
        "null,(true)],[(1 ),(t0 ),null,(true)]]}";
    private static final String GURU_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 ),null,(true)]," +
            "[(1 ),(t0 ),null,(true)],[(1 ),(v6185723 ),null,(true)],[(1 ),(v6531685 )]]}";

    private TitleMakerTemplateValidationService templateValidationService;

    @Mock
    private TovarTreeService tovarService;

    @Mock
    private VisualService vs;

    @Mock
    private ModelStorageInternalService modelStorageInternalService;

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private SizeMeasureService sizeMeasureService;

    @Mock
    private IParameterLoaderService parameterLoader;
    @Mock
    private ValueLinkServiceInterface valueLinkService;

    @Mock
    private AutoUser autoUser;

    @Before
    public void init() {
        templateValidationService = new TitleMakerTemplateValidationService();
        templateValidationService.setModelStorageInternalService(modelStorageInternalService);
        templateValidationService.setModelStorageService(modelStorageService);
        templateValidationService.setParameterLoaderService(parameterLoader);
        templateValidationService.setTovarTreeService(tovarService);
        templateValidationService.setSizeMeasureService(sizeMeasureService);
        templateValidationService.setValueLinkService(valueLinkService);
        templateValidationService.setAutoUser(autoUser);
    }

    @Test
    public void testSaveGoodJsTemplate() {
        TMTemplateView template = new TMTemplateView();

        OutputBlock block = new OutputBlock();
        block.setOutput("output");
        block.setCondition("condition");
        template.setTemplateForTesting("value", new JavaScriptTemplate(block), TemplateType.TITLE);

        String result = templateValidationService.validateMandatoryFields(template);
        assertNull(result);
    }

    @Test
    public void testTemplateSetting() {
        TMTemplateView template = new TMTemplateView();
        for (TemplateType templateType : TemplateType.values()) {
            template.setTemplate(templateType.name(), templateType);
        }
        assertEquals(TemplateType.TITLE.name(), template.getTmTemplate().getValue());
        assertEquals(TemplateType.SKU_TITLE.name(), template.getTmTemplate().getSkuTemplate());
        assertEquals(TemplateType.GURU_TITLE.name(), template.getTmTemplate().getGuruTemplate());
        assertEquals(TemplateType.BLUE_GROUPING_TITLE.name(), template.getTmTemplate().getBlueGroupingTemplate());
    }

    @Test
    public void testSaveBadJsTemplate() {
        TMTemplateView template = new TMTemplateView();

        template.setTemplateForTesting("value", new JavaScriptTemplate(new OutputBlock()),
            TemplateType.TITLE);

        String result = templateValidationService.validateMandatoryFields(template);
        assertTrue(result.contains("Не указаны обязательные поля для некоторых блоков."));
    }

    @Test
    public void testSaveBadGuruTemplate() {
        TMTemplateView template = new TMTemplateView();
        template.getTmTemplate().setHasGuruTemplate(true);
        template.setTemplateForTesting("template", new JavaScriptTemplate(new OutputBlock()),
            TemplateType.GURU_TITLE);

        String result = templateValidationService.validateMandatoryFields(template);
        assertTrue(result.contains("Не указаны обязательные поля для некоторых блоков."));
    }

    @Test
    public void testSaveBadSkuTemplate() {
        TMTemplateView template = new TMTemplateView();

        template.setTemplateForTesting("template", new JavaScriptTemplate(new OutputBlock()),
            TemplateType.SKU_TITLE);
        String result = templateValidationService.validateMandatoryFields(template);
        assertTrue(result.contains("Не указаны обязательные поля для некоторых блоков."));
    }

    @Test
    public void testValidateTemplateStructure() {
        TMTemplateView templateView = new TMTemplateView();
        templateView.setHasGuruTemplate(true);
        for (TemplateType templateType : TemplateType.values()) {
            templateView.setTemplate(MODEL_NAME_TEMPLATE, templateType);
        }
        String result = templateValidationService.validateTemplateStructure(templateView.getTmTemplate());

        assertNull(result);
    }

    @Test
    public void testValidateInvalidTemplateStructure() {
        TMTemplate template = new TMTemplate();
        template.setValue(INVALID_CODE_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateInvalidGuruTemplate() {
        TMTemplate template = new TMTemplate();
        template.setHasGuruTemplate(true);
        template.setGuruTemplate(INVALID_CODE_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateInvalidBlueGroupingTemplateStructure() {
        TMTemplate template = new TMTemplate();
        template.setBlueGroupingTemplate(INVALID_CODE_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateInvalidSkuTemplate() {
        TMTemplate template = new TMTemplate();
        template.setSkuTemplate(INVALID_CODE_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateBadTemplate() {
        TMTemplate template = new TMTemplate();
        template.setValue(MODEL_NAME_BAD_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateBadGuruTemplate() {
        TMTemplate template = new TMTemplate();
        template.setHasGuruTemplate(true);
        template.setGuruTemplate(MODEL_NAME_BAD_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    public void testValidateBadBlueGroupTemplate() {
        TMTemplate template = new TMTemplate();
        template.setBlueGroupingTemplate(MODEL_NAME_BAD_TEMPLATE);

        String result = templateValidationService.validateTemplateStructure(template);

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("CheckStyle")
    public void testMakeExampleRandomModelsTitles() {
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        CategoryParam vendor = TitleMakerTemplateServiceTestHelper.createParameter(
            1, "vendor", Param.Type.ENUM, true, true, true);
        Option vendorOpt = new OptionImpl(5L, "Vendor");
        Option localVendor = new OptionImpl(vendorOpt, Option.OptionType.VENDOR);
        vendor.addOption(localVendor);
        entities.addParameter(vendor);
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            2, "name", Param.Type.STRING, true, false, false));
        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);

        CommonModel model = new CommonModel();
        model.setSource(CommonModel.Source.GURU);
        model.setCurrentType(CommonModel.Source.GURU);

        TMTemplateView templateView = new TMTemplateView();
        templateView.setTemplate(MODEL_NAME_TEMPLATE, TemplateType.SKU_TITLE);

        List<ModelTitle> result = templateValidationService.getExampleTitles(
            CATEGORY_HID, templateView, 1,
            new HashSet<>(Collections.singleton(TemplateType.SKU_TITLE)), true)
            .getGoodTitles(TemplateType.SKU_TITLE);

        assertEquals(1, result.size());
        assertEquals("Vendor name", result.get(0).getTitle());
    }

    @Test
    public void testBlueGroupingValidation() {
        TMTemplate template = new TMTemplate();
        template.setHid(CATEGORY_HID);
        template.setBlueGroupingTemplate(SIMPLE_GURU_TEMPLATE);
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        CategoryParam param1 = TitleMakerTemplateServiceTestHelper.createParameter(
            123456, "param1", Param.Type.ENUM, true, true, true);
        param1.setBlueGrouping(true);

        CategoryParam param2 = TitleMakerTemplateServiceTestHelper.createParameter(
            7893318, "param2", Param.Type.ENUM, true, false, false);
        param2.setBlueGrouping(true);

        entities.addAllParameters(Arrays.asList(param1, param2));
        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);

        TMTemplateValidationResult result = templateValidationService.validateBlueGrouping(template);
        assertNotNull(result);
        assertThat(result.getTemplateValidationResult(),
            Matchers.allOf(Matchers.notNullValue(),
                StringContains.containsString(param1.getName()),
                Matchers.not(StringContains.containsString(param2.getName()))));
    }

    @Test
    public void testRandomSampleNotLoadingSameTypeTwice() {
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        CategoryParam vendor = TitleMakerTemplateServiceTestHelper.createParameter(
            1, "vendor", Param.Type.ENUM, true, true, true);
        Option vendorOpt = new OptionImpl(5L, "Vendor");
        Option localVendor = new OptionImpl(vendorOpt, Option.OptionType.VENDOR);
        localVendor.setPublished(true);
        vendor.addOption(localVendor);
        entities.addParameter(vendor);

        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);

        when(tovarService.loadCategoryByHid(eq(CATEGORY_HID))).thenReturn(
            TovarCategoryBuilder.newBuilder(1, CATEGORY_HID).setGroup(true).setGuruCategoryId(10L)
                .addShowModelType(CommonModel.Source.GURU).create());

        CommonModel guru = CommonModelBuilder.newBuilder()
            .category(1L)
            .id(1000L).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .title("GURU").published(true)
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                1, "vendor", Param.Type.ENUM, 5L, null))
            .modelRelation(2000L, 1L, ModelRelation.RelationType.SKU_MODEL)
            .endModel();

        CommonModel sku = CommonModelBuilder.newBuilder()
            .category(1L)
            .id(2000L).source(CommonModel.Source.SKU).currentType(CommonModel.Source.SKU)
            .title("SKU").published(true)
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                1, "vendor", Param.Type.ENUM, 5L, null))
            .modelRelation(1000L, 1L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModel();

        when(sizeMeasureService.listSizeMeasures(anyLong())).thenReturn(new ArrayList<>());

        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())
        ).then(args -> {
            CommonModel.Source type = args.getArgument(2);
            if (type == CommonModel.Source.SKU) {
                return Collections.singletonList(sku);
            }
            return Collections.emptyList();
        });

        doAnswer(i -> {
            Consumer<ModelStorage.Model> consumer = i.getArgument(2);
            consumer.accept(ModelProtoConverter.convert(guru));
            return null;
        }).when(modelStorageService).processQueryFullModels(anyLong(), any(MboIndexesFilter.class),
            any(Consumer.class));

        TMTemplateView templateView = new TMTemplateView();
        templateView.setTemplate(MODEL_NAME_TEMPLATE, TemplateType.SKU_TITLE);
        templateView.setTemplate(MODEL_NAME_TEMPLATE, TemplateType.BLUE_GROUPING_TITLE);

        TMTemplateValidationResult result = templateValidationService.validateTemplateBeforeSave(
            CATEGORY_HID, templateView, 5,
            ImmutableSet.of(TemplateType.SKU_TITLE, TemplateType.BLUE_GROUPING_TITLE));

        Assertions.assertThat(result.isValid()).isTrue();
    }
}

