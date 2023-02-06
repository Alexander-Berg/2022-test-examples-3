package ru.yandex.market.mboc.common.msku;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.bolts.function.Function;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.availability.msku.MskuShortInfo;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.stockstorage.MbocStockInfo;
import ru.yandex.market.mboc.common.dict.stockstorage.MbocStockRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.07.19
 */
public class TestUtils {
    public static final Function<Long, String> TEST_MAPPING_NAME_BUILDER = (id) -> String.format("TEST NAME #%d", id);

    public static final long DEFAULT_CATEGORY_ID = 100;
    public static final long DEFAULT_VENDOR_ID = 200;
    public static final long CATEGORY_ID_MAX_VALUE = 100L;
    private static final long SEED = 56087496;

    private TestUtils() {
    }

    public static EnhancedRandom createMskuRandom() {
        return createMskuRandom(SEED);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public static EnhancedRandom createMskuRandom(long seed) {
        return new EnhancedRandomBuilder().seed(seed)
            // msku
            .exclude(new FieldDefinition<>("deleted", Boolean.class, Msku.class))
            .exclude(new FieldDefinition<>("mskuParameterValues", MskuParameters.class, Msku.class))
            .exclude(new FieldDefinition<>("cargoTypeLmsIds", Long[].class, Msku.class))
            .exclude(new FieldDefinition<>("modifiedAt", LocalDateTime.class, Msku.class))
            .exclude(new FieldDefinition<>("parameterValuesProto", byte[].class, Msku.class))
            // randomizer
            .randomize(Long.class, new LongRangeRandomizer(1L, 1000L, seed))
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100, seed))
            .randomize(new FieldDefinition<>("categoryId", Long.class, Msku.class),
                new LongRangeRandomizer(1L, CATEGORY_ID_MAX_VALUE, seed))
            .dateRange(LocalDate.of(2007, Month.JANUARY, 1),
                LocalDate.of(2080, Month.DECEMBER, 31))
            .build();
    }

