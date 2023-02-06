package ru.yandex.market.mbo.tms.model.transitions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mboc.export.MbocExport;
import ru.yandex.market.mboc.http.SupplierOffer;

@SuppressWarnings({"checkstyle:magicNumber"})
public class OfferMappingsTransitionsHandlerTest {
    private static final long HID = 1000L;
    private static final long UID = 1L;

    private OfferMappingsTransitionsHandler handler;
    private ModelStorageServiceStub modelStorageService;

    @Before
    public void setUp() {
        AutoUser autoUser = new AutoUser(UID);
        modelStorageService = Mockito.spy(new ModelStorageServiceStub());
        handler = new OfferMappingsTransitionsHandler(modelStorageService, autoUser);
    }

    @Test
    public void testDifferentType() {
        List<MbocExport.MappingTransition> transitions = Arrays.asList(

        );
        handler.process(transitions);
        Mockito.verifyZeroInteractions(modelStorageService);
    }

    @Test
    public void testTransitions() {
        List<MbocExport.MappingTransition> transitions = Arrays.asList(
            // should be processed
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(1L)
                .setNewMskuId(2L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_VIRTUAL)
                .build(),
            // ignored because newId = 0
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(4L)
                .setNewMskuId(0L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_VIRTUAL)
                .build(),
            // ignored because fastSku 5 does not exist
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(5L)
                .setNewMskuId(6L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_VIRTUAL)
                .build(),
            // ignored because fastSku 7 is deleted
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(7L)
                .setNewMskuId(8L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_VIRTUAL)
                .build(),
            // ignored because skuType != TYPE_VIRTUAL
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(9L)
                .setNewMskuId(10L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_MARKET)
                .build(),
            // ignored because sku from storage actually is not FAST_SKU
            MbocExport.MappingTransition.newBuilder()
                .setOldMskuId(3L)
                .setNewMskuId(10L)
                .setOldSkuType(SupplierOffer.SkuType.TYPE_VIRTUAL)
                .build()
        );
        modelStorageService.saveModels(
            ModelSaveGroup.fromModels(
                createFastSku(1L, false),
                createFastSku(4L, false),
                createFastSku(7L, true),
                createSku(2L, 22L),
                createSku(3L, 33L)),
            new ModelSaveContext(UID)
        );
        Mockito.clearInvocations(modelStorageService);

        handler.process(transitions);

        Set<Long> expectedSearchedIds = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L));
        Mockito.verify(modelStorageService).searchByIds(Mockito.eq(expectedSearchedIds));

        ArgumentCaptor<ModelSaveGroup> groupArgumentCaptor = ArgumentCaptor.forClass(ModelSaveGroup.class);
        Mockito.verify(modelStorageService).saveModels(groupArgumentCaptor.capture(), Mockito.any());
        ModelSaveGroup group = groupArgumentCaptor.getValue();
        Assertions.assertThat(group.getModels()).extracting(CommonModel::getId)
            .containsExactlyInAnyOrder(1L, 4L);
        Assertions.assertThat(group.getModelTransitions().values().stream().flatMap(List::stream)
            .collect(Collectors.toList())).containsExactlyInAnyOrder(
            new ModelTransition()
                .setOldEntityId(1L)
                .setNewEntityId(2L)
                .setOldEntityDeleted(true)
                .setEntityType(EntityType.SKU)
                .setPrimaryTransition(true)
                .setType(ModelTransitionType.TRANSFORMATION)
                .setReason(ModelTransitionReason.FAST_SKU_TRANSFORMATION),
            new ModelTransition()
                .setOldEntityId(1L)
                .setNewEntityId(22L)
                .setOldEntityDeleted(true)
                .setEntityType(EntityType.MODEL)
                .setPrimaryTransition(true)
                .setType(ModelTransitionType.TRANSFORMATION)
                .setReason(ModelTransitionReason.FAST_SKU_TRANSFORMATION)
        );
    }

    @Test
    public void testFastSkuToPartnerSkuTransition() throws InvalidProtocolBufferException {
        String json = "{\"date\":\"1853177279000\",\"id\":\"20\",\"action_id\":\"18\"," +
            "\"supplier_type\":\"TYPE_MARKET_SHOP\",\"supplier_id\":\"373\",\"supplier_sku_id\":\"EqTQxjAhI\"," +
            "\"old_msku_id\":\"100500\",\"new_msku_id\":\"100500\",\"old_sku_type\":\"TYPE_VIRTUAL\"," +
            "\"new_sku_type\":\"TYPE_PARTNER\"}";

        MbocExport.MappingTransition.Builder builder = MbocExport.MappingTransition.newBuilder();
        JsonFormat.parser().merge(json, builder);

        List<MbocExport.MappingTransition> messages = Arrays.asList(builder.build());
        modelStorageService.saveModels(
            ModelSaveGroup.fromModels(
                createPartnerSku(100500L, 100L)
            ),
            new ModelSaveContext(UID)
        );
        Mockito.clearInvocations(modelStorageService);

        handler.process(messages);

        ArgumentCaptor<ModelSaveGroup> groupArgumentCaptor = ArgumentCaptor.forClass(ModelSaveGroup.class);
        Mockito.verify(modelStorageService).saveModels(groupArgumentCaptor.capture(), Mockito.any());
        ModelSaveGroup group = groupArgumentCaptor.getValue();

        Assertions.assertThat(group.getModelTransitions().values().stream().flatMap(List::stream)
            .collect(Collectors.toList())).containsExactlyInAnyOrder(
            new ModelTransition()
                .setOldEntityId(100500L)
                .setNewEntityId(100L)
                .setOldEntityDeleted(false)
                .setEntityType(EntityType.MODEL)
                .setPrimaryTransition(true)
                .setType(ModelTransitionType.TRANSFORMATION)
                .setReason(ModelTransitionReason.FAST_SKU_TRANSFORMATION)
        );

    }

    private CommonModel createFastSku(long id, boolean deleted) {
        return CommonModelBuilder.newBuilder(id, HID)
            .setDeleted(deleted)
            .currentType(CommonModel.Source.FAST_SKU)
            .source(CommonModel.Source.FAST_SKU)
            .endModel();
    }

    private CommonModel createPartnerSku(long id, long skuParentId) {
        return CommonModelBuilder.newBuilder(id, HID)
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.PARTNER)
            .quality(KnownIds.IS_PARTNER_PARAM_ID)
            .withSkuParentRelation(HID, skuParentId)
            .endModel();
    }

    private CommonModel createSku(long id, long skuParentId) {
        return CommonModelBuilder.newBuilder(id, HID)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .withSkuParentRelation(HID, skuParentId)
            .endModel();
    }
}

