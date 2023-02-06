package ru.yandex.mail.junit_extensions.cerberus_runner;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.junit_extensions.program_runner.RegisterProgramsRegistry;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.junit_extensions.cerberus_runner.TestProgramRegistry.CERBERUS_PORT;
import static ru.yandex.mail.junit_extensions.cerberus_runner.TestProgramRegistry.DB_NAME;

@RunCerberus
@ExtendWith(PgLocalExtension.class)
@InitDb(name = DB_NAME, migration = "migrations")
@RegisterProgramsRegistry(TestProgramRegistry.class)
class ExtensionTest {
    @Test
    @SneakyThrows
    @DisplayName("Verify that cerberus is running and available")
    void testCerberus() {
        val client = new CerberusClient(CERBERUS_PORT);

        assertThat(client.ping())
            .map(HttpResponse::statusCode)
            .contains(200);
    }
}
