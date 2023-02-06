package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 19.05.15.
 */
public class RemoveAllContactsFromAbookRule extends ExternalResource {
    private AllureStepStorage user;

    private RemoveAllContactsFromAbookRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllContactsFromAbookRule removeAllContactsFromAbookRule(AllureStepStorage user) {
        return new RemoveAllContactsFromAbookRule(user);
    }

    @Override
    protected void before() throws Throwable {
        user.apiAbookSteps().removeAllAbookContacts();
    }
}
