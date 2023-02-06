package ru.yandex.market.mbo.db.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

/**.
 * Перекомпилирует все объекты, тем самым проверяет какие из них валидны <p>
 * Борется с ошибочно удаленными/переименованными объектами БД, которые на самом деле еще используются в DDL
 * других объектов <p>
 * Чтобы этот валидатор не проверял объект, нужно добавить его в игнор таблицу через ченджсет ликвибейза
 * в mbo-db/src/sql/util/migration_validation_ignore.sql" <p><p>
 *
 * {@link #testGetInvalid()} используется для сбора невалидных объектов, но для непосредственно валидации не нужен <p>
 * Если в валидатор нужно добавить новые схемы, стоит убрать @Ignore, запустить тесты и посмотреть какие объекты
 * невалдидны, затем решить проблему с этими объектами чтобы они стали валидными, либо добавить их в игнор-таблицу
 * <p>
 * @author belkinmike
 */
public class ObjectConsistencyValidatorTest extends BaseIntegrationTest {

    private static final String SQL_PATH_OBJECTS_TO_VALIDATE = "sql/validator_get_objects_to_validate.sql";
    private static final String SQL_PATH_ALL_OBJECTS = "sql/validator_get_all_objects.sql";
    private static final String SQL_PATH_CURRENT_USER = "sql/validator_get_current_user.sql";
    private static final Logger log = LogManager.getLogger(ObjectConsistencyValidatorTest.class);

    private static final String MIGRATION_VALIDATION_IGNORE = "MIGRATION_VALIDATION_IGNORE";
    private static final String SITE_CATALOG = "SITE_CATALOG";

    private static String sqlToGetObjectsForValidator;
    private static String sqlToGetAllObjects;
    private static String sqlToGetCurrentUser;

