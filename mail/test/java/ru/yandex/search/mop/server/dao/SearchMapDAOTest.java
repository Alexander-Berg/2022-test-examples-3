package ru.yandex.search.mop.server.dao;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.search.mop.common.searchmap.BackendHost;
import ru.yandex.search.mop.common.searchmap.HostGroup;
import ru.yandex.search.mop.common.searchmap.Metashard;
import ru.yandex.search.mop.common.searchmap.Queue;
import ru.yandex.search.mop.common.searchmap.SearchMap;
import ru.yandex.search.mop.common.services.Service;
import ru.yandex.search.mop.server.MopServerCluster;
import ru.yandex.test.util.TestBase;

public class SearchMapDAOTest extends TestBase {
    private static final String HOST_GROUP_ERROR =
        "HostGroup doesn't contain backendHost: id = ";
    private static final String METASHARD_ERROR = "Searchmap doesn't contain ";

    @Test
    public void testGetSearchMap() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster()) {
            cluster.apply("searchmap.sql");
            cluster.start();

            SearchMapDAO dao = cluster.mopServer().searchMapDAO();
            SearchMap searchMap = dao.getSearchMap();


            // check version
            Assert.assertEquals(7, searchMap.version());


            // check queues
            List<Queue> queues = searchMap.queues();
            Assert.assertEquals(1, queues.size());
            Queue queue = queues.get(0);
            Assert.assertEquals(0, queue.id());
            Assert.assertEquals(9, queue.hosts().size());
            Assert.assertEquals(
                "man1-6099-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|man2-1055-556-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|man2-1056-8cc-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|myt1-0091-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|myt1-0104-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|myt1-0174-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|sas2-5135-179-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|sas2-5142-711-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663"
                + "|sas2-5152-aec-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net:18662/18663",
                queue.queueStr());


            // check hostGroups
            List<HostGroup> hostGroups = searchMap.hostGroups();
            Assert.assertEquals(5, hostGroups.size());

            HostGroup group = hostGroups.get(0);
            Assert.assertEquals(0, group.id());
            Set<BackendHost> hosts = group.hosts();
            Assert.assertEquals(3, hosts.size());
            BackendHost host = new BackendHost("host1", 26763, 26764, 26767, 26769, 26767, true, "man");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host2", 18065, 18066, 18069, 18071, 18069, true, "sas");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host3", 26763, 26764, 26767, 26769, 26767, true, "vla");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));

            group = hostGroups.get(1);
            Assert.assertEquals(1, group.id());
            hosts = group.hosts();
            Assert.assertEquals(2, hosts.size());
            host = new BackendHost("host4", 26763, 26764, 26767, 26769, 26767, true, "man");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host5", 26763, 26764, 26767, 26769, 26767, true, "sas");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));

            group = hostGroups.get(2);
            Assert.assertEquals(2, group.id());
            hosts = group.hosts();
            Assert.assertEquals(3, hosts.size());
            host = new BackendHost("host1", 26763, 26764, 26765, 26769, 26765, true, "man");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host2", 18065, 18066, 18067, 18071, 18067, true, "sas");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host3", 26763, 26764, 26765, 26769, 26765, true, "vla");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));

            group = hostGroups.get(3);
            Assert.assertEquals(3, group.id());
            hosts = group.hosts();
            Assert.assertEquals(2, hosts.size());
            host = new BackendHost("host4", 26763, 26764, 26765, 26769, 26765, true, "man");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host5", 26763, 26764, 26765, 26769, 26765, true, "sas");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));

            group = hostGroups.get(4);
            Assert.assertEquals(4, group.id());
            hosts = group.hosts();
            Assert.assertEquals(2, hosts.size());
            host = new BackendHost("host6", 26763, 26764, 26767, 26769, 26767, true, "vla");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));
            host = new BackendHost("host7", 26763, 26764, 26767, 26769, 26767, true, "sas");
            Assert.assertTrue(HOST_GROUP_ERROR + group.id() + ' ' + host, hosts.contains(host));


            // check metashards
            List<Metashard> metashards = searchMap.metashards();
            Assert.assertEquals(12, metashards.size());

            Metashard metashard = new Metashard(0, Service.CORP_CHANGE_LOG, 0, 4, 0, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.CORP_CHANGE_LOG_OFFLINE, 0, 4, 0, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.CHANGE_LOG, 0, 0, 0, 5);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(1, Service.CHANGE_LOG, 0, 1, 6, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(1, Service.CHANGE_LOG_OFFLINE, 0, 1, 0, 5);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.CHANGE_LOG_OFFLINE, 0, 0, 6, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.SUBSCRIPTIONS_PROD_1, 0, 0, 0, 5);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(1, Service.SUBSCRIPTIONS_PROD_1, 0, 1, 6, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.SUBSCRIPTIONS_PROD_2, 0, 0, 0, 5);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(1, Service.SUBSCRIPTIONS_PROD_2, 0, 1, 6, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(0, Service.IEX, 0, 2, 0, 5);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));

            metashard = new Metashard(1, Service.IEX, 0, 3, 6, 10);
            Assert.assertTrue(METASHARD_ERROR + metashard, metashards.contains(metashard));
        }
    }
}
