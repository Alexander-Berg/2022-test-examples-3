package ru.yandex.market.common.test.db;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;

/**
 * Базовый класс для юнит-тестов базы данных.
 *
 * ВНИМАНИЕ! Не ставьте здесь {@link org.springframework.test.annotation.DirtiesContext @DirtiesContext},
 * Иначе ненужная очистка контекста будет выполнятся каждый раз.
 *
 * @author zoom
 */
@TestExecutionListeners({
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MemCachedServiceTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
public class DbUnitTest {
}
