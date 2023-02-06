package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentMarketParameterValue;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.exceptions.NotLeafCategoryError;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.utils.CollectionItemMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.addStrParam;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.setMarketSkuId;

public class RemoveCskuRedundantParametersTaskActionTest extends DBDcpStateGenerator {

    public static final Long SKU_ID = 1L;
    public static final Long PARENT_MODEL_ID = 2L;
    public static final Long OPERATOR_FILLED_PARAM_ID = 100500L;
    public static final String OPERATOR_FILLED_PARAM_NAME = "operator_param_name";
    public static final Long PARAM_ID = 100501L;
    public static final String PARAM_NAME = "param_name";
    public static final Long MODEL_BLOCKED_PARAM_ID = 100502L;
    public static final String MODEL_PARAM_NAME = "model_blocked_param_name";
    private static final Long MIGRATED_PARAM_FROM_1 = 100L;
    private static final Long MIGRATED_PARAM_TO_1 = 101L;

    private CategoryInfoProducer categoryInfoProducer;

    private ModelStorageHelper modelStorageHelper;

    private CategoryData categoryData;

    private ModelStorage.Model sku;
    private ModelStorage.Model parentModel;
    private RemoveCskuRedundantParametersTaskAction removeCskuRedundantParametersTaskAction;
    private Map<Long, ModelStorage.Model> models = new HashMap<>();

