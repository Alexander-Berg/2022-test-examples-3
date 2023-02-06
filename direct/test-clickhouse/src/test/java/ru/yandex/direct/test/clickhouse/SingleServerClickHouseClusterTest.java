package ru.yandex.direct.test.clickhouse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.utils.Checked;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;


@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class SingleServerClickHouseClusterTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();

    /**
     * Проверка, что {@link JunitRuleClickHouseCluster#singleServerCluster()} просто работает.
     */
    @Test
    public void itWorks() throws IOException, InterruptedException, SQLException {
        ClickHouseCluster cluster = junitRuleClickHouseCluster.singleServerCluster();
        DataSource dataSource = new ClickHouseDataSource(cluster.getClickHouseJdbcUrls().get("clickhouse"));
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT hostName()");
                softly.assertThat(resultSet.next())
                        .as("SELECT hostName() returns at least one record")
                        .isTrue();
                softly.assertThat(resultSet.getString(1))
                        .as("SELECT hostName() returns what expected")
                        .isEqualTo("clickhouse");
            }

            try (Statement statement = conn.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT name FROM system.databases");
                List<String> dbNames = new ArrayList<>();
                while (resultSet.next()) {
                    dbNames.add(resultSet.getString(1));
                }
                softly.assertThat(dbNames)
                        .as("Fresh ClickHouse has no custom databases")
                        .containsExactly("default", "system");
            }
        }
    }

    @Parameters({
            "Antarctica/Troll",
            "Asia/Novosibirsk",
            MOSCOW_TIMEZONE,
    })
    @Test
    public void checkTimezoneLikeInProduction(String timeZoneName) throws IOException, InterruptedException, SQLException {
        final TimeZone initialTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneName));
            ClickHouseCluster clickHouseCluster = junitRuleClickHouseCluster.singleServerCluster();
            DataSource dataSource = new ClickHouseDataSource(
                    clickHouseCluster.getClickHouseJdbcUrls().values().iterator().next());
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("SELECT timezone()");
                    Assertions.assertThatCode(resultSet::next)
                            .describedAs("Clickhouse returned timezone")
                            .doesNotThrowAnyException();
                    Assertions.assertThat(Checked.get(() -> resultSet.getString(1)))
                            .describedAs("Should have timezone {} like in production ClickHouse", MOSCOW_TIMEZONE)
                            .isIn(MOSCOW_TIMEZONE, "W-SU");
                }
            }
        } finally {
            TimeZone.setDefault(initialTimeZone);
        }
    }
}
