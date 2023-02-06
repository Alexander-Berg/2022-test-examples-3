package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logbroker.LogbrokerInteractionException;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.service.queue.UploadReferenceItemsToLogbrokerService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:MagicNumber")
public class UploadReferenceItemsToLogbrokerExecutorTest extends MdmBaseDbTestClass {
    private static final long SEED = 16345;

    @Autowired
    SendReferenceItemQRepository sendReferenceItemQRepository;

    private EnhancedRandom random;

    private UploadReferenceItemsToLogbrokerExecutor executor;

    private ReferenceItemRepository referenceItemRepository;

    private MdmLogbrokerServiceMock logbrokerProducerService;

    private UploadReferenceItemsToLogbrokerService uploadReferenceItemsToLogbrokerService;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);

        StorageKeyValueService keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue(MdmProperties.LB_TO_IRIS_DYNAMIC_BATCH_SIZE_KEY, 1);

        referenceItemRepository = new ReferenceItemRepositoryMock();

        logbrokerProducerService = new MdmLogbrokerServiceMock();

        uploadReferenceItemsToLogbrokerService = new UploadReferenceItemsToLogbrokerService(
            referenceItemRepository,
            sendReferenceItemQRepository,
            logbrokerProducerService,
            keyValueService
        );

        executor = new UploadReferenceItemsToLogbrokerExecutor(uploadReferenceItemsToLogbrokerService);
    }

    @Test
    public void whenUploadReferenceItemsToLogbrokerShouldMarkAsProcessed() {
        ReferenceItemWrapper before = getItem(new ShopSkuKey(1709, "42_0"));
        referenceItemRepository.insert(before);
        sendReferenceItemQRepository.enqueue(before.getShopSkuKey());
        executor.execute();
        List<SskuToRefreshInfo> after = sendReferenceItemQRepository.findAll();
        Assertions.assertThat(after).hasSize(1);
        Assertions.assertThat(after.get(0).getEntityKey()).isEqualTo(before.getKey());
        Assertions.assertThat(after.get(0).isProcessed()).isTrue();
    }

    @Test
    public void whenUploadReferenceItemsToLogbrokerShouldNotChangeReferenceItem() {
        ReferenceItemWrapper before = getItem(new ShopSkuKey(1709, "42_0"));
        referenceItemRepository.insert(before);
        sendReferenceItemQRepository.enqueue(before.getShopSkuKey());
        executor.execute();
        ReferenceItemWrapper after = referenceItemRepository.findById(before.getShopSkuKey());
        Assertions.assertThat(after.getItem()).isEqualTo(before.getItem());
        Assertions.assertThat(sendReferenceItemQRepository.findAll().get(0).getEntityKey()).isEqualTo(before.getKey());
    }

    @Test
    public void whenUploadReferenceItemsToLogbrokerShouldProcessAllItems() {
        List<ReferenceItemWrapper> items = List.of(
            getItem(new ShopSkuKey(1709, "42_0")),
            getItem(new ShopSkuKey(1709, "42_1")),
            getItem(new ShopSkuKey(1709, "42_2")),
            getItem(new ShopSkuKey(1709, "42_3"))
        );
        referenceItemRepository.insertBatch(items);
        sendReferenceItemQRepository.enqueueAll(
            items.stream().map(ReferenceItemWrapper::getShopSkuKey).collect(Collectors.toList()));

        List<ReferenceItemWrapper> unprocessedBefore = getNextBatch(10);
        Assertions.assertThat(unprocessedBefore).hasSameSizeAs(items);
        Assertions.assertThat(unprocessedBefore).containsExactlyInAnyOrder(items.toArray(ReferenceItemWrapper[]::new));

        executor.execute();

        List<ReferenceItemWrapper> unprocessedAfter = getNextBatch(4);
        Assertions.assertThat(unprocessedAfter).isEmpty();
    }

    @Test
    public void whenUploadReferenceItemsToLogbrokerShouldStopOnLogbrokerFailure() {
        ReferenceItemWrapper before = getItem(new ShopSkuKey(1709, "42_0"));
        referenceItemRepository.insert(before);
        sendReferenceItemQRepository.enqueue(before.getShopSkuKey());

        // fail all events
        logbrokerProducerService.setShouldFail(true);

        SoftAssertions.assertSoftly(softAssertions -> {
            // expect LogbrokerInteractionException
            softAssertions.assertThatCode(() -> executor.execute()).isInstanceOf(LogbrokerInteractionException.class);

            // and reference item not changed in any way
            ReferenceItemWrapper after = referenceItemRepository.findById(before.getShopSkuKey());
            softAssertions.assertThat(after.isProcessed()).isFalse();
            softAssertions.assertThat(after.getItem()).isEqualTo(before.getItem());
            softAssertions.assertThat(after).isEqualTo(before);
        });
    }

    @Test
    public void whenUploadEmptyReferenceItemsToLogbrokerMarkThemProcessed() {
        List<ReferenceItemWrapper> items = List.of(
            new ReferenceItemWrapper(MdmIrisPayload.Item.newBuilder()
                .setItemId(ItemWrapperTestUtil.getMdmIdentifier(new ShopSkuKey(1709, "42_0")))
                .build()),
            getItem(new ShopSkuKey(1709, "42_1")),
            getItem(new ShopSkuKey(1709, "42_2")),
            getItem(new ShopSkuKey(1709, "42_3"))
        );
        referenceItemRepository.insertBatch(items);
        sendReferenceItemQRepository.enqueueAll(
            items.stream().map(ReferenceItemWrapper::getShopSkuKey).collect(Collectors.toList()));

        List<ReferenceItemWrapper> unprocessedBefore = getNextBatch(1000);
        Assertions.assertThat(unprocessedBefore).hasSameSizeAs(items);
        Assertions.assertThat(unprocessedBefore).containsExactlyInAnyOrder(items.toArray(ReferenceItemWrapper[]::new));

        executor.execute();

        List<ReferenceItemWrapper> unprocessedAfter = getNextBatch(1000);
        Assertions.assertThat(unprocessedAfter).isEmpty();
    }

    private List<ReferenceItemWrapper> getNextBatch(int batchSize) {
        List<SskuToRefreshInfo> toSend = sendReferenceItemQRepository.getUnprocessedBatch(batchSize);
        List<ShopSkuKey> keysToSend = toSend.stream().map(SskuToRefreshInfo::getEntityKey).collect(Collectors.toList());
        List<ReferenceItemWrapper> unprocessedItems =
            referenceItemRepository.findByIds(keysToSend);
        return unprocessedItems;
    }

    private ReferenceItemWrapper getItem(ShopSkuKey key) {
        MdmIrisPayload.Item.Builder itemBuilder = MdmIrisPayload.Item.newBuilder()
            .setItemId(ItemWrapperTestUtil.getMdmIdentifier(key));

        // information 0 WAREHOUSE - w+h+l+wg
        itemBuilder.addInformationBuilder()
            .setSource(
                MdmIrisPayload.Associate.newBuilder()
                    .setId("1")
                    .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
            )
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(1d, 2d, 3d, 4d, null, null)
                    .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            );

        // information 1 SUPPLIER - wn
        itemBuilder.addInformationBuilder()
            .setSource(
                MdmIrisPayload.Associate.newBuilder()
                    .setId("2")
                    .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
            )
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(1d, 2d, 3d, 4d, null, null)
                    .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.ITEM)
            );

        // information 2 MDM - rsl
        itemBuilder.addInformationBuilder()
            .setSource(
                MdmIrisPayload.Associate.newBuilder()
                    .setId("3")
                    .setType(MdmIrisPayload.MasterDataSource.MDM)
            )
            .setName(MdmIrisPayload.StringValue.newBuilder().setValue(key.getShopSku()).setUpdatedTs(0))
            .setBoxCapacity(MdmIrisPayload.Int32Value.newBuilder().setValue(random.nextInt()).setUpdatedTs(2))
            .addCargotypeId(MdmIrisPayload.Int32Value.newBuilder().setValue(random.nextInt()).setUpdatedTs(4))
            .addMinInboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder().setValue(3).setUpdatedTs(1));


        var result = new ReferenceItemWrapper()
            .setReferenceItem(itemBuilder.build());
        result.setProcessed(false);
        return result;
    }


}
