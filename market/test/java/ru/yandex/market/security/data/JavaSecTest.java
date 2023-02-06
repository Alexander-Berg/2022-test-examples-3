package ru.yandex.market.security.data;

import java.sql.SQLException;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitTester;
import ru.yandex.market.common.test.jdbc.H2SqlTransformer;
import ru.yandex.market.common.test.jdbc.InstrumentedDataSourceFactory;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, value = "true")
})
public class JavaSecTest {

    protected static EmbeddedDatabase dataSource;
    protected static DbUnitTester tester;

    protected static void init(Class<?> cls) {
        final EmbeddedDatabaseFactoryBean factoryBean = new EmbeddedDatabaseFactoryBean();
        factoryBean.setDatabaseType(EmbeddedDatabaseType.H2);
        factoryBean.setDataSourceFactory(new InstrumentedDataSourceFactory(new H2SqlTransformer()));
        factoryBean.setDatabaseName("testDataBase" + System.currentTimeMillis());
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setScripts(
                script("schema.sql"),
                script("S_ID.sql"),
                script("OP_DESC.sql"),
                script("AUTHORITY.sql"),
                script("OP_PERM.sql"),
                script("AUTH_LINK.sql"),
                script("AUTHORITY_CHECKER.sql"),
                script("DOMAIN.sql"),
                script("DOMAIN_ADMINS.sql"),
                script("PERM_AUTH.sql"),
                script("STATIC_AUTH.sql"),
                script("STATIC_DOMAIN_AUTH.sql")
        );
        factoryBean.setDatabasePopulator(populator);
        dataSource = factoryBean.getDatabase();

        tester = new DbUnitTester(cls, dataSource, "JAVA_SEC");
    }

    @BeforeEach
    void setUp() throws SQLException {
        tester.cleanUpDb();
    }

    @AfterAll
    static void destroy() {
        dataSource.shutdown();
        dataSource = null;
        tester = null;
    }

    public static Resource script(String path) {
        return new ClassPathResource("sql/unittest/JAVA_SEC/" + path);
    }


}
