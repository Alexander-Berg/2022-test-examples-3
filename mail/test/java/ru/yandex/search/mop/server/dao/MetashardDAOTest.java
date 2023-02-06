package ru.yandex.search.mop.server.dao;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.search.mop.common.services.Service;
import ru.yandex.search.mop.server.MopServerCluster;
import ru.yandex.test.util.TestBase;

public class MetashardDAOTest extends TestBase {
    @Test
    public void testGetHostGroupId() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster()) {
            cluster.apply("searchmap.sql");
            cluster.start();

            MetashardDAO dao = cluster.mopServer().metashardDAO();

            int hostGroupId = dao.getHostGroupId(Service.IEX, 0);
            Assert.assertEquals(2, hostGroupId);

            hostGroupId = dao.getHostGroupId(Service.CORP_CHANGE_LOG_OFFLINE, 0);
            Assert.assertEquals(4, hostGroupId);

            hostGroupId = dao.getHostGroupId(Service.CHANGE_LOG, 1);
            Assert.assertEquals(1, hostGroupId);

            try {
                dao.getHostGroupId(Service.CHANGE_LOG, 2);
                Assert.fail("Expected java.sql.SQLException");
            } catch (SQLException e) {
                Assert.assertEquals(
                    "Not found host_group_id for label: 2, and service: change_log",
                    e.getMessage());
            }
        }
    }

    @Test
    public void testGetMaxVersion() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster()) {
            cluster.apply("searchmap.sql");
            cluster.start();

            MetashardDAO dao = cluster.mopServer().metashardDAO();

            int maxVersion = dao.getMaxVersion();
            Assert.assertEquals(7, maxVersion);
        }
    }
}
