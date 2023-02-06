package ru.yandex.market.deepmind.common.repository.service_offer;

import java.time.Instant;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ServiceOfferReplicaRepositoryTest extends DeepmindBaseDbTestClass {
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Autowired
    private JdbcTemplate deepmindSqlJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        deepmindSupplierRepository.save(
            supplier(1, null, SupplierType.THIRD_PARTY),
            supplier(2, null, SupplierType.THIRD_PARTY),
            supplier(3, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000056"),
            supplier(100, 100, SupplierType.BUSINESS),
            supplier(101, 100, SupplierType.THIRD_PARTY),
            supplier(102, 100, SupplierType.FIRST_PARTY),
            supplier(103, 100, SupplierType.FIRST_PARTY),
            supplier(105, 100, SupplierType.THIRD_PARTY)
        );
    }

    @Test
    public void testInsert() {
        var offer = offer(1, 1, "sku1", 1);
        serviceOfferReplicaRepository.save(offer);

        var sss = serviceOfferReplicaRepository.findAll();

        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, 1, "sku1", 1)
            );
    }

    @Test
    public void testUpdate() {
        // insert
        var offer = serviceOfferReplicaRepository.save(offer(1, 1, "sku1", 1)).get(0);

        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer
            );

        // update
        offer.setMskuId(2L);
        serviceOfferReplicaRepository.save(offer);

        // assert
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, 1, "sku1", 2)
            );
    }

    @Test
    public void testDelete() {
        // insert
        serviceOfferReplicaRepository.save(offer(1, 1, "sku1", 100));
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll()).hasSize(1);

        serviceOfferReplicaRepository.delete(new ServiceOfferKey(1, "sku1"));

        // assert
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll()).isEmpty();
    }

    @Test
    public void testFilterBySupplierType() {
        serviceOfferReplicaRepository.save(
            offer(1, 1, "sku2", 100, SupplierType.THIRD_PARTY),
            offer(101, 101, "sku2", 100, SupplierType.THIRD_PARTY),
            offer(102, 102, "sku2", 100, SupplierType.FIRST_PARTY)
        );
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findAll()).hasSize(3);

        var thirdPartyFilter = new ServiceOfferReplicaFilter().setSupplierTypes(SupplierType.THIRD_PARTY);
        var firstPartyFilter = new ServiceOfferReplicaFilter().setSupplierTypes(SupplierType.FIRST_PARTY);
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findOffers(thirdPartyFilter))
            .hasSize(2);
        DeepmindAssertions.assertThatServiceOffers(serviceOfferReplicaRepository.findOffers(firstPartyFilter))
            .hasSize(1);
    }

    @Test
    public void testFilterByKey() {
        serviceOfferReplicaRepository.save(
            offer(1, 1, "sku", 100),
            offer(2, 2, "sku", 100)
        );

        var offer = serviceOfferReplicaRepository.findOfferByKey(new ServiceOfferKey(1, "sku"));
        DeepmindAssertions.assertThat(offer)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .isEqualTo(new ServiceOfferKey(1, "sku"));
    }

    @Test
    public void testSearchByShopSkuOrRealShopSku() {
        var offersConverter = new OffersConverterImpl(deepmindSqlJdbcTemplate, new BeruIdMock(),
            deepmindSupplierRepository);

        serviceOfferReplicaRepository.save(
            offer(2, 2, "sku21", 100, SupplierType.THIRD_PARTY),
            offer(2, 2, "sku22", 100, SupplierType.THIRD_PARTY),
            offer(3, 3, "sku22", 100, SupplierType.REAL_SUPPLIER),
            offer(4, 4, "sku22", 100, SupplierType.REAL_SUPPLIER),
            offer(3, 3, "sku31", 100, SupplierType.REAL_SUPPLIER),
            offer(3, 3, "sku32", 100, SupplierType.REAL_SUPPLIER),
            offer(3, 3, "sku3,2", 100, SupplierType.REAL_SUPPLIER)
        );

        var search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku22");
        var offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku22"),
                new ServiceOfferKey(3, "sku22"),
                new ServiceOfferKey(4, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "2_sku22");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "000056.sku22");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(3, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku22 2_sku22 000056.sku22");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku22"),
                new ServiceOfferKey(3, "sku22"),
                new ServiceOfferKey(4, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku22 000056.sku22");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku22"),
                new ServiceOfferKey(3, "sku22"),
                new ServiceOfferKey(4, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku22 2_sku22");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku22"),
                new ServiceOfferKey(3, "sku22"),
                new ServiceOfferKey(4, "sku22")
            );

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "2_");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .isEmpty();

        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "a123b_ssku");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .isEmpty();

        // delimeters: space or comma+space but not just comma
        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku22,000056.sku32");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));
        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .isEmpty();

        // shop sku value contains comma
        search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "sku3,2");
        offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));
        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getShopSku)
            .containsExactly("sku3,2");
    }

    @Test
    public void testOffersConverterSupplierCache() {
        var deepmindSupplierRepositorySpy = Mockito.spy(deepmindSupplierRepository);
        var offersConverter = new OffersConverterImpl(deepmindSqlJdbcTemplate, new BeruIdMock(),
            deepmindSupplierRepositorySpy);
        ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "123_ssku 123_ssku2 123_ssku3");
        verify(deepmindSupplierRepositorySpy, times(1)).findById(123);
    }

    /**
     * Этот тест проверяет, что если у нас expanded версия 1P shop_sku совпадает просто с shop_sku,
     * то поиск возвращает только 1P оффер. Более формально: https://nda.ya.ru/t/hvBTy-X63iTCTy
     *
     * Это не самое хорошее решение, так как фактически поиск не возвращает все офферы.
     * Но его пришлось реализовать в угоду производительности. DEEPMIND-172
     *
     * К счастью, на проде только один оффер у которого совпадает shop_sku: https://nda.ya.ru/t/hvBTy-X63iTCTy
     */
    @Test
    public void testReturnOnly1PIfSeveralSskuIsApplied() {
        var offersConverter = new OffersConverterImpl(deepmindSqlJdbcTemplate, new BeruIdMock(),
            deepmindSupplierRepository);
        serviceOfferReplicaRepository.save(
            offer(2, 2, "sku-3p", 100),
            offer(3, 3, "000056.sku-1p", 100),
            offer(3, 3, "sku-1p", 100)
        );

        var search = ServiceOfferCriterias.searchBySskuOrRealSsku(offersConverter, "000056.sku-1p, sku-3p");
        var offers = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter().addCriteria(search));

        Assertions.assertThat(offers)
            .extracting(ServiceOfferReplica::getServiceOfferKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(2, "sku-3p"),
                new ServiceOfferKey(3, "sku-1p")
            );
    }

    private Supplier supplier(int id, @Nullable Integer businessId, SupplierType type) {
        return new Supplier().setId(id).setBusinessId(businessId).setName("Supplier").setSupplierType(type);
    }

    private ServiceOfferReplica offer(
        Integer businessId, int supplierId, String shopSku, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica offer(
        Integer businessId, int supplierId, String shopSku, long mskuId, SupplierType supplierType) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
