package ru.yandex.market.common.test.db;

import javax.sql.DataSource;

import org.dbunit.DefaultOperationListener;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;

public class DatasourceInjectOperationListener extends DefaultOperationListener {

    private final DataSource dataSource;

    public DatasourceInjectOperationListener(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void connectionRetrieved(IDatabaseConnection connection) {
        super.connectionRetrieved(connection);

        Object datatypeFactory = connection.getConfig().getProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY);
        if (!(datatypeFactory instanceof DatasourceInjectable)) {
            return;
        }
        DatasourceInjectable datasourceInjectable = (DatasourceInjectable) datatypeFactory;
        datasourceInjectable.init(dataSource);
    }

    public interface DatasourceInjectable {
        void init(DataSource dataSource);
    }

}
