package ru.yandex.market.tsum.pipelines.common.jobs.sandbox.liquibase;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit-tests for {@link PostgresHelper}.
 *
 * @author fbokovikov
 */
public class PostgresHelperTest {

    @Test
    public void testingJdbcUrl() {
        String testingUrl = PostgresHelper.testingPgaasJdbc("market_abo_test");
        Assert.assertEquals(
            "jdbc:postgresql://pgaas-test.mail.yandex.net:12000/market_abo_test?sslmode=require&prepareThreshold=0",
            testingUrl
        );
    }

    @Test
    public void productionJdbcUrl() {
        String productionUrl = PostgresHelper.productionPgaasJdbc("abodb");
        Assert.assertEquals(
            "jdbc:postgresql://pgaas.mail.yandex.net:12000/abodb?sslmode=require&prepareThreshold=0",
            productionUrl
        );
    }
}
