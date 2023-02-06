package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * @author mariya-murm
 */
public class RemoveAllEmailsFromWhitelistRule extends ExternalResource{
    private AllureStepStorage user;

    private RemoveAllEmailsFromWhitelistRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllEmailsFromWhitelistRule removeAllEmailsFromWhitelist(AllureStepStorage user) {
        return new RemoveAllEmailsFromWhitelistRule(user);
    }

    @Override
    protected void before() throws Throwable {
        removeEmailsFromWhitelist();
    }

    public RemoveAllEmailsFromWhitelistRule removeEmailsFromWhitelist() {
        user.apiFiltersSteps().removeAllEmailsFromWhiteList();
        return this;
    }
}
