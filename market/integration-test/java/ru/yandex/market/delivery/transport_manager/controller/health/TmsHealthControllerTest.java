package ru.yandex.market.delivery.transport_manager.controller.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitDatabaseConnectionQrtz"
)
@DatabaseSetup(value = "/repository/health/qrtz/setup.xml")
public class TmsHealthControllerTest extends AbstractContextualTest {

    @Autowired
    TmsHealthController healthController;

    // JobLogAnalysisService в либе Tms не поддерживает внешние часы и работает через System.currentTimeMillis(),
    // поэтому проверяем только начало сообщения, без фактического диффа

    // Также, в силу кэширования внутри либы, сетап теста можно провести только один раз, потом используется кэш

    @Test
    void detectDelay() {
        String healthCheck = healthController.failedTms("delayedJob");
        softly.assertThat(healthCheck).startsWith("2;Job delayedJob delay time exceeded. Max: 86401 seconds");
    }

    @Test
    void detectOverdue() {
        String healthCheck = healthController.failedTms("overdueJob");
        softly.assertThat(healthCheck).startsWith("2;Job overdueJob has never been finished yet. " +
            "Job overdueJob execution time exceeded. Max: 31 seconds");
    }

    @Test
    void detectFailure() {
        String healthCheck = healthController.failedTms("failedJob");
        softly.assertThat(healthCheck).isEqualTo(
            "2;Job Exception: class org.apache.ibatis.exceptions.TooManyResultsException: Expected one result (or " +
                "null) to be returned by selectOne(), but found: 2");
    }

    @Test
    void camelCaseConversion() {
        String healthCheck = healthController.failedTms("failed_job");
        softly.assertThat(healthCheck).isEqualTo(
            "2;Job Exception: class org.apache.ibatis.exceptions.TooManyResultsException: Expected one result (or " +
                "null) to be returned by selectOne(), but found: 2");
    }

    @Test
    void ok() {
        String healthCheck = healthController.failedTms("normalJob");
        softly.assertThat(healthCheck).isEqualTo("0;OK");
    }
}
