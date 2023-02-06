package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.MarkableImportant;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

/**
 * @author pavponn
 */
public class LizaMarkableImportant implements MarkableImportant {

    private InitStepsRule steps;

    public LizaMarkableImportant(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void markAsImportant(int order) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().mail().home().displayedMessages().list().get(order).importanceLabel());
    }

    @Override
    public void markAsUnimportant(int order) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().mail().home().displayedMessages().list().get(order).isImportance());
    }
}
