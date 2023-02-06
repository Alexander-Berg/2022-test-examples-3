package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.util.TestResultSaver;

import static ru.yandex.autotests.innerpochta.api.StatHandler.statHandler;

public class SendToStatAfterClassRule extends ExternalResource {
    @Override
    protected void after() {
        if (TestResultSaver.getInstance().isPassedOrFailedInResult()) {
            statHandler()
                .withData(String.join("", TestResultSaver.getInstance().getResults()))
                .callStatHandler();
        }
        TestResultSaver.getInstance().clearResults();
        super.after();
    }
}
