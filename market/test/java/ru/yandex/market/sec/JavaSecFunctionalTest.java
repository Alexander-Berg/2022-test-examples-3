package ru.yandex.market.sec;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTester;
import ru.yandex.market.common.test.db.InitByDbUnitListener;
import ru.yandex.market.partner.test.context.EmptyDbFunctionalTest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.CheckerResolver;
import ru.yandex.market.security.core.CachedKampferFactory;
import ru.yandex.market.security.core.KampferFactory;
import ru.yandex.market.security.core.MainSecManager;
import ru.yandex.market.security.core.SimpleAuthoritiesLoader;
import ru.yandex.market.security.migration.JavaSecMigrationService;
import ru.yandex.market.security.migration.JavaSecMigrator;
import ru.yandex.market.security.migration.model.JavaSecMigrationRequest;

/**
 * Базовый функциональный тест для джавасека.
 * Между тестами не выполняет очистку БД. Вместо этого перед классом очищает один раз,
 * наливает вручную и переиспользует состояние БД.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(truncateAllTables = false)
public class JavaSecFunctionalTest extends EmptyDbFunctionalTest {

    protected static final String DOMAIN = "MBI-PARTNER";
    protected static MainSecManager secManager;
    protected static SimpleAuthoritiesLoader authoritiesLoader;
    protected static KampferFactory kampferFactory;

    @SuppressWarnings("unused")
    @InitByDbUnitListener
    protected DbUnitTester dbUnitTester;

    @Autowired
    @Qualifier("dataSource")
    protected DataSource dataSource;

    @Autowired
    protected CheckerResolver checkerResolver;

    @Autowired
    protected JavaSecMigrationService javaSecMigrationService;

    @AfterAll
    static void tearDown() {
        secManager = null;
        authoritiesLoader = null;
    }

    /**
     * Некоторый хак, чтобы можно было инициализировать данные для java-sec правил один раз, не очищая
     * данные каждый раз. Нельзя делать через {@link org.junit.jupiter.api.BeforeAll BeforeAll},
     * потому что нужен рабочий autowire.
     * <p>
     * TODO Вероятно, можно сделать через
     * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
     */
    @BeforeEach
    protected void setUp() throws Exception {
        if (secManager == null) {
            // Инициализация здесь, чтобы не переливался dataset перед каждым тестом
            dbUnitTester.cleanUpDb(
                    Stream.of(FunctionalTest.nonTruncatedTables()).collect(Collectors.toSet()),
                    Collections.emptySet());
            // Полный путь, чтобы загрузка датасета не зависела от исполняемого класса
            dbUnitTester.insertDataSet("classpath:ru/yandex/market/sec/JavaSecRulesTest.data.csv");

            final JavaSecMigrationRequest request = JavaSecMigrator.createRequest(DOMAIN);
            javaSecMigrationService.tryMigrate(request);

            authoritiesLoader = new SimpleAuthoritiesLoader();

            authoritiesLoader.setKampferFactory(kampferFactory = new CachedKampferFactory(dataSource, 60));
            secManager = new MainSecManager();
            secManager.setCheckerResolver(checkerResolver);
            secManager.setDomain(DOMAIN);
            secManager.setAuthoritiesLoaders(Collections.singletonList(authoritiesLoader));
        }
    }
}

