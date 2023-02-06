package ru.yandex.market.deliverycalculator.storage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.deliverycalculator.storage.configs.DeliveryCalculatorStorageTestConfig;

/**
 * Базовый класс для написания функциональных тестов в модуле delivery calculator storage.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DeliveryCalculatorStorageTestConfig.class)
@ActiveProfiles({"functionalTest", "development"})
public abstract class FunctionalTest extends JupiterDbUnitTest {
}
