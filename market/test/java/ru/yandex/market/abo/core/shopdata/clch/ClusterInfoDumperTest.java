package ru.yandex.market.abo.core.shopdata.clch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Created by antipov93@yndx-team.ru
 */
@Transactional("pgTransactionManager")
public class ClusterInfoDumperTest extends EmptyTest {

    @Autowired
    private ClusterInfoDumper clusterInfoDumper;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        pgJdbcTemplate.update("INSERT INTO shop (id, cpa, cpc) VALUES (1, 'ON', 'OFF'), (2, 'ON', 'OFF')");
        pgJdbcTemplate.update("INSERT INTO clch_cluster (id, shop_set_id, status) VALUES (1, 1, 0)");
        pgJdbcTemplate.update("INSERT INTO clch_shop_set (set_id, shop_id) VALUES (1, 1), (1, 2)");
    }

    @Test
    public void testUploadToYT() throws Exception {
        assertFalse(clusterInfoDumper.loadShopClusters().isEmpty());
    }
}
