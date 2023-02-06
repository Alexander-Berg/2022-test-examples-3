package ru.yandex.search.mop.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.searchmap.SearchMap;
import ru.yandex.parser.searchmap.SearchMapHost;
import ru.yandex.parser.searchmap.SearchMapRow;
import ru.yandex.parser.searchmap.ZooKeeperAddress;
import ru.yandex.search.mop.common.searchmap.BackendHost;
import ru.yandex.search.mop.common.searchmap.HostGroup;
import ru.yandex.search.mop.common.searchmap.Metashard;
import ru.yandex.search.mop.common.searchmap.Queue;
import ru.yandex.search.mop.common.searchmap.QueueHost;
import ru.yandex.search.mop.common.services.Service;
import ru.yandex.search.prefix.PrefixType;

public class SearchMapWrapperTest {
    @Test
    public void testWrapping() throws Exception {
//        change_log iNum:0,host:host1,shards:0-2,zk:zk1|zk2|zk3
//        change_log iNum:0,host:host2,shards:0-2,zk:zk1|zk2|zk3
//        change_log iNum:0,host:host3,shards:0-2,zk:zk1|zk2|zk3
//        change_log iNum:1,host:host4,shards:3-5,zk:zk4|zk5|zk6
//        change_log iNum:1,host:host5,shards:3-5,zk:zk4|zk5|zk6
//        change_log iNum:1,host:host6,shards:3-5,zk:zk4|zk5|zk6
//
//        iex iNum:0,host:host1,shards:0-1,zk:zk4|zk5|zk6
//        iex iNum:0,host:host2,shards:0-1,zk:zk4|zk5|zk6
//        iex iNum:0,host:host3,shards:0-1,zk:zk4|zk5|zk6
//        iex iNum:1,host:host4,shards:2-3,zk:zk1|zk2|zk3
//        iex iNum:1,host:host5,shards:2-3,zk:zk1|zk2|zk3
//        iex iNum:1,host:host6,shards:2-3,zk:zk1|zk2|zk3
//
//        corp_change_log iNum:0,host:host1,shards:0-2,zk:zk1|zk2|zk3
//        corp_change_log iNum:0,host:host2,shards:0-2,zk:zk1|zk2|zk3
//        corp_change_log iNum:0,host:host3,shards:0-2,zk:zk1|zk2|zk3
//        corp_change_log iNum:1,host:host4,shards:3-5,zk:zk4|zk5|zk6
//        corp_change_log iNum:1,host:host5,shards:3-5,zk:zk4|zk5|zk6
//        corp_change_log iNum:1,host:host6,shards:3-5,zk:zk4|zk5|zk6
//
        long version = 0L;
        List<Queue> queues = new ArrayList<>();
        List<QueueHost> queueHosts1 = new ArrayList<>();
        queueHosts1.add(new QueueHost("zk1", 80, 81));
        queueHosts1.add(new QueueHost("zk2", 80, 81));
        queueHosts1.add(new QueueHost("zk3", 80, 81));
        queues.add(new Queue(0, queueHosts1));
        List<QueueHost> queueHosts2 = new ArrayList<>();
        queueHosts2.add(new QueueHost("zk4", 80, 81));
        queueHosts2.add(new QueueHost("zk5", 80, 81));
        queueHosts2.add(new QueueHost("zk6", 80, 81));
        queues.add(new Queue(1, queueHosts2));

        List<HostGroup> hostGroups = new ArrayList<>();
        Set<BackendHost> hosts1 = new HashSet<>();
        hosts1.add(new BackendHost("host1", 80, 81, 84, 86, 88));
        hosts1.add(new BackendHost("host2", 80, 81, 84, 86, 88));
        hosts1.add(new BackendHost("host3", 80, 81, 84, 86, 88));
        hostGroups.add(new HostGroup(0, hosts1));
        Set<BackendHost> hosts2 = new HashSet<>();
        hosts2.add(new BackendHost("host4", 80, 81, 84, 86, 88));
        hosts2.add(new BackendHost("host5", 80, 81, 84, 86, 88));
        hosts2.add(new BackendHost("host6", 80, 81, 84, 86, 88));
        hostGroups.add(new HostGroup(1, hosts2));

        List<Metashard> metashards = new ArrayList<>();
        metashards.add(new Metashard(0, Service.CHANGE_LOG, 0, 0, 0, 2));
        metashards.add(new Metashard(1, Service.CHANGE_LOG, 1, 1, 3, 5));
        metashards.add(new Metashard(0, Service.IEX, 1, 0, 0, 1));
        metashards.add(new Metashard(1, Service.IEX, 0, 1, 2, 3));
        metashards.add(new Metashard(0, Service.CORP_CHANGE_LOG, 0, 0, 0, 2));
        metashards.add(new Metashard(1, Service.CORP_CHANGE_LOG, 1, 1, 3, 5));

        ru.yandex.search.mop.common.searchmap.SearchMap newSearchMap =
            new ru.yandex.search.mop.common.searchmap.SearchMap(
                version,
                queues,
                hostGroups,
                metashards);
        SearchMap searchMap = new SearchMapWrapper(
            newSearchMap,
            "search_port_ng",
            "queue_id_port");

        List<ZooKeeperAddress> zkList1 = new ArrayList<>();
        zkList1.add(new ZooKeeperAddress(new HttpHost("zk1", 81), 80));
        zkList1.add(new ZooKeeperAddress(new HttpHost("zk2", 81), 80));
        zkList1.add(new ZooKeeperAddress(new HttpHost("zk3", 81), 80));

        List<ZooKeeperAddress> zkList2 = new ArrayList<>();
        zkList2.add(new ZooKeeperAddress(new HttpHost("zk4", 81), 80));
        zkList2.add(new ZooKeeperAddress(new HttpHost("zk5", 81), 80));
        zkList2.add(new ZooKeeperAddress(new HttpHost("zk6", 81), 80));

        SearchMapHost host1 = new SearchMapHost("host1", 81, 88);
        SearchMapHost host2 = new SearchMapHost("host2", 81, 88);
        SearchMapHost host3 = new SearchMapHost("host3", 81, 88);

        SearchMapHost host4 = new SearchMapHost("host4", 81, 88);
        SearchMapHost host5 = new SearchMapHost("host5", 81, 88);
        SearchMapHost host6 = new SearchMapHost("host6", 81, 88);

        SearchMapRow row = searchMap.row("change_log");
        Assert.assertEquals(PrefixType.LONG, row.prefixType());
        Assert.assertEquals(zkList1, row.get(0).zk());
        Assert.assertEquals(0, row.get(0).iNum());
        Assert.assertTrue(row.get(0).contains(host1));
        Assert.assertTrue(row.get(0).contains(host2));
        Assert.assertTrue(row.get(0).contains(host3));
        Assert.assertFalse(row.get(0).contains(host4));
        Assert.assertFalse(row.get(0).contains(host5));
        Assert.assertFalse(row.get(0).contains(host6));
        Assert.assertSame(row.get(0), row.get(1));
        Assert.assertSame(row.get(0), row.get(2));
        Assert.assertEquals(zkList2, row.get(3).zk());
        Assert.assertEquals(row.get(3).iNum(), 1);
        Assert.assertTrue(row.get(3).contains(host4));
        Assert.assertTrue(row.get(3).contains(host5));
        Assert.assertTrue(row.get(3).contains(host6));
        Assert.assertFalse(row.get(3).contains(host1));
        Assert.assertFalse(row.get(3).contains(host2));
        Assert.assertFalse(row.get(3).contains(host3));
        Assert.assertSame(row.get(3), row.get(4));
        Assert.assertSame(row.get(3), row.get(5));


        SearchMapRow row2 = searchMap.row("iex");
        Assert.assertEquals(PrefixType.LONG, row2.prefixType());
        Assert.assertEquals(zkList2, row2.get(0).zk());
        Assert.assertEquals(0, row2.get(0).iNum());
        Assert.assertTrue(row2.get(0).contains(host1));
        Assert.assertTrue(row2.get(0).contains(host2));
        Assert.assertTrue(row2.get(0).contains(host3));
        Assert.assertFalse(row2.get(0).contains(host4));
        Assert.assertFalse(row2.get(0).contains(host5));
        Assert.assertFalse(row2.get(0).contains(host6));
        Assert.assertSame(row2.get(0), row2.get(1));
        Assert.assertEquals(zkList1, row2.get(2).zk());
        Assert.assertEquals(1, row2.get(2).iNum());
        Assert.assertTrue(row2.get(2).contains(host4));
        Assert.assertTrue(row2.get(2).contains(host5));
        Assert.assertTrue(row2.get(2).contains(host6));
        Assert.assertFalse(row2.get(2).contains(host1));
        Assert.assertFalse(row2.get(2).contains(host2));
        Assert.assertFalse(row2.get(2).contains(host3));
        Assert.assertSame(row2.get(2), row2.get(3));

        Assert.assertSame(
            searchMap.row("change_log"),
            searchMap.row("corp_change_log"));
        Assert.assertSame(
            searchMap.row("change_log").get(0).zk(),
            searchMap.row("iex").get(3).zk());
        Assert.assertSame(
            searchMap.row("change_log").get(3).get(0),
            searchMap.row("iex").get(2).get(0));
    }
}
