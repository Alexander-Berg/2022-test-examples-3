package ru.yandex.market.mboc.common.services.business;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.LinkedToBusinessSupplierCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.OffersForServiceFilter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;

public abstract class BusinessSupplierServiceTest extends BaseDbTestClass {

    private OfferRepository offerRepository;
    private SupplierRepository supplierRepository;

    private BusinessSupplierService businessSupplierService;

    private final Supplier business = new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS);
    private final Supplier bizSupplier1 = new Supplier(2001, "biz-child1").setBusinessId(2000)
        .setType(MbocSupplierType.THIRD_PARTY);
    private final Supplier bizSupplier2 = new Supplier(2002, "biz-child2").setBusinessId(2000)
        .setType(MbocSupplierType.THIRD_PARTY);
    private final Supplier bizSupplier3 = new Supplier(2003, "biz-child3").setBusinessId(2000)
        .setType(MbocSupplierType.THIRD_PARTY);
    private final Supplier notLinkedSupplier = new Supplier(2010, "not-linked-supplier")
        .setType(MbocSupplierType.THIRD_PARTY);

    @Before
    public void setUp() throws Exception {
        offerRepository = getOfferRepository();
        supplierRepository = getSupplierRepository();

        businessSupplierService = new BusinessSupplierService(supplierRepository, offerRepository);

        supplierRepository.insertBatch(
            business,
            bizSupplier1,
            bizSupplier2,
            bizSupplier3,
            notLinkedSupplier
        );
    }

    @Test
    public void testFindOffersForServiceByShopSkuKeys() {
        offerRepository.insertOffers(
            offer(1001, business.getId(), "bizsku1")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier1)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK),
            offer(1002, business.getId(), "bizsku2")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier3)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK),
            offer(1003, business.getId(), "bizsku3")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier3)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
        );

        // find on linked supplier
        List<ShopSkuKey> keys = List.of(new ShopSkuKey(bizSupplier1.getId(), "bizsku1"));
        List<OfferForService> offers =
            businessSupplierService.findOffersForServiceByShopSkuKeys(new OffersFilter(), keys);
        Map<Integer, List<OfferForService>> map =
            offers.stream().collect(Collectors.groupingBy(OfferForService::getSupplierId));
        Assertions.assertThat(map).hasSize(1);

        List<OfferForService> offersForSupplier = map.get(bizSupplier1.getId());
        assertEquals(1, offersForSupplier.size());
        assertEquals(1001, offersForSupplier.get(0).getBaseOffer().getId());
        assertEquals("bizsku1", offersForSupplier.get(0).getShopSku());

        // do not find on business
        keys = List.of(new ShopSkuKey(business.getId(), "bizsku1"));
        offers = businessSupplierService.findOffersForServiceByShopSkuKeys(new OffersFilter(), keys);
        Assertions.assertThat(offers).hasSize(0);

        // multiple search
        keys = List.of(
            new ShopSkuKey(bizSupplier2.getId(), "bizsku2"),
            new ShopSkuKey(bizSupplier3.getId(), "bizsku3")
        );
        offers = businessSupplierService.findOffersForServiceByShopSkuKeys(new OffersFilter(), keys);
        map = offers.stream().collect(Collectors.groupingBy(OfferForService::getSupplierId));
        Assertions.assertThat(map).hasSize(2);

        offersForSupplier = map.get(bizSupplier2.getId());
        assertEquals(1, offersForSupplier.size());
        assertEquals(1002, offersForSupplier.get(0).getBaseOffer().getId());
        assertEquals("bizsku2", offersForSupplier.get(0).getShopSku());

        offersForSupplier = map.get(bizSupplier3.getId());
        assertEquals(1, offersForSupplier.size());
        assertEquals(1003, offersForSupplier.get(0).getBaseOffer().getId());
        assertEquals("bizsku3", offersForSupplier.get(0).getShopSku());

        // do not find because of service offer limitation
        keys = List.of(
            new ShopSkuKey(bizSupplier3.getId(), "bizsku1")
        );
        offers = businessSupplierService.findOffersForServiceByShopSkuKeys(new OffersFilter(), keys);
        Assertions.assertThat(offers).hasSize(0);
    }

    @Test
    public void testFindOffersForServiceByShopSkuKeysNotLinkedSupplier() {
        offerRepository.insertOffers(
            offer(1001, notLinkedSupplier.getId(), "ssku1")
                .addNewServiceOfferIfNotExistsForTests(notLinkedSupplier)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK),
            offer(1002, business.getId(), "ssku2")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier3)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
        );

        List<ShopSkuKey> keys = List.of(new ShopSkuKey(notLinkedSupplier.getId(), "ssku1"));
        List<OfferForService> offers =
            businessSupplierService.findOffersForServiceByShopSkuKeys(new OffersFilter(), keys);
        Map<Integer, List<OfferForService>> map =
            offers.stream().collect(Collectors.groupingBy(OfferForService::getSupplierId));
        Assertions.assertThat(map).hasSize(1);

        List<OfferForService> offersForSupplier = map.get(notLinkedSupplier.getId());
        assertEquals(1, offersForSupplier.size());
        assertEquals(1001, offersForSupplier.get(0).getBaseOffer().getId());
        assertEquals("ssku1", offersForSupplier.get(0).getShopSku());
    }

    @Test
    public void testFindOffersForService() {
        offerRepository.insertOffers(
            offer(1001, business.getId(), "bizsku1")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier1)
                .updateAcceptanceStatusForTests(bizSupplier1.getId(), Offer.AcceptanceStatus.OK)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2),
            offer(1002, business.getId(), "bizsku2")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .updateAcceptanceStatusForTests(bizSupplier2.getId(), Offer.AcceptanceStatus.OK)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier3)
                .updateAcceptanceStatusForTests(bizSupplier3.getId(), Offer.AcceptanceStatus.TRASH),
            offer(1003, business.getId(), "bizsku3")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier2)
                .addNewServiceOfferIfNotExistsForTests(bizSupplier3)
        );

        // service acceptance filter
        OffersFilter offersFilter = new OffersFilter()
            .setBusinessIds(List.of(business.getId()))
            .addCriteria(new LinkedToBusinessSupplierCriteria(List.of(bizSupplier2.getId(), bizSupplier3.getId())));
        var offersForServiceFilter = OffersForServiceFilter.from(offersFilter);
        offersForServiceFilter.setServiceAcceptanceStatuses(Set.of(Offer.AcceptanceStatus.NEW));
        List<OfferForService> offersForService = offerRepository.findOffersForService(offersForServiceFilter, false);
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getId())
            .containsExactlyInAnyOrder(1001L, 1003L, 1003L);
        Assertions.assertThat(offersForService)
            .extracting(OfferForService::getAcceptanceStatus)
            .containsOnly(Offer.AcceptanceStatus.NEW);

        // base acceptance filter
        offersFilter = new OffersFilter()
            .setAcceptanceStatus(Offer.AcceptanceStatus.OK)
            .setBusinessIds(List.of(business.getId()))
            .addCriteria(new LinkedToBusinessSupplierCriteria(List.of(bizSupplier2.getId(), bizSupplier3.getId())));
        offersForServiceFilter = OffersForServiceFilter.from(offersFilter);
        offersForService = offerRepository.findOffersForService(offersForServiceFilter, false);
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getId())
            .containsExactlyInAnyOrder(1001L, 1002L, 1002L);
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getAcceptanceStatus())
            .containsOnly(Offer.AcceptanceStatus.OK);
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getIsOfferContentPresent())
            .containsOnly(false);
    }

    @Test
    public void testFindOffersForServiceWithContent() {
        offerRepository.insertOffers(
            offer(1001, business.getId(), "bizsku1")
                .addNewServiceOfferIfNotExistsForTests(bizSupplier1)
                .storeOfferContent(OfferContent.builder().urls(List.of("https://url.ru")).build())
        );

        OffersFilter offersFilter = new OffersFilter()
            .setBusinessIds(List.of(business.getId()))
            .setFetchOfferContent(true);
        var offersForServiceFilter = OffersForServiceFilter.from(offersFilter);
        List<OfferForService> offersForService = offerRepository.findOffersForService(offersForServiceFilter, false);
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getId())
            .containsExactlyInAnyOrder(1001L);
        Assertions.assertThat(offersForService)
            .flatExtracting(o -> o.getBaseOffer().extractOfferContent().getUrls())
            .containsExactly("https://url.ru");
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getIsOfferContentPresent())
            .containsOnly(true);
    }

    protected abstract OfferRepository getOfferRepository();

    protected abstract SupplierRepository getSupplierRepository();

    private Offer offer(int id, int supplierId, String shopSku) {
        return Offer.builder()
            .id(id)
            .title("1")
            .mappingDestination(Offer.MappingDestination.BLUE)
            .categoryId(99L)
            .approvedSkuMapping(new Offer.Mapping(22L, DateTimeUtils.dateTimeNow(), null))
            .approvedSkuMappingConfidence(CONTENT)
            .isOfferContentPresent(true)
            .shopCategoryName("c")
            .offerContent(OfferContent.builder().build())
            .uploadToYtStamp(11L)
            .shopSku(shopSku)
            .businessId(supplierId)
            .build();
    }
}
