package ru.yandex.market.mbo.statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.catalogue.CategoryMatcherParamService;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContext;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditService;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.generalization.ModelGeneralizationService;
import ru.yandex.market.mbo.db.modelstorage.generalization.ModelGeneralizationServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.ActionType;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.BillingMode;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.EntityType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueMetadata;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorage.YangGetAuditActionsResponse.AuditParamAction;
import ru.yandex.market.mbo.http.YangLogStorage.YangGetAuditActionsResponse.CategoryActions;
import ru.yandex.market.mbo.http.YangLogStorage.YangGetAuditActionsResponse.CategoryParamActions;
import ru.yandex.market.mbo.http.YangLogStorage.YangGetAuditActionsResponse.ModelAuditActions;
import ru.yandex.market.mbo.http.YangLogStorage.YangGetAuditActionsResponse.PropertyValueAction;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.user.UserManagerMock;
import ru.yandex.market.mbo.users.MboUsers;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.categoryAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.createAuditAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.createLocalVendor;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.createParameter;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelPickerAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.optionAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.refreshTime;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.refreshTimeToNow;

@SuppressWarnings("checkstyle:magicnumber")
public class ModelAuditStatisticsServiceImplTest {
    private static final Long OPERATOR_UID = 10101L;
    private static final Long MANAGER_UID = 20202L;
    private static final MboUser TEST_USER = new MboUser("test", OPERATOR_UID, "test", "test", "test");
    private static final String DEFAULT_TASK_SUITE_CREATED_DATE = "1970-01-01T00:00:00.000";
    private static final ImmutableMap<String, Long> IMAGE_PARAMS = ImmutableMap.<String, Long>builder()
        .put("XL-Picture", 1L)
        .put("XLPictureSizeX", 2L)
        .put("XLPictureSizeY", 3L)
        .build();


    private ModelAuditStatisticsServiceImpl service;
    private AuditServiceMock auditServiceMock;
    private CategoryParametersService extractorService;
    private ModelStorageServiceStub modelStorageService;
    private UserManager userManager;
    private ModelAuditService modelAuditService;
    private ModelAuditContext modelAuditContext;
    private ModelGeneralizationService modelGeneralizationService;
    private long actionsStartTime;

    public static final long HITMAN_ID = 1234L;

    private static final Long CATEGORY_ID = 1L;

    @Before
    public void setUp() throws Exception {
        extractorService = Mockito.mock(CategoryParametersService.class);
        auditServiceMock = new AuditServiceMock();
        modelStorageService = Mockito.spy(new ModelStorageServiceStub());
        modelAuditService = new ModelAuditServiceImpl(auditServiceMock);
        modelAuditContext = Mockito.mock(ModelAuditContext.class);
        Mockito.when(modelAuditContext.getSource()).thenReturn(AuditAction.Source.YANG_TASK);
        Mockito.when(modelAuditContext.getSourceId()).thenReturn(String.valueOf(HITMAN_ID));
        Mockito.when(modelAuditContext.isBilledOperation()).thenReturn(true);
        Mockito.when(modelAuditContext.isBillableParameter(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(true);
        Mockito.when(modelAuditContext.getParameterId(Mockito.anyLong(), Mockito.anyString()))
            .thenAnswer(invocation -> Optional.ofNullable(IMAGE_PARAMS.get(invocation.getArgument(1))));

        Mockito.when(modelAuditContext.getStats()).thenReturn(new SaveStats());
        BaseGuruServiceImpl guruService = new BaseGuruServiceImpl();
        guruService.addCategory(CATEGORY_ID, CATEGORY_ID + 1000, true);

        List<CategoryParam> params = new ArrayList<>();
        params.add(ParameterBuilder.builder()
            .id(IMAGE_PARAMS.get("XL-Picture")).xsl("XL-Picture").type(Param.Type.STRING).endParameter());
        params.add(ParameterBuilder.builder()
            .id(IMAGE_PARAMS.get("XLPictureSizeX")).xsl("XLPictureSizeX").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(IMAGE_PARAMS.get("XLPictureSizeY")).xsl("XLPictureSizeY").type(Param.Type.NUMERIC).endParameter());

        modelGeneralizationService = new ModelGeneralizationServiceImpl(guruService, new AutoUser(111),
            CategoryParametersServiceClientStub.ofCategoryParams(CATEGORY_ID, params));
        userManager = new UserManagerMock();
        userManager.addUser(TEST_USER);
        service = new ModelAuditStatisticsServiceImpl(auditServiceMock, extractorService,
            modelStorageService, userManager);
    }

    @Test
    public void cleanerShouldRemoveActionsOnDeletedModels() {
        SquashedUserActions contractorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setDeleted(false)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new"))))
            .addModelActions(modelActions(2L)
                .setDeleted(true)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new"))))
            .addModelActions(modelActions(3L)
                .setDeleted(false)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new"))));
        SquashedUserActions inspectorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setDeleted(false)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new"))))
            .addModelActions(modelActions(3L)
                .setDeleted(true)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new"))));

        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.getHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.removeChangedNotBilledParameters();

        assertThat(helper.getContractorActions().getAllModelActions()).containsOnlyKeys(1L);
        assertThat(helper.getInspectorActions().getAllModelActions()).containsOnlyKeys(1L);
    }

    @Test
    public void picturesWereMovedInsideOneModel() {
        refreshToNow();
        // operator
        // -- move 2 pics inside one model
        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(2L, EntityType.MODEL_PICTURE, ActionType.DELETE, 1L,
                "1", "1", "", 1L, BillingMode.BILLING_MODE_FILL, 1L))
            .add(createAuditAction(2L, EntityType.MODEL_PICTURE, ActionType.DELETE, 2L,
                "2", "2", "", 1L, BillingMode.BILLING_MODE_FILL, 1L))
            .add(createAuditAction(2L, EntityType.MODEL_PICTURE, ActionType.CREATE, 3L,
                "3", "", "1", 1L, BillingMode.BILLING_MODE_FILL, 1L))
            .add(createAuditAction(2L, EntityType.MODEL_PICTURE, ActionType.CREATE, 4L,
                "4", "", "2", 1L, BillingMode.BILLING_MODE_FILL, 1L))
            .build());

        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(1L)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(CATEGORY_ID)
            .build();

        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenReturn(Collections.singletonList(createModel(2, CommonModel.Source.SKU)));

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);
        Map<Long, YangLogStorage.ModelStatistic> modelStatisticMap = res.getModelStatisticList().stream()
            .collect(Collectors.toMap(YangLogStorage.ModelStatistic::getModelId, s -> s));

        assertThat(modelStatisticMap).containsOnlyKeys(2L);
        assertThat(modelStatisticMap.get(2L).getContractorActions().getPictureUploadedCount()).isEqualTo(0);
        assertThat(modelStatisticMap.get(2L).getContractorActions().getPictureCopiedCount()).isEqualTo(0);
    }

    @Test
    public void cleanerShouldRemoveNonBillingActions() {
        SquashedUserActions contractorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .add(modelPickerAction(1L, "option1", "", "val"))
                    .add(createAuditAction(1L, EntityType.MODEL_PICKER,
                        ActionType.CREATE, 2L, "option1", "", "val", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .add(modelPickerAction(3L, "option1", "", "val"))
                    .build())
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .add(modelSingleParamAction(1L, "param1", "old", "new"))
                    .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                        2L, "param2", "old", "new", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .add(modelSingleParamAction(3L, "param3", "old", "new"))
                    .build())
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .add(modelParamAction(1L, "param1", "", "new", ActionType.CREATE))
                    .add(createAuditAction(1L, EntityType.MODEL_PARAM,
                        ActionType.DELETE, 2L, "param2", "old", "", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .add(modelParamAction(3L, "param3", "", "option", ActionType.CREATE))
                    .build())
            );
        SquashedUserActions inspectorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .add(modelPickerAction(1L, "option1", "", "val"))
                    .add(createAuditAction(1L, EntityType.MODEL_PICKER,
                        ActionType.CREATE, 3L, "option1", "", "val", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .build())
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .add(modelSingleParamAction(1L, "param1", "old", "new"))
                    .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                        3L, "param3", "old", "new", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .build())
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .add(modelParamAction(1L, "param1", "", "new", ActionType.CREATE))
                    .add(createAuditAction(1L, EntityType.MODEL_PARAM,
                        ActionType.DELETE, 3L, "param3", "option", "", 1L,
                        AuditAction.BillingMode.BILLING_MODE_NONE))
                    .build())
            );

        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.getHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.removeChangedNotBilledParameters();

        SquashedUserActions.ModelActions contractorBilledActions =
            helper.getContractorActions().getModelActionsById(1L);
        assertThat(contractorBilledActions.getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(1L, "option1"));
        assertThat(contractorBilledActions.getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(1L, "param1"));
        assertThat(contractorBilledActions.getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(1L, "param1", "new"));

        SquashedUserActions.ModelActions inspectorBilledActions =
            helper.getInspectorActions().getModelActionsById(1L);
        assertThat(inspectorBilledActions.getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(1L, "option1"));
        assertThat(inspectorBilledActions.getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(1L, "param1"));
        assertThat(inspectorBilledActions.getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(1L, "param1", "new"));
    }

