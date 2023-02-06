package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mboc.common.ReplicaCluster;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.offers.upload.YtOfferUploadQueueService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.yt.YtWriter;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.ProviderProductInfo;
import ru.yandex.market.mboc.http.MboMappings.ProviderProductInfoResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createNonProcessedOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createUploadToYtOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createUploadedOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createWhiteOffer;

/**
 * Тесты {@link UploadApprovedMappingsYtExecutor}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UploadApprovedMappingsYtExecutorTest extends BaseDbTestClass {

    private static final long CATEGORY_ID = 20;
    private static final int BERU_ID = 465852;
    private static final int SEED = 42;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .build();
    private static long nextStamp = 0;
    private UploadApprovedMappingsYtExecutor uploadApprovedMappingsYtExecutor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private YtOfferUploadQueueService ytOfferUploadQueueService;
    private Map<ReplicaCluster, UnstableInit<YtWriter>> ytWriterMap;
    private Offer offer;
    private Offer nonApprovedOffer;
    private Offer uploadedOffer;

    private Supplier supplier1;
    private Supplier supplier2;

    @Before
    public void setUp() throws Exception {
        ytWriterMap = new HashMap<>();
        for (ReplicaCluster cluster : ReplicaCluster.getAllMdmClusters()) {
            ytWriterMap.put(cluster, UnstableInit.simple(Mockito.mock(YtWriter.class)));
        }
        uploadApprovedMappingsYtExecutor = new UploadApprovedMappingsYtExecutor(
                offerRepository,
                supplierRepository,
                transactionHelper,
                ytWriterMap,
                ytOfferUploadQueueService,
                BERU_ID,
                true
        );

        supplier1 = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier1);
        supplier2 = new Supplier(2, "Supplier #2", null, null);
        supplierRepository.insert(supplier2);
        supplierRepository.insert(new Supplier(3, "Supplier #3", null, null));
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test supplier"));

        offer = createOffer(supplier1);
        nonApprovedOffer = createNonProcessedOffer(supplier1);
        uploadedOffer = createUploadedOffer();
    }

    private static List<ProviderProductInfo> captureYtStoreRequest(YtWriter ytWriter, int number) {
        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytWriter, times(number)).store(requestCaptor.capture());
        return requestCaptor.getAllValues().stream().flatMap(l -> l.stream()).collect(Collectors.toList());
    }

    private void verityNoYtInteractions() {
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, never()).store(Mockito.any());
            Mockito.verify(ytWriter, never()).storeDiffsAndDocs(Mockito.any(), Mockito.any());
        }
    }

    private void mockYtWriterToAnswerOk() {
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.when(value.get().store(Mockito.any()))
                .thenAnswer(__ -> ProviderProductInfoResponse.newBuilder()
                    .setStatus(ProviderProductInfoResponse.Status.OK)
                    .build()
                );
        }
    }

    @Test
    public void testSuccessScenario() throws Exception {
        // arrange
        insertOfferKeepStamp(offer);
        insertOfferKeepStamp(nonApprovedOffer);
        // Normally non-approved offers won't be added to queue, but it can happen that offer
        // became non-approved while in queue. Let's check for that too
        ytOfferUploadQueueService.enqueueByIds(List.of(nonApprovedOffer.getId()), LocalDateTime.now());
        insertOfferKeepStamp(uploadedOffer);
        Offer newNonApprovedOffer = offerRepository.getOfferById(nonApprovedOffer.getId());
        Offer newUploadedOffer = offerRepository.getOfferById(uploadedOffer.getId());

        mockYtWriterToAnswerOk();

        assertThat(offer).matches(this::needsUploadToYt);
        assertThat(nonApprovedOffer).matches(this::needsUploadToYt);
        assertThat(uploadedOffer).matches(this::needsUploadToYt);

        // act
        uploadApprovedMappingsYtExecutor.execute();

        // assert
        for (UnstableInit<YtWriter> ytWriter : ytWriterMap.values()) {
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter.get(), 1);
            assertThat(infos).extracting(ProviderProductInfo::getShopId).containsExactly(1);
        }

        // в БД должен только записаться первый оффер
        Offer offerFromDb = offerRepository.getOfferById(this.offer.getId());
        assertThat(offerFromDb).matches(this::notNeedsUploadToYt);

        assertThat(offerFromDb.getLastVersion()).isGreaterThan(offer.getLastVersion());

        // остальные офферы не должны никак измениться
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId()))
                .usingRecursiveComparison().isEqualTo(newNonApprovedOffer);
        assertThat(offerRepository.getOfferById(uploadedOffer.getId()))
                .usingRecursiveComparison().isEqualTo(newUploadedOffer);

        assertThat(offer).matches(this::notNeedsUploadToYt);
        assertThat(uploadedOffer).matches(this::notNeedsUploadToYt);
        assertThat(nonApprovedOffer).matches(this::needsUploadToYt);
    }

    @Test
    public void testNoRequestsIfNoApprovedOffers() throws Exception {
        // Normally non-approved offers won't be added to queue, but it can happen that offer
        // became non-approved while in queue. Let's check for that too
        offerRepository.insertOffer(nonApprovedOffer);
        ytOfferUploadQueueService.enqueueByIds(List.of(nonApprovedOffer.getId()), LocalDateTime.now());

        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId())).matches(this::needsUploadToYt);

        // запускаем
        uploadApprovedMappingsYtExecutor.execute();

        // проверяем
        verityNoYtInteractions();
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId())).matches(this::needsUploadToYt);
    }

    @Test
    public void testNoRequestsIfNoNotUploadedOffers() throws Exception {
        assertThat(ytOfferUploadQueueService.getForUpload(10)).isEmpty();
        // запускаем
        uploadApprovedMappingsYtExecutor.execute();
        // проверяем
        verityNoYtInteractions();
    }

    @Test
    public void testWhiteOffer() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.update(supplier1);
        Offer whiteOffer = createWhiteOffer(supplier1);
        whiteOffer.setSupplierSkuMapping(
            new Offer.Mapping(
                11,
                LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
            )
        );

        insertOfferKeepStamp(whiteOffer);
        assertThat(whiteOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();
        uploadApprovedMappingsYtExecutor.execute();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, times(1)).store(Mockito.any());
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            Assertions.assertThat(infos.get(0).getMappingType()).isEqualTo(MboMappings.MappingType.PRICE_COMPARISION);
        }

        assertThat(whiteOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testBlueMappedCategoryId() {
        Offer blueOffer = createOffer(supplier1);
        blueOffer.setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(1L)
            .setApprovedSkuMappingInternal(null)
            .setModelId(null);

        insertOfferKeepStamp(blueOffer);
        assertThat(blueOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, times(1)).store(Mockito.any());
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            Assertions.assertThat(infos.get(0).getMappingType()).isEqualTo(MboMappings.MappingType.SUPPLIER);
        }
        assertThat(blueOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testWhiteMappedCategoryId() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.update(supplier1);
        Offer whiteOffer = createWhiteOffer(supplier1);
        whiteOffer.setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER)
            .setMappedCategoryId(1L)
            .setApprovedSkuMappingInternal(null)
            .setModelId(null);

        insertOfferKeepStamp(whiteOffer);
        assertThat(whiteOffer).matches(this::needsUploadToYt);
        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, times(1)).store(Mockito.any());
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            Assertions.assertThat(infos.get(0).getMappingType()).isEqualTo(MboMappings.MappingType.PRICE_COMPARISION);
        }
        assertThat(whiteOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testWhiteOfferCategoryId() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.update(supplier1);
        Offer whiteOffer = createWhiteOffer(supplier1);
        whiteOffer.setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER);

        insertOfferKeepStamp(whiteOffer);
        assertThat(whiteOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, times(1)).store(Mockito.any());
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            Assertions.assertThat(infos.get(0).getMappingType()).isEqualTo(MboMappings.MappingType.PRICE_COMPARISION);
        }
        assertThat(whiteOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testWhiteOfferModelId() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.update(supplier1);
        Offer whiteOffer = createWhiteOffer(supplier1);
        whiteOffer.setModelId(1L);

        insertOfferKeepStamp(whiteOffer);
        assertThat(whiteOffer).matches(this::needsUploadToYt);
        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.verify(ytWriter, times(1)).store(Mockito.any());
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            Assertions.assertThat(infos.get(0).getMappingType()).isEqualTo(MboMappings.MappingType.PRICE_COMPARISION);
        }

        assertThat(whiteOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testNegativeScenario() {
        // arrange
        insertOfferKeepStamp(offer);
        insertOfferKeepStamp(nonApprovedOffer);
        // Normally non-approved offers won't be added to queue, but it can happen that offer
        // became non-approved while in queue. Let's check for that too
        ytOfferUploadQueueService.enqueueByIds(List.of(nonApprovedOffer.getId()), LocalDateTime.now());
        insertOfferKeepStamp(uploadedOffer);

        assertThat(offer).matches(this::needsUploadToYt);
        assertThat(nonApprovedOffer).matches(this::needsUploadToYt);
        assertThat(uploadedOffer).matches(this::needsUploadToYt);

        Offer newNonApprovedOffer = offerRepository.getOfferById(nonApprovedOffer.getId());
        Offer newUploadedOffer = offerRepository.getOfferById(uploadedOffer.getId());

        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            Mockito.when(ytWriter.store(Mockito.any()))
                .thenAnswer(__ -> ProviderProductInfoResponse.newBuilder()
                    .setStatus(ProviderProductInfoResponse.Status.ERROR)
                    .setMessage("Custom unique error message one")
                    .build()
                );
        }

        // act
        Assertions.assertThatThrownBy(() -> {
            uploadApprovedMappingsYtExecutor.execute();
        }).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Action 'Process offers batch' failed");

        // Should set UploadToYtStamp
        Offer repositoryOffer = offerRepository.getOfferById(offer.getId());

        // Upload unsuccessful - offers should not be removed from the queue
        assertThat(newNonApprovedOffer).matches(this::needsUploadToYt);
        assertThat(newUploadedOffer).matches(this::needsUploadToYt);
        assertThat(repositoryOffer).matches(this::needsUploadToYt);
        assertThat(repositoryOffer.getUploadToYtStamp()).isNotNull();

        // This offers should not be changed at all
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId()))
            .usingRecursiveComparison().isEqualTo(newNonApprovedOffer);
        assertThat(offerRepository.getOfferById(uploadedOffer.getId()))
            .usingRecursiveComparison().isEqualTo(newUploadedOffer);
    }

    @Test
    public void testSuccessUploadIfUcOnSecondCallReturnTrue() throws Exception {
        insertOfferKeepStamp(offer);
        assertThat(offer).matches(this::needsUploadToYt);

        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.when(value.get().store(Mockito.any()))
                .thenAnswer(__ -> ProviderProductInfoResponse.newBuilder()
                    .setStatus(ProviderProductInfoResponse.Status.ERROR)
                    .setMessage("Custom unique error message two")
                    .build()
                )
                .thenAnswer(__ -> ProviderProductInfoResponse.newBuilder()
                    .setStatus(ProviderProductInfoResponse.Status.OK)
                    .build()
                );
        }


        // запускаем
        uploadApprovedMappingsYtExecutor.execute();

        // проверяем
        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(2)).store(requestCaptor.capture());
        }
        assertThat(requestCaptor.getAllValues().get(1)).hasSize(1);
        assertThat(offer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testSuccessUploadIfUcOnSecondCallReturnTrue2() throws Exception {
        insertOfferKeepStamp(offer);

        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.when(value.get().store(Mockito.any()))
                .thenThrow(new ServiceException("Test exception"))
                .thenAnswer(__ -> ProviderProductInfoResponse.newBuilder()
                    .setStatus(ProviderProductInfoResponse.Status.OK)
                    .build()
                );
        }

        // запускаем
        uploadApprovedMappingsYtExecutor.execute();

        // проверяем
        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(2)).store(requestCaptor.capture());
        }
        assertThat(requestCaptor.getAllValues().get(1)).hasSize(1);
    }

    @Test
    public void testRequestInfoCorrectlySet() throws Exception {
        insertOfferKeepStamp(offer);
        assertThat(offer).matches(this::needsUploadToYt);
        offer = offerRepository.getOfferById(offer.getId());

        mockYtWriterToAnswerOk();


        // запускаем
        uploadApprovedMappingsYtExecutor.execute();

        // проверяем
        assertThat(offer).matches(this::notNeedsUploadToYt);

        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            YtWriter ytWriter = value.get();
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter, 1);
            assertThat(infos).hasSize(1);

            ProviderProductInfo info = infos.get(0);

            assertThat(info.getShopId()).isEqualTo(offer.getBusinessId());
            assertThat(info.getShopSkuId()).isEqualTo(offer.getShopSku());
            assertThat(info.getShopCategoryName()).isEqualTo(offer.getShopCategoryName());
            assertThat(info.getMarketSkuId()).isEqualTo(offer.getContentSkuMapping().getMappingId());

            if (offer.getBarCode() != null) {
                assertThat(info.getBarcodeList()).containsExactly(offer.getBarCode());
            } else {
                assertThat(info.getBarcodeList()).isEmpty();
            }
            if (offer.extractOfferContent().containsExtraShopField(ExcelHeaders.DESCRIPTION.getTitle())) {
                assertThat(info.getDescription())
                    .isEqualTo(offer.extractOfferContent().getDescription());
            } else {
                assertThat(info.hasDescription()).isFalse();
            }
        }
    }

    @Test
    public void testUploadToYtOrder() {
        assertThat(ytOfferUploadQueueService.getForUpload(1000)).isEmpty();
        ArrayList<Long> uploadToYtStamps = new ArrayList<>(1_000);
        for (long i = 0; i < 1_000; i++) {
            uploadToYtStamps.add(i);
        }
        Collections.shuffle(uploadToYtStamps, RANDOM); // a permutation of 0...N-1
        for (int i = 0; i < 1_000; i++) {
            Offer offerForUpload = createUploadToYtOffer(1001, Offer.MappingType.APPROVED,
                Offer.MappingDestination.BLUE, OfferTestUtils.simpleSupplier(), "12345" + i
            );
            offerForUpload.setUploadToYtStamp(uploadToYtStamps.get(i));
            insertOfferKeepStamp(offerForUpload);
        }
        assertThat(ytOfferUploadQueueService.getForUpload(1000)).hasSize(1000);

        List<Long> stamps = new ArrayList<>();
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.when(value.get().store(Mockito.any()))
                .thenAnswer(p -> {
                    List<ProviderProductInfo> request = (List<ProviderProductInfo>) p.getArguments()[0];
                    stamps.addAll(
                        request.stream()
                            .map(ProviderProductInfo::getUploadToYtStamp)
                            .collect(Collectors.toList())
                    );
                    return ProviderProductInfoResponse.newBuilder()
                        .setStatus(ProviderProductInfoResponse.Status.OK)
                        .build();
                });
        }


        uploadApprovedMappingsYtExecutor.execute();

        assertThat(ytOfferUploadQueueService.getForUpload(1000)).isEmpty();

        Assertions.assertThat(stamps).hasSize(uploadToYtStamps.size() * ytWriterMap.size());
        // каждое значение пришло в stamps ровно по одному разу на каждый кластер
        Assertions.assertThat(stamps.stream().collect(Collectors.toMap(x -> x, __ -> 1, (x, y) -> x + y)).values())
            .containsOnly(ytWriterMap.size());
        Assertions.assertThat(stamps).hasSameElementsAs(uploadToYtStamps);
    }

    @Test
    public void testDuplicateShopSkuInErp() {
        Supplier supplier1 = supplierRepository.findById(1)
            .setRealSupplierId("REAL")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        Supplier supplier2 = supplierRepository.findById(2)
            .setRealSupplierId("REAL2")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        supplierRepository.update(supplier1);
        supplierRepository.update(supplier2);

        Offer offer1 = createOffer(1001, supplier1).setShopSku("123456");
        Offer offer2 = createOffer(1001, supplier2).setShopSku("123456");

        insertOfferKeepStamp(offer1);
        insertOfferKeepStamp(offer2);

        assertThat(offerRepository.getOfferById(offer1.getId())).matches(this::needsUploadToYt);
        assertThat(offerRepository.getOfferById(offer2.getId())).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();

        // First assertion: statement above doesn't fail.

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(1)).store(requestCaptor.capture());
        }

        List<ProviderProductInfo> request = requestCaptor.getAllValues().get(0);
        assertThat(request).hasSize(4); // Both 1P and 3P copies are written x2 offers
        assertThat(request).extracting(r -> new ShopSkuKey(r.getShopId(), r.getShopSkuId()))
            .containsExactlyInAnyOrder(
                new ShopSkuKey(1, "123456"),
                new ShopSkuKey(2, "123456"),
                new ShopSkuKey(BERU_ID, "REAL.123456"),
                new ShopSkuKey(BERU_ID, "REAL2.123456")
            );

        assertThat(offerRepository.getOfferById(offer1.getId())).matches(this::notNeedsUploadToYt);
        assertThat(offerRepository.getOfferById(offer2.getId())).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testUploadFlags() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("tms-offers/upload-to-yt-offers.yml");
        insertOffersKeepStamps(offers);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();

        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(1)).store(requestCaptor.capture());
        }

        List<ProviderProductInfo> first = requestCaptor.getAllValues().get(0);
        assertEquals(3, first.size());
        assertEquals("sku4", first.get(0).getShopSkuId());
        assertEquals(101010L, first.get(0).getMarketSkuId());
        assertEquals("sku3", first.get(1).getShopSkuId());
        assertEquals(101010L, first.get(1).getMarketSkuId());
        assertEquals("sku5", first.get(2).getShopSkuId());
        assertEquals(101010L, first.get(2).getMarketSkuId());

        Map<Long, Offer> byId = offerRepository.findAll().stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        for (long id : new long[]{3, 5}) {
            assertThat(byId.get(id)).matches(this::notNeedsUploadToYt);
        }
    }

    @Test
    public void testUploadSupplierMappingDelete() {
        Offer someOffer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setSupplierSkuMapping(OfferTestUtils.mapping(0))
            .approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.PARTNER_SELF)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setUploadToYtStamp(1L);

        insertOfferKeepStamp(someOffer);
        assertThat(someOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();

        Offer updated = offerRepository.getOfferById(someOffer.getId());
        assertThat(someOffer).matches(this::notNeedsUploadToYt);
        assertEquals(Long.valueOf(CATEGORY_ID), updated.getCategoryId());
        assertEquals(0, updated.getApprovedSkuMapping().getMappingId());
    }

    @Test
    public void testMappingUploadedForFMCGSupplierType() {
        // arrange
        Supplier fmcgSupplier = OfferTestUtils.fmcgSupplier();
        Offer fmcgOffer = createOffer(1, fmcgSupplier);
        supplierRepository.insert(fmcgSupplier);
        insertOfferKeepStamp(fmcgOffer);
        assertThat(fmcgOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        // act
        uploadApprovedMappingsYtExecutor.execute();

        // assert
        for (UnstableInit<YtWriter> ytWriter : ytWriterMap.values()) {
            List<ProviderProductInfo> infos = captureYtStoreRequest(ytWriter.get(), 1);
            assertThat(infos).extracting(ProviderProductInfo::getShopId).containsExactly(fmcgSupplier.getId());
        }

        Offer offerFromDb = offerRepository.getOfferById(fmcgOffer.getId());
        assertThat(offerFromDb).matches(this::notNeedsUploadToYt);
        assertThat(offerFromDb.getLastVersion()).isGreaterThan(fmcgOffer.getLastVersion());
    }

    @Test
    public void testOfferDuplicationBusinessSupplier() {
        Supplier bizSupplier = OfferTestUtils.businessSupplier();
        int businessId = bizSupplier.getId();

        Supplier supplier1p = new Supplier(businessId + 1, "biz child 1")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("real")
            .setBusinessId(businessId);
        Supplier supplier3p = new Supplier(businessId + 2, "biz child 2")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(businessId);
        Supplier supplier3p2 = new Supplier(businessId + 3, "biz child 3")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(businessId);

        supplierRepository.insertBatch(bizSupplier, supplier1p, supplier3p, supplier3p2);

        Offer bizOffer = createUploadToYtOffer(
            1,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            bizSupplier,
            "ssku1")
            .addNewServiceOfferIfNotExistsForTests(supplier1p)
            .addNewServiceOfferIfNotExistsForTests(supplier3p)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer offer1p = createUploadToYtOffer(
            2,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            supplier1p,
            "ssku2");
        Offer offer3p = createUploadToYtOffer(
            3,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            supplier3p,
            "ssku3");
        insertOffersKeepStamps(bizOffer, offer1p, offer3p);

        assertThat(bizOffer).matches(this::needsUploadToYt);
        assertThat(offer1p).matches(this::needsUploadToYt);
        assertThat(offer3p).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();


        uploadApprovedMappingsYtExecutor.execute();

        assertThat(bizOffer).matches(this::notNeedsUploadToYt);
        assertThat(offer1p).matches(this::notNeedsUploadToYt);
        assertThat(offer3p).matches(this::notNeedsUploadToYt);

        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(1)).store(requestCaptor.capture());
        }

        List<ProviderProductInfo> request = requestCaptor.getAllValues().get(0);
        assertThat(request).hasSize(7);
        assertThat(request).extracting(r -> new ShopSkuKey(r.getShopId(), r.getShopSkuId()))
            .containsExactlyInAnyOrder(
                new ShopSkuKey(businessId, "ssku1"),
                new ShopSkuKey(businessId + 1, "ssku1"),
                new ShopSkuKey(businessId + 1, "ssku2"),
                new ShopSkuKey(BERU_ID, "real.ssku1"),
                new ShopSkuKey(BERU_ID, "real.ssku2"),
                new ShopSkuKey(businessId + 2, "ssku1"),
                new ShopSkuKey(businessId + 2, "ssku3")
            );
    }

    @Test
    public void testOfferMappedToBusinessSupplier() {
        Supplier bizSupplier = OfferTestUtils.businessSupplier();
        supplierRepository.insert(bizSupplier);

        Offer bizOffer = createUploadToYtOffer(
            1,
            Offer.MappingType.APPROVED,
            Offer.MappingDestination.BLUE,
            bizSupplier,
            "ssku1");
        insertOfferKeepStamp(bizOffer);
        assertThat(bizOffer).matches(this::needsUploadToYt);

        mockYtWriterToAnswerOk();

        uploadApprovedMappingsYtExecutor.execute();

        ArgumentCaptor<List<ProviderProductInfo>> requestCaptor = ArgumentCaptor.forClass(List.class);
        for (UnstableInit<YtWriter> value : ytWriterMap.values()) {
            Mockito.verify(value.get(), times(1)).store(requestCaptor.capture());
        }

        List<ProviderProductInfo> request = requestCaptor.getAllValues().get(0);
        assertThat(request).hasSize(1);
        ProviderProductInfo providerProductInfo = request.get(0);
        assertEquals(MboMappings.MappingType.BUSINESS, providerProductInfo.getMappingType());
        assertEquals(providerProductInfo.getShopId(), bizSupplier.getId());
        assertEquals(providerProductInfo.getShopSkuId(), "ssku1");
        assertThat(bizOffer).matches(this::notNeedsUploadToYt);
    }

    @Test
    public void testNoRequestsIfOnlyFastSkuOffers() {
        Offer.Mapping approvedSkuMapping = offer.getApprovedSkuMapping();
        Offer.Mapping fastMapping = new Offer.Mapping(approvedSkuMapping.getMappingId(), LocalDateTime.now(),
            Offer.SkuType.FAST_SKU);
        offer.setApprovedSkuMappingInternal(fastMapping);
        insertOfferKeepStamp(offer);
        ytOfferUploadQueueService.enqueueByIds(List.of(offer.getId()), LocalDateTime.now());

        assertThat(offerRepository.getOfferById(offer.getId())).matches(this::needsUploadToYt);

        // запускаем
        uploadApprovedMappingsYtExecutor.execute();

        // проверяем
        verityNoYtInteractions();
        assertThat(offerRepository.getOfferById(offer.getId())).matches(this::notNeedsUploadToYt);
    }

    private boolean notNeedsUploadToYt(Offer offer) {
        return !needsUploadToYt(offer);
    }

    private boolean notNeedsUploadToYt(Long offerId) {
        return !needsUploadToYt(offerId);
    }

    private boolean needsUploadToYt(Offer offer) {
        return needsUploadToYt(offer.getId());
    }

    private boolean needsUploadToYt(Long offerId) {
        return ytOfferUploadQueueService.areAllOfferIdsInQueue(List.of(offerId));
    }

    private void insertOffersKeepStamps(Collection<Offer> offers) {
        offers.forEach(this::insertOfferKeepStamp);
    }

    private void insertOffersKeepStamps(Offer... offers) {
        Arrays.stream(offers).forEach(this::insertOfferKeepStamp);
    }

    private void insertOfferKeepStamp(Offer offer) {
        var stamp = offer.getUploadToYtStamp();
        offerRepository.insertOffer(offer);
        OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offer.getId(), stamp);
        offer.setUploadToYtStamp(stamp);
    }
}
