package ru.yandex.market.deepmind.app.openapi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.deepmind.app.model.ApiError;
import ru.yandex.market.deepmind.app.model.AvailabilitiesByIntervalResponse;
import ru.yandex.market.deepmind.app.model.AvailabilitiesResponse;
import ru.yandex.market.deepmind.app.model.AvailabilityByIntervalInfo;
import ru.yandex.market.deepmind.app.model.AvailabilityInfo;
import ru.yandex.market.deepmind.app.model.GetAvailabilitiesByIntervalRequest;
import ru.yandex.market.deepmind.app.model.GetAvailabilitiesRequest;
import ru.yandex.market.deepmind.app.openapi.exception.ApiResponseEntityExceptionHandler;
import ru.yandex.market.deepmind.common.availability.AvailabilityInterval;
import ru.yandex.market.deepmind.common.availability.SeasonPeriodUtils;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailabilityByWarehouse;
import ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.availability.ssku.SskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceImpl;
import ru.yandex.market.deepmind.common.services.availability.SwitchControlServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.availability.PeriodResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepmind.app.openapi.utils.AvailabilityInfoConverter.convert;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SOFINO;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;

/**
 * Tests of {@link AvailabilitiesApiController}.
 */
public class AvailabilitiesApiControllerTest extends BaseOpenApiTest {
    private static final int BERU_ID = 465852;
    private static final Msku MSKU_404040 = TestUtils.newMsku(404040L, 33L);
    private static final Warehouse WAREHOUSE_998 = new Warehouse().setId(998L).setName("WAREHOUSE-NAME");

    private static final String GET_SSKU = "/api/v1/availabilities/get-ssku-warehouse-availability";
    private static final String GET_SSKU_BY_INTERVAL = "/api/v1/availabilities/get-ssku-warehouse-availability-by" +
        "-interval";

    private AvailabilitiesApiController controller;

    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Autowired
    private OffersConverter offersConverter;
    @Autowired
    private PartnerRelationRepository partnerRelationRepository;
    @Autowired
    private SskuStatusRepository sskuStatusRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private MskuAvailabilityMatrixChecker mskuAvailabilityMatrixChecker;
    private SskuAvailabilityMatrixChecker sskuAvailabilityMatrixChecker;

    @Before
    public void setUp() {
        offersConverter.clearCache();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(new Warehouse()
            .setId(163L).setName("Warehouse 163").setType(WarehouseType.FULFILLMENT));
        deepmindWarehouseRepository.save(new Warehouse()
            .setId(999999999L).setName("Warehouse 999999999").setType(WarehouseType.FULFILLMENT));

        deepmindMskuRepository.save(List.of(
            TestUtils.newMsku(100000, 22L),
            TestUtils.newMsku(404040L, 33L).setCargoTypes(1L, 2L, 3L),
            TestUtils.newMsku(303030L, 22L),
            TestUtils.newMsku(505050L, 22L)
        ));

        var cargoTypeCachingServiceMock = new DeepmindCargoTypeCachingServiceMock();
        cargoTypeCachingServiceMock.put(
            new CargoTypeSnapshot(1L, "cargo10", 10L),
            new CargoTypeSnapshot(2L, "cargo20", 20L),
            new CargoTypeSnapshot(3L, "cargo30", 30L)
        );

        mskuAvailabilityMatrixChecker = Mockito.mock(MskuAvailabilityMatrixChecker.class);
        Mockito.when(mskuAvailabilityMatrixChecker.computeMskuDeliveryConstraints(
            any(), any(), any(), anyMap())
        ).thenReturn(ImmutableMultimap.of());

        sskuAvailabilityMatrixChecker = Mockito.mock(SskuAvailabilityMatrixChecker.class);
        Mockito.when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(
            anyCollection(), anyMap(), any())
        ).thenReturn(Map.of());

        var shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceImpl(
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            deepmindMskuRepository,
                deepmindWarehouseRepository,
            mskuAvailabilityMatrixChecker,
            sskuAvailabilityMatrixChecker,
            new SwitchControlServiceMock(),
            partnerRelationRepository,
            sskuStatusRepository);

