package ru.yandex.market.psku.postprocessor.service.remapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClusterRemapperServiceTest extends BaseDBTest {
    private static final String MBOC_TEST_USER = "mbocTestUser";

    private static final int HID = 91491;
    private static final int BIZ_ID1 = 100;
    private static final int BIZ_ID2 = 101;
    private static final String SHOP_SKU1 = "SHOP_SKU1";
    private static final String SHOP_SKU2 = "SHOP_SKU2";
    private static final String SHOP_SKU3 = "SHOP_SKU3";
    private static final long EXISTING_MSKU_ID1 = 200501L;
    private static final long EXISTING_MSKU_ID2 = 200502L;
    private static final long EXISTING_MSKU_ID3 = 200502L;
    private static final long EXISTING_PSKU_ID1 = 100501L;
    private static final long EXISTING_PSKU_ID2 = 200505L;
    int maxThreadsInPoolQueue = 1;
    ThreadPoolExecutor threadPoolExecutor;

    private MboMappingsServiceMock mboMappingsServiceMock = new MboMappingsServiceMock();
    private ClusterRemapperService remapperService;

    @Captor
    ArgumentCaptor<MboMappings.UpdateMappingsRequest> updateMappingsRequestCaptor;

    @Autowired
    private ClusterMetaDao clusterMetaDao;

    @Autowired
    private ClusterContentDao clusterContentDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        threadPoolExecutor = null;
        remapperService = new ClusterRemapperService(clusterMetaDao, clusterContentDao, mboMappingsServiceMock,
            MBOC_TEST_USER, 5_000, 500, threadPoolExecutor, maxThreadsInPoolQueue);
    }

    @Test
    public void testSimpleOneCluster() {
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID3);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID2);

        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long pskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        Long dsbsClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID2);
        Long fastClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.FAST_CARD, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID3);


        remapperService.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        ClusterContent clusterContent2 = clusterContentDao.fetchOneById(dsbsClusterId);
        ClusterContent clusterContent3 = clusterContentDao.fetchOneById(fastClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent3.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);

        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_MSKU_ID1);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU2)).isEqualTo(EXISTING_MSKU_ID2);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID2, SHOP_SKU3)).isEqualTo(EXISTING_MSKU_ID3);
    }


    @Test
    public void testUnmappedPskuSkipped() {
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);

        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long pskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        Long noExistingMappingPskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID2, EXISTING_MSKU_ID2);

        remapperService.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        ClusterContent clusterContent2 = clusterContentDao.fetchOneById(noExistingMappingPskuClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.REMAPPING_SKIPPED);

        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_MSKU_ID1);
    }

    @Test
    public void testSkuNotExistsMbocErrorStoredAsSkipped() {
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);

        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long targetSkuNotExistsClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID2);

        // simulate error, on which we should NOT retry next time
        mboMappingsServiceMock.addUpdateMappingError(BIZ_ID1, SHOP_SKU1, "No target sku",
            MboMappings.ProviderProductInfoResponse.ErrorKind.SKU_NOT_EXISTS);

        remapperService.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(targetSkuNotExistsClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPING_SKIPPED);

        // check mapping stays
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_PSKU_ID1);
    }

    @Test
    public void testOneOfRemapsFailsThenRemapCalledAgain() {
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID3);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID2);

        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long pskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        Long dsbsClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID2);
        Long fastClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.FAST_CARD, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID3);

        // simulate error, on which we should retry next time
        mboMappingsServiceMock.addUpdateMappingError(BIZ_ID1, SHOP_SKU2, "Test fail on second",
            MboMappings.ProviderProductInfoResponse.ErrorKind.CONCURRENT_MODIFICATION);

        remapperService.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.MAPPING_MODERATION_FINISHED); // still not finished

        // first and last cluster contents should be remapped, second - no
        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        ClusterContent clusterContent2 = clusterContentDao.fetchOneById(dsbsClusterId);
        ClusterContent clusterContent3 = clusterContentDao.fetchOneById(fastClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.NEW);
        assertThat(clusterContent3.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);

        // remove error and run again
        mboMappingsServiceMock.removeUpdateMappingError(BIZ_ID1, SHOP_SKU2);
        remapperService.doRemap();

        clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED); // finished now
        clusterContent2 = clusterContentDao.fetchOneById(dsbsClusterId);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);

        // check final mappings
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_MSKU_ID1);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU2)).isEqualTo(EXISTING_MSKU_ID2);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID2, SHOP_SKU3)).isEqualTo(EXISTING_MSKU_ID3);
    }

    @Test
    public void testMultipleClusters() {
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID3);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID2);

        long clusterMetaId1 = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        long clusterMetaId2 = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);

        Long pskuClusterId = createSkuClusterContent(clusterMetaId1, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        Long dsbsClusterId = createOfferClusterContent(clusterMetaId2, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID2);
        Long fastClusterId = createOfferClusterContent(clusterMetaId2, ClusterContentStatus.NEW,
            ClusterContentType.FAST_CARD, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID3);


        remapperService.doRemap();

        ClusterMeta clusterMeta1 = clusterMetaDao.findById(clusterMetaId1);
        ClusterMeta clusterMeta2 = clusterMetaDao.findById(clusterMetaId2);
        assertThat(clusterMeta1.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);
        assertThat(clusterMeta2.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        ClusterContent clusterContent2 = clusterContentDao.fetchOneById(dsbsClusterId);
        ClusterContent clusterContent3 = clusterContentDao.fetchOneById(fastClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent3.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);

        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_MSKU_ID1);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU2)).isEqualTo(EXISTING_MSKU_ID2);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID2, SHOP_SKU3)).isEqualTo(EXISTING_MSKU_ID3);
    }

    @Test
    public void testMultibatching() {
        ClusterRemapperService microBatchRemapper = new ClusterRemapperService(clusterMetaDao, clusterContentDao,
            mboMappingsServiceMock,
            MBOC_TEST_USER, 5_000, 2, threadPoolExecutor, maxThreadsInPoolQueue);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU1, EXISTING_PSKU_ID1);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID3);
        mboMappingsServiceMock.addMapping(HID, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID2);

        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long pskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        Long dsbsClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, BIZ_ID1, SHOP_SKU2, EXISTING_MSKU_ID2);
        Long fastClusterId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.FAST_CARD, BIZ_ID2, SHOP_SKU3, EXISTING_MSKU_ID3);


        microBatchRemapper.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.REMAPPING_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        ClusterContent clusterContent2 = clusterContentDao.fetchOneById(dsbsClusterId);
        ClusterContent clusterContent3 = clusterContentDao.fetchOneById(fastClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent2.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);
        assertThat(clusterContent3.getStatus()).isEqualTo(ClusterContentStatus.REMAPPED);

        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU1)).isEqualTo(EXISTING_MSKU_ID1);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID1, SHOP_SKU2)).isEqualTo(EXISTING_MSKU_ID2);
        assertThat(mboMappingsServiceMock.getMappingSkuId(BIZ_ID2, SHOP_SKU3)).isEqualTo(EXISTING_MSKU_ID3);
    }

    @Test
    public void skipClusterIfNothingToRemap() {
        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        long clusterContentOfferId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, 123L, "String offerId", -1L);
        long clusterContentSkuId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, 345L, -1L);
        long clusterContentTargetSkuId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, 346L, null);

        ClusterRemapperService microBatchRemapper = new ClusterRemapperService(clusterMetaDao, clusterContentDao,
            mboMappingsServiceMock,
            MBOC_TEST_USER, 5_000, 2, threadPoolExecutor, maxThreadsInPoolQueue);

        microBatchRemapper.doRemap();

        assertThat(ClusterStatus.PSKU_DELETE_FINISHED).isEqualTo(clusterMetaDao.findById(clusterMetaId).getStatus());
    }

    @Test
    public void doNotSkipClusterIfExistToRemap() {
        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        long clusterContentOfferId = createOfferClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.DSBS, 123L, "String offerId", -1L);
        long clusterContentSkuId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, 345L, 346L);
        long clusterContentTargetSkuId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
            ClusterContentType.PSKU, 346L, null);

        ClusterRemapperService microBatchRemapper = new ClusterRemapperService(clusterMetaDao, clusterContentDao,
            mboMappingsServiceMock,
            MBOC_TEST_USER, 5_000, 2, threadPoolExecutor, maxThreadsInPoolQueue);

        microBatchRemapper.doRemap();

        assertThat(ClusterStatus.REMAPPING_FINISHED).isEqualTo(clusterMetaDao.findById(clusterMetaId).getStatus());
    }

    @Test
    public void testForbiddenMapping() {
        long clusterMetaId = createClusterMetaWithStatus(ClusterStatus.MAPPING_MODERATION_FINISHED);
        Long pskuClusterId = createSkuClusterContent(clusterMetaId, ClusterContentStatus.NEW,
                ClusterContentType.PSKU, EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);

        clusterContentDao.forbidRemap(EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        remapperService.doRemap();

        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.PSKU_DELETE_FINISHED);

        ClusterContent clusterContent1 = clusterContentDao.fetchOneById(pskuClusterId);
        assertThat(clusterContent1.getStatus()).isEqualTo(ClusterContentStatus.DELETED);
    }

    private Long createSkuClusterContent(long clusterMetaId,
                                         ClusterContentStatus status,
                                         ClusterContentType type,
                                         long pskuId,
                                         Long targetSkuId) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setStatus(status);
        clusterContent.setType(type);
        clusterContent.setSkuId(pskuId);
        clusterContent.setTargetSkuId(targetSkuId);
        clusterContentDao.insert(clusterContent);
        return clusterContent.getId();
    }

    private Long createOfferClusterContent(long clusterMetaId,
                                           ClusterContentStatus status,
                                           ClusterContentType type,
                                           long bizId, String offerId,
                                           long targetSkuId) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setType(type);
        clusterContent.setStatus(status);
        clusterContent.setBusinessId(bizId);
        clusterContent.setOfferId(offerId);
        clusterContent.setTargetSkuId(targetSkuId);
        clusterContentDao.insert(clusterContent);
        return clusterContent.getId();
    }

    private Long createClusterMetaWithStatus(ClusterStatus clusterStatus) {
        ClusterMeta clusterMeta = new ClusterMeta();
        clusterMeta.setStatus(clusterStatus);
        clusterMetaDao.insert(clusterMeta);
        return clusterMeta.getId();
    }
}
