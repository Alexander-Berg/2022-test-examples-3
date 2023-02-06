package ru.yandex.market.crm.platform.services.facts;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.InboxSourceConfig;
import ru.yandex.market.crm.platform.config.StorageConfig;
import ru.yandex.market.crm.platform.config.TestConfigs;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.models.MinimalExample;
import ru.yandex.market.crm.platform.models.NoTimeExample;
import ru.yandex.market.crm.platform.services.facts.impl.FactSchemaService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.common.FactsColumns.FACT;
import static ru.yandex.market.crm.platform.common.FactsColumns.FACT_ID;
import static ru.yandex.market.crm.platform.common.FactsColumns.ID;
import static ru.yandex.market.crm.platform.common.FactsColumns.ID_TYPE;
import static ru.yandex.market.crm.platform.common.FactsColumns.TIMESTAMP;

public class FactSchemeServiceTest {

    private FactConfig noTimeFact;
    private FactConfig factWithOptions;
    private FactSchemaService service;

    private static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }

    @Before
    public void before() {
        noTimeFact = new FactConfig(
                "noTimeFact",
                "noTimeFact",
                Collections.singletonList(InboxSourceConfig.INSTANCE),
                TestConfigs.model(NoTimeExample.class),
                null,
                null,
                List.of(),
                Map.of("hahn", store())
        );

        factWithOptions = new FactConfig(
                "testFactWithOptions",
                "testFactWithOptions",
                Collections.singletonList(InboxSourceConfig.INSTANCE),
                TestConfigs.model(MinimalExample.class),
                null,
                null,
                List.of(),
                Map.of("hahn", store())
        );

        service = new FactSchemaService();
    }

    @Test
    public void noTimestampAndFactIdTest() {
        MapF<String, YTreeNode> tableAttrs = service.getFactTableAttrs(noTimeFact);
        assertBasicAttrs(tableAttrs);
        assertNull(getColumn(tableAttrs, TIMESTAMP));
        assertNull(getColumn(tableAttrs, FACT_ID));
    }

    @Test
    public void copyTest() {
        MapF<String, YTreeNode> attrs = service.getFactTableAttrs(noTimeFact);
        assertTrue(attrs.getOptional("schema").isPresent());
        attrs.removeO("schema");
        assertFalse(attrs.getOptional("schema").isPresent());
        attrs = service.getFactTableAttrs(noTimeFact);
        assertTrue(attrs.getOptional("schema").isPresent());
    }

    @Test
    public void withTimestampAndFactIdTest() {
        MapF<String, YTreeNode> tableAttrs = service.getFactTableAttrs(factWithOptions);
        assertBasicAttrs(tableAttrs);
        assertNotNull(getColumn(tableAttrs, TIMESTAMP));
        assertNotNull(getColumn(tableAttrs, FACT_ID));
    }

    private void assertBasicAttrs(MapF<String, YTreeNode> tableAttrs) {
        assertTrue(tableAttrs.getOrThrow("dynamic").boolValue());
        assertNotNull(getColumn(tableAttrs, ID));
        assertNotNull(getColumn(tableAttrs, ID_TYPE));
        assertNotNull(getColumn(tableAttrs, FACT));
    }

    private YTreeNode getColumn(Map<String, YTreeNode> tableAttrs, String name) {
        List<YTreeNode> columns = tableAttrs.get("schema").asList();
        return columns.stream().filter(f -> f.mapNode().getString("name").equals(name)).findFirst().orElse(null);
    }
}
