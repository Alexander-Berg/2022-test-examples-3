package ru.yandex.market.security.core;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.security.data.JavaSecTest;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.OperationAuthorities;
import ru.yandex.market.security.model.OperationPermission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, value = "true")
})
class SimpleAuthoritiesLoaderTest extends JavaSecTest {

    SimpleAuthoritiesLoader loader;

    @BeforeAll
    static void init() {
        JavaSecTest.init(SimpleAuthoritiesLoaderTest.class);
    }

    @BeforeEach
    void setUp() {
        KampferFactory factory = new SimpleKampferFactory(dataSource);
        loader = new SimpleAuthoritiesLoader();
        loader.setKampferFactory(factory);
    }

    @Test
    void load() {
        tester.insertDataSet("SimpleAuthoritiesLoaderTest.before.csv");
        OperationAuthorities loaded = loader.load("test2", "op");
        assertEquals("op", loaded.getOperationName());
        assertEquals(1, loaded.getPermissions().size());

        OperationPermission opPerm = loaded.getPermissions().get(0);
        assertEquals("op", opPerm.getOperationName());
        assertEquals("test2", opPerm.getDomain());
        assertEquals(1, opPerm.getAuthorities().size());

        Authority auth = opPerm.getAuthorities().get(0);
        assertEquals("checker", auth.getChecker());
        assertEquals("permauthparam", auth.getParams());
        assertEquals("auth4", auth.getName());
        assertEquals(0, auth.getSufficientLinks().size());
        assertEquals(1, auth.getRequiresLinks().size());

        Authority link = auth.getRequiresLinks().iterator().next();
        assertEquals("checker2", link.getChecker());
        assertEquals("authlinkparam", link.getParams());
        assertEquals("auth5", link.getName());
        assertEquals(0, link.getSufficientLinks().size());
        assertEquals(0, link.getRequiresLinks().size());
    }

    @Test
    void loadMissing() {
        assertNull(loader.load("test2", "missing"));
    }
}
