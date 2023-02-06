package ru.yandex.market.common.test.db.ddl.datatype;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.test.db.DatasourceInjectOperationListener;

public class CustomPostgresqlDataTypeFactory extends PostgresqlDataTypeFactory
        implements DatasourceInjectOperationListener.DatasourceInjectable {
    private static final Logger log = LoggerFactory.getLogger(CustomPostgresqlDataTypeFactory.class);

    private Set<String> enumNames;

    @Override
    public void init(DataSource dataSource) {
        if (enumNames == null) {
            enumNames = Collections.unmodifiableSet(fetchEnumTypes(dataSource));
            log.debug("Initialized with {} enums", enumNames.size());
        }
    }

    protected Set<String> fetchEnumTypes(DataSource dataSource) {
        Set<String> enumTypes = new HashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement s = connection.createStatement()) {
                try (ResultSet rs = s.executeQuery("" +
                        "select n.nspname = ANY(current_schemas(true))," +
                        "       n.nspname as enum_schema,\n" +
                        "       t.typname as enum_name\n" +
                        "from pg_type t\n" +
                        "         join pg_enum e on t.oid = e.enumtypid\n" +
                        "         join pg_catalog.pg_namespace n ON n.oid = t.typnamespace\n" +
                        "group by 1, 2, 3")) {
                    rs.setFetchSize(1000);
                    while (rs.next()) {
                        boolean onPath = rs.getBoolean(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);

                        enumTypes.add("\"" + schema + "\".\"" + name + "\"");
                        enumTypes.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get enum_types", e);
        }
        return enumTypes;
    }

    public Set<String> getEnumNames() {
        return enumNames;
    }

    public void setEnumNames(Collection<String> enumNames) {
        this.enumNames = Collections.unmodifiableSet(new HashSet<>(enumNames));
    }

    @Override
    public boolean isEnumType(String sqlTypeName) {
        return enumNames.contains(sqlTypeName);
    }

    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (sqlType == Types.SQLXML) {
            return new XmlDataType();
        }
        if (JsonbDataType.TYPE.equals(sqlTypeName)) {
            return new JsonbDataType();
        }
        if (sqlType == Types.ARRAY) {
            if (sqlTypeName.equals("_varchar")) {
                return new ArrayDataType(sqlTypeName, sqlType, false);
            }
            if (sqlTypeName.equals("_text")) {
                return new ArrayDataType(sqlTypeName, sqlType, false);
            }
            if (sqlTypeName.contains("int")) {
                return new ArrayDataType(sqlTypeName, sqlType, true);
            }
            throw new UnsupportedOperationException("Unsupported sql type: " + sqlTypeName);
        }
        if (isEnumType(sqlTypeName)) {
            sqlType = Types.OTHER;
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
