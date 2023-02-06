package ru.yandex.market.deepmind.common.availability;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.google.common.collect.Multimap;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.MskuServicesTestUtils;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailabilityByWarehouse;
import ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.availability.SwitchControlServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceImpl;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mboc.common.utils.availability.PeriodResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.MskuServicesTestUtils.nextCategoryAvailabilityMatrix;
import static ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker.convertCargoTypesToString;
import static ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker.convertToResponse;
import static ru.yandex.market.deepmind.common.repository.season.SeasonRepository.DEFAULT_ID;
import static ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils.convertDatesToRangeString;

@SuppressWarnings("checkstyle:magicnumber")
public class MskuAvailabilityMatrixCheckerTest extends DeepmindBaseDbTestClass {
    private static final Warehouse WAREHOUSE_1 = new Warehouse()
        .setId(100L)
        .setName("uno")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    private static final Warehouse WAREHOUSE_2 = new Warehouse()
        .setId(200L)
        .setName("dos")
        .setCargoTypeLmsIds(1L, 2L, 3L)
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    private static final Warehouse WAREHOUSE_3 = new Warehouse()
        .setId(300L)
        .setName("dropship warehouse")
        .setType(WarehouseType.DROPSHIP);


    @Autowired
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Autowired
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Autowired
    private MskuStatusRepository mskuStatusRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;

    private MskuAvailabilityMatrixChecker mskuAvailabilityMatrixChecker;
    private DeepmindCategoryCachingServiceMock deepmindCategoryCachingServiceMock;
    private EnhancedRandom mskuRandom;
    private SwitchControlServiceMock switchControlService;

    private static String categoryName(int id) {
        return "category-" + id;
    }

    private static PeriodResponse period(String fromMmDd, String toMmDd, LocalDate date) {
        SeasonPeriod seasonPeriod = new SeasonPeriod().setDeliveryFromMmDd(fromMmDd).setDeliveryToMmDd(toMmDd);
        SeasonPeriodUtils.PeriodWithDates withDates =
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(date, seasonPeriod);
        return convertToResponse(withDates);
    }

    private static Msku nextMsku(long mskuId) {
        return TestUtils.newMsku(mskuId);
    }

    private static SeasonPeriod whSeasonPeriod(long warehouseId, String start, String end) {
        return new SeasonPeriod()
            .setWarehouseId(warehouseId)
            .setFromMmDd("01-01")
            .setToMmDd("31-01")
            .setDeliveryFromMmDd(start)
            .setDeliveryToMmDd(end);
    }

