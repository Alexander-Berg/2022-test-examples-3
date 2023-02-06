package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.warehouse.BmdmEntityToWarehouseConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityRequest;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult;

public class UpdateWarehouseDataManualExecutorTest extends MdmBaseDbTestClass {

    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private BmdmEntityToWarehouseConverter entityToWarehouseConverter;
    @Mock
    private MdmEntityStorageService mdmEntityStorageService;

    private UpdateWarehouseDataManualExecutor executor;
    private ArgumentCaptor<SaveMdmEntityRequest> argument = ArgumentCaptor.forClass(SaveMdmEntityRequest.class);

    @Before
    public void setUp() {
        String data = "{ \"warehouse\": [{\"warehouse_id\": \"1\", \"warehouse_priority\": \"1\", " +
            "\"warehouse_name\": \"A\"},{\"warehouse_id\": \"2\", \"warehouse_priority\": \"2\", \"warehouse_name\": " +
            "\"B\"},{\"warehouse_id\": \"3\", \"warehouse_priority\": \"3\", \"warehouse_name\": \"C\"}] }";
        skv.putValue(MdmProperties.UPDATED_WAREHOUSE_DATA, data);
        Mockito.when(mdmEntityStorageService.save(argument.capture())).then(ivc ->
            SaveMdmEntityResponse.newBuilder()
                .addAllResults(getSaveMdmEntityResults(((SaveMdmEntityRequest) ivc.getArguments()[0])
                        .getMdmEntitiesList().stream().map(MdmEntity::getMdmId).collect(Collectors.toList()),
                    SaveMdmEntityResult.Code.OK))
                .build());

        executor = new UpdateWarehouseDataManualExecutor(skv, entityToWarehouseConverter, mdmEntityStorageService);
    }

    @Test
    public void testImportNewData() {
        executor.execute();
        var saveRequest = argument.getValue();
        checkSaveRequest(saveRequest.getMdmEntitiesList());
    }

    private void checkSaveRequest(List<MdmEntity> entities) {
        // remove all meta
        var entitiesWithOutMeta = entities.stream().map(entity -> {
            var result = MdmEntity.newBuilder()
                .setMdmEntityTypeId(entity.getMdmEntityTypeId());
            entity.getMdmAttributeValuesMap().entrySet().forEach(entry -> {
                result.putMdmAttributeValues(entry.getKey(),
                    MdmAttributeValues.newBuilder()
                        .addAllValues(entry.getValue().getValuesList().stream()
                            .map(singleEntity -> {
                                var newEntity = MdmAttributeValue.newBuilder();
                                if (singleEntity.hasInt64()) {
                                    newEntity.setInt64(singleEntity.getInt64());
                                }
                                if (singleEntity.hasString()) {
                                    newEntity.setString(singleEntity.getString());
                                }
                                if (singleEntity.hasNumeric()) {
                                    newEntity.setNumeric(singleEntity.getNumeric());
                                }
                                return newEntity.build();
                            })
                            .collect(Collectors.toList()))
                        .setMdmAttributeId(entry.getKey())
                        .build());
                });

                return result.build();
            })
            .collect(Collectors.toList());
        Assertions.assertThat(entitiesWithOutMeta)
            .containsExactlyInAnyOrderElementsOf(getExpectedEntitiesToSave());
    }

    private List<MdmEntity> getExpectedEntitiesToSave() {
        return List.of(
            MdmEntity.newBuilder().setMdmEntityTypeId(KnownBmdmIds.WAREHOUSE_ENTITY_TYPE_ID)
                .putAllMdmAttributeValues(getAttrValues(1, 1, "A")).build(),
            MdmEntity.newBuilder().setMdmEntityTypeId(KnownBmdmIds.WAREHOUSE_ENTITY_TYPE_ID)
                .putAllMdmAttributeValues(getAttrValues(2, 2, "B")).build(),
            MdmEntity.newBuilder().setMdmEntityTypeId(KnownBmdmIds.WAREHOUSE_ENTITY_TYPE_ID)
                .putAllMdmAttributeValues(getAttrValues(3, 3, "C")).build()
        );
    }

    private Map<Long, MdmAttributeValues> getAttrValues(int id, int priority, String name) {
        Map<Long, MdmAttributeValues> attrubuteValuesMap = new HashMap<>();
        attrubuteValuesMap.put(KnownBmdmIds.WAREHOUSE_ID_ATTR_ID,
            MdmAttributeValues.newBuilder()
                .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_ID_ATTR_ID)
                .addAllValues(List.of(MdmAttributeValue.newBuilder()
                    .setInt64(id)
                    .build()))
                .build());
        attrubuteValuesMap.put(KnownBmdmIds.WAREHOUSE_NAME_ATTR_ID,
            MdmAttributeValues.newBuilder()
                .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_NAME_ATTR_ID)
                .addAllValues(List.of(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString(name))
                    .build()))
                .build());
        attrubuteValuesMap.put(KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID,
            MdmAttributeValues.newBuilder()
                .setMdmAttributeId(KnownBmdmIds.WAREHOUSE_PRIORITY_ATTR_ID)
                .addAllValues(List.of(MdmAttributeValue.newBuilder()
                    .setNumeric(String.valueOf(priority))
                    .build()))
                .build());
        return attrubuteValuesMap;
    }

    private List<SaveMdmEntityResult> getSaveMdmEntityResults(List<Long> mdmIds, SaveMdmEntityResult.Code code) {
        return mdmIds.stream()
            .map(item -> getSaveMdmEntityResult(item, code))
            .collect(Collectors.toList());
    }

    private SaveMdmEntityResult getSaveMdmEntityResult(Long mdmId, SaveMdmEntityResult.Code code) {
        return SaveMdmEntityResult.newBuilder()
            .setMdmId(mdmId)
            .setCode(code)
            .setFrom(Instant.now().toEpochMilli())
            .build();
    }

}
