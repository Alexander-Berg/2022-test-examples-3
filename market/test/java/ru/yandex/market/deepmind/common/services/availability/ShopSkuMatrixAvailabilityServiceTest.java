package ru.yandex.market.deepmind.common.services.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailabilityContext;
import ru.yandex.market.deepmind.common.availability.ssku.SskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SupplierAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.DeepmindConstants;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.mboc.common.utils.availability.PeriodResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CONTENTLAB_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_RETURN_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.repository.season.SeasonRepository.DEFAULT_ID;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.CONTENTLAB;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.CROSSDOCK_ROSTOV;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.CROSSDOCK_SOFINO;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.MARSHRUT;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.ROSTOV;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SOFINO;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SOFINO_RETURN;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SORTING_CENTER_1;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;

@SuppressWarnings("checkstyle:MagicNumber")
public class ShopSkuMatrixAvailabilityServiceTest extends DeepmindBaseDbTestClass {
    private static final int SUPPLIER_3P_ID = 84;
    private static final Msku MSKU_404040 = TestUtils.newMsku(404040L, 10L);
    private static final Msku MSKU_505050 = TestUtils.newMsku(505050L, 22L);
    private static final Msku MSKU_10 = TestUtils.newMsku(10L, 10L);
    private static final Msku MSKU_20 = TestUtils.newMsku(20L, 12L);
    private static final Msku ARCHIVE_MSKU = TestUtils.newMsku(191919L, 14L);

    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Autowired
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Autowired
    private MskuStatusRepository mskuStatusRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Autowired
    private SupplierAvailabilityMatrixRepository supplierAvailabilityMatrixRepository;
    @Autowired
    private PartnerRelationRepository partnerRelationRepository;
    @Autowired
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private DeepmindCategoryCachingServiceMock deepmindCategoryCachingServiceMock;
    private SwitchControlServiceMock switchControlService;
    private MskuAvailabilityMatrixChecker mskuAvailabilityMatrixChecker;
    private SskuAvailabilityMatrixChecker sskuAvailabilityMatrixChecker;

    private ShopSkuMatrixAvailabilityService service;

