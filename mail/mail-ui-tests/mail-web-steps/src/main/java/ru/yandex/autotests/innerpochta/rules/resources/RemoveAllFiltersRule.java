package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 31.08.15.
 */
public class RemoveAllFiltersRule extends ExternalResource{
    private AllureStepStorage user;

    private RemoveAllFiltersRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllFiltersRule removeAllFiltersRule(AllureStepStorage user) {
        return new RemoveAllFiltersRule(user);
    }

    @Override
    protected void before() throws Throwable {
        removeFilters();
    }

    public RemoveAllFiltersRule removeFilters() {
        user.apiFiltersSteps().deleteAllUserFilters();
        return this;
    }
}
