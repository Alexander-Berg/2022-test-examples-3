package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.MarkableRead;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author pavponn
 */
public class LizaMarkable implements MarkableRead {

    private InitStepsRule steps;

    public LizaMarkable(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void markAsRead(int order) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).messageUnread()
        );
    }

    @Override
    public void markAsUnread(int order) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).messageRead()
        );
    }
}
