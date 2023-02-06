package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentMatchers;

import ru.yandex.market.gutgin.tms.base.ModelGeneration;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CSKUDataPreparationTest extends DBDcpStateGenerator {

    private static final long SKU_ID = 100L;
    private static final long SKU_ID_2 = 101L;
    private static final long FAST_SKU_ID = 102L;
    private static final long MSKU_ID = 103L;
    private static final long MMODEL_ID = 1003L;
    private static final long MODEL_ID = 110L;
    private static final long ADDITIOANL_SKU_ID = 1000L;
    private static final long ADDITIOANL_SKU_ID_2 = 1001L;
    private static final long ADDITIOANL_MODEL_ID = 1100L;

    private CSKUDataPreparation cskuDataPreparation;
    private ModelStorageHelper modelStorageHelper;

    @Before
    public void setUp() {
        super.setUp();
        this.modelStorageHelper = mock(ModelStorageHelper.class);
        this.cskuDataPreparation = new CSKUDataPreparation(modelStorageHelper, gcSkuValidationDao);
    }

    @Test
    public void badSkuWithBadModelThenNoData() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(100L);
        mockSkuAndParent(badModel(), badSku());

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).isEmpty();
        //assert validation is generated
        List<GcSkuValidation> validations =
                gcSkuValidationDao.fetchByTicketIdsAndType(Arrays.asList(gcSkuTickets.get(0).getId()),
                        GcSkuValidationType.MAPPING_ON_VALID_MODEL_VALIDATION);
        assertThat(validations).hasSize(1);
        assertThat(validations).extracting(GcSkuValidation::getIsOk).containsOnly(false);
    }

    @Test
    public void goodSkuWithGoodModelThenOk() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(100L);
        ModelStorage.Model model = goodModel(MODEL_ID);
        ModelStorage.Model sku = goodSku(SKU_ID, MODEL_ID);
        mockSkuAndParent(model, sku);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnly(model);
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnly(sku);
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
    }

    @Test
    public void multipleTicketsWithSameModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(5);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(1).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(2).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(3).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(4).setExistingMboPskuId(SKU_ID);

        ModelStorage.Model model = goodModel(MODEL_ID);
        ModelStorage.Model sku = goodSku(SKU_ID, MODEL_ID);
        mockSkuAndParent(model, sku);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(5);
        assertThat(ticketWrappers).isSortedAccordingTo(Comparator.comparing(t -> t.getTicket().getId()));
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnly(model);
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnly(sku);
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsAll(gcSkuTickets);
    }

    @Test
    public void twoTicketsWithDifferentSkus() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(1).setExistingMboPskuId(ADDITIOANL_SKU_ID);
        ModelStorage.Model model = goodModel(MODEL_ID);
        ModelStorage.Model sku = goodSku(SKU_ID, MODEL_ID);
        ModelStorage.Model model2 = goodModel(ADDITIOANL_MODEL_ID);
        ModelStorage.Model sku2 = goodSku(ADDITIOANL_SKU_ID, ADDITIOANL_MODEL_ID);
        mockSkuAndParent(model, sku, model2, sku2);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(2);
        assertThat(ticketWrappers).isSortedAccordingTo(Comparator.comparing(t -> t.getTicket().getId()));
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        Map<Long, TicketWrapper> wrapperBySkuId = ticketWrappers.stream()
                .collect(Collectors.toMap(ticketWrapper -> ticketWrapper.getTicket().getExistingMboPskuId(),
                        Function.identity()));
        TicketWrapper ticketWrapper1 = wrapperBySkuId.get(SKU_ID);
        assertThat(ticketWrapper1).extracting(TicketWrapper::getSku).isEqualTo(sku);
        assertThat(ticketWrapper1).extracting(TicketWrapper::getParentModel).isEqualTo(model);

        TicketWrapper ticketWrapper2 = wrapperBySkuId.get(ADDITIOANL_SKU_ID);
        assertThat(ticketWrapper2).extracting(TicketWrapper::getSku).isEqualTo(sku2);
        assertThat(ticketWrapper2).extracting(TicketWrapper::getParentModel).isEqualTo(model2);
    }

    @Test
    public void collectDataForGroupedRequestReturnsWrapperWithAllSkusForParentModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(0).setDcpGroupId(100);
        gcSkuTickets.get(1).setExistingMboPskuId(ADDITIOANL_SKU_ID);
        gcSkuTickets.get(1).setDcpGroupId(100);

        ModelStorage.Model model1 = goodModel(MODEL_ID, SKU_ID, SKU_ID_2);
        ModelStorage.Model sku11 = goodSku(SKU_ID, MODEL_ID);
        ModelStorage.Model sku12 = goodSku(SKU_ID_2, MODEL_ID);

        ModelStorage.Model model2 = goodModel(ADDITIOANL_MODEL_ID, ADDITIOANL_SKU_ID, ADDITIOANL_SKU_ID_2);
        ModelStorage.Model sku21 = goodSku(ADDITIOANL_SKU_ID, ADDITIOANL_MODEL_ID);
        ModelStorage.Model sku22 = goodSku(ADDITIOANL_SKU_ID, ADDITIOANL_MODEL_ID);
        mockModelWithMultipleSkus(model1, sku11, sku12, model2, sku21, sku22);

        List<TicketWrapper> ticketWrappers =
                cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(2);
        assertThat(ticketWrappers).isSortedAccordingTo(Comparator.comparing(t -> t.getTicket().getId()));
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        Map<Long, TicketWrapper> wrapperBySkuId = ticketWrappers.stream()
                .collect(Collectors.toMap(ticketWrapper -> ticketWrapper.getTicket().getExistingMboPskuId(),
                        Function.identity()));
        TicketWrapper ticketWrapper1 = wrapperBySkuId.get(SKU_ID);
        assertThat(ticketWrapper1).extracting(TicketWrapper::getSku).isEqualTo(sku11);
        assertThat(ticketWrapper1).extracting(TicketWrapper::getParentModel).isEqualTo(model1);
        assertThat(ticketWrapper1).extracting(TicketWrapper::getAllSkus).isEqualTo(Arrays.asList(sku11,
                sku12));
        assertThat(ticketWrapper1).extracting(TicketWrapper::isValid).isEqualTo(true);

        TicketWrapper ticketWrapper2 = wrapperBySkuId.get(ADDITIOANL_SKU_ID);
        assertThat(ticketWrapper2).extracting(TicketWrapper::getSku).isEqualTo(sku21);
        assertThat(ticketWrapper2).extracting(TicketWrapper::getParentModel).isEqualTo(model2);
        assertThat(ticketWrapper2).extracting(TicketWrapper::getAllSkus).isEqualTo(Arrays.asList(sku21,
                sku22));
        assertThat(ticketWrapper2).extracting(TicketWrapper::isValid).isEqualTo(true);
    }

    @Test
    public void groupedTicketsWithEmptyMapping() {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        tickets.forEach(ticket -> ticket.setDcpGroupId(100));
        gcSkuTicketDao.update(tickets);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(tickets);

        assertThat(ticketWrappers).hasSize(2);
        assertThat(ticketWrappers).isSortedAccordingTo(Comparator.comparing(t -> t.getTicket().getId()));
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getAllSkus).containsOnly(Collections.emptyList());
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsAll(tickets);
    }

    @Test
    public void ticketWithoutSku() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
    }

    @Test
    public void ticketWithDeletedSku() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnlyNulls();
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
    }

    @Test
    public void ticketWithFastSkuConverting() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(FAST_SKU_ID);
        ModelStorage.Model fastSku = fastSku(FAST_SKU_ID);

        when(modelStorageHelper.findModels(any(), eq(false), eq(true)))
                .thenReturn(Collections.singletonList(fastSku));

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getParentModel()
                .getRelationsList().get(0).getId()).containsExactly(fastSku.getId());
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getSku().getCurrentType())
                .containsExactly(ModelStorage.ModelType.SKU.name());
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
        assertThat(ticketWrappers.get(0).getSku().getPublished()).isTrue();
        assertThat(ticketWrappers.get(0).getParentModel().getPublished()).isTrue();
    }

    @Test
    public void whenHiddenFastSkuThenPublishedResult() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(FAST_SKU_ID);
        ModelStorage.Model fastSku = fastSku(FAST_SKU_ID).toBuilder().setPublished(false).build();

        when(modelStorageHelper.findModels(any(), eq(false), eq(true)))
                .thenReturn(Collections.singletonList(fastSku));

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getParentModel()
                .getRelationsList().get(0).getId()).containsExactly(fastSku.getId());
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getSku().getCurrentType())
                .containsExactly(ModelStorage.ModelType.SKU.name());
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
        assertThat(ticketWrappers.get(0).getSku().getPublished()).isTrue();
        assertThat(ticketWrappers.get(0).getParentModel().getPublished()).isTrue();
    }

    @Test
    public void whenHiddenSkuThenHiddenResult() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        ModelStorage.Model model = goodModel(MODEL_ID);
        ModelStorage.Model sku = goodSku(SKU_ID, MODEL_ID).toBuilder().setPublished(false).build();
        mockSkuAndParent(model, sku);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnly(model);
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnly(sku);
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
        assertThat(ticketWrappers.get(0).getSku().getPublished()).isFalse();
        assertThat(ticketWrappers.get(0).getParentModel().getPublished()).isTrue();
    }


    @Test
    public void ticketWithFastSkuAsPartOfGroupConverting() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(FAST_SKU_ID);
        ModelStorage.Model fastSku = fastSku(FAST_SKU_ID);

        when(modelStorageHelper.findModels(any(), eq(false), eq(true)))
                .thenReturn(Collections.singletonList(fastSku));

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getParentModel()
                .getRelationsList().get(0).getId()).containsExactly(fastSku.getId());
        assertThat(ticketWrappers).extracting(wrapper -> wrapper.getSku().getCurrentType())
                .containsExactly(ModelStorage.ModelType.SKU.name());
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
        assertThat(ticketWrappers.get(0).getSku().getPublished()).isTrue();
        assertThat(ticketWrappers.get(0).getParentModel().getPublished()).isTrue();
    }

    @Test
    public void differentCategory() {
        GcSkuValidationDao gcSkuValidationDao = mock(GcSkuValidationDao.class);
        cskuDataPreparation = new CSKUDataPreparation(modelStorageHelper, gcSkuValidationDao);
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        gcSkuTickets.get(0).setCategoryId(SKU_ID);
        ModelStorage.Model model = goodModel(MODEL_ID);
        ModelStorage.Model sku = goodSku(SKU_ID, MODEL_ID);
        sku = sku.toBuilder().setStrictChecksRequired(true).build();
        mockSkuAndParent(model, sku);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(0);
        verify(gcSkuValidationDao, times(1))
                .saveValidationResults(any(), eq(GcSkuValidationType.CATEGORY));
    }

    @Test
    public void mskuIsValidModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(SKU_ID);
        ModelStorage.Model mModel = ModelGeneration.generateMModel(MODEL_ID, SKU_ID);
        ModelStorage.Model mSku = ModelGeneration.generateMsku(SKU_ID, MODEL_ID);
        mockSkuAndParent(mModel, mSku);

        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(gcSkuTickets);

        assertThat(ticketWrappers).hasSize(1);
        assertThat(ticketWrappers).extracting(TicketWrapper::isValid).containsOnly(true);
        assertThat(ticketWrappers).extracting(TicketWrapper::getParentModel).containsOnly(mModel);
        assertThat(ticketWrappers).extracting(TicketWrapper::getSku).containsOnly(mSku);
        assertThat(ticketWrappers).extracting(TicketWrapper::getTicket).containsOnly(gcSkuTickets.get(0));
    }

    private void mockSkuAndParent(ModelStorage.Model model, ModelStorage.Model sku) {
        doReturn(Collections.singletonList(model)).when(modelStorageHelper)
                .findModels(argThat(t -> t.contains(MODEL_ID)));
        doReturn(Collections.singletonList(sku)).when(modelStorageHelper)
                .findModels(argThat(t -> t.contains(SKU_ID)), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean());
    }

    private void mockModelWithMultipleSkus(ModelStorage.Model model1,
                                           ModelStorage.Model sku11, ModelStorage.Model sku12,
                                           ModelStorage.Model model2,
                                           ModelStorage.Model sku21, ModelStorage.Model sku22) {

        doReturn(Arrays.asList(model1, model2)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(MODEL_ID, ADDITIOANL_MODEL_ID))));
        doReturn(Arrays.asList(sku11, sku21)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(SKU_ID, ADDITIOANL_SKU_ID))), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean());

        // model1 имеет под собой 2 sku
        doReturn(Arrays.asList(sku11, sku12)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(SKU_ID_2, SKU_ID))));
        // model2 имеет под собой 2 sku
        doReturn(Arrays.asList(sku21, sku22)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(ADDITIOANL_SKU_ID, ADDITIOANL_SKU_ID_2))));

    }

    private void mockSkuAndParent(ModelStorage.Model model,
                                  ModelStorage.Model sku,
                                  ModelStorage.Model model2,
                                  ModelStorage.Model sku2) {
        doReturn(Arrays.asList(model, model2)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(MODEL_ID, ADDITIOANL_MODEL_ID))));
        doReturn(Arrays.asList(sku, sku2)).when(modelStorageHelper)
                .findModels(argThat(t -> t.containsAll(Arrays.asList(SKU_ID, ADDITIOANL_SKU_ID))), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean());
    }

    @NotNull
    private ModelStorage.Model badSku() {
        return ModelStorage.Model.newBuilder()
                .setId(SKU_ID)
                .setSourceType("PARTNER_SKU")
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(MODEL_ID)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build())
                .build();
    }

    @NotNull
    private ModelStorage.Model badModel() {
        return ModelStorage.Model.newBuilder()
                .setId(MODEL_ID)
                .setSourceType("NOT_PARTNER_SKU")
                .build();
    }

    @NotNull
    private ModelStorage.Model goodSku(long skuId, long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(skuId)
                .setSourceType("PARTNER_SKU")
                .setCurrentType("SKU")
                .setPublished(true)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(modelId)
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .build())
                .build();
    }

    @NotNull
    private ModelStorage.Model fastSku(long skuId) {
        return ModelStorage.Model.newBuilder()
                .setId(skuId)
                .setSourceType("FAST_SKU")
                .setCurrentType("FAST_SKU")
                .build();
    }

    @NotNull
    private ModelStorage.Model goodModel(long modelId, long... skus) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setPublished(true)
                .setSourceType("PARTNER")
                .setCurrentType("GURU");

        for (long l : skus) {
            builder.addRelations(ModelStorage.Relation.newBuilder()
                    .setId(l)
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .build());
        }
        return builder.build();
    }
}
