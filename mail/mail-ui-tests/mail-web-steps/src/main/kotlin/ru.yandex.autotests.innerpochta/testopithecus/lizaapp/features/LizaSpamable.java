package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.Spamable;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

/**
 * @author pavponn
 */
public class LizaSpamable implements Spamable {

    private InitStepsRule steps;

    public LizaSpamable(final InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void moveToSpam(int order) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).checkBox()
            );
    }

    @Override
    public void moveFromSpam(int order) {

    }
}