    @Before
    public void setUp() {
        super.setUp();
        this.categoryData = mock(CategoryData.class);
        when(categoryData.getHid()).thenReturn(CATEGORY_ID);
        when(categoryData.containsParam(anyLong())).thenReturn(true);
        when(categoryData.isSkuParameter(OPERATOR_FILLED_PARAM_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(MODEL_BLOCKED_PARAM_ID)).thenReturn(false);
        when(categoryData.isSkuParameter(MIGRATED_PARAM_TO_1)).thenReturn(false);
        when(categoryData.getParamById(anyLong())).thenAnswer((Answer<MboParameters.Parameter>) invocation -> {
            Object[] args = invocation.getArguments();
            Long id = (Long) args[0];
            return MboParameters.Parameter.newBuilder()
                    .setId(id)
                    .build();
        });
        doReturn(List.of(MIGRATED_PARAM_FROM_1)).when(categoryData)
                .getMigratedToParams(CollectionItemMatcher.setOf(MIGRATED_PARAM_TO_1));
        this.categoryInfoProducer = getCategoryInfoProducer();
        ModelStorageServiceMock modelStorageServiceMock = new ModelStorageServiceMock();
        this.modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        removeCskuRedundantParametersTaskAction = new RemoveCskuRedundantParametersTaskAction(categoryInfoProducer,
                gcSkuTicketDao, new Judge(), modelStorageHelper, gcSkuValidationDao);

        sku = ModelStorage.Model.newBuilder()
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(OPERATOR_FILLED_PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(OPERATOR_FILLED_PARAM_ID.toString()))
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(PARAM_ID.toString()).build())
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .setOwnerId(123)
                        .build()
                )
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(PARENT_MODEL_ID)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .setId(SKU_ID)
                .build();
        parentModel = ModelStorage.Model.newBuilder()
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MODEL_BLOCKED_PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(MODEL_BLOCKED_PARAM_ID.toString()).build())
                        .setValueSource(ModelStorage.ModificationSource.RULE)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MIGRATED_PARAM_TO_1)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(MIGRATED_PARAM_TO_1.toString()).build())
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .setSourceType(ModelStorage.ModelType.PARTNER.name())
                .setId(PARENT_MODEL_ID)
                .build();
        models.put(SKU_ID, sku);
        models.put(PARENT_MODEL_ID, parentModel);
        when(modelStorageHelper.findModelHierarchy(Set.of(SKU_ID))).thenReturn(models);
    }

    @Test
    public void whenMigratedParamIsBlockedThenRemoveBothParamsFromOffer() {
        doReturn(List.of(MIGRATED_PARAM_FROM_1)).when(categoryData)
                .getMigratedToParams(any());
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(Math.toIntExact(PARAM_ID), PARAM_NAME, "5", builder);
                addStrParam(Math.toIntExact(MIGRATED_PARAM_FROM_1), PARAM_NAME, "5", builder);
                addStrParam(Math.toIntExact(MIGRATED_PARAM_TO_1), PARAM_NAME, "50", builder);
                setMarketSkuId(SKU_ID, builder);
            });
        });

        ProcessTaskResult<ProcessDataBucketData> result = removeCskuRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets.size()).isEqualTo(1);

        List<DataCampContentMarketParameterValue.MarketParameterValue> parameterValuesList =
                tickets.get(0).getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList();
        assertThat(parameterValuesList).extracting(DataCampContentMarketParameterValue.MarketParameterValue::getParamId)
                .containsOnly(PARAM_ID);
    }

    @Test
    public void whenBlockedParamsThenRemoveFromOffer() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(Math.toIntExact(PARAM_ID), PARAM_NAME, "50", builder);
                addStrParam(Math.toIntExact(OPERATOR_FILLED_PARAM_ID), OPERATOR_FILLED_PARAM_NAME,
                        "2", builder);
                addStrParam(Math.toIntExact(MODEL_BLOCKED_PARAM_ID), MODEL_PARAM_NAME,
                        "Модельный параметр", builder);
                setMarketSkuId(SKU_ID, builder);
            });
        });


        ProcessTaskResult<ProcessDataBucketData> result = removeCskuRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());
        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> OPERATOR_FILLED_PARAM_ID.equals(param.getParamId())
                                || MODEL_BLOCKED_PARAM_ID.equals(param.getParamId()))
                ).isTrue());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .allMatch(param -> PARAM_ID.equals(param.getParamId()))).isTrue());
    }

    @Ignore
    @Test
    public void whenBlockedParamsHaveSameValueInOfferThenDoNotRemoveFromOffer() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(Math.toIntExact(PARAM_ID), PARAM_NAME, PARAM_ID.toString(), builder);
                addStrParam(Math.toIntExact(OPERATOR_FILLED_PARAM_ID), OPERATOR_FILLED_PARAM_NAME,
                        OPERATOR_FILLED_PARAM_ID.toString(), builder);
                addStrParam(Math.toIntExact(MODEL_BLOCKED_PARAM_ID), MODEL_PARAM_NAME,
                        MODEL_BLOCKED_PARAM_ID.toString(), builder);
                setMarketSkuId(SKU_ID, builder);
            });
        });


        ProcessTaskResult<ProcessDataBucketData> result = removeCskuRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());
        //MODEL_BLOCKED_PARAM_ID не в categoryInfo
        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> MODEL_BLOCKED_PARAM_ID.equals(param.getParamId()))
                ).isTrue());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .map(DataCampContentMarketParameterValue.MarketParameterValue::getParamId)
                        .collect(Collectors.toSet())).containsAll(
                                Set.of(PARAM_ID, OPERATOR_FILLED_PARAM_ID)));
    }

    @Ignore
    @Test
    public void whenBlockedParamsHaveNullValueInModelThenRemoveThem() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(Math.toIntExact(PARAM_ID), PARAM_NAME, PARAM_ID.toString(), builder);
                addStrParam(Math.toIntExact(OPERATOR_FILLED_PARAM_ID), OPERATOR_FILLED_PARAM_NAME,
                        OPERATOR_FILLED_PARAM_ID.toString(), builder);
                setMarketSkuId(SKU_ID, builder);
            });
        });
        ModelStorage.Model.Builder builder = sku.toBuilder();
        builder.clearParameterValues();
        builder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(OPERATOR_FILLED_PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build());
        sku = builder.build();
        models.put(SKU_ID, sku);

        ProcessTaskResult<ProcessDataBucketData> result = removeCskuRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());
        //MODEL_BLOCKED_PARAM_ID не в categoryInfo
        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> OPERATOR_FILLED_PARAM_ID.equals(param.getParamId()))
                ).isTrue());
    }

    @Test
    public void whenNonLeafCategoryThenSaveValidationInfo() {
        when(categoryInfoProducer.getCategoryData(CATEGORY_ID)).thenThrow(new NotLeafCategoryError("Ай-яй-яй",
                "Тестовая категория", CATEGORY_ID));
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(Math.toIntExact(OPERATOR_FILLED_PARAM_ID), OPERATOR_FILLED_PARAM_NAME,
                        "2", builder);
                setMarketSkuId(SKU_ID, builder);
            });
        });

        ProcessTaskResult<ProcessDataBucketData> result = removeCskuRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));
        tickets.forEach(ticket -> assertThat(GcSkuTicketStatus.RESULT_UPLOAD_STARTED).isEqualTo(ticket.getStatus()));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());
        tickets.forEach(ticket -> {
            assertThat(ticket.getDatacampOffer().getContent().getPartner()
                    .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                    .anyMatch(param -> OPERATOR_FILLED_PARAM_ID.equals(param.getParamId())
                            || MODEL_BLOCKED_PARAM_ID.equals(param.getParamId()))
            ).isTrue();
            assertThat(ticket.getValid()).isFalse();
        });

        List<GcSkuValidation> gcSkuValidations = gcSkuValidationDao.getGcSkuValidations(GcSkuValidationType.CATEGORY,
                tickets.get(0).getId());
        assertThat(gcSkuValidations).hasSize(1);
    }

    private CategoryInfoProducer getCategoryInfoProducer() {
        CategoryInfoProducer categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);

        Long2LongMap migratedParamMap = new Long2LongOpenHashMap();
        migratedParamMap.put(MIGRATED_PARAM_FROM_1, MIGRATED_PARAM_TO_1);

        when(categoryInfoProducer.extractCategoryInfo(any(), anyLong(), any()))
                .thenReturn(CategoryInfo.newBuilder()
                        .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_UI))
                        .addParameter(
                                ParameterInfoBuilder.asNumeric()
                                        .setId(OPERATOR_FILLED_PARAM_ID)
                                        .setName(OPERATOR_FILLED_PARAM_NAME)
                                        .setXslName(OPERATOR_FILLED_PARAM_NAME)
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .addParameter(
                                ParameterInfoBuilder.asString()
                                        .setId(PARAM_ID)
                                        .setName(PARAM_NAME)
                                        .setXslName(PARAM_NAME)
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .addParameter(
                                ParameterInfoBuilder.asString()
                                        .setId(MIGRATED_PARAM_FROM_1)
                                        .setName(PARAM_NAME)
                                        .setXslName(PARAM_NAME)
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .addParameter(
                                ParameterInfoBuilder.asString()
                                        .setId(MIGRATED_PARAM_TO_1)
                                        .setName(PARAM_NAME)
                                        .setXslName(PARAM_NAME)
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .setId(1L)
                        .setMigratedParams(migratedParamMap)
                        .build(ImportContentType.DCP_UI));
        when(categoryInfoProducer.getCategoryData(anyLong())).thenReturn(categoryData);
        return categoryInfoProducer;
    }
}
