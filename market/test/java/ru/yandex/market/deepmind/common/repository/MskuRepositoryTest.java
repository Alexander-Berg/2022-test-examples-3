package ru.yandex.market.deepmind.common.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.MappingExistence;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.jooq.repo.Sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.services.MappingExistence.HAS_MAPPING;
import static ru.yandex.market.deepmind.common.services.MappingExistence.NO_MAPPING;

public class MskuRepositoryTest extends DeepmindBaseDbTestClass {

    private static final int TEST_DATA_COUNT = 1;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MskuRepository deepmindMskuRepository;

    @Autowired
    private MskuStatusRepository mskuStatusRepository;

    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    @Autowired
    private SupplierRepository deepmindSupplierRepository;

    @Autowired
    private MskuStockRepository mskuStockRepository;
    private EnhancedRandom random;

    @Before
    public void setUpRandom() {
        random = TestUtils.createMskuRandom();
    }

    @Test
    public void filterByCategory() {
        List<Msku> hasCategory = TestUtils.randomMskuStream(random, TEST_DATA_COUNT)
            .peek(m -> m.setCategoryId(1L))
            .collect(Collectors.toList());

        List<Msku> hasNoCategory = TestUtils.randomMskuList(random, TEST_DATA_COUNT);

        deepmindMskuRepository.save(hasCategory);
        deepmindMskuRepository.save(hasNoCategory);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setCategoryIds(1L));

        assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasCategory);
    }

    @Test
    public void filterByCategories() {
        Map<Long, Msku> hasCategory = IntStream.range(0, TEST_DATA_COUNT)
            .mapToObj(i -> {
                Msku msku = TestUtils.randomMsku(random);
                msku.setCategoryId(TestUtils.CATEGORY_ID_MAX_VALUE + msku.getCategoryId());
                return msku;
            })
            .collect(Collectors.toMap(Msku::getCategoryId, Function.identity()));

        List<Msku> hasNoCategory = TestUtils.randomMskuList(random, TEST_DATA_COUNT);

        deepmindMskuRepository.save(hasCategory.values());
        deepmindMskuRepository.save(hasNoCategory);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setCategoryIds(hasCategory.values().stream().map(Msku::getCategoryId).collect(Collectors.toList())));

        assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrderElementsOf(hasCategory.values());
    }

    @Test
    public void filterBySearchText() {
        List<Msku> values = LongStream.range(1, 1000)
            .mapToObj(i -> TestUtils.randomMsku(random).setId(i).setTitle("title: " + i))
            .collect(Collectors.toList());
        deepmindMskuRepository.save(values);

        // 99, 199, 299, ...990, 991, .. 999
        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setSearchText("99"));
        assertThat(found).hasSize(19);
    }

    @Test
    public void filterBySearchTextInId() {
        List<Msku> values = LongStream.range(1, 1000)
            .mapToObj(i -> TestUtils.randomMsku(random).setId(i).setTitle("title"))
            .collect(Collectors.toList());
        deepmindMskuRepository.save(values);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setSearchText("99 199", true));
        assertThat(found).hasSize(2);
    }

    @Test
    public void filterBySearchTextInId2() {
        List<Msku> values = LongStream.range(1, 1000)
            .mapToObj(i -> TestUtils.randomMsku(random).setId(i).setTitle("title"))
            .collect(Collectors.toList());
        deepmindMskuRepository.save(values);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setSearchText("99,199 299", true));
        assertThat(found).hasSize(3);
    }

    @Test
    public void filterByMskuId() {
        Msku msku = deepmindMskuRepository.save(TestUtils.randomMsku(random));

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setMarketSkuIds(msku.getId()));
        assertThat(found).containsExactlyInAnyOrder(
            msku
        );
    }

    @Test
    public void filterByHasMapping() {
        List<Msku> hasApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasApprovedMapping);
        saveOffers(hasApprovedMapping);

        List<Msku> hasNoApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasNoApprovedMapping);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter().setMappingExistence(HAS_MAPPING));

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasApprovedMapping);
    }

    @Test
    public void filterByHasNoMapping() {
        List<Msku> hasApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);

        deepmindMskuRepository.save(hasApprovedMapping);
        saveOffers(hasApprovedMapping);

        List<Msku> hasNoApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasNoApprovedMapping);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter().setMappingExistence(NO_MAPPING));

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasNoApprovedMapping);
    }

    @Test
    public void filterByHasMappingOnStock() {
        List<Msku> hasApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasApprovedMapping);
        saveOffers(hasApprovedMapping);

        List<Msku> hasMappingInStock = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasMappingInStock);
        saveOffersWithStocks(random, hasMappingInStock, stockInfo -> stockInfo.setFitInternal(5));

        List<Msku> hasNoApprovedMapping = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasNoApprovedMapping);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter()
            .setMappingExistence(MappingExistence.HAS_MAPPING_ON_STOCK));

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasMappingInStock);
    }

    @Test
    public void filterByHasStatus() {
        List<Msku> hasStatus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasStatus);

        TestUtils.insertStatusesForMsku(random, hasStatus, seasonRepository, mskuStatusRepository);

        List<Msku> hasNoStatus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasNoStatus);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter().setHasStatus(true));

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasStatus);
    }

    @Test
    public void filterByHasNoStatus() {
        List<Msku> hasStatus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasStatus);

        TestUtils.insertStatusesForMsku(random, hasStatus, seasonRepository, mskuStatusRepository);

        List<Msku> hasNoStatus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(hasNoStatus);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter().setHasStatus(false));

        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(hasNoStatus);
    }

    @Test
    public void filterByStatus() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku3 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku4 = deepmindMskuRepository.save(TestUtils.randomMsku(random));

        mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku1.getId())
            .setMskuStatus(MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now()));
        mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku2.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));
        mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku3.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE));

        List<Msku> actual = deepmindMskuRepository.find(new MskuFilter()
            .setMskuStatusValues(MskuStatusValue.NPD, MskuStatusValue.REGULAR));

        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(msku1, msku2);
    }

    @Test
    public void filterBySeasonIds() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku3 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku4 = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku msku5 = deepmindMskuRepository.save(TestUtils.randomMsku(random));

        Season season1 = seasonRepository.save(new Season().setName("весна-лето"));
        Season season2 = seasonRepository.save(new Season().setName("осень-зима"));
        Season season3 = seasonRepository.save(new Season().setName("Распродажа"));

        MskuStatus status1 = mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku1.getId())
            .setMskuStatus(MskuStatusValue.SEASONAL)
            .setSeasonId(season1.getId()));
        MskuStatus status2 = mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku2.getId())
            .setMskuStatus(MskuStatusValue.SEASONAL)
            .setSeasonId(season2.getId()));
        MskuStatus status3 = mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku3.getId())
            .setMskuStatus(MskuStatusValue.SEASONAL)
            .setSeasonId(season3.getId()));
        MskuStatus status4 = mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku4.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));

        List<Msku> actual = deepmindMskuRepository.find(new MskuFilter()
            .setSeasonIds(season1.getId(), season2.getId()));

        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(msku1, msku2);
    }

    @Test
    public void testFilterBySupplierId() {
        List<Msku> mskus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(mskus);
        List<ServiceOfferReplica> offers = saveOffers(mskus);
        Set<Integer> suppliers = offers.stream().map(ServiceOfferReplica::getBusinessId).collect(Collectors.toSet());

        List<Msku> mskusWithoutOffers = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(mskusWithoutOffers);

        List<Msku> found = deepmindMskuRepository.find(new MskuFilter().setSupplierIds(suppliers));
        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsOnlyElementsOf(mskus);
    }

    @Test
    public void testSorting() {
        List<Msku> mskus = TestUtils.randomMskuList(random, TEST_DATA_COUNT);
        deepmindMskuRepository.save(mskus);

        List<Msku> fromDb = deepmindMskuRepository
            .find(new MskuFilter(), Sorting.asc(MskuRepository.SortBy.TITLE), OffsetFilter.all());

        String lastTitle = null;
        for (Msku msku : fromDb) {
            if (lastTitle == null) {
                lastTitle = msku.getTitle();
            }
            Assertions.assertThat(msku.getTitle().compareTo(lastTitle)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    public void shouldReturnNotDeletedMskus() {
        Msku saved = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        Msku deleted = deepmindMskuRepository.save(TestUtils.randomMsku(random));
        deepmindMskuRepository.delete(deleted.getId());
        deleted = deepmindMskuRepository.getById(deleted.getId());

        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter()
            .setMarketSkuIds(saved.getId(), deleted.getId()));
        Assertions.assertThat(mskus).containsExactlyInAnyOrder(saved);
    }

    private List<ServiceOfferReplica> saveOffers(Collection<Msku> mskus) {
        var offers =  mskus.stream()
            .map(m -> TestUtils.createOffer(Math.toIntExact(m.getId()), "ssku " + m.getId(), m.getId()))
            .collect(Collectors.toList());
        List<Supplier> suppliers = offers.stream()
            .map(ServiceOfferReplica::getBusinessId)
            .distinct()
            .map(id -> new Supplier().setId(id).setName("Supplier " + id))
            .collect(Collectors.toList());
        deepmindSupplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(offers);
        return offers;
    }

    private List<MskuStockInfo> saveOffersWithStocks(EnhancedRandom random,
                                                     Collection<Msku> mskus, Consumer<MskuStockInfo> consumer) {
        var offers =  mskus.stream()
            .map(m -> TestUtils.createOffer(Math.toIntExact(m.getId()), "ssku " + m.getId(), m.getId()))
            .collect(Collectors.toList());
        List<Supplier> suppliers = offers.stream()
            .map(ServiceOfferReplica::getBusinessId)
            .distinct()
            .map(id -> new Supplier().setId(id).setName("Supplier " + id))
            .collect(Collectors.toList());
        deepmindSupplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(offers);
        List<MskuStockInfo> stocks = offers.stream()
            .map(o -> new MskuStockInfo()
                .setSupplierId(o.getBusinessId())
                .setShopSku(o.getShopSku())
                .setWarehouseId(random.nextInt())
            )
            .peek(consumer)
            .collect(Collectors.toList());
        mskuStockRepository.insertBatch(stocks);
        return stocks;
    }
}
