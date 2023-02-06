package ru.yandex.market.mbo.mdm.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.randomizers.text.StringRandomizer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.LbFailedOfferQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.old.FailedOfferFromSskuTypeQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.old.LbFailedOfferQueueFailedRepositoryOld;
import ru.yandex.market.mbo.mdm.common.service.queue.FailedOffersException;
import ru.yandex.market.mbo.mdm.common.service.queue.MdmQueueFailedOffersService;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessLbFailedOfferQueueService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 * @date 9/6/21
 */
public class ProcessLbFailedOfferQueueServiceTest extends MdmBaseDbTestClass {
    private final static int BIZ_SUPPLIER = 100;
    private final static int ANOTHER_BIZ_SUPPLIER = 101;
    private final static String SHOP_SKU = "cupcake";
    private final static String YET_ANOTHER_SHOP_SKU = "cookie";
    private final static int BATCH_SIZE = 50;
    private final static int SKU_ID_LENGTH = 100;
    private final static long SEED = 1385325434L;

    @Autowired
    private LbFailedOfferQueueRepository lbFailedOfferQueueRepository;
    @Autowired
    private LbFailedOfferQueueFailedRepositoryOld lbFailedOfferQueueFailedRepository;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private MdmQueueFailedOffersService failedOffersService;

    private StorageKeyValueService storageKeyValue;
    private MdmDatacampService mdmDatacampService;

    @Captor
    private ArgumentCaptor<Integer> someBusinessIdCaptor;
    @Captor
    private ArgumentCaptor<Collection<String>> someSkusCaptor;

    private ProcessLbFailedOfferQueueService processLbFailedOfferQueueService;
    private StringRandomizer stringRandomizer;

    @Before
    public void before() {
        storageKeyValue = new StorageKeyValueServiceMock();
        mdmDatacampService = Mockito.mock(MdmDatacampService.class);

        processLbFailedOfferQueueService = new ProcessLbFailedOfferQueueService(
            storageKeyValue,
            mdmDatacampService,
            lbFailedOfferQueueRepository,
            failedOffersService,
            lbFailedOfferQueueFailedRepository
        );

        Mockito.doNothing().when(mdmDatacampService).importOffersFromDatacamp(
            someBusinessIdCaptor.capture(),
            someSkusCaptor.capture()
        );

        stringRandomizer = new StringRandomizer(SKU_ID_LENGTH, SKU_ID_LENGTH, SEED);
    }

    @Test
    public void successfullyProcessFailedOfferQueueTest() {
        List<ShopSkuKey> originalKeys = List.of(
            new ShopSkuKey(BIZ_SUPPLIER, SHOP_SKU),
            new ShopSkuKey(BIZ_SUPPLIER, YET_ANOTHER_SHOP_SKU)
        );
        queuesManager.enqueueLbFailedOffers(originalKeys);

        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount()).isEqualTo(originalKeys.size());
        List<SskuToRefreshInfo> queueBeforeProcessing = lbFailedOfferQueueRepository.getUnprocessedBatch(BATCH_SIZE);
        Assertions.assertThat(keys(queueBeforeProcessing)).containsAll(originalKeys);

