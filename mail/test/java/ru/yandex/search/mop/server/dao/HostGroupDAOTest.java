package ru.yandex.search.mop.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.search.mop.common.searchmap.BackendHost;
import ru.yandex.search.mop.common.searchmap.HostGroup;
import ru.yandex.search.mop.server.MopServerCluster;
import ru.yandex.test.util.TestBase;

public class HostGroupDAOTest extends TestBase {
    @Test
    public void testInsertHostGroup() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster()) {
            cluster.apply("searchmap.sql");
            cluster.start();

            HostGroupDAO dao = cluster.mopServer().hostGroupDAO();

            // existed host
            BackendHost existing = new BackendHost("host1", 26763, 26764, 26767, 26769, 26767, true, "man");
            int version = dao.insertHostGroup(0, existing);
            Assert.assertEquals(-1, version);

            // insert
            BackendHost host = new BackendHost("host-yp.man", 80, 81, 84, 86, 84, true, "man");
            version = cluster.mopServer().hostGroupDAO().insertHostGroup(4, host);
            Assert.assertEquals(8, version);

            try (Connection connection =
                cluster.mopServer().connectionPool().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM host_group WHERE id = 4 AND hostname = 'host-yp.man'"))
            {
                ResultSet resultSet = statement.executeQuery();
                List<HostGroup> hostGroups = HostGroupDAO.parse(resultSet);
                // host groups from 0 to 4
                Assert.assertEquals(5, hostGroups.size());
                Assert.assertNull(hostGroups.get(0));
                Assert.assertNull(hostGroups.get(1));
                Assert.assertNull(hostGroups.get(2));
                Assert.assertNull(hostGroups.get(3));
                HostGroup hostGroup = hostGroups.get(4);
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    host + " not in host group 4",
                    hostGroup.hosts().contains(host));
            }

            // check version increment
            host = new BackendHost("host-yp.sas", 80, 81, 84, 86, 84, true, "sas");
            version = cluster.mopServer().hostGroupDAO().insertHostGroup(4, host);
            Assert.assertEquals(9, version);
        }
    }
}
