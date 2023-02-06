package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.jooq.repo.Sorting;
import ru.yandex.market.mboc.common.config.yt.YtOfferMappingTablesConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MappingSkuType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.OfferQueueOfferMappingYt;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferQueueOfferMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.ModifyRowsRequestMock;
import ru.yandex.market.mboc.common.utils.RealConverter;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;

public class UploadOfferMappingsToYtExecutorTest extends BaseDbTestClass {
    private static final Long MSKU_ID = 10000L;
    private static final Long MSKU_ID_1 = 10001L;

    private static final int SUPPLIER_ID = 1;

    @Resource
    private OfferRepository offerRepository;
    @Resource
    private SupplierRepository supplierRepository;
    @Resource
    private OfferQueueOfferMappingRepository queueRepository;
    @Value("${mboc.beru.businessId}")
    private int beruBusinessId;

    private UploadOfferMappingsToYtExecutor executor;

    private ListMultimap<Boolean, ModifyRowsRequest> requestsHolder; //true - for mapping, false - for reversed

    private Optional<RuntimeException> exceptionHolder = Optional.empty();

    @Before
    public void setUp() {
        YtTableRpcApi mappingRpcApi = Mockito.mock(YtTableRpcApi.class);
        Mockito.when(mappingRpcApi.createModifyRowRequest()).thenAnswer(invocation -> new ModifyRowsRequestMock());
        YtTableRpcApi offerBySkuIdRpcApi = Mockito.mock(YtTableRpcApi.class);
        Mockito.when(offerBySkuIdRpcApi.createModifyRowRequest()).thenAnswer(invocation -> new ModifyRowsRequestMock());
        requestsHolder = ArrayListMultimap.create();
        SupplierService supplierService = new SupplierService(supplierRepository);
        executor = new UploadOfferMappingsToYtExecutor(null, queueRepository, null,
            null, null, supplierService, beruBusinessId) {
            @Override
            public void ensureYtInitialized() {
                ytMappingTableRpcApi = mappingRpcApi;
                ytOfferBySkuIdTableRpcApi = offerBySkuIdRpcApi;
            }

            @Override
            protected void executeRequest(ModifyRowsRequest request, YtTableRpcApi ytTableRpcApi) {
                if (exceptionHolder.isPresent()) {
                    throw exceptionHolder.get();
                }
                requestsHolder.put(ytTableRpcApi == mappingRpcApi, request);
            }
        };

        supplierRepository.insert(
            new Supplier().setId(SUPPLIER_ID).setName("supplier1").setType(MbocSupplierType.BUSINESS)
        );
    }

    @Test(expected = RuntimeException.class)
    public void testFailed() {
        try {
            offerRepository.insertOffer(offer("of1", MSKU_ID));
            Assertions.assertThat(queueRepository.findAll()).hasSize(1);
            exceptionHolder = Optional.of(new RuntimeException("Q"));
            executor.execute();
        } finally {
            Assertions.assertThat(queueRepository.findAll()).hasSize(1);
            Assertions.assertThat(queueRepository.findAll().get(0).getLastError())
                .isNotNull();
        }
    }

    @Test
    public void testOK() {
        offerRepository.insertOffer(offer("of1", MSKU_ID));

        Offer offer = offerRepository.findAll().get(0);
        Long approvedTs1 = offer.getApprovedSkuMapping().getInstant().toEpochMilli();
        offer.updateApprovedSkuMapping(
            new Offer.Mapping(MSKU_ID_1, LocalDateTime.now(), Offer.SkuType.MARKET),
            Offer.MappingConfidence.CONTENT
        );
        Long approvedTs2 = offer.getApprovedSkuMapping().getInstant().toEpochMilli();
        offerRepository.updateOffers(offer);

        var queue = queueRepository.find(null,
            Sorting.asc(OfferQueueOfferMappingRepository.SortBy.ID),
            OffsetFilter.all());

        Assertions.assertThat(queue)
            .usingElementComparatorIgnoringFields("id", "enqueuedTs")
            .containsExactly(
                new OfferQueueOfferMappingYt()
                    .setOfferId(offer.getId())
                    .setBusinessId(SUPPLIER_ID)
                    .setShopSku(offer.getShopSku())
                    .setNewMskuId(MSKU_ID)
                    .setAttempt(0)
                    .setApprovedMappingTs(approvedTs1),
                new OfferQueueOfferMappingYt()
                    .setOfferId(offer.getId())
                    .setBusinessId(SUPPLIER_ID)
                    .setShopSku(offer.getShopSku())
                    .setOldMskuId(MSKU_ID)
                    .setNewMskuId(MSKU_ID_1)
                    .setAttempt(0)
                    .setApprovedMappingTs(approvedTs2)
                    .setSkuType(MappingSkuType.MARKET)
            );

        executor.execute();

        List<ModifyRowsRequest> mappingRequests = requestsHolder.get(true);
        Assertions.assertThat(mappingRequests).hasSize(1);

        Map<String, Object> expected1 = new HashMap<>(
            Map.of(YtOfferMappingTablesConfig.SHOP_SKU, offer.getShopSku(),
                YtOfferMappingTablesConfig.BUSINESS_ID, offer.getBusinessId(),
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_ID, MSKU_ID,
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_TS, approvedTs1)
        );
        Map<String, Object> expected2 = new HashMap<>(
            Map.of(YtOfferMappingTablesConfig.SHOP_SKU, offer.getShopSku(),
                YtOfferMappingTablesConfig.BUSINESS_ID, offer.getBusinessId(),
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_ID, MSKU_ID_1,
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_SKU_TYPE, MappingSkuType.MARKET.name(),
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_TS, approvedTs2)
        );
        ModifyRowsRequestMock modifyRowsRequestMock = (ModifyRowsRequestMock) mappingRequests.get(0);
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation())
            .containsExactly(expected1, expected2);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();

