package ru.yandex.market.deepmind.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;

import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.07.19
 */
public class TestUtils {
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
            // season
            .exclude(new FieldDefinition<>("modifiedAt", LocalDateTime.class, Season.class))
            // msku status
            .exclude(new FieldDefinition<>("modifiedAt", LocalDateTime.class, MskuStatus.class))
            // msku
            .exclude(new FieldDefinition<>("deleted", Boolean.class, Msku.class))
            .exclude(new FieldDefinition<>("cargoTypes", Long[].class, Msku.class))
            .exclude(new FieldDefinition<>("modifiedTs", LocalDateTime.class, Msku.class))
            // randomizer
            .randomize(Long.class, new LongRangeRandomizer(1L, 1000L, seed))
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100, seed))
            .randomize(new FieldDefinition<>("categoryId", Long.class, Msku.class),
                new LongRangeRandomizer(1L, CATEGORY_ID_MAX_VALUE, seed))
            .dateRange(LocalDate.of(2007, Month.JANUARY, 1),
                LocalDate.of(2080, Month.DECEMBER, 31))
            .build();
    }

    public static void insertStatusesForMsku(
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
            .map(m -> randomMskuStatus(random).setMarketSkuId(m.getId()))
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
        MskuRepository deepmindMskuRepository
    ) {
        return deepmindMskuRepository.save(msku);
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
    }

    public static List<ServiceOfferReplica> insertApprovedMappingsForMsku(
        EnhancedRandom random,
        List<Msku> msku,
        SupplierRepository supplierRepository,
        ServiceOfferReplicaRepository serviceOfferReplicaRepository
    ) {
        List<ServiceOfferReplica> offers = msku.stream()
            .map(m -> offer(random.nextInt(), m.getId()))
            .collect(Collectors.toList());

        List<Supplier> suppliers = offers.stream()
            .map(ServiceOfferReplica::getBusinessId)
            .distinct()
            .map(id -> new Supplier().setId(id).setName("Supplier " + id))
            .collect(Collectors.toList());

        supplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(offers);
        return offers;
    }

    public static List<MskuStockInfo> insertStocksForMsku(
        EnhancedRandom random,
        List<Msku> msku,
        Consumer<MskuStockInfo> consumer,
        SupplierRepository supplierRepository,
        MskuStockRepository mskuStockRepository,
        ServiceOfferReplicaRepository serviceOfferReplicaRepository
    ) {
        List<ServiceOfferReplica> offers = msku.stream()
            .map(m -> offer(random.nextInt(), m.getId()))
            .collect(Collectors.toList());

        List<Supplier> suppliers = offers.stream()
            .map(ServiceOfferReplica::getBusinessId)
            .distinct()
            .map(id -> new Supplier().setId(id).setName("Supplier " + id))
            .collect(Collectors.toList());

        List<MskuStockInfo> stocks = offers.stream()
            .map(o -> new MskuStockInfo()
                .setSupplierId(o.getBusinessId())
                .setShopSku(o.getShopSku())
                .setWarehouseId(random.nextInt())
            )
            .peek(consumer)
            .collect(Collectors.toList());

        supplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(offers);
        mskuStockRepository.insertBatch(stocks);

        return stocks;
    }

    private static ServiceOfferReplica offer(int supplierId, long mskuId) {
        var shopSku = "shop-sku-" + supplierId;
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    public static Msku randomMsku(EnhancedRandom random) {
        return random.nextObject(Msku.class).setDeleted(false);
    }

    public static MskuStatus randomMskuStatus(EnhancedRandom random) {
        return random.nextObject(MskuStatus.class);
    }

    public static Stream<Msku> randomMskuStream(EnhancedRandom random, int count) {
        return randomMskuStream(random, random.nextObject(Long.class), count);
    }

    public static Stream<Msku> randomMskuStream(EnhancedRandom random, long startId, int count) {
        return LongStream.range(startId, startId + count)
            .mapToObj(i -> randomMsku(random).setId(i));
    }

    public static List<Msku> randomMskuList(EnhancedRandom random, long startId, int count) {
        return randomMskuStream(random, startId, count).collect(Collectors.toList());
    }

    public static List<Msku> randomMskuList(EnhancedRandom random, int count) {
        return randomMskuStream(random, count).collect(Collectors.toList());
    }

    public static MskuAvailabilityMatrix mskuAvailability(
        long mskuId, long warehouseId, String from, String to, boolean available
    ) {
        MskuAvailabilityMatrix result = new MskuAvailabilityMatrix()
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setCreatedLogin("test");

        if (from != null) {
            result.setFromDate(LocalDate.parse(from));
        }
        if (to != null) {
            result.setToDate(LocalDate.parse(to));
        }
        return result;
    }

    public static Msku newMsku(long id) {
        return newMsku(id, SkuTypeEnum.SKU);
    }

    public static Msku newMsku(long id, SkuTypeEnum skuType) {
        return newMsku(id, DEFAULT_CATEGORY_ID, skuType);
    }

    public static Msku newMsku(long id, long categoryId, SkuTypeEnum skuType) {
        return new Msku()
            .setId(id)
            .setCategoryId(categoryId)
            .setTitle("MSKU #" + id)
            .setVendorId(DEFAULT_VENDOR_ID)
            .setModifiedTs(Instant.now())
            .setSkuType(skuType)
            .setDeleted(false);
    }

    public static Msku newMsku(long id, long categoryId) {
        return new Msku()
            .setId(id)
            .setCategoryId(categoryId)
            .setTitle("MSKU #" + id)
            .setVendorId(DEFAULT_VENDOR_ID)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.SKU)
            .setDeleted(false);
    }

    public static Msku newMsku(long mskuId, long categoryId, List<Long> cargoTypes) {
        Long[] cargo = null;
        if (!cargoTypes.isEmpty()) {
            cargo = cargoTypes.toArray(new Long[0]);
        }
        return newMsku(mskuId, categoryId).setCargoTypes(cargo);
    }

    public static ServiceOfferReplica createOffer(int supplierId, String ssku, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
