package ru.yandex.market.psku.postprocessor.service.metrics;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.MetricsDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.dto.metrics.ClusterOfferCountByStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsDaoTest extends BaseDBTest {
    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    MetricsDao metricsDao;


    @Test
    public void test() {
        ClusterMeta cm = new ClusterMeta();
        cm.setId(1L);
        cm.setStatus(ClusterStatus.REMAPPING_FINISHED);
        clusterMetaDao.insert(cm);

        ClusterContent cc1 = new ClusterContent();
        cc1.setClusterMetaId(1L);
        cc1.setType(ClusterContentType.DSBS);
        cc1.setStatus(ClusterContentStatus.REMAPPED);
        clusterContentDao.insert(cc1);

        ClusterContent cc2 = new ClusterContent();
        cc2.setClusterMetaId(1L);
        cc2.setType(ClusterContentType.DSBS);
        cc2.setStatus(ClusterContentStatus.REMAPPED);
        clusterContentDao.insert(cc2);

        List<ClusterOfferCountByStatus> clusterOfferCountByStatuses =
                metricsDao.readMappingModerationClustersAndOffersByStatus();
        assertThat(clusterOfferCountByStatuses)
                .containsExactly(new ClusterOfferCountByStatus(ClusterStatus.REMAPPING_FINISHED, 1L, 1L));
    }

}
