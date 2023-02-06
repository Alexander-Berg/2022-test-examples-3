package ru.yandex.autotests.innerpochta.pop3.base;

import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.innerpochta.imap.rules.base.IgnoreRule;
import ru.yandex.autotests.innerpochta.imap.rules.base.WriteAllureParamsRule;

import static ru.yandex.autotests.innerpochta.imap.rules.base.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.imap.rules.base.WriteAllureParamsRule.writeParamsForAllure;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.09.14
 * Time: 18:51
 */
public class BaseTest {

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

}
