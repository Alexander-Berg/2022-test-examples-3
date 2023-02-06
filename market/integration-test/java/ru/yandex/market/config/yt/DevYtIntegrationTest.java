package ru.yandex.market.config.yt;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(DevYtJdbcConfig.class)
public class DevYtIntegrationTest extends JupiterDbUnitTest {
}
