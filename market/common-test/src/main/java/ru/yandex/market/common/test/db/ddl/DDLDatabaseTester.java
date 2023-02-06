package ru.yandex.market.common.test.db.ddl;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.dbunit.AbstractDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IOperationListener;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;

/**
 * @author antipov93.
 */
public final class DDLDatabaseTester extends AbstractDatabaseTester {
    @Nullable
    private final DbUnitDataBaseConfig config;
    private final DDLHelper ddl;
    private final DataSource dataSource;

    DDLDatabaseTester(
            DataSource dataSource,
            String schema,
            @Nullable DbUnitDataBaseConfig config,
            DDLHelper ddl
    ) {
        super(schema);
        this.dataSource = dataSource;
        this.config = config;
        this.ddl = ddl;
    }

    public DDLHelper getDDL() {
        return ddl;
    }

    @Nullable
    public IOperationListener getOperationListener() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("operationListener");
            field.setAccessible(true);
            return (IOperationListener) field.get(this);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IDatabaseConnection getConnection() throws SQLException, DatabaseUnitException {
        String schema = StringUtils.trimToNull(getSchema());
        return createConnection(schema);
    }

    private IDatabaseConnection createConnection(String schema) throws SQLException, DatabaseUnitException {
        IDatabaseConnection connection = ddl.getConnection(dataSource, schema);
        if (config != null) {
            Properties properties = new Properties();
            for (DbUnitDataBaseConfig.Entry entry : config.value()) {
                properties.setProperty(entry.name(), entry.value());
            }
            connection.getConfig().setPropertiesByString(properties);
        }
        if (schema == null) {
            connection.getConfig().setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
        }
        return connection;
    }
}
