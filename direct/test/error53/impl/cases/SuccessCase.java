package ru.yandex.autotests.directapi.test.error53.impl.cases;

import ru.yandex.autotests.directapi.test.error53.impl.Logins;
import ru.yandex.autotests.directapi.test.error53.impl.TestCaseBuilder;

import static ru.yandex.autotests.directapi.test.error53.impl.TestActions.invokeGetMethod;

public final class SuccessCase {
    public static Object[] success() {
        return new TestCaseBuilder()
                .withDescription("Успешный доступ")
                .withOperator(Logins.CLIENT)
                .withTestAction(invokeGetMethod())
                .build();
    }
}