    private static SeasonRepository.SeasonWithPeriods seasonsOf(SeasonPeriod... seasonPeriods) {
        return new SeasonRepository.SeasonWithPeriods(new Season(), List.of(seasonPeriods));
    }

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WAREHOUSE_1, WAREHOUSE_2, WAREHOUSE_3);
        deepmindCargoTypeSnapshotRepository.save(List.of(new CargoTypeSnapshot()
                .setId(1L)
                .setDescription("жиденький")
                .setMboParameterId(10L),
            new CargoTypeSnapshot()
                .setId(2L)
                .setDescription("Красный")
                .setMboParameterId(20L),
            new CargoTypeSnapshot()
                .setId(3L)
                .setDescription("Радиокативный")
                .setMboParameterId(30L),
            new CargoTypeSnapshot()
                .setId(4L)
                .setMboParameterId(40L)));
        deepmindCategoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        switchControlService = new SwitchControlServiceMock();
        mskuAvailabilityMatrixChecker = new MskuAvailabilityMatrixChecker(mskuAvailabilityMatrixRepository,
            categoryAvailabilityMatrixRepository, mskuStatusRepository,
            new DeepmindCargoTypeCachingServiceImpl(deepmindCargoTypeSnapshotRepository),
            deepmindCategoryCachingServiceMock,
            seasonRepository
        );
        mskuRandom = TestUtils.createMskuRandom();
    }

    private Map<Long, Msku> prepareMskus() {
        return LongStream.range(1L, 9L)
            .mapToObj(id ->
                deepmindMskuRepository.save(TestUtils.newMsku(id)
                    .setCategoryId(id + 20)
                    .setVendorId(1L))
            ).collect(Collectors.toMap(Msku::getId, v -> v));
    }

    private Map<Long, Msku> prepareMatrix(String fromDate, String toDate, BlockReasonKey blockReasonKey) {
        Map<Long, Msku> mskus = prepareMskus();
        mskuAvailabilityMatrixRepository.save(
            // false with warehouse
            MskuServicesTestUtils.mskuMatrix(false, 1L, WAREHOUSE_1.getId(), fromDate, toDate, blockReasonKey),
            // true with warehouse
            MskuServicesTestUtils.mskuMatrix(true, 2L, WAREHOUSE_1.getId(), fromDate, toDate, blockReasonKey)
        );
        return mskus;
    }

    private List<Long> prepareCategoryMatrix(BlockReasonKey blockReasonKey) {
        Map<Long, Msku> mskus = prepareMskus();
        deepmindCategoryCachingServiceMock
            .addCategory(21, categoryName(21))
            .addCategory(22, categoryName(22))
            .addCategory(23, categoryName(23))
            .addCategory(24, categoryName(24))
            .addCategory(25, categoryName(25))
            .addCategory(26, categoryName(26))
            .addCategory(27, categoryName(27))
            .addCategory(28, categoryName(28));

        categoryAvailabilityMatrixRepository.save(
            // false with warehouse
            nextCategoryAvailabilityMatrix(false, 21L, WAREHOUSE_1.getId(), blockReasonKey),
            // true with warehouse
            nextCategoryAvailabilityMatrix(true, 22L, WAREHOUSE_1.getId(), blockReasonKey)
        );
        return mskus.keySet().stream().map(id -> id + 20).collect(Collectors.toList());
    }

    @Test
    public void shouldComputeMskuConstraintsIfDeliveryNotAllowed() {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(30);
        Map<Long, Msku> mskuById = prepareMatrix(fromDate.toString(), toDate.toString(),
            BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS);
        LocalDate targetDate = fromDate.plusDays(3);

        var tmp = mskuAvailabilityMatrixRepository.findAll();

        Multimap<Long, MatrixAvailability> mskuConstraints = mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(WAREHOUSE_1, targetDate, targetDate, mskuById);

        assertThat(mskuConstraints.keySet()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(mskuConstraints.get(1L)).containsExactlyInAnyOrder(
            MatrixAvailabilityUtils
                .mskuInWarehouse(false, TestUtils.newMsku(1L), WAREHOUSE_1, fromDate, toDate, null,
                    BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS)
        );
        assertThat(mskuConstraints.get(2L)).containsExactlyInAnyOrder(
            MatrixAvailabilityUtils
                .mskuInWarehouse(true, TestUtils.newMsku(2L), WAREHOUSE_1, fromDate, toDate, null,
                    BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS)
        );
    }

    @Test
    public void shouldComputeMskuConstraintsIfMskuInArchiveOrEndOfLifeStatus() {
        switchControlService.setForceSkipMskuStatusConstraints(false);

        Map<Long, Msku> mskuIds = prepareMskus();

        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setSeasonId(null)
            .setMarketSkuId(1L).setMskuStatus(MskuStatusValue.REGULAR));
        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setSeasonId(null)
            .setMarketSkuId(2L).setMskuStatus(MskuStatusValue.END_OF_LIFE));
        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setSeasonId(null)
            .setMarketSkuId(3L).setMskuStatus(MskuStatusValue.ARCHIVE));

        LocalDate fromDate = LocalDate.now();
        LocalDate targetDate = fromDate.plusDays(3);

        Multimap<Long, MatrixAvailability> mskuConstraints = mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(WAREHOUSE_1, targetDate, targetDate, mskuIds);
        assertThat(mskuConstraints.values())
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.MSKU_END_OF_LIFE, BlockReasonKey.MSKU_ARCHIVED);
        assertThat(mskuConstraints.values())
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuEndOfLife(TestUtils.newMsku(2L)),
                MatrixAvailabilityUtils.mskuArchived(TestUtils.newMsku((3L)))
            );
    }

    private void saveWithSeason(Long mskuId, SeasonRepository.SeasonWithPeriods seasonWithPeriods) {
        seasonWithPeriods.getSeason().setId(mskuId);
        seasonWithPeriods.getSeason().setName(String.format("Season for {%s}", mskuId));
        SeasonRepository.SeasonWithPeriods season = seasonRepository.saveWithPeriodsAndReturn(seasonWithPeriods);

        mskuStatusRepository.save(
            mskuRandom.nextObject(MskuStatus.class)
                .setSeasonId(season.getId())
                .setMarketSkuId(mskuId)
                .setMskuStatus(MskuStatusValue.SEASONAL));
    }

    @Test
    public void shouldComputeMskuConstraintsIfMskuInSeasonalStatus() {
        switchControlService.setForceSkipMskuStatusConstraints(false);
        Map<Long, Msku> mskuIds = prepareMskus();

        final LocalDate deliveryDate = LocalDate.of(2019, 10, 2);

        // период попадает в дату поставки - Ок
        saveWithSeason(1L, seasonsOf(
            whSeasonPeriod(WAREHOUSE_1.getId(), "08-01", "08-30"),
            whSeasonPeriod(WAREHOUSE_1.getId(), "10-01", "10-20")
        ));

        // период не попадает в дату поставки - НЕ Ок
        saveWithSeason(2L, seasonsOf(
            whSeasonPeriod(WAREHOUSE_1.getId(), "08-01", "08-30")
        ));

        // совсем нет периода - Ок
        saveWithSeason(3L, seasonsOf());

        // период по всем складам не попадает в дату поставки - НЕ Ок
        saveWithSeason(4L, seasonsOf(
            whSeasonPeriod(DEFAULT_ID, "01-01", "02-29"),
            whSeasonPeriod(DEFAULT_ID, "08-01", "08-30")
        ));

        // период по всем складам попадает в дату поставки - Ок
        saveWithSeason(5L, seasonsOf(
            whSeasonPeriod(DEFAULT_ID, "08-01", "08-30"),
            whSeasonPeriod(DEFAULT_ID, "10-01", "10-20")
        ));

        Multimap<Long, MatrixAvailability> mskuConstraints = mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(WAREHOUSE_1, deliveryDate, deliveryDate, mskuIds);

        assertThat(mskuConstraints.values())
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.MSKU_IN_SEASON);

        assertThat(mskuConstraints.values())
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInSeason(2L, WAREHOUSE_1,
                    period("08-01", "08-30", deliveryDate)),
                MatrixAvailabilityUtils.mskuInSeason(4L, WAREHOUSE_1,
                    period("01-01", "02-29", deliveryDate),
                    period("08-01", "08-30", deliveryDate)
                )
            );
    }

    @Test
    public void shouldComputeCategoryConstraintsIfDeliveryNotAllowed() {
        List<Long> categories = prepareCategoryMatrix(BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS);

        Map<Long, MatrixAvailability> mskuConstraints = mskuAvailabilityMatrixChecker
            .computeCategoryDeliveryConstraints(WAREHOUSE_1, categories);

        assertThat(mskuConstraints.values())
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS);

        assertThat(mskuConstraints.values())
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInCategory(WAREHOUSE_1, 21, categoryName(21), null,
                    BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS)
            );
    }

    @Test
    public void shouldComputeCategoryAvailabilityConsideringHierarchy() {
        deepmindCategoryCachingServiceMock
            .addCategory(1, "Some root")
            .addCategory(2, "Child", 1)
            .addCategory(3, "Child #2-1", 2)
            .addCategory(4, "Child #2-2 enable all", 2)
            .addCategory(5, "Root child just inherit", 1);

        categoryAvailabilityMatrixRepository.save(
            nextCategoryAvailabilityMatrix(false, 1, WAREHOUSE_1.getId()),
            nextCategoryAvailabilityMatrix(true, 1, WAREHOUSE_2.getId()),
            nextCategoryAvailabilityMatrix(true, 2, WAREHOUSE_1.getId()), // override
            nextCategoryAvailabilityMatrix(false, 3, WAREHOUSE_2.getId()),
            nextCategoryAvailabilityMatrix(true, 4, WAREHOUSE_1.getId()),
            nextCategoryAvailabilityMatrix(true, 4, WAREHOUSE_2.getId())
        );

        var availabilityMap1 = mskuAvailabilityMatrixChecker.computeCategoryDeliveryConstraints(WAREHOUSE_1,
            List.of(5L, 4L, 3L));
        Assertions.assertThat(availabilityMap1.values())
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInCategory(WAREHOUSE_1, 5L, "Root child just inherit", null, null)
            );

        var availabilityMap2 = mskuAvailabilityMatrixChecker.computeCategoryDeliveryConstraints(WAREHOUSE_2,
            List.of(5L, 4L, 3L));
        Assertions.assertThat(availabilityMap2.values())
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInCategory(WAREHOUSE_2, 3L, "Child #2-1", null, null)
            );

        var availabilityMap3 = mskuAvailabilityMatrixChecker.computeCategoryDeliveryConstraints(WAREHOUSE_3,
            List.of(5L, 4L, 3L));
        Assertions.assertThat(availabilityMap3.values()).isEmpty();
    }

    @Test
    public void shouldComputeCargoTypeConstraints() {
        prepareMskus();

        Map<Long, Msku> mskus = Map.of(
            1L, TestUtils.newMsku(1L, 1L, List.of(1L, 2L, 3L)),
            2L, TestUtils.newMsku(2L, 1L, List.of()),
            3L, TestUtils.newMsku(3L, 1L, List.of(1L))
        );

        Map<Long, MatrixAvailabilityByWarehouse> result = mskuAvailabilityMatrixChecker
            .computeMskuCargoTypeConstraints(Map.of(
                WAREHOUSE_1.getId(), WAREHOUSE_1,
                WAREHOUSE_2.getId(), WAREHOUSE_2
            ), mskus);
        assertThat(result).containsOnlyKeys(1L, 2L, 3L);
        var blockReasonKeys = new HashSet<BlockReasonKey>();
        result.values().forEach(m -> {
            blockReasonKeys.addAll(m.getAvailabilities(WAREHOUSE_1.getId()).stream()
                .map(MatrixAvailability::getBlockReasonKey).collect(Collectors.toSet()));
            blockReasonKeys.addAll(m.getAvailabilities(WAREHOUSE_2.getId()).stream()
                .map(MatrixAvailability::getBlockReasonKey).collect(Collectors.toSet()));
        });
        assertThat(blockReasonKeys)
            .containsOnly(BlockReasonKey.MSKU_MISSING_CARGO_TYPES);

        Assertions.assertThat(result.get(1L).getAvailabilities(WAREHOUSE_1.getId()))
            .containsExactly(MatrixAvailabilityUtils.mskuMissingCargoTypes(mskus.get(1L), WAREHOUSE_1,
                List.of(1L, 2L, 3L), "Красный #2, Радиокативный #3, жиденький #1"));
        Assertions.assertThat(result.get(2L).getAvailabilities(WAREHOUSE_1.getId())).isEmpty();
        Assertions.assertThat(result.get(3L).getAvailabilities(WAREHOUSE_1.getId()))
            .containsExactly(MatrixAvailabilityUtils.mskuMissingCargoTypes(mskus.get(3L), WAREHOUSE_1,
                List.of(1L), "жиденький #1"));

        Assertions.assertThat(result.get(1L).getAvailabilities(WAREHOUSE_2.getId())).isEmpty();
        Assertions.assertThat(result.get(2L).getAvailabilities(WAREHOUSE_2.getId())).isEmpty();
        Assertions.assertThat(result.get(3L).getAvailabilities(WAREHOUSE_2.getId())).isEmpty();
    }

    @Test
    public void shouldConvertDatesToRangeString() {
        String fromDateString = "2019-07-18";
        LocalDate fromDate = LocalDate.parse(fromDateString);
        String toDateString = "2019-07-31";
        LocalDate toDate = LocalDate.parse(toDateString);

        assertThat(convertDatesToRangeString(fromDate, toDate))
            .isEqualTo("c " + fromDateString + " по " + toDateString);
        assertThat(convertDatesToRangeString(fromDate, null))
            .isEqualTo("c " + fromDateString);
        assertThat(convertDatesToRangeString(null, toDate))
            .isEqualTo("по " + toDateString);
        assertThat(convertDatesToRangeString(null, null))
            .isEmpty();
    }

    @Test
    public void shouldNotFailForRepeatedCategories() {
        deepmindCategoryCachingServiceMock.addCategory(1, "Test");
        // Shouldn't fail
        mskuAvailabilityMatrixChecker.computeCategoryDeliveryConstraints(deepmindWarehouseRepository.findAll().get(0),
            List.of(1L, 1L, 1L, 1L));
    }

    @Test
    public void shouldConvertCargoTypesToString() {
        assertThat(convertCargoTypesToString(Map.of(
            11L, "1 тип",
            12L, "2 неизвестный тип",
            333L, "3 типчик"), List.of(11L, 12L)))
            .isEqualTo("1 тип #11, " +
                "2 неизвестный тип #12");
    }

    @Test
    public void checkDeliveryOpen() {
        SeasonRepository.SeasonWithPeriods periods = new SeasonRepository.SeasonWithPeriods(new Season(),
            List.of(new SeasonPeriod().setWarehouseId(1L))
        );

        LocalDate date = LocalDate.of(2018, Month.JULY, 8);
        MatrixAvailability availability = MskuAvailabilityMatrixChecker.checkDeliveryRestrictions(1,
            date, date, periods, WAREHOUSE_1);

        assertThat(availability).isNull();
    }

    @Test
    public void checkDeliveryRestrictionsAllWarehouses() {
        SeasonRepository.SeasonWithPeriods periods = new SeasonRepository.SeasonWithPeriods(new Season(),
            List.of(
                new SeasonPeriod().setWarehouseId(1L).setDeliveryFromMmDd("07-01").setDeliveryToMmDd("07-31"),
                new SeasonPeriod().setWarehouseId(1L).setDeliveryFromMmDd("07-01").setDeliveryToMmDd("07-31"),
                new SeasonPeriod().setWarehouseId(DEFAULT_ID).setDeliveryFromMmDd("07-01").setDeliveryToMmDd("07-31")
            )
        );

        LocalDate date = LocalDate.of(2018, Month.JULY, 8);
        MatrixAvailability availability = MskuAvailabilityMatrixChecker.checkDeliveryRestrictions(1,
            date, date, periods, WAREHOUSE_1);

        assertThat(availability).isNull();
    }

    private List<Msku> generateMskus(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> deepmindMskuRepository.save(nextMsku(i + 1)))
            .collect(Collectors.toList());
    }

    @Test
    public void mskuAvailabilityMatrixChecker() {
        deepmindWarehouseRepository.deleteAll();
        List<Msku> skus = generateMskus(5);
        Warehouse warehouse = new Warehouse().setId(1L).setName("1 Свиблово").setType(WarehouseType.FULFILLMENT);
        deepmindWarehouseRepository.save(warehouse);

        LocalDate date = LocalDate.now();
        Multimap<Long, MatrixAvailability> emptyResult = mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(
                warehouse, date, date,
                Map.of(skus.get(0).getId(), new Msku(skus.get(0)))
            );

        assertThat(emptyResult.values()).hasSize(0);
    }
}
