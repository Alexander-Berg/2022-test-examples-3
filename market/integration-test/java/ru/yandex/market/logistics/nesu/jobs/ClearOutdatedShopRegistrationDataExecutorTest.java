package ru.yandex.market.logistics.nesu.jobs;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.ClearOutdatedShopRegistrationDataExecutor;

@DisplayName("Тесты на удаление устаревших данных, необходимых для регистрации магазина")
public class ClearOutdatedShopRegistrationDataExecutorTest extends AbstractContextualTest {
    @Autowired
    private ClearOutdatedShopRegistrationDataExecutor clearOutdatedShopRegistrationDataExecutor;

    @Test
    @DisplayName("Удаление одной записи")
    @DatabaseSetup("/jobs/executors/clear_outdated_shop_registration_data/before/prepare.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/clear_outdated_shop_registration_data/after/after_single_record_clear.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singleRecordDeleted() {
        clearOutdatedShopRegistrationDataExecutor.doJob(null);
    }

    @Test
    @DisplayName("Удаление нескольких записи")
    @DatabaseSetup("/jobs/executors/clear_outdated_shop_registration_data/before/prepare.xml")
    @DatabaseSetup(
        value = "/jobs/executors/clear_outdated_shop_registration_data/before/additional_regitered_shop.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executors/clear_outdated_shop_registration_data/after/after_multiple_records_clear.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleRecordsDeleted() {
        clearOutdatedShopRegistrationDataExecutor.doJob(null);
    }
}
