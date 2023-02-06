package ru.yandex.market.mbo.db.transfer.step;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.db.transfer.ModelTransferDAO;
import ru.yandex.market.mbo.db.transfer.UserService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep.Type;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 09.09.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTransferStepInfoServiceTest {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final TovarCategory CATEGORY_1 = TovarCategoryBuilder.newBuilder(1, 11L)
        .setName("first category")
        .setGuruCategoryId(111L)
        .create();
    private static final TovarCategory CATEGORY_2 = TovarCategoryBuilder.newBuilder(2, 12L)
        .setName("second category")
        .setGuruCategoryId(112L)
        .create();
    private static final TovarCategory CATEGORY_3 = TovarCategoryBuilder.newBuilder(3, 13L)
        .setName("third category")
        .setGuruCategoryId(113L)
        .create();
    private static TovarCategory category4 = TovarCategoryBuilder.newBuilder(4, 14L)
        .setName("fourth category")
        .setGuruCategoryId(114L)
        .create();

    private static final Collection<TovarCategory> CATEGORIES =
        Arrays.asList(CATEGORY_1, CATEGORY_2, CATEGORY_3, category4);

    private ModelTransferStepRepository stepRepository;
    private ModelTransferStepInfoService stepInfoService;
    private ModelTransferStepInfoDAO modelTransferStepInfoDAO;
    private TovarTreeService tovarTreeService;
    private TaskQueueRegistrator taskQueueRegistrator;
    private Map<ModelTransfer.Type, List<Type>> transferStepMap = new HashMap<>();
    private Map<ModelTransferStep.Type, List<ModelTransferStep.Type>> dependencies = new HashMap<>();
    private Map<ModelTransferStep.Type, ModelTransferStep> stepTemplates = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        stepRepository = new ModelTransferStepRepository(transferStepMap, stepTemplates,
            ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());

        ModelTransferDAO modelTransferDAO = mock(ModelTransferDAO.class);
        modelTransferStepInfoDAO = mock(ModelTransferStepInfoDAO.class);
        taskQueueRegistrator = mock(TaskQueueRegistrator.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(args -> {
            TransactionCallback<?> action = args.getArgument(0);
            return action.doInTransaction(null);
        });
        ModelTransferStepDependencyService stepDependencyService = new ModelTransferStepDependencyService(
            dependencies, modelTransferStepInfoDAO, stepRepository, transactionTemplate);
        UserService userService = mock(UserService.class);
        tovarTreeService = new TovarTreeServiceMock(CATEGORIES);

        stepInfoService = new ModelTransferStepInfoService(stepRepository, modelTransferDAO, modelTransferStepInfoDAO,
            stepDependencyService, userService, tovarTreeService, taskQueueRegistrator);
        stepInfoService.setTransactionTemplate(transactionTemplate);
    }

    @Test
    public void testComputeDeadline() {
        addStepTemplates(ModelTransfer.Type.MODEL_TRANSFER,
            step(Type.CONFIGURE_LIST_OF_MODELS, -2, 1),
            step(Type.MOVE_MODELS, 2, 1));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.MODEL_TRANSFER).build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(2);
        assertThat(stepInfos.get(0).getDeadline()).isEqualTo(date("01-09-2018 11:00:00"));
        assertThat(stepInfos.get(1).getDeadline()).isEqualTo(date("01-09-2018 15:00:00"));
    }

    @Test
    public void filterUnpublish() {
        addStepTemplates(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            step(Type.CONFIGURE_LIST_OF_MODELS),
            step(Type.MOVE_MODELS),
            step(Type.UNPUSLISH_CATEGORIES),
            step(Type.CHECK_NO_MODELS_IN_CATEGORY));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.CATEGORY_TRANSFORMATION)
            .sourceCategory(11L, false)
            .sourceCategory(12L, false)
            .destinationCategory(13L, false)
            .destinationCategory(14L, false)
            .build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(2);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);

        stepInfos.clear();
        transfer.getSourceCategories().get(0).setUnpublish(true);

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(4);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);
        assertThat(stepInfos.get(2).getStepType()).isEqualTo(Type.UNPUSLISH_CATEGORIES);
        assertThat(stepInfos.get(3).getStepType()).isEqualTo(Type.CHECK_NO_MODELS_IN_CATEGORY);
    }

    @Test
    public void keepMoveCategoriesSteps() {
        transferStepMap.put(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            Arrays.asList(Type.MOVE_CATEGORIES_TO_LEAF_LEVEL,
                Type.CONFIGURE_LIST_OF_MODELS,
                Type.MOVE_MODELS,
                Type.MOVE_CATEGORIES_TO_REQUIRED_LEVEL));
        addStepTemplates(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            step(Type.MOVE_CATEGORIES_TO_LEAF_LEVEL),
            step(Type.CONFIGURE_LIST_OF_MODELS),
            step(Type.MOVE_MODELS),
            step(Type.MOVE_CATEGORIES_TO_REQUIRED_LEVEL));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.CATEGORY_TRANSFORMATION)
            .sourceCategory(10L, false, true)
            .sourceCategory(11L, false, false)
            .destinationCategory(12L, false)
            .destinationCategory(13L, false)
            .build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(4);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.MOVE_CATEGORIES_TO_LEAF_LEVEL);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(2).getStepType()).isEqualTo(Type.MOVE_MODELS);
        assertThat(stepInfos.get(3).getStepType()).isEqualTo(Type.MOVE_CATEGORIES_TO_REQUIRED_LEVEL);
    }

    @Test
    public void removeMoveCategoriesSteps() {
        addStepTemplates(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            step(Type.MOVE_CATEGORIES_TO_LEAF_LEVEL),
            step(Type.CONFIGURE_LIST_OF_MODELS),
            step(Type.MOVE_MODELS),
            step(Type.MOVE_CATEGORIES_TO_REQUIRED_LEVEL));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.CATEGORY_TRANSFORMATION)
            .sourceCategory(10L, false, true)
            .sourceCategory(11L, false, true)
            .destinationCategory(12L, false)
            .destinationCategory(13L, false)
            .build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(2);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);
    }

    @Test
    public void removeFreezeUnfreezeForClassifierSteps() {
        addStepTemplates(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            step(Type.CONFIGURE_LIST_OF_MODELS),
            step(Type.FREEZE_DUMP_FOR_CLASSIFIER),
            step(Type.MOVE_CATEGORIES_TO_LEAF_LEVEL),
            step(Type.MOVE_CATEGORIES_TO_REQUIRED_LEVEL),
            step(Type.DISABLE_OLD_CATEGORIES_IN_CLASSIFIER_TRAINER),
            step(Type.UNFREEZE_DUMP_FOR_CLASSIFIER),
            step(Type.MOVE_MODELS));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.CATEGORY_TRANSFORMATION)
            .sourceCategory(10L, false, true)
            .sourceCategory(11L, false, true)
            .destinationCategory(12L, false)
            .destinationCategory(13L, false)
            .build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(2);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);
    }

    @Test
    public void filterNewCategory() {
        addStepTemplates(ModelTransfer.Type.CATEGORY_TRANSFORMATION,
            step(Type.CONFIGURE_LIST_OF_MODELS),
            step(Type.MOVE_MODELS),
            step(Type.CONFIGURE_NEW_CATEGORIES),
            step(Type.CREATE_GURU_CATEGORIES),
            step(Type.COPY_OPERATOR_CARD_AND_CARD_TEMPLATES),
            step(Type.ENABLE_NEW_CATEGORIES_IN_CLASSIFIER),
            step(Type.PUBLISH_GURU_IN_CATEGORIES),
            step(Type.SEND_GURU_PUBLISHED_EMAIL));
        List<ModelTransferStepInfo> stepInfos = new ArrayList<>();
        when(modelTransferStepInfoDAO.create(any())).thenAnswer(args -> {
            ModelTransferStepInfo stepInfo = args.getArgument(0);
            stepInfos.add(stepInfo);
            return stepInfo;
        });

        ModelTransfer transfer = modelTransferBuilder(ModelTransfer.Type.CATEGORY_TRANSFORMATION)
            .sourceCategory(11L, false)
            .sourceCategory(12L, false)
            .destinationCategory(13L, false)
            .destinationCategory(14L, false)
            .build();

        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(2);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);

        stepInfos.clear();
        transfer.getDestinationCategories().get(0).setNewCategory(true);
        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(7);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);
        assertThat(stepInfos.get(2).getStepType()).isEqualTo(Type.CONFIGURE_NEW_CATEGORIES);
        assertThat(stepInfos.get(3).getStepType()).isEqualTo(Type.COPY_OPERATOR_CARD_AND_CARD_TEMPLATES);
        assertThat(stepInfos.get(4).getStepType()).isEqualTo(Type.ENABLE_NEW_CATEGORIES_IN_CLASSIFIER);
        assertThat(stepInfos.get(5).getStepType()).isEqualTo(Type.PUBLISH_GURU_IN_CATEGORIES);
        assertThat(stepInfos.get(6).getStepType()).isEqualTo(Type.SEND_GURU_PUBLISHED_EMAIL);

        stepInfos.clear();
        category4.setGuruCategoryId(0L);
        stepInfoService.initializeSteps(transfer, transfer.getManager());

        assertThat(stepInfos.size()).isEqualTo(8);
        assertThat(stepInfos.get(0).getStepType()).isEqualTo(Type.CONFIGURE_LIST_OF_MODELS);
        assertThat(stepInfos.get(1).getStepType()).isEqualTo(Type.MOVE_MODELS);
        assertThat(stepInfos.get(2).getStepType()).isEqualTo(Type.CONFIGURE_NEW_CATEGORIES);
        assertThat(stepInfos.get(3).getStepType()).isEqualTo(Type.CREATE_GURU_CATEGORIES);
        assertThat(stepInfos.get(4).getStepType()).isEqualTo(Type.COPY_OPERATOR_CARD_AND_CARD_TEMPLATES);
        assertThat(stepInfos.get(5).getStepType()).isEqualTo(Type.ENABLE_NEW_CATEGORIES_IN_CLASSIFIER);
        assertThat(stepInfos.get(6).getStepType()).isEqualTo(Type.PUBLISH_GURU_IN_CATEGORIES);
        assertThat(stepInfos.get(7).getStepType()).isEqualTo(Type.SEND_GURU_PUBLISHED_EMAIL);
    }

    private Date date(String textDate) {
        try {
            return SIMPLE_DATE_FORMAT.parse(textDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelTransferBuilder modelTransferBuilder(ModelTransfer.Type transferType) {
        return ModelTransferBuilder.newBuilder()
            .id(100L)
            .transferType(transferType)
            .manager(new User(1L))
            .transferDate(date("01-09-2018 12:00:00"));
    }

    private ModelTransferStep step(ModelTransferStep.Type stepType, int startOffsetInHours, int durationInHours) {
        return new ModelTransferStep(stepType, ModelTransferStep.ExecutionType.MANUAL, stepType.name(),
            startOffsetInHours, durationInHours);
    }

    private ModelTransferStep step(ModelTransferStep.Type stepType) {
        return step(stepType, 0, 0);
    }

    private void addStepTemplates(ModelTransfer.Type transferType, ModelTransferStep... steps) {
        List<ModelTransferStep> stepList = Arrays.asList(steps);
        transferStepMap.put(transferType, stepList.stream().map(ModelTransferStep::getStepType)
            .collect(Collectors.toList()));
        stepList.forEach(s -> stepTemplates.put(s.getStepType(), s));
    }
}
