package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.MovableToFolder;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.CoreMatchers.not;

/**
 * @author pavponn
 */
public class LizaMovableToFolder implements MovableToFolder {

    private InitStepsRule steps;

    public LizaMovableToFolder(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void moveMessageToFolder(int order, @NotNull String folderName) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().mail().home().displayedMessages().list().waitUntil(not(empty())).get(order).checkBox(),
                steps.pages().mail().home().toolbar().moveMessageBtn()
            )
            .clicksOnElementWithText( steps.pages().mail().home().moveMessageDropdownMenu().customFolders(), folderName);
    }
}
