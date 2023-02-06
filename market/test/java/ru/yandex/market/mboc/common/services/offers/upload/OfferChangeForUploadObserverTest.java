package ru.yandex.market.mboc.common.services.offers.upload;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class OfferChangeForUploadObserverTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private YtOfferUploadQueueService ytQueue;
    @Autowired
    private ErpOfferUploadQueueService erpQueue;
    @Autowired
    private MdmOfferUploadQueueService mdmQueue;

    Supplier supplier;
    Supplier businessSupplier = OfferTestUtils.businessSupplier();

    @Before
    public void setup() {
        supplier = new Supplier(42, "Real supplier", null, null)
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("77777");
        supplierRepository.insertBatch(supplier, businessSupplier);
    }

    @Test
    public void detectsYtChanges() {
        // Triggers on business id change
        testChange(ytQueue, true, offer -> {
                    offer.setBusinessId(businessSupplier.getId());
            }
        );
        testServiceOfferChanges(ytQueue);
        // Triggers on shop sku change
        testChange(ytQueue, true, offer ->
                offer.setShopSku("new-ssku"));
        // Triggers on title change
        testChange(ytQueue, true, offer ->
                offer.setTitle("new title"));
        // Triggers on shop category change
        testChange(ytQueue, true, offer ->
                offer.setShopCategoryName("new-shop-cat"));
        // Triggers on barcode change
        testChange(ytQueue, true, offer ->
                offer.setBarCode("new-barcode"));
        // Triggers on vendor change
        testChange(ytQueue, true, offer ->
                offer.setVendor("new-vendor"));

        // Triggers on VendorCode change
        testChange(ytQueue, true, offer ->
                offer.setVendorCode("new-vendorCode"));
        // Triggers on category id change
        testChange(ytQueue, true, offer ->
                offer.setCategoryIdForTests(100L, Offer.BindingKind.APPROVED));
        // Triggers on mappedModelId change
        testChange(ytQueue, true, offer ->
                offer.setMappedModelId(10000L));
        // Triggers on mappedModelConfidence change
        testChange(ytQueue, true, offer ->
            offer.setMappedModelConfidence(Offer.MappingConfidence.CONTENT));
        // Triggers on mappedCategoryId change
        testChange(ytQueue, true, offer ->
            offer.setMappedCategoryId(10000L));
        // Triggers on mappedCategoryConfidence change
        testChange(ytQueue, true, offer ->
            offer.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT));
        // Triggers on offer destination change
        testChange(ytQueue, true, offer ->
                offer.setOfferDestination(Offer.MappingDestination.WHITE));

        // Triggers on changed timestamp in approved mapping
        testChange(ytQueue, true, offer ->
        {
            var current = offer.getApprovedSkuMapping();
            offer.setApprovedSkuMappingInternal(new Offer.Mapping(current.getMappingId(), LocalDateTime.now()));
        });

        // Triggers on changed timestamp in approved mapping
        testChange(ytQueue, true, offer ->
        {
            var current = offer.getApprovedSkuMapping();
            offer.setApprovedSkuMappingInternal(new Offer.Mapping(current.getMappingId(), LocalDateTime.now()));
        });
        // Triggers on changed timestamp in deleted mapping
        testChange(ytQueue, true, preset -> {
                    preset.setDeletedApprovedSkuMapping(new Offer.Mapping(98L, LocalDateTime.now()));
                }, offer ->
                {
                    var current = offer.getDeletedApprovedSkuMapping();
                    offer.setDeletedApprovedSkuMapping(new Offer.Mapping(current.getMappingId(), LocalDateTime.now()));
                }
        );

        // Check content description
        testChange(ytQueue, true,
                preset -> preset.storeOfferContent(OfferContent.builder().description("desc").build()),
                offer -> offer.storeOfferContent(OfferContent.builder().description("new desc").build()));

        // Check should not trigger
        testChange(ytQueue, false, offer -> {
            offer.setAdult(true)
                    .setModifiedByLogin("login")
                    .setBeruPrice(322.0D)
                    .setVendorId(544)
                    .setContentComment("no comments")
                    .setContentLabMessage("msg")
                    .setContentChangedTs(LocalDateTime.now());
        });
    }

    @Test
    public void detectsErpChanges() {
        testServiceOfferChanges(erpQueue);
        testChange(erpQueue, false, offer -> {
            offer.setAdult(true)
                .setModifiedByLogin("login")
                .setBeruPrice(322.0D)
                .setVendor("vndr");
        });
        testChange(erpQueue, false,
            preset -> preset.updateApprovedSkuMapping(null),
            offer -> offer.updateApprovedSkuMapping(null));

        testChange(erpQueue, true, offer ->
            offer.setTitle("testTitle"));
        testChange(erpQueue, true, offer ->
            offer.setCategoryIdForTests(1121L, Offer.BindingKind.APPROVED));
        testChange(erpQueue, true, offer ->
            offer.setVendorId(1331));
        testChange(erpQueue, true, offer ->
            offer.setMarketVendorName("testMVN"));
        testChange(erpQueue, true, offer ->
            offer.setBarCode("134"));
        // Only mappingid has changed
        testChange(erpQueue, true, offer ->
        {
            var current = offer.getApprovedSkuMapping();
            offer.setApprovedSkuMappingInternal(new Offer.Mapping(
                    100L, current.getTimestamp(), current.getSkuType()));
        });
        testChange(erpQueue, true, offer ->
                offer.setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER));
        testChange(erpQueue, false, offer ->
        {
            var current = offer.getApprovedSkuMapping();
            offer.setApprovedSkuMappingInternal(new Offer.Mapping(
                    current.getMappingId(), LocalDateTime.now(), Offer.SkuType.PARTNER20));
        });
    }

    @Test
    public void detectsMdmChanges() {
        testServiceOfferChanges(mdmQueue);
        testChange(mdmQueue, false, offer -> {
            offer.setTitle("new title")
                    .setAdult(true)
                    .setModifiedByLogin("login")
                    .setCategoryIdForTests(245L, Offer.BindingKind.APPROVED)
                    .setBarCode("134")
                    .setBeruPrice(322.0D)
                    .setMappedModelId(467L, Offer.MappingConfidence.PARTNER_SELF)
                    .setVendor("vnd")
                    .setVendorId(544)
                    .setApprovedSkuMappingInternal(new Offer.Mapping(99L, LocalDateTime.now()))
                    .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);
        });
        testChange(mdmQueue, true, offer -> {
            offer.setShopSku("newShopSku123");
        });
    }

    private void testServiceOfferChanges(OfferUploadQueueService queueService) {
        // Triggers on new service offer
        testChange(queueService, true,
            preset -> {
                preset.setServiceOffers(Collections.emptyList());
            },
            offer -> {
                offer.addNewServiceOfferIfNotExistsForTests(supplier);
            }
        );
        // Triggers on deleted service offer
        testChange(queueService, true, preset -> {
                preset.addNewServiceOfferIfNotExistsForTests(List.of(supplier, businessSupplier));
            }
            , offer -> {
                offer.setServiceOffers(List.of(new Offer.ServiceOffer(supplier.getId(), supplier.getType(), Offer.AcceptanceStatus.OK)));
            }
        );
        // Triggers on changed service offer
        testChange(queueService, true, preset -> {
                preset.addNewServiceOfferIfNotExistsForTests(supplier);
            }
            , offer -> {
                var so = offer.getServiceOffer(supplier.getId()).orElseThrow();
                offer.setServiceOffers(new Offer.ServiceOffer(so.getSupplierId(),
                    so.getSupplierType(), Offer.AcceptanceStatus.TRASH));
            }
        );
    }

    private void testChange(OfferUploadQueueService queue, boolean shouldTriggerUpload, Consumer<Offer> change) {
        testChange(queue, shouldTriggerUpload, null, change);
    }

    private void testChange(OfferUploadQueueService queue, boolean shouldTriggerUpload, Consumer<Offer> preset,
                            Consumer<Offer> change) {
        var offer = OfferTestUtils.nextOffer(supplier)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .addNewServiceOfferIfNotExistsForTests(supplier)
                .setMappedCategoryId(99L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(new Offer.Mapping(
                        99L,
                        LocalDateTime.now(),
                        Offer.SkuType.MARKET
                ))
                .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);
        ;
        offerRepository.insertOffer(offer);

        var offerFromRepo = offerRepository.findOffers(new OffersFilter()
                .setOfferId(offer.getId())
                .setFetchOfferContent(true)).get(0);

        if (preset != null) {
            preset.accept(offerFromRepo);
            offerRepository.updateOffer(offerFromRepo);
            offerFromRepo = offerRepository.findOffers(new OffersFilter()
                    .setOfferId(offerFromRepo.getId())
                    .setFetchOfferContent(true)).get(0);
        }

        // To clean from queue
        queue.dequeueOffers(List.of(offer));

        change.accept(offerFromRepo);
        offerRepository.updateOffer(offerFromRepo);

        Assertions.assertThat(queue.areAllOffersInQueue(List.of(offerFromRepo)))
                .isEqualTo(shouldTriggerUpload);
    }

    private enum UploadTo {
        YT, ERP, MDM;
    }
}
