package ru.yandex.search.mop.common.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.io.StringBuilderWriter;
import ru.yandex.search.mop.common.searchmap.BackendHost;
import ru.yandex.search.mop.common.searchmap.Metashard;
import ru.yandex.search.mop.common.searchmap.Queue;
import ru.yandex.search.mop.common.searchmap.QueueHost;
import ru.yandex.search.mop.common.searchmap.SearchMap;
import ru.yandex.search.mop.common.searchmap.ShardsRange;
import ru.yandex.search.mop.common.services.Service;
import ru.yandex.search.mop.common.writers.SearchMapStringWriter;

public class SearchMapStringParserTest {
    @Test
    public void testParsingOneLine() throws Exception {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream("searchmap_one_line.txt"),
                StandardCharsets.UTF_8))) {
            SearchMap searchMap = SearchMapStringParser.INSTANCE.parse(reader);

            Assert.assertEquals(1, searchMap.hostGroups().size());
            Assert.assertEquals(0, searchMap.hostGroups().get(0).id());
            Set<BackendHost> hosts = searchMap.hosts(0);
            Assert.assertEquals(1, hosts.size());
            BackendHost host = new BackendHost(
                "vla2-5421-15d-all-mail-lucene--5d8-18065.gencfg-c.yandex.net",
                18065, 18066, 18069, 18071, 18069);
            Assert.assertEquals(host, hosts.iterator().next());

            Assert.assertEquals(1, searchMap.queues().size());
            Queue queue = searchMap.queues().get(0);
            Assert.assertEquals(3, queue.hosts().size());
            List<QueueHost> expectedQueueHosts = new ArrayList<>();
            expectedQueueHosts.add(new QueueHost(
                "man1-6099-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net",
                18662, 18663));
            expectedQueueHosts.add(new QueueHost(
                "man2-1055-556-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net",
                18662, 18663));
            expectedQueueHosts.add(new QueueHost(
                "man2-1056-8cc-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net",
                18662, 18663));
            Queue expectedQueue = new Queue(0, expectedQueueHosts);
            Assert.assertEquals(expectedQueue.queueStr(), queue.queueStr());

            Assert.assertEquals(1, searchMap.metashards().size());
            Metashard metashard = searchMap.metashards().get(0);
            Assert.assertEquals(Service.CHANGE_LOG, metashard.service());
            Assert.assertEquals(0, metashard.label());
            Assert.assertEquals(0, metashard.queueId());
            Assert.assertEquals(0, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(32768, 32798), metashard.shardsRange());
        }
    }

    @Test
    public void testLineNotMatchPattern() {
        try {
            SearchMapStringParser.INSTANCE.parseLine(
                "change_log iNum:0,tag:host3,host:host3,shards:0-10,"
                    + "zk:queue1:80/81|queue2:80/81|queue3:80/81,"
                    + "search_port_ng:81,search_port:80,dump_port:86",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        } catch (IOException e) {
            Assert.assertEquals("Searchmap line doesn't match pattern", e.getMessage());
            return;
        }
        Assert.fail("Searchmap line doesn't match pattern exception expected");
    }

    @Test
    public void testQueueNotMatchPattern() {
        try {
            SearchMapStringParser.INSTANCE.parseQueue("zk:", new ArrayList<>());
        } catch (IOException e) {
            Assert.assertEquals("Can't parse queue: zk:", e.getMessage());
            return;
        }
        Assert.fail("Can't parse queue exception expected");
    }

    @Test
    public void testParsingSearchmap() throws Exception {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream("searchmap.txt"),
                StandardCharsets.UTF_8))) {
            SearchMap searchMap = SearchMapStringParser.INSTANCE.parse(reader);

            Assert.assertEquals(5, searchMap.hostGroups().size());
            Assert.assertEquals(0, searchMap.hostGroups().get(0).id());
            Set<BackendHost> hosts = searchMap.hostGroups().get(0).hosts();
            Assert.assertEquals(4, hosts.size());
            Assert.assertTrue(hosts.contains(new BackendHost("host1", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host2", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host3", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host4", 80, 81, 84, 86, 84)));
            Assert.assertEquals(1, searchMap.hostGroups().get(1).id());
            hosts = searchMap.hostGroups().get(1).hosts();
            Assert.assertEquals(4, hosts.size());
            Assert.assertTrue(hosts.contains(new BackendHost("host5", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host6", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host7", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host8", 80, 81, 84, 86, 84)));
            Assert.assertEquals(2, searchMap.hostGroups().get(2).id());
            hosts = searchMap.hostGroups().get(2).hosts();
            Assert.assertEquals(2, hosts.size());
            Assert.assertTrue(hosts.contains(new BackendHost("host9", 80, 81, 84, 86, 84)));
            Assert.assertTrue(hosts.contains(new BackendHost("host10", 80, 81, 84, 86, 84)));
            Assert.assertEquals(3, searchMap.hostGroups().get(3).id());
            hosts = searchMap.hostGroups().get(3).hosts();
            Assert.assertEquals(4, hosts.size());
            Assert.assertTrue(hosts.contains(new BackendHost("host5", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host6", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host7", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host8", 80, 81, 82, 86, 82)));
            Assert.assertEquals(4, searchMap.hostGroups().get(4).id());
            hosts = searchMap.hostGroups().get(4).hosts();
            Assert.assertEquals(4, hosts.size());
            Assert.assertTrue(hosts.contains(new BackendHost("host11", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host12", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host13", 80, 81, 82, 86, 82)));
            Assert.assertTrue(hosts.contains(new BackendHost("host14", 80, 81, 82, 86, 82)));

            Assert.assertEquals(3, searchMap.queues().size());
            Queue queue = searchMap.queues().get(0);
            Assert.assertEquals(3, queue.hosts().size());
            Assert.assertEquals("queue1:80/81|queue2:80/81|queue3:80/81", queue.queueStr());
            queue = searchMap.queues().get(1);
            Assert.assertEquals(3, queue.hosts().size());
            Assert.assertEquals("queue4:80/81|queue5:80/81|queue6:80/81", queue.queueStr());
            queue = searchMap.queues().get(2);
            Assert.assertEquals(3, queue.hosts().size());
            Assert.assertEquals("queue7:80/81|queue8:80/81|queue9:80/81", queue.queueStr());

            Assert.assertEquals(8, searchMap.metashards().size());
            Metashard metashard = searchMap.metashards().get(0);
            Assert.assertEquals(Service.CHANGE_LOG, metashard.service());
            Assert.assertEquals(0, metashard.label());
            Assert.assertEquals(0, metashard.queueId());
            Assert.assertEquals(0, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(0, 10), metashard.shardsRange());
            metashard = searchMap.metashards().get(1);
            Assert.assertEquals(Service.CHANGE_LOG, metashard.service());
            Assert.assertEquals(1, metashard.label());
            Assert.assertEquals(1, metashard.queueId());
            Assert.assertEquals(1, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(11, 20), metashard.shardsRange());
            metashard = searchMap.metashards().get(2);
            Assert.assertEquals(Service.CHANGE_LOG, metashard.service());
            Assert.assertEquals(2, metashard.label());
            Assert.assertEquals(2, metashard.queueId());
            Assert.assertEquals(2, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(21, 30), metashard.shardsRange());
            metashard = searchMap.metashards().get(3);
            Assert.assertEquals(Service.CHANGE_LOG_OFFLINE, metashard.service());
            Assert.assertEquals(0, metashard.label());
            Assert.assertEquals(2, metashard.queueId());
            Assert.assertEquals(0, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(0, 30), metashard.shardsRange());
            metashard = searchMap.metashards().get(4);
            Assert.assertEquals(Service.CHANGE_LOG_OFFLINE, metashard.service());
            Assert.assertEquals(1, metashard.label());
            Assert.assertEquals(1, metashard.queueId());
            Assert.assertEquals(2, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(31, 60), metashard.shardsRange());
            metashard = searchMap.metashards().get(5);
            Assert.assertEquals(Service.CHANGE_LOG_OFFLINE, metashard.service());
            Assert.assertEquals(2, metashard.label());
            Assert.assertEquals(0, metashard.queueId());
            Assert.assertEquals(1, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(61, 100), metashard.shardsRange());
            metashard = searchMap.metashards().get(6);
            Assert.assertEquals(Service.IEX, metashard.service());
            Assert.assertEquals(0, metashard.label());
            Assert.assertEquals(2, metashard.queueId());
            Assert.assertEquals(3, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(0, 1000), metashard.shardsRange());
            metashard = searchMap.metashards().get(7);
            Assert.assertEquals(Service.IEX, metashard.service());
            Assert.assertEquals(1, metashard.label());
            Assert.assertEquals(0, metashard.queueId());
            Assert.assertEquals(4, metashard.hostGroupId());
            Assert.assertEquals(new ShardsRange(1001, 2000), metashard.shardsRange());
        }
    }

    @Test
    public void testStringParsingAndStringWriter() throws Exception {
        String[] sbwLines;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream("searchmap.txt"),
                StandardCharsets.UTF_8))) {
            SearchMap searchMap = SearchMapStringParser.INSTANCE.parse(reader);
            StringBuilderWriter sbw = SearchMapStringWriter.INSTANCE.write(searchMap);
            sbwLines = sbw.toString().split("\n");
            Arrays.sort(sbwLines);
        }
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream("searchmap.txt"),
                StandardCharsets.UTF_8))) {
            String[] expectedLines = reader.lines().toArray(String[]::new);
            Arrays.sort(expectedLines);
            Assert.assertArrayEquals(expectedLines, sbwLines);
        }
    }
}
