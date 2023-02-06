package ru.yandex.market.mbo.gwt.server.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.TitleMakerTemplateServiceTestHelper;
import ru.yandex.market.mbo.db.TitleMakerTemplateValidationService;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageInternalService;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.titlemaker.ModelTitle;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplateView;
import ru.yandex.market.mbo.gwt.models.titlemaker.TemplateType;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.models.visual.VisualCategory;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TitleMakerTemplateServiceTest {

    private static final long CATEGORY_HID = 1;
    private static final long GURU_CATEGORY_ID = 10;
    private static final long WRONG_CATEGORY_HID = 0;
    private static final String MODEL_NAME_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

    private static final String SIMPLE_GURU_TEMPLATE = "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 )," +
        "null,(true)],[(1 ),(t0 ),null,(true)]]}";
    private static final String GURU_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 ),null,(true)]," +
            "[(1 ),(t0 ),null,(true)],[(1 ),(v6185723 ),null,(true)],[(1 ),(v6531685 )]]}";

    private TitleMakerTemplateServiceRemoteImpl templateService;

    @Mock
    private TovarTreeService tovarTreeService;

    @Mock
    private TitlemakerTemplateDao titlemakerTemplateDao;

    @Mock
    private VisualService vs;

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private ModelStorageInternalService modelStorageInternalService;

    @Mock
    private SizeMeasureService sizeMeasureService;

    @Mock
    private IParameterLoaderService parameterLoader;

    @Mock
    private GuruService guruService;

    @Mock
    private ValueLinkServiceInterface valueLinkService;

    @Mock
    private AutoUser autoUser;

    @Before
    public void init() {
        templateService = new TitleMakerTemplateServiceRemoteImpl();
        templateService.setModelStorageService(modelStorageService);
        templateService.setParameterLoaderService(parameterLoader);
        templateService.setVisualService(vs);
        templateService.setTitlemakerTemplateDao(titlemakerTemplateDao);
        templateService.setGuruService(guruService);

        TitleMakerTemplateValidationService templateValidationService = new TitleMakerTemplateValidationService();
        templateValidationService.setModelStorageService(modelStorageService);
        templateValidationService.setModelStorageInternalService(modelStorageInternalService);
        templateValidationService.setParameterLoaderService(parameterLoader);
        templateValidationService.setTovarTreeService(tovarTreeService);
        templateValidationService.setSizeMeasureService(sizeMeasureService);
        templateValidationService.setValueLinkService(valueLinkService);
        templateValidationService.setAutoUser(autoUser);
        templateService.setTitleMakerTemplateValidationService(templateValidationService);
    }

    @Test
    @SuppressWarnings("CheckStyle")
    public void testParameterLoad() {
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            1, "param1", Param.Type.ENUM, true, true, true));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            2, "param2", Param.Type.ENUM, true, false, false));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            3, "param3", Param.Type.ENUM, false, true, false));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            4, "param4", Param.Type.ENUM, false, false, true));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            5, "name", Param.Type.STRING, true, false, false));
        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);

        List<CategoryParam> result = templateService.loadParametersForTemplate(CATEGORY_HID);
        TitleMakerTemplateServiceTestHelper.assertParams(result, "param3", "param4", "name");
    }

    @Test(expected = OperationException.class)
    public void testParameterLoadWrongCategory() {
        when(parameterLoader.loadCategoryEntitiesByHid(WRONG_CATEGORY_HID)).thenReturn(null);
        templateService.loadParametersForTemplate(WRONG_CATEGORY_HID);
    }

    @Test
    public void testRemoveTemplateByHid() {
        when(vs.loadPublishedVisualCategoryByHid(CATEGORY_HID)).thenReturn(new VisualCategory());

        templateService.removeTemplateByHid(CATEGORY_HID);

        verify(titlemakerTemplateDao).removeTemplateByHid(CATEGORY_HID);
    }

    @Test(expected = OperationException.class)
    public void testRemoveTemplateVisualUseTitlemakerByHid() {
        VisualCategory category = new VisualCategory();
        category.setUseTitleMaker(true);
        when(vs.loadPublishedVisualCategoryByHid(CATEGORY_HID)).thenReturn(category);

        templateService.removeTemplateByHid(CATEGORY_HID);
    }

    @Test
    public void testRemoveTemplateNonVisualHid() {
        when(vs.loadPublishedVisualCategoryByHid(CATEGORY_HID)).thenReturn(null);

        templateService.removeTemplateByHid(CATEGORY_HID);

        verify(titlemakerTemplateDao).removeTemplateByHid(CATEGORY_HID);
    }

    @Test
    public void testLoadTemplateByHid() {
        TMTemplate template = new TMTemplate();
        template.setValue(MODEL_NAME_TEMPLATE);
        template.setGuruTemplate(MODEL_NAME_TEMPLATE);
        when(titlemakerTemplateDao.loadTemplateByHid(CATEGORY_HID)).thenReturn(template);

        TMTemplateView view = templateService.loadTemplateByHid(CATEGORY_HID);

        assertNotNull(view.getTemplateByType(TemplateType.TITLE));
        assertNotNull(view.getTemplateByType(TemplateType.GURU_TITLE));
    }

    @Test
    @SuppressWarnings("CheckStyle")
    public void testMakeExampleTitles() {
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        CategoryParam vendor = TitleMakerTemplateServiceTestHelper.createParameter(
            1, "vendor", Param.Type.ENUM, true, true, true);
        Option vendorOpt = new OptionImpl(5L, "Vendor");
        Option localVendor = new OptionImpl(vendorOpt, Option.OptionType.VENDOR);
        localVendor.setPublished(true);
        vendor.addOption(localVendor);
        entities.addParameter(vendor);
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            2, "name", Param.Type.STRING, true, false, false));
        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);

        when(tovarTreeService.loadCategoryByHid(eq(CATEGORY_HID))).thenReturn(
            TovarCategoryBuilder.newBuilder(1, CATEGORY_HID).setGroup(true).setGuruCategoryId(10L)
                .addShowModelType(CommonModel.Source.GURU).create());

        CommonModel model = new CommonModel();
        model.setSource(CommonModel.Source.GURU);
        model.setCurrentType(CommonModel.Source.GURU);
        model.setPublished(true);
        model.addParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
            1, "vendor", Param.Type.ENUM, 5L, null));
        model.addParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
            2, "name", Param.Type.STRING, null, "Model"));
        when(modelStorageInternalService.getModelsRandomSample(
            anyLong(), anyInt(), any(CommonModel.Source.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(Arrays.asList(model));

        when(sizeMeasureService.listSizeMeasures(anyLong())).thenReturn(new ArrayList<>());

        TMTemplateView templateView = new TMTemplateView();
        templateView.setHasGuruTemplate(true);
        templateView.setTemplate(MODEL_NAME_TEMPLATE, TemplateType.GURU_TITLE);

        List<ModelTitle> result = templateService.getExampleTitles(
            CATEGORY_HID, templateView, 5, Collections.singleton(TemplateType.GURU_TITLE), false)
            .getGoodTitles(TemplateType.GURU_TITLE);

        assertEquals(1, result.size());
        assertEquals("Vendor Model", result.get(0).getTitle());
    }

    @Test
    public void testGetIncorrectGuruModels() {
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR, Param.Type.ENUM, false, true, true));
        when(parameterLoader.loadCategoryEntitiesByHid(CATEGORY_HID)).thenReturn(entities);
        when(guruService.getGuruCategoryByHid(CATEGORY_HID)).thenReturn(GURU_CATEGORY_ID);
        when(guruService.isGroupCategory(anyLong())).thenReturn(true);
        //test skip always filled
        templateService.getIncorrectModels(CATEGORY_HID, CommonModel.Source.GURU, SIMPLE_GURU_TEMPLATE);
        verifyZeroInteractions(modelStorageService);

        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            6185723L, "required", Param.Type.ENUM, false, true, false));
        entities.addParameter(TitleMakerTemplateServiceTestHelper.createParameter(
            6531685L, "optional", Param.Type.ENUM, false, true, false));

        CommonModel model = new CommonModel();
        model.setId(10000L);
        model.setSource(CommonModel.Source.GURU);
        model.setCurrentType(CommonModel.Source.GURU);
        model.addParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(
            KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR, Param.Type.ENUM, 5L, null));

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Consumer<CommonModel> consumer = invocation.getArgument(2);
                consumer.accept(model);
                return null;
            }
        }).when(modelStorageService).processQueryModels(anyLong(), any(), any());

        Set<Long> incorrect = templateService.getIncorrectModels(CATEGORY_HID, CommonModel.Source.GURU, GURU_TEMPLATE);
        assertEquals(1, incorrect.size());
        assertEquals((Long) model.getId(), incorrect.iterator().next());

        //adding parent
        model.setParentModelId(10001L);
        CommonModel parent = new CommonModel();
        parent.setId(10001L);
        parent.setSource(CommonModel.Source.GURU);
        parent.setCurrentType(CommonModel.Source.GURU);
        parent.addParameterValue(TitleMakerTemplateServiceTestHelper.createParameterValue(6185723L, "required",
            Param.Type.ENUM, 2L, null));

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Consumer<CommonModel> consumer = invocation.getArgument(2);
                consumer.accept(parent);
                return null;
            }
        }).when(modelStorageService).processCategoryModels(anyLong(), anyList(), any(), any());

        incorrect = templateService.getIncorrectModels(CATEGORY_HID, CommonModel.Source.GURU, GURU_TEMPLATE);
        assertEquals(0, incorrect.size());
    }
}
