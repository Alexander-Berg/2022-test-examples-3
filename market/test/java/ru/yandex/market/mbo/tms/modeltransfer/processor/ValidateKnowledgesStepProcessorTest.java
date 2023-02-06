package ru.yandex.market.mbo.tms.modeltransfer.processor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.TitleMakerTemplateServiceTestHelper;
import ru.yandex.market.mbo.db.TitleMakerTemplateValidationService;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageInternalService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.templates.OutputTemplateService;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.transfer.DestinationCategory;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.SourceCategory;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryEntitiesResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryEntityResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryEntityType;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelTransferList;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.models.visual.OutputTemplateBuilder;
import ru.yandex.market.mbo.gwt.models.visual.OutputTemplatesBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.gwt.models.visual.templates.RangeField;
import ru.yandex.market.mbo.gwt.models.visual.templates.RangeFieldList;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.user.AutoUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author dmserebr
 * @date 20.11.18
 */
@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.class)
public class ValidateKnowledgesStepProcessorTest {

    private static final String MODEL_NAME_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 ),null,(true)],[(1 ),(t0 ),null,(true)],[(1 ),(v3 ),null,(true)]]}";
    private static final long SOURCE_CAT_1 = 9L;
    private static final long SOURCE_CAT_2 = 10L;
    private static final long TARGET_CAT_1 = 11L;
    private static final long TARGET_CAT_2 = 12L;

    @Mock
    private TovarTreeService tovarTreeService;

    @Mock
    private TitlemakerTemplateDao titlemakerTemplateDao;

    @Mock
    private ModelFormService modelFormService;

    @Mock
    private OutputTemplateService outputTemplateService;

    @Mock
    private ModelStorageInternalService modelStorageInternalService;

    @Mock
    private SizeMeasureService sizeMeasureService;

    @Mock
    private IParameterLoaderService parameterLoader;

    @Mock
    private ValueLinkServiceInterface valueLinkService;

    @Mock
    private AutoUser autoUser;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TitleMakerTemplateValidationService titleMakerTemplateValidationService;

    private ValidateKnowledgesStepProcessor stepProcessor;

    @Before
    public void before() {
        titleMakerTemplateValidationService = new TitleMakerTemplateValidationService();
        titleMakerTemplateValidationService.setModelStorageInternalService(modelStorageInternalService);
        titleMakerTemplateValidationService.setParameterLoaderService(parameterLoader);
        titleMakerTemplateValidationService.setTovarTreeService(tovarTreeService);
        titleMakerTemplateValidationService.setSizeMeasureService(sizeMeasureService);
        titleMakerTemplateValidationService.setValueLinkService(valueLinkService);
        titleMakerTemplateValidationService.setAutoUser(autoUser);

        stepProcessor = new ValidateKnowledgesStepProcessor(
            tovarTreeService, titlemakerTemplateDao, modelFormService, outputTemplateService,
            titleMakerTemplateValidationService);

        initCategoryEntities();
    }

    @Test
    public void testEverythingEmptyNonGroupCategory() {
        Mockito.doReturn(TovarCategoryBuilder.newBuilder().setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createEmptyTitlemakerTemplate())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createEmptyOutputTemplate())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(null)
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(8, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен основной шаблон для карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для тач-карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон карточки для колдунщика");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для поисковой карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для дружественной карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для SEO");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "не задан шаблон операторской карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "не задан шаблон тайтлов для гуру-карточек");
    }

    @Test
    public void testEverythingEmptyGroupCategory() {
        Mockito.doReturn(TovarCategoryBuilder.newBuilder().setGroup(true).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createEmptyTitlemakerTemplate())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createEmptyOutputTemplate())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(null)
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(10, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен основной шаблон для карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для тач-карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон карточки для колдунщика");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для поисковой карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для дружественной карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для SEO");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для групповой карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнены Ranged fields");
        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "не задан шаблон операторской карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(9), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "не задан шаблон тайтлов для гуру-карточек");
    }

    @Test
    public void testPartiallyFilled() {
        CategoryEntities entities = new CategoryEntities(TARGET_CAT_1, Collections.emptyList());
        when(parameterLoader.loadCategoryEntitiesByHid(TARGET_CAT_1)).thenReturn(entities);

        Mockito.doReturn(TovarCategoryBuilder.newBuilder().setGroup(true).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createPartiallyFilledOutputTemplate())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(null)
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(10, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "основной шаблон для карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для тач-карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон карточки для колдунщика");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для поисковой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для дружественной карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для SEO");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнен шаблон для групповой карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "не заполнены Ranged fields");
        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "не задан шаблон операторской карточки");
        assertCategoryEntityResultEntry(result.getResultEntries().get(9), TARGET_CAT_1, ResultEntry.Status.WARNING,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "предупреждение при сохранении шаблонов тайтлов для категории 11: " +
                "не сгенерирован ни один пример тайтла, не удается проверить корректность шаблона");
    }

    @Test
    public void testEverythingFilledNonGroupCategory() {
        // Add model with all filled params
        CommonModel model = createTestModel();
        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(Collections.singletonList(model));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals(8, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "основной шаблон для карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для тач-карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон карточки для колдунщика заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для поисковой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для дружественной карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для SEO заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "шаблон операторской карточки задан");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "валидация шаблона тайтлов типа \"гуру-шаблон\" прошла успешно");
    }

    @Test
    public void testEverythingFilledGroupCategory() {
        // Add model with all filled params + its modification
        CommonModel model = createTestModel();
        CommonModel modification = createTestModification();
        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(Arrays.asList(model, modification));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(true).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createFilledOutputTemplateGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals(10, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "основной шаблон для карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для тач-карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон карточки для колдунщика заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для поисковой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для дружественной карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для SEO заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для групповой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "Ranged fields заполнены");
        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "шаблон операторской карточки задан");
        assertCategoryEntityResultEntry(result.getResultEntries().get(9), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "валидация шаблона тайтлов типа \"гуру-шаблон\" прошла успешно");
    }

    @Test
    public void testFailedTitlesGenerated() {
        // Add model with missing param testParam
        CommonModel model = createTestModel(false);
        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(Collections.singletonList(model));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(8, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "ошибка валидации шаблонов тайтлов категории 11 - найдены ошибочные модели. " +
                "Убедитесь, что шаблоны возможно пересохранить на странице \"Знания\"");
    }

    @Test
    public void testNoTitlesGenerated() {
        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.anyLong());
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals(8, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.WARNING,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "предупреждение при сохранении шаблонов тайтлов для категории 11: " +
                "не сгенерирован ни один пример тайтла, не удается проверить корректность шаблона");
    }

    @Test
    public void testSkuTemplateIsMissing() {
        CommonModel model = createTestModel();
        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(Collections.singletonList(model));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.anyLong());
        Mockito.doReturn(createTitlemakerTemplateWithGuruAndSku())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(SOURCE_CAT_1));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.anyLong());
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.anyLong(), Mockito.any());

        ListOfModelParameterLandingConfig config = config(SOURCE_CAT_1, TARGET_CAT_1, 1L);
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(9, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "валидация шаблона тайтлов типа \"гуру-шаблон\" прошла успешно");
        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "не задан шаблон тайтлов для SKU (и в исходных категориях он задан)");
    }

    @Test
    public void testValidateMultipleCategories() {
        // Add model with all filled params
        CommonModel model = createTestModel();
        CommonModel modelAnotherCategory = createTestModelAnotherCategory();
        when(modelStorageInternalService.getModelsRandomSample(
            Mockito.eq(TARGET_CAT_1), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(),
            anyBoolean(), anyBoolean())
        ).thenReturn(Collections.singletonList(model));
        when(modelStorageInternalService.getModelsRandomSample(
            Mockito.eq(TARGET_CAT_2), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(),
            anyBoolean(), anyBoolean())
        ).thenReturn(Collections.singletonList(modelAnotherCategory));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.eq(TARGET_CAT_1), Mockito.eq(FormType.MODEL_EDITOR));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_2).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.eq(TARGET_CAT_2), Mockito.eq(FormType.MODEL_EDITOR));

        ListOfModelParameterLandingConfig config = config(ListOfModelsConfigBuilder.newBuilder()
            .models(SOURCE_CAT_1, TARGET_CAT_1, 1L)
            .models(SOURCE_CAT_1, TARGET_CAT_2, 3L)
        );
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals(16, result.getResultEntries().size());
        assertCategoryEntityResultEntry(result.getResultEntries().get(0), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "основной шаблон для карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(1), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для тач-карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(2), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон карточки для колдунщика заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(3), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для поисковой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(4), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для дружественной карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(5), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для SEO заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(6), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "шаблон операторской карточки задан");
        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.SUCCESS,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "валидация шаблона тайтлов типа \"гуру-шаблон\" прошла успешно");

        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "основной шаблон для карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(9), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для тач-карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(10), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон карточки для колдунщика заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(11), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для поисковой карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(12), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для дружественной карточки заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(13), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_CARD_TEMPLATE, "шаблон для SEO заполнен");
        assertCategoryEntityResultEntry(result.getResultEntries().get(14), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.MODEL_FORM_TEMPLATE, "шаблон операторской карточки задан");
        assertCategoryEntityResultEntry(result.getResultEntries().get(15), TARGET_CAT_2, ResultEntry.Status.SUCCESS,
            CategoryEntityType.TITLEMAKER_TEMPLATE, "валидация шаблона тайтлов типа \"гуру-шаблон\" прошла успешно");
    }

    @Test
    public void testAffectingSourceCategoriesForSkuTemplateValidation() {
        // Source category 1 affects only target category 1, source category 2 affects both target categories 1 & 2
        // SKU template is present in source category 1 - need to validate it only in target category 1 (not in 2)
        Mockito.doReturn(createTitlemakerTemplateWithGuruAndSku())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(SOURCE_CAT_1));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(SOURCE_CAT_2));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_1).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.eq(TARGET_CAT_1));
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.eq(TARGET_CAT_1), Mockito.eq(FormType.MODEL_EDITOR));

        Mockito.doReturn(TovarCategoryBuilder.newBuilder()
            .setHid(TARGET_CAT_2).addShowModelType(CommonModel.Source.GURU).setGroup(false).create())
            .when(tovarTreeService).loadCategoryByHid(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createTitlemakerTemplateWithGuru())
            .when(titlemakerTemplateDao).loadTemplateByHid(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createFilledOutputTemplateNonGroupCategory())
            .when(outputTemplateService).getTemplates(Mockito.eq(TARGET_CAT_2));
        Mockito.doReturn(createModelForm())
            .when(modelFormService).getModelForm(Mockito.eq(TARGET_CAT_2), Mockito.eq(FormType.MODEL_EDITOR));

        ListOfModelParameterLandingConfig config = config(ListOfModelsConfigBuilder.newBuilder()
            .models(SOURCE_CAT_1, TARGET_CAT_1, 1L)
            .models(SOURCE_CAT_2, TARGET_CAT_1, 4L)
            .models(SOURCE_CAT_2, TARGET_CAT_2, 3L)
        );
        CategoryEntitiesResult result = stepProcessor.executeStep(new ResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(17, result.getResultEntries().size());

        assertCategoryEntityResultEntry(result.getResultEntries().get(7), TARGET_CAT_1, ResultEntry.Status.WARNING,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "предупреждение при сохранении шаблонов тайтлов для категории 11: " +
                "не сгенерирован ни один пример тайтла, не удается проверить корректность шаблона");
        assertCategoryEntityResultEntry(result.getResultEntries().get(8), TARGET_CAT_1, ResultEntry.Status.FAILURE,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "не задан шаблон тайтлов для SKU (и в исходных категориях он задан)");
        assertCategoryEntityResultEntry(result.getResultEntries().get(16), TARGET_CAT_2, ResultEntry.Status.WARNING,
            CategoryEntityType.TITLEMAKER_TEMPLATE,
            "предупреждение при сохранении шаблонов тайтлов для категории 12: " +
                "не сгенерирован ни один пример тайтла, не удается проверить корректность шаблона");
    }

    private void assertCategoryEntityResultEntry(CategoryEntityResultEntry entry,
                                                 long categoryId,
                                                 ResultEntry.Status status,
                                                 CategoryEntityType entityType, String statusMessage) {
        Assert.assertEquals(categoryId, entry.getCategoryId());
        Assert.assertEquals(status, entry.getStatus());
        Assert.assertEquals(entityType, entry.getEntityType());
        Assert.assertEquals(statusMessage, entry.getStatusMessage());
    }

    private OutputTemplates createEmptyOutputTemplate() {
        return OutputTemplatesBuilder.newBuilder()
            .putModelTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .putBriefModelTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .putMicroModelTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .putMicroModelSearchTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .putFriendlyModelTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .seoTemplate(OutputTemplateBuilder.newBuilder().content("").build())
            .designGroupParams(OutputTemplateBuilder.newBuilder().content("").build())
            .rangeFields(new RangeFieldList(Collections.emptyList()))
            .build();
    }

    private OutputTemplates createPartiallyFilledOutputTemplate() {
        return OutputTemplatesBuilder.newBuilder()
            .putModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("modelTemplate").build())
            .putMicroModelSearchTemplate(OutputTemplateBuilder.newBuilder()
                .content("microModelSearchTemplate").build())
            .putFriendlyModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("friendlyModelTemplate").build())
            .build();
    }

    private OutputTemplates createFilledOutputTemplateNonGroupCategory() {
        return OutputTemplatesBuilder.newBuilder()
            .putModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("modelTemplate").build())
            .putBriefModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("briefModelTemplate").build())
            .putMicroModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("microModelTemplate").build())
            .putMicroModelSearchTemplate(OutputTemplateBuilder.newBuilder()
                .content("microModelSearchTemplate").build())
            .putFriendlyModelTemplate(OutputTemplateBuilder.newBuilder()
                .content("friendlyModelTemplate").build())
            .seoTemplate(OutputTemplateBuilder.newBuilder()
                .content("seoTemplate").build())
            .build();
    }

    private OutputTemplates createFilledOutputTemplateGroupCategory() {
        RangeField field1 = new RangeField();
        field1.setIndex(1);
        field1.setParameterId(1234L);
        RangeField field2 = new RangeField();
        field2.setIndex(2);
        field2.setParameterId(5678L);

        OutputTemplates nonGroupTemplates = createFilledOutputTemplateNonGroupCategory();
        nonGroupTemplates.setDesignGroupParams(OutputTemplateBuilder.newBuilder()
            .content("designGroupParams").build());
        nonGroupTemplates.setRangeFields(new RangeFieldList(Arrays.asList(field1, field2)));
        return nonGroupTemplates;
    }

    private CommonModel createTestModel() {
        return createTestModel(true);
    }

    private CommonModel createTestModel(boolean fillTestParam) {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder(1L, 11L, 5L)
            .currentType(CommonModel.Source.GURU)
            .published(true)
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                1, "vendor", Param.Type.ENUM, 5L, null))
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                2, "name", Param.Type.STRING, null, "Model"));
        if (fillTestParam) {
            builder = builder.putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                3, "testParam", Param.Type.NUMERIC, new BigDecimal(10)));
        }
        return builder.getModel();
    }

    private CommonModel createTestModelAnotherCategory() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder(3L, 12L, 6L)
            .currentType(CommonModel.Source.GURU)
            .published(true)
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                1, "vendor", Param.Type.ENUM, 6L, null))
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                2, "name", Param.Type.STRING, null, "Model 2"));
        builder = builder.putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
            3, "testParam", Param.Type.NUMERIC, new BigDecimal(10)));
        return builder.getModel();
    }

    private CommonModel createTestModification() {
        return CommonModelBuilder.newBuilder(2L, 11L, 5L)
            .parentModelId(1L)
            .currentType(CommonModel.Source.GURU)
            .published(true)
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                1, "vendor", Param.Type.ENUM, 5L, null))
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                2, "name", Param.Type.STRING, null, "Modification"))
            .putParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
                3, "testParam", Param.Type.NUMERIC, new BigDecimal(11)))
            .getModel();
    }

    private ModelForm createModelForm() {
        return new ModelFormBuilder<ModelForm>()
            .startTab().name("test")
            .startBlock().name("test block")
            .property("test prop")
            .endBlock()
            .endTab()
            .getModelForm();
    }

    private TMTemplate createEmptyTitlemakerTemplate() {
        return new TMTemplate();
    }

    private TMTemplate createTitlemakerTemplateWithGuru() {
        TMTemplate tmTemplate = new TMTemplate();
        tmTemplate.setHasGuruTemplate(true);
        tmTemplate.setGuruTemplate(MODEL_NAME_TEMPLATE);
        return tmTemplate;
    }

    private TMTemplate createTitlemakerTemplateWithGuruAndSku() {
        TMTemplate tmTemplate = createTitlemakerTemplateWithGuru();
        tmTemplate.setSkuTemplate(MODEL_NAME_TEMPLATE);
        return tmTemplate;
    }

    private void initCategoryEntities() {
        CategoryEntities entities = new CategoryEntities(TARGET_CAT_1, Collections.emptyList());
        CategoryParam vendor = TitleMakerTemplateServiceTestHelper.createParameter(
            1, "vendor", Param.Type.ENUM, true, true, true);
        Option vendorOpt = new OptionImpl(5L, "Vendor", Option.OptionType.VENDOR);
        Option localVendor = new OptionImpl(vendorOpt, Option.OptionType.VENDOR);
        localVendor.setPublished(true);
        vendor.addOption(localVendor);
        entities.addParameter(vendor);
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            2, "name", Param.Type.STRING, true, false, false));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            3, "testParam", Param.Type.NUMERIC, true, false, false));
        when(parameterLoader.loadCategoryEntitiesByHid(TARGET_CAT_1)).thenReturn(entities);

        CategoryEntities entities2 = new CategoryEntities(TARGET_CAT_2, Collections.emptyList());
        CategoryParam vendor2 = TitleMakerTemplateServiceTestHelper.createParameter(
            1, "vendor", Param.Type.ENUM, true, true, true);
        Option vendorOpt2 = new OptionImpl(6L, "Vendor", Option.OptionType.VENDOR);
        Option localVendor2 = new OptionImpl(vendorOpt2, Option.OptionType.VENDOR);
        localVendor2.setPublished(true);
        vendor2.addOption(localVendor2);
        entities2.addParameter(vendor2);
        entities2.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            2, "name", Param.Type.STRING, true, false, false));
        entities2.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            3, "testParam", Param.Type.NUMERIC, true, false, false));
        when(parameterLoader.loadCategoryEntitiesByHid(TARGET_CAT_2)).thenReturn(entities2);
    }

    private ModelTransferJobContext<ListOfModelParameterLandingConfig> getTestContext(
        ListOfModelParameterLandingConfig config
    ) {
        ModelTransfer modelTransfer = new ModelTransfer();
        modelTransfer.setId(1L);
        modelTransfer.setSourceCategories(config.getListOfModelsConfig().getEntitiesList().stream()
            .map(ModelTransferList::getSourceCategoryId).distinct().map(id -> {
                SourceCategory sourceCategory = new SourceCategory();
                sourceCategory.setId(id);
                return sourceCategory;
            }).collect(Collectors.toList()));
        modelTransfer.setDestinationCategories(config.getListOfModelsConfig().getEntitiesList().stream()
            .map(ModelTransferList::getTargetCategoryId).distinct().map(id -> {
                DestinationCategory destinationCategory = new DestinationCategory();
                destinationCategory.setId(id);
                return destinationCategory;
            }).collect(Collectors.toList()));

        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(1L);

        return new ModelTransferJobContext<>(modelTransfer, stepInfo, Collections.singletonList(stepInfo),
            config, Collections.emptyList());
    }

    private ListOfModelParameterLandingConfig config(long source, long target, long... models) {
        return new ListOfModelParameterLandingConfig(ListOfModelsConfigBuilder.newBuilder()
            .models(source, target, models).build()
        );
    }

    private ListOfModelParameterLandingConfig config(ListOfModelsConfigBuilder builder) {
        return new ListOfModelParameterLandingConfig(builder.build());
    }
}
