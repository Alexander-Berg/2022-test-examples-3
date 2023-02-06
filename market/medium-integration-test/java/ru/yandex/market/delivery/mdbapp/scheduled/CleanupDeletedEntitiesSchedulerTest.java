package ru.yandex.market.delivery.mdbapp.scheduled;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.DeletedEntitiesService;
import ru.yandex.market.delivery.mdbapp.configuration.DeletedEntitiesProperties;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Джобы очистки таблиц для удалённых сущностей")
public class CleanupDeletedEntitiesSchedulerTest extends AbstractMediumContextualTest {

    @Autowired
    private DeletedEntitiesService deletedEntitiesService;

    @Autowired
    private DeletedEntitiesProperties deletedEntitiesProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-05-02T12:00:00Z"),  ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешное удаление")
    @DatabaseSetup("/scheduled/cleanupDeletedEntitiesScheduler/before/setup.xml")
    @ExpectedDatabase(
        value = "/scheduled/cleanupDeletedEntitiesScheduler/after/success_delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void success() {
        CleanupDeletedEntitiesScheduler cleanupDeletedEntitiesScheduler = new CleanupDeletedEntitiesScheduler(
            deletedEntitiesService,
            deletedEntitiesProperties,
            clock
        );
        cleanupDeletedEntitiesScheduler.cleanupDeletedEntities();
    }
}
