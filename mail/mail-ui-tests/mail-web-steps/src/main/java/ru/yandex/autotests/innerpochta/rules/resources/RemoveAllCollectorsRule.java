package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 15.05.15.
 */
public class RemoveAllCollectorsRule extends ExternalResource {

    private AllureStepStorage user;

    private RemoveAllCollectorsRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllCollectorsRule removeAllCollectorsRule(AllureStepStorage user) {
        return new RemoveAllCollectorsRule(user);
    }

    @Override
    protected void before() throws Throwable {
        user.apiCollectorSteps().removeAllUserCollectors();
    }
}