    @Test
    public void correctionsShouldRemoveModelsWithoutAnyActions() {
        SquashedUserActions contractorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", ""))))
            .addModelActions(modelActions(2L));
        SquashedUserActions inspectorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(2L, "param2", "new", ""))))
            .addModelActions(modelActions(3L));

        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.getHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.computeInspectorCorrections();
        helper.filterOutEmptyActions();
        helper.removeEmptyModelActions();

        assertThat(helper.getContractorActions().getAllModelActions()).isEmpty();
        assertThat(helper.getInspectorActions().getAllModelActions()).isEmpty();
        assertThat(helper.getCorrectionsActions().getAllModelActions()).isEmpty();
    }

    @SuppressWarnings("checkstyle:methodlength")
    @Test
    public void correctionsShouldSplitParametersIntoThreeSets() {
        SquashedUserActions contractorActions = new SquashedUserActions()
            .addModelActions(modelActions(1L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new")))
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .single(modelParamAction(11L, "param11", "", "new", ActionType.CREATE)))
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .single(modelPickerAction(11L, "option1", "", "new"))))
            .addModelActions(modelActions(2L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .add(modelSingleParamAction(1L, "param1", "old", "new"))
                    .add(modelSingleParamAction(2L, "param2", "old", "new"))
                    .build())
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .add(modelParamAction(11L, "param11", "new", "", ActionType.DELETE))
                    .add(modelParamAction(12L, "param12", "", "new", ActionType.CREATE))
                    .build())
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .add(modelPickerAction(11L, "option11", "", "new"))
                    .add(modelPickerAction(12L, "option12", "", "new"))
                    .build()))
            .addParamActions(new SquashedUserActions.CategoryParamActions()
                .setOptionId(1L)
                .setParamId(3L)
                .setCreateAction(Optional.of(
                    optionAction(1L, 3L, "", "", "", ActionType.CREATE)
                        .setUserId(OPERATOR_UID)
                ))
                .setChangedAliases(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, 3L, Option.ALIASES_PROPERTY_NAME, "a", "", ActionType.DELETE))
                    .add(optionAction(1L, 3L, Option.ALIASES_PROPERTY_NAME, "", "b", ActionType.CREATE))
                    .add(optionAction(1L, 3L, Option.ALIASES_PROPERTY_NAME, "", "c", ActionType.CREATE))
                    .build())
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, 3L, CategoryMatcherParamService.CUT_OFF_WORD, "", "X", ActionType.CREATE))
                    .add(optionAction(1L, 3L, CategoryMatcherParamService.CUT_OFF_WORD, "", "Y", ActionType.CREATE))
                    .add(optionAction(1L, 3L, CategoryMatcherParamService.CUT_OFF_WORD, "", "Z", ActionType.CREATE))
                    .build()
                )
            )
            .setCategoryActions(new SquashedUserActions.CategoryActions()
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "AAA", "", ActionType.DELETE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "EE", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "II", ActionType.CREATE))
                    .build()
                )
            );
        SquashedUserActions inspectorActions = new SquashedUserActions()
            .addModelActions(modelActions(2L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .add(modelSingleParamAction(1L, "param1", "new", "newest"))
                    .add(modelSingleParamAction(3L, "param3", "old", "new"))
                    .build())
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .add(modelParamAction(11L, "param11", "", "new", ActionType.CREATE))
                    .add(modelParamAction(13L, "param13", "", "new", ActionType.CREATE))
                    .build())
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .add(modelPickerAction(11L, "option11", "new", "newest"))
                    .add(modelPickerAction(13L, "option13", "", "new"))
                    .build()))
            .addModelActions(modelActions(3L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .single(modelSingleParamAction(1L, "param1", "old", "new")))
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .single(modelParamAction(11L, "param11", "", "new", ActionType.CREATE)))
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .single(modelPickerAction(11L, "option1", "", "new"))))
            .addParamActions(new SquashedUserActions.CategoryParamActions()
                .setOptionId(1L)
                .setParamId(3L)
                .setCreateAction(Optional.empty())
                .setChangedAliases(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, 100L, Option.ALIASES_PROPERTY_NAME, "b", "", ActionType.DELETE))
                    .add(optionAction(1L, 100L, Option.ALIASES_PROPERTY_NAME, "", "h", ActionType.CREATE))
                    .build())
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, 101L, CategoryMatcherParamService.CUT_OFF_WORD, "X", "", ActionType.DELETE))
                    .add(optionAction(1L, 101L, CategoryMatcherParamService.CUT_OFF_WORD, "Y", "", ActionType.DELETE))
                    .add(optionAction(1L, 101L, CategoryMatcherParamService.CUT_OFF_WORD, "", "O", ActionType.CREATE))
                    .build()
                )
            )
            .setCategoryActions(new SquashedUserActions.CategoryActions()
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "EE", "", ActionType.DELETE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "RR", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "TT", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "MM", ActionType.CREATE))
                    .build()
                )
            );

        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.getHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.computeInspectorCorrections();
        helper.filterOutEmptyActions();
        helper.removeEmptyModelActions();

        contractorActions = helper.getContractorActions();
        inspectorActions = helper.getInspectorActions();
        SquashedUserActions corrections = helper.getCorrectionsActions();

        assertThat(contractorActions.getModelActionsById(1L).getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(1, "param1"));
        assertThat(contractorActions.getModelActionsById(2L).getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(2, "param2"));
        assertThat(contractorActions.getModelActionsById(1L).getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(11, "param11", "new"));
        assertThat(contractorActions.getModelActionsById(2L).getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(12, "param12", "new"));
        assertThat(contractorActions.getModelActionsById(1L).getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(11L, "option1"));
        assertThat(contractorActions.getModelActionsById(2L).getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(12L, "option12"));
        assertThat(contractorActions.getParamActionsMap().get(1L).getChangedAliases())
            .containsOnlyKeys(new GroupingSquashStrategy.PropertyValue(Option.ALIASES_PROPERTY_NAME, "c"));
        assertThat(contractorActions.getParamActionsMap().get(1L).getChangedCutOffWords())
            .containsOnlyKeys(
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "Z"));
        assertThat(contractorActions.getCategoryActions().getCutOffWords())
            .containsOnlyKeys(
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "II"));

        assertThat(inspectorActions.getModelActionsById(2L).getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(3, "param3"));
        assertThat(inspectorActions.getModelActionsById(3L).getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(1, "param1"));
        assertThat(inspectorActions.getModelActionsById(2L).getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(13, "param13", "new"));
        assertThat(inspectorActions.getModelActionsById(3L).getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(11, "param11", "new"));
        assertThat(inspectorActions.getModelActionsById(2L).getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(13L, "option13"));
        assertThat(inspectorActions.getModelActionsById(3L).getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(11L, "option1"));
        assertThat(inspectorActions.getParamActionsMap().get(1L).getChangedAliases())
            .containsOnlyKeys(new GroupingSquashStrategy.PropertyValue(Option.ALIASES_PROPERTY_NAME, "h"));
        assertThat(inspectorActions.getParamActionsMap().get(1L).getChangedCutOffWords())
            .containsOnlyKeys(
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "O"));
        assertThat(inspectorActions.getCategoryActions().getCutOffWords())
            .containsOnlyKeys(
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "RR"),
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "TT"),
                new GroupingSquashStrategy.PropertyValue(CategoryMatcherParamService.CUT_OFF_WORD, "MM"));

        assertThat(corrections.getModelActionsById(2L).getChangedSingleValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.SingleParamValue(1, "param1"));
        assertThat(corrections.getModelActionsById(2L).getChangedMultiValueParams())
            .containsOnlyKeys(new GroupingSquashStrategy.MultiParamValue(11, "param11", "new"));
        assertThat(corrections.getModelActionsById(2L).getChangedPickers())
            .containsOnlyKeys(new GroupingSquashStrategy.ParameterProperty(11L, "option11"));
        assertThat(corrections.getParamActionsMap().get(1L).getChangedAliases()).isEmpty();
        assertThat(corrections.getParamActionsMap().get(1L).getChangedCutOffWords()).isEmpty();
        assertThat(corrections.getCategoryActions().getCutOffWords()).isEmpty();
    }

    @Test
    public void testConvertToStatistics() {
        SquashedUserActions contractorActions = new SquashedUserActions()
            .addParamActions(new SquashedUserActions.CategoryParamActions() // Will be ignored as it is not vendor
                .setOptionId(5L)
                .setParamId(100L)
                .setCreateAction(Optional.of(
                    optionAction(5L, 100L, "", "", "", ActionType.CREATE)
                        .setUserId(OPERATOR_UID)
                ))
            )
            .addParamActions(new SquashedUserActions.CategoryParamActions()
                .setOptionId(1L)
                .setParamId(KnownIds.VENDOR_PARAM_ID)
                .setCreateAction(Optional.of(
                    optionAction(1L, KnownIds.VENDOR_PARAM_ID, "", "", "", ActionType.CREATE)
                        .setUserId(OPERATOR_UID)
                ))
                .setChangedAliases(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, Option.ALIASES_PROPERTY_NAME,
                        "a", "", ActionType.DELETE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, Option.ALIASES_PROPERTY_NAME,
                        "", "b", ActionType.CREATE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, Option.ALIASES_PROPERTY_NAME,
                        "", "c", ActionType.CREATE))
                    .build())
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "", "X", ActionType.CREATE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "", "Y", ActionType.CREATE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "", "Z", ActionType.CREATE))
                    .build()
                )
            )
            .setCategoryActions(new SquashedUserActions.CategoryActions()
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "AAA", "", ActionType.DELETE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "EE", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "II", ActionType.CREATE))
                    .build()
                )
            );
        SquashedUserActions inspectorActions = new SquashedUserActions()
            .addParamActions(new SquashedUserActions.CategoryParamActions()
                .setOptionId(1L)
                .setParamId(KnownIds.VENDOR_PARAM_ID)
                .setCreateAction(Optional.empty())
                .setChangedAliases(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, Option.ALIASES_PROPERTY_NAME,
                        "b", "", ActionType.DELETE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, Option.ALIASES_PROPERTY_NAME,
                        "", "h", ActionType.CREATE))
                    .build())
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "X", "", ActionType.DELETE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "Y", "", ActionType.DELETE))
                    .add(optionAction(1L, KnownIds.VENDOR_PARAM_ID, CategoryMatcherParamService.CUT_OFF_WORD,
                        "", "O", ActionType.CREATE))
                    .build()
                )
            )
            .setCategoryActions(new SquashedUserActions.CategoryActions()
                .setChangedCutOffWords(MapBuilder.propertyValueMapBuilder()
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "EE", "", ActionType.DELETE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "RR", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "TT", ActionType.CREATE))
                    .add(categoryAction(CategoryMatcherParamService.CUT_OFF_WORD, "", "MM", ActionType.CREATE))
                    .build()
                )
            );
        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.getHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.computeInspectorCorrections();
        helper.filterOutEmptyActions();

        //testing to statistics
        List<YangLogStorage.ParameterStatistic> paramStats =
            helper.getParameterStatistics(OPERATOR_UID, MANAGER_UID);
        assertThat(paramStats.size()).isEqualTo(1);

        YangLogStorage.ParameterStatistic paramStat = paramStats.get(0);
        assertThat(paramStat.getEntityId()).isEqualTo(1L);
        assertThat(paramStat.getChangesList()).containsExactlyInAnyOrder(
            YangLogStorage.ParameterActions.newBuilder()
                .setUid(OPERATOR_UID)
                .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                .setCreatedInTask(true)
                .setCreatedAction(createActionInfo())
                .addAliasesActions(createActionInfo())
                .addCutOffWordsActions(createActionInfo())
                .build(),
            YangLogStorage.ParameterActions.newBuilder()
                .setUid(MANAGER_UID)
                .setCreatedInTask(false)
                .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                .addAliasesActions(createActionInfo())
                .addCutOffWordsActions(createActionInfo())
                .build(),
            YangLogStorage.ParameterActions.newBuilder()
                .setUid(MANAGER_UID)
                .setCreatedInTask(false)
                .setChangesType(YangLogStorage.ChangesType.CORRECTIONS)
                //.addAllCutOffWordsActions(0)
                //.addAllAliasesActions(0)
                .build());

        YangLogStorage.CategoryStatistic categoryStat =
            helper.getCategoryStatistic(OPERATOR_UID, MANAGER_UID);
        assertThat(categoryStat.getContractorChanges()).isEqualTo(
            YangLogStorage.CategoryActions.newBuilder()
                .addCutOffWordsActions(createActionInfo())
                .build()
        );
        assertThat(categoryStat.getInspectorChanges()).isEqualTo(
            YangLogStorage.CategoryActions.newBuilder()
                .addAllCutOffWordsActions(Arrays.asList(
                    createActionInfo(),
                    createActionInfo(),
                    createActionInfo()
                ))
                .build()
        );
        assertThat(categoryStat.getInspectorCorrections()).isEqualTo(
            YangLogStorage.CategoryActions.newBuilder()
                //.addAllCutOffWordsActions(0)
                .build()
        );
    }

    @Test
    public void converterShouldComputeCountersCorrectly() {
        YangLogStorage.ModelActions modelActions =
            service.getHelper().convertToModelActions(modelActions(1L)
                .setChangedSingleValueParams(MapBuilder.singleParamValueMapBuilder()
                    .add(modelSingleParamAction(1L, "param1", "old", "new"))
                    .add(modelSingleParamAction(20L, XslNames.IS_SKU, "true", "false"))
                    .build())
                .setChangedMultiValueParams(MapBuilder.multiParamValueMapBuilder()
                    .add(modelParamAction(2L, "param2", "", "new", ActionType.CREATE))
                    .add(modelParamAction(2L, "param2", "", "more", ActionType.CREATE))
                    .add(modelParamAction(30L, XslNames.ALIASES, "", "new", ActionType.CREATE))
                    .add(modelParamAction(40L, XslNames.BAR_CODE, "", "new", ActionType.CREATE))
                    .add(modelParamAction(50L, XslNames.VENDOR_CODE, "", "new", ActionType.CREATE))
                    .add(modelParamAction(60L, XslNames.CUT_OFF_WORD, "", "new", ActionType.CREATE))
                    .build())
                .setChangedPickers(MapBuilder.parameterPropertyMapBuilder()
                    .single(modelPickerAction(1L, "option1", "old", "new"))));

        assertThat(modelActions.getAliasesCount()).isEqualTo(1);
        assertThat(modelActions.getBarCodeCount()).isEqualTo(1);
        assertThat(modelActions.getVendorCodeCount()).isEqualTo(1);
        assertThat(modelActions.getCutOffWordCount()).isEqualTo(1);
        assertThat(modelActions.getIsSkuCount()).isEqualTo(1);
        assertThat(modelActions.getParamCount()).isEqualTo(3);
        assertThat(modelActions.getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(1L, 2L, 2L);
        assertThat(modelActions.getPickerAddedCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void shouldComputeNewRequestFromOriginalRequestAndAudit() {
        refreshTime();
        long contractorUserId = 1L;
        long inspectorUserId = 2L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(inspectorUserId)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(1L)
            .build();

        Mockito.when(modelStorageService.getModels(Mockito.eq(1L), Mockito.anyCollection()))
            .thenAnswer(invocation -> {
                ImmutableMap<Long, CommonModel.Source> sources = ImmutableMap.<Long, CommonModel.Source>builder()
                    .put(1L, CommonModel.Source.GURU)
                    .put(3L, CommonModel.Source.SKU)
                    .put(4L, CommonModel.Source.GURU)
                    .build();

                Collection<Long> modelIds = invocation.getArgument(1);
                return modelIds.stream()
                    .map(id -> sources.containsKey(id) ? createModel(id, sources.get(id)) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            });

        mockExtractorServiceForNewRequestComputation();
        writeActionsForNewRequestComputation(contractorUserId, inspectorUserId);

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);

        assertThat(res.getCategoryId()).isEqualTo(originalRequest.getCategoryId());
        assertThat(res.getId()).isEqualTo(originalRequest.getId());
        assertThat(res.getHitmanId()).isEqualTo(originalRequest.getHitmanId());
        assertThat(res.getContractorInfo()).isEqualTo(originalRequest.getContractorInfo());
        assertThat(res.getInspectorInfo()).isEqualTo(originalRequest.getInspectorInfo());

        Map<Long, YangLogStorage.ModelStatistic> modelStatistic = res.getModelStatisticList().stream()
            .collect(Collectors.toMap(YangLogStorage.ModelStatistic::getModelId, Function.identity()));
        assertThat(modelStatistic).containsOnlyKeys(1L, 3L, 4L);

        YangLogStorage.ModelStatistic model1 = modelStatistic.get(1L);
        assertThat(model1.getType()).isEqualTo(ModelStorage.ModelType.GURU);
        assertThat(model1.getModelId()).isEqualTo(1L);
        assertThat(model1.getCreatedInTask()).isFalse();
        // contractor
        assertThat(model1.getContractorActions().getAliasesCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getVendorCodeCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getBarCodeCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getCutOffWordCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getIsSkuCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getPickerAddedCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getPictureUploadedCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getPictureCopiedCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getParamCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(2L);
        // inspector
        assertThat(model1.getInspectorActions().getAliasesCount()).isEqualTo(1);
        assertThat(model1.getInspectorActions().getVendorCodeCount()).isEqualTo(0);
        assertThat(model1.getInspectorActions().getBarCodeCount()).isEqualTo(0);
        assertThat(model1.getInspectorActions().getCutOffWordCount()).isEqualTo(1);
        assertThat(model1.getInspectorActions().getIsSkuCount()).isEqualTo(0);
        assertThat(model1.getInspectorActions().getPickerAddedCount()).isEqualTo(0);
        assertThat(model1.getInspectorActions().getPictureUploadedCount()).isEqualTo(0);
        assertThat(model1.getInspectorActions().getParamCount()).isEqualTo(1);
        assertThat(model1.getInspectorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(4L);
        // corrections
        assertThat(model1.getCorrectionsActions().getAliasesCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getVendorCodeCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getBarCodeCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getCutOffWordCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getIsSkuCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getPickerAddedCount()).isEqualTo(0);
        assertThat(model1.getCorrectionsActions().getPictureUploadedCount()).isEqualTo(1);
        assertThat(model1.getCorrectionsActions().getParamCount()).isEqualTo(2);
        assertThat(model1.getCorrectionsActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(1L, 11L);

        // created passed correctly
        YangLogStorage.ModelStatistic model3 = modelStatistic.get(3L);
        assertThat(model3.getType()).isEqualTo(ModelStorage.ModelType.SKU);
        assertThat(model3.getModelId()).isEqualTo(3L);
        assertThat(model3.getCreatedInTask()).isTrue();
        assertThat(model3.getCreatedByUid()).isEqualTo(contractorUserId);
        assertThat(model3.getContractorActions().getParamCount()).isEqualTo(1);
        assertThat(model3.getContractorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(11L);

        // created passed correctly
        YangLogStorage.ModelStatistic model4 = modelStatistic.get(4L);
        assertThat(model4.getType()).isEqualTo(ModelStorage.ModelType.GURU);
        assertThat(model4.getModelId()).isEqualTo(4L);
        assertThat(model4.getCreatedInTask()).isTrue();
        assertThat(model4.getCreatedByUid()).isEqualTo(inspectorUserId);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void shouldIgnoreInspectorAuditWhenInspectorInfoNotPresentInRequest() {
        refreshTime();
        long contractorUserId = 1L;
        long inspectorUserId = 2L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(1L)
            .build();

        Mockito.when(modelStorageService.getModels(Mockito.eq(1L), Mockito.anyCollection()))
            .thenAnswer(invocation -> {
                Collection<Long> modelIds = invocation.getArgument(1);
                Assertions.assertThat(modelIds).containsExactlyInAnyOrder(1L, 2L, 3L);
                return Arrays.asList(
                    createModel(1L, CommonModel.Source.GURU),
                    createModel(2L, CommonModel.Source.GURU),
                    createModel(3L, CommonModel.Source.SKU));
            });

        mockExtractorServiceForNewRequestComputation();
        writeActionsForNewRequestComputation(contractorUserId, inspectorUserId);

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);

        assertThat(res.getCategoryId()).isEqualTo(originalRequest.getCategoryId());
        assertThat(res.getId()).isEqualTo(originalRequest.getId());
        assertThat(res.getHitmanId()).isEqualTo(originalRequest.getHitmanId());
        assertThat(res.getContractorInfo()).isEqualTo(originalRequest.getContractorInfo());
        assertThat(res.getInspectorInfo()).isEqualTo(originalRequest.getInspectorInfo());

        Map<Long, YangLogStorage.ModelStatistic> modelStatistic = res.getModelStatisticList().stream()
            .collect(Collectors.toMap(YangLogStorage.ModelStatistic::getModelId, Function.identity()));
        assertThat(modelStatistic).containsOnlyKeys(1L, 2L, 3L);

        YangLogStorage.ModelStatistic model1 = modelStatistic.get(1L);
        assertThat(model1.getType()).isEqualTo(ModelStorage.ModelType.GURU);
        assertThat(model1.getModelId()).isEqualTo(1L);
        assertThat(model1.getCreatedInTask()).isFalse();
        // contractor
        assertThat(model1.getContractorActions().getAliasesCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getVendorCodeCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getBarCodeCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getCutOffWordCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getIsSkuCount()).isEqualTo(0);
        assertThat(model1.getContractorActions().getPickerAddedCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getPictureUploadedCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getPictureCopiedCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getParamCount()).isEqualTo(2);
        assertThat(model1.getContractorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(model1.hasInspectorActions()).isFalse();
        assertThat(model1.hasCorrectionsActions()).isFalse();

        YangLogStorage.ModelStatistic model2 = modelStatistic.get(2L);
        assertThat(model2.getType()).isEqualTo(ModelStorage.ModelType.GURU);
        assertThat(model2.getModelId()).isEqualTo(2L);
        assertThat(model2.getContractorActions().getParamCount()).isEqualTo(1);
        assertThat(model2.getContractorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(1L);
        assertThat(model2.hasInspectorActions()).isFalse();
        assertThat(model2.hasCorrectionsActions()).isFalse();

        // created passed correctly
        YangLogStorage.ModelStatistic model3 = modelStatistic.get(3L);
        assertThat(model3.getType()).isEqualTo(ModelStorage.ModelType.SKU);
        assertThat(model3.getModelId()).isEqualTo(3L);
        assertThat(model3.getCreatedInTask()).isTrue();
        assertThat(model3.getCreatedByUid()).isEqualTo(contractorUserId);
        assertThat(model3.getContractorActions().getParamCount()).isEqualTo(1);
        assertThat(model3.getContractorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(11L);
        assertThat(model3.hasInspectorActions()).isFalse();
        assertThat(model3.hasCorrectionsActions()).isFalse();
    }

    private void mockExtractorServiceForNewRequestComputation() {
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(1))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter("param1", false, 1L))
                .addParameter(createParameter("param2", false, 2L))
                .addParameter(createParameter("param3", false, 3L))
                .addParameter(createParameter("param4", false, 4L))
                .addParameter(createParameter("technical_param", false, 5L))
                .addParameter(createParameter(XslNames.IS_SKU, false, 6L))
                .addParameter(createParameter(XslNames.XL_PICTURE, false, 7L))
                .addParameter(createParameter(XslNames.XL_PICTURE + "_1", false, 8L))

                .addParameter(createParameter("multiparam1", true, 11L))
                .addParameter(createParameter("technical_multiparam", true, 13L))
                .addParameter(createParameter(XslNames.ALIASES, true, 14L))
                .addParameter(createParameter(XslNames.BAR_CODE, true, 15L))
                .addParameter(createParameter(XslNames.VENDOR_CODE, true, 16L))
                .addParameter(createParameter(XslNames.CUT_OFF_WORD, true, 17L))
                .build()));
    }

    private void writeActionsForNewRequestComputation(long contractorUserId, long inspectorUserId) {
        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            // model 1 contractor
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old", "new", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                2L, "param2", "old2", "new2", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                3L, "param3", "old3", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                5L, "technical_param", "old", "new", 1L,
                AuditAction.BillingMode.BILLING_MODE_NONE, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                6L, XslNames.IS_SKU, "true", "false", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PICTURE, ActionType.UPDATE,
                7L, XslNames.XL_PICTURE, "true", "false", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PICTURE, ActionType.CREATE,
                8L, XslNames.XL_PICTURE + "_1", "", "false1", 1L,
                BillingMode.BILLING_MODE_COPY, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.DELETE,
                11L, "multiparam1", "oldval", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                15L, XslNames.BAR_CODE, "", "oldval", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                16L, XslNames.VENDOR_CODE, "", "oldval", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                17L, XslNames.CUT_OFF_WORD, "", "oldval1", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PICKER, ActionType.CREATE,
                18L, XslNames.CUT_OFF_WORD, "", "oldval1", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            // model 1 inspector
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "new", "newest", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                4L, "param4", "old", "new", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                11L, "multiparam1", "", "oldval", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                13L, "technical_multiparam", "", "oldval", 1L,
                AuditAction.BillingMode.BILLING_MODE_NONE, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                14L, XslNames.ALIASES, "", "oldval", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.DELETE,
                16L, XslNames.VENDOR_CODE, "oldval", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_NONE, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                17L, XslNames.CUT_OFF_WORD, "", "oldval2", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PICTURE, ActionType.UPDATE,
                7L, XslNames.XL_PICTURE, "false", "new-pic", 1L,
                BillingMode.BILLING_MODE_FILL, inspectorUserId))

            // model 2 (deleted) contractor
            .add(createAuditAction(2L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old", "new", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            // model 2 (deleted) inspector
            .add(createAuditAction(2L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                2L, "param2", "new", "newest", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(2L, EntityType.MODEL_GURU, ActionType.DELETE,
                null, "", "", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_NONE, inspectorUserId))

            // model 3 (created) contractor
            .add(createAuditAction(3L, EntityType.MODEL_SKU, ActionType.CREATE,
                null, "", "", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL, contractorUserId))
            .add(createAuditAction(3L, EntityType.SKU_PARAM, ActionType.CREATE,
                11L, "multiparam1", "", "ololo", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL, contractorUserId))

            // model 4 (created) inspector
            .add(createAuditAction(4L, EntityType.MODEL_GURU, ActionType.CREATE,
                null, "", "", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL, inspectorUserId))
            .build());
    }

    @Test
    public void shouldConvertAuditToActions() {
        refreshToNow();
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(1))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(1)
                .addParameter(createParameter("param1", false, 1L))
                .addParameter(createParameter(XslNames.IS_SKU, false, 2L))
                .addParameter(createParameter("technical_param", false, 3L))

                .addParameter(createParameter(XslNames.XL_PICTURE, false, 11L))

                .addParameter(createParameter("multiparam1", true, 21L))
                .addParameter(createParameter(XslNames.BAR_CODE, true, 22L))
                .addParameter(createParameter(XslNames.VENDOR_CODE, true, 23L))
                .addParameter(createParameter(XslNames.CUT_OFF_WORD, true, 24L))
                .addParameter(createParameter(XslNames.ALIASES, true, 25L))

                .addParameter(createParameter("picker_param", true, 31L))
                .build()));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old1", "new1", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                2L, XslNames.IS_SKU, "false", "true", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                3L, "technical_param", "old", "new", 1L,
                AuditAction.BillingMode.BILLING_MODE_NONE, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PICTURE, ActionType.UPDATE,
                11L, XslNames.XL_PICTURE, "old11", "new11", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.DELETE,
                21L, "multiparam1", "old21", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                22L, XslNames.BAR_CODE, "", "new22", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                23L, XslNames.VENDOR_CODE, "", "new23", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                24L, XslNames.CUT_OFF_WORD, "", "new24", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                25L, XslNames.ALIASES, "", "new25", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(1L, EntityType.MODEL_PICKER, ActionType.CREATE,
                31L, "violet", "", "new31", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .build());

        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenReturn(Collections.singletonList(createModel(1, CommonModel.Source.GURU)));

        YangLogStorage.YangGetAuditActionsResponse yangAuditActions = service
            .getYangAuditActions(YangLogStorage.YangGetAuditActionsRequest.newBuilder()
                .setCategoryId(1L)
                .setIncludeRuleActions(false)
                .setUid(OPERATOR_UID)
                .setTaskId("1234")
                .build());

        MboUsers.MboUser user = yangAuditActions.getUser();
        assertThat(user.getUid()).isEqualTo(TEST_USER.getUid());
        assertThat(user.getMboFullname()).isEqualTo(TEST_USER.getFullname());
        assertThat(yangAuditActions.getModelActionsList()).hasSize(1);
        ModelAuditActions modelActions =
            yangAuditActions.getModelActions(0);

        assertThat(modelActions.getModelId()).isEqualTo(1L);
        assertThat(modelActions.getParamList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.UPDATE,
                "old1", "new1", 1L, "param1", 0),
            buildAction(MboAudit.ActionType.DELETE,
                "old21", "", 21L, "multiparam1", 4));
        assertThat(modelActions.getIsSku()).isEqualTo(
            buildAction(MboAudit.ActionType.UPDATE,
                "false", "true", 2L, XslNames.IS_SKU, 1));
        assertThat(modelActions.getBarCodeList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.CREATE,
                "", "new22", 22L, XslNames.BAR_CODE, 5));
        assertThat(modelActions.getVendorCodeList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.CREATE,
                "", "new23", 23L, XslNames.VENDOR_CODE, 6));
        assertThat(modelActions.getCutOffWordList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.CREATE,
                "", "new24", 24L, XslNames.CUT_OFF_WORD, 7));
        assertThat(modelActions.getAliasesList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.CREATE,
                "", "new25", 25L, XslNames.ALIASES, 8));

        assertThat(modelActions.getPickerList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.CREATE,
                "", "new31", 31L, "picker_param", 9).toBuilder()
                .setOptionName("violet")
                .build());

        assertThat(modelActions.getPictureList()).containsExactlyInAnyOrder(
            buildAction(MboAudit.ActionType.UPDATE,
                "old11", "new11", 11L, XslNames.XL_PICTURE, 3));


        YangLogStorage.YangGetAuditActionsResponse yangAuditActionsNoUid =
            service.getYangAuditActions(YangLogStorage.YangGetAuditActionsRequest.newBuilder()
                .setCategoryId(1L)
                .setIncludeRuleActions(false)
                .setTaskId("1234")
                .build());

        assertThat(yangAuditActionsNoUid).isEqualTo(yangAuditActions);
    }

    @Test
    public void shouldWorkForDifferentTaskTypes() {
        refreshToNow();
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter("param1", false, 1L))
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenReturn(Collections.singletonList(createModel(1L, CommonModel.Source.GURU)));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old1", "new1", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .build());
        List<YangLogStorage.YangLogStoreRequest> yangAuditActionsList =
            Stream.of(
                YangLogStorage.YangTaskType.BLUE_LOGS,
                YangLogStorage.YangTaskType.WHITE_LOGS,
                YangLogStorage.YangTaskType.DEEPMATCHER_LOGS,
                YangLogStorage.YangTaskType.CONTENT_LAB,
                YangLogStorage.YangTaskType.MSKU_FROM_PSKU_GENERATION,
                YangLogStorage.YangTaskType.FILL_SKU,
                YangLogStorage.YangTaskType.FMCG_BLUE_LOGS
            )
                .map(tt -> service.computeModelStatsTask(
                    YangLogStorage.YangLogStoreRequest.newBuilder()
                        .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                            .setUid(OPERATOR_UID)
                            .build())
                        .setTaskType(tt)
                        .setId("1234")
                        .setHitmanId(1234L)
                        .setCategoryId(CATEGORY_ID)
                        .build()
                    )
                )
                .collect(Collectors.toList());

        yangAuditActionsList.forEach(resp -> assertThat(resp.getModelStatisticCount()).isEqualTo(1));
    }

    @Test
    public void testModelsLoadingCache() {
        refreshToNow();
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter("param1", false, 1L))
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenAnswer(invocation -> {
                Collection<Long> modelIds = invocation.getArgument(1);
                assertThat(modelIds).hasSize(1);
                return modelIds.stream()
                    .map(id -> createModel(id, CommonModel.Source.GURU))
                    .collect(Collectors.toList());
            });

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old1", "new1", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, OPERATOR_UID))
            .add(createAuditAction(2L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                1L, "param1", "old1", "new1", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, MANAGER_UID))
            .build());
        YangLogStorage.YangLogStoreRequest resp = service.computeModelStatsTask(
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(OPERATOR_UID)
                    .build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(MANAGER_UID)
                    .build())
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setId("1234")
                .setHitmanId(1234L)
                .setCategoryId(CATEGORY_ID)
                .build()
        );

        assertThat(resp.getModelStatisticCount()).isEqualTo(2);
    }

    @Test
    public void shouldTakeCategoryAndOptionsActions() {
        refreshToNow();
        long contractorUserId = 1;
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(1L))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(1L)
                .addParameter(createParameter(XslNames.VENDOR, true, 23L))
                .build()));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            // Category cut off word changes - should make it into the final result
            .add(createAuditAction(1L, EntityType.CATEGORY, ActionType.CREATE,
                null, XslNames.CUT_OFF_WORD, "", "чехол", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.CATEGORY, ActionType.UPDATE,
                null, XslNames.CUT_OFF_WORD, "мяо-цынь", "мясынь", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.CATEGORY, ActionType.DELETE,
                null, XslNames.CUT_OFF_WORD, "uwu", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))

            // Some other random category attribute - should be ignored
            .add(createAuditAction(1L, EntityType.CATEGORY, ActionType.CREATE,
                null, XslNames.ALIASES, "", "Мобильные мясыни", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))

            // Parameter option changes - aliases and cut off words are ok, the rest is ignored
            .add(createAuditAction(1L, EntityType.OPTION, ActionType.CREATE,
                23L, XslNames.CUT_OFF_WORD, "", "Уф", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.OPTION, ActionType.UPDATE,
                23L, XslNames.CUT_OFF_WORD, "Ыть", "Пыть", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.OPTION, ActionType.DELETE,
                23L, Option.ALIASES_PROPERTY_NAME, "Мяу", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.OPTION, ActionType.DELETE,
                23L, "Опубликованность", "Да", "", 1L,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .build());
        YangLogStorage.YangGetAuditActionsResponse yangAuditActions = service
            .getYangAuditActions(YangLogStorage.YangGetAuditActionsRequest.newBuilder()
                .setCategoryId(1L)
                .setIncludeRuleActions(false)
                .setUid(contractorUserId)
                .setTaskId("1234")
                .build());
        assertThat(yangAuditActions.getModelActionsList()).isEmpty();
        CategoryActions categoryActions = yangAuditActions.getCategoryActions();
        assertThat(categoryActions.getCutOffWordsList()).hasSize(3);
        assertThat(categoryActions.getCutOffWordsList()).containsExactlyInAnyOrder(
            buildPropAction(MboAudit.ActionType.CREATE, "", "чехол", 0),
            buildPropAction(MboAudit.ActionType.UPDATE, "мяо-цынь", "мясынь", 1),
            buildPropAction(MboAudit.ActionType.DELETE, "uwu", "", 2)
        );

        assertThat(yangAuditActions.getCategoryParamActionsList()).hasSize(1);
        assertThat(yangAuditActions.getCategoryParamActionsList()).containsExactlyInAnyOrder(
            buildParamAction(
                23L,
                XslNames.VENDOR,
                Arrays.asList(buildPropAction(MboAudit.ActionType.DELETE, "Мяу", "", 6)),
                Arrays.asList(buildPropAction(MboAudit.ActionType.CREATE, "", "Уф", 4),
                    buildPropAction(MboAudit.ActionType.UPDATE, "Ыть", "Пыть", 5))
            )
        );
    }

    @Test
    public void shouldRemoveModelAutoAliases() {
        refreshTime();
        long contractorUserId = 1L;
        long inspectorUserId = 2L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(inspectorUserId)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(CATEGORY_ID)
            .build();

        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter("multiparam1", true, 11L))
                .addParameter(createParameter(XslNames.ALIASES, true, 14L))
                .addParameter(createParameter(XslNames.BAR_CODE, true, 15L))
                .addParameter(createParameter(XslNames.VENDOR_CODE, true, 16L))
                .addParameter(createParameter(XslNames.CUT_OFF_WORD, true, 17L))
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(1L), Mockito.anyCollection()))
            .thenReturn(Arrays.asList(
                createModel(1L, CommonModel.Source.GURU)));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            // model 1 contractor
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                15L, XslNames.BAR_CODE, "", "oldval", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                15L, XslNames.BAR_CODE, "", "title", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))

            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                14L, XslNames.ALIASES, "", "oldval", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                14L, XslNames.ALIASES, "", "title", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                14L, XslNames.ALIASES, "", "bar_code", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                14L, XslNames.ALIASES, "", "vendor_code", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))

            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                17L, XslNames.CUT_OFF_WORD, "", "oldval1", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                17L, XslNames.CUT_OFF_WORD, "", "title", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))

            // model 1 inspector
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                11L, "multiparam1", "", "oldval", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                11L, "multiparam1", "", "title", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))

            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                16L, XslNames.VENDOR_CODE, "", "oldval", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                16L, XslNames.VENDOR_CODE, "", "title", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, inspectorUserId))
            .build());

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);

        YangLogStorage.ModelStatistic model1 = res.getModelStatisticList().get(0);
        // contractor
        assertThat(model1.getContractorActions().getBarCodeCount()).isEqualTo(2);
        assertThat(model1.getContractorActions().getAliasesCount()).isEqualTo(1);
        assertThat(model1.getContractorActions().getCutOffWordCount()).isEqualTo(2);
        // inspector
        assertThat(model1.getInspectorActions().getParamCount()).isEqualTo(2);
        assertThat(model1.getInspectorActions().getParamList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(11L, 11L);
        assertThat(model1.getInspectorActions().getVendorCodeCount()).isEqualTo(2);
    }

    @Test
    public void shouldRemoveParameterAutoAliases() {
        refreshTime();
        long contractorUserId = 1L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(CATEGORY_ID)
            .build();

        long localVendorId = 654321L;
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .addParameter(createLocalVendor(KnownIds.VENDOR_PARAM_ID, localVendorId, "remove_me!"))
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(1L), Mockito.anyCollection()))
            .thenReturn(Arrays.asList(createModel(1L, CommonModel.Source.GURU)));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(optionAction(localVendorId, KnownIds.VENDOR_PARAM_ID, CATEGORY_ID, Option.ALIASES_PROPERTY_NAME,
                "", "bill_me!", ActionType.CREATE, contractorUserId))
            .add(optionAction(localVendorId, KnownIds.VENDOR_PARAM_ID, CATEGORY_ID, Option.ALIASES_PROPERTY_NAME,
                "", "remove_me!", ActionType.CREATE, contractorUserId))
            .build());

        Optional<YangLogStorage.ParameterStatistic> vendorStat = service.computeModelStatsTask(originalRequest)
            .getParameterStatisticList()
            .stream()
            .filter(s -> s.getEntityId() == localVendorId)
            .findFirst();
        assertThat(vendorStat.isPresent()).isTrue();
        assertThat(vendorStat.get().getChanges(0).getAliasesActionsCount()).isEqualTo(1);
    }

    @Test
    public void shouldRemoveIsSkuReset() {
        refreshTime();
        long contractorUserId = 1L;
        long inspectorUserId = 2L;
        long parameterId = 14L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(inspectorUserId)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(1234L)
            .setCategoryId(CATEGORY_ID)
            .build();

        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter(XslNames.IS_SKU, false, parameterId))
                .build()));

        long model1 = 1L;
        long model2 = 2L;
        long model3 = 3L;
        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenReturn(Arrays.asList(
                createModel(model1, CommonModel.Source.GURU),
                createModel(model2, CommonModel.Source.GURU),
                createModel(model3, CommonModel.Source.GURU)));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                parameterId, XslNames.IS_SKU, "true", "false", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(2L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                parameterId, XslNames.IS_SKU, "true", "", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .add(createAuditAction(3L, EntityType.MODEL_PARAM, ActionType.UPDATE,
                parameterId, XslNames.IS_SKU, "", "true", CATEGORY_ID,
                AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .build());

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);
        assertThat(res.getModelStatisticCount()).isEqualTo(3L);
        Map<Long, YangLogStorage.ModelStatistic> modelIdToStatistics = res.getModelStatisticList()
            .stream().collect(Collectors.toMap(YangLogStorage.ModelStatistic::getModelId, s -> s));
        assertThat(modelIdToStatistics.get(model1).getContractorActions().getIsSkuCount()).isEqualTo(0);
        assertThat(modelIdToStatistics.get(model2).getContractorActions().getIsSkuCount()).isEqualTo(0);
        assertThat(modelIdToStatistics.get(model3).getContractorActions().getIsSkuCount()).isEqualTo(1);
        assertThat(modelIdToStatistics.get(model1).hasIsSku()).isEqualTo(true);
        assertThat(modelIdToStatistics.get(model1).getIsSku()).isEqualTo(false);
        assertThat(modelIdToStatistics.get(model2).hasIsSku()).isEqualTo(true);
        assertThat(modelIdToStatistics.get(model2).getIsSku()).isEqualTo(false);
        assertThat(modelIdToStatistics.get(model3).hasIsSku()).isEqualTo(true);
        assertThat(modelIdToStatistics.get(model3).getIsSku()).isEqualTo(true);
    }

    @Test
    public void testGroupCategoryMoveDownOldParam() {
        CommonModel model = createModel(1L, CommonModel.Source.GURU);
        CommonModel modif1 = createModel(2L, CommonModel.Source.GURU);
        modif1.setParentModelId(1L);
        CommonModel modif2 = createModel(3L, CommonModel.Source.GURU);
        modif2.setParentModelId(1L);
        addUserParam(model, 1L, "vla", 1923123L);

        applyChangesAndDoAudit(Arrays.asList(model, modif1, modif2), (models) -> {
            models.stream().filter(m -> m.getId() == 2L).forEach(
                mod -> addUserParam(mod, 1L, "bla", OPERATOR_UID));
            return models;
        });
        mockCategoryResponse(1L);
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(
            defaultRequest(false)
        );
        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(2L);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamCount()).isEqualTo(1);
    }

    @Test
    public void testGroupCategoryMoveDownParam() {
        CommonModel model = createModel(1L, CommonModel.Source.GURU);
        CommonModel modif1 = createModel(2L, CommonModel.Source.GURU);
        modif1.setParentModelId(1L);
        CommonModel modif2 = createModel(3L, CommonModel.Source.GURU);
        modif2.setParentModelId(1L);
        Collection<CommonModel> allModels = Arrays.asList(model, modif1, modif2);
        applyChangesAndDoAudit(allModels, (models) -> {
            models.stream()
                .peek(m -> m.setModifiedUserId(OPERATOR_UID))
                .filter(m -> m.getId() == 1L).forEach(
                mod -> {
                    addUserParam(mod, 1L, "vla", OPERATOR_UID);
                    addUserParam(mod, 2L, "vlawer", OPERATOR_UID);
                });
            return models;
        });
        applyChangesAndDoAudit(allModels, (models) -> {
            models.stream()
                .peek(m -> m.setModifiedUserId(OPERATOR_UID))
                .filter(m -> m.getId() == 2L).forEach(
                mod -> addUserParam(mod, 1L, "bla", OPERATOR_UID));
            return models;
        });
        mockCategoryResponse(1L, 2L);
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(
            defaultRequest(false)
        );
        assertThat(response.getModelStatisticList()).extracting(YangLogStorage.ModelStatistic::getModelId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(response.getModelStatisticList().stream()
            .sorted(Comparator.comparingLong(YangLogStorage.ModelStatistic::getModelId))
            .collect(Collectors.toList()))
            .extracting(m -> m.getContractorActions().getParamCount())
            .containsExactly(2, 1, 0);
        //this will move param back to model
        applyChangesAndDoAudit(allModels, (models) -> {
            models.stream()
                .peek(m -> m.setModifiedUserId(OPERATOR_UID))
                .filter(m -> m.getId() == 3L).forEach(
                mod -> {
                    mod.removeAllParameterValues(1L);
                    addUserParam(mod, 1L, "bla", OPERATOR_UID);
                });
            return models;
        });
        response = service.computeModelStatsFromAudit(
            defaultRequest(false)
        );
        assertThat(response.getModelStatisticList()).extracting(YangLogStorage.ModelStatistic::getModelId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(response.getModelStatisticList().stream()
            .sorted(Comparator.comparingLong(YangLogStorage.ModelStatistic::getModelId))
            .collect(Collectors.toList()))
            .extracting(m -> m.getContractorActions().getParamCount())
            .containsExactly(1, 0, 0);
    }

    @Test
    public void testGroupCategoryMoveDownPicture() {
        CommonModel model = createModel(1L, CommonModel.Source.GURU);
        CommonModel modif1 = createModel(2L, CommonModel.Source.GURU);
        modif1.setParentModelId(1L);
        CommonModel modif2 = createModel(3L, CommonModel.Source.GURU);
        modif2.setParentModelId(1L);
        Collection<CommonModel> allModels = Arrays.asList(model, modif1, modif2);
        applyChangesAndDoAudit(allModels, (models) -> {
            models.stream()
                .peek(m -> m.setModifiedUserId(OPERATOR_UID))
                .filter(m -> m.getId() == 1L).forEach(
                mod -> {
                    pictureParams("url1", OPERATOR_UID).forEach(pv -> addParam(mod, () -> pv));
                    mod.updatePicturesFromParams();
                });
            return models;
        });
        applyChangesAndDoAudit(allModels, (models) -> {
            models.stream()
                .peek(m -> m.setModifiedUserId(OPERATOR_UID))
                .filter(m -> m.getId() == 2L).forEach(
                mod -> {
                    pictureParams("url2", OPERATOR_UID).forEach(pv -> addParam(mod, () -> pv));
                    mod.updatePicturesFromParams();
                });
            return models;
        });
        mockCategoryResponse();
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(
            defaultRequest(false)
        );
        assertThat(response.getModelStatisticList()).extracting(YangLogStorage.ModelStatistic::getModelId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(response.getModelStatisticList().stream()
            .sorted(Comparator.comparingLong(YangLogStorage.ModelStatistic::getModelId))
            .collect(Collectors.toList()))
            .extracting(m -> m.getContractorActions().getPictureUploadedCount())
            .containsExactly(1, 1, 0);
        assertThat(response.getModelStatisticList())
            .extracting(m -> m.getContractorActions().getPictureCopiedCount())
            .containsExactly(0, 0, 0);
    }

    @Test
    public void shouldWriteParameterIdToStatisticForBarcodeConflictsTask() {
        refreshTime();
        long contractorUserId = 1L;
        long parameterId = 14L;
        long model = 1L;
        YangLogStorage.YangLogStoreRequest originalRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(contractorUserId)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(1234L)
            .setCategoryId(CATEGORY_ID)
            .addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
                .setModelId(model)
                .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                    .setParameterId(parameterId)
                    .build())
                .build())
            .build();

        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addParameter(createParameter(XslNames.BAR_CODE, true, parameterId))
                .build()));

        Mockito.when(modelStorageService.getModels(Mockito.eq(CATEGORY_ID), Mockito.anyCollection()))
            .thenReturn(Arrays.asList(createModel(model, CommonModel.Source.GURU)));

        auditServiceMock.writeActions(ImmutableList.<AuditAction>builder()
            .add(createAuditAction(1L, EntityType.MODEL_PARAM, ActionType.CREATE,
                parameterId, XslNames.BAR_CODE, null, "1234", CATEGORY_ID,
                BillingMode.BILLING_MODE_FILL_CUSTOM, contractorUserId))
            .build());

        YangLogStorage.YangLogStoreRequest res = service.computeModelStatsTask(originalRequest);
        assertThat(res.getModelStatisticCount()).isEqualTo(1L);
        Map<Long, YangLogStorage.ModelStatistic> modelIdToStatistics = res.getModelStatisticList()
            .stream().collect(Collectors.toMap(YangLogStorage.ModelStatistic::getModelId, s -> s));
        assertThat(modelIdToStatistics.get(model).getContractorActions().getBarCodeCount()).isEqualTo(1);
        assertThat(modelIdToStatistics.get(model).getContractorActions().getBarCode(0).getEntityId())
            .isEqualTo(parameterId);
    }

    @Test
    public void testComputeParamMetadataValueSourceChanges() {
        long modelId = 1L;
        long paramId = 1L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        addUserParam(model, paramId, "vla", 1923123L);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    addUserParam(mod, paramId, "bla", OPERATOR_UID);

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_FILLED))
                    );
                });
            return models;
        });
        mockCategoryResponse(paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
                .setModelId(modelId)
                .setUid(OPERATOR_UID)
                .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                    .setParameterId(paramId)
                    .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamCount()).isEqualTo(1);

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(1);
        YangLogStorage.ParameterConflict parameterConflict =
            skuParameterConflictStatistic.getParameterConflictsList().get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataChangesValueSourceWithOtherChanges() {
        long modelId = 1L;
        long paramId = 1L;
        long otherParamId = 1234L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        addUserParam(model, paramId, "vla", 1923123L);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    addParam(model, () -> {
                        ParameterValue param = userParam(paramId, "bla", OPERATOR_UID);
                        param.setConfidence(123432);
                        param.setOwnerId(1234L);
                        return param;
                    });

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_VIEWED)
                        )
                    );
                });
            return models;
        });
        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    addParam(model, () -> {
                        ParameterValue param = userParam(paramId, "bla2", OPERATOR_UID);
                        param.setConfidence(4324324);
                        param.setOwnerId(324534L);
                        return param;
                    });
                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_FILLED),
                            otherParamId, new ParameterValueMetadata(otherParamId, ModificationSource.OPERATOR_VIEWED)
                        )
                    );
                });
            return models;
        });
        mockCategoryResponse(paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamCount()).isEqualTo(1);

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(1);
        YangLogStorage.ParameterConflict parameterConflict =
            skuParameterConflictStatistic.getParameterConflictsList().get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataChangesMultivalueParamDeleted() {
        long modelId = 1L;
        long paramId = 1L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        List<String> barcodes = ImmutableList.of("b1", "b2", "b3", "b4");
        String xslName = "BarCode";
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.STRING,
            barcodes.stream().map(WordUtil::defaultWord).toArray(Word[]::new));
        model.putParameterValues(parameterValues);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    model.setModifiedUserId(OPERATOR_UID);

                    ParameterValues parameterValuesNew = new ParameterValues(paramId, xslName, Param.Type.STRING,
                        List.of("b1", "b2", "new1").stream().map(WordUtil::defaultWord).toArray(Word[]::new));
                    parameterValuesNew.setLastModificationInfo(ModificationSource.OPERATOR_FILLED, OPERATOR_UID, new Date());

                    model.putParameterValues(parameterValuesNew);

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_FILLED)
                        )
                    );
                });
            return models;
        });

        mockCategoryResponse(true, paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        String s = JsonFormat.printToString(response);

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getBarCodeCount()).isEqualTo(1); // 1 added

        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getRemovedParamCount()).isEqualTo(2); // 2 removed

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(1);
        YangLogStorage.ParameterConflict parameterConflict =
            skuParameterConflictStatistic.getParameterConflictsList().get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataChangesDoNotBillActionsNotInConflict() {
        long modelId = 1L;
        long paramId = 777L;
        long otherParamId = 1234L;
        long otherParamId2 = 23423L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        addUserParam(model, paramId, "vla", 1923123L);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    addParam(model, () -> {
                        ParameterValue param = userParam(paramId, "bla", OPERATOR_UID);
                        param.setConfidence(123432);
                        param.setOwnerId(1234L);
                        return param;
                    });

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_VIEWED)
                        )
                    );

                    addParam(model, () -> {
                        ParameterValue param = userParam(otherParamId, "q", OPERATOR_UID);
                        param.setConfidence(123432);
                        param.setOwnerId(1234L);
                        return param;
                    });

                    addParam(model, () -> userParam(otherParamId2, "w", OPERATOR_UID));
                });
            return models;
        });
        mockCategoryResponse(paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamCount()).isEqualTo(1);

        Assert.assertEquals(paramId, response.getModelStatisticList().get(0).getContractorActions()
            .getParamList().get(0).getEntityId());

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(1);
        YangLogStorage.ParameterConflict parameterConflict =
            skuParameterConflictStatistic.getParameterConflictsList().get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataPreserveConflictsFromRequests() {
        long modelId = 1L;
        long paramId = 777L;
        long paramId2 = 888L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        addUserParam(model, paramId, "vla", 1923123L);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    addParam(model, () -> {
                        ParameterValue param = userParam(paramId, "bla", OPERATOR_UID);
                        param.setConfidence(123432);
                        param.setOwnerId(1234L);
                        return param;
                    });

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_VIEWED)
                        )
                    );
                });
            return models;
        });
        mockCategoryResponse(paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .setDecision(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED)
                .build())
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId2)
                .setDecision(YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamCount()).isEqualTo(1);

        Assert.assertEquals(paramId, response.getModelStatisticList().get(0).getContractorActions()
            .getParamList().get(0).getEntityId());

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(2);

        List<YangLogStorage.ParameterConflict> parameterConflicts = skuParameterConflictStatistic.getParameterConflictsList();
        parameterConflicts = parameterConflicts.stream()
            .sorted(Comparator.comparing(x -> x.getParameterId()))
            .collect(Collectors.toList());

        YangLogStorage.ParameterConflict parameterConflict = parameterConflicts.get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO, parameterConflict.getDecision());

        parameterConflict = parameterConflicts.get(1);
        Assert.assertEquals(paramId2, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataChangesParamHypothesis() {
        long modelId = 1L;
        long paramId = 777L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        addUserParam(model, paramId, "vla", 1923123L);
        ParameterValueHypothesis hypo1p = new ParameterValueHypothesis(paramId, "xsl" + paramId, Param.Type.STRING,
            Collections.singletonList(WordUtil.defaultWord("hypo1")), OPERATOR_UID);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    model.setModifiedUserId(OPERATOR_UID);
                    model.putParameterValueHypothesis(hypo1p);

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_FILLED)
                        )
                    );
                });
            return models;
        });
        mockCategoryResponse(paramId);

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamHypothesisCount()).isEqualTo(1);

        Assert.assertEquals(paramId, response.getModelStatisticList().get(0).getContractorActions()
            .getParamHypothesisList().get(0).getEntityId());

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(1);
        YangLogStorage.ParameterConflict parameterConflict =
            skuParameterConflictStatistic.getParameterConflictsList().get(0);
        Assert.assertEquals(paramId, parameterConflict.getParameterId());
        Assert.assertEquals(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED, parameterConflict.getDecision());
    }

    @Test
    public void testComputeParamMetadataChangesParamRemoveHypothesis() {
        long modelId = 1L;
        long paramId = 777L;
        long paramId2mutivalue = 712377L;
        CommonModel model = createModel(modelId, CommonModel.Source.GURU);
        ParameterValueHypothesis hypo1p = new ParameterValueHypothesis(paramId, "xsl" + paramId, Param.Type.STRING,
            Collections.singletonList(WordUtil.defaultWord("hypo1")), OPERATOR_UID);
        model.putParameterValueHypothesis(hypo1p);
        ParameterValueHypothesis hypo2p = new ParameterValueHypothesis(paramId2mutivalue, "xsl" + paramId2mutivalue,
            Param.Type.STRING, Collections.singletonList(WordUtil.defaultWord("hypo2")), OPERATOR_UID);
        model.putParameterValueHypothesis(hypo2p);

        applyChangesAndDoAudit(Arrays.asList(model), (models) -> {
            models.stream().filter(m -> m.getId() == modelId).forEach(
                mod -> {
                    model.setModifiedUserId(OPERATOR_UID);
                    model.removeParameterValueHypothesis(paramId);
                    model.removeParameterValueHypothesis(paramId2mutivalue);

                    mod.setParameterValuesMetadata(
                        ImmutableMap.of(
                            paramId, new ParameterValueMetadata(paramId, ModificationSource.OPERATOR_FILLED),
                            paramId2mutivalue, new ParameterValueMetadata(paramId2mutivalue,
                                ModificationSource.OPERATOR_FILLED)
                        )
                    );
                });
            return models;
        });
        mockCategoryResponse(Map.of(paramId, false, paramId2mutivalue, true));

        YangLogStorage.YangLogStoreRequest.Builder request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);

        request.addSkuParameterConflictStatistic(YangLogStorage.SkuParameterConflictStatistic.newBuilder()
            .setModelId(modelId)
            .setUid(OPERATOR_UID)
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId)
                .build())
            .addParameterConflicts(YangLogStorage.ParameterConflict.newBuilder()
                .setParameterId(paramId2mutivalue)
                .build())
            .build());
        YangLogStorage.YangLogStoreRequest response = service.computeModelStatsFromAudit(request.build());

        assertThat(response.getModelStatisticCount()).isEqualTo(1);
        assertThat(response.getModelStatisticList().get(0).getModelId()).isEqualTo(modelId);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getParamHypothesisCount()).isEqualTo(0);

        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getRemovedParamHypothesisCount()).isEqualTo(2);
        assertThat(response.getModelStatisticList().get(0).getContractorActions()
            .getRemovedParamHypothesisList()).extracting(YangLogStorage.ActionInfo::getEntityId)
            .containsExactlyInAnyOrder(paramId, paramId2mutivalue);

        assertThat(response.getSkuParameterConflictStatisticCount()).isEqualTo(1);
        YangLogStorage.SkuParameterConflictStatistic skuParameterConflictStatistic =
            response.getSkuParameterConflictStatisticList().get(0);

        Assert.assertEquals(modelId, skuParameterConflictStatistic.getModelId());
        assertThat(skuParameterConflictStatistic.getParameterConflictsCount()).isEqualTo(2);
        assertThat(skuParameterConflictStatistic.getParameterConflictsList())
            .extracting(YangLogStorage.ParameterConflict::getParameterId)
            .containsExactlyInAnyOrder(paramId, paramId2mutivalue);
        assertThat(skuParameterConflictStatistic.getParameterConflictsList())
            .extracting(YangLogStorage.ParameterConflict::getDecision)
            .containsExactlyInAnyOrder(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED,
                YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED);
    }

    private List<ParameterValue> pictureParams(String url, long uid) {
        List<ParameterValue> parameterValues = Arrays.asList(
            ParameterValueBuilder.newBuilder()
                .xslName("XL-Picture")
                .words(url)
                .build(),
            ParameterValueBuilder.newBuilder()
                .xslName("XLPictureSizeX")
                .num(100)
                .build(),
            ParameterValueBuilder.newBuilder()
                .xslName("XLPictureSizeY")
                .num(100)
                .build()
        );
        parameterValues.forEach(p -> {
            p.setParamId(IMAGE_PARAMS.get(p.getXslName()));
            p.setModificationSource(ModificationSource.OPERATOR_FILLED);
            p.setLastModificationUid(uid);
            p.setLastModificationDate(new Date());
        });
        return parameterValues;
    }

    private void applyChangesAndDoAudit(Collection<CommonModel> models,
                                        UnaryOperator<Collection<CommonModel>> changes) {
        Map<Long, CommonModel> before = models.stream().collect(
            Collectors.toMap(CommonModel::getId, CommonModel::new));
        Collection<CommonModel> after = changes.apply(models);
        modelGeneralizationService.generalizeGroups(
            modelGeneralizationService.createGroups(after, before, GeneralizationStrategy.AUTO),
            false,
            false,
            new OperationStats());

        modelAuditService.auditModels(after, before, modelAuditContext);
        modelStorageService.setModelsMap(after.stream().collect(Collectors.toMap(CommonModel::getId, m -> m)));
    }

    private void addParam(CommonModel model, Supplier<ParameterValue> pv) {
        ParameterValue parameterValue = pv.get();
        model.addParameterValue(parameterValue);
        model.setModifiedUserId(parameterValue.getLastModificationUid());
    }

    private void addUserParam(CommonModel model, Long paramId, String value, Long uid) {
        addParam(model, () -> userParam(paramId, value, uid));
    }

    private ParameterValue userParam(Long paramId, String value, Long uid) {
        ParameterValue parameterValue = new ParameterValue(paramId, "xsl" + paramId, Param.Type.STRING);
        parameterValue.setStringValue(WordUtil.defaultWords(value));
        parameterValue.setLastModificationInfo(ModificationSource.OPERATOR_FILLED, uid, new Date());
        return parameterValue;
    }

    private SquashedUserActions.ModelActions modelActions(Long id) {
        return new SquashedUserActions.ModelActions(CommonModelBuilder.newBuilder(id, CATEGORY_ID).getModel());
    }

    private YangLogStorage.YangLogStoreRequest defaultRequest(boolean withInspection) {
        YangLogStorage.YangLogStoreRequest.Builder builder = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(OPERATOR_UID)
                .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                .build())
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setHitmanId(HITMAN_ID)
            .setCategoryId(CATEGORY_ID);
        if (withInspection) {
            builder.setInspectorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(MANAGER_UID)
                    .setTaskSuiteCreatedDate(DEFAULT_TASK_SUITE_CREATED_DATE)
                    .build()
            );
        }
        return builder.build();
    }

    private void mockCategoryResponse(Long... userParamIds) {
        mockCategoryResponse(false, userParamIds);
    }

    private void mockCategoryResponse(boolean multivalue, Long... userParamIds) {
        mockCategoryResponse(Arrays.stream(userParamIds).collect(Collectors.toMap(id -> id, id -> multivalue)));
    }

    private void mockCategoryResponse(Map<Long, Boolean> paramAndMultivalueMap) {
        MboParameters.Category.Builder catBuild = MboParameters.Category.newBuilder()
            .setHid(CATEGORY_ID);
        paramAndMultivalueMap.forEach((paramId, multivalue) ->
            catBuild.addParameter(createParameter("xsl" + paramId, multivalue, paramId)));
        Mockito.when(extractorService.getParameters(Mockito.eq(requestForCategory(CATEGORY_ID))))
            .thenReturn(responseForCategory(catBuild.build()));
    }


    private AuditParamAction buildAction(MboAudit.ActionType type, String oldValue, String newValue, long paramId,
                                         String xslName, long seconds) {
        return AuditParamAction.newBuilder()
            .setActionType(type)
            .setOldValue(oldValue)
            .setNewValue(newValue)
            .setParamId(paramId)
            .setXslName(xslName)
            .setTimestamp(actionsStartTime + seconds * 1000)
            .build();
    }

    private PropertyValueAction buildPropAction(MboAudit.ActionType type,
                                                String oldValue,
                                                String newValue,
                                                long seconds) {
        return PropertyValueAction.newBuilder()
            .setOldValue(oldValue)
            .setNewValue(newValue)
            .setTimestamp(actionsStartTime + seconds * 1000)
            .setActionType(type)
            .build();
    }

    private CategoryParamActions buildParamAction(long paramId,
                                                  String paramName,
                                                  List<PropertyValueAction> aliases,
                                                  List<PropertyValueAction> cutOffs) {
        return CategoryParamActions.newBuilder()
            .setParamId(paramId)
            .setParamName(paramName)
            .addAllAliases(aliases)
            .addAllCutOffWords(cutOffs)
            .build();
    }

    private static class MapBuilder<K> {
        private Map<K, List<AuditAction>> map = new HashMap<>();
        private final Function<AuditAction, K> keyExtractor;

        MapBuilder(Function<AuditAction, K> keyExtractor) {
            this.keyExtractor = keyExtractor;
        }

        static MapBuilder<GroupingSquashStrategy.PropertyValue> propertyValueMapBuilder() {
            return new MapBuilder<>(GroupingSquashStrategy.PropertyValue::new);
        }

        static MapBuilder<GroupingSquashStrategy.ParameterProperty> parameterPropertyMapBuilder() {
            return new MapBuilder<>(GroupingSquashStrategy.ParameterProperty::new);
        }

        static MapBuilder<GroupingSquashStrategy.MultiParamValue> multiParamValueMapBuilder() {
            return new MapBuilder<>(GroupingSquashStrategy.MultiParamValue::new);
        }

        static MapBuilder<GroupingSquashStrategy.SingleParamValue> singleParamValueMapBuilder() {
            return new MapBuilder<>(GroupingSquashStrategy.SingleParamValue::new);
        }

        static MapBuilder<String> propertyNameMapBuilder() {
            return new MapBuilder<>(AuditAction::getPropertyName);
        }

        public Map<K, List<AuditAction>> single(AuditAction value) {
            add(value);
            return build();
        }

        public MapBuilder<K> add(AuditAction value) {
            map.put(keyExtractor.apply(value), Collections.singletonList(value));
            return this;
        }

        public Map<K, List<AuditAction>> build() {
            return map;
        }
    }

    private CommonModel createModel(long id, CommonModel.Source currentType) {
        return CommonModelBuilder.newBuilder()
            .id(id)
            .category(CATEGORY_ID)
            .modifiedUserId(0)
            .currentType(currentType)
            .startParameterValue()
            .paramId(42L)
            .xslName(XslNames.NAME)
            .type(Param.Type.STRING)
            .words(new Word(Language.RUSSIAN.getId(), "title"))
            .endParameterValue()
            .startParameterValue()
            .paramId(43L)
            .xslName(XslNames.BAR_CODE)
            .words("bar_code")
            .endParameterValue()
            .startParameterValue()
            .paramId(44L)
            .xslName(XslNames.VENDOR_CODE)
            .words("vendor_code")
            .endParameterValue()
            .endModel();
    }

    private YangLogStorage.ActionInfo createActionInfo() {
        return YangLogStorage.ActionInfo.newBuilder().setAuditActionId(1L).build();
    }

    private MboParameters.GetCategoryParametersRequest requestForCategory(long categoryId) {
        return MboParameters.GetCategoryParametersRequest.newBuilder()
            .setCategoryId(categoryId)
            .setTimestamp(-1)
            .build();
    }

    private MboParameters.GetCategoryParametersResponse responseForCategory(MboParameters.Category category) {
        return MboParameters.GetCategoryParametersResponse.newBuilder()
            .setCategoryParameters(category)
            .build();
    }

    private void refreshToNow() {
        actionsStartTime = refreshTimeToNow() * 1000;
    }
}
