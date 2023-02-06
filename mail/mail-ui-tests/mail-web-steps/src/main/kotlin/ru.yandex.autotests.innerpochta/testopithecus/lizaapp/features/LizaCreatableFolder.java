package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.CreatableFolder;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

/**
 * @author pavponn
 */
public class LizaCreatableFolder implements CreatableFolder {

    private InitStepsRule steps;

    public LizaCreatableFolder(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void createFolder(@NotNull String folderDisplayName) {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().mail().home().createFolderBtn())
            .inputsTextInElement(steps.pages().mail().home().createFolderPopup().folderName(), folderDisplayName)
            .clicksOn(steps.pages().mail().home().createFolderPopup().create());
    }
}
