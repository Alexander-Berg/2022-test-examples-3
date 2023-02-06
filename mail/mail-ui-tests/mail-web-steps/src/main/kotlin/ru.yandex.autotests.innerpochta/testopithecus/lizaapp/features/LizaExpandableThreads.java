package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.ExpandableThreads;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author pavponn
 */
public class LizaExpandableThreads implements ExpandableThreads {

    private InitStepsRule steps;

    public LizaExpandableThreads(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void markThreadMessageAsRead(int threadOrder, int messageOrder) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty()))
                    .get(threadOrder).expandedThreadMessageList().get(messageOrder).messageUnread()
            );
    }

    @Override
    public void markThreadMessageAsUnRead(int threadOrder, int messageOrder) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty()))
                    .get(threadOrder).expandedThreadMessageList().get(messageOrder).messageRead()
            );

    }

    @Override
    public void expandThread(int order) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).onlyExpandThread()
        );
    }

    @Override
    public void collapseThread(int order) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).onlyCollapseThread()
        );
    }
}
