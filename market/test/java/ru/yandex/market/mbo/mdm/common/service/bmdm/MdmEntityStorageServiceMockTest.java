package ru.yandex.market.mbo.mdm.common.service.bmdm;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.MdmExternalKey;
import ru.yandex.market.mdm.http.MdmExternalKeys;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByExternalKeysRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByMdmIdsRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityRequest;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResponse;
import ru.yandex.market.mdm.http.search.MdmEntityIds;

/**
 * Докатились, тесты на тесты.
 */
public class MdmEntityStorageServiceMockTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmEntityStorageService mdmEntityStorageService;

    @Test
    public void testInsertAndGet() {
        //given
        long mskuId = 120;
        MdmEntity mdmEntity = MdmEntity.newBuilder()
            .setMdmEntityTypeId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID)
            .putMdmAttributeValues(
                KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID,
                TestBmdmUtils.createSingleInt64Value(
                    KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID, mskuId, "AUTO", "", 1232432)
                )
            .putMdmAttributeValues(
                Long.MAX_VALUE,
                TestBmdmUtils.createSingleStringValue(
                    Long.MAX_VALUE, "some random value", "MDM_OPERATOR", "albina-gima",
                    Instant.parse("2020-09-08T10:15:30.00Z").toEpochMilli()
                )
            )
            .build();

        //when
        SaveMdmEntityResponse saveMdmEntityResponse = mdmEntityStorageService.save(SaveMdmEntityRequest.newBuilder()
            .addMdmEntities(mdmEntity)
            .build());

        GetMdmEntityResponse byMdmIds = mdmEntityStorageService.getByMdmIds(
            GetMdmEntityByMdmIdsRequest.newBuilder()
                .setMdmIds(MdmEntityIds.newBuilder()
                    .setMdmEntityTypeId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID)
                    .addMdmIds(saveMdmEntityResponse.getResults(0).getMdmId())
                    .build())
                .build()
        );

        GetMdmEntityResponse byExternalKey = mdmEntityStorageService.getByExternalKeys(
            GetMdmEntityByExternalKeysRequest.newBuilder()
                .setMdmExternalKeys(MdmExternalKeys.newBuilder()
                    .setMdmEntityTypeId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID)
                    .addMdmExternalKeys(MdmExternalKey.newBuilder()
                        .addMdmAttributeValues(MdmAttributeValues.newBuilder()
                            .setMdmAttributeId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID)
                            .addValues(MdmAttributeValue.newBuilder().setInt64(mskuId))
                            .addMdmAttributePath(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID)
                            .build())
                        .build())
                    .build())
                .build()
        );

        //then
        Assertions.assertThat(byMdmIds.getMdmEntitiesCount()).isEqualTo(1);
        TestBmdmUtils.assertEqualsWithoutMdmIdAndUpdateMeta(byMdmIds.getMdmEntities(0), mdmEntity);

        Assertions.assertThat(byExternalKey.getMdmEntitiesCount()).isEqualTo(1);
        TestBmdmUtils.assertEqualsWithoutMdmIdAndUpdateMeta(byExternalKey.getMdmEntities(0), mdmEntity);
    }
}
