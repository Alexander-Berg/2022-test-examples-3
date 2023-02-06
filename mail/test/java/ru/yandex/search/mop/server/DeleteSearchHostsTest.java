package ru.yandex.search.mop.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.search.mop.common.searchmap.BackendHost;
import ru.yandex.search.mop.common.searchmap.HostGroup;
import ru.yandex.search.mop.common.searchmap.SearchMap;
import ru.yandex.search.mop.server.dao.HostGroupDAO;
import ru.yandex.test.util.TestBase;

public class DeleteSearchHostsTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.apply("searchmap.sql");
            cluster.start();

            SearchMap searchMap = cluster.mopServer().searchMapContainer().searchMap();

            String uri = cluster.mopServer().host().toString() +
                "/delete-search-hosts?";

            // test bad context
            // no host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }

            // empty host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "host=")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }

            // test delete host

            BackendHost host0 =
                new BackendHost("host1", 26763, 26764, 26767, 26769, 26767, true, "man");
            Assert.assertTrue(
                "HostGroup 0 doesn't contain " + host0,
                searchMap.hosts(0).contains(host0));
            BackendHost host2 =
                new BackendHost("host1", 26763, 26764, 26765, 26769, 26765, true, "man");
            Assert.assertTrue(
                "HostGroup 2 doesn't contain " + host2,
                searchMap.hosts(2).contains(host2));

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "host=host1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(8, searchMap.version());
                Assert.assertFalse(
                    "HostGroup 0 contains deleted " + host0,
                    searchMap.hosts(0).contains(host0));
                Assert.assertFalse(
                    "HostGroup 2 contains deleted " + host2,
                    searchMap.hosts(2).contains(host2));
            }

            // check DB

            try (Connection connection =
                     cluster.mopServer().connectionPool().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM host_group WHERE hostname = 'host1'")) {
                ResultSet resultSet = statement.executeQuery();
                List<HostGroup> hostGroups = HostGroupDAO.parse(resultSet);
                Assert.assertEquals(0, hostGroups.size());
            }

            // delete the same host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "host=host1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
                Assert.assertEquals(8, searchMap.version());

            }
        }
    }
}
