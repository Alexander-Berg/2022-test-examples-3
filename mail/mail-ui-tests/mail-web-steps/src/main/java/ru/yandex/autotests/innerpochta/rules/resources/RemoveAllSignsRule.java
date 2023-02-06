package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.CLEAR_SIGNS_AMOUNT;

/**
 * Created by mabelpines on 23.07.15.
 */
public class RemoveAllSignsRule extends ExternalResource{

    private AllureStepStorage user;

    private RemoveAllSignsRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllSignsRule removeAllSignsRule(AllureStepStorage user) {
        return new RemoveAllSignsRule(user);
    }

    @Override
    protected void before() throws Throwable {
        user.apiSettingsSteps().changeSignsAmountTo(CLEAR_SIGNS_AMOUNT);
    }
}