        processLbFailedOfferQueueService.processQueueItems();

        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount()).isEqualTo(0);
        List<SskuToRefreshInfo> processedItems = lbFailedOfferQueueRepository.findAll();
        Assertions.assertThat(keys(processedItems)).containsAll(originalKeys);
    }

    @Test
    public void offersShouldBGroupedByBusinessITest() {
        List<ShopSkuKey> lbFailedOffersByBusiness1 = generateSskus(BIZ_SUPPLIER);
        List<ShopSkuKey> lbFailedOffersByBusiness2 = generateSskus(ANOTHER_BIZ_SUPPLIER);

        queuesManager.enqueueLbFailedOffers(lbFailedOffersByBusiness1);
        queuesManager.enqueueLbFailedOffers(lbFailedOffersByBusiness2);

        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount())
            .isEqualTo(lbFailedOffersByBusiness1.size() + lbFailedOffersByBusiness2.size());
        List<SskuToRefreshInfo> queueBeforeProcessing = lbFailedOfferQueueRepository.getUnprocessedBatch(BATCH_SIZE);
        Assertions.assertThat(keys(queueBeforeProcessing)).containsAll(lbFailedOffersByBusiness1);
        Assertions.assertThat(keys(queueBeforeProcessing)).containsAll(lbFailedOffersByBusiness2);

        processLbFailedOfferQueueService.processQueueItems();
        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount()).isEqualTo(0);
        List<SskuToRefreshInfo> processedItems = lbFailedOfferQueueRepository.findAll();
        Assertions.assertThat(keys(processedItems)).containsAll(lbFailedOffersByBusiness1);
        Assertions.assertThat(keys(processedItems)).containsAll(lbFailedOffersByBusiness2);

        Assertions.assertThat(someBusinessIdCaptor.getAllValues().get(0)).isEqualTo(BIZ_SUPPLIER);
        Assertions.assertThat(someBusinessIdCaptor.getAllValues().get(1)).isEqualTo(ANOTHER_BIZ_SUPPLIER);

        List<String> skuIds1 = lbFailedOffersByBusiness1.stream()
            .map(ShopSkuKey::getShopSku)
            .collect(Collectors.toList());
        List<String> skuIds2 = lbFailedOffersByBusiness2.stream()
            .map(ShopSkuKey::getShopSku)
            .collect(Collectors.toList());
        Assertions.assertThat(someSkusCaptor.getAllValues().get(0)).containsAll(skuIds1);
        Assertions.assertThat(someSkusCaptor.getAllValues().get(1)).containsAll(skuIds2);
    }

    @Test
    public void testFailedOffersWithExceededRetryCountSavedToAnotherRepoAsWell() {
        // given
        storageKeyValue.putValue(MdmProperties.USE_RETRIES_IN_LB_FAILED_OFFER_QUEUE, true);
        lbFailedOfferQueueFailedRepository.deleteAll();

        List<ShopSkuKey> lbFailedOffersByBusiness1 = generateSskus(BIZ_SUPPLIER);
        List<ShopSkuKey> lbFailedOffersByBusiness2 = generateSskus(ANOTHER_BIZ_SUPPLIER);

        queuesManager.enqueueLbFailedOffers(lbFailedOffersByBusiness1);
        queuesManager.enqueueLbFailedOffers(lbFailedOffersByBusiness2);

        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount())
            .isEqualTo(lbFailedOffersByBusiness1.size() + lbFailedOffersByBusiness2.size());
        List<SskuToRefreshInfo> queueBeforeProcessing = lbFailedOfferQueueRepository.getUnprocessedBatch(BATCH_SIZE);
        Assertions.assertThat(keys(queueBeforeProcessing)).containsAll(lbFailedOffersByBusiness1);
        Assertions.assertThat(keys(queueBeforeProcessing)).containsAll(lbFailedOffersByBusiness2);

        // when
        Mockito.doThrow(
            new FailedOffersException("Exception", BIZ_SUPPLIER, skus(lbFailedOffersByBusiness1)))
            .when(mdmDatacampService).importOffersFromDatacamp(Mockito.anyInt(), Mockito.anyCollection());
        Mockito.doNothing().when(mdmDatacampService)
            .importOffersFromDatacamp(ANOTHER_BIZ_SUPPLIER, skus(lbFailedOffersByBusiness2));

        processLbFailedOfferQueueService.processQueueItems();

        // then
        Assertions.assertThat(lbFailedOfferQueueRepository.getUnprocessedItemsCount()).isEqualTo(0);
        List<SskuToRefreshInfo> processedItems = lbFailedOfferQueueRepository.findAll();
        Assertions.assertThat(keys(processedItems)).containsAll(lbFailedOffersByBusiness1);
        Assertions.assertThat(keys(processedItems)).containsAll(lbFailedOffersByBusiness2);

        Set<Integer> supplierIds = processedItems.stream().map(SskuToRefreshInfo::getSupplierId)
            .collect(Collectors.toSet());
        Assertions.assertThat(supplierIds).containsExactlyInAnyOrder(BIZ_SUPPLIER, ANOTHER_BIZ_SUPPLIER);

        Set<ShopSkuKey> discardedKeys = processedItems.stream()
            .filter(MdmQueueInfoBase::isDiscarded)
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toSet());
        Assertions.assertThat(discardedKeys).containsExactlyInAnyOrderElementsOf(lbFailedOffersByBusiness1);

        // проверяем таблицу с failed-офферами
        List<FailedOfferFromSskuTypeQueue> offersWithExceededRetryCount = lbFailedOfferQueueFailedRepository.findAll();
        Assertions.assertThat(offersWithExceededRetryCount).isNotEmpty();

        Set<ShopSkuKey> retryExceededKeys = offersWithExceededRetryCount.stream()
            .map(FailedOfferFromSskuTypeQueue::getEntityKey)
            .collect(Collectors.toSet());
        Assertions.assertThat(retryExceededKeys).containsExactlyInAnyOrderElementsOf(lbFailedOffersByBusiness1);

        for (FailedOfferFromSskuTypeQueue offer : offersWithExceededRetryCount) {
            Assertions.assertThat(offer.getRetryCountFromMainQueue()).isEqualTo(1);
            Assertions.assertThat(offer.getAddedTimestampFromMainQueue()).isNotNull();
            Assertions.assertThat(offer.getEntityKey()).isNotNull();
            Assertions.assertThat(offer.getRefreshReasonsFromMainQueue()).isEmpty();
        }
    }

    private static List<ShopSkuKey> keys(List<SskuToRefreshInfo> infos) {
        return infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
    }

    private static List<String> skus(List<ShopSkuKey> keys) {
        return keys.stream().map(ShopSkuKey::getShopSku).collect(Collectors.toList());
    }

    private List<ShopSkuKey> generateSskus(int supplierId) {
        return IntStream.range(0, 3)
            .mapToObj(i -> stringRandomizer.getRandomValue())
            .map(sku -> new ShopSkuKey(supplierId, sku))
            .collect(Collectors.toList());
    }
}
