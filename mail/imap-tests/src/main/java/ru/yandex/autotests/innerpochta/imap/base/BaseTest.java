package ru.yandex.autotests.innerpochta.imap.base;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.model.MultipleFailureException;

import ru.yandex.autotests.innerpochta.imap.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.rules.base.IgnoreRule;
import ru.yandex.autotests.innerpochta.imap.rules.base.WriteAllureParamsRule;
import ru.yandex.junitextensions.rules.retry.RetryRule;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.rules.base.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.imap.rules.base.WriteAllureParamsRule.writeParamsForAllure;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:52
 * Базовый класс для всех тестовых классов, содержит обязательные рулы
 */
public class BaseTest {
    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .every(1, TimeUnit.SECONDS).times(1);

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    protected static ImapClient newLoginedClient(Class<?> forClass) {
        return new ImapClient().loginWith(props().account(forClass.getSimpleName()));
    }
}
