package ru.yandex.market.mbo.reactui.service.audit;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.category.mappings.CategoryMappingService;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.billing.dao.FullPaidEntry;
import ru.yandex.market.mbo.db.billing.dao.PaidEntryDao;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.DecimalText;
import ru.yandex.market.mbo.reactui.service.audit.tree.ModelNodeContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.ParameterNodeContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.TaskNodeContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.TreeDepthFirstVisitor;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.OfferNodeContext;
import ru.yandex.market.mbo.statistic.ModelAuditStatisticsService;
import ru.yandex.market.mbo.statistic.model.RawStatistics;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

/**
 * @author dergachevfv
 * @since 11/27/19
 */
@SuppressWarnings("checkstyle:magicNumber")
public class AuditTreeBuilderServiceTest {

    private static final long CATEGORY_ID = 100L;
    private static final long MODEL_ID = 1L;
    private static final long OFFER_ID = 100L;
    private static final String OFFER_ID_STRING = "100";
    private static final long USER_ID_1 = 101L;
    private static final long USER_ID_2 = 102L;
    private static final double CONTRACTOR_ERROR = 2d;
    private static final double INSPECTOR_PRICE = 1d;

    private ModelAuditStatisticsService modelAuditStatisticsService;
    private ModelStorageService modelStorageServiceCardApi;
    private PaidEntryDao paidEntryDao;
    private YangBillingService yangBillingService;
    private AuditTreeBuilderService auditTreeBuilderService;
    private BillingPricesRegistry pricesRegistry;
    private YangLogStorage.YangLogStoreRequest yangLogStoreRequest;
    private IParameterLoaderService parameterLoaderService;
    private CategoryModelsService categoryModelsService;
    private CategoryMappingService categoryMappingService;
    private OfferStorageService offerStorageService;
    private MboCategoryService mboCategoryService;
    private AccessControlService accessControlService;
    private GuruVendorsReader guruVendorsReader;

