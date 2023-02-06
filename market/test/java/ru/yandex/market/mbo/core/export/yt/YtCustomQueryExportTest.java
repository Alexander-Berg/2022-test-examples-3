package ru.yandex.market.mbo.core.export.yt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableSchema;

@RunWith(MockitoJUnitRunner.class)
public class YtCustomQueryExportTest {

    public static final YPath YT_DIR = YPath.simple("//home/mbo/temp");
    public static final String TEMP_TABLE_NAME = "temp_table";
    public static final YPath TEMP_TABLE = YT_DIR.child(TEMP_TABLE_NAME);

    public static final String SELECT_QUERY = "select id " +
        "from super_table";

    public static final int EXPORT_SIZE = 10;

    private YtCustomQueryExporter exporter;
    private TestYt testYt;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private Statement statement;

    @Before
    public void setUp() throws SQLException {
        testYt = new TestYt();
        namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(namedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        statement = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);

        exporter = new YtCustomQueryExporter(testYt, namedParameterJdbcTemplate);
        ReflectionTestUtils.setField(exporter, "tempOffersPath", YT_DIR.toString());
    }

    //TestYt при записи в таблицу не может поставить мета данные media, поэтому вычитываем данные и сверяем count
    @Test
    public void testExport() throws SQLException {
        prepareTestDataSet();
        exporter.exportDataToTable(Optional.empty(),
            TEMP_TABLE_NAME,
            YtTableSchema.create(
                YtColumnSchema.create("id", YtColumnSchema.Type.STRING)),
            SELECT_QUERY,
            ((rs, rowNum) -> YTree.mapBuilder()
                .key("id").value(rs.getString("id"))
                .buildMap())
        );
        Assert.assertTrue(testYt.cypress().exists(YT_DIR));
        testYt.cypress().get(TEMP_TABLE);
        Assert.assertEquals(EXPORT_SIZE,
            testYt.tables().readToList(TEMP_TABLE, YTableEntryTypes.YSON).size());
    }

    private void prepareTestDataSet() throws SQLException {
        ResultSet resultSet = generateResultSet();
        Mockito.when(statement.executeQuery(SELECT_QUERY)).thenReturn(resultSet);
    }

    private ResultSet generateResultSet() {
        ResultSet rs = Mockito.mock(ResultSet.class);
        try {
            Mockito.when(rs.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

            Mockito.when(rs.getString("id"))
                .thenReturn("1")
                .thenReturn("2")
                .thenReturn("3")
                .thenReturn("4")
                .thenReturn("5")
                .thenReturn("6")
                .thenReturn("7")
                .thenReturn("8")
                .thenReturn("9")
                .thenReturn("10");
        } catch (SQLException e) {
            //ignore
        }
        return rs;
    }
}
