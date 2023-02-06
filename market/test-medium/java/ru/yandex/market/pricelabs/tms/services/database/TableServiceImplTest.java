package ru.yandex.market.pricelabs.tms.services.database;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableServiceImplTest extends AbstractTmsSpringConfiguration {

    @Autowired
    @Qualifier("externalTableVersionService")
    private TableRevisionService tableRevisionService;

    private String cluster;

    @BeforeEach
    void init() {
        ((TableServiceImpl) tableRevisionService).clearTables();
        this.cluster = testControls.getCurrentCluster();
    }

    @Test
    void testEmptyRevisions() {
        assertEquals(0, tableRevisionService.getTableRevisions(cluster, "prefix1").size());
        assertEquals(0, tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1", "table2")).size());
        assertTrue(tableRevisionService.getLastTableName(cluster, "prefix1").isEmpty());
    }

    @Test
    void testSetRevision() {
        assertTrue(tableRevisionService.getTableRevision(cluster, "prefix1", "table1").isEmpty());

        var expect = new MapBuilder().with("table1", 1).map;
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table1", 1);
        assertEquals(expect, tableRevisionService.getTableRevisions(cluster, "prefix1"));
        assertEquals(1, tableRevisionService.getTableRevision(cluster, "prefix1", "table1").orElseThrow());

        assertEquals(0, tableRevisionService.getTableRevisions(cluster, "prefix2").size());
        assertTrue(tableRevisionService.getTableRevision(cluster, "prefix1", "table2").isEmpty());
        assertTrue(tableRevisionService.getTableRevision(cluster, "prefix2", "table1").isEmpty());

        assertEquals(expect, tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1")));
        assertEquals(expect, tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1", "table2")));
        assertEquals(0, tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table2")).size());
        assertEquals("table1", tableRevisionService.getLastTableName(cluster, "prefix1").orElseThrow());
        assertTrue(tableRevisionService.getLastTableName(cluster, "prefix2").isEmpty());
    }

    @Test
    void testSetRevisionDifferentClusters() {
        assertTrue(tableRevisionService.getTableRevision("cluster1", "prefix1", "table1").isEmpty());
        assertTrue(tableRevisionService.getTableRevision("cluster2", "prefix1", "table1").isEmpty());

        var expect = new MapBuilder().with("table1", 1).map;
        tableRevisionService.saveTableRevision("cluster1", "prefix1", "table1", 1);
        assertEquals(expect, tableRevisionService.getTableRevisions("cluster1", "prefix1"));
        assertEquals(Object2LongMaps.emptyMap(), tableRevisionService.getTableRevisions("cluster2", "prefix1"));
        assertEquals(1, tableRevisionService.getTableRevision("cluster1", "prefix1", "table1").orElseThrow());
        assertTrue(tableRevisionService.getTableRevision("cluster2", "prefix1", "table1").isEmpty());
        assertEquals("table1", tableRevisionService.getLastTableName("cluster1", "prefix1").orElseThrow());
        assertTrue(tableRevisionService.getLastTableName("cluster2", "prefix1").isEmpty());

        var expect2 = new MapBuilder().with("table1", 2).map;
        tableRevisionService.saveTableRevision("cluster2", "prefix1", "table1", 2);
        assertEquals(expect, tableRevisionService.getTableRevisions("cluster1", "prefix1"));
        assertEquals(expect2, tableRevisionService.getTableRevisions("cluster2", "prefix1"));

        assertEquals(1, tableRevisionService.getTableRevision("cluster1", "prefix1", "table1").orElseThrow());
        assertEquals(2, tableRevisionService.getTableRevision("cluster2", "prefix1", "table1").orElseThrow());

        assertEquals("table1", tableRevisionService.getLastTableName("cluster1", "prefix1").orElseThrow());
        assertEquals("table1", tableRevisionService.getLastTableName("cluster2", "prefix1").orElseThrow());

        tableRevisionService.saveTableRevision("cluster2", "prefix1", "table1", 3);
        assertEquals(1, tableRevisionService.getTableRevision("cluster1", "prefix1", "table1").orElseThrow());
        assertEquals(3, tableRevisionService.getTableRevision("cluster2", "prefix1", "table1").orElseThrow());

    }

    @Test
    void testSetRevisions() {
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table1", 1);
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table2", 2);
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table3", 3);
        tableRevisionService.saveTableRevision(cluster, "prefix2", "table1", 4);

        assertEquals(new MapBuilder()
                        .with("table1", 1)
                        .with("table2", 2)
                        .with("table3", 3)
                        .map,
                tableRevisionService.getTableRevisions(cluster, "prefix1"));

        assertEquals(new MapBuilder().with("table1", 4).map,
                tableRevisionService.getTableRevisions(cluster, "prefix2"));

        assertEquals(3, tableRevisionService.getTableRevision(cluster, "prefix1", "table3").orElseThrow());
        assertEquals(4, tableRevisionService.getTableRevision(cluster, "prefix2", "table1").orElseThrow());


        assertEquals(new MapBuilder()
                        .with("table1", 1)
                        .map,
                tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1")));
        assertEquals(new MapBuilder()
                        .with("table1", 1)
                        .with("table2", 2)
                        .map,
                tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1", "table2")));
        assertEquals(new MapBuilder()
                        .with("table1", 1)
                        .map,
                tableRevisionService.getTableRevisions(cluster, "prefix1", List.of("table1", "table4")));

        assertEquals(new MapBuilder().with("table1", 4).map,
                tableRevisionService.getTableRevisions(cluster, "prefix2", List.of("table1")));

        assertEquals(0, tableRevisionService.getTableRevisions(cluster, "prefix2", List.of("table2")).size());
        assertEquals("table3", tableRevisionService.getLastTableName(cluster, "prefix1").orElseThrow());
        assertEquals("table1", tableRevisionService.getLastTableName(cluster, "prefix2").orElseThrow());
        assertTrue(tableRevisionService.getLastTableName(cluster, "prefix3").isEmpty());
    }

    @Test
    void testOverwriteRevisions() {
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table1", 1);
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table2", 2);
        tableRevisionService.saveTableRevision(cluster, "prefix1", "table3", 3);

        tableRevisionService.saveTableRevision(cluster, "prefix1", "table2", 4);

        assertEquals(new MapBuilder()
                        .with("table1", 1)
                        .with("table2", 4)
                        .with("table3", 3)
                        .map,
                tableRevisionService.getTableRevisions(cluster, "prefix1"));

        assertEquals(4, tableRevisionService.getTableRevision(cluster, "prefix1", "table2").orElseThrow());
    }


    static class MapBuilder {
        final Object2LongMap<String> map = new Object2LongOpenHashMap<>();

        MapBuilder with(String name, long revision) {
            map.put(name, revision);
            return this;
        }

    }

}
