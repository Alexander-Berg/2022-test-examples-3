package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 31.05.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class OfferStatServiceTest extends BaseIntegrationTestClass {

    @Autowired
    OfferStatService offerStatService;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("slaveSqlNamedParameterJdbcTemplate")
    NamedParameterJdbcTemplate slaveJdbcTemplate;

    @Autowired
    OfferRepositoryImpl offerRepository;

    @Autowired
    SupplierRepositoryImpl supplierRepository;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Test
    public void testStatDoesntFail() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        offerRepository.insertOffers(offers);
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setCategoryIdForTests(null, Offer.BindingKind.SUGGESTED));

        offerStatService.updateOfferStat();
        Map<Integer, OfferStatService.OfferStat> stat = offerStatService.getOfferStat();
        assertThat(stat).hasSize(7);
        assertThat(stat.get(42).getOffersCount()).isEqualTo(3);
        assertThat(stat.get(42).getAcceptedOffers()).isEqualTo(3);
        assertThat(stat.get(42).getSendToContent()).isEqualTo(3);
        assertThat(stat.get(77).getInProcessOffers()).isEqualTo(1);
        assertThat(stat.get(77).getRequireAttention()).isEqualTo(1);
        assertThat(stat.get(78).getOffersWithApprovedMappings()).isEqualTo(2);
        assertThat(stat.get(79).getNeedInfo()).isEqualTo(1);
        assertThat(stat.get(79).getNoAcceptanceStatus()).isEqualTo(1);
        assertThat(stat.get(79).getInModerationOffers()).isEqualTo(1);
    }

    @Test
    public void testCountOfferProcessingStatusesRequest() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        offerRepository.insertOffers(offers);

        NamedParameterJdbcTemplate masterSpy = Mockito.spy(jdbcTemplate);
        NamedParameterJdbcTemplate slaveSpy = Mockito.spy(slaveJdbcTemplate);
        offerStatService = new OfferStatService(masterSpy, slaveSpy,
            null, null, offerRepository, storageKeyValueService);

        Map<SupplierOffer.OfferProcessingStatus, Integer> stat = offerStatService.countOffersByProcessingStatus(
            new OffersFilter().addBusinessId(42));
        assertThat(stat).containsExactly(Maps.immutableEntry(SupplierOffer.OfferProcessingStatus.IN_WORK, 3));

        Mockito.verify(masterSpy, Mockito.times(0))
            .query(Mockito.anyString(), Mockito.anyMap(), Mockito.any(RowCallbackHandler.class));
        Mockito.verify(slaveSpy, Mockito.times(1))
            .query(Mockito.anyString(), Mockito.anyMap(), Mockito.any(RowCallbackHandler.class));
    }

    @Test
    public void testCountOfferProcessingStatusesRequestWithCriteria() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        offerRepository.insertOffers(offers);

        Map<SupplierOffer.OfferProcessingStatus, Integer> stat = offerStatService.countOffersByProcessingStatus(
            new OffersFilter()
                .addBusinessId(78)
                .addCriteria(OfferCriterias.hasMapping(Offer.MappingType.APPROVED))
                .addCriteria(OfferCriterias.externalTextSearch("Напильники для PGaaS")));
        assertThat(stat).containsExactly(Maps.immutableEntry(SupplierOffer.OfferProcessingStatus.READY, 1));
    }

    @Test
    public void testStatRotation() throws InterruptedException {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        offerRepository.insertOffers(offers);

        offerStatService.updateOfferStat();
        Map<Integer, OfferStatService.OfferStat> stat = offerStatService.getOfferStat();
        assertThat(stat).hasSize(7);
        assertThat(stat.get(42).getOffersCount()).isEqualTo(3);
        assertThat(stat.get(42).getAcceptedOffers()).isEqualTo(3);
        assertThat(stat.get(42).getSendToContent()).isEqualTo(3);
        assertThat(stat.get(77).getInProcessOffers()).isEqualTo(1);
        assertThat(stat.get(77).getRequireAttention()).isEqualTo(1);
        assertThat(stat.get(78).getOffersWithApprovedMappings()).isEqualTo(2);
        assertThat(stat.get(79).getNeedInfo()).isEqualTo(1);
        assertThat(stat.get(79).getNoAcceptanceStatus()).isEqualTo(1);
        assertThat(stat.get(79).getInModerationOffers()).isEqualTo(1);

        offerRepository.insertOffer(OfferTestUtils.nextOffer().setBusinessId(42)
            .setCategoryIdForTests(42L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer().setBusinessId(42)
            .setCategoryIdForTests(43L, Offer.BindingKind.SUGGESTED));
        offerStatService.updateOfferStat();

        stat = offerStatService.getOfferStat();
        assertThat(stat).hasSize(7);
        assertThat(stat.get(42).getOffersCount()).isEqualTo(5);
    }

    @Test
    public void testCalculateStatForOffersOnBusiness() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        offerRepository.insertOffers(offers);

        offerStatService.updateOfferStat();
        Map<Integer, OfferStatService.OfferStat> stat = offerStatService.getOfferStat();
        assertThat(stat).hasSize(7);
        assertThat(stat).doesNotContainKey(200);

        assertThat(stat.get(201).getOffersCount()).isEqualTo(2);
        assertThat(stat.get(202).getOffersCount()).isEqualTo(1);
        assertThat(stat.get(203).getOffersCount()).isEqualTo(1);

        assertThat(stat.get(201).getAcceptedOffers()).isEqualTo(2);
        assertThat(stat.get(202).getAcceptedOffers()).isEqualTo(1);
        assertThat(stat.get(203).getAcceptedOffers()).isEqualTo(0);

        assertThat(stat.get(201).getSendToContent()).isEqualTo(2);
        assertThat(stat.get(202).getSendToContent()).isEqualTo(1);
        assertThat(stat.get(203).getSendToContent()).isEqualTo(1); // смотрим acceptance базового

        assertThat(stat.get(201).getInProcessOffers()).isEqualTo(0);
        assertThat(stat.get(202).getInProcessOffers()).isEqualTo(0);
        assertThat(stat.get(203).getInProcessOffers()).isEqualTo(0);

        assertThat(stat.get(201).getRequireAttention()).isEqualTo(0);
        assertThat(stat.get(202).getRequireAttention()).isEqualTo(0);
        assertThat(stat.get(203).getRequireAttention()).isEqualTo(0);

        assertThat(stat.get(201).getOffersWithApprovedMappings()).isEqualTo(1);
        assertThat(stat.get(202).getOffersWithApprovedMappings()).isEqualTo(0);
        assertThat(stat.get(203).getOffersWithApprovedMappings()).isEqualTo(0);

        assertThat(stat.get(201).getNeedInfo()).isEqualTo(0);
        assertThat(stat.get(202).getNeedInfo()).isEqualTo(0);
        assertThat(stat.get(203).getNeedInfo()).isEqualTo(0);

        assertThat(stat.get(201).getNoAcceptanceStatus()).isEqualTo(0);
        assertThat(stat.get(202).getNoAcceptanceStatus()).isEqualTo(0);
        assertThat(stat.get(203).getNoAcceptanceStatus()).isEqualTo(1);

        assertThat(stat.get(201).getInModerationOffers()).isEqualTo(0);
        assertThat(stat.get(202).getInModerationOffers()).isEqualTo(0);
        assertThat(stat.get(203).getInModerationOffers()).isEqualTo(0);
    }

    @Test
    public void testCategoryStat() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        offerRepository.insertOffers(offers);
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setCategoryIdForTests(null, Offer.BindingKind.SUGGESTED));

        offerStatService.updateOfferStat();
        assertThat(offerStatService.getOffersCategories()).containsExactlyInAnyOrder(12L, 22L, 33L);
    }

    @Test
    public void testCountBusinessOffers() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/stat-test.yml");
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        offerRepository.insertOffers(offers);
        offerStatService.updateOfferStat();

        assertEquals(1, offerStatService.countBusinessOffers(200));
        assertEquals(0, offerStatService.countBusinessOffers(999));
    }

    @Test
    public void testSendingToQueueOfOutdatedStats() {
        jdbcTemplate.update("truncate mbo_category.offer_stat", Map.of());
        jdbcTemplate.update("truncate mbo_category.offer_stat_queue", Map.of());
        jdbcTemplate.update("insert into mbo_category.offer_stat\n" +
            "(category_id, supplier_id, business_supplier_id, last_updated, offers_count, accepted, in_process,\n" +
            "    in_moderation,\n" +
            "    approved_mappings,\n" +
            "    require_attention,\n" +
            "    need_info,\n" +
            "    no_acceptance_status,\n" +
            "    send_to_content,\n" +
            "    hidden_offers,\n" +
            "    automatic_availability_offers,\n" +
            "    accepted_need_content\n" +
            ") values\n" +
            "(100500, 999, 999, now() - interval '6 hour', 0,0,0,0,0,0,0,0,0,0,0,0),\n" +
            "(100501, 999, 999, now() - interval '5 hour', 0,0,0,0,0,0,0,0,0,0,0,0),\n" +
            "(100502, 999, 999, now() - interval '1 hour', 0,0,0,0,0,0,0,0,0,0,0,0)\n ", Map.of()
        );

        offerStatService.sendOutdatedGroupsQueue();
        jdbcTemplate.query("select count(*) from mbo_category.offer_stat_queue",
            (rs, row) -> assertThat(rs.getInt(1)).isEqualTo(2));
    }

}
