package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.VghValidationRequirements;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslMarkups;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.ExistingGoldenItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.GoldenItemSaveResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingData;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MicrometerWatch;

public class SskuProcessingPipeProcessorSendToErpTest extends MdmBaseDbTestClass {
    private static final BeruId BERU_ID = new BeruIdMock();
    private static final String SHOP_SKU = "shop-sku";
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(BERU_ID.getId(), SHOP_SKU);
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(BERU_ID.getBusinessId(), SHOP_SKU);

    @Autowired
    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor;
    @Autowired
    private SendToErpQueueRepository sendToErpQueueRepository;

    @Test
    public void whenCisHandleModeAppearsSendOfferToErp() {
        // given
        MdmIrisPayload.ReferenceInformation vghInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 1.0, 1.0))
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
                .setId("123"))
            .build();
        MdmIrisPayload.Item existingItem = MdmIrisPayload.Item.newBuilder()
            .addInformation(vghInformation)
            .build();
        ReferenceItemWrapper existingBusinessReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(BUSINESS_KEY, existingItem));
        ReferenceItemWrapper existingServiceReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(SHOP_SKU_KEY, existingItem));
        SskuProcessingData sskuProcessingData = sskuProcessingData(
            List.of(existingBusinessReferenceItem, existingServiceReferenceItem)
        );

        MdmIrisPayload.ReferenceInformation freshCalculatedCisInfo =
            ItemWrapperTestUtil.createCisReferenceInfo(MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
        MdmIrisPayload.Item freshCalculatedItem = existingItem.toBuilder()
            .addInformation(freshCalculatedCisInfo)
            .build();
        ReferenceItemWrapper freshCalculatedBusinessReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(BUSINESS_KEY, freshCalculatedItem));
        ReferenceItemWrapper freshCalculatedServiceReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(SHOP_SKU_KEY, freshCalculatedItem));
        GoldenItemSaveResult goldenItemSaveResult = goldenItemSaveResult(
            List.of(freshCalculatedServiceReferenceItem, freshCalculatedBusinessReferenceItem)
        );

        // when
        sskuProcessingPipeProcessor.enqueueForNextPipes(
            sskuProcessingData,
            goldenItemSaveResult,
            MdmQueuePriorities.MANUAL_OFFERS_PRIORITY,
            Mockito.mock(MicrometerWatch.class)
        );

        // then
        List<SskuToRefreshInfo> infos = sendToErpQueueRepository.getUnprocessedBatch(1000);
        Assertions.assertThat(infos)
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(BUSINESS_KEY);
    }

    @Test
    public void whenCisHandleModeChangedSendOfferToErp() {
        // given
        MdmIrisPayload.ReferenceInformation vghInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 1.0, 1.0))
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
                .setId("123"))
            .build();
        MdmIrisPayload.ReferenceInformation existingCisInfo =
            ItemWrapperTestUtil.createCisReferenceInfo(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
        MdmIrisPayload.Item existingItem = MdmIrisPayload.Item.newBuilder()
            .addInformation(vghInformation)
            .addInformation(existingCisInfo)
            .build();
        ReferenceItemWrapper existingBusinessReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(BUSINESS_KEY, existingItem));
        ReferenceItemWrapper existingServiceReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(SHOP_SKU_KEY, existingItem));
        SskuProcessingData sskuProcessingData = sskuProcessingData(
            List.of(existingBusinessReferenceItem, existingServiceReferenceItem)
        );

        MdmIrisPayload.ReferenceInformation freshCalculatedCisInfo =
            ItemWrapperTestUtil.createCisReferenceInfo(MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
        MdmIrisPayload.Item freshCalculatedItem = existingItem.toBuilder()
            .addInformation(freshCalculatedCisInfo)
            .build();
        ReferenceItemWrapper freshCalculatedBusinessReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(BUSINESS_KEY, freshCalculatedItem));
        ReferenceItemWrapper freshCalculatedServiceReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(SHOP_SKU_KEY, freshCalculatedItem));
        GoldenItemSaveResult goldenItemSaveResult = goldenItemSaveResult(
            List.of(freshCalculatedServiceReferenceItem, freshCalculatedBusinessReferenceItem)
        );

        // when
        sskuProcessingPipeProcessor.enqueueForNextPipes(
            sskuProcessingData,
            goldenItemSaveResult,
            MdmQueuePriorities.MANUAL_OFFERS_PRIORITY,
            Mockito.mock(MicrometerWatch.class)
        );

        // then
        List<SskuToRefreshInfo> infos = sendToErpQueueRepository.getUnprocessedBatch(1000);
        Assertions.assertThat(infos)
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(BUSINESS_KEY);
    }

    @Test
    public void whenNoChangesNotSendOfferToErp() {
        // given
        MdmIrisPayload.ReferenceInformation vghInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setItemShippingUnit(ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, 1.0, 1.0))
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
                .setId("123"))
            .build();
        MdmIrisPayload.ReferenceInformation cisInfo =
            ItemWrapperTestUtil.createCisReferenceInfo(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .addInformation(vghInformation)
            .addInformation(cisInfo)
            .build();
        ReferenceItemWrapper businessReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(BUSINESS_KEY, item));
        ReferenceItemWrapper serviceReferenceItem = new ReferenceItemWrapper()
            .setReferenceItem(updateItemKey(SHOP_SKU_KEY, item));
        SskuProcessingData sskuProcessingData = sskuProcessingData(
            List.of(businessReferenceItem, serviceReferenceItem)
        );
        GoldenItemSaveResult goldenItemSaveResult = goldenItemSaveResult(
            List.of(businessReferenceItem, serviceReferenceItem)
        );

        // when
        sskuProcessingPipeProcessor.enqueueForNextPipes(
            sskuProcessingData,
            goldenItemSaveResult,
            MdmQueuePriorities.MANUAL_OFFERS_PRIORITY,
            Mockito.mock(MicrometerWatch.class)
        );

        // then
        Assertions.assertThat(sendToErpQueueRepository.countItems()).isZero();
    }

    private static SskuProcessingData sskuProcessingData(Collection<ReferenceItemWrapper> referenceItems) {
        MdmSskuKeyGroup businessGroup = MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY, List.of(SHOP_SKU_KEY));
        return new SskuProcessingData(
            Set.of(BUSINESS_KEY),
            Set.of(SHOP_SKU_KEY),
            List.of(businessGroup),
            MdmSskuGroupUtils.keyGroupsByAnyKey(List.of(businessGroup)),
            Map.of(),
            Map.of(),
            Map.of(),
            referenceItems.stream()
                .map(refItem -> new ExistingGoldenItemWrapper(refItem, null))
                .collect(Collectors.toMap(ExistingGoldenItemWrapper::getKey, Function.identity())),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            VghValidationRequirements.NO_REQUIREMENTS,
            Set.of(),
            new RslMarkups(),
            Map.of(),
            Map.of(),
            Map.of(),
            Set.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }

    private static GoldenItemSaveResult goldenItemSaveResult(Collection<ReferenceItemWrapper> referenceItemWrappers) {
        return new GoldenItemSaveResult(
            referenceItemWrappers.stream()
                .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity())),
            Map.of(),
            Map.of()
        );
    }

    private static MdmIrisPayload.Item updateItemKey(ShopSkuKey shopSkuKey, MdmIrisPayload.Item item) {
        return item.toBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(shopSkuKey.getSupplierId())
                .setShopSku(shopSkuKey.getShopSku()))
            .build();
    }
}
