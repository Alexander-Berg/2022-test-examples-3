package ru.yandex.direct.jobs;

import ru.yandex.direct.common.spring.TestingComponent;
import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;

/**
 * Для того, чтобы не падал тест {@link TestingComponentWhiteListTest}
 */
@TestingComponent
public class DummyTestingComponent {
}
