package manual.ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.psku.postprocessor.bazinga.deduplication.YtClusterDumper;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.deduplication.ClusterContentYtDto;

@Ignore
public class YtClusterDumperTest extends BaseDBTest {
    // Поставить сюда токен. Можно взять в секретнице
    private static final String YT_TOKEN = "";

    private static final int TOTAL_CLUSTER_METAS = 1000;
    private static final int CLUSTER_CONTENTS_PER_META = 4;
    private static final Random random = new Random();

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    ClusterMetaDao clusterMetaDao;

    YtClusterDumper ytClusterDumper;

    @Before
    public void setUp() throws Exception {
        Yt yt = YtUtils.http("hahn.yt.yandex.net", YT_TOKEN);
        String ytPath = "//home/market/development/ir/psku-post-processor/deduplication/dumped-clusters";
        ytClusterDumper = new YtClusterDumper(yt, ytPath, clusterContentDao);
        prepareData();
    }

    @Test
    public void testDbQuery() {
        List<ClusterContentYtDto> clusterContentsYtDtos = clusterContentDao.getClusterContentsYtDtos(100, null);
        Assertions.assertThat(clusterContentsYtDtos).hasSize(100);
    }

    @Test
    public void createYtDump() {
        ytClusterDumper.execute(null);
    }

    private void prepareData() {
        IntStream.range(0, TOTAL_CLUSTER_METAS).forEach(i -> {
            ClusterMeta clusterMeta = new ClusterMeta();
            clusterMeta.setCategoryId(random.nextLong());
            clusterMeta.setType(ClusterType.values()[random.nextInt(5)]);
            clusterMeta.setStatus(ClusterStatus.INVALID);
            clusterMetaDao.insert(clusterMeta);

            Long clusterMetaId = clusterMeta.getId();
            IntStream.range(0, CLUSTER_CONTENTS_PER_META).forEach(j -> {
                long skuId = random.nextLong();
                long offerId = random.nextLong();
                long businessId = random.nextLong();

                ClusterContent clusterContent = new ClusterContent();
                clusterContent.setClusterMetaId(clusterMetaId);
                clusterContent.setType(ClusterContentType.values()[random.nextInt(4)]);
                clusterContent.setSkuId(skuId % 2 == 0 ? skuId : null);
                clusterContent.setOfferId(offerId % 2 == 0 ? String.valueOf(offerId) : null);
                clusterContent.setBusinessId(businessId % 2 == 0 ? businessId : null);
                clusterContent.setTargetSkuId(random.nextLong());
                clusterContent.setSupposedTargetSkuId(random.nextLong());
                clusterContent.setStatus(ClusterContentStatus.CARD_CREATE_IN_PROCESS);
                clusterContentDao.insert(clusterContent);
            });
        });
    }
}
