package ru.yandex.market.deepmind.tms.services;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
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

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.WarehouseCargotypeChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.WarehouseCargotypeChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.services.cargotype.SyncCargotypeService;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tms.executors.ImportWarehouseExecutor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mboc.common.msku.CargoType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;

@SuppressWarnings("checkstyle:magicnumber")
public class ImportWarehouseExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private WarehouseCargotypeChangedHandler warehouseCargotypeChangedHandler;

    private LMSClient client;
    private Warehouse marshrut;
    private Warehouse rostov;
    private Warehouse sofino;
    private Warehouse kvadro;
    private ImportWarehouseExecutor executor;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        marshrut = deepmindWarehouseRepository.save(deepmindWarehouseRepository.getById(MARSHRUT_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L)
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT));
        rostov = deepmindWarehouseRepository.save(deepmindWarehouseRepository.getById(ROSTOV_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L)
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT));
        // cargotypes of crossdock waerhouse should be the same
        deepmindWarehouseRepository.save(deepmindWarehouseRepository.getById(CROSSDOCK_ROSTOV_ID)
            .setCargoTypeLmsIds(1L, 2L, 3L, 11L)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        sofino = deepmindWarehouseRepository.save(deepmindWarehouseRepository.getById(SOFINO_ID)
            .setCargoTypeLmsIds(11L)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        // cargotypes of crossdock waerhouse should be the same
        deepmindWarehouseRepository.save(deepmindWarehouseRepository.getById(CROSSDOCK_SOFINO_ID)
            .setCargoTypeLmsIds(11L)
            .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK));
        kvadro = deepmindWarehouseRepository.save(
            new Warehouse().setId(4L)
                .setName("kvatro")
                .setCargoTypeLmsIds(11L)
                .setType(WarehouseType.DROPSHIP)
                .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP)
        );

        client = Mockito.mock(LMSClient.class);
        var syncCargotypeService = new SyncCargotypeService(
            deepmindWarehouseRepository,
            client,
            warehouseCargotypeChangedHandler
        );
        executor = new ImportWarehouseExecutor(deepmindWarehouseRepository, client, syncCargotypeService);
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

        Warehouse newWarehouse0 = deepmindWarehouseRepository.getById(100500L);
        Warehouse newWarehouse1 = deepmindWarehouseRepository.getById(100501L);
        Warehouse updatedWarehouse1 = deepmindWarehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = deepmindWarehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = deepmindWarehouseRepository.getById(sofino.getId());

        Assertions.assertThat(List.of(newWarehouse0, newWarehouse1,
            updatedWarehouse1, updatedWarehouse2, updatedWarehouse3
        ))
            .usingElementComparatorIgnoringFields("modifiedAt")
            .containsExactlyInAnyOrder(
                new Warehouse().setId(100500L)
                    .setName("Warehouse #100500")
                    .setType(WarehouseType.FULFILLMENT)
                    .setCargoTypeLmsIds(42L, CargoType.HEAVY_GOOD20.lmsId()
                    ),
                new Warehouse()
                    .setId(100501L)
                    .setName("Warehouse #100501")
                    .setType(WarehouseType.FULFILLMENT),
                marshrut.setName("Warehouse #145"),
                rostov.setName("Warehouse #147"),
                sofino.setName("Warehouse #172")
            );
    }

    @Test
    public void shouldImportCargoTypesAndCalendaringMode() {
        deepmindMskuRepository.save(
            msku(100, 10, Set.of(1L)),
            msku(200, 20, Set.of(2L)),
            msku(300, 30, Set.of(3L)),
            msku(400, 40, Set.of(4L)),
            msku(1100, 110, Set.of(11L)),
            msku(10000, 10, Set.of(1L, 4L)),
            msku(10001, 13, Set.of(1L, 4L))
        );

        Warehouse notSycnCargoWarehouse =
            deepmindWarehouseRepository.save(
                new Warehouse()
                    .setId(42L)
                    .setName("empty_cargo")
                    .setType(WarehouseType.DROPSHIP)
            );

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

        Warehouse updatedWarehouse1 = deepmindWarehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = deepmindWarehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = deepmindWarehouseRepository.getById(sofino.getId());
        assertThat(updatedWarehouse1.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(updatedWarehouse2.getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(1L);
        assertThat(updatedWarehouse3.getCargoTypeLmsIds())
            .isEmpty();

        List<WarehouseCargotypeChangedTask> queueTasks = getQueueTasks();
        assertThat(queueTasks)
            .usingElementComparatorOnFields("categoryId", "cargotypesIds")
            .containsExactlyInAnyOrder(
                new WarehouseCargotypeChangedTask(20L, Set.of(2L, 3L, 11L), null, null),
                new WarehouseCargotypeChangedTask(30L, Set.of(2L, 3L, 11L), null, null),
                new WarehouseCargotypeChangedTask(110L, Set.of(2L, 3L, 11L), null, null)
            );
    }

    @Test
    public void emptyCargotypesWontFail() {
        deepmindMskuRepository.save(
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

        Warehouse updatedWarehouse1 = deepmindWarehouseRepository.getById(marshrut.getId());
        Warehouse updatedWarehouse2 = deepmindWarehouseRepository.getById(rostov.getId());
        Warehouse updatedWarehouse3 = deepmindWarehouseRepository.getById(sofino.getId());
        assertThat(updatedWarehouse1.getCargoTypeLmsIds()).containsExactlyInAnyOrder(marshrut.getCargoTypeLmsIds());
        assertThat(updatedWarehouse2.getCargoTypeLmsIds()).containsExactlyInAnyOrder(rostov.getCargoTypeLmsIds());
        assertThat(updatedWarehouse3.getCargoTypeLmsIds()).containsExactlyInAnyOrder(sofino.getCargoTypeLmsIds());

        List<WarehouseCargotypeChangedTask> queueTasks = getQueueTasks();
        assertThat(queueTasks).isEmpty();
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

        Warehouse marshrutNew = deepmindWarehouseRepository.getById(MARSHRUT_ID);
        Warehouse sofinoNew = deepmindWarehouseRepository.getById(SOFINO_ID);
        Warehouse crossdockSofinoNew = deepmindWarehouseRepository.getById(CROSSDOCK_SOFINO_ID);

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

        Warehouse marshrutNew = deepmindWarehouseRepository.getById(MARSHRUT_ID);
        Warehouse rostovNew = deepmindWarehouseRepository.getById(ROSTOV_ID);
        Warehouse crossdockRostovNew = deepmindWarehouseRepository.getById(CROSSDOCK_ROSTOV_ID);

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

    private Msku msku(long mskuId, long categoryId, Collection<Long> cargotypes) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(categoryId)
            .setCargoTypes(cargotypes.toArray(Long[]::new))
            .setSkuType(SkuTypeEnum.SKU);
    }

    private Set<Integer> toInts(Long[] longs) {
        return Arrays.stream(longs)
            .mapToInt(Long::intValue)
            .boxed()
            .collect(Collectors.toSet());
    }
}
