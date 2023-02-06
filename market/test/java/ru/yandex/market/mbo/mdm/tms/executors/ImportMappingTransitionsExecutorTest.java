package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ExpectedMappingQuality;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MbocApiMappingsImportServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.service.mapping.MappingsUpdateService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.mdm.common.utils.MdmQueueInfoBaseUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.RealConverter;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ImportMappingTransitionsExecutorTest extends MdmBaseDbTestClass {

    private static final int FAKE_BERU_ID = 999999;
    private static final int CATEGORY_ID_1 = 10;
    private static final int CATEGORY_ID_2 = 11;
    private static final long MSKU_ID_1 = 200;
    private static final long MSKU_ID_2 = 201;

    // should be large enough to pull everything in one batch
    private static final int SIZE = 100;
    private final List<MboMappings.ProviderProductInfo> offers = new ArrayList<>();
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MappingsUpdateService mappingsUpdateService;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;

    private StorageKeyValueServiceMock keyValueService;

    private ImportMappingTransitionsExecutor executor;

    @Before
    public void setUp() {
        MboMappingsService mboMappingsService = Mockito.mock(MboMappingsService.class);

        Mockito.when(mboMappingsService.searchProductInfoByYtStamp(
            Mockito.any(MboMappings.SearchProductInfoByYtStampRequest.class)
        )).thenAnswer((Answer<MboMappings.SearchProductInfoByYtStampResponse>) invocation -> {
                MboMappings.SearchProductInfoByYtStampRequest request = invocation.getArgument(0);
                long fromStamp = request.getFromStamp();
                long limit = request.getCount();
                List<MboMappings.ProviderProductInfo> matchedMappings = getProviderProductInfos(fromStamp, limit);
                return MboMappings.SearchProductInfoByYtStampResponse.newBuilder()
                    .addAllProviderProductInfo(matchedMappings)
                    .build();
            }
        );

        offers.clear();
        keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue(
            MdmProperties.MAPPING_IMPORT_TIMESTAMP_KEY,
            0L
        );
        executor = new ImportMappingTransitionsExecutor(
            new MbocApiMappingsImportServiceImpl(
                mboMappingsService,
                mdmSskuGroupManager,
                mappingsUpdateService,
                mdmSupplierRepository,
                mdmQueuesManager,
                new SupplierConverterServiceMock(),
                keyValueService
            ),
            Mockito.mock(MdmSolomonPushService.class)
        );
    }

    @Test
    public void whenNoOffersShouldNotFail() {
        executor.execute();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mskuToRefreshRepository.findAll()).isEmpty();
            s.assertThat(sskuToRefreshRepository.findAll()).isEmpty();
            s.assertThat(mappingsCacheRepository.findAll()).isEmpty();
        });
    }

    @Test
    public void whenNewMappingShouldAddToCache() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        MappingCacheDao savedMapping = mappingsCacheRepository.findByIds(List.of(key1), ExpectedMappingQuality.ANY)
            .get(0);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedMapping).isNotNull();
            s.assertThat(savedMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);
            s.assertThat(mappingsCacheRepository.findAll()).hasSize(1);
        });
    }

    @Test
    public void whenNewMappingShouldAddBlueAndUnknownToCacheAndIgnoreWhite() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        ShopSkuKey key2 = new ShopSkuKey(2, "offer_2");
        ShopSkuKey key3 = new ShopSkuKey(3, "offer_3");
        mdmSupplierRepository.insertBatch(
            supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null),
            supplier(key2.getSupplierId(), MdmSupplierType.MARKET_SHOP, null)
        );
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key2, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key3, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        MappingCacheDao savedMapping1 = mappingsCacheRepository.findByIds(List.of(key1), ExpectedMappingQuality.ANY)
            .get(0);
        MappingCacheDao savedMapping2 = mappingsCacheRepository.findByIds(List.of(key2), ExpectedMappingQuality.ANY)
            .stream().findFirst().orElse(null);
        MappingCacheDao savedMapping3 = mappingsCacheRepository.findByIds(List.of(key3), ExpectedMappingQuality.ANY)
            .get(0);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedMapping1).isNotNull();
            s.assertThat(savedMapping1).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedMapping1).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            Assertions.assertThat(savedMapping2).isNull();

            Assertions.assertThat(savedMapping3).isNotNull();
            s.assertThat(savedMapping3).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedMapping3).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            s.assertThat(mappingsCacheRepository.findAll()).hasSize(2);
        });
    }

    @Test
    public void whenNewBusinessMappingShouldAddToCacheAllBusinessGroup() {
        int businessId = 111;
        int xSupplierId = 222;
        int ySupplierId = 223;
        String shopSku = "offer_1";
        mdmSupplierRepository.insertBatch(
            supplier(businessId, MdmSupplierType.BUSINESS, null),
            supplier(xSupplierId, MdmSupplierType.THIRD_PARTY, businessId),
            supplier(ySupplierId, MdmSupplierType.THIRD_PARTY, businessId)
        );
        sskuExistenceRepository.markExistence(List.of(
            new ShopSkuKey(xSupplierId, shopSku),
            new ShopSkuKey(ySupplierId, shopSku)
        ), true);
        ShopSkuKey rootKey = new ShopSkuKey(businessId, shopSku);
        addProviderProductInfo(rootKey, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.BUSINESS);
        executor.execute();

        MappingCacheDao savedRootMapping = mappingsCacheRepository.findByIds(List.of(rootKey),
            ExpectedMappingQuality.ANY)
            .get(0);
        MappingCacheDao savedXMapping = mappingsCacheRepository.findByIds(
            List.of(new ShopSkuKey(xSupplierId, shopSku)), ExpectedMappingQuality.ANY).get(0);
        MappingCacheDao savedYMapping = mappingsCacheRepository.findByIds(
            List.of(new ShopSkuKey(ySupplierId, shopSku)), ExpectedMappingQuality.ANY).get(0);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedRootMapping).isNotNull();
            s.assertThat(savedRootMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedRootMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            Assertions.assertThat(savedXMapping).isNotNull();
            s.assertThat(savedXMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedXMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            Assertions.assertThat(savedYMapping).isNotNull();
            s.assertThat(savedYMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedYMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            s.assertThat(mappingsCacheRepository.findAll()).hasSize(3);
        });
    }

    @Test
    public void whenNewServiceMappingShouldAddMappingToAllBusinessGroup() {
        // given
        int businessId = 111;
        int xSupplierId = 222;
        int ySupplierId = 223;
        String shopSku = "offer_1";
        ShopSkuKey bizKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKeyX = new ShopSkuKey(xSupplierId, shopSku);
        ShopSkuKey serviceKeyY = new ShopSkuKey(ySupplierId, shopSku);
        mdmSupplierRepository.insertBatch(
            supplier(businessId, MdmSupplierType.BUSINESS, null),
            supplier(xSupplierId, MdmSupplierType.THIRD_PARTY, businessId),
            supplier(ySupplierId, MdmSupplierType.THIRD_PARTY, businessId)
        );
        sskuExistenceRepository.markExistence(List.of(serviceKeyX, serviceKeyY), true);
        addProviderProductInfo(serviceKeyX, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.BUSINESS);

        // when
        executor.execute();

        // then
        MappingCacheDao savedRootMapping = mappingsCacheRepository.findById(bizKey);
        MappingCacheDao savedXMapping = mappingsCacheRepository.findById(serviceKeyX);
        MappingCacheDao savedYMapping = mappingsCacheRepository.findById(serviceKeyY);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedRootMapping).isNotNull();
            s.assertThat(savedRootMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedRootMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            Assertions.assertThat(savedXMapping).isNotNull();
            s.assertThat(savedXMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedXMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            Assertions.assertThat(savedYMapping).isNotNull();
            s.assertThat(savedYMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedYMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);

            s.assertThat(mappingsCacheRepository.findAll()).hasSize(3);
        });
    }


    @Test
    public void whenUpdatedMappingShouldUpdateCache() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_2, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        MappingCacheDao savedMapping = mappingsCacheRepository.findByIds(List.of(key1), ExpectedMappingQuality.ANY)
            .get(0);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedMapping).isNotNull();
            s.assertThat(savedMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_2);
            s.assertThat(mappingsCacheRepository.findAll()).hasSize(1);
        });
    }

    @Test
    public void whenUpdatedSameMappingShouldSaveLatestVersionToCache() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));

        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key1, CATEGORY_ID_2, MSKU_ID_2, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key1, CATEGORY_ID_2, MSKU_ID_2, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key1, CATEGORY_ID_2, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        MappingCacheDao savedMapping = mappingsCacheRepository.findByIds(List.of(key1), ExpectedMappingQuality.ANY)
            .get(0);

        SoftAssertions.assertSoftly(s -> {
            Assertions.assertThat(savedMapping).isNotNull();
            s.assertThat(savedMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_2);
            s.assertThat(savedMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);
            s.assertThat(mappingsCacheRepository.findAll()).hasSize(1);
        });
    }

    @Test
    public void whenDeletionMarkReceivedMappingShouldBeDeleted() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));
        mappingsCacheRepository.insertOrUpdateAll(List.of(mappingCacheDao(key1, 111L)));

        // mskuID == 0 means mapping deletion
        addProviderProductInfo(key1, CATEGORY_ID_1, 0, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        MappingCacheDao updatedMapping = mappingsCacheRepository.findById(key1);

        Assertions.assertThat(updatedMapping).isNull();
    }

    @Test
    public void whenAdd1PMappingShouldProcessWithExternalKey() {
        ShopSkuKey key = new ShopSkuKey(32, "offer_1p");
        ShopSkuKey externalKey = new ShopSkuKey(FAKE_BERU_ID,
            RealConverter.generateSSKU(String.valueOf(key.getSupplierId()), key.getShopSku()));
        mdmSupplierRepository.insert(supplier(key.getSupplierId(), MdmSupplierType.REAL_SUPPLIER, null));
        mdmSupplierRepository.insert(supplier(externalKey.getSupplierId(), MdmSupplierType.FIRST_PARTY, null));

        addProviderProductInfo(key, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.REAL_SUPPLIER);
        executor.execute();

        MappingCacheDao savedMapping = mappingsCacheRepository.findByIds(
            List.of(externalKey), ExpectedMappingQuality.ANY).get(0);
        List<Long> mskuKeys = MdmQueueInfoBaseUtils.keys(mskuToRefreshRepository.getUnprocessedBatch(SIZE));
        List<ShopSkuKey> sskuKeys = MdmQueueInfoBaseUtils.keys(sskuToRefreshRepository.getUnprocessedBatch(SIZE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mskuKeys).containsOnly(MSKU_ID_1);
            s.assertThat(sskuKeys).containsOnly(externalKey);

            Assertions.assertThat(savedMapping).isNotNull();
            s.assertThat(savedMapping).extracting(MappingCacheDao::getCategoryId).isEqualTo(CATEGORY_ID_1);
            s.assertThat(savedMapping).extracting(MappingCacheDao::getMskuId).isEqualTo(MSKU_ID_1);
        });
    }


    @Test
    public void whenAddMappingShouldSaveStampToKeyValueStorage() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        long stamp = keyValueService.getLong(MdmProperties.MAPPING_IMPORT_TIMESTAMP_KEY, 0L);

        Assertions.assertThat(stamp).isGreaterThan(0);
    }

    @Test
    public void whenFirstRunShouldEnqueueAll() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        ShopSkuKey key2 = new ShopSkuKey(2, "offer_2");
        ShopSkuKey key3 = new ShopSkuKey(3, "offer_3");
        mdmSupplierRepository.insertBatch(
            supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null),
            supplier(key2.getSupplierId(), MdmSupplierType.THIRD_PARTY, null),
            supplier(key3.getSupplierId(), MdmSupplierType.THIRD_PARTY, null)
        );
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key2, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        addProviderProductInfo(key3, CATEGORY_ID_2, MSKU_ID_2, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        List<MdmMskuQueueInfo> mskuBatch = mskuToRefreshRepository.getUnprocessedBatch(SIZE);
        List<SskuToRefreshInfo> sskuBatch = sskuToRefreshRepository.getUnprocessedBatch(SIZE);

        List<Long> mskuKeys = MdmQueueInfoBaseUtils.keys(mskuBatch);
        List<ShopSkuKey> sskuKeys = MdmQueueInfoBaseUtils.keys(sskuBatch);

        List<MdmEnqueueReason> mskuReasons = MdmQueueInfoBaseUtils.reasons(mskuBatch);
        List<MdmEnqueueReason> sskuReasons = MdmQueueInfoBaseUtils.reasons(sskuBatch);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mskuKeys).hasSize(2);
            s.assertThat(sskuKeys).hasSize(3);
            s.assertThat(mskuKeys).containsOnly(MSKU_ID_1, MSKU_ID_2);
            s.assertThat(sskuKeys).containsOnly(key1, key2, key3);
            s.assertThat(mskuReasons).containsOnly(MdmEnqueueReason.CHANGED_MAPPING_MBOC);
            s.assertThat(sskuReasons).containsOnly(MdmEnqueueReason.CHANGED_MAPPING_MBOC);
        });
    }

    @Test
    public void whenUpdatedMappingShouldEnqueueOldMsku() {
        ShopSkuKey key1 = new ShopSkuKey(1, "offer_1");
        mdmSupplierRepository.insert(supplier(key1.getSupplierId(), MdmSupplierType.THIRD_PARTY, null));
        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_1, MboMappings.MappingType.SUPPLIER);
        executor.execute();
        processRefreshQueues(SIZE);

        addProviderProductInfo(key1, CATEGORY_ID_1, MSKU_ID_2, MboMappings.MappingType.SUPPLIER);
        executor.execute();

        List<Long> mskuKeys = MdmQueueInfoBaseUtils.keys(mskuToRefreshRepository.getUnprocessedBatch(SIZE));
        List<ShopSkuKey> sskuKeys = MdmQueueInfoBaseUtils.keys(sskuToRefreshRepository.getUnprocessedBatch(SIZE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mskuKeys).containsExactlyInAnyOrder(MSKU_ID_1, MSKU_ID_2);
            s.assertThat(sskuKeys).containsOnly(key1);
        });
    }

    private void processRefreshQueues(int count) {
        mskuToRefreshRepository.markProcessed(
            MdmQueueInfoBaseUtils.ids(mskuToRefreshRepository.getUnprocessedBatch(count))
        );
        sskuToRefreshRepository.markProcessed(
            MdmQueueInfoBaseUtils.ids(sskuToRefreshRepository.getUnprocessedBatch(count))
        );
    }

    /**
     * @noinspection SameParameterValue
     */
    private void addProviderProductInfo(ShopSkuKey shopSkuKey,
                                        int categoryId,
                                        long mskuId,
                                        MboMappings.MappingType mappingType) {
        MboMappings.ProviderProductInfo.Builder info = MboMappings.ProviderProductInfo.newBuilder()
            .setShopId(shopSkuKey.getSupplierId())
            .setShopSkuId(shopSkuKey.getShopSku())
            .setTitle("best offer " + shopSkuKey.getShopSku())
            .setShopCategoryName("shopCategoryName")
            .setMarketCategoryId(categoryId)
            .setMarketModelId(categoryId * 1000 + (mskuId % 1000))
            .setMarketSkuId(mskuId)
            .setOfferUpdateTs(Instant.now().toEpochMilli())
            .setMappingType(mappingType);

        offers.removeIf(o ->
            Objects.equals(o.getShopSkuId(), info.getShopSkuId()) && o.getShopId() == info.getShopId());

        offers.add(info.setUploadToYtStamp(offers.size() + 1).build());

        if (mappingType == MboMappings.MappingType.REAL_SUPPLIER) {
            info.setMappingType(MboMappings.MappingType.SUPPLIER);
            info.setShopId(FAKE_BERU_ID);
            info.setShopSkuId(RealConverter.generateSSKU(String.valueOf(shopSkuKey.getSupplierId()),
                shopSkuKey.getShopSku()));
            offers.add(info.setUploadToYtStamp(offers.size() + 1).build());
        }

    }

    private List<MboMappings.ProviderProductInfo> getProviderProductInfos(long fromStamp, long limit) {
        // offers всегда отсортирована по Comparator.comparing(MboMappings.ProviderProductInfo::getUploadToYtStamp)
        return offers.stream()
            .filter(m -> m.getUploadToYtStamp() >= fromStamp)
            .limit(limit)
            .collect(Collectors.toList());
    }

    private MdmSupplier supplier(int id, MdmSupplierType type, Integer business) {
        MdmSupplier s = new MdmSupplier();
        s.setId(id);
        s.setType(type);
        if (business != null) {
            s.setBusinessId(business);
        }
        s.setBusinessEnabled(true);
        return s;
    }

    private static MappingCacheDao mappingCacheDao(ShopSkuKey shopSkuKey, long mskuId) {
        return new MappingCacheDao()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(CATEGORY_ID_1)
            .setVersionTimestamp(Instant.ofEpochSecond(1L));
    }
}
