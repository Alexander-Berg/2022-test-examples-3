package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 07.09.15.
 */
public class RemoveAllAbookGroups extends ExternalResource {

    private AllureStepStorage user;

    private RemoveAllAbookGroups(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllAbookGroups removeAllAbookGroups(AllureStepStorage user) {
        return new RemoveAllAbookGroups(user);
    }

    @Override
    protected void before() throws Throwable {
        user.apiAbookSteps().removeAllAbookGroups();
    }
}
