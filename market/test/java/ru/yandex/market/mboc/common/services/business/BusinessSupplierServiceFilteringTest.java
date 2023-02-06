package ru.yandex.market.mboc.common.services.business;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.search.LinkedToBusinessSupplierCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.OffersForServiceFilter;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;


/**
 * @author shadoff
 * created on 11/25/20
 */
public class BusinessSupplierServiceFilteringTest extends BaseDbTestClass {

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    @Autowired
    private OfferRepositoryImpl offerRepository;

    private AtomicInteger skuNum = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child3").setBusinessId(2000),
            new Supplier(2003, "biz-child4").setBusinessId(2000),
            new Supplier(2010, "not-linked-supplier")
        );
    }

    @Test
    public void testLimitAndOffset() {
        Offer offer1 = baseOffer(2000, 2001);
        Offer offer2 = baseOffer(2000, 2002);
        Offer offer3 = baseOffer(2000, 2002);
        Offer offer4 = baseOffer(2000, 2002);
        Offer offer5 = baseOffer(2000, 2001, 2002);

        offerRepository.insertOffers(offer1, offer2, offer3, offer4, offer5);

        OffersFilter filter = new OffersFilter()
            .addCriteria(new LinkedToBusinessSupplierCriteria(List.of(2002)))
            .setLimit(2)
            .setOffset(2)
            .setOrderBy(OffersFilter.Field.ID);

        long countOffersForService = offerRepository.getCountOffersForService(filter);
        Assertions.assertThat(countOffersForService).isEqualTo(4);

        List<OfferForService> offersForService = offerRepository.findOffersForService(filter, false);

        Assertions.assertThat(offersForService).hasSize(2);
        Assertions.assertThat(offersForService)
            .extracting(OfferForService::getBusinessSkuKey)
            .containsExactlyInAnyOrder(
                offer4.getBusinessSkuKey(),
                offer5.getBusinessSkuKey()
            );
        Assertions.assertThat(offersForService)
            .extracting(OfferForService::getShopSkuKey)
            .containsExactlyInAnyOrder(
                new ShopSkuKey(offer4.getServiceOffers().get(0).getSupplierId(), offer4.getShopSku()),
                new ShopSkuKey(offer5.getServiceOffers().get(1).getSupplierId(), offer5.getShopSku())
            );

        var ids = offerRepository.findOfferIdsForService(OffersForServiceFilter.from(filter));
        Assertions.assertThat(offersForService)
            .extracting(o -> o.getBaseOffer().getId()).containsExactlyElementsOf(ids);
    }

    @Test
    public void testOffersForServiceFilterPurifiesOffersFilter() {
       var offersFilter = new OffersFilter()
           .addCriteria(new LinkedToBusinessSupplierCriteria(List.of(2002)));
       var filter = OffersForServiceFilter.from(offersFilter);
       Assertions.assertThat(filter.getOffersFilter().getCriterias()).isEmpty();
       Assertions.assertThat(filter.getServiceSuppliers()).containsExactly(2002);
    }

    private Offer baseOffer(int supplierId) {
        return baseOffer(supplierId, supplierId);
    }

    private Offer baseOffer(int businessId, int... supplierIds) {
        Offer offer = new Offer()
            .setShopSku("Sku" + skuNum.incrementAndGet())
            .setMappingDestination(Offer.MappingDestination.BLUE)
            .setTitle("Title")
            .setShopCategoryName("Category")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setBusinessId(businessId);
        supplierRepository.findByIds(Arrays.stream(supplierIds).boxed().collect(Collectors.toList()))
            .forEach(offer::addNewServiceOfferIfNotExistsForTests);
        return offer;
    }
}
