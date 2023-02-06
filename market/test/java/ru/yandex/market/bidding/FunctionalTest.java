package ru.yandex.market.bidding;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(classes = FunctionalTestConfig.class)
@ActiveProfiles(profiles = {
        "functionalTest",
        "development",
        "shop" // Пока только для магазинного. Следует расширять
})
public abstract class FunctionalTest extends JupiterDbUnitTest {
}
