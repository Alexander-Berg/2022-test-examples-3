package ru.yandex.market.load.admin.postgres;

import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.load.admin.AbstractFunctionalTest;

@TestPropertySource(properties = {
        "spring.liquibase.clearChecksums=true",
        "spring.liquibase.dropFirst=true"
})
@Disabled
public class LiquibaseResetTest extends AbstractFunctionalTest {
    @Test
    public void resetAll() throws DatabaseException {
    }
}