    @Resource(name = "siteCatalogJdbcTemplate")
    protected JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "contentJdbcTemplate")
    protected JdbcTemplate contentJdbcTemplate; // market_content

    @Resource(name = "contentDraftJdbcTemplate")
    protected JdbcTemplate contentDraftJdbcTemplate; // market_content_draft

    @Resource(name = "marketDepotJdbcTemplate")
    protected JdbcTemplate marketDepotJdbcTemplate; // watches

    @BeforeClass
    public static void loadResources() throws IOException {
        sqlToGetObjectsForValidator = loadResource(SQL_PATH_OBJECTS_TO_VALIDATE);
        sqlToGetAllObjects = loadResource(SQL_PATH_ALL_OBJECTS);
        sqlToGetCurrentUser = loadResource(SQL_PATH_CURRENT_USER);
    }

    private static String loadResource(String path) throws IOException {
        String sql;
        try (InputStream stream = ObjectConsistencyValidatorTest.class.getClassLoader().getResourceAsStream(path)) {
            assert stream != null;
            sql = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        }
        return sql;
    }

    @Test
    public void testSiteCatalogValidate() {
        validate(siteCatalogJdbcTemplate, true);
    }

    @Test
    public void testMarketContentValidate() {
        validate(contentJdbcTemplate, false);
    }

    @Test
    public void testMarketContentDraftValidate() {
        validate(contentDraftJdbcTemplate, false);
    }

    @Test
    public void testWatchesValidate() {
        validate(marketDepotJdbcTemplate, false);
    }

    @Test
    @Ignore
    public void testGetInvalid() {
        StringBuilder sb = new StringBuilder();
        sb.append(validateAndGetInvalid(siteCatalogJdbcTemplate))
            .append(validateAndGetInvalid(contentJdbcTemplate))
            .append(validateAndGetInvalid(contentDraftJdbcTemplate))
            .append(validateAndGetInvalid(marketDepotJdbcTemplate));

    }

    private void validate(JdbcTemplate jdbcTemplate, boolean isSiteCatalog) {
        String sql = sqlToGetObjectsForValidator;

        String currentSchemaWithNumber = jdbcTemplate.queryForObject(sqlToGetCurrentUser, String.class);
        assert currentSchemaWithNumber != null;
        var params = new Object[] {currentSchemaWithNumber };

        int postfixStart = currentSchemaWithNumber.lastIndexOf('_');
        String currentSchemaNumber = currentSchemaWithNumber.substring(postfixStart + 1);
        String correctMigrationValidationIgnore = isSiteCatalog ? String.format(" %s ", MIGRATION_VALIDATION_IGNORE) :
                            String.format(" %s_%s.%s ", SITE_CATALOG, currentSchemaNumber, MIGRATION_VALIDATION_IGNORE);

        String[] sqlSplit = sql.split("!");
        sql = String.format("%s%s%s", sqlSplit[0], correctMigrationValidationIgnore, sqlSplit[1]);

        List<DatabaseObject> objects = jdbcTemplate.query(sql, params, new DatabaseObjectRowMapper());
        for (DatabaseObject obj : objects) {
            try {
                jdbcTemplate.execute(getValidationSql(obj));
            } catch (Exception e) {
                throw new ObjectConsistencyValidatorException("THIS IS DB OBJECT CONSISTENCY VALIDATOR MESSAGE.\n" +
                    "Compilation check failed during object compile verification step for:\n" +
                    obj.getType() + " " + obj.getFullName() + "\n" +
                    "Known problems:\n" +
                    " - failed to acquire lock for object recompilation, mb it is in use by some process.\n" +
                    "You can check this with query : select * from DBA_DDL_LOCKS WHERE name = <quoted_object_name>\n" +
                    " - inconsistent object, which depends on broken or missing objects\n", e);
            }
        }
    }

    private String validateAndGetInvalid(JdbcTemplate jdbcTemplate) {
        StringBuilder badObjectsInsert = new StringBuilder();

        String sql = sqlToGetAllObjects;
        String currentSchemaWithNumber = jdbcTemplate.queryForObject(sqlToGetCurrentUser, String.class);
        assert currentSchemaWithNumber != null;

        var params = new Object[] {currentSchemaWithNumber };

        List<DatabaseObject> objects = jdbcTemplate.query(sql, params, new DatabaseObjectRowMapper());
        for (DatabaseObject obj : objects) {
            try {
                jdbcTemplate.execute(getValidationSql(obj));
            } catch (Exception e) {
                badObjectsInsert.append(obj.getInsertLine()).append("\n");
            }
        }
        if (!badObjectsInsert.toString().isEmpty()) {
            log.debug("Found invalid objects, you can insert them into ignore table with:\n" +
                badObjectsInsert);
        }
        return String.valueOf(badObjectsInsert.append("\n"));
    }

    private String getValidationSql(DatabaseObject obj) {
        String sqlAlter;
        switch (obj.getType().toLowerCase()) {
            case "package":
                sqlAlter = "ALTER PACKAGE " + obj.getFullName() + " COMPILE";
                break;
            case "package body":
                sqlAlter = "ALTER PACKAGE " + obj.getFullName() + " COMPILE BODY";
                break;
            default:
                sqlAlter = "ALTER " + obj.getType() + " " + obj.getFullName() + " COMPILE";
                break;
        }
        return sqlAlter;
    }

    static class DatabaseObject {

        private String objectName;
        private String owner;
        private String objectType;

        public void setName(String objectName) {
            this.objectName = objectName;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public void setType(String objectType) {
            this.objectType = objectType;
        }

        public String getType() {
            return objectType;
        }

        public String getName() {
            return objectName;
        }

        public String getOwner() {
            return owner;
        }

        public String getFullName() {
            return "\"" + getOwner() + "\".\"" + getName() + "\"";
        }

        public String getInsertLine() {
            return String.format("INSERT INTO MIGRATION_VALIDATION_IGNORE (object_type, owner, object_name) " +
                "VALUES ('%s', '%s', '%s');", getType(), getOwner(), getName());
        }
    }

    static class DatabaseObjectRowMapper implements RowMapper<DatabaseObject> {
        @Override
        public DatabaseObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            DatabaseObject dbObject = new DatabaseObject();
            dbObject.setName(rs.getString("OBJECT_NAME"));
            dbObject.setOwner(rs.getString("OWNER"));
            dbObject.setType(rs.getString("OBJECT_TYPE"));

            return dbObject;
        }
    }

}
