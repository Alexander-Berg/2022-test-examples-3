package ru.yandex.market.mboc.common.offers.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.lightmapper.criteria.NotCriteria;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferLite;
import ru.yandex.market.mboc.common.offers.model.OfferShortInfo;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.SupplierKeyGreaterCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.SupplierTypeCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.TestSupplierCriteria;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;

/**
 * @author yuramalinov
 * @created 16.04.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferRepositoryImplFindTest extends BaseDbTestClass {
    @Autowired
    private OfferRepository repository;
    @Autowired
    private SupplierRepository supplierRepository;

    private OfferRepositoryMock repositoryMock;
    private List<Offer> initOffers;
    private NamedParameterJdbcTemplate jdbcSpy;
    private NamedParameterJdbcTemplate originalJdbcTemplate;

    @Before
    public void loadData() {
        List<Offer> offerList = YamlTestUtil.readOffersFromResources("offers/search-test.yml");

        Assertions.assertThat(offerList.stream().map(Offer::getId).distinct())
            .withFailMessage("Offers should have distinct ids!")
            .hasSize(offerList.size());

        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

        originalJdbcTemplate = (NamedParameterJdbcTemplate)
            ReflectionTestUtils.getField(repository, "jdbcTemplate");

        jdbcSpy = Mockito.spy(originalJdbcTemplate);
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbcSpy);

        repository.insertOffers(offerList);
        // Офферы прогружаем без фильра целиком из БД, т.к. у них обновится lastVersion
        this.repositoryMock = new OfferRepositoryMock();
        this.initOffers = repository.findAll();
        this.repositoryMock.setOffers(initOffers);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(repository, "jdbcTemplate", originalJdbcTemplate);
    }

    @Test
    public void testFullSearch() {
        findOffersAndValidateFilter(new OffersFilter());
    }

    @Test
    public void testSupplierId() {
        findOffersAndValidateFilter(new OffersFilter().addBusinessId(42));
    }

    @Test
    public void testLastVersionAfter() {
        long lastVersion = initOffers.get(2).getLastVersion();
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setLastVersionAfter(lastVersion));
        offers.forEach(o -> assertThat(o.getLastVersion(), Matchers.greaterThan(lastVersion)));
    }

    @Test
    public void testOfferIds() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setOfferIds(2, 3));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    public void testGetBySingleId() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setOfferId(2));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(2L);
    }

    @Test
    public void testGetBySingleUnexistingId() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setOfferId(123456));
        Assertions.assertThat(offers).isEmpty();
    }

    @Test
    public void testBusinessSkuKeys() {
        List<BusinessSkuKey> keys = List.of(new BusinessSkuKey(42, "sku1"), new BusinessSkuKey(42, "sku3"));
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setBusinessSkuKeysInternal(keys));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    public void testSearch() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setSearch("search"));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    public void testSearchByShopSkuList() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setSearch("sku2 sku3  sku4  [[["));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    public void testSearchByShopSku() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setSearch(" sku1  "));
        Assertions.assertThat(offers)
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(1L);
    }

    @Test
    public void testSearchByRealSupplierIdAndShopSkuList() {
        List<Offer> offers = repository.findOffers(new OffersFilter().setSearch(
            "sku2  000042.sku17 000042.sku5 with-dot.assortiment"));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(2L, 17L, 5L, 20L);
    }

    @Test
    public void testSearchByRealSupplierIdAndShopSku() {
        List<Offer> offers = repository.findOffers(new OffersFilter().setSearch("  000042.sku5"));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(5L);
    }

    @Test
    public void testSearchById() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setSearch("6"));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(6L);
    }

    @Test
    public void testSearchByTrackerTicket() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setSearch("MCPTEST-20"));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(6L);
    }

    @Test
    public void testAcceptance() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setAcceptanceStatus(Offer.AcceptanceStatus.TRASH));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactlyInAnyOrder(4L, 5L, 8L);
    }

    @Test
    public void testReadAllOffers() {
        List<Offer> loaded = new ArrayList<>();
        repository.findOffers(new OffersFilter(), loaded::add);
        assertEquals(initOffers.size(), loaded.size());
    }

    @Test
    public void testReadAllOffersLimit() {
        List<Offer> loaded = new ArrayList<>();
        repository.findOffers(new OffersFilter().setLimit(2), loaded::add);
        Assertions.assertThat(loaded).hasSize(2);
    }

    @Test
    public void testReadAllOffersUpdatedAfter() {
        List<Offer> loaded = new ArrayList<>();
        initOffers.sort(Comparator.comparing(Offer::getLastVersion));
        repository.findOffers(
            new OffersFilter().setLastVersionAfter(initOffers.get(1).getLastVersion()), loaded::add
        );
        Assertions.assertThat(loaded).hasSize(initOffers.size() - 2);
    }

    @Test
    public void testFindOffersShortInfo() {
        List<OfferShortInfo> loaded = new ArrayList<>();
        repository.findShortOfferInfo(new OffersFilter().setOfferId(1L), loaded::add);
        Assertions.assertThat(loaded).hasSize(1);
    }

    @Test
    public void testFindOffersLite() {
        List<OfferLite> loaded = new ArrayList<>();
        repository.findOffersLite(new OffersFilter().setLimit(1), loaded::add);
        Assertions.assertThat(loaded).hasSize(1);
    }

    @Test
    public void testReadAllOffersUpdatedAfterLimit() {
        List<Offer> loaded = new ArrayList<>();
        initOffers.sort(Comparator.comparing(Offer::getLastVersion));
        repository.findOffers(
            new OffersFilter()
                .setLastVersionAfter(initOffers.get(1).getLastVersion())
                .setLimit(1),
            loaded::add);
        Assertions.assertThat(loaded).hasSize(1);
    }

    @Test
    public void testFilterByProcessingStatus() {
        List<Offer> openOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.OPEN));
        Assertions.assertThat(openOffers).hasSize(6);

        List<Offer> classificationOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.IN_CLASSIFICATION));
        Assertions.assertThat(classificationOffers).hasSize(2);

        List<Offer> classifiedOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.CLASSIFIED));
        Assertions.assertThat(classifiedOffers).hasSize(2);

        List<Offer> processingOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.IN_PROCESS));
        Assertions.assertThat(processingOffers).hasSize(2);

        List<Offer> processedOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.PROCESSED));
        Assertions.assertThat(processedOffers).hasSize(2);

        List<Offer> reopenOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.REOPEN));
        Assertions.assertThat(reopenOffers).hasSize(3);

        List<Offer> severalOffers = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.IN_PROCESS, Offer.ProcessingStatus.PROCESSED));
        Assertions.assertThat(severalOffers).extracting(Offer::getId)
            .containsExactlyInAnyOrder(processingOffers.get(0).getId(), processingOffers.get(1).getId(),
                processedOffers.get(0).getId(), processedOffers.get(1).getId());

        List<Offer> inModerationRejected = findOffersAndValidateFilter(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.IN_MODERATION_REJECTED));
        Assertions.assertThat(inModerationRejected).hasSize(2);
    }

    @Test
    public void testOrderBy() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID, OffersFilter.OrderType.DESC));

        for (int i = 0; i < offers.size() - 1; i++) {
            Offer offer = offers.get(i);
            Offer next = offers.get(i + 1);
            Assertions.assertThat(offer.getId()).isGreaterThanOrEqualTo(next.getId());
        }
    }

    @Test
    public void testMultipleOrders() {
        List<Offer> offers1 = findOffersAndValidateFilter(new OffersFilter()
            .setOrderBy(
                OffersFilter.Field.SUPPLIER_ID, OffersFilter.OrderType.ASC,
                OffersFilter.Field.ID, OffersFilter.OrderType.DESC
            ));
        Assertions.assertThat(offers1).extracting(Offer::getId)
            .containsExactly(11L, 19L, 3L, 2L, 1L, 24L, 23L, 22L, 21L, 18L, 16L, 15L, 14L, 13L, 12L, 17L, 5L, 6L, 10L,
                9L, 8L, 7L, 20L, 4L);
    }

    @Test
    public void testLimit() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setLimit(2)
            .setOrderBy(OffersFilter.Field.ID));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactly(1L, 2L);
    }

    @Test
    public void testOffset() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setOffset(1)
            .setOrderBy(OffersFilter.Field.ID));
        Assertions.assertThat(offers).extracting(Offer::getId).startsWith(2L);
    }

    @Test
    public void testLimitOffset() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setLimit(2)
            .setOffset(1)
            .setOrderBy(OffersFilter.Field.ID));
        Assertions.assertThat(offers).extracting(Offer::getId).containsExactly(2L, 3L);
    }

    @Test
    public void testSearchingByTrackerTickets() {
        List<Offer> singleSearch = findOffersAndValidateFilter(
            new OffersFilter().setTrackerTickets("MCPTEST-10"));
        Assertions.assertThat(singleSearch).extracting(Offer::getId).containsExactlyInAnyOrder(3L, 5L);

        List<Offer> severalSearch = findOffersAndValidateFilter(
            new OffersFilter().setTrackerTickets("MCPTEST-10", "MCPTEST-20"));
        Assertions.assertThat(severalSearch).extracting(Offer::getId).containsExactlyInAnyOrder(3L, 5L, 6L);

        List<Offer> searchWithNotExistingTicket = findOffersAndValidateFilter(
            new OffersFilter().setTrackerTickets("MBO-1", "MCPTEST-20"));
        Assertions.assertThat(searchWithNotExistingTicket).extracting(Offer::getId).containsExactlyInAnyOrder(6L);

        List<Offer> emptySearch = findOffersAndValidateFilter(
            new OffersFilter().setTrackerTickets());
        Assertions.assertThat(emptySearch).isEmpty();
    }

    @Test
    public void testFilteringByMappingDestination() {
        List<Offer> uploadedToUc = findOffersAndValidateFilter(
            new OffersFilter().setMappingDestination(Offer.MappingDestination.BLUE)
        );
        Assertions.assertThat(uploadedToUc).extracting(Offer::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);

        List<Offer> notUploadedToUc = findOffersAndValidateFilter(
            new OffersFilter().setMappingDestination(Offer.MappingDestination.WHITE));
        Assertions.assertThat(notUploadedToUc).extracting(Offer::getId).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    public void testFilteringByOfferDestination() {
        List<Offer> uploadedToUc = findOffersAndValidateFilter(
            new OffersFilter().setOfferDestination(Offer.MappingDestination.BLUE)
        );
        Assertions.assertThat(uploadedToUc).extracting(Offer::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
    }

    @Test
    public void testSearchByMinMaxId() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().setMinIdInclusive(3L));
        Assertions.assertThat(offers).hasSize(22);

        offers = findOffersAndValidateFilter(new OffersFilter().setMaxIdInclusive(3L));
        Assertions.assertThat(offers).hasSize(3);

        offers = findOffersAndValidateFilter(new OffersFilter().setMinIdInclusive(2L).setMaxIdInclusive(3L));
        Assertions.assertThat(offers).hasSize(2);
    }

    @Test
    public void testSkuGreater() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter().addCriteria(
            new SupplierKeyGreaterCriteria(79, "sku7")));
        Assertions.assertThat(offers).hasSize(4);

        offers = findOffersAndValidateFilter(new OffersFilter().addCriteria(
            new SupplierKeyGreaterCriteria(78, "sku9")));
        Assertions.assertThat(offers).hasSize(6);
    }

    @Test
    public void testFindByApprovedMappings() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setMarketSkuSearch(new OffersFilter.MarketSkuSearch(Collections.singleton(101010L), true, false)));
        Assertions.assertThat(offers).extracting(Offer::getId)
            .containsExactlyInAnyOrder(4L, 5L, 17L);
    }

    @Test
    public void testFindSomethingWithForUpdate() {
        List<Offer> offers = repository.findOffers(new OffersFilter()
                .setMarketSkuSearch(new OffersFilter.MarketSkuSearch(Collections.singleton(101010L), true, false)),
            true);
        Assertions.assertThat(offers).extracting(Offer::getId)
            .containsExactlyInAnyOrder(4L, 5L, 17L);
    }

    @Test
    public void testFindBySupplierMappings() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setMarketSkuSearch(new OffersFilter.MarketSkuSearch(Collections.singleton(101010L), false, true)));
        Assertions.assertThat(offers).hasSize(1);
        Assertions.assertThat(offers.get(0).getId()).isEqualTo(5);
    }

    @Test
    public void testFindBySuggestMappings() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setMarketSkuSearch(new OffersFilter.MarketSkuSearch(
                Collections.singleton(101010L), false, false, true, false)));
        Assertions.assertThat(offers).hasSize(1);
        Assertions.assertThat(offers.get(0).getId()).isEqualTo(17);
    }

    @Test
    public void testFindByApprovedSupplierSuggestMappings() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setMarketSkuSearch(new OffersFilter.MarketSkuSearch(
                Collections.singleton(101010L), true, true, true, false)));
        Assertions.assertThat(offers)
            .extracting(Offer::getId)
            .containsExactly(4L, 5L, 17L);
    }

    @Test
    public void testMatchingOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forMatching())
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(6L, 9L, 13L, 14L, 16L, 17L, 18L, 19L, 20L));
    }


    @Test
    public void testMatchingFromModerationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forMatchingFromModeration())
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(Collections.singletonList(18L));
    }

    @Test
    public void testMatchingFromClassificationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forMatchingFromClassification())
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(List.of(9L));
    }

    @Test
    public void testClassificationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forClassification(false, false))
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(1L, 2L, 3L, 7L, 12L, 15L));
    }

    @Test
    public void testForcedClassificationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forClassificationForced())
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(List.of(1L, 10L, 16L, 20L));
    }

    @Test
    public void testClassificationFromModerationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forClassificationFromModeration(false, false))
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .isEqualTo(Collections.singletonList(15L));
    }

    @Test
    public void testModerationOfferCriteria() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(OfferCriterias.forModeration())
            .setOrderBy(OffersFilter.Field.ID));

        Assertions.assertThat(offers.stream().map(Offer::getId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(12L, 20L);
    }

    @Test
    public void testFindSupplierType() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(new SupplierTypeCriteria(supplierRepository, MbocSupplierType.FIRST_PARTY)));
        Assertions.assertThat(offers).hasSize(1);
        Assertions.assertThat(offers.get(0).getId()).isEqualTo(4);
    }

    @Test
    public void testFindNotSupplierType() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .addCriteria(new NotCriteria<>(
                new SupplierTypeCriteria(
                    supplierRepository, MbocSupplierType.FIRST_PARTY, MbocSupplierType.THIRD_PARTY))));
        Assertions.assertThat(offers)
            .extracting(Offer::getId)
            .containsExactly(5L, 17L);
    }

    @Test
    public void testFindTestSuppliers() {
        List<Offer> offers = findOffersAndValidateFilter(new OffersFilter()
            .addCriteria(new TestSupplierCriteria(supplierRepository)));
        Assertions.assertThat(offers).hasSize(1);
        Assertions.assertThat(offers.get(0).getId()).isEqualTo(11);
    }

    @Test
    public void testCategoryStat() {
        OffersFilter filter = new OffersFilter().addBusinessId(42);
        List<OfferRepository.CategoryStat> stat = repository.countCategoryStat(filter);
        List<OfferRepository.CategoryStat> mocked = repositoryMock.countCategoryStat(filter);

        Assertions.assertThat(stat).usingElementComparatorIgnoringFields()
            .containsExactly(new OfferRepository.CategoryStat(12, 2, 1),
                new OfferRepository.CategoryStat(33, 2, 0));

        Assertions.assertThat(mocked).usingElementComparatorIgnoringFields()
            .containsExactly(new OfferRepository.CategoryStat(12, 2, 1),
                new OfferRepository.CategoryStat(33, 2, 0));
    }

    @Test
    public void testSupplierStat() {
        OffersFilter filter = new OffersFilter().addCriteria(OfferCriterias
            .supplierIdInCriteria(Arrays.asList(42, 465852)));
        List<OfferRepository.SupplierStat> stat = repository.countSupplierStat(filter, false);
        List<OfferRepository.SupplierStat> mocked = repositoryMock.countSupplierStat(filter, false);

        LocalDateTime statTs = LocalDateTime.of(2017, 10, 28, 10, 15, 20);

        Assertions.assertThat(stat).usingElementComparatorIgnoringFields()
            .containsExactly(
                new OfferRepository.SupplierStat(42, 4, statTs),
                new OfferRepository.SupplierStat(465852, 1, statTs));

        Assertions.assertThat(mocked).usingElementComparatorIgnoringFields()
            .containsExactly(
                new OfferRepository.SupplierStat(42, 4, statTs),
                new OfferRepository.SupplierStat(465852, 1, statTs));
    }

    @Test
    public void testShopSkuListDifferentSupplierIds() {
        List<Offer> offers = repository.findOffersByBusinessSkuKeys(
            Arrays.asList(new BusinessSkuKey(42, "sku1"), new BusinessSkuKey(77, "sku5")));

        Assertions.assertThat(offers).hasSize(2).extracting(Offer::getId).containsExactlyInAnyOrder(1L, 5L);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLargeShopSkuList() {
        List<Offer> offers = IntStream.range(1, 2003)
            .mapToObj(n -> new Offer()
                .setTitle("Offer #" + n)
                .setShopCategoryName(n % 2 == 0 ? "good" : "bad")
                .setBusinessId(42)
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.initEmptyContent())
                .setShopSku("shop-sku-" + n))
            .collect(Collectors.toList());

        repository.insertOffers(offers);

        var shopSkus = offers.stream().map(Offer::getBusinessSkuKey).collect(Collectors.toList());

        Mockito.reset(jdbcSpy);
        List<Offer> result = repository.findOffersByBusinessSkuKeys(
            new OffersFilter().setSearch("good").setFetchOfferContent(true), shopSkus);

        Assertions.assertThat(result).hasSize(1001);
        Assertions.assertThat(result).extracting(offer -> offer.getShopCategoryName())
            .containsOnly("good");

        // Searching for 1000 + 1000 + 2 shop_sku_key pairs, verify query count
        Mockito.verify(jdbcSpy, Mockito.times(3))
            .query(Mockito.anyString(), Mockito.anyMap(), (RowMapper<Object>) Mockito.any());
    }

    @Test
    public void testOffersCategories() {
        // проверяем, что в репозитории действительно есть офферы с непроставленной категорией
        Assertions.assertThat(initOffers).extracting(Offer::getCategoryId).containsNull();

        List<Long> categories = repository.getOffersCategories();
        List<Long> mocked = repositoryMock.getOffersCategories();

        Assertions.assertThat(mocked)
            .containsExactlyInAnyOrderElementsOf(categories)
            .containsExactlyInAnyOrder(12L, 22L, 33L, 1299L);
    }

    @Test
    public void testSetByLastShopSkuKey() {
        OffersFilter filter = new OffersFilter()
            .setLastShopSkuKey(new ShopSkuKey(43, "sku4"))
            .setOrderBy(OffersFilter.Field.SUPPLIER_ID, OffersFilter.OrderType.ASC,
                OffersFilter.Field.SHOP_SKU, OffersFilter.OrderType.ASC)
            .setLimit(2);
        Collection<Offer> result = repository.findOffers(filter);
        Assertions.assertThat(result)
            .extracting(Offer::getShopSkuKey)
            .containsExactly(
                new ShopSkuKey(77, "sku17"),
                new ShopSkuKey(77, "sku5")
            );

        filter = new OffersFilter()
            .setLastShopSkuKey(new ShopSkuKey(78, "sku6"))
            .setOrderBy(OffersFilter.Field.SUPPLIER_ID, OffersFilter.OrderType.ASC,
                OffersFilter.Field.SHOP_SKU, OffersFilter.OrderType.ASC);
        result = repository.findOffers(filter);
        Assertions.assertThat(result)
            .extracting(Offer::getShopSkuKey)
            .containsExactly(
                new ShopSkuKey(79, "sku10"),
                new ShopSkuKey(79, "sku7"),
                new ShopSkuKey(79, "sku8"),
                new ShopSkuKey(79, "sku9"),
                new ShopSkuKey(99, "with-dot.assortiment"),
                new ShopSkuKey(465852, "sku4")
            );

        filter = new OffersFilter()
            .setLastShopSkuKey(new ShopSkuKey(465852, "sku4"))
            .setOrderBy(OffersFilter.Field.SUPPLIER_ID, OffersFilter.OrderType.ASC,
                OffersFilter.Field.SHOP_SKU, OffersFilter.OrderType.ASC);
        result = repository.findOffers(filter);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void testUseLastShopSkuKey() {
        repository.deleteAllInTest();

        repository.insertOffers(
            createOffer(1, "ssku-1"),
            createOffer(1, "ssku-2"),
            createOffer(1, "ssku-3"),
            createOffer(2, "ssku-1"),
            createOffer(2, "ssku-2"),
            createOffer(2, "ssku-3"),
            createOffer(3, "ssku-1"),
            createOffer(3, "ssku-2"),
            createOffer(3, "ssku-3")
        );

        var filter = new OffersFilter()
            .setOrderBy(
                OffersFilter.Field.SUPPLIER_ID, OffersFilter.OrderType.ASC,
                OffersFilter.Field.SHOP_SKU, OffersFilter.OrderType.ASC
            );

        var result = repository.findOffers(filter.setLastShopSkuKey(new ShopSkuKey(2, "ssku-2")));
        Assertions.assertThat(result)
            .extracting(Offer::getBusinessSkuKey)
            .containsExactly(
                new BusinessSkuKey(2, "ssku-3"),
                new BusinessSkuKey(3, "ssku-1"),
                new BusinessSkuKey(3, "ssku-2"),
                new BusinessSkuKey(3, "ssku-3")
            );


        result = repository.findOffers(filter.setLastShopSkuKey(new ShopSkuKey(3, "ssku-1")));
        Assertions.assertThat(result)
            .extracting(Offer::getBusinessSkuKey)
            .containsExactly(
                new BusinessSkuKey(3, "ssku-2"),
                new BusinessSkuKey(3, "ssku-3")
            );

        result = repository.findOffers(filter.setLastShopSkuKey(new ShopSkuKey(2, "ssku-3")));
        Assertions.assertThat(result)
            .extracting(Offer::getBusinessSkuKey)
            .containsExactly(
                new BusinessSkuKey(3, "ssku-1"),
                new BusinessSkuKey(3, "ssku-2"),
                new BusinessSkuKey(3, "ssku-3")
            );

        result = repository.findOffers(filter.setLastShopSkuKey(new ShopSkuKey(3, "ssku-3")));
        Assertions.assertThat(result).isEmpty();
    }

    private Offer createOffer(int businessId, String shopSku) {
        return new Offer().setBusinessId(businessId).setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setShopCategoryName("Category " + 1)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setVendor("Vendor: " + 1)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(1, DateTimeUtils.dateTimeNow()), CONTENT)
            .setServiceOffers(List.of(new Offer.ServiceOffer(businessId)));
    }

    private List<Offer> findOffersAndValidateFilter(OffersFilter filter) {
        //  force order by to compare lists instead of sets
        if (filter.getOrders().isEmpty()) {
            filter.setOrderBy(OffersFilter.Field.ID);
        }
        List<Offer> repositoryOffers = repository.findOffers(filter);
        List<Offer> filterOffers = repositoryMock.findOffers(filter);

        assertEquals("ids must be the same in repo and in filter search",
            repositoryOffers.stream().map(Offer::getId).collect(Collectors.toList()),
            filterOffers.stream().map(Offer::getId).collect(Collectors.toList()));
        return repositoryOffers;
    }
}