    @Before
    public void setUp() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_DROPSHIP);
        serviceOfferReplicaRepository.save(
            new ServiceOfferReplica()
                .setBusinessId(SUPPLIER_3P_ID)
                .setSupplierId(SUPPLIER_3P_ID)
                .setShopSku("sku-msku-archived")
                .setTitle("archived offer")
                .setCategoryId(99L)
                .setSeqId(0L)
                .setMskuId(ARCHIVE_MSKU.getId())
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setAcceptanceStatus(OfferAcceptanceStatus.OK)
                .setModifiedTs(Instant.now())
        );

        deepmindCategoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        deepmindCategoryCachingServiceMock.addCategory(10, "Category 10");
        deepmindCategoryCachingServiceMock.addCategory(12, "Category 12");
        deepmindCategoryCachingServiceMock.addCategory(14, "Category 14");
        deepmindCategoryCachingServiceMock.addCategory(22, "Category 22");
        deepmindCategoryCachingServiceMock.addCategory(33, "Category 33");
        deepmindCategoryCachingServiceMock.addCategory(88, "Category 88");

        var deepmindCargoTypeCachingService = new DeepmindCargoTypeCachingServiceMock();
        deepmindCargoTypeCachingService.put(
            new CargoTypeSnapshot(1L, "cargo10", 10L),
            new CargoTypeSnapshot(2L, "cargo20", 20L),
            new CargoTypeSnapshot(3L, "cargo30", 30L)
        );

        deepmindMskuRepository.save(
            MSKU_10,
            MSKU_20,
            MSKU_404040,
            MSKU_505050,
            ARCHIVE_MSKU
        );

        deepmindWarehouseRepository.save(SORTING_CENTER_1);
        deepmindWarehouseRepository.save(SOFINO_RETURN);

        switchControlService = new SwitchControlServiceMock();
        mskuAvailabilityMatrixChecker = new MskuAvailabilityMatrixChecker(
            mskuAvailabilityMatrixRepository,
            categoryAvailabilityMatrixRepository,
            mskuStatusRepository,
            deepmindCargoTypeCachingService,
            deepmindCategoryCachingServiceMock,
            seasonRepository
        );
        sskuAvailabilityMatrixChecker = new SskuAvailabilityMatrixChecker(
            sskuAvailabilityMatrixRepository,
            deepmindSupplierRepository,
            deepmindWarehouseRepository,
            supplierAvailabilityMatrixRepository
        );

        service = new ShopSkuMatrixAvailabilityServiceImpl(
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            deepmindMskuRepository,
            deepmindWarehouseRepository,
            mskuAvailabilityMatrixChecker,
            sskuAvailabilityMatrixChecker,
            switchControlService,
            partnerRelationRepository,
            sskuStatusRepository
        );
    }

    @Test
    public void testOfferDelisted() {
        sskuStatusRepository.save(sskuStatus(101, "sku100", OfferAvailability.DELISTED));


        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );
        var blockReasonKeys = new HashSet<BlockReasonKey>();
        availabilityMap.values().forEach(s ->
            s.getAllMatrixAvailabilities(TOMILINO_ID).forEach(m -> blockReasonKeys.add(m.getBlockReasonKey()))
        );

        Assertions.assertThat(blockReasonKeys)
            .containsOnly(BlockReasonKey.SSKU_OFFER_DELISTED);

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrder(101, "sku100", TOMILINO_ID,
                MatrixAvailabilityUtils.offerDelisted(101, "sku100")
            );
    }

    @Test
    public void testOfferInactive() {
        sskuStatusRepository.save(sskuStatus(101, "sku100", OfferAvailability.INACTIVE_TMP));
        sskuStatusRepository.save(sskuStatus(102, "sku100", OfferAvailability.INACTIVE_TMP));

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            // block by inactive_tmp only work for 1P оффер
            .containsExactlyInAnyOrder(102, "sku100", TOMILINO_ID,
                MatrixAvailabilityUtils.offerInactiveTmp(102, "sku100")
            );

        Assertions
            .assertThat(availabilityMap.values().stream()
                .flatMap(x -> x.getAllMatrixAvailabilities(TOMILINO_ID).stream())
                .collect(Collectors.toList()))
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.SSKU_OFFER_INACTIVE_TMP_OTHER);
    }

    @Test
    public void testOfferInactiveSpecialBlockReasons() {
        sskuStatusRepository.save(
            sskuStatus(77, "sku6", OfferAvailability.INACTIVE_TMP).setHasNoPurchasePrice(true));
        sskuStatusRepository.save(
            sskuStatus(102, "sku100", OfferAvailability.INACTIVE_TMP).setStatusFinishAt(Instant.now()));

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(77, "sku6"),
                new ServiceOfferKey(102, "sku100")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        var resultMap = availabilityMap.values().stream()
            .collect(Collectors.toMap(ShopSkuAvailability::getServiceOfferKey,
                s -> s.getAllMatrixAvailabilities(TOMILINO_ID)));

        assertThat(resultMap)
            .hasSize(2);

        assertThat(resultMap.get(new ServiceOfferKey(77, "sku6")))
            .containsExactly(MatrixAvailabilityUtils.offerInactiveTmpWithNoPrice(77, "sku6"));
        assertThat(resultMap.get(new ServiceOfferKey(102, "sku100")))
            .containsExactly(MatrixAvailabilityUtils.offerInactiveTmpWithPeriod(102, "sku100"));

        Assertions
            .assertThat(availabilityMap.values().stream()
                .flatMap(x -> x.getAllMatrixAvailabilities(TOMILINO_ID).stream())
                .collect(Collectors.toList()))
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.SSKU_OFFER_INACTIVE_TMP_WITH_PERIOD,
                BlockReasonKey.SSKU_OFFER_INACTIVE_TMP_NO_PURCHASE_PRICE);
    }

    @Test
    public void testMsku() {
        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.MSKU_LEGAL_REQUIREMENTS),
            mskuMatrix(MSKU_505050, false, SOFINO_ID).setBlockReasonKey(BlockReasonKey.MSKU_GOODS_STORAGE_SPACE),
            mskuMatrix(MSKU_10, false, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5"),
                new ServiceOfferKey(79, "sku9"),
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, SOFINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrderShopSkuKeys(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5"),
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100")
            );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, TOMILINO, null, null,
                    BlockReasonKey.MSKU_LEGAL_REQUIREMENTS)
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(SOFINO_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_505050, SOFINO, null, null,
                    BlockReasonKey.MSKU_GOODS_STORAGE_SPACE)
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(101, "sku100")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_10, ROSTOV, null, null,
                    BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS)
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(102, "sku100")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_10, ROSTOV, null, null,
                    BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS)
            );

        // without warehouse
        availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5"),
                new ServiceOfferKey(79, "sku9"),
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100")
            ), new ShopSkuAvailabilityContext()
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values()).isEmpty();
    }

    @Test
    public void testMskuInInterval() {
        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, MARSHRUT_ID)
                .setFromDate(LocalDate.of(2020, 1, 23))
        );

        // with date before
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 1, 20))
                .setWarehouseIds(MARSHRUT_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .isEmpty();

        // with date after
        availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 1, 25))
                .setWarehouseIds(MARSHRUT_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(MARSHRUT_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, MARSHRUT,
                    LocalDate.of(2020, 1, 23), null, null)
            );
    }

    @Test
    public void testMskuArchived() {
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(ARCHIVE_MSKU.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(SUPPLIER_3P_ID, "sku-msku-archived")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV_ID)
        );

        Assertions
            .assertThat(availabilityMap.values())
            .hasSize(1);

        Assertions
            .assertThat(availabilityMap.values().stream()
                .map(x -> x.getAllMatrixAvailabilities(ROSTOV_ID))
                .findFirst().get())
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.MSKU_ARCHIVED);

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(SUPPLIER_3P_ID, "sku-msku-archived")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuArchived(ARCHIVE_MSKU)
            )
            .doesNotContainForWarehouseIds(TOMILINO_ID);
    }

    @Test
    public void testMskuEndOfLife() {
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(ARCHIVE_MSKU.getId())
            .setMskuStatus(MskuStatusValue.END_OF_LIFE)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(SUPPLIER_3P_ID, "sku-msku-archived")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(SUPPLIER_3P_ID, "sku-msku-archived")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuEndOfLife(ARCHIVE_MSKU)
            );
    }

    @Test
    public void testMskuCargoTypes() {
        Msku msku = deepmindMskuRepository.findById(404040L).orElseThrow().setCargoTypes(1L);
        deepmindMskuRepository.save(msku);
        msku = deepmindMskuRepository.findById(505050L).orElseThrow().setCargoTypes(2L, 3L);
        deepmindMskuRepository.save(msku);

        deepmindWarehouseRepository.save(
            deepmindWarehouseRepository.getById(TOMILINO_ID)
                .setCargoTypeLmsIds(3L)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuMissingCargoTypes(MSKU_404040, TOMILINO, List.of(1L), "cargo10 #1")
            );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuMissingCargoTypes(MSKU_505050, TOMILINO, List.of(2L), "cargo20 #2")
            );
    }

    @Test
    public void testMskuInCategory() {
        categoryAvailabilityMatrixRepository.save(
            categoryMatrix(10L, SOFINO_ID),
            categoryMatrix(22L, TOMILINO_ID),
            categoryMatrix(12L, TOMILINO_ID).setComment("comment tomilino 12")
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5"),
                new ServiceOfferKey(201, "sku200"),
                new ServiceOfferKey(202, "sku200")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, SOFINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrder(60, "sku4", SOFINO_ID,
                MatrixAvailabilityUtils.mskuInCategory(SOFINO, 10, "Category 10", null, null)
            )
            .containsExactlyInAnyOrder(77, "sku5", TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInCategory(TOMILINO, 22, "Category 22", null, null)
            )
            .containsExactlyInAnyOrder(201, "sku200", TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInCategory(TOMILINO, 12, "Category 12", "comment tomilino 12", null)
            )
            .doesNotContainForWarehouseId(60, "sku4", TOMILINO_ID)
            .doesNotContainForWarehouseId(77, "sku5", SOFINO_ID)
            .doesNotContainShopSkuKey(202, "sku200");
    }

    @Test
    public void testMskuInCategoryWhenMskuIsMissingInDb() {
        // mock model
        Model model = new Model()
            .setId(33L).setTitle("super-model").setCategoryId(33L).setParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(10L).setBoolValue(false).setXslName("cargo-param 10").build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(20L).setBoolValue(true).setXslName("cargo-param 20").build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(30L).setBoolValue(true).setXslName("cargo-param 30").build()
            ));

        // change approve mapping id
        var sku4 = serviceOfferReplicaRepository.save(serviceOfferReplicaRepository
            .findOffers(new ServiceOfferReplicaFilter().setBusinessIds(60).setShopSkus(List.of("sku4"))).get(0)
            .setMskuId(model.getId())
            .setCategoryId(model.getCategoryId())
        ).get(0);
        serviceOfferReplicaRepository.save(serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setBusinessIds(60).setShopSkus(List.of("sku4"))).stream()
            .map(o -> o.setMskuId(sku4.getMskuId()))
            .collect(Collectors.toList())
        );

        categoryAvailabilityMatrixRepository.save(
            categoryMatrix(33L, ROSTOV_ID)
        );

        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInCategory(ROSTOV, 33, "Category 33", null, null)
            );
    }

    @Test
    public void testMskuInSeason() {
        SeasonRepository.SeasonWithPeriods season = new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Новый год"),
            List.of(new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("01-23")
                .setToMmDd("02-12")
                .setDeliveryFromMmDd("01-23")
                .setDeliveryToMmDd("02-12")
            ));
        long seasonId = seasonRepository.saveWithPeriods(season).getId();
        mskuStatusRepository.save(mskuSeasonalStatus(MSKU_404040, seasonId));

        // get in current date
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setInboundDate(LocalDate.of(2020, 1, 20))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1579726800000L, 1581454800000L, "23 января", "12 февраля", "01-23", "02-12"))
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(77, "sku5");

        // get in interval, inside season
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setFrom(LocalDate.of(2020, 1, 23))
                .setTo(LocalDate.of(2020, 1, 30))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .isEmpty();

        // get in interval, partly inside and outside season
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setFrom(LocalDate.of(2020, 1, 1))
                .setTo(LocalDate.of(2020, 1, 30))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1579726800000L, 1581454800000L, "23 января", "12 февраля", "01-23", "02-12"))
            );
    }

    @Test
    public void testSeveralPeriods() {
        SeasonRepository.SeasonWithPeriods season = new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Сезон"),
            List.of(
                // доставка разрешена с 23 января по 12 февраля
                new SeasonPeriod()
                    .setWarehouseId(DEFAULT_ID)
                    .setFromMmDd("01-23").setToMmDd("02-12")
                    .setDeliveryFromMmDd("01-23").setDeliveryToMmDd("02-12"),
                // доставка разрешена с 1 июня по 1 июля
                new SeasonPeriod()
                    .setWarehouseId(DEFAULT_ID)
                    .setFromMmDd("06-01").setToMmDd("07-01")
                    .setDeliveryFromMmDd("06-01").setDeliveryToMmDd("07-01"),
                // доставка разрешена с 1 июня по 1 августа на томилино
                new SeasonPeriod()
                    .setWarehouseId(TOMILINO_ID)
                    .setFromMmDd("06-01").setToMmDd("08-01")
                    .setDeliveryFromMmDd("06-01").setDeliveryToMmDd("08-01")
            ));
        long seasonId = seasonRepository.saveWithPeriods(season).getId();
        mskuStatusRepository.save(mskuSeasonalStatus(MSKU_404040, seasonId));

        // проверяем с 5 июня по 15 июня
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setFrom(LocalDate.of(2020, 6, 5))
                .setTo(LocalDate.of(2020, 6, 15))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).isEmpty();

        // проверяем с 5 июня по 15 июля
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setFrom(LocalDate.of(2020, 6, 5))
                .setTo(LocalDate.of(2020, 7, 15))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).isEmpty();

        // проверяем с 5 июня по 15 августа
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setFrom(LocalDate.of(2020, 6, 5))
                .setTo(LocalDate.of(2020, 8, 15))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1590958800000L, 1596229200000L, "1 июня", "1 августа", "06-01", "08-01"))
            );

        // проверяем с 1 января по 31 декабря
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
                .setFrom(LocalDate.of(2020, 1, 1))
                .setTo(LocalDate.of(2020, 12, 31))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1590958800000L, 1596229200000L, "1 июня", "1 августа", "06-01", "08-01"))
            );
    }

    @Test
    public void testMskuInSeasonIn29February() {
        SeasonRepository.SeasonWithPeriods season = new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Февраль"),
            List.of(new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("02-28").setToMmDd("03-01")
                .setDeliveryFromMmDd("02-29").setDeliveryToMmDd("03-01")
            ));
        long seasonId = seasonRepository.saveWithPeriods(season).getId();
        mskuStatusRepository.save(mskuSeasonalStatus(MSKU_404040, seasonId));

        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2019, 3, 1))
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).isEmpty();
    }

    @Test
    public void testMskuInSeasonAndMskuPeriod() {
        // доставка разрешена с 23 января по 12 февраля
        SeasonRepository.SeasonWithPeriods season = new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Новый год"),
            List.of(new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("01-23").setToMmDd("02-12")
                .setDeliveryFromMmDd("01-23").setDeliveryToMmDd("02-12")
            ));
        long seasonId = seasonRepository.saveWithPeriods(season).getId();
        mskuStatusRepository.save(mskuSeasonalStatus(MSKU_404040, seasonId));

        // msku запрещен к доставке по 2 февраля 2020
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(404040L)
                .setWarehouseId(TOMILINO_ID)
                .setToDate(LocalDate.of(2020, 2, 2))
                .setAvailable(false)
        );

        // проверяем блокировки на 20 января
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 1, 20))
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1579726800000L, 1581454800000L, "23 января", "12 февраля", "01-23", "02-12")
                ),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, TOMILINO,
                    null, LocalDate.of(2020, 2, 2), null)
            );

        // проверяем блокировки на 1 февраля
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 2, 1))
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, TOMILINO,
                    null, LocalDate.of(2020, 2, 2), null)
            );

        // проверяем блокировки на 10 февраля
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 2, 10))
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(60, "sku4");
    }

    @Test
    public void testMskuInSeasonAndCategoryOnWarehouse() {
        SeasonRepository.SeasonWithPeriods season = new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Новый год"),
            List.of(new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("01-01")
                .setToMmDd("01-01")
                .setDeliveryFromMmDd("01-01")
                .setDeliveryToMmDd("02-01")
            ));
        long seasonId = seasonRepository.saveWithPeriods(season).getId();
        mskuStatusRepository.save(mskuSeasonalStatus(MSKU_404040, seasonId));

        categoryAvailabilityMatrixRepository.save(
            categoryMatrix(MSKU_404040.getCategoryId(), TOMILINO_ID)
        );

        // current date
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(ROSTOV_ID, TOMILINO_ID)
                .setInboundDate(LocalDate.of(2020, 1, 15))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInCategory(TOMILINO, 10, "Category 10", null, null)
            )
            .doesNotContainForWarehouseIds(ROSTOV_ID);

        // in future
        availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(ROSTOV_ID, TOMILINO_ID)
                .setInboundDate(LocalDate.of(2020, 2, 15))
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), TOMILINO,
                    new PeriodResponse(1609448400000L, 1612126800000L, "1 января", "1 февраля", "01-01", "02-01")),
                MatrixAvailabilityUtils.mskuInCategory(TOMILINO, 10, "Category 10", null, null)
            )
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInSeason(MSKU_404040.getId(), ROSTOV,
                    new PeriodResponse(1609448400000L, 1612126800000L, "1 января", "1 февраля", "01-01", "02-01"))
            );
    }

    @Test
    public void testSupplierIsNotAvailable() {
        /*
         *      | TMN  | RSTV
         * 60   | true | null
         * 77   | true | false
         */
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(TOMILINO_ID).setAvailable(true),
            new SupplierAvailabilityMatrix().setSupplierId(77).setWarehouseId(TOMILINO_ID).setAvailable(true),
            new SupplierAvailabilityMatrix().setSupplierId(77).setWarehouseId(ROSTOV_ID).setAvailable(false)
                .setComment("comment rostov 77").setBlockReasonKey(BlockReasonKey.SUPPLIER_DEBT)
        ));

        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4"),
            new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(true, 60, "Test supplier 60",
                    TOMILINO, List.of(MARSHRUT, ROSTOV, TOMILINO, SOFINO, SOFINO_RETURN), null, null)
            )
            .doesNotContainForWarehouseIds(ROSTOV_ID);

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(true, 77, "Test supplier 77",
                    TOMILINO, List.of(MARSHRUT, TOMILINO, SOFINO, SOFINO_RETURN), null, null)
            )
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 77, "Test supplier 77",
                    ROSTOV, List.of(MARSHRUT, TOMILINO, SOFINO, SOFINO_RETURN), "comment rostov 77",
                    BlockReasonKey.SUPPLIER_DEBT)
            );

        // check for all_warehouse
        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4"),
            new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(SOFINO_ID)
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values()).isEmpty();
    }

    @Test
    public void testSupplierIsTotalNotAvailableInAllWarehouses() {
        /*
         *      | TMN   | RSTV   | other warehouses
         * 60   | false | false | false...
         */
        deepmindWarehouseRepository.save(CONTENTLAB);
        DeepmindWarehouseRepository.Filter filter = new DeepmindWarehouseRepository.Filter()
            .setUsingTypes(WarehouseUsingType.USE_FOR_FULFILLMENT);
        for (Warehouse warehouse : deepmindWarehouseRepository.find(filter)) {
            supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
                new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(warehouse.getId()).setAvailable(false)
                    .setComment("comment")
            ));
        }

        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplier(false, 60, "Test supplier 60", "comment", null)
            );

        // Если в запросе указать, какой-то скрытый склад, например, лабораторию контета,
        // то ответ уже будет другим
        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, CONTENTLAB_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 60, "Test supplier 60",
                    TOMILINO, List.of(CONTENTLAB), "comment", null)
            )
            .doesNotContainForWarehouseIds(CONTENTLAB_ID);
    }

    @Test
    public void testSupplierIsNotAvailableSkipped() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(TOMILINO_ID).setAvailable(false)
        ));

        // without any flag
        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 60, "Test supplier 60",
                    TOMILINO, List.of(MARSHRUT, ROSTOV, SOFINO, SOFINO_RETURN), null, null)
            );

        // with flag
        switchControlService.setForceSkipSupplierConstraints(true);
        switchControlService.invalidateCache();

        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(60, "sku4");
    }

    @Test
    public void testSupplierIsNotAvailableSkippedByMskuId() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(TOMILINO_ID).setAvailable(false)
        ));

        // without any flag
        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 60, "Test supplier 60",
                    TOMILINO, List.of(MARSHRUT, ROSTOV, SOFINO, SOFINO_RETURN), null, null)
            );

        // with msku flag
        switchControlService.setForceSkipMsku(MatrixAvailability.Reason.SUPPLIER, 404040L, TOMILINO_ID);
        switchControlService.setForceSkipMsku(MatrixAvailability.Reason.SUPPLIER, 404040L, ROSTOV_ID);
        switchControlService.invalidateCache();

        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(60, "sku4");
    }

    @Test
    public void testSupplierIsNotAvailableSkippedBySskuId() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(TOMILINO_ID).setAvailable(false)
        ));

        // without any flag
        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 60, "Test supplier 60",
                    TOMILINO, List.of(MARSHRUT, ROSTOV, SOFINO, SOFINO_RETURN), null, null)
            );

        // with ssku flag
        switchControlService.setForceSkipShopSku(MatrixAvailability.Reason.SUPPLIER, 60, "sku4", TOMILINO_ID);
        switchControlService.setForceSkipShopSku(MatrixAvailability.Reason.SUPPLIER, 60, "sku4", ROSTOV_ID);
        switchControlService.invalidateCache();

        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(60, "sku4");
    }

    @Test
    public void testSkippedInOneWarehouse() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(TOMILINO_ID).setAvailable(false),
            new SupplierAvailabilityMatrix().setSupplierId(60).setWarehouseId(SOFINO_ID).setAvailable(false)
        ));

        switchControlService.setForceSkipShopSku(MatrixAvailability.Reason.SUPPLIER, 60, "sku4", TOMILINO_ID);
        switchControlService.invalidateCache();

        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID, SOFINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SOFINO_ID,
                MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 60, "Test supplier 60",
                    SOFINO, List.of(MARSHRUT, ROSTOV, SOFINO_RETURN), null, null)
            )
            .doesNotContainForWarehouseIds(TOMILINO_ID);
    }

    @Test
    public void testSskuHiding() {
        sskuAvailabilityMatrixRepository.save(
            createMatrix(60, "sku4", ROSTOV_ID, false),
            createMatrix(60, "sku4", SOFINO_ID, false).setComment("sku4 comment")
        );

        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(SOFINO_ID, TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SOFINO_ID,
                MatrixAvailabilityUtils.ssku("Напильники для PGaaS!", 60, "sku4", false, null, null, "sku4 comment",
                    null)
            )
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.ssku("Напильники для PGaaS!", 60, "sku4", false, null, null, null, null)
            );
    }

    @Test
    public void testDeadstock() {
        // предварительно выставляем блокировки
        var matrix1 = new SskuAvailabilityMatrix()
            .setSupplierId(77)
            .setShopSku("sku5")
            .setWarehouseId(ROSTOV_ID)
            .setAvailable(false)
            .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK);
        var matrix2 = new SskuAvailabilityMatrix()
            .setSupplierId(77)
            .setShopSku("sku5")
            .setWarehouseId(TOMILINO_ID)
            .setAvailable(false)
            .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK);
        var matrix3 = new SskuAvailabilityMatrix()
            .setSupplierId(77)
            .setShopSku("sku5")
            .setWarehouseId(SOFINO_ID)
            .setAvailable(false)
            .setComment("comment")
            .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK);
        SecurityUtil.wrapWithLogin(DeepmindConstants.AUTO_DEADSTOCK_ROBOT, () ->
            sskuAvailabilityMatrixRepository.save(matrix1, matrix2)
        );
        SecurityUtil.wrapWithLogin("user", () ->
            sskuAvailabilityMatrixRepository.save(matrix3)
        );


        // act
        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(SOFINO_ID, ROSTOV_ID, TOMILINO_ID)
        );

        Assertions
            .assertThat(availabilityMap.values().stream()
                .flatMap(x -> {
                    var tmp = new ArrayList<MatrixAvailability>();
                    tmp.addAll(x.getAllMatrixAvailabilities(ROSTOV_ID));
                    tmp.addAll(x.getAllMatrixAvailabilities(TOMILINO_ID));
                    tmp.addAll(x.getAllMatrixAvailabilities(SOFINO_ID));
                    return tmp.stream();
                })
                .collect(Collectors.toList()))
            .extracting(MatrixAvailability::getBlockReasonKey)
            .containsOnly(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK, BlockReasonKey.SSKU_DEADSTOCK);

        // assert
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(SOFINO_ID,
                MatrixAvailabilityUtils.ssku("Какое-то название", 77, "sku5", false, null, null, "comment",
                    BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK)
            )
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.deadstock(77, "sku5", ROSTOV_ID)
            )
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.deadstock(77, "sku5", TOMILINO_ID)
            );
    }

    @Test
    public void testHidingSkipped() {
        sskuAvailabilityMatrixRepository.save(
            createMatrix(60, "sku4", TOMILINO_ID, false),
            createMatrix(60, "sku4", ROSTOV_ID, false)
        );

        var availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID)
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.ssku("Напильники для PGaaS!", 60, "sku4", false, null, null, null, null)
            );

        switchControlService.setForceSkipSskuConstraints(true);
        switchControlService.invalidateCache();

        availabilityMap = service.computeAvailability(List.of(
            new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(TOMILINO_ID, ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(60, "sku4");
    }

    public SskuAvailabilityMatrix createMatrix(int supplierId, String ssku, long warehouseId,
                                               boolean available) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setCreatedLogin("user");
    }

    @Test
    public void testSeveralAvailabilities() {
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(ARCHIVE_MSKU.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );
        var sku4 = serviceOfferReplicaRepository.save(serviceOfferReplicaRepository
            .findOffers(new ServiceOfferReplicaFilter().setBusinessIds(60).setShopSkus(List.of("sku4"))).get(0)
            .setMskuId(ARCHIVE_MSKU.getId())
        ).get(0);
        serviceOfferReplicaRepository.save(serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
                .setBusinessIds(60).setShopSkus(List.of("sku4"))).stream()
            .map(o -> o.setMskuId(sku4.getMskuId()))
            .collect(Collectors.toList())
        );
        sskuStatusRepository.save(sskuStatus(60, sku4.getShopSku(), OfferAvailability.DELISTED));

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.offerDelisted(60, "sku4"),
                MatrixAvailabilityUtils.mskuArchived(ARCHIVE_MSKU)
            );
    }

    @Test
    public void testSeveralAvailabilitiesWithAllowedStatus() {
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(ARCHIVE_MSKU.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setAvailable(true)
                .setCreatedLogin("unit-test")
                .setMarketSkuId(ARCHIVE_MSKU.getId())
                .setWarehouseId(ROSTOV_ID)
        );
        var sku4 = serviceOfferReplicaRepository.save(serviceOfferReplicaRepository
            .findOffers(new ServiceOfferReplicaFilter().setBusinessIds(60).setShopSkus(List.of("sku4"))).get(0)
            .setMskuId(ARCHIVE_MSKU.getId())
        ).get(0);
        serviceOfferReplicaRepository.save(serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
                .setBusinessIds(60).setShopSkus(List.of("sku4"))).stream()
            .map(o -> o.setMskuId(sku4.getMskuId()))
            .collect(Collectors.toList())
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext()
                .setWarehouseIds(ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(true, ARCHIVE_MSKU, ROSTOV, null, null, null),
                MatrixAvailabilityUtils.mskuArchived(ARCHIVE_MSKU)
            );
    }

    @Test
    public void searchShouldWorkEvenIfMskuNotSyncedFromMbo() {
        long marketSkuId = 30967937;

        var offer = serviceOfferReplicaRepository.save(offer(42, "shop-sku-test", marketSkuId)).get(0);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(offer.getBusinessId(), offer.getShopSku(), OfferAvailability.DELISTED));

        var shopSkuKey = new ServiceOfferKey(offer.getSupplierId(), offer.getShopSku());
        assertThat(deepmindMskuRepository.findById(marketSkuId)).isEmpty();

        var availabilityMap = service.computeAvailability(
            List.of(shopSkuKey),
            new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(shopSkuKey).findByWarehouseId(TOMILINO_ID)
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.offerDelisted(shopSkuKey)
            );
    }

    @Test
    public void searchShouldWorkEvenIfOfferHasNoCategoryId() {
        long marketSkuId = 30967937;

        var offer = serviceOfferReplicaRepository.save(offer(42, "shop-sku-test", marketSkuId).setCategoryId(99L))
            .get(0);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(sskuStatus(offer.getBusinessId(), offer.getShopSku(), OfferAvailability.DELISTED));

        var shopSkuKey = new ServiceOfferKey(offer.getSupplierId(), offer.getShopSku());
        assertThat(deepmindMskuRepository.findById(marketSkuId)).isEmpty();

        var availabilityMap = service.computeAvailability(
            List.of(shopSkuKey),
            new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(shopSkuKey).findByWarehouseId(TOMILINO_ID)
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.offerDelisted(shopSkuKey)
            );
    }

    @Test
    public void searchShouldNotFetchFromMbo() {
        sskuStatusRepository.save(sskuStatus(60, "sku4",
            OfferAvailability.DELISTED));

        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4").findByWarehouseId(TOMILINO_ID)
            .containsExactlyInAnyOrder(MatrixAvailabilityUtils.offerDelisted(60, "sku4"));
    }

    @Test
    public void searchShouldWorkEvenIfNoApprovedMapping() {
        var shopSkuKey = new ServiceOfferKey(42, "shop-sku-test");

        var availabilityMap = service.computeAvailability(
            List.of(shopSkuKey), new ShopSkuAvailabilityContext()
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .doesNotContainShopSkuKey(shopSkuKey);
    }

    @Test
    public void searchShouldWorkEvenMskuIsDeletedInMbo() {
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(404040L)
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuArchived(MSKU_404040)
            );
    }

    @Test
    public void searchShouldWorkEvenMskuIsDeletedInDB() {
        deepmindMskuRepository.delete(404040L);

        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(404040L)
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.mskuArchived(MSKU_404040)
            );
    }

    @Test
    public void searchShouldWorkEvenMskuIsMissing() {
        var sku4 = serviceOfferReplicaRepository.save(serviceOfferReplicaRepository
            .findOffers(new ServiceOfferReplicaFilter().setBusinessIds(60).setShopSkus(List.of("sku4"))).get(0)
            .setMskuId(100500L)
        ).get(0);
        sskuStatusRepository.save(sskuStatus(60, sku4.getShopSku(), OfferAvailability.DELISTED));

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID,
                MatrixAvailabilityUtils.offerDelisted(60, "sku4")
            );
    }

    @Test
    public void testShouldProcessOnlyBlueOffers() {
        var offer2 = serviceOfferReplicaRepository.save(
            offer(80, "sku-from-biz-supplier", 404040L)
                .setTitle("sku-from-biz-supplier")
                .setSupplierId(83)
                .setCategoryId(1L)
                .setSupplierType(SupplierType.BUSINESS)
        ).get(0);
        var offer3 = serviceOfferReplicaRepository.save(
            offer(82, "sku-from-biz-supplier", 404040L)
                .setSupplierId(84)
                .setTitle("sku-from-biz-supplier")
                .setCategoryId(1L)
                .setSupplierType(SupplierType.THIRD_PARTY)
        ).get(0);

        serviceOfferReplicaRepository.save(offer2, offer3);


        sskuStatusRepository.save(
            sskuStatus(60, "sku4", OfferAvailability.DELISTED),
            sskuStatus(83, offer2.getShopSku(), OfferAvailability.DELISTED),
            sskuStatus(84, offer3.getShopSku(), OfferAvailability.DELISTED)
        );

        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(83, "sku-from-biz-supplier"),
                new ServiceOfferKey(84, "sku-from-biz-supplier")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(TOMILINO_ID)
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrderShopSkuKeys(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(84, "sku-from-biz-supplier")
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(TOMILINO_ID, MatrixAvailabilityUtils.offerDelisted(60, "sku4"));
        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(84, "sku-from-biz-supplier")
            .containsExactlyInAnyOrder(TOMILINO_ID, MatrixAvailabilityUtils.offerDelisted(84, "sku-from-biz-supplier"));
    }

    @Test
    public void testProcessOnCrossdockWarehouse() {
        partnerRelationRepository.save(partnerRelation(60, 1, CROSSDOCK_SOFINO_ID, PartnerRelationType.CROSSDOCK));
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.DELISTED));
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(404040L)
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );
        mskuAvailabilityMatrixRepository.save(mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO_ID));

        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(CROSSDOCK_SOFINO_ID)
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrderShopSkuKeys(new ServiceOfferKey(60, "sku4"))
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(CROSSDOCK_SOFINO_ID,
                MatrixAvailabilityUtils.offerDelisted(60, "sku4"),
                MatrixAvailabilityUtils.mskuArchived(MSKU_404040),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, CROSSDOCK_SOFINO, null, null, null)
            );

        // test that some blocked availabilities not preserved
        ShopSkuAvailability sskuAvailability = availabilityMap.get(new ServiceOfferKey(60, "sku4"));
        var blockedMatrixAvailabilities = sskuAvailability.getBlockedMatrixAvailabilities(CROSSDOCK_SOFINO);
        Assertions.assertThat(blockedMatrixAvailabilities)
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, CROSSDOCK_SOFINO, null, null, null)
            );
    }

    @Test
    public void testProcessOnSortingCenterWarehouse() {
        partnerRelationRepository.save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));
        sskuStatusRepository.save(sskuStatus(60, "sku4", OfferAvailability.DELISTED));
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(404040L)
            .setMskuStatus(MskuStatusValue.ARCHIVE)
        );
        mskuAvailabilityMatrixRepository.save(mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()));

        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(SORTING_CENTER_1.getId())
        );
        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .containsExactlyInAnyOrderShopSkuKeys(new ServiceOfferKey(60, "sku4"))
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.offerDelisted(60, "sku4"),
                MatrixAvailabilityUtils.mskuArchived(MSKU_404040),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            );

        // test that some blocked availabilities not preserved
        ShopSkuAvailability sskuAvailability = availabilityMap.get(new ServiceOfferKey(60, "sku4"));
        var blockedMatrixAvailabilities = sskuAvailability.getBlockedMatrixAvailabilities(SORTING_CENTER_1);
        Assertions.assertThat(blockedMatrixAvailabilities)
            .containsExactlyInAnyOrder(
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            );
    }

    /**
     * Тест проверяет, что если поставщик возит только в сортцентр, а блокировки выставлены на кроссдок склад,
     * то блокировки на кроссдок склад не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesNotRelationWarehousesOfDropshipSupplier() {
        // Если у поставщика есть связка to_warehouse, то именно на этот склад должны выставляться блокировки
        partnerRelationRepository.save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_505050, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_505050, false, CROSSDOCK_SOFINO.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ),
            new ShopSkuAvailabilityContext().setWarehouseIds(CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            )
            .doesNotContainForWarehouseIds(CROSSDOCK_SOFINO.getId());
        DeepmindAssertions.assertAvailability(availabilityMap.values()).doesNotContainShopSkuKey(77, "sku5");
    }

    /**
     * Тест проверяет, что если поставщик возит только в кроссдок склад, а блокировки выставлены на сорт.центр,
     * то блокировки на сорт.центр не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesNotRelationWarehousesOfSupplierWithToWarehouse() {
        // Если у поставщика есть связка to_warehouse, то именно на этот склад должны выставляться блокировки
        partnerRelationRepository.save(partnerRelation(60, 1, CROSSDOCK_ROSTOV_ID, PartnerRelationType.CROSSDOCK));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, CROSSDOCK_ROSTOV.getId()),
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, MARSHRUT.getId()),
            mskuMatrix(MSKU_505050, false, CROSSDOCK_ROSTOV.getId()),
            mskuMatrix(MSKU_505050, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_505050, false, MARSHRUT.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(CROSSDOCK_ROSTOV.getId(), SORTING_CENTER_1.getId(), MARSHRUT.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(CROSSDOCK_ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, CROSSDOCK_ROSTOV, null, null, null)
            )
            .containsExactlyInAnyOrder(MARSHRUT_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, MARSHRUT, null, null, null)
            )
            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId());

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(MARSHRUT_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_505050, MARSHRUT, null, null, null)
            )
            .doesNotContainForWarehouseIds(CROSSDOCK_ROSTOV_ID, SORTING_CENTER_1.getId());
    }

    /**
     * Тест проверяет, что если поставщик возит только в сортцентр, а блокировки выставлены и на ФФ склады,
     * то блокировки будут учитываться как на СОРТ.ЦЕНТР, так и на ФФ склад. На кроссдок не будут
     */
    @Test
    public void testNotIgnoreAvailabilitiesOnFFWarehouseNotRelationWarehousesOfDropshipSupplier() {
        // Если у поставщика есть связка to_warehouse, то именно на этот склад должны выставляться блокировки
        partnerRelationRepository.save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(),
                CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            )
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, ROSTOV, null, null, null)
            )
            .doesNotContainForWarehouseIds(CROSSDOCK_SOFINO.getId());
    }

    @Test
    public void testIgnoreAvailabilitiesWithNoToWarehouse() {
        partnerRelationRepository.save(partnerRelation(60, 1, null, PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId()),
            mskuMatrix(MSKU_505050, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_505050, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId())
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, ROSTOV, null, null, null)
            );
        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(77, "sku5")
            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId())
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_505050, ROSTOV, null, null, null)
            );
    }

    /**
     * Тест проверяет, что если поставщик возит только по модели дропшип, а блокировки выставлены на фф склад,
     * то блокировки на фф склад не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesOnFFWarehouseOfDropshipSupplier() {
        // Настраиваем, что поставщик возит только по модели дропшип
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
                .setDropship(true).setFulfillment(false).setCrossdock(false));
        partnerRelationRepository
            .save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, MARSHRUT.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(MARSHRUT.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            )
            .doesNotContainForWarehouseIds(MARSHRUT.getId());
    }

    /**
     * Тест проверяет, что если поставщик возит только по модели кроссдок, а блокировки выставлены на фф склад,
     * то блокировки на фф склад не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesOnFFWarehouseOfCrossdockSupplier() {
        // Настраиваем, что поставщик возит только по модели кроссдок
        deepmindSupplierRepository.save(
            deepmindSupplierRepository.findById(60).orElseThrow()
                .setCrossdock(true).setFulfillment(false).setDropship(false));
        partnerRelationRepository
            .save(partnerRelation(60, 1, CROSSDOCK_ROSTOV.getId(), PartnerRelationType.CROSSDOCK));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, CROSSDOCK_ROSTOV.getId()),
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, MARSHRUT.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext()
                .setWarehouseIds(CROSSDOCK_ROSTOV.getId(), SORTING_CENTER_1.getId(), MARSHRUT.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(CROSSDOCK_ROSTOV_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, CROSSDOCK_ROSTOV, null, null, null)
            )
            .containsExactlyInAnyOrder(MARSHRUT_ID,
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, MARSHRUT, null, null, null)
            )
            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId());
    }

    /**
     * Тест проверяет, что если поставщик возит по модели дропшип и FF, а блокировки выставлены и на ФФ склады,
     * то блокировки будут учитываться как на СОРТ.ЦЕНТР, так и на ФФ склад. На кроссдок не будут
     */
    @Test
    public void testNotIgnoreAvailabilitiesOnCrossdockWarehouseOfFFAndDropshipSupplier() {
        // Настраиваем, что поставщик возит только по модели кроссдок
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
                .setDropship(true).setFulfillment(true).setCrossdock(false));
        partnerRelationRepository
            .save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(),
                CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            )
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, ROSTOV, null, null, null)
            )
            .doesNotContainForWarehouseIds(CROSSDOCK_SOFINO.getId());
    }

    /**
     * Тест проверяет, что если поставщик возит по модели дропшип, но связки нет, то блокировки не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesOnDropshipSupplierWithoutToWarehouse() {
        // Настраиваем, что поставщик возит только по модели дропшип
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
            .setDropship(true).setFulfillment(true).setCrossdock(false));
        partnerRelationRepository
            .save(partnerRelation(60, 1, null, PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(),
                CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, ROSTOV, null, null, null)
            )
            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId(), CROSSDOCK_SOFINO.getId());
    }

    /**
     * Тест проверяет, что если поставщик имеет связку, но не имеет признак, то блокировки не будут учитываться.
     */
    @Test
    public void testIgnoreAvailabilitiesOnDropshipSupplierWithoutDropshipMark() {
        // Настраиваем, что поставщик возит только по модели дропшип
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
            .setDropship(false).setFulfillment(true).setCrossdock(false));
        partnerRelationRepository
            .save(partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(),
                CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, ROSTOV, null, null, null)
            )
            // MBI-55595 Неправильная разметка поставщиков
