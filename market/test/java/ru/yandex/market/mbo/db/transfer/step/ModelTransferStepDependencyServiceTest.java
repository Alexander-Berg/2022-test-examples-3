package ru.yandex.market.mbo.db.transfer.step;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep.Type;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author danfertev
 * @since 09.09.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTransferStepDependencyServiceTest {
    private static final long TRANSFER_ID = 100L;

    private ModelTransferStepDependencyService stepDependencyService;
    private ModelTransferStepInfoDAO stepInfoDAO;
    private ModelTransferStepRepository stepRepository;
    private Map<Type, List<Type>> stepDependencies = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        stepInfoDAO = mock(ModelTransferStepInfoDAO.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(args -> {
            TransactionCallback<?> action = args.getArgument(0);
            return action.doInTransaction(null);
        });
        stepRepository = mock(ModelTransferStepRepository.class);
        stepDependencyService = new ModelTransferStepDependencyService(stepDependencies, stepInfoDAO, stepRepository,
            transactionTemplate);
    }

    @Test
    public void noDependencies() {
        ModelTransferStepInfo si = stepInfo(1L, Type.MOVE_MODELS);

        stepDependencyService.updateIsReadyToExecute(si);

        assertThat(si.isReadyToExecute()).isTrue();
    }

    @Test
    public void singleDependencyNoResults() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isFalse();
        assertThat(si2.getBlockedByStepIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void singleDependencyNotCompleted() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.FAILED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isFalse();
        assertThat(si2.getBlockedByStepIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void singleDependencyCompletedNoValidation() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isTrue();
    }

    @Test
    public void singleDependencyCompletedNotValidated() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isFalse();
        assertThat(si2.getBlockedByStepIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void singleDependencyCompletedValidationFailed() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        si1.setValidationResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.VALIDATION,
            ResultInfo.Status.FAILED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isFalse();
        assertThat(si2.getBlockedByStepIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void singleDependencyCompletedValidated() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        si1.setValidationResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.VALIDATION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si2);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si2);

        assertThat(si2.isReadyToExecute()).isTrue();
    }

    @Test
    public void singleDependencyCompletedValidatedSecondLevelDependencyNotCompleted() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.FORBID_CATEGORY_OPERATIONS);
        ModelTransferStepInfo si2 = stepInfo(2L, Type.CONFIGURE_LIST_OF_MODELS);
        si2.setExecutionResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        si2.setValidationResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.VALIDATION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si3 = stepInfo(3L, Type.MOVE_MODELS);

        addDependencies(Type.CONFIGURE_LIST_OF_MODELS, Type.FORBID_CATEGORY_OPERATIONS);
        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.FORBID_CATEGORY_OPERATIONS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si2);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si3);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si3);

        assertThat(si3.isReadyToExecute()).isFalse();
        assertThat(si3.getBlockedByStepIds()).containsExactlyInAnyOrder(2L);
    }

    @Test
    public void singleDependencyCompletedValidatedSecondLevelDependencyCompleted() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.FORBID_CATEGORY_OPERATIONS);
        si1.setExecutionResultInfos(Collections.singletonList(resultInfo(1L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si2 = stepInfo(2L, Type.CONFIGURE_LIST_OF_MODELS);
        si2.setExecutionResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        si2.setValidationResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.VALIDATION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si3 = stepInfo(3L, Type.MOVE_MODELS);

        addDependencies(Type.CONFIGURE_LIST_OF_MODELS, Type.FORBID_CATEGORY_OPERATIONS);
        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.FORBID_CATEGORY_OPERATIONS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si2);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si3);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si3);

        assertThat(si3.isReadyToExecute()).isTrue();
    }

    @Test
    public void multipleDependencies() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        ModelTransferStepInfo si2 = stepInfo(2L, Type.FREEZE_DUMP_FOR_TRANSFER);
        si2.setExecutionResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.EXECUTION,
            ResultInfo.Status.COMPLETED)));
        si2.setValidationResultInfos(Collections.singletonList(resultInfo(2L, ResultInfo.Type.VALIDATION,
            ResultInfo.Status.COMPLETED)));
        ModelTransferStepInfo si3 = stepInfo(3L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS, Type.FREEZE_DUMP_FOR_TRANSFER);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.FREEZE_DUMP_FOR_TRANSFER))).thenReturn(si2);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si3);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.FREEZE_DUMP_FOR_TRANSFER))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si3);

        assertThat(si3.isReadyToExecute()).isFalse();
        assertThat(si3.getBlockedByStepIds()).containsExactlyInAnyOrder(1L);
    }

    @Test
    public void multipleDependenciesNotCompleted() {
        ModelTransferStepInfo si1 = stepInfo(1L, Type.CONFIGURE_LIST_OF_MODELS);
        ModelTransferStepInfo si2 = stepInfo(2L, Type.FREEZE_DUMP_FOR_TRANSFER);
        ModelTransferStepInfo si3 = stepInfo(3L, Type.MOVE_MODELS);

        addDependencies(Type.MOVE_MODELS, Type.CONFIGURE_LIST_OF_MODELS, Type.FREEZE_DUMP_FOR_TRANSFER);

        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(si1);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.FREEZE_DUMP_FOR_TRANSFER))).thenReturn(si2);
        when(stepInfoDAO.loadStepInfoByType(eq(TRANSFER_ID), eq(Type.MOVE_MODELS))).thenReturn(si3);
        ModelTransferStepResultService<?> stepResultService = mock(ModelTransferStepResultService.class);
        when(stepRepository.getValidationService(any(ModelTransferStepInfo.class))).thenCallRealMethod();
        when(stepRepository.getValidationService(eq(Type.CONFIGURE_LIST_OF_MODELS))).thenReturn(stepResultService);

        stepDependencyService.updateIsReadyToExecute(si3);

        assertThat(si3.isReadyToExecute()).isFalse();
        assertThat(si3.getBlockedByStepIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    private ModelTransferStepInfo stepInfo(long id, Type type) {
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(id);
        stepInfo.setTransferId(TRANSFER_ID);
        stepInfo.setStepType(type);
        stepInfo.setExecutionResultInfos(new ArrayList<>());
        stepInfo.setValidationResultInfos(new ArrayList<>());
        return stepInfo;
    }

    private ResultInfo resultInfo(long stepId, ResultInfo.Type resultType, ResultInfo.Status status) {
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setStepId(stepId);
        resultInfo.setResultType(resultType);
        resultInfo.setStatus(status);
        resultInfo.setStarted(new Date());
        if (status == ResultInfo.Status.COMPLETED) {
            resultInfo.setCompleted(new Date());
        }
        return resultInfo;
    }

    private void addDependencies(Type stepType, Type... deps) {
        stepDependencies.put(stepType, Arrays.asList(deps));
    }
}
