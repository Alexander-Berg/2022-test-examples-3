package ru.yandex.market.mbo.billing;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * @author amaslak
 */
public class BillingDatabaseMock {

    private BillingDatabaseMock() {
    }

    public static DataSource getBillingDatasource() {
        String dbName = BillingDatabaseMock.class.getSimpleName() + UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:mbo-core/ru/yandex/market/mbo/billing/billing-test-db.sql'" +
                ";MODE=Oracle"
        );
        return dataSource;
    }
}
