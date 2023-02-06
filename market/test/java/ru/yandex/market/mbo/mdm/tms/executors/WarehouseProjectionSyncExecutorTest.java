package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.BmdmWarehouseWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.WarehouseProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.WarehouseProjectionService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.warehouse.BmdmEntityToWarehouseConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse;

public class WarehouseProjectionSyncExecutorTest extends MdmBaseDbTestClass {

    @Autowired
    private WarehouseProjectionRepository warehouseProjectionRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private BmdmEntityToWarehouseConverter entityToWarehouseConverter;
    @Mock
    private MdmEntityStorageService mdmEntityStorageService;

    private WarehouseProjectionService warehouseProjectionService;
    private WarehouseProjectionSyncExecutor executor;
    private EnhancedRandom random;
    private List<MdmEntity> entities;

    @Before
    public void setUp() {
        storageKeyValueService.putValue(MdmProperties.REFRESH_WAREHOUSE_PROJECTION_REPO, true);
        storageKeyValueService.invalidateCache();
        random = TestDataUtils.defaultRandom(29L);
        Mockito.when(mdmEntityStorageService.getByExternalKeys(Mockito.any())).then(ivc ->
            GetMdmEntityResponse.newBuilder()
                .addAllMdmEntities(generateRandomEntities(5, false))
                .build());
        warehouseProjectionService = new WarehouseProjectionService(
            mdmEntityStorageService,
            entityToWarehouseConverter
        );
        executor = new WarehouseProjectionSyncExecutor(
            storageKeyValueService,
            warehouseProjectionRepository,
            warehouseProjectionService
        );
    }

    @Test
    public void testLoadWarehouseProjection() {
        executor.execute();
        List<BmdmWarehouseWrapper> items = warehouseProjectionRepository.findAll();
        List<BmdmWarehouseWrapper> expected = mdmEntitiesToPojo();
        Assertions.assertThat(items)
            .containsExactlyInAnyOrder(expected.toArray(new BmdmWarehouseWrapper[expected.size()]));

        // next run should replace all existent params with new
        executor.execute();
        var newItems = warehouseProjectionRepository.findAll();
        expected = mdmEntitiesToPojo();
        Assertions.assertThat(newItems)
            .containsExactlyInAnyOrder(expected.toArray(new BmdmWarehouseWrapper[expected.size()]));
    }

    @Test
    public void testLoadBannedValues() {
        Mockito.when(mdmEntityStorageService.getByExternalKeys(Mockito.any())).then(ivc ->
            GetMdmEntityResponse.newBuilder()
                .addAllMdmEntities(generateRandomEntities(5, true))
                .build());

        executor.execute();
        List<BmdmWarehouseWrapper> items = warehouseProjectionRepository.findAll();
        List<BmdmWarehouseWrapper> expected = mdmEntitiesToPojo();
        Assertions.assertThat(items)
            .containsExactlyInAnyOrder(expected.toArray(new BmdmWarehouseWrapper[expected.size()]));
    }

    private List<MdmEntity> generateRandomEntities(int number, boolean isBanned) {
        entities = new ArrayList();
        for(int i = 0; i < number; ++i) {
            entities.add(generateEntity(isBanned));
        }
        return entities;
    }

    private MdmEntity generateEntity(boolean isBanned) {
        return MdmEntity.newBuilder().putAllMdmAttributeValues(Map.of(
                KnownBmdmIds.WAREHOUSE_ID_ATTR_ID,
                MdmAttributeValues.newBuilder()
                    .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_ID_ATTR_ID)
                    .addValues(MdmAttributeValue.newBuilder().setInt64(Math.abs(random.nextLong())).build())
                    .build(),
                KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID,
                MdmAttributeValues.newBuilder()
                    .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID)
                    .addValues(MdmAttributeValue.newBuilder()
                        .setNumeric(String.valueOf(isBanned ? 0L : Math.abs(random.nextLong()))).build())
                    .build(),
                KnownBmdmIds.WAREHOUSE_NAME_ATTR_ID,
                MdmAttributeValues.newBuilder()
                    .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_NAME_ATTR_ID)
                    .addValues(MdmAttributeValue.newBuilder().setString(I18nStringUtils.fromSingleRuString("ABC")).build())
                    .build()))
            .setMdmEntityTypeId(KnownBmdmIds.WAREHOUSE_ENTITY_TYPE_ID)
            .build();
    }

    private List<BmdmWarehouseWrapper> mdmEntitiesToPojo() {
        return entities.stream()
            .map(entity -> new BmdmWarehouseWrapper()
                .setWarehouseId(entity.getMdmAttributeValuesMap().get(KnownBmdmIds.WAREHOUSE_ID_ATTR_ID)
                    .getValuesList().get(0).getInt64())
                .setPriority(Long.valueOf(entity.getMdmAttributeValuesMap()
                    .get(KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID).getValuesList().get(0).getNumeric()))
                .setName(I18nStringUtils.extractRuStrings(entity.getMdmAttributeValuesMap()
                    .get(KnownBmdmIds.WAREHOUSE_NAME_ATTR_ID).getValuesList().get(0).getString()).get(0))
                .setBanned(Long.valueOf(entity.getMdmAttributeValuesMap()
                    .get(KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID).getValuesList().get(0).getNumeric()) == 0L))
            .collect(Collectors.toList());
    }

}
