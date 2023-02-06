package ru.yandex.market.core.database;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(classes = EmbeddedPostgresConfig.class)
public abstract class FunctionalTest extends JupiterDbUnitTest {
}
