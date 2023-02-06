package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.common.YSSet;
import com.yandex.xplat.testopithecus.GroupMode;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.util.Utils;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

/**
 * @author pavponn
 */
public class LizaGroupMode implements GroupMode {

    private InitStepsRule steps;

    public LizaGroupMode(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public boolean isInGroupMode() {
        // Считаем, что в group mode если активен тулбар
        return Utils.isPresent().matches(steps.pages().mail().home().enabledToolbar());
    }

    @Nullable
    @Override
    public YSSet<Integer> getSelectedMessages() {
        ElementsCollection<MessageBlock> msgBlocks = steps.pages().mail().home().displayedMessages().list();
        return new YSSet(range(0, msgBlocks.size())
            .filter(i -> msgBlocks.get(i).checkBox().isSelected())
            .boxed()
            .collect(Collectors.toSet()));
    }

    @Override
    public void markAsReadSelectedMessages() {
        steps.user().defaultSteps().clicksOn(steps.pages().mail().home().toolbar().markAsReadButton());
    }

    @Override
    public void markAsUnreadSelectedMessages() {
        steps.user().defaultSteps().clicksOn(steps.pages().mail().home().toolbar().markAsUnreadButton());
    }

    @Override
    public void deleteSelectedMessages() {
        steps.user().defaultSteps().clicksOn(steps.pages().mail().home().toolbar().deleteButton());
    }

    @Override
    public int getNumberOfSelectedMessages() {
        return 0;
    }

    @Override
    public void selectMessage(int byOrder) {

    }

    @Override
    public void selectAllMessages() {

    }

    @Override
    public void initialMessageSelect(int byOrder) {

    }

    @Override
    public void applyLabelsToSelectedMessages(@NotNull List<String> labelNames) {

    }

    @Override
    public void removeLabelsFromSelectedMessages(@NotNull List<String> labelNames) {

    }

    @Override
    public void markAsImportantSelectedMessages() {

    }

    @Override
    public void markAsUnImportantSelectedMessages() {

    }

    @Override
    public void markAsSpamSelectedMessages() {

    }

    @Override
    public void markAsNotSpamSelectedMessages() {

    }

    @Override
    public void moveToFolderSelectedMessages(@NotNull String folderName) {

    }

    @Override
    public void archiveSelectedMessages() {

    }

    @Override
    public void unselectMessage(int byOrder) {

    }

    @Override
    public void unselectAllMessages() {

    }
}
