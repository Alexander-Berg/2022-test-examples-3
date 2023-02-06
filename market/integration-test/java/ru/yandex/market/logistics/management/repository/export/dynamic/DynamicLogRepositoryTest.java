package ru.yandex.market.logistics.management.repository.export.dynamic;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;


class DynamicLogRepositoryTest extends AbstractContextualTest {
    @Autowired
    private DynamicLogRepository dynamicLogRepository;

    @DatabaseSetup("/data/repository/dynamic_log/log_entries.xml")
    @ExpectedDatabase(
        value = "/data/repository/dynamic_log/after/delete_old_dynamic_log.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    public void deleteByValidatedBefore() {
        dynamicLogRepository.deleteByValidatedBefore(LocalDateTime.of(
            2021, 3, 15, 0, 0, 30
        ));
    }
}
