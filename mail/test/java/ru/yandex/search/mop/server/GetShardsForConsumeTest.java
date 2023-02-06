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

public class GetShardsForConsumeTest extends TestBase {
    @Test
    public void testGetShardsForConsume() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.apply("searchmap.sql");
            cluster.start();

            SearchMap searchMap = cluster.mopServer().searchMapContainer().searchMap();

            String uri = cluster.mopServer().host().toString() +
                "/get-shards-for-consume?";


            // test bad context


            // no label

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }


            // scope failed

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=host123&port=80&scope=random")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }


            // no dc

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "label=0&host=host1&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }


            // empty host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }


            // for response checking

            String queue =
                "zk:man1-6099-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|man2-1055-556-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|man2-1056-8cc-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|myt1-0091-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|myt1-0104-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|myt1-0174-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|sas2-5135-179-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|sas2-5142-711-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                    + "|sas2-5152-aec-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663";
            String ports = ",json_indexer_port:84,search_port_ng:81,search_port:80,"
                + "dump_port:86,queue_id_port:84";
            String portsIex = ",json_indexer_port:82,search_port_ng:81,search_port:80,"
                + "dump_port:86,queue_id_port:82";

            String additional = "change_log"
                + " iNum:0,tag:host123,host:host123,shards:0-5,"
                + queue + ports;
            additional += "\nchange_log_offline"
                + " iNum:0,tag:host123,host:host123,shards:6-10,"
                + queue + ports;
            additional += "\nsubscriptions_prod_1"
                + " iNum:0,tag:host123,host:host123,shards:0-5,"
                + queue + ports;
            additional += "\nsubscriptions_prod_2"
                + " iNum:0,tag:host123,host:host123,shards:0-5,"
                + queue + ports;
            additional += "\niex iNum:0,tag:host123,host:host123,shards:0-5,"
                + queue + portsIex;


            // ok

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=host123&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(9, searchMap.version());

                cluster.checkResponse(response, "searchmap_mail_bp.txt", additional);
            }


            // test host exist in other label

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=1&host=host123&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
                Assert.assertEquals(9, searchMap.version());
            }


            // before adding host

            BackendHost host =
                new BackendHost("host10", 80, 81, 84, 86, 84, true, "man");
            BackendHost hostIex =
                new BackendHost("host10", 80, 81, 82, 86, 82, true, "man");
            Assert.assertFalse(
                "HostGroup 0 contains " + host,
                searchMap.hosts(0).contains(host));
            Assert.assertEquals(4, searchMap.hosts(0).size());
            Assert.assertFalse(
                "HostGroup 1 contains " + host,
                searchMap.hosts(1).contains(host));
            Assert.assertEquals(2, searchMap.hosts(1).size());
            Assert.assertFalse(
                "HostGroup 2 contains " + hostIex,
                searchMap.hosts(2).contains(hostIex));
            Assert.assertEquals(4, searchMap.hosts(2).size());
            Assert.assertFalse(
                "HostGroup 3 contains " + hostIex,
                searchMap.hosts(3).contains(hostIex));
            Assert.assertEquals(2, searchMap.hosts(3).size());
            Assert.assertFalse(
                "HostGroup 4 contains " + host,
                searchMap.hosts(4).contains(host));
            Assert.assertEquals(2, searchMap.hosts(4).size());

            // add host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=host10&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                // check response

                additional += "\nchange_log"
                    + " iNum:0,tag:host10,host:host10,shards:0-5,"
                    + queue + ports;
                additional += "\nchange_log_offline"
                    + " iNum:0,tag:host10,host:host10,shards:6-10,"
                    + queue + ports;
                additional += "\nsubscriptions_prod_1"
                    + " iNum:0,tag:host10,host:host10,shards:0-5,"
                    + queue + ports;
                additional += "\nsubscriptions_prod_2"
                    + " iNum:0,tag:host10,host:host10,shards:0-5,"
                    + queue + ports;
                additional += "\niex iNum:0,tag:host10,host:host10,shards:0-5,"
                    + queue + portsIex;

                cluster.checkResponse(response, "searchmap_mail_bp.txt", additional);

                // check cache

                Assert.assertEquals(11, searchMap.version());
                Assert.assertTrue(
                    "HostGroup 0 doesn't contain " + host,
                    searchMap.hosts(0).contains(host));
                Assert.assertEquals(5, searchMap.hosts(0).size());
                Assert.assertFalse(
                    "HostGroup 1 contains " + host,
                    searchMap.hosts(1).contains(host));
                Assert.assertEquals(2, searchMap.hosts(1).size());
                Assert.assertTrue(
                    "HostGroup 2 doesn't contain " + hostIex,
                    searchMap.hosts(2).contains(hostIex));
                Assert.assertEquals(5, searchMap.hosts(2).size());
                Assert.assertFalse(
                    "HostGroup 3 contains " + hostIex,
                    searchMap.hosts(3).contains(hostIex));
                Assert.assertEquals(2, searchMap.hosts(3).size());
                Assert.assertFalse(
                    "HostGroup 4 contains " + host,
                    searchMap.hosts(4).contains(host));
                Assert.assertEquals(2, searchMap.hosts(4).size());
            }

            // check DB

            try (Connection connection =
                cluster.mopServer().connectionPool().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM host_group WHERE hostname = 'host10'"))
            {
                ResultSet resultSet = statement.executeQuery();
                List<HostGroup> hostGroups = HostGroupDAO.parse(resultSet);
                // host groups from 0 to 2
                Assert.assertEquals(3, hostGroups.size());
                HostGroup hostGroup = hostGroups.get(0);
                Assert.assertEquals(0, hostGroup.id());
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    "HostGroup 0 doesn't contain " + host,
                    hostGroup.hosts().contains(host));

                Assert.assertNull(hostGroups.get(1));

                hostGroup = hostGroups.get(2);
                Assert.assertEquals(2, hostGroup.id());
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    "HostGroup 2 doesn't contain " + hostIex,
                    hostGroup.hosts().contains(hostIex));
            }


            // add the same host

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=host10&port=80&scope=mail_bp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(11, searchMap.version());
                Assert.assertEquals(5, searchMap.hosts(0).size());
                Assert.assertEquals(2, searchMap.hosts(1).size());
                Assert.assertEquals(5, searchMap.hosts(2).size());
                Assert.assertEquals(2, searchMap.hosts(3).size());
                Assert.assertEquals(2, searchMap.hosts(4).size());
            }


            // add host to corp, incorrect label

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=1&host=host10&port=80&scope=mail_corp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, response);
                Assert.assertEquals(11, searchMap.version());
            }


            // add host to corp

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "dc=man&label=0&host=host10&port=80&scope=mail_corp")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                // check response

               String additionalCorp = "corp_change_log"
                    + " iNum:0,tag:host10,host:host10,shards:0-10,"
                    + queue + ports;
                additionalCorp += "\ncorp_change_log_offline"
                    + " iNum:0,tag:host10,host:host10,shards:0-10,"
                    + queue + ports;

                cluster.checkResponse(response, "searchmap_mail_corp.txt", additionalCorp);

                // check cache

                Assert.assertEquals(12, searchMap.version());
                Assert.assertEquals(5, searchMap.hosts(0).size());
                Assert.assertEquals(2, searchMap.hosts(1).size());
                Assert.assertEquals(5, searchMap.hosts(2).size());
                Assert.assertEquals(2, searchMap.hosts(3).size());
                Assert.assertTrue(
                    "HostGroup 4 doesn't contain " + host,
                    searchMap.hosts(4).contains(host));
                Assert.assertEquals(3, searchMap.hosts(4).size());
            }

            // check DB

            try (Connection connection =
                cluster.mopServer().connectionPool().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM host_group WHERE hostname = 'host10'"))
            {
                ResultSet resultSet = statement.executeQuery();
                List<HostGroup> hostGroups = HostGroupDAO.parse(resultSet);
                // host groups from 0 to 4
                Assert.assertEquals(5, hostGroups.size());

                HostGroup hostGroup = hostGroups.get(0);
                Assert.assertEquals(0, hostGroup.id());
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    "HostGroup 0 doesn't contain " + host,
                    hostGroup.hosts().contains(host));

                Assert.assertNull(hostGroups.get(1));

                hostGroup = hostGroups.get(2);
                Assert.assertEquals(2, hostGroup.id());
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    "HostGroup 2 doesn't contain " + hostIex,
                    hostGroup.hosts().contains(hostIex));

                Assert.assertNull(hostGroups.get(3));

                hostGroup = hostGroups.get(4);
                Assert.assertEquals(4, hostGroup.id());
                Assert.assertEquals(1, hostGroup.hosts().size());
                Assert.assertTrue(
                    "HostGroup 4 doesn't contain " + host,
                    hostGroup.hosts().contains(host ));
            }
        }
    }
}
