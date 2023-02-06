package ru.yandex.market.notifier.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.notifier.configuration.TestNotifierConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestNotifierConfig.class)
@WebAppConfiguration
public class NotifierContextTest {

    @DisplayName("Контекст нотифаера должен стартовать")
    @Test
    public void shouldStartWithTasks() {
    }
}
