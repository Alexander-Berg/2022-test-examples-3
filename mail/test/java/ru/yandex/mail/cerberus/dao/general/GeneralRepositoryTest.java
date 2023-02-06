package ru.yandex.mail.cerberus.dao.general;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.general.GeneralRepositoryTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class GeneralRepositoryTest {
    static final String DB_NAME = "general_repository_db";

    @Inject
    GeneralRepository generalRepository;

    @Test
    @DisplayName("Verify that pg_partman run_maintenance call is possible")
    void runMaintenanceTest() {
        assertThatCode(() -> generalRepository.runPgPartmanMaintenance())
            .doesNotThrowAnyException();
    }
}
