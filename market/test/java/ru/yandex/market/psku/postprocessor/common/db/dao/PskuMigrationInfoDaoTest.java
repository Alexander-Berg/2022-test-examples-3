package ru.yandex.market.psku.postprocessor.common.db.dao;


import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuDeleteProcessStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MigratedPskusDeleteInfo;


public class PskuMigrationInfoDaoTest extends BaseDBTest {

    @Autowired
    MigratedPskusDeleteInfoDao migratedPskusDeleteInfoDao;

    @Test
    public void testGetAcceptedSkuIdsByPModelIds() {
        long pModel1Id = 11L;
        long pModel2Id = 12L;
        long sku1Id = 101L;
        long sku2Id = 102L;
        long sku3Id = 103L;
        long otherPModelId = 10001L;
        long otherSkuId = 10002L;
        createMigratedPskuPModelInfo(pModel1Id, PskuDeleteProcessStatus.PSKU_DELETED, sku1Id);
        createMigratedPskuPModelInfo(pModel1Id, PskuDeleteProcessStatus.PSKU_DELETED, sku2Id);
        createMigratedPskuPModelInfo(pModel2Id, PskuDeleteProcessStatus.PSKU_DELETED, sku3Id);
        // other
        createMigratedPskuPModelInfo(pModel2Id, PskuDeleteProcessStatus.NEW, otherSkuId);
        createMigratedPskuPModelInfo(otherPModelId, PskuDeleteProcessStatus.PMODEL_DELETED, otherSkuId);
        createMigratedPskuPModelInfo(pModel1Id, PskuDeleteProcessStatus.PMODEL_DELETED, otherSkuId);
        createMigratedPskuPModelInfo(otherPModelId, PskuDeleteProcessStatus.PSKU_DELETED, otherSkuId);
        Map<Long, List<Long>> pModelIdsToSkus =
            migratedPskusDeleteInfoDao.getAcceptedSkuIdsByPModelIds(ImmutableList.of(pModel1Id, pModel2Id));
        Assertions.assertThat(pModelIdsToSkus.size()).isEqualTo(2);
        Assertions.assertThat(pModelIdsToSkus.get(pModel1Id)).containsExactlyInAnyOrder(sku1Id, sku2Id);
        Assertions.assertThat(pModelIdsToSkus.get(pModel2Id)).containsExactlyInAnyOrder(sku3Id);
    }

    private void createMigratedPskuPModelInfo(long pModelId, PskuDeleteProcessStatus status,
                                                           long acceptingSkuId) {
        MigratedPskusDeleteInfo pskuMigrationInfo = new MigratedPskusDeleteInfo();
        pskuMigrationInfo.setAcceptingPskuId(acceptingSkuId);
        pskuMigrationInfo.setDeletingPskuPmodelId(pModelId);
        pskuMigrationInfo.setDeletingPskuId(1L);
        migratedPskusDeleteInfoDao.insertWithProcessingStatus(pskuMigrationInfo, status);
    }

}
