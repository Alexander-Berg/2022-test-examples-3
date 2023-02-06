package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.DeleteMessage;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

/**
 * @author pavponn
 */
public class LizaDeleteMessage implements DeleteMessage {

    private InitStepsRule steps;

    public LizaDeleteMessage(final InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void deleteMessage(int order) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).checkBox()
        );
        steps.user().defaultSteps().clicksOn(steps.pages().mail().home().toolbar().deleteButton());
    }
}
