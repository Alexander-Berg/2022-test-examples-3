package ru.yandex.market.logistics.iris;

import org.junit.Test;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthTest extends AbstractContextualTest {

    /**
     * Сценарий #1: проверка на OK.
     */
    @Test
    public void ok() throws Exception {
        String monrun = httpOperationWithResult(
            get("/ping"),
            status().is2xxSuccessful()
        );

        assertions().assertThat(monrun).startsWith("0;");
    }
}
