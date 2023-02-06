package ru.yandex.market.mbo.tt.status.listeners.handlers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.mbo.db.modelstorage.GeneratedSkuService;
import ru.yandex.market.mbo.db.modelstorage.ModelEditService;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelMergeServiceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.comments.Comment;
import ru.yandex.market.mbo.tt.comments.CommentsManager;
import ru.yandex.market.mbo.tt.legacy.TaskTrackerBeans;
import ru.yandex.market.mbo.tt.model.Priority;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.providers.model.CheckModelFromVendorTaskManager;
import ru.yandex.market.mbo.tt.providers.model.ModelFromVendorRawTask;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author galaev@yandex-team.ru
 * @since 20/03/2018.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CheckModelFromVendorTaskListHandlerTest {

    private static final long TASK_ID = 2L;
    private static final long TASK_LIST_ID = 1L;
    private static final long CATEGORY_ID = 3L;
    private static final int OWNER_ID = 0;
    private static final long VENDOR_MODEL_ID = 4L;
    private static final long GURU_MODEL_ID = 5L;
    private static final long VENDOR_ID = 6L;

    private static final AutoUser AUTO_USER = new AutoUser(OWNER_ID);

    private CategoryParam param1;
    private CategoryParam param2;
    private CategoryParam xlPicture;
    private CategoryParam xlPictureX;
    private CategoryParam xlPictureY;
    private CategoryParam xlPicture2;
    private CategoryParam xlPicture2X;
    private CategoryParam xlPicture2Y;
    private CategoryParam xlPicture3;
    private CategoryParam xlPicture3X;
    private CategoryParam xlPicture3Y;

    @Mock
    private CommentsManager commentsManager;
    @Mock
    private StatusManager statusManager;
    @Mock
    private TaskTracker taskTracker;
    @Mock
    private ModelEditService modelEditService;
    @Mock
    private CheckModelFromVendorTaskManager checkModelFromVendorTaskManager;
    @Mock
    private GeneratedSkuService skuService;
    @Mock
    private ModelImageSyncService modelImageSyncService;

    private CheckModelFromVendorTaskListHandler checkModelFromVendorTaskListHandler;

    @Before
    public void setUp() throws Exception {
        checkModelFromVendorTaskListHandler = new CheckModelFromVendorTaskListHandler(
            new TaskTrackerBeans(
                taskTracker,
                statusManager,
                null,
                null,
                commentsManager,
                AUTO_USER,
                null
            ),
            modelEditService,
            checkModelFromVendorTaskManager,
            skuService,
            new ModelMergeServiceStub(null),
            modelImageSyncService
        );

        Mockito.when(statusManager.getCurrentTaskStatus(TASK_ID))
            .thenReturn(Status.TASK_ACCEPTED);
        Mockito.when(commentsManager.getLastComment(any(), Mockito.anyLong()))
            .thenReturn(new Comment(0, "comment", 0, null));
        Mockito.when(taskTracker.getTask(Mockito.anyLong())).thenReturn(new Task(0, 0, 0, Status.TASK_ACCEPTED));

        initParameters();
    }

    @Test
    public void testUpdateGuruModel() {
        TaskList taskList = new TaskList(TASK_LIST_ID, CATEGORY_ID, Priority.DEFAULT.ordinal(), OWNER_ID,
            Status.TASK_LIST_ACCEPTED, TaskType.CHECK_FILL_MODEL_FROM_VENDOR, 0, null);

        ModelFromVendorRawTask rawTask = new ModelFromVendorRawTask(0);
        rawTask.setType(AutoGenerationApi.TicketType.UPDATE.name());
        rawTask.setCategoryId(CATEGORY_ID);
        rawTask.setVendorModelId(VENDOR_MODEL_ID);
        rawTask.setGuruModelId(GURU_MODEL_ID);

        CommonModel guruModel = createModel(CommonModel.Source.VENDOR);
        CommonModel vendorModel = createModel(CommonModel.Source.GURU);

        Mockito.when(checkModelFromVendorTaskManager.getTask(TASK_ID))
            .thenReturn(rawTask);
        Mockito.doReturn(vendorModel)
            .when(modelEditService)
            .getModel(VENDOR_MODEL_ID, CATEGORY_ID);
        Mockito.doReturn(guruModel)
            .when(modelEditService)
            .getModel(GURU_MODEL_ID, CATEGORY_ID);

        Mockito.doReturn(
            new GroupOperationStatus(
                new OperationStatus(OperationStatusType.OK, OperationType.CHANGE, -1L)
            )
        )
        .when(skuService)
        .createOrUpdateSku(eq(vendorModel), eq(guruModel), any(ModelSaveContext.class),
                any(GeneratedSkuService.GeneratedSkuSyncContext.class));

        checkModelFromVendorTaskListHandler.handleTaskStatusChanging(taskList, TASK_ID);

        Assert.assertEquals(vendorModel.getParameterValues().size(), guruModel.getParameterValues().size());
        Assert.assertEquals(vendorModel.getPictures().size(), guruModel.getPictures().size());

        verify(skuService).createOrUpdateSku(eq(vendorModel), eq(guruModel), any(ModelSaveContext.class),
            any(GeneratedSkuService.GeneratedSkuSyncContext.class));
    }

    private CommonModel createModel(CommonModel.Source type) {
        CommonModelBuilder model = CommonModelBuilder.newBuilder(0, CATEGORY_ID, VENDOR_ID)
            .id(type == CommonModel.Source.VENDOR ? VENDOR_MODEL_ID : GURU_MODEL_ID)
            .source(type)
            .currentType(type)
            .param(param1).setOption(1L)
            .param(param2).setOption(1L)
            .param(xlPicture).setString("urlxl")
            .param(xlPictureX).setNumeric(100)
            .param(xlPictureY).setNumeric(100)
            .picture("XL-Picture", "urlxl");
        if (type == CommonModel.Source.VENDOR) {
            model.param(xlPicture2).setString("urlxl2")
                .param(xlPicture2X).setNumeric(100)
                .param(xlPicture2Y).setNumeric(100)
                .param(xlPicture3).setString("urlxl3")
                .param(xlPicture3X).setNumeric(100)
                .param(xlPicture3Y).setNumeric(100)
                .picture("XL-Picture_2", "urlxl2")
                .picture("XL-Picture_3", "urlxl3");
        }
        return model.getModel();
    }

    private void initParameters() {
        long paramId = 1L;
        param1 = CategoryParamBuilder.newBuilder()
            .setId(1)
            .setXslName("param1")
            .setType(Param.Type.ENUM)
            .build();
        param2 = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("param2")
            .setType(Param.Type.ENUM)
            .build();
        xlPicture = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XL-Picture")
            .setType(Param.Type.STRING)
            .build();
        xlPictureX = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XLPictureSizeX")
            .setType(Param.Type.NUMERIC)
            .build();
        xlPictureY = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XLPictureSizeY")
            .setType(Param.Type.NUMERIC)
            .build();
        xlPicture2 = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XL-Picture_2")
            .setType(Param.Type.STRING)
            .build();
        xlPicture2X = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XLPictureSizeX_2")
            .setType(Param.Type.NUMERIC)
            .build();
        xlPicture2Y = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XLPictureSizeY_2")
            .setType(Param.Type.NUMERIC)
            .build();
        xlPicture3 = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XL-Picture_3")
            .setType(Param.Type.STRING)
            .build();
        xlPicture3X = CategoryParamBuilder.newBuilder()
            .setId(paramId++)
            .setXslName("XLPictureSizeX_3")
            .setType(Param.Type.NUMERIC)
            .build();
        xlPicture3Y = CategoryParamBuilder.newBuilder()
            .setId(paramId)
            .setXslName("XLPictureSizeY_3")
            .setType(Param.Type.NUMERIC)
            .build();
    }
}
