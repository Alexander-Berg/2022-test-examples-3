package ru.yandex.market.partner.security.checker;

import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.AuthorityChecker;

/**
 * Общие тесты для {@link AuthorityChecker} компонента mbi-partner.
 *
 * @author Vladislav Bauer
 */
public class AuthorityCheckerTest extends FunctionalTest {

    @Autowired
    private ApplicationContext context;

    /**
     * Тест проверяет что все {@link AuthorityChecker} могут успешно инициализироваться.
     */
    @Test
    public void testCheckersInitialization() {
        final Map<String, AuthorityChecker> checkers = context.getBeansOfType(AuthorityChecker.class);
        MatcherAssert.assertThat(checkers.isEmpty(), Matchers.equalTo(false));
    }

}