//DEEPMIND-2570: those unused methods blocking moving msku status repository to Deepmind
/*    public static void insertStatusesForMsku(
        EnhancedRandom random,
        List<Msku> msku,
        SeasonRepository seasonRepository,
        MskuStatusRepository mskuStatusRepository
    ) {
        insertStatusesForMsku(random, msku, seasonRepository, mskuStatusRepository, status -> {
        });
    }

    public static void insertStatusesForMsku(
        EnhancedRandom random,
        List<Msku> msku,
        SeasonRepository seasonRepository,
        MskuStatusRepository mskuStatusRepository,
        Consumer<MskuStatus> statusConsumer
    ) {
        List<MskuStatus> statuses = msku.stream()
            .map(m -> randomMskuStatus(random).setMarketSkuId(m.getMarketSkuId()))
            .peek(statusConsumer)
            .collect(Collectors.toList());

        List<Season> seasons = statuses.stream()
            .filter(s -> s.getSeasonId() != null)
            .map(s -> random.nextObject(Season.class).setName("name " + s.getSeasonId()).setId(s.getSeasonId()))
            .collect(Collectors.toList());

        seasonRepository.save(seasons);
        mskuStatusRepository.save(statuses);
    }

    public static List<Msku> insertMsku(
        List<Msku> msku,
        MskuRepository mskuRepository
    ) {
        return mskuRepository.save(msku);
    }

    public static List<Season> insertSeasonsForMskuCategories(
        EnhancedRandom random,
        List<Msku> msku,
        SeasonRepository seasonRepository,
        CategorySettingsRepository categorySettingsRepository
    ) {
        Set<Long> categoryIds = msku.stream().map(Msku::getCategoryId).collect(Collectors.toSet());
        long seasonId = random.nextObject(Long.class);
        List<CategorySettings> settings = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            CategorySettings categorySettings = random.nextObject(CategorySettings.class)
                .setCategoryId(categoryId)
                .setSeasonId(seasonId++);
            settings.add(categorySettings);
        }
        List<Season> seasons = settings.stream()
            .map(s -> random.nextObject(Season.class).setId(s.getSeasonId()))
            .collect(Collectors.toList());

        seasonRepository.save(seasons);
        categorySettingsRepository.save(settings);
        return seasons;
    }*/

    public static List<Offer> insertApprovedMappingsForMsku(
        List<Msku> msku,
        SupplierRepository supplierRepository,
        OfferRepository offerRepository
    ) {
        List<Offer> offers = msku.stream()
            .map(m -> OfferTestUtils.nextOffer()
                .setCategoryIdForTests(m.getCategoryId(), Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(m.getMarketSkuId()),
                    Offer.MappingConfidence.CONTENT)
            )
            .collect(Collectors.toList());

        List<Supplier> suppliers = offers.stream()
            .map(Offer::getBusinessId)
            .distinct()
            .map(id -> new Supplier(id, "Supplier " + id))
            .collect(Collectors.toList());

        supplierRepository.insertOrUpdateAll(suppliers);
        offerRepository.insertOffers(offers);
        return offers;
    }

    public static List<MbocStockInfo> insertStocksForMsku(
        EnhancedRandom random,
        List<Msku> msku,
        Consumer<MbocStockInfo> consumer,
        SupplierRepository supplierRepository,
        MbocStockRepository mbocStockRepository,
        OfferRepository offerRepository
    ) {
        List<Offer> offers = msku.stream()
            .map(m -> OfferTestUtils.nextOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(m.getMarketSkuId()),
                    Offer.MappingConfidence.CONTENT)
            )
            .collect(Collectors.toList());

        List<Supplier> suppliers = offers.stream()
            .map(Offer::getBusinessId)
            .distinct()
            .map(id -> new Supplier(id, "Supplier " + id))
            .collect(Collectors.toList());

        List<MbocStockInfo> stocks = offers.stream()
            .map(o -> new MbocStockInfo()
                .setSupplierId(o.getBusinessId())
                .setShopSku(o.getShopSku())
                .setWarehouseId(random.nextInt())
            )
            .peek(consumer)
            .collect(Collectors.toList());

        supplierRepository.insertOrUpdateAll(suppliers);
        offerRepository.insertOffers(offers);
        mbocStockRepository.insertBatch(stocks);

        return stocks;
    }


    public static Msku randomMsku(EnhancedRandom random) {
        return random.nextObject(Msku.class).setDeleted(false);
    }

    public static Stream<Msku> randomMskuStream(EnhancedRandom random, int count) {
        return randomMskuStream(random, random.nextObject(Long.class), count);
    }

    public static Stream<Msku> randomMskuStream(EnhancedRandom random, long startId, int count) {
        return LongStream.range(startId, startId + count)
            .mapToObj(i -> randomMsku(random).setMarketSkuId(i));
    }

    public static List<Msku> randomMskuList(EnhancedRandom random, long startId, int count) {
        return randomMskuStream(random, startId, count).collect(Collectors.toList());
    }

    public static List<Msku> randomMskuList(EnhancedRandom random, int count) {
        return randomMskuStream(random, count).collect(Collectors.toList());
    }

    public static MskuShortInfo mskuShortInfo(long mskuId, long categoryId) {
        return mskuShortInfo(mskuId, categoryId, List.of());
    }

    public static MskuShortInfo mskuShortInfo(long mskuId, long categoryId, List<Long> cargoTypes) {
        return new MskuShortInfo(mskuId, "MSKU #" + mskuId, categoryId, DEFAULT_VENDOR_ID, cargoTypes);
    }

    public static Msku newMsku(long marketSkuId) {
        return newMsku(marketSkuId, DEFAULT_CATEGORY_ID);
    }

    public static Msku newMsku(long marketSkuId, long categoryId) {
        return new Msku()
            .setMarketSkuId(marketSkuId)
            .setParentModelId(0L)
            .setCategoryId(categoryId)
            .setTitle("MSKU #" + marketSkuId)
            .setVendorId(DEFAULT_VENDOR_ID)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
    }

    public static void mockMskuRepositoryFindTitles(MskuRepository mskuRepository) {
        Mockito.when(mskuRepository.findTitles(Mockito.any())).thenAnswer((Answer<Map<Long, String>>) invocation ->
            ((Collection<Long>) invocation.getArgument(0)).stream().collect(Collectors.toMap(
                it -> it,
                TEST_MAPPING_NAME_BUILDER,
                (n1, n2) -> n1
            )));
        Mockito.when(mskuRepository.findTitlesForOffers(Mockito.any()))
            .thenAnswer((Answer<Map<Long, String>>) invocation -> {
            Collection<Offer> argument = invocation.getArgument(0);
            return argument.stream().flatMap(it -> it.getMappingIds().stream()).collect(Collectors.toMap(
                it -> it,
                TEST_MAPPING_NAME_BUILDER,
                (n1, n2) -> n1
            ));
        });
    }

    public static void mockMskuRepositoryFindForOffers(MskuRepository mskuRepository) {
        Mockito.when(mskuRepository.findForOffers(Mockito.any()))
            .thenAnswer((Answer<List<Msku>>) invocation -> {
                Collection<Offer> argument = invocation.getArgument(0);
                return argument.stream().flatMap(it -> it.getMappingIds().stream())
                    .distinct()
                    .map(id -> newMsku(id).setTitle(TEST_MAPPING_NAME_BUILDER.apply(id)).setCreationTs(Instant.MIN))
                    .collect(Collectors.toList());
            });
    }
}
