package ru.yandex.market;

import java.sql.ResultSet;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.test.db.DbQueryConfigChecker;
import ru.yandex.market.sqb.test.db.DbSqlUtils;
import ru.yandex.market.sqb.test.db.DbUtils;
import ru.yandex.market.sqb.util.SqbGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Интеграционные тесты выгрузок с реальной БД.
 *
 * @author Vladislav Bauer
 */
class DataIntegrationTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "shops_data",
        "suppliers_data",
        "fmcg_feeds_data",
        "vendor_billing_data",
        "direct_data",
        "eats_and_lavka_partners_data",
        "foreign_shops_data"
    })
    void testSmoke(final String configName) throws Exception {
        final Supplier<String> configReader = DataUnitTestHelper.resource(configName, DataUnitTestHelper.CONFIG_XML);
        final QueryModel queryModel = SqbGenerationUtils.readQueryModel(configReader);
        final String sql = SqbGenerationUtils.generateSQL(queryModel);
        assertThat(sql, not(emptyOrNullString()));

        DbUtils.runWithDb(() ->
            DbUtils.runWithStatement(statement -> {
                final String limitedSql = DbSqlUtils.limitedQuery(sql);
                final ResultSet resultSet = statement.executeQuery(limitedSql);
                DbQueryConfigChecker.checkColumns(resultSet, queryModel);
                return null;
            })
        );
    }

}