        controller = new AvailabilitiesApiController(
            serviceOfferReplicaRepository,
            shopSkuMatrixAvailabilityService,
                deepmindWarehouseRepository,
            offersConverter
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiResponseEntityExceptionHandler()).build();
    }

    @Test
    public void searchByKeysShouldCallMatrixWithCorrectArguments() throws Exception {
        ArgumentCaptor<Warehouse> mskuWarehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        ArgumentCaptor<LocalDate> fromDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Map> mskuCaptor = ArgumentCaptor.forClass(Map.class);

        ArgumentCaptor<Warehouse> categoryWarehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        ArgumentCaptor<Collection> categoriesCaptor = ArgumentCaptor.forClass(Collection.class);

        ArgumentCaptor<Map<Long, Warehouse>> cargoWarehouseCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> cargoMskuLmsIdsCaptor = ArgumentCaptor.forClass(Map.class);

        // with args
        var request = new GetAvailabilitiesRequest()
            .addKeysItem(new ServiceOfferKey(60, "sku4"))
            .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
            .addWarehouseIdsItem(TOMILINO_ID)
            .date(LocalDate.parse("2019-07-07"));
        postJson(GET_SSKU, request);

        Mockito.verify(mskuAvailabilityMatrixChecker, Mockito.atLeastOnce())
            .computeMskuDeliveryConstraints(mskuWarehouseCaptor.capture(), fromDateCaptor.capture(),
                toDateCaptor.capture(), mskuCaptor.capture());
        assertThat(mskuCaptor.getAllValues()).flatExtracting(Map::keySet)
            .containsExactlyInAnyOrder(404040L, 505050L);
        assertThat(mskuWarehouseCaptor.getAllValues().stream())
            .extracting(Warehouse::getId)
            .containsExactlyInAnyOrder(TOMILINO_ID);
        assertThat(fromDateCaptor.getAllValues().stream().distinct())
            .containsExactlyInAnyOrder(LocalDate.parse("2019-07-07"));
        assertThat(toDateCaptor.getAllValues().stream().distinct())
            .containsExactlyInAnyOrder(LocalDate.parse("2019-07-07"));

        Mockito.verify(mskuAvailabilityMatrixChecker, Mockito.atLeastOnce()).computeCategoryDeliveryConstraints(
            categoryWarehouseCaptor.capture(), categoriesCaptor.capture());
        assertThat(categoriesCaptor.getAllValues()).flatExtracting(v -> v).containsExactlyInAnyOrder(22L, 33L);
        assertThat(categoryWarehouseCaptor.getAllValues().stream()).extracting(Warehouse::getId)
            .containsExactlyInAnyOrder(TOMILINO_ID);

        Mockito.verify(mskuAvailabilityMatrixChecker, Mockito.atLeastOnce()).computeMskuCargoTypeConstraints(
            cargoWarehouseCaptor.capture(), cargoMskuLmsIdsCaptor.capture());
        assertThat(cargoMskuLmsIdsCaptor.getAllValues()).flatExtracting(Map::values)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                TestUtils.newMsku(404040L, 33L, List.of(1L, 2L, 3L)),
                TestUtils.newMsku(505050L, 22L, List.of())
            );
        assertThat(cargoWarehouseCaptor.getAllValues().stream().flatMap(v -> v.keySet().stream()).distinct())
            .containsExactlyInAnyOrder(TOMILINO_ID);
    }

    @Test
    public void searchByKeysShouldCallMatrixWithCorrectArgumentsWithoutParams() throws Exception {
        ArgumentCaptor<Warehouse> mskuWarehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        ArgumentCaptor<LocalDate> fromDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Map> mskuCaptor = ArgumentCaptor.forClass(Map.class);

        var request = new GetAvailabilitiesRequest()
            .addKeysItem(new ServiceOfferKey(60, "sku4"))
            .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
            .addWarehouseIdsItem(TOMILINO_ID);
        postJson(GET_SSKU, request);

        Mockito.verify(mskuAvailabilityMatrixChecker, Mockito.atLeastOnce()).computeMskuDeliveryConstraints(
            mskuWarehouseCaptor.capture(), fromDateCaptor.capture(), toDateCaptor.capture(), mskuCaptor.capture());
        assertThat(mskuCaptor.getAllValues()).flatExtracting(Map::keySet).containsExactlyInAnyOrder(404040L, 505050L);
        assertThat(mskuWarehouseCaptor.getAllValues().stream()).extracting(Warehouse::getId)
            .containsExactlyInAnyOrder(TOMILINO_ID);
        assertThat(fromDateCaptor.getAllValues().stream().distinct()).containsExactlyInAnyOrder(LocalDate.now());

        // For now category is not called
        Mockito.verify(mskuAvailabilityMatrixChecker, Mockito.never())
            .computeCategoryDeliveryConstraints(any(), anyList());
    }

    @Test
    public void searchShouldReturnWarehouseAvailabilityWithoutDate() throws Exception {
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(60, "sku4"));
        assertThat(response.getAvailabilities().get(0).getAllowInbound()).isTrue();

        var warehouseAvailabilities = response.getAvailabilities().get(0);
        assertThat(warehouseAvailabilities.getAllowInbound()).isTrue();
    }

    @Test
    public void searchShouldWorkWithRussianLetters() throws Exception {
        serviceOfferReplicaRepository.save(serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setBusinessIds(60)
            .setShopSkus(List.of("sku4"))).get(0).setShopSku("Русский1")
        );

        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "Русский1"))
                .addKeysItem(new ServiceOfferKey(60, "Русский1"))
                .addKeysItem(new ServiceOfferKey(60, "Й1"))
                .addKeysItem(new ServiceOfferKey(60, "Щ1"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        assertThat(responses).hasSize(1);
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(60, "Русский1"));
        assertThat(response.getAvailabilities().get(0).getAllowInbound()).isTrue();

        var warehouseAvailabilities = response.getAvailabilities().get(0);
        assertThat(warehouseAvailabilities.getAllowInbound()).isTrue();
    }

    @Test
    public void searchByKeysShouldNotFindPassBeruWithIncorrectShopSku() throws Exception {
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "no dot symbol!"))
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .date(LocalDate.parse("2019-07-07"))
        );

        assertThat(responses)
            .extracting(v -> v.getKey())
            .containsExactlyInAnyOrder(new ServiceOfferKey(BERU_ID, "000042.sku5"));
    }

    @Test
    public void searchByKeyWillBeCorrectIf1POfferIsPassed() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.offerDelisted(key));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        // assert
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(response.getAvailabilities()).containsExactly(
            convert(TOMILINO, List.of(MatrixAvailabilityUtils.offerDelisted(key)))
        );
    }

    @Test
    public void searchByKeyWillBeCorrectIf1POfferIsPassedInInterval() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.offerDelisted(key));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-01-02")).dateTo(LocalDate.parse("2020-01-03"))
        );

        // assert
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(response.getAvailabilities())
            .containsExactlyElementsOf(
                convert(LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-03"),
                    TOMILINO, List.of(MatrixAvailabilityUtils.offerDelisted(key))
                )
            );
    }

    @Test
    public void getBySskuOnIntervalWithoutBlocksReturnsNotNullAvailabilities() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.offerDelisted(key));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-01-02")).dateTo(LocalDate.parse("2020-01-03"))
        );

        // assert
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(response.getAvailabilities())
            .containsExactlyElementsOf(
                convert(LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-03"),
                    TOMILINO, List.of()
                )
            );
    }

    @Test
    public void searchReturnCorrectInterval() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-10", "08-20"));

        var season = MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse);
        var ssku = MatrixAvailabilityUtils.ssku("title", 77, "sku5", true,
            LocalDate.parse("2022-08-05"), LocalDate.parse("2022-08-15"), null, BlockReasonKey.OTHER);

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, new MatrixAvailabilityByWarehouse()
                .addAvailabilities(TOMILINO_ID, season)
                .addAvailabilities(TOMILINO_ID, ssku)));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(key)
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2022-08-01")).dateTo(LocalDate.parse("2022-08-30"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(key);
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2022-08-01"))
                    .dateTo(LocalDate.parse("2022-08-04"))
                    .allowInbound(false)
                    .infos(List.of(convert(season))),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2022-08-05"))
                    .dateTo(LocalDate.parse("2022-08-20"))
                    .allowInbound(true)
                    .infos(List.of()),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2022-08-21"))
                    .dateTo(LocalDate.parse("2022-08-30"))
                    .allowInbound(false)
                    .infos(List.of(convert(season)))
            );
    }

    @Test
    public void searchReturnCorrectIntervalWhenRequestedExceedsRightLimit() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-15", "12-01"));
        int year = LocalDate.ofInstant(Instant.ofEpochMilli(periodResponse.getToTimestamp()),
            TimeZone.getDefault().toZoneId()).getYear();
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO,
            periodResponse));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(key)
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse(year + "-11-25")).dateTo(LocalDate.parse(year + "-12-25"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(key);
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-11-25"))
                    .dateTo(LocalDate.parse(year + "-12-01"))
                    .allowInbound(true)
                    .infos(List.of()),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-12-02"))
                    .dateTo(LocalDate.parse(year + "-12-25"))
                    .allowInbound(false)
                    .infos(List.of(convert(MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse))))
            );
    }

    @Test
    public void searchReturnCorrectIntervalWhenRequestedExceedsLeftLimit() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-15", "12-01"));
        int year = LocalDate.ofInstant(Instant.ofEpochMilli(periodResponse.getToTimestamp()),
            TimeZone.getDefault().toZoneId()).getYear();
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO,
            periodResponse));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse(year + "-07-25")).dateTo(LocalDate.parse(year + "-11-26"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));

        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-07-25"))
                    .dateTo(LocalDate.parse(year + "-08-14"))
                    .allowInbound(false)
                    .infos(List.of(convert(MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse)))),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-08-15"))
                    .dateTo(LocalDate.parse(year + "-11-26"))
                    .allowInbound(true)
                    .infos(List.of())
            );
    }

    @Test
    public void searchReturnCorrectIntervalWhenRequestedExceedsBothLimits() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-15", "12-01"));
        int year = LocalDate.ofInstant(
            Instant.ofEpochMilli(periodResponse.getToTimestamp()), TimeZone.getDefault().toZoneId()).getYear();
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID,
            MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse(year + "-07-25")).dateTo(LocalDate.parse(year + "-12-26"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-07-25"))
                    .dateTo(LocalDate.parse(year + "-08-14"))
                    .allowInbound(false)
                    .infos(List.of(convert(MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse)))),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-08-15"))
                    .dateTo(LocalDate.parse(year + "-12-01"))
                    .allowInbound(true)
                    .infos(List.of()),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-12-02"))
                    .dateTo(LocalDate.parse(year + "-12-26"))
                    .allowInbound(false)
                    .infos(List.of(convert(MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse))))
            );
    }

    @Test
    public void searchReturnCorrectIntervalWhenRequestedExceedsNoLimits() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-15", "12-01"));
        int year = LocalDate.ofInstant(
            Instant.ofEpochMilli(periodResponse.getToTimestamp()), TimeZone.getDefault().toZoneId()).getYear();
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO,
            periodResponse));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse(year + "-08-25")).dateTo(LocalDate.parse(year + "-08-25"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse(year + "-08-25"))
                    .dateTo(LocalDate.parse(year + "-08-25"))
                    .allowInbound(true)
                    .infos(List.of())
            );
    }

    @Test
    public void searchReturnCorrectIntervalWhenRequestedPassedLimits() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var periodResponse = MskuAvailabilityMatrixChecker.convertToResponse(
            SeasonPeriodUtils.toCloseFutureDeliveryPeriod(LocalDate.now(), "08-15", "12-01"));
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID, MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO,
            periodResponse));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2021-11-25")).dateTo(LocalDate.parse("2021-12-25"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(availability.getAvailabilities())
            .containsExactlyElementsOf(
                convert(LocalDate.parse("2021-11-25"), LocalDate.parse("2021-12-25"),
                    TOMILINO, List.of(MatrixAvailabilityUtils.mskuInSeason(1L, TOMILINO, periodResponse))
                )
            );
    }


    @Test
    public void searchByKeyCheckIntervalToBeMostPrecise() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var ssku = MatrixAvailabilityUtils.ssku("title", BERU_ID, "000042.sku5", false,
            LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-04"), "comment", null);

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, new MatrixAvailabilityByWarehouse().addAvailabilities(TOMILINO_ID, ssku)));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-01-01")).dateTo(LocalDate.parse("2020-01-05"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2020-01-01"))
                    .dateTo(LocalDate.parse("2020-01-01"))
                    .allowInbound(true)
                    .infos(List.of()),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2020-01-02"))
                    .dateTo(LocalDate.parse("2020-01-04"))
                    .allowInbound(false)
                    .infos(List.of(convert(ssku))),
                new AvailabilityByIntervalInfo()
                    .warehouseId(TOMILINO_ID)
                    .dateFrom(LocalDate.parse("2020-01-05"))
                    .dateTo(LocalDate.parse("2020-01-05"))
                    .allowInbound(true)
                    .infos(List.of())
            );
    }

    @Test
    public void searchShouldCheck3pOffers() throws Exception {
        var msku = TestUtils.newMsku(191919L, 14L);
        deepmindMskuRepository.save(msku);

        serviceOfferReplicaRepository.save(
            new ServiceOfferReplica()
                .setBusinessId(84)
                .setSupplierId(84)
                .setShopSku("sku-msku-archived")
                .setTitle("sku-msku-archived")
                .setCategoryId(1L)
                .setSeqId(0L)
                .setMskuId(191919L)
                .setSupplierType(SupplierType.REAL_SUPPLIER)
                .setModifiedTs(Instant.now())
                .setAcceptanceStatus(OfferAcceptanceStatus.OK)
        );

        when(mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(any(), any(), any(), argThat(contains(191919L))))
            .thenReturn(ImmutableMultimap.of(
                191919L,
                MatrixAvailabilityUtils.mskuArchived(msku)
            ));

        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(84, "sku-msku-archived"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        assertThat(responses)
            .extracting(AvailabilitiesResponse::getKey)
            .containsExactlyInAnyOrder(new ServiceOfferKey(84, "sku-msku-archived"));
        var availabilitiesResponse = responses.get(0);
        var availabilityInfo = availabilitiesResponse.getAvailabilities().get(0);
        assertThat(availabilityInfo.getAllowInbound()).isFalse();
    }

    @Test
    public void searchShouldReturnFalseForDelistedOffers() throws Exception {
        sskuStatusRepository.save(new SskuStatus().setSupplierId(60).setShopSku("sku4")
            .setAvailability(OfferAvailability.DELISTED));

        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        ErrorInfo errorInfo = MbocErrors.get().mskuNotAvailableForDeliveryDelistedOffer();

        var availability4 = responses.stream()
            .filter(v -> v.getKey().getShopSku().equals("sku4"))
            .flatMap(v -> v.getAvailabilities().stream())
            .findFirst().orElseThrow();
        assertThat(availability4.getAllowInbound()).isFalse();
        assertThat(availability4.getInfos())
            .extracting(v -> v.getMessage().getCode())
            .containsExactlyInAnyOrder(errorInfo.getErrorCode());

        var availability5 = responses.stream()
            .filter(v -> v.getKey().getShopSku().equals("000042.sku5"))
            .flatMap(v -> v.getAvailabilities().stream())
            .findFirst().orElseThrow();
        assertThat(availability5.getAllowInbound()).isTrue();
        assertThat(availability5.getInfos()).isEmpty();
    }

    @Test
    public void searchShouldContainRenderedMessage() throws Exception {
        var sku4 = serviceOfferReplicaRepository.findOfferByKey(60, "sku4");
        List<PeriodResponse> periods = List.of(
            new PeriodResponse(0, 0, "1 августа", "7 ноября", "08-01", "11-07"),
            new PeriodResponse(0, 0, "20 декабря", "13 января", "12-20", "01-13")
        );
        when(mskuAvailabilityMatrixChecker.computeMskuDeliveryConstraints(
            any(), any(), any(), argThat(contains(sku4.getMskuId()))))
            .thenReturn(ImmutableMultimap.of(
                sku4.getMskuId(),
                MatrixAvailabilityUtils.mskuInSeason(sku4.getMskuId(), WAREHOUSE_998, periods)
            ));

        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        var availability4 = responses.stream()
            .filter(v -> v.getKey().getShopSku().equals("sku4"))
            .flatMap(v -> v.getAvailabilities().stream())
            .findFirst().orElseThrow();
        assertThat(availability4.getAllowInbound()).isFalse();
        assertThat(availability4.getInfos())
            .extracting(v -> v.getMessage().getText())
            .containsExactlyInAnyOrder("Склад 'WAREHOUSE-NAME' #998 не принимает этот товар в текущий момент," +
                " возможные даты поставок: 1 августа - 7 ноября, 20 декабря - 13 января");
    }

    @Test
    public void testReturnsBothCargoTypeAvailabilityAndOtherReason() throws Exception {
        var missingCargoTypes = MatrixAvailabilityUtils.mskuMissingCargoTypes(MSKU_404040, TOMILINO, List.of(2L),
            "cargo 2");
        // тест проверяет, что если оффер заблокирован одновременно
        // из-за карготипов и любой другой причине (например, потому что оффер delisted),
        // то будет возвращено 2 статуса
        Mockito.when(mskuAvailabilityMatrixChecker.computeMskuCargoTypeConstraints(anyMap(), anyMap()))
            .thenAnswer(invok -> {
                Map<Long, Warehouse> warehouseMap = invok.getArgument(0);
                if (warehouseMap.containsKey(TOMILINO_ID)) {
                    MatrixAvailabilityByWarehouse container = new MatrixAvailabilityByWarehouse();
                    container.addAvailabilities(TOMILINO_ID, missingCargoTypes);
                    return Map.of(MSKU_404040.getId(), container);
                } else {
                    return Map.of();
                }
            });

        // блокируем delisted оффер
        sskuStatusRepository.save(new SskuStatus().setSupplierId(60).setShopSku("sku4")
            .setAvailability(OfferAvailability.DELISTED));

        // блокируем по карготипу
        Msku msku = deepmindMskuRepository.findById(404040L).orElseThrow().setCargoTypes(1L);
        deepmindMskuRepository.save(msku);

        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        var availability = responses.stream()
            .filter(v -> v.getKey().equals(new ServiceOfferKey(60, "sku4")))
            .flatMap(v -> v.getAvailabilities().stream())
            .findFirst().orElseThrow();

        assertThat(availability.getAllowInbound()).isFalse();
        assertThat(availability.getInfos())
            .extracting(v -> v.getMessage().getText())
            .containsExactlyInAnyOrder(
                missingCargoTypes.toErrorInfo().toString(),
                MbocErrors.get().mskuNotAvailableForDeliveryDelistedOffer().toString()
            );
    }

    @Test
    public void testPassIllegalWarehouseIdWillFallWithBadRequest() throws Exception {
        var mvcResult = post400Json(GET_SSKU,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .addWarehouseIdsItem(292L)
        );

        var apiError = readJson(mvcResult, ApiError.class);
        assertThat(apiError.getMessage()).isEqualTo("Unknown warehouse_ids: [292]");
    }

    @Test
    public void testPassIllegalWarehouseUsingTypeWillFallWithBadRequest() throws Exception {
        var mvcResult = post400Json(GET_SSKU,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(163L)
                .addWarehouseIdsItem(171L)
                .addWarehouseIdsItem(999999999L)
        );

        var apiError = readJson(mvcResult, ApiError.class);
        assertThat(apiError.getMessage()).isEqualTo("Unknown fulfillment warehouses: [163, 999999999]");
    }

    @Test
    public void testPassIllegalWarehouseUsingTypeWillFallWithBadRequestInInterval() throws Exception {
        var mvcResult = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(163L)
                .addWarehouseIdsItem(171L)
                .addWarehouseIdsItem(999999999L)
                .dateFrom(LocalDate.parse("2020-01-02")).dateTo(LocalDate.parse("2020-01-03"))
        );
        var apiError = readJson(mvcResult, ApiError.class);
        assertThat(apiError.getMessage()).isEqualTo("Unknown fulfillment warehouses: [163, 999999999]");
    }

    @Test
    public void searchServiceOffers() throws Exception {
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(102, "sku100"))
                .addKeysItem(new ServiceOfferKey(202, "sku200"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        assertThat(responses)
            .extracting(s -> s.getKey())
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(102, "sku100")
                // supplier_id 202 weren't find, because it is MARKET_SHOP
            );
    }

    @Test
    public void searchShouldSkipBizOffers() throws Exception {
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(100, "sku100"))
                .addKeysItem(new ServiceOfferKey(200, "sku200"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        assertThat(responses).isEmpty();
    }

    @Test
    public void testPassWrongParamsInIntevalHandlerWillFallWithBadRequest() throws Exception {
        var mvcResult1 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .addWarehouseIdsItem(292L)
                .dateFrom(LocalDate.parse("2020-01-02")).dateTo(LocalDate.parse("2020-02-02"))
        );
        var apiError1 = readJson(mvcResult1, ApiError.class);
        assertThat(apiError1.getMessage()).isEqualTo("Unknown warehouse_ids: [292]");

        var mvcResult2 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );
        var apiError2 = readJson(mvcResult2, ApiError.class);
        assertThat(apiError2.getMessage()).isEqualTo("Both dateFrom and dateTo should be set");

        var mvcResult22 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateTo(LocalDate.parse("2020-02-02"))
        );
        var apiError22 = readJson(mvcResult22, ApiError.class);
        assertThat(apiError22.getMessage()).isEqualTo("Both dateFrom and dateTo should be set");

        var mvcResult3 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-02-02"))
        );
        var apiError3 = readJson(mvcResult3, ApiError.class);
        assertThat(apiError3.getMessage()).isEqualTo("Both dateFrom and dateTo should be set");

        var mvcResult4 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .dateFrom(LocalDate.parse("2020-01-01")).dateTo(LocalDate.parse("2020-02-02"))
        );
        var apiError4 = readJson(mvcResult4, ApiError.class);
        assertThat(apiError4.getMessage()).isEqualTo("warehouseIds should be set");

        var mvcResult5 = post400Json(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-02-02")).dateTo(LocalDate.parse("2020-01-01"))
        );
        var apiError5 = readJson(mvcResult5, ApiError.class);
        assertThat(apiError5.getMessage())
            .isEqualTo("dateFrom (2020-02-02) should be before or equal dateTo (2020-01-01)");

        // good check
        postJson(GET_SSKU_BY_INTERVAL,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-01-01")).dateTo(LocalDate.parse("2020-02-02"))
        );
    }

    @Test
    public void testReturnIntervalWarehouseIfIntervalWasPass() throws Exception {
        var archived = MatrixAvailabilityUtils.mskuArchived(MSKU_404040);
        var msku = MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, TOMILINO, LocalDate.parse("2020-05-07"),
            null, null);
        when(mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(any(), any(), any(), argThat(contains(MSKU_404040.getId()))))
            .thenReturn(ImmutableMultimap.<Long, MatrixAvailability>builder()
                .putAll(MSKU_404040.getId(), archived, msku).build()
            );

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-05-01")).dateTo(LocalDate.parse("2020-05-20"))
        );

        // assert
        var availability = responses.get(0);
        assertThat(availability.getKey()).isEqualTo(new ServiceOfferKey(60, "sku4"));
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                convert(TOMILINO_ID,
                    AvailabilityInterval.interval("2020-05-01", "2020-05-06"), archived),
                convert(TOMILINO_ID,
                    AvailabilityInterval.interval("2020-05-07", "2020-05-20"), archived, msku)
            );
    }

    /**
     * https://st.yandex-team.ru/DEEPMIND-485
     */
    @Test
    public void testDontChangeSingletonIntervalIfPassedDiffentIntervals() throws Exception {
        var msku = MatrixAvailabilityUtils.mskuInWarehouse(false, MSKU_404040, TOMILINO, null, null, null);
        when(mskuAvailabilityMatrixChecker
            .computeMskuDeliveryConstraints(any(), any(), any(), argThat(contains(MSKU_404040.getId()))))
            .thenReturn(ImmutableMultimap.<Long, MatrixAvailability>builder()
                .putAll(MSKU_404040.getId(), msku).build()
            );

        // first run with one interval
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-05-01")).dateTo(LocalDate.parse("2020-05-20"))
        );

        var availability = responses.get(0);
        assertThat(availability.getAvailabilities())
            .containsExactlyInAnyOrder(
                convert(TOMILINO_ID,
                    AvailabilityInterval.interval("2020-05-01", "2020-05-20"), msku)
            );

        // second run with other interval
        var responses2 = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(60, "sku4"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.parse("2020-05-01")).dateTo(LocalDate.parse("2020-05-30"))
        );

        var availability2 = responses2.get(0);
        assertThat(availability2.getAvailabilities())
            .containsExactlyInAnyOrder(
                convert(TOMILINO_ID,
                    AvailabilityInterval.interval("2020-05-01", "2020-05-30"), msku)
            );
    }

    @Test
    public void testPriority() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID,
            MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 77, "", TOMILINO,
                List.of(SOFINO), "", BlockReasonKey.OTHER));
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID,
            MatrixAvailabilityUtils.ssku("", 77, "sku5", true, null, null, "", BlockReasonKey.OTHER));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU, AvailabilitiesResponse.class,
            new GetAvailabilitiesRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
        );

        // assert
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(response.getAvailabilities()).containsExactly(
            new AvailabilityInfo()
                .warehouseId(TOMILINO_ID)
                .infos(List.of())
                .allowInbound(true) // because ssku priority is higher then supplier
        );
    }

    @Test
    public void testPriorityByInterval() throws Exception {
        // arrange
        var key = new ServiceOfferKey(77, "sku5");
        var matrixAvailabilityByWarehouse = new MatrixAvailabilityByWarehouse();
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID,
            MatrixAvailabilityUtils.sskuSupplierInWarehouse(false, 77, "", TOMILINO,
                List.of(SOFINO), "", BlockReasonKey.OTHER));
        matrixAvailabilityByWarehouse.addAvailabilities(TOMILINO_ID,
            MatrixAvailabilityUtils.ssku("", 77, "sku5", true, null, null, "", BlockReasonKey.OTHER));

        Mockito
            .when(sskuAvailabilityMatrixChecker.computeSskuDeliveryConstraints(argThat(contains(key)), anyMap(), any()))
            .thenReturn(Map.of(key, matrixAvailabilityByWarehouse));

        // act
        var responses = postJsonList(GET_SSKU_BY_INTERVAL, AvailabilitiesByIntervalResponse.class,
            new GetAvailabilitiesByIntervalRequest()
                .addKeysItem(new ServiceOfferKey(BERU_ID, "000042.sku5"))
                .addWarehouseIdsItem(TOMILINO_ID)
                .dateFrom(LocalDate.now()).dateTo(LocalDate.now())
        );

        // assert
        var response = responses.get(0);
        assertThat(response.getKey()).isEqualTo(new ServiceOfferKey(BERU_ID, "000042.sku5"));
        assertThat(response.getAvailabilities()).containsExactly(
            new AvailabilityByIntervalInfo()
                .warehouseId(TOMILINO_ID)
                .infos(List.of())
                .dateFrom(LocalDate.now())
                .dateTo(LocalDate.now())
                .allowInbound(true) // because ssku priority is higher then supplier
        );
    }

    @Test
    public void testSendWrongJsonShouldContainTextException() throws Exception {
        var mvcResult = mockMvc.perform(post(GET_SSKU)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"keys\":[{\"supplierId\":\"TEXT\",\"shopSku\":\"sku4\"}],\"warehouseIds\":[171]}"))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isBadRequest())
            .andReturn();

        var apiError = readJson(mvcResult, ApiError.class);
        assertThat(apiError.getMessage()).contains("Cannot deserialize value of type");
    }

    private Model createTestModel(long id, long categoryId) {
        return new Model()
            .setId(id)
            .setTitle("Model " + id)
            .setCategoryId(categoryId)
            .setModelType(Model.ModelType.GURU)
            .setVendorCodes(List.of("TEST", "TSET"))
            .setBarCodes(List.of("A", "B"));
    }

    private ArgumentMatcher<Map<Long, Msku>> contains(long mskuId) {
        return mskuIds -> mskuIds.containsKey(mskuId);
    }

    private ArgumentMatcher<Collection<ServiceOfferKey>> contains(ServiceOfferKey key) {
        return keys -> keys.contains(key);
    }
}