    @Before
    public void init() {
        modelAuditStatisticsService = Mockito.mock(ModelAuditStatisticsService.class);
        modelStorageServiceCardApi = Mockito.mock(ModelStorageService.class);
        paidEntryDao = Mockito.mock(PaidEntryDao.class);
        yangBillingService = Mockito.mock(YangBillingService.class);
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        categoryModelsService = Mockito.mock(CategoryModelsService.class);
        categoryMappingService = Mockito.mock(CategoryMappingService.class);
        offerStorageService = Mockito.mock(OfferStorageService.class);
        mboCategoryService = Mockito.mock(MboCategoryService.class);
        accessControlService = Mockito.mock(AccessControlService.class);
        guruVendorsReader = Mockito.mock(GuruVendorsReader.class);

        auditTreeBuilderService = new AuditTreeBuilderService(modelAuditStatisticsService, parameterLoaderService,
            categoryModelsService, categoryMappingService, modelStorageServiceCardApi, paidEntryDao, yangBillingService,
            offerStorageService, mboCategoryService, accessControlService, guruVendorsReader);
        pricesRegistry = new BillingPricesRegistry(Collections.emptyList(), Collections.emptyList());

        yangLogStoreRequest = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setContractorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(USER_ID_1)
                    .build())
            .setInspectorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(USER_ID_2)
                    .build())
            .build();
    }

    @Test
    public void testErrorPrices() {
        Mockito.when(accessControlService.canAccessUserData(anyLong()))
            .thenReturn(true);
        Mockito.when(categoryModelsService.getModels(any()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());
        Mockito.when(mboCategoryService.getContentCommentTypes(any()))
            .thenReturn(MboCategory.ContentCommentTypes.Response.newBuilder().build());
        Mockito.when(mboCategoryService.searchMappingsByInternalOfferId(any()))
            .thenReturn(MboCategory.SearchMappingsByInternalOfferIdResponse.newBuilder()
                .addOffer(SupplierOffer.Offer.newBuilder()
                    .setInternalOfferId(OFFER_ID)
                    .setTitle("offer1"))
                .build());
        Mockito.when(modelStorageServiceCardApi.findModels(any()))
            .thenReturn(ModelStorage.GetModelsResponse.newBuilder().build());
        Mockito.when(parameterLoaderService.loadRusParamNames())
            .thenReturn(Map.of(1L, "param1"));

        YangLogStorage.YangLogStoreRequest origStatistics = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setContractorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(USER_ID_1))
            .setInspectorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(USER_ID_2))
            .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                .setModelId(1L)
                .setInspectorActions(YangLogStorage.ModelActions.newBuilder()
                    .addParam(YangLogStorage.ActionInfo.newBuilder()
                        .setAuditActionId(111L)
                        .setEntityId(1L))))
            .addAllMappingStatistic(List.of(
                YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER_ID_1)
                    .setOfferId(OFFER_ID)
                    .setMarketSkuId(1000L)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED)
                    .build(),
                YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER_ID_2)
                    .setOfferId(OFFER_ID)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH)
                    .build()))
            .build();

        YangLogStorage.YangLogStoreRequest operatorStatistics = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setContractorInfo(
                YangLogStorage.OperatorInfo.newBuilder()
                    .setUid(USER_ID_1)
                    .build())
            .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                .setModelId(1L)
                .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                    .addParam(YangLogStorage.ActionInfo.newBuilder()
                        .setAuditActionId(1L)
                        .setEntityId(1L)
                        .build())))
            .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                .setUid(USER_ID_1)
                .setOfferId(OFFER_ID)
                .setMarketSkuId(1L)
                .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED))
            .build();
        Mockito.when(modelAuditStatisticsService.computeModelStatsTask(any()))
            .thenReturn(operatorStatistics);

        Mockito.when(paidEntryDao.getYangPaidEntries(anyLong(), anyLong(), eq(USER_ID_1)))
            .thenReturn(Collections.emptyList());
        Mockito.when(paidEntryDao.getYangPaidEntries(anyLong(), anyLong(), eq(USER_ID_2)))
            .thenReturn(List.of(
                FullPaidEntry.newBuilder()
                    .uid(USER_ID_2)
                    .price(INSPECTOR_PRICE)
                    .auditActionId(1L)
                    .operationId((long) PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION.getId())
                    .build(),
                FullPaidEntry.newBuilder()
                    .uid(USER_ID_2)
                    .price(INSPECTOR_PRICE)
                    .linkData(OFFER_ID_STRING)
                    .operationId((long) PaidAction.YANG_SKU_MAPPING_CORRECTION.getId())
                    .build()
            ));

        Mockito.when(yangBillingService.calculateBilling(any()))
            .thenReturn(List.of(
                FullPaidEntry.newBuilder()
                    .uid(USER_ID_1)
                    .price(CONTRACTOR_ERROR)
                    .auditActionId(1L)
                    .operationId((long) PaidAction.YANG_ADD_PARAM_VALUE.getId())
                    .build(),
                FullPaidEntry.newBuilder()
                    .uid(USER_ID_1)
                    .price(CONTRACTOR_ERROR)
                    .linkData(OFFER_ID_STRING)
                    .operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build()
            ));

        Mockito.when(modelAuditStatisticsService.getYangBilledActionsByUID(any()))
            .thenReturn(Map.of(
                USER_ID_1, new SquashedUserActions()
                    .addModelActions(new SquashedUserActions.ModelActions(
                            CommonModelBuilder.newBuilder(1L, CATEGORY_ID).getModel())
                        .setChangedSingleValueParams(Map.of(
                            new GroupingSquashStrategy.SingleParamValue(1L, "param1"),
                            List.of(new AuditAction()
                                .setParameterId(1L)
                                .setPropertyName("param1")
                                .setActionId(1L))))),
                USER_ID_2, new SquashedUserActions()
                    .addModelActions(new SquashedUserActions.ModelActions(
                            CommonModelBuilder.newBuilder(1L, CATEGORY_ID).getModel())
                        .setChangedSingleValueParams(Map.of(
                            new GroupingSquashStrategy.SingleParamValue(1L, "param1"),
                            List.of(new AuditAction()
                                .setParameterId(1L)
                                .setPropertyName("param1")
                                .setActionId(1L)))))
            ));

        AuditNode.Builder auditTreeBuilder =
            auditTreeBuilderService.getAuditTreeBuilder(new RawStatistics(new Date(), origStatistics));

        Assertions.assertThat(auditTreeBuilder).isNotNull();

        BigDecimal inspectorPrice = convertPrice(INSPECTOR_PRICE);
        BigDecimal contractorError = convertPrice(CONTRACTOR_ERROR);

        TreeDepthFirstVisitor<AuditNode.Builder> pricesChecker = node -> {
            if (!node.hasChildren()) {
                // leaf node, check prices!
                Map<String, List<BaseComponent>> data = node.getData();

                Assertions.assertThat(data.get(ColumnDefinition.OPERATOR_ERROR.name()))
                    .usingFieldByFieldElementComparator()
                    .containsExactly(new DecimalText(contractorError, FormatUtils.formatPrice(contractorError)));

                Assertions.assertThat(data.get(ColumnDefinition.INSPECTOR_PRICE.name()))
                    .usingFieldByFieldElementComparator()
                    .containsExactly(new DecimalText(inspectorPrice, FormatUtils.formatPrice(inspectorPrice)));
            }
        };
        pricesChecker.visit(auditTreeBuilder);
    }

    private BigDecimal convertPrice(double price) {
        return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(30));
    }

    @Test
    public void skuContextsAreGroupsUnderModelContextsWithMissedModifications() {
        Model sku11 = Model.newBuilder()
            .setId(11L)
            .setCurrentType("SKU")
            .addRelations(ModelStorage.Relation.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(101L)
                .build())
            .build();

        Model sku12 = Model.newBuilder()
            .setId(12L)
            .setCurrentType("SKU")
            .build();
        Model sku21 = Model.newBuilder()
            .setId(21L)
            .setCurrentType("SKU")
            .build();
        Model sku22 = Model.newBuilder()
            .setId(22L)
            .setCurrentType("SKU")
            .build();

        Model model1 = Model.newBuilder()
            .setId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setCategoryId(CATEGORY_ID)
                .setId(sku12.getId())
                .build())
            .setCurrentType("GURU")
            .build();

        Model modification101 = Model.newBuilder()
            .setId(101L)
            .setCurrentType("GURU")
            .setParentId(model1.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setCategoryId(CATEGORY_ID)
                .setId(sku11.getId())
                .build())
            .build();

        Model model2 = Model.newBuilder()
            .setId(2L)
            .setCurrentType("GURU")
            .addAllRelations(Arrays.asList(
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku21.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build(),
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku22.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build()
            ))
            .build();

        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addAllModelIds(Collections.singletonList(101L))
            .build();

        ModelStorage.GetModelsResponse missedModifications = ModelStorage.GetModelsResponse.newBuilder()
            .addAllModels(Collections.singletonList(modification101))
            .build();

        Mockito.when(modelStorageServiceCardApi.findModels(eq(request)))
            .thenReturn(missedModifications);

        SquashedUserActions operatorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            sku11.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku11)).setCreated(true),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)).setCreated(true)
        ));

        SquashedUserActions inspectorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)),
            model2.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model2)),
            sku21.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku21)).setCreated(true),
            sku22.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku22)).setCreated(true)
        ));

        Map<Long, Model> modelsById = Map.of(
            model1.getId(), model1,
            model2.getId(), model2,
            sku11.getId(), sku11,
            sku12.getId(), sku12,
            sku21.getId(), sku21,
            sku22.getId(), sku22
        );

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(yangLogStoreRequest, modelsById, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, null);

        TaskNodeContext taskNodeContext = new TaskNodeContext(operatorActions, inspectorActions,
            null, null, null, null);
        List<ModelNodeContext> modelNodeContexts = auditTreeHelper.toModelNodeContexts(taskNodeContext);

        // 2 models
        // 1 missed modification
        // 4 skus
        // expected result:
        //   model1
        //     sku12
        //     sku11
        //   model2
        //     sku21
        //     sku22
        Assertions.assertThat(modelNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ModelNodeContext(model1.getId(), model1, false,
                    Arrays.asList(
                        new ModelNodeContext(sku11.getId(), sku11, true, null,
                            operatorActions.getModelActionsById(sku11.getId()),
                            inspectorActions.getModelActionsById(sku11.getId())),
                        new ModelNodeContext(sku12.getId(), sku12, true, null,
                            operatorActions.getModelActionsById(sku12.getId()),
                            inspectorActions.getModelActionsById(sku12.getId()))
                    ),
                    operatorActions.getModelActionsById(model1.getId()),
                    inspectorActions.getModelActionsById(model1.getId())),
                new ModelNodeContext(model2.getId(), model2, false,
                    Arrays.asList(
                        new ModelNodeContext(sku21.getId(), sku21, true, null,
                            operatorActions.getModelActionsById(sku21.getId()),
                            inspectorActions.getModelActionsById(sku21.getId())),
                        new ModelNodeContext(sku22.getId(), sku22, true, null,
                            operatorActions.getModelActionsById(sku22.getId()),
                            inspectorActions.getModelActionsById(sku22.getId()))
                    ),
                    operatorActions.getModelActionsById(model2.getId()),
                    inspectorActions.getModelActionsById(model2.getId()))
            );
    }

    @Test
    public void skuContextWithoutActions() {
        Model sku11 = Model.newBuilder()
            .setId(11L)
            .setCurrentType("SKU")
            .addRelations(ModelStorage.Relation.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(101L)
                .build())
            .build();

        Model sku12 = Model.newBuilder()
            .setId(12L)
            .setCurrentType("SKU")
            .build();
        Model sku21 = Model.newBuilder()
            .setId(21L)
            .setCurrentType("SKU")
            .build();
        Model sku22 = Model.newBuilder()
            .setId(22L)
            .setCurrentType("SKU")
            .build();

        Model model1 = Model.newBuilder()
            .setId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setCategoryId(CATEGORY_ID)
                .setId(sku12.getId())
                .build())
            .setCurrentType("GURU")
            .build();

        Model modification101 = Model.newBuilder()
            .setId(101L)
            .setCurrentType("GURU")
            .setParentId(model1.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setCategoryId(CATEGORY_ID)
                .setId(sku11.getId())
                .build())
            .build();

        Model model2 = Model.newBuilder()
            .setId(2L)
            .setCurrentType("GURU")
            .addAllRelations(Arrays.asList(
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku21.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build(),
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku22.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build()
            ))
            .build();

        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addAllModelIds(Collections.singletonList(101L))
            .build();

        ModelStorage.GetModelsResponse missedModifications = ModelStorage.GetModelsResponse.newBuilder()
            .addAllModels(Collections.singletonList(modification101))
            .build();

        Mockito.when(modelStorageServiceCardApi.findModels(eq(request)))
            .thenReturn(missedModifications);

        SquashedUserActions operatorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)).setCreated(true)
        ));

        SquashedUserActions inspectorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)),
            model2.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model2)),
            sku21.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku21)).setCreated(true),
            sku22.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku22)).setCreated(true)
        ));

        Map<Long, Model> modelsById = Map.of(
            model1.getId(), model1,
            model2.getId(), model2,
            sku11.getId(), sku11,
            sku12.getId(), sku12,
            sku21.getId(), sku21,
            sku22.getId(), sku22
        );

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(yangLogStoreRequest, modelsById, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, null);

        TaskNodeContext taskNodeContext = new TaskNodeContext(operatorActions, inspectorActions,
            null, null, null, null);
        List<ModelNodeContext> modelNodeContexts = auditTreeHelper.toModelNodeContexts(taskNodeContext);

        // 2 models
        // 1 missed modification
        // 3 skus
        // expected result:
        //   model1
        //     sku12
        //   model2
        //     sku21
        //     sku22
        Assertions.assertThat(modelNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ModelNodeContext(model1.getId(), model1, false,
                    Collections.singletonList(
                        new ModelNodeContext(sku12.getId(), sku12, true, null,
                            operatorActions.getModelActionsById(sku12.getId()),
                            inspectorActions.getModelActionsById(sku12.getId()))
                    ),
                    operatorActions.getModelActionsById(model1.getId()),
                    inspectorActions.getModelActionsById(model1.getId())),
                new ModelNodeContext(model2.getId(), model2, false,
                    Arrays.asList(
                        new ModelNodeContext(sku21.getId(), sku21, true, null,
                            operatorActions.getModelActionsById(sku21.getId()),
                            inspectorActions.getModelActionsById(sku21.getId())),
                        new ModelNodeContext(sku22.getId(), sku22, true, null,
                            operatorActions.getModelActionsById(sku22.getId()),
                            inspectorActions.getModelActionsById(sku22.getId()))
                    ),
                    operatorActions.getModelActionsById(model2.getId()),
                    inspectorActions.getModelActionsById(model2.getId()))
            );
    }

    @Test
    public void skuContextsAreGroupedUnderModelContexts() {
        Model sku11 = Model.newBuilder()
            .setId(11L)
            .setCurrentType("SKU")
            .build();
        Model sku12 = Model.newBuilder()
            .setId(12L)
            .setCurrentType("SKU")
            .build();
        Model sku21 = Model.newBuilder()
            .setId(21L)
            .setCurrentType("SKU")
            .build();
        Model sku22 = Model.newBuilder()
            .setId(22L)
            .setCurrentType("SKU")
            .build();

        Model model1 = Model.newBuilder()
            .setId(1L)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setCategoryId(CATEGORY_ID)
                .setId(sku12.getId())
                .build())
            .setCurrentType("GURU")
            .build();

        Model modification101 = Model.newBuilder()
            .setId(101L)
            .setCurrentType("GURU")
            .setParentId(model1.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(sku11.getId())
                .setCategoryId(CATEGORY_ID)
                .build())
            .build();

        Model model2 = Model.newBuilder()
            .setId(2L)
            .setCurrentType("GURU")
            .addAllRelations(Arrays.asList(
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku21.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build(),
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setId(sku22.getId())
                    .setCategoryId(CATEGORY_ID)
                    .build()
            ))
            .build();

        SquashedUserActions operatorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            modification101.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(modification101)),
            sku11.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku11)).setCreated(true),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)).setCreated(true)
        ));

        SquashedUserActions inspectorActions = new SquashedUserActions().setAllModelActions(Map.of(
            model1.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model1)),
            sku12.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku12)),
            model2.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(model2)),
            sku21.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku21)).setCreated(true),
            sku22.getId(), new SquashedUserActions.ModelActions(ModelProtoConverter.convert(sku22)).setCreated(true)
        ));

        Map<Long, Model> modelsById = Map.of(
            model1.getId(), model1,
            modification101.getId(), modification101,
            model2.getId(), model2,
            sku11.getId(), sku11,
            sku12.getId(), sku12,
            sku21.getId(), sku21,
            sku22.getId(), sku22
        );

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(yangLogStoreRequest, modelsById, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, null);

        TaskNodeContext taskNodeContext = new TaskNodeContext(operatorActions, inspectorActions,
            null, null, null, null);
        List<ModelNodeContext> modelNodeContexts = auditTreeHelper.toModelNodeContexts(taskNodeContext);

        // 3 models
        // 4 skus
        // expected result:
        //   model1
        //     sku12
        //   modification101
        //     sku11
        //   model2
        //     sku21
        //     sku22
        Assertions.assertThat(modelNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ModelNodeContext(model1.getId(), model1, false,
                    Collections.singletonList(
                        new ModelNodeContext(sku12.getId(), sku12, true, null,
                            operatorActions.getModelActionsById(sku12.getId()),
                            inspectorActions.getModelActionsById(sku12.getId()))
                    ),
                    operatorActions.getModelActionsById(model1.getId()),
                    inspectorActions.getModelActionsById(model1.getId())),
                new ModelNodeContext(modification101.getId(), modification101, false,
                    Collections.singletonList(
                        new ModelNodeContext(sku11.getId(), sku11, true, null,
                            operatorActions.getModelActionsById(sku11.getId()),
                            inspectorActions.getModelActionsById(sku11.getId()))
                    ),
                    operatorActions.getModelActionsById(modification101.getId()),
                    inspectorActions.getModelActionsById(modification101.getId())),
                new ModelNodeContext(model2.getId(), model2, false,
                    Arrays.asList(
                        new ModelNodeContext(sku21.getId(), sku21, true, null,
                            operatorActions.getModelActionsById(sku21.getId()),
                            inspectorActions.getModelActionsById(sku21.getId())),
                        new ModelNodeContext(sku22.getId(), sku22, true, null,
                            operatorActions.getModelActionsById(sku22.getId()),
                            inspectorActions.getModelActionsById(sku22.getId()))
                    ),
                    operatorActions.getModelActionsById(model2.getId()),
                    inspectorActions.getModelActionsById(model2.getId()))
            );
    }

    @Test
    public void whiteOffersNodes() {
        OffersStorage.GenerationDataOffer offer1 = OffersStorage.GenerationDataOffer.newBuilder()
            .setClassifierMagicId("offer1")
            .setOffer("offer1")
            .build();

        OffersStorage.GenerationDataOffer offer2 = OffersStorage.GenerationDataOffer.newBuilder()
            .setClassifierMagicId("offer2")
            .setOffer("offer2")
            .build();

        YangLogStorage.YangLogStoreRequest request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                .setOfferId(offer1.getClassifierMagicId())
                .build())
            .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                .setOfferId(offer2.getClassifierMagicId())
                .build())
            .build();

        Mockito.when(offerStorageService.getOffersByIds(any())).thenReturn(OffersStorage.GetOffersResponse.newBuilder()
            .addAllOffers(Arrays.asList(offer1, offer2))
            .build());

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(request, null, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, null);

        TaskNodeContext taskNodeContext = new TaskNodeContext(null, null,
            new HashMap<>(), new HashMap<>(), null, null);
        List<OfferNodeContext<String, YangLogStorage.MatchingStatistic>> offerNodeContexts =
            auditTreeHelper.toWhiteOfferNodeContexts(taskNodeContext);

        //2 offers
        Assertions.assertThat(offerNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new OfferNodeContext<>(null, null, offer1.getClassifierMagicId(), offer1.getOffer(),
                            null, null, pricesRegistry, null),
                new OfferNodeContext<>(null, null, offer2.getClassifierMagicId(), offer2.getOffer(),
                            null, null, pricesRegistry, null)
            );
    }

    @Test
    public void blueOffersTest() {
        SupplierOffer.Offer offer1 = SupplierOffer.Offer.newBuilder()
            .setInternalOfferId(1L)
            .setTitle("offer1")
            .build();

        SupplierOffer.Offer offer2 = SupplierOffer.Offer.newBuilder()
            .setInternalOfferId(2L)
            .setTitle("offer2")
            .build();

        YangLogStorage.YangLogStoreRequest request = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                .setOfferId(offer1.getInternalOfferId())
                .build())
            .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                .setOfferId(offer2.getInternalOfferId())
                .build())
            .build();

        Mockito.when(mboCategoryService.searchMappingsByInternalOfferId(any())).thenReturn(
            MboCategory.SearchMappingsByInternalOfferIdResponse.newBuilder()
                .addAllOffer(Arrays.asList(offer1, offer2))
                .build());

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(request, null, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, new HashMap<>());

        TaskNodeContext taskNodeContext = new TaskNodeContext(null, null,
            null, null, new HashMap<>(), new HashMap<>());
        List<OfferNodeContext<Long, YangLogStorage.MappingStatistic>> offerNodeContexts =
            auditTreeHelper.toBlueOfferNodeContexts(taskNodeContext);

        //2 offers
        Assertions.assertThat(offerNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new OfferNodeContext<>(null, null, offer1.getInternalOfferId(), offer1.getTitle(),
                            null, null, pricesRegistry, null),
                new OfferNodeContext<>(null, null, offer2.getInternalOfferId(), offer2.getTitle(),
                            null, null, pricesRegistry, null)
            );
    }

    @Test
    public void toParameterNodeContexts() {
        AuditAction operatorSingleAction1 =
            modelSingleParamAction(1L, "param1", "old", "new");
        AuditAction operatorSingleAction2 =
            modelSingleParamAction(2L, "param2", "old", "new");
        AuditAction operatorMultiAction1 =
            modelParamAction(11L, "param1m", "old", "new", AuditAction.ActionType.CREATE);
        AuditAction operatorMultiAction2 =
            modelParamAction(12L, "param2m", "old", "new", AuditAction.ActionType.CREATE);

        SquashedUserActions.ModelActions operatorActions = new SquashedUserActions.ModelActions(new CommonModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new,
                    operatorSingleAction1,
                    operatorSingleAction2))
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new,
                    operatorMultiAction1,
                    operatorMultiAction2));

        AuditAction inspectorSingleAction1 =
            modelSingleParamAction(1L, "param1", "old", "new");
        AuditAction inspectorSingleAction2 =
            modelSingleParamAction(3L, "param3", "old", "new");
        AuditAction inspectorMultiAction1 =
            modelParamAction(11L, "param1m", "old", "new", AuditAction.ActionType.UPDATE);
        AuditAction inspectorMultiAction2 =
            modelParamAction(13L, "param3m", "old", "new", AuditAction.ActionType.UPDATE);

        Mockito.when(parameterLoaderService.loadRusParamNames()).thenReturn(Map.of(
            1L, "param1",
            3L, "param3",
            11L, "param1m",
            13L, "param3m",
            2L, "param2",
            12L, "param2m"
        ));

        SquashedUserActions.ModelActions inspectorActions = new SquashedUserActions.ModelActions(new CommonModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new,
                    inspectorSingleAction1,
                    inspectorSingleAction2))
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new,
                    inspectorMultiAction1,
                    inspectorMultiAction2));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, operatorActions, inspectorActions);

        AuditTreeBuilderService.AuditTreeHelper auditTreeHelper =
            auditTreeBuilderService.new AuditTreeHelper(yangLogStoreRequest, null, pricesRegistry,
                parameterLoaderService, CATEGORY_ID, null);

        List<ParameterNodeContext> parameterNodeContexts = auditTreeHelper.toParameterNodeContexts(context);

        Assertions.assertThat(parameterNodeContexts).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                createParameterNodeContext(
                    createSingleParamValue(operatorSingleAction1),
                    operatorSingleAction1,
                    inspectorSingleAction1),
                createParameterNodeContext(
                    createSingleParamValue(operatorSingleAction2),
                    operatorSingleAction2,
                    null),
                createParameterNodeContext(
                    createMultiParamValue(operatorMultiAction1),
                    operatorMultiAction1,
                    inspectorMultiAction1),
                createParameterNodeContext(
                    createMultiParamValue(operatorMultiAction2),
                    operatorMultiAction2,
                    null),
                createParameterNodeContext(
                    createSingleParamValue(inspectorSingleAction2),
                    null,
                    inspectorSingleAction2),
                createParameterNodeContext(
                    createMultiParamValue(inspectorMultiAction2),
                    null,
                    inspectorMultiAction2)
            );
    }

    private ParameterNodeContext createParameterNodeContext(
        ParameterNodeContext.ParamValue paramValue,
        AuditAction operatorAction,
        AuditAction inspectorAction
    ) {
        return new ParameterNodeContext(
            paramValue, CATEGORY_ID,
            USER_ID_1, USER_ID_2, pricesRegistry,
            operatorAction != null ? Collections.singletonList(operatorAction) : null,
            inspectorAction != null ? Collections.singletonList(inspectorAction) : null);
    }

    private ParameterNodeContext.ParamValue createSingleParamValue(AuditAction action) {
        return new ParameterNodeContext.ParamValue(new GroupingSquashStrategy.SingleParamValue(action));
    }

    private ParameterNodeContext.ParamValue createMultiParamValue(AuditAction action) {
        return new ParameterNodeContext.ParamValue(new GroupingSquashStrategy.MultiParamValue(action));
    }

    private <K> Map<K, List<AuditAction>> createMap(Function<AuditAction, K> keyExtractor,
                                                    AuditAction... actions) {
        return Arrays.stream(actions)
            .collect(Collectors.toMap(keyExtractor, Collections::singletonList));
    }
}
