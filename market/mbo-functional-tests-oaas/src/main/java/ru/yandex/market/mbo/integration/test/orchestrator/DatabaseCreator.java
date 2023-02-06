package ru.yandex.market.mbo.integration.test.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;

/**
 * @author s-ermakov
 */
public class DatabaseCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseCreator.class);

    private JdbcTemplate masterJDBCTemplate;

    public DatabaseCreator(JdbcTemplate masterJDBCTemplate) {
        this.masterJDBCTemplate = masterJDBCTemplate;
    }

    /**
     * Creates empty schemas.
     * If schema already exists, then recreates it.
     * If smth goes wrong, drops all schemas.
     */
    public void createSchemas(Collection<String> schemas) {
        try {
            schemas.forEach(this::createSchema);
        } catch (RuntimeException e) {
            try {
                dropSchemas(schemas);
            } catch (RuntimeException e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
    }

    /**
     * Reuse already created schemas.
     * If schema not exists, then creates it.
     * If smth goes wrong, doesn't drop anything.
     */
    public void reuseSchemas(Collection<String> schemas) {
        schemas.forEach(this::createSchemaIfNotExists);
    }

    public void dropSchemas(Collection<String> schemas) {
        RuntimeException exception = null;
        for (String schema : schemas) {
            try {
                dropSchema(schema);
            } catch (RuntimeException e) {
                if (exception == null) {
                    exception = e;
                    continue;
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    public void createSchema(String schema) {
        try {
            masterJDBCTemplate.update("call CREATE_TEST_USER(?)", schema);
        } catch (Throwable e) {
            if (isSchemaAlreadyExists(e, schema)) {
                recreateSchema(schema);
                return;
            }
            if (isIllegalName(e)) {
                throw getIllegalSchemaNameException(schema);
            }
            throw new RuntimeException("Failed to create schema: " + schema, e);
        }
        log.debug("Successfully created schema: " + schema);
    }

    public void createSchemaIfNotExists(String schema) {
        try {
            masterJDBCTemplate.update("call CREATE_TEST_USER(?)", schema);
            log.debug("Successfully created schema: " + schema);
        } catch (Throwable e) {
            if (isSchemaAlreadyExists(e, schema)) {
                log.debug("Schema already exists. Schema: " + schema);
                return;
            }
            if (isIllegalName(e)) {
                throw getIllegalSchemaNameException(schema);
            }
            throw new RuntimeException("Failed to create schema: " + schema, e);
        }
    }

    public void dropSchema(String schema) {
        try {
            masterJDBCTemplate.update("call DROP_TEST_USER(?)", schema);
        } catch (Throwable e) {
            if (isUserNotExist(e, schema)) {
                return;
            }
            if (isIllegalName(e)) {
                throw getIllegalSchemaNameException(schema);
            }
            throw new RuntimeException("Failed to drop schema: " + schema, e);
        }
        log.debug("Successfully drop schema: " + schema);
    }

    public void recreateSchema(String schema) {
        dropSchema(schema);
        try {
            masterJDBCTemplate.update("call CREATE_TEST_USER(?)", schema);
        } catch (Throwable e) {
            if (isSchemaAlreadyExists(e, schema)) {
                throw new RuntimeException(String.format("Failed to recreate schema '%1$s'. " +
                    "Schema with name '%1$s' already exists.", schema), e);
            }
            if (isIllegalName(e)) {
                throw getIllegalSchemaNameException(schema);
            }
            throw new RuntimeException("Failed to recreate schema: " + schema, e);
        }
        log.debug("Successfully recreated schema: " + schema);
    }

    private boolean isIllegalName(Throwable e) {
        return e.getMessage().contains("Invalid schema name");
    }

    private boolean isUserNotExist(Throwable e, String schema) {
        return e.getMessage().contains("user '" + schema + "' does not exist") ||
            e.getMessage().contains("пользователь '" + schema + "' не существует");
    }

    private boolean isSchemaAlreadyExists(Throwable e, String schema) {
        return e.getMessage().contains("user name '" + schema + "' conflicts with another user or role name") ||
            e.getMessage().contains(
                "имя пользователя '" + schema + "' противоречит имени другого пользователя или роли");
    }

    private RuntimeException getIllegalSchemaNameException(String schema) {
        return new RuntimeException("Illegal test-schema name: " + schema + ". Check if:\n" +
            "- you don't want to add new schema for tests;\n" +
            "- name matches the format: '<schema>_<N>', where schema in CAPS ans N - is two digits number." +
            " Ex. SITE_CATALOG_02;\n" +
            "- schema name exists in table: select * from system.allowed_schemas");
    }
}