//            .doesNotContainForWarehouseIds(SORTING_CENTER_1.getId(), CROSSDOCK_SOFINO.getId());
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            )
            .doesNotContainForWarehouseIds(CROSSDOCK_SOFINO.getId());
    }

    /**
     * Тест проверяет, что если поставщик имеет связку, но не имеет признак, то блокировки не будут учитываться.
     */
    @Test
    public void testSeveralFromWarehouseIdInPartnerRelation() {
        // Настраиваем, что поставщик возит только по модели дропшип
        deepmindSupplierRepository
            .save(deepmindSupplierRepository.findById(60).orElseThrow()
                .setDropship(true).setFulfillment(false).setCrossdock(false));
        var partnerRelation = partnerRelation(60, 1, SORTING_CENTER_1.getId(), PartnerRelationType.DROPSHIP);
        partnerRelationRepository.save(partnerRelation.setFromWarehouseIds(1L, 1000L));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_404040, false, SORTING_CENTER_1.getId()),
            mskuMatrix(MSKU_404040, false, CROSSDOCK_SOFINO.getId()),
            mskuMatrix(MSKU_404040, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(60, "sku4")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId(),
                CROSSDOCK_SOFINO.getId(), SORTING_CENTER_1.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(SORTING_CENTER_1.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, SORTING_CENTER_1, null, null, null)
            );
    }

    @Test
    public void testCreateAvailabilitiesForRealSuppliersWhichAreNotFF() {
        // Настраиваем, что поставщик ни возит ни по одной из моделей
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(77).orElseThrow().setFulfillment(false));

        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(MSKU_505050, false, ROSTOV.getId())
        );
        var availabilityMap = service.computeAvailability(
            List.of(new ServiceOfferKey(77, "sku5")),
            new ShopSkuAvailabilityContext().setWarehouseIds(ROSTOV.getId())
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values()).findByShopSkuKey(77, "sku5")
            .containsExactlyInAnyOrder(ROSTOV.getId(),
                MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_505050, ROSTOV, null, null, null));
    }

    @Test
    public void testSskuInInterval() {
        sskuAvailabilityMatrixRepository.save(
            createMatrix(60, "sku4", ROSTOV_ID, false)
                .setDateFrom(LocalDate.of(2020, 1, 23))
        );

        // with date before
        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 1, 20))
                .setWarehouseIds(ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .isEmpty();

        // with date after
        availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(60, "sku4")
            ), new ShopSkuAvailabilityContext()
                .setInboundDate(LocalDate.of(2020, 1, 25))
                .setWarehouseIds(ROSTOV_ID)
        );

        DeepmindAssertions.assertAvailability(availabilityMap.values())
            .findByShopSkuKey(60, "sku4")
            .containsExactlyInAnyOrder(ROSTOV_ID,
                MatrixAvailabilityUtils.ssku("Напильники для PGaaS!", 60, "sku4", false,
                    LocalDate.of(2020, 1, 23), null, null, null)
            );
    }

    @Test
    public void testSofinoReturnWarehouseIgnoreAvailabilites() {
        sskuStatusRepository.save(sskuStatus(101, "sku100", OfferAvailability.DELISTED));


        var availabilityMap = service.computeAvailability(
            List.of(
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(102, "sku100"),
                new ServiceOfferKey(77, "sku5")
            ), new ShopSkuAvailabilityContext().setWarehouseIds(SOFINO_RETURN_ID)
        );

        Assert.assertTrue(availabilityMap.isEmpty());
    }

    private MskuAvailabilityMatrix mskuMatrix(Msku msku, boolean available, long warehouseId) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(msku.getId())
            .setAvailable(available)
            .setWarehouseId(warehouseId);
    }

    private MskuStatus mskuSeasonalStatus(Msku msku, long seasonId) {
        return new MskuStatus()
            .setMarketSkuId(msku.getId())
            .setMskuStatus(MskuStatusValue.SEASONAL)
            .setSeasonId(seasonId);
    }

    private CategoryAvailabilityMatrix categoryMatrix(long categoryId, long warehouseId) {
        return new CategoryAvailabilityMatrix()
            .setAvailable(false)
            .setCategoryId(categoryId)
            .setCreatedLogin("user")
            .setWarehouseId(warehouseId);
    }

    private PartnerRelation partnerRelation(int supplierId, long fromWarehouse, @Nullable Long toWarehouse,
                                            PartnerRelationType type) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setFromWarehouseIds(fromWarehouse)
            .setToWarehouseId(toWarehouse)
            .setRelationType(type);
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability offerAvailability) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(offerAvailability);
    }

    private ServiceOfferReplica offer(int supplierId, String ssku, long mskuId) {
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
