package ru.yandex.market.mbo.tms.modeltransfer;

import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.db.transfer.ModelTransferServiceImpl;
import ru.yandex.market.mbo.db.transfer.UserService;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepDependencyService;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepInfoService;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepRepository;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepResultInfoService;
import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferFilter;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferFindResult;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dmserebr
 * @date 14.08.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelTransferTaskHandlerTestBase {

    private static final long TRANSFER_ID = 10L;

    private final Answer validationEnqueuer = invocation ->
        ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
        .resultType(ResultInfo.Type.VALIDATION).build();

    private ModelTransferServiceImpl modelTransferService;
    private AutoUser autoUser;
    private UserService userService;

    ModelTransferStepDependencyService modelTransferStepDependencyService;
    ModelTransferTaskHandler modelTransferTaskHandler;
    ModelTransferStepInfoService modelTransferStepInfoService;
    ModelTransferStepRepository stepRepository;
    ModelTransferProcessingRepository modelTransferProcessingRepository;
    List<ResultInfo> executionResultInfos;
    List<ResultInfo> validationResultInfos;
    List<ModelTransferStepInfo> stepInfos;

    @Before
    public void before() throws Exception {
        modelTransferService = Mockito.mock(ModelTransferServiceImpl.class);
        modelTransferStepInfoService = Mockito.mock(ModelTransferStepInfoService.class);
        stepRepository = Mockito.mock(ModelTransferStepRepository.class);
        modelTransferStepDependencyService = Mockito.mock(ModelTransferStepDependencyService.class);
        modelTransferProcessingRepository  = Mockito.mock(ModelTransferProcessingRepository.class);
        autoUser = Mockito.mock(AutoUser.class);
        userService = Mockito.mock(UserService.class);
        ModelTransferStepResultInfoService resultInfoService = Mockito.mock(ModelTransferStepResultInfoService.class);

        Mockito.when(autoUser.getId()).thenReturn(6789L);
        Mockito.when(userService.getUser(Mockito.eq(autoUser.getId()))).then((Answer<User>) invocation -> {
            User user = new User();
            user.setName("Test user");
            user.setId(autoUser.getId());
            user.setLogin("test");
            return user;
        });

        ModelTransferStepResultService validationStepResultService =
            Mockito.mock(ModelTransferAutoStepResultServiceStub.class);
        Mockito.when(stepRepository.getValidationService(Mockito.any(ModelTransferStep.Type.class)))
            .thenReturn(validationStepResultService);
        Mockito.doAnswer(validationEnqueuer)
            .when(validationStepResultService).doAction(Mockito.anyLong(),
            Mockito.eq(ResultInfo.Action.ENQUEUE), Mockito.anyString(), Mockito.any(User.class));

        executionResultInfos = new ArrayList<>();
        validationResultInfos = new ArrayList<>();
        ModelTransfer transfer = ModelTransferBuilder.newBuilder()
            .id(TRANSFER_ID)
            .transferType(ModelTransfer.Type.MODEL_TRANSFER)
            .manager(userService.getUser(autoUser.getId()))
            .build();

        Mockito.when(modelTransferService.findModelTransfers(Mockito.any(ModelTransferFilter.class),
            Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(new ModelTransferFindResult(Collections.singletonList(transfer), 1));

        Mockito.when(modelTransferService.get(Mockito.anyLong()))
            .thenReturn(transfer);

        stepInfos = getStepInfos();
        Mockito.when(modelTransferStepInfoService.getStepInfos(Mockito.anyLong())).thenReturn(stepInfos);

        Mockito.when(modelTransferStepDependencyService.getDependencies(Mockito.any(ModelTransferStep.Type.class)))
            .thenReturn(Collections.emptyList());

        modelTransferTaskHandler = Mockito.spy(new ModelTransferTaskHandler(modelTransferService,
            modelTransferStepInfoService,
            stepRepository, modelTransferProcessingRepository, autoUser, userService,
            modelTransferStepDependencyService, resultInfoService));
    }

    protected List<ModelTransferStepInfo> getStepInfos() {
        return Collections.emptyList();
    }
}
