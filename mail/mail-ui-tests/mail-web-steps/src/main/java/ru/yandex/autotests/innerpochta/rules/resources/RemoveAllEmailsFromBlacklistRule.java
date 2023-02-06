package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 12.10.15.
 */
public class RemoveAllEmailsFromBlacklistRule extends ExternalResource{
    private AllureStepStorage user;

    private RemoveAllEmailsFromBlacklistRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllEmailsFromBlacklistRule removeAllEmailsFromBlacklist(AllureStepStorage user) {
        return new RemoveAllEmailsFromBlacklistRule(user);
    }

    @Override
    protected void before() throws Throwable {
        removeEmailsFromBlacklist();
    }

    public RemoveAllEmailsFromBlacklistRule removeEmailsFromBlacklist() {
        user.apiFiltersSteps().removeAllEmailsFromBlackList();
        return this;
    }
}
