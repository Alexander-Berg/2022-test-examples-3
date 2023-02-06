package ru.yandex.market.security.core;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.security.AuthorityChecker;
import ru.yandex.market.security.CheckerResolver;
import ru.yandex.market.security.SecManager;
import ru.yandex.market.security.checker.FalseChecker;
import ru.yandex.market.security.checker.TrueChecker;
import ru.yandex.market.security.checker.UidChecker;
import ru.yandex.market.security.data.JavaSecTest;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.UidableFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, value = "true")
})
class MainSecManagerTest extends JavaSecTest {
    SecManager secManager;

    @BeforeAll
    static void init() {
        JavaSecTest.init(MainSecManagerTest.class);
    }

    @BeforeEach
    void setUp() {
        KampferFactory factory = new CachedKampferFactory(dataSource, 10);
        SimpleAuthoritiesLoader loader = new SimpleAuthoritiesLoader();
        loader.setKampferFactory(factory);
        MainSecManager mainSecManager = new MainSecManager();
        mainSecManager.setAuthoritiesLoaders(Collections.singletonList(loader));
        mainSecManager.setDomain("test");
        mainSecManager.setCheckerResolver(createCheckerResolver());
        secManager = mainSecManager;
    }

    private CheckerResolver createCheckerResolver() {
        MapCheckerResolver mapCheckerResolver = new MapCheckerResolver();
        mapCheckerResolver.setResolvers(ImmutableMap.of(
                "true", new TrueChecker(),
                "false", FalseChecker.INSTANCE,
                "uid", new UidChecker(),
                "even", new EvenParamChecker()
        ));
        return mapCheckerResolver;
    }

    @Test
    void test() {
        tester.insertDataSet("MainSecManagerTest.before.csv");
        assertFalse(secManager.hasAuthority("auth4", "", new Object()));
        assertTrue(secManager.hasAuthority("auth5", "", new Object()));
        assertFalse(secManager.hasAuthority("missing", "", new Object()));
        assertFalse(secManager.hasAuthority("auth6", "123", UidableFactory.buildSimple(1)));
        assertTrue(secManager.hasAuthority("auth6", "3", UidableFactory.buildSimple(3)));
    }

    @Test
    void testDifferentAuthorityParams() {
        tester.insertDataSet("MainSecManagerTest.before.csv");
        assertFalse(secManager.hasAuthority("even_auth", "5", new Object()));
        assertTrue(secManager.hasAuthority("even_auth", "6", new Object()));
        assertFalse(secManager.hasAuthority("even_composite_auth", "", new Object()));
        assertFalse(secManager.canDo("even_composite_op", new Object()));
    }

    private static class EvenParamChecker implements AuthorityChecker {
        @Override
        public boolean check(Object data, Authority authority) {
            return Integer.valueOf(authority.getParams()) % 2 == 0;
        }
    }

}
