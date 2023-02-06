package ru.yandex.market;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;

/**
 * Базовый класс для функциональных тестов.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@SpringJUnitConfig(classes = EmbeddedPostgresConfig.class)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
}
