package ru.yandex.market.stats.test.util;

import org.apache.log4j.Logger;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author kormushin
 *         Created on 27.03.15.
 */
@Service
@Profile("integration-tests")
public class MetadataDbHelper {
    private static final Logger LOG = Logger.getLogger(MetadataDbHelper.class);

    private DataSource metadataDataSource;

    private final FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder()
            .setCaseSensitiveTableNames(true)
            .setColumnSensing(true);

    private IDatabaseConnection dbConn;

    @Autowired
    public void setMetadataDataSource(DataSource metadataDataSource) {
        this.metadataDataSource = metadataDataSource;
    }

    @PostConstruct
    public void afterPropertiesSet() throws SQLException {
        dbConn = new DatabaseDataSourceConnection(metadataDataSource);
    }

    public IDataSet load(String resource) throws DataSetException {
        return dataSetBuilder.build(
                getClass().getResourceAsStream(resource)
        );
    }

    public void execute(DatabaseOperation operation, IDataSet dataSet) throws DatabaseUnitException, SQLException {
        operation.execute(dbConn, dataSet);
    }

    public void truncateTable(String tableName) throws DatabaseUnitException, SQLException {
        DatabaseOperation.TRUNCATE_TABLE.execute(dbConn,
                new DefaultDataSet(new DefaultTable(tableName)));
    }

    public ITable getActualTable(String tableName) throws SQLException, DataSetException {
        return dbConn.createDataSet().getTable(tableName);
    }

    public static void assertEquals(ITable updates, SqlRowSet sqlRowSet, final String[] columns) throws DataSetException {
        sqlRowSet.first();
        for (int i = 0; i < updates.getRowCount(); i++) {
            for (String column : columns) {
                assertThat("Column " + column + " validation failed", sqlRowSet.getString(column), is(updates.getValue(i, column)));
            }
            sqlRowSet.next();
        }
    }

}