        List<ModifyRowsRequest> bySkuIdRequests = requestsHolder.get(false);
        Assertions.assertThat(bySkuIdRequests).hasSize(1);
        modifyRowsRequestMock = (ModifyRowsRequestMock) bySkuIdRequests.get(0);
        expected1.put(YtOfferMappingTablesConfig.ID, offer.getId());
        expected2.put(YtOfferMappingTablesConfig.ID, offer.getId());
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation())
            .containsExactly(expected1, expected2);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .containsExactly(new HashMap<>(
                Map.of(YtOfferMappingTablesConfig.ID, offer.getId(),
                    YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_ID, MSKU_ID)));
    }

    @Test
    public void test1PKeyConversion() {
        var supplier = supplierRepository.findById(SUPPLIER_ID);
        supplier.setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("real_id");
        supplierRepository.update(supplier);

        offerRepository.insertOffer(offer("of1", MSKU_ID));

        Offer offer = offerRepository.findAll().get(0);
        Long approvedTs = offer.getApprovedSkuMapping().getInstant().toEpochMilli();

        var queue = queueRepository.find(null,
            Sorting.asc(OfferQueueOfferMappingRepository.SortBy.ID),
            OffsetFilter.all());

        Assertions.assertThat(queue)
            .usingElementComparatorIgnoringFields("id", "enqueuedTs")
            .containsExactly(
                new OfferQueueOfferMappingYt()
                    .setOfferId(offer.getId())
                    .setBusinessId(SUPPLIER_ID)
                    .setShopSku(offer.getShopSku())
                    .setNewMskuId(MSKU_ID)
                    .setAttempt(0)
                    .setApprovedMappingTs(approvedTs)
            );

        executor.execute();

        List<ModifyRowsRequest> mappingRequests = requestsHolder.get(true);
        Assertions.assertThat(mappingRequests).hasSize(1);

        String expectedShopSku = RealConverter.generateSSKU(supplier.getRealSupplierId(), offer.getShopSku());

        Map<String, Object> expected = new HashMap<>(Map.of(
            YtOfferMappingTablesConfig.SHOP_SKU, expectedShopSku,
            YtOfferMappingTablesConfig.BUSINESS_ID, beruBusinessId,
            YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_ID, MSKU_ID,
            YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_TS, approvedTs)
        );

        ModifyRowsRequestMock modifyRowsRequestMock = (ModifyRowsRequestMock) mappingRequests.get(0);
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation()).containsExactly(expected);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();

        List<ModifyRowsRequest> bySkuIdRequests = requestsHolder.get(false);
        Assertions.assertThat(bySkuIdRequests).hasSize(1);
        modifyRowsRequestMock = (ModifyRowsRequestMock) bySkuIdRequests.get(0);
        expected.put(YtOfferMappingTablesConfig.ID, offer.getId());
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation()).containsExactly(expected);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testDeletion() {
        offerRepository.insertOffer(offer("of1", MSKU_ID));
        queueRepository.deleteAll();
        Offer offer = offerRepository.findAll().get(0);
        offer.updateApprovedSkuMapping(null);
        offerRepository.updateOffers(offer);
        Assertions.assertThat(queueRepository.findAll())
            .map(x -> x.setId(null).setEnqueuedTs(null))
            .containsExactlyInAnyOrder(
                new OfferQueueOfferMappingYt()
                    .setOfferId(offer.getId())
                    .setBusinessId(SUPPLIER_ID)
                    .setShopSku(offer.getShopSku())
                    .setOldMskuId(MSKU_ID)
                    .setAttempt(0)
            );
        executor.execute();
        List<ModifyRowsRequest> mappingRequests = requestsHolder.get(true);
        Assertions.assertThat(mappingRequests).hasSize(1);

        Map<String, Object> deletionKey = new HashMap<>(
            Map.of(YtOfferMappingTablesConfig.SHOP_SKU, offer.getShopSku(),
                YtOfferMappingTablesConfig.BUSINESS_ID, offer.getBusinessId())
        );
        Map<String, Object> deletionKey2 = new HashMap<>(
            Map.of(YtOfferMappingTablesConfig.ID, offer.getId(),
                YtOfferMappingTablesConfig.APPROVED_SKU_MAPPING_ID, MSKU_ID)
        );
        ModifyRowsRequestMock modifyRowsRequestMock = (ModifyRowsRequestMock) mappingRequests.get(0);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .containsExactly(deletionKey);
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation()).isEmpty();

        List<ModifyRowsRequest> bySkuIdRequests = requestsHolder.get(false);
        Assertions.assertThat(bySkuIdRequests).hasSize(1);
        modifyRowsRequestMock = (ModifyRowsRequestMock) bySkuIdRequests.get(0);
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .containsExactly(deletionKey2);
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getUpdation()).isEmpty();
    }

    private Offer offer(String shopSku, Long mskuId) {
        Offer offer = new Offer()
            .setBusinessId(SUPPLIER_ID)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setShopCategoryName("Category")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .setServiceOffers(List.of(new Offer.ServiceOffer(
                    SUPPLIER_ID,
                    MbocSupplierType.THIRD_PARTY,
                    Offer.AcceptanceStatus.OK
                )
            ));
        if (mskuId != null) {
            offer.setApprovedSkuMappingInternal(new Offer.Mapping(mskuId, LocalDateTime.now()))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        }
        return offer;
    }
}
