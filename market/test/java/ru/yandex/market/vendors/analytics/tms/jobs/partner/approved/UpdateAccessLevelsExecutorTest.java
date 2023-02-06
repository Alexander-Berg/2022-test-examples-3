package ru.yandex.market.vendors.analytics.tms.jobs.partner.approved;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Функциональный тест для джобы {@link UpdateAccessLevelsExecutor}.
 *
 * @author sergeymironov
 */
class UpdateAccessLevelsExecutorTest extends FunctionalTest {

    @Autowired
    private UpdateAccessLevelsExecutor updateAccessLevelsExecutor;

    @Test
    @DbUnitDataSet(
            before = "UpdateAccessLevelsExecutorTest.before.csv",
            after = "UpdateAccessLevelsExecutorTest.after.csv")
    @ClickhouseDbUnitDataSet(before = "UpdateAccessLevelsExecutorTest.before.clickhouse.csv")
    @DisplayName("Тестирует обновление уровней доступа магазина")
    void updateAccessLevelsExecutorTestTest() {
        updateAccessLevelsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "UpdateAccessLevelsExecutorTest_calculatesCsvSeparately.before.csv",
            after = "UpdateAccessLevelsExecutorTest_calculatesCsvSeparately.after.csv")
    @ClickhouseDbUnitDataSet(before = "UpdateAccessLevelsExecutorTest_calculatesCsvSeparately.before.clickhouse.csv")
    @DisplayName("Тестирует обновление уровней доступа магазина по источнику CSV.")
    void updateAccessLevelsExecutor_calculatesCsvSeparately() {
        updateAccessLevelsExecutor.doJob(null);
    }
}
