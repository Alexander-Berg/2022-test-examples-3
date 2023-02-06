package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 13.05.15.
 */
public class RemoveAllLabelsRule extends ExternalResource {

    private AllureStepStorage user;

    private RemoveAllLabelsRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllLabelsRule removeAllLabelsRule(AllureStepStorage user) {
        return new RemoveAllLabelsRule(user);
    }

    @Override
    protected void before() throws Throwable {
        deleteLabels();
    }

    public RemoveAllLabelsRule deleteLabels() {
        user.apiLabelsSteps().deleteAllCustomLabels();
        return this;
    }
}
