package ru.yandex.market.jmf.module.def.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@SpringJUnitConfig(ConstShardingTest.Configuration.class)
@TestPropertySource(properties = {
        "postgres.cluster.shards.constSharding.datasource.schema=" +
                "${postgres.cluster.shards.main.datasource.schema}_const_sharding",
        "postgres.cluster.shards.constSharding.datasource.minIdle=1",
        "postgres.cluster.shards.constSharding.datasource.maxTotal=4",
        "postgres.cluster.shards.constSharding.datasource.username=${postgres.cluster.shards.main.datasource.username}"
})
public class ConstShardingTest {
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private TxService txService;

    private List<String> gids;

    @BeforeEach
    public void setUp() {
        this.gids = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        txService.runInTx(() -> gids.forEach(bcpService::delete));
    }

    @Test
    public void testThatDataIsSavedOnAnotherShard() {
        var gid = txService.doInTx(() -> bcpService.create(Fqn.of("sharded"), Map.of(
                "title", "Title",
                "attr1", "value"
        )).getGid());
        gids.add(gid);
        var shardedEntity = txService.doInTx(() -> entityStorageService.<Entity>get(gid));

        Assertions.assertNotNull(shardedEntity);
        Assertions.assertEquals("value", shardedEntity.getAttribute("attr1"));
    }

    @Test
    public void testThatShardedDataIsReallyExistsOnlyOnAnotherShard() {
        Assertions.assertThrows(PersistenceException.class, () -> txService.runInTx(() ->
                dbService.<Long>querySql("SELECT id FROM tbl_sharded", Map.of(), "main"))
        );
    }

    @Test
    public void testThatDataIsSavedOnBothShardsAtTheSameTransaction() {
        var gids = txService.doInTx(() -> {
            var sharded = bcpService.create(Fqn.of("sharded"), Map.of(
                    "title", "Title",
                    "attr1", "value sharded"
            ));
            var main = bcpService.create(Fqn.of("sharded"), Map.of(
                    "title", "Title",
                    "attr1", "value main"
            ));
            return Stream.of(sharded, main).map(HasGid::getGid).collect(Collectors.toList());
        });
        this.gids.addAll(gids);
        var shardedEntity = txService.doInTx(() -> entityStorageService.<Entity>get(gids.get(0)));
        var mainEntity = txService.doInTx(() -> entityStorageService.<Entity>get(gids.get(1)));

        Assertions.assertNotNull(shardedEntity);
        Assertions.assertEquals("value sharded", shardedEntity.getAttribute("attr1"));

        Assertions.assertNotNull(mainEntity);
        Assertions.assertEquals("value main", mainEntity.getAttribute("attr1"));
    }

    @Test
    public void testThatShardedObjectsCouldBeReferencedViaObjectAttributeType() {
        var gids = txService.doInTx(() -> {
            var sharded = bcpService.create(Fqn.of("sharded"), Map.of(
                    "title", "Title",
                    "attr1", "value sharded"
            ));
            var main = bcpService.create(Fqn.of("main"), Map.of(
                    "title", "Title",
                    "attr1", "value main",
                    "shardedObj", sharded
            ));
            return Stream.of(sharded, main).map(HasGid::getGid).collect(Collectors.toList());
        });

        var shardedEntity = txService.doInTx(() -> {
            var main = entityStorageService.get(gids.get(1));

            return main.<Entity>getAttribute("shardedObj");
        });

        Assertions.assertNotNull(shardedEntity);
        Assertions.assertEquals("value sharded", shardedEntity.getAttribute("attr1"));
    }

    @org.springframework.context.annotation.Configuration
    @Import({
            ModuleDefaultTestConfiguration.class
    })
    public static class Configuration extends AbstractModuleConfiguration {

        protected Configuration() {
            super("module/default/test/sharding");
        }
    }
}
