package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.MessageListDisplay;
import com.yandex.xplat.testopithecus.MessageView;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import java.util.List;

import static ru.yandex.autotests.innerpochta.testopithecus.lizaapp.utils.Utils.convertHtmlMessagesToModel;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author pavponn
 */
public class LizaMessageListDisplay implements MessageListDisplay {

    private InitStepsRule steps;

    public LizaMessageListDisplay(InitStepsRule steps) {
        this.steps = steps;
    }

    @NotNull
    @Override
    public List<MessageView> getMessageList(int limit) {
        ElementsCollection<MessageBlock> allVisibleMessages =
            steps.pages().mail().home().displayedMessages().threadMainMessagesList().waitUntil(not(empty()));
        return convertHtmlMessagesToModel(allVisibleMessages);
    }

    @Override
    public void refreshMessageList() {
        steps.user().defaultSteps().refreshPage();
    }

    @Override
    public int unreadCounter() {
        return steps.user().leftColumnSteps().unreadCounter();
    }

//    @Override
//    public boolean isInThreadMode() {
//        String title = steps.pages().mail().home().foldersNavigation().currentFolder().getText();
//        return
//            !title.equals(DefaultFolderName.getOutgoing()) &&
//            !title.equals(DefaultFolderName.getDraft()) &&
//            !title.equals(DefaultFolderName.getTrash()) &&
//            !title.equals(DefaultFolderName.getSpam());
//    }

    @Override
    public void goToAccountSwitcher() {
        steps.user().defaultSteps().clicksOn(steps.pages().mail().home().userName());
    }
}
