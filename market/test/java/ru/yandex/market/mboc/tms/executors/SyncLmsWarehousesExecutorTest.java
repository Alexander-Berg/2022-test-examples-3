package ru.yandex.market.mboc.tms.executors;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.cargotype.SyncCargotypeService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.WarehouseType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.WarehouseUsingType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Warehouse;
import ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository;
import ru.yandex.market.mboc.common.msku.CargoType;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.SOFINO_ID;

@SuppressWarnings("checkstyle:magicnumber")
public class SyncLmsWarehousesExecutorTest extends BaseDbTestClass {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private MskuRepository mskuRepository;

    private LMSClient client;
    private Warehouse marshrut;
    private Warehouse rostov;
    private Warehouse sofino;
    private Warehouse kvadro;
    private SyncLmsWarehousesExecutor executor;

    @Before
    public void setUp() {
        marshrut = warehouseRepository.save(warehouseRepository.getById(MARSHRUT_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L).setCalendaringEnabled(false)
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT));
        rostov = warehouseRepository.save(warehouseRepository.getById(ROSTOV_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L).setCalendaringEnabled(true)
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT));
        // cargotypes of crossdock waerhouse should be the same
        warehouseRepository.save(warehouseRepository.getById(CROSSDOCK_ROSTOV_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L).setCalendaringEnabled(true)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        sofino = warehouseRepository.save(warehouseRepository.getById(SOFINO_ID)
            .setCargoTypeLmsIds(11L).setCalendaringEnabled(false)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        // cargotypes of crossdock waerhouse should be the same
        warehouseRepository.save(warehouseRepository.getById(CROSSDOCK_SOFINO_ID)
            .setCargoTypeLmsIds(11L).setCalendaringEnabled(false)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        kvadro = warehouseRepository.save(new Warehouse().setId(4L).setName("kvatro")
            .setCargoTypeLmsIds(11L).setCalendaringEnabled(false).setType(WarehouseType.DROPSHIP)
            .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP));

        client = Mockito.mock(LMSClient.class);
        var syncCargotypeService = new SyncCargotypeService(warehouseRepository, client);
        executor = new SyncLmsWarehousesExecutor(warehouseRepository, client, syncCargotypeService);
    }

    @Test
    public void testImportNewFFWarehouse() {
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(100500, PartnerType.FULFILLMENT, false),
                createPartnerResponse(100501, PartnerType.FULFILLMENT, true),
                createPartnerResponse(marshrut.getId(), PartnerType.FULFILLMENT, false),
                createPartnerResponse(rostov.getId(), PartnerType.FULFILLMENT, true),
                createPartnerResponse(sofino.getId(), PartnerType.FULFILLMENT, true)
            ));
        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Collections.singletonList(
                new PartnerCargoTypesDto(100500L, 31L, Set.of(42, (int) CargoType.HEAVY_GOOD20.lmsId()))
            ));

        executor.execute();

        Warehouse newWarehouse0 = warehouseRepository.getById(100500L);
        Warehouse newWarehouse1 = warehouseRepository.getById(100501L);
        Warehouse updatedWarehouse1 = warehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = warehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = warehouseRepository.getById(sofino.getId());

        Assertions.assertThat(List.of(newWarehouse0, newWarehouse1,
                updatedWarehouse1, updatedWarehouse2, updatedWarehouse3
            ))
            .usingElementComparatorIgnoringFields("modifiedAt")
            .containsExactlyInAnyOrder(
                new Warehouse().setId(100500L).setName("Warehouse #100500")
                    .setCalendaringEnabled(false).setType(WarehouseType.FULFILLMENT)
                    .setCargoTypeLmsIds(42L, CargoType.HEAVY_GOOD20.lmsId()),
                new Warehouse().setId(100501L).setName("Warehouse #100501")
                    .setCalendaringEnabled(true).setType(WarehouseType.FULFILLMENT),
                marshrut.setName("Warehouse #145"),
                rostov.setName("Warehouse #147"),
                sofino.setName("Warehouse #172").setCalendaringEnabled(true)
            );
    }

    @Test
    public void testImportDropshipAndSortingCenterWarehouses() {
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(100, PartnerType.DROPSHIP, true),
                createPartnerResponse(101, PartnerType.DROPSHIP, false),
                createPartnerResponse(200, PartnerType.SORTING_CENTER, null)
            ));

        executor.execute();

        Warehouse updatedWarehouse1 = warehouseRepository.getById(100L);
        Warehouse updatedWarehouse2 = warehouseRepository.getById(101L);
        Warehouse updatedWarehouse3 = warehouseRepository.getById(200L);
        assertThat(updatedWarehouse1.getType()).isEqualTo(WarehouseType.DROPSHIP);
        assertThat(updatedWarehouse2.getType()).isEqualTo(WarehouseType.DROPSHIP);
        assertThat(updatedWarehouse3.getType()).isEqualTo(WarehouseType.SORTING_CENTER);
    }

    @Test
    public void shouldImportCargoTypesAndCalendaringMode() {
        mskuRepository.save(
            msku(100, 10, Set.of(1L)),
            msku(200, 20, Set.of(2L)),
            msku(300, 30, Set.of(3L)),
            msku(400, 40, Set.of(4L)),
            msku(1100, 110, Set.of(11L)),
            msku(10000, 10, Set.of(1L, 4L)),
            msku(10001, 13, Set.of(1L, 4L))
        );

        Warehouse notSycnCargoWarehouse =
            warehouseRepository.save(new Warehouse().setId(42L).setName("empty_cargo").setType(WarehouseType.DROPSHIP));

        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Arrays.asList(
                new PartnerCargoTypesDto(MARSHRUT_ID, 11L, Set.of(1, 2)),
                new PartnerCargoTypesDto(ROSTOV_ID, 21L, Set.of(1)),
                new PartnerCargoTypesDto(SOFINO_ID, 31L, Set.of()),
                new PartnerCargoTypesDto(notSycnCargoWarehouse.getId(), 41L, Set.of(1, 2, 3, 4, 11))
            ));
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(MARSHRUT_ID, PartnerType.FULFILLMENT, true),
                createPartnerResponse(ROSTOV_ID, PartnerType.FULFILLMENT, false),
                createPartnerResponse(SOFINO_ID, PartnerType.FULFILLMENT, null),
                createPartnerResponse(notSycnCargoWarehouse.getId(), PartnerType.DROPSHIP, null)
            ));

        executor.execute();

        Warehouse updatedWarehouse1 = warehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = warehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = warehouseRepository.getById(sofino.getId());
        assertThat(updatedWarehouse1.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(updatedWarehouse1.getCalendaringEnabled()).isEqualTo(true);
        assertThat(updatedWarehouse2.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L);
        assertThat(updatedWarehouse2.getCalendaringEnabled()).isEqualTo(false);
        assertThat(updatedWarehouse3.getCargoTypeLmsIds())
            .isEmpty();
        assertThat(updatedWarehouse3.getCalendaringEnabled()).isEqualTo(false);
    }

    @Test
    public void emptyCargotypesWontFail() {
        mskuRepository.save(
            msku(100, 10, Set.of(1L)),
            msku(200, 20, Set.of(2L)),
            msku(300, 30, Set.of(3L)),
            msku(400, 40, Set.of(4L)),
            msku(1100, 110, Set.of(11L)),
            msku(10000, 10, Set.of(1L, 4L)),
            msku(10001, 13, Set.of(1L, 4L))
        );

        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Arrays.asList(
                new PartnerCargoTypesDto(MARSHRUT_ID, 11L, toInts(marshrut.getCargoTypeLmsIds())),
                new PartnerCargoTypesDto(ROSTOV_ID, 21L, toInts(rostov.getCargoTypeLmsIds())),
                new PartnerCargoTypesDto(SOFINO_ID, 31L, toInts(sofino.getCargoTypeLmsIds()))
            ));
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(MARSHRUT_ID, PartnerType.FULFILLMENT, true),
                createPartnerResponse(ROSTOV_ID, PartnerType.FULFILLMENT, false),
                createPartnerResponse(SOFINO_ID, PartnerType.FULFILLMENT, null)
            ));

        executor.execute();

        Warehouse updatedWarehouse1 = warehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = warehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = warehouseRepository.getById(sofino.getId());
        assertThat(updatedWarehouse1.getCargoTypeLmsIds()).containsExactlyInAnyOrder(marshrut.getCargoTypeLmsIds());
        assertThat(updatedWarehouse1.getCalendaringEnabled()).isEqualTo(true);
        assertThat(updatedWarehouse2.getCargoTypeLmsIds()).containsExactlyInAnyOrder(rostov.getCargoTypeLmsIds());
        assertThat(updatedWarehouse2.getCalendaringEnabled()).isEqualTo(false);
        assertThat(updatedWarehouse3.getCargoTypeLmsIds()).containsExactlyInAnyOrder(sofino.getCargoTypeLmsIds());
        assertThat(updatedWarehouse3.getCalendaringEnabled()).isEqualTo(false);
    }

    @Test
    public void updateCargotypesInSofinoWillAlsoSyncCargotypesInCrossdockSofino() {
        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Arrays.asList(
                new PartnerCargoTypesDto(MARSHRUT_ID, 11L, Set.of(1, 2, 4, 5)),
                new PartnerCargoTypesDto(SOFINO_ID, 31L, Set.of(1, (int) CargoType.HEAVY_GOOD20.lmsId()))
            ));
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(MARSHRUT_ID, PartnerType.FULFILLMENT, true),
                createPartnerResponse(SOFINO_ID, PartnerType.FULFILLMENT, null)
            ));

        executor.execute();

        Warehouse marshrutNew = warehouseRepository.getById(MARSHRUT_ID);
        Warehouse sofinoNew = warehouseRepository.getById(SOFINO_ID);
        Warehouse crossdockSofinoNew = warehouseRepository.getById(CROSSDOCK_SOFINO_ID);

        assertThat(marshrutNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, 2L, 4L, 5L);
        assertThat(sofinoNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, CargoType.HEAVY_GOOD20.lmsId());
        assertThat(crossdockSofinoNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L);
    }

    @Test
    public void updateCargotypesInRostovWillAlsoSyncCargotypesInCrossdockRostov() {
        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Arrays.asList(
                new PartnerCargoTypesDto(MARSHRUT_ID, 11L, Set.of(1, 2, 4, 5)),
                new PartnerCargoTypesDto(ROSTOV_ID, 31L, Set.of(1, (int) CargoType.HEAVY_GOOD20.lmsId()))
            ));
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Arrays.asList(
                createPartnerResponse(MARSHRUT_ID, PartnerType.FULFILLMENT, true),
                createPartnerResponse(ROSTOV_ID, PartnerType.FULFILLMENT, null)
            ));

        executor.execute();

        Warehouse marshrutNew = warehouseRepository.getById(MARSHRUT_ID);
        Warehouse rostovNew = warehouseRepository.getById(ROSTOV_ID);
        Warehouse crossdockRostovNew = warehouseRepository.getById(CROSSDOCK_ROSTOV_ID);

        assertThat(marshrutNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, 2L, 4L, 5L);
        assertThat(rostovNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, CargoType.HEAVY_GOOD20.lmsId());
        assertThat(crossdockRostovNew.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L);
    }

    @Test
    public void syncShouldFailIfChangedTooManyCargotypes() {
        Mockito.when(client.getPartnerCargoTypes(Mockito.anyList()))
            .thenReturn(Collections.singletonList(
                new PartnerCargoTypesDto(ROSTOV_ID, 31L, Set.of(42, (int) CargoType.HEAVY_GOOD20.lmsId()))
            ));
        Mockito.when(client.searchPartners(Mockito.any()))
            .thenReturn(Collections.singletonList(
                createPartnerResponse(ROSTOV_ID, PartnerType.FULFILLMENT, null)
            ));

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("MBO-26872 There are too many <" + SyncCargotypeService.MAX_CHANGED_VALUES + ">" +
            " cargotypes changes. Warehouse: " + ROSTOV_ID + "; changed cargotypes: [1, 2, 3, 11, 42, 301];" +
            " If changes were made properly then use /api/dev/deepmind/sync-cargo-types to sync");

        executor.execute();
    }

    private PartnerResponse createPartnerResponse(long id, PartnerType partnerType,
                                                  @Nullable Boolean calendaringEnabled) {
        PartnerResponse.PartnerResponseBuilder builder = PartnerResponse.newBuilder()
            .id(id)
            .readableName("Warehouse #" + id)
            .partnerType(partnerType);
        if (calendaringEnabled == null) {
            builder.params(Collections.emptyList());
        } else {
            PartnerExternalParam calendaringParam = new PartnerExternalParam(
                PartnerExternalParamType.IS_CALENDARING_ENABLED.name(), "", String.valueOf(calendaringEnabled));
            builder.params(Collections.singletonList(calendaringParam));
        }
        return builder.build();
    }

    private Msku msku(long mskuId, long categoryId, Set<Long> cargotypes) {
        return new Msku()
            .setMarketSkuId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setParentModelId(1L)
            .setVendorId(1L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now())
            .setCategoryId(categoryId)
            .setCargoTypeLmsIds(cargotypes.toArray(Long[]::new));
    }

    private Set<Integer> toInts(Long[] longs) {
        return Arrays.stream(longs)
            .mapToInt(v -> v.intValue())
            .boxed()
            .collect(Collectors.toSet());
    }
}
