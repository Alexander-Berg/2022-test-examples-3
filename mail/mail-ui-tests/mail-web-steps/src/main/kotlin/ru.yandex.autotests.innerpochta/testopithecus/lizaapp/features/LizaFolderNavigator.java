package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.FolderNavigator;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

/**
 * @author pavponn
 */
public class LizaFolderNavigator implements FolderNavigator {

    private InitStepsRule steps;

    public LizaFolderNavigator(final InitStepsRule steps) {
        this.steps = steps;
    }
    @NotNull
    @Override
    public List<String> getFoldersList() {
        return steps.pages().mail().home().foldersNavigation().allFolders().waitUntil(not(empty()))
            .stream().map(WebElement::getText).collect(Collectors.toList());
    }

//    @Override
//    public void goToFolder(@NotNull String folderDisplayName) {
//        steps.user().defaultSteps()
//            .clicksOnElementWithText(steps.pages().mail().home().foldersNavigation().allFolders(), folderDisplayName);
//        steps.user().leftColumnSteps().shouldBeInFolder(folderDisplayName);
//    }

    @Override
    public void openFolderList() {

    }

    @Override
    public void closeFolderList() {

    }

    @Override
    public void goToFolder(@NotNull String folderDisplayName, @NotNull List<String> parentFolders) {

    }

    @Override
    public boolean isInTabsMode() {
        return false;
    }
}
