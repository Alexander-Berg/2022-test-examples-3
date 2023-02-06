package ru.yandex.autotests.innerpochta.testopithecus.lizaapp;

import com.yandex.xplat.testopithecus.ComposeMessage;
import com.yandex.xplat.testopithecus.ComposeMessageFeature;
import com.yandex.xplat.testopithecus.CreatableFolder;
import com.yandex.xplat.testopithecus.CreatableFolderFeature;
import com.yandex.xplat.testopithecus.DeleteMessage;
import com.yandex.xplat.testopithecus.DeleteMessageFeature;
import com.yandex.xplat.testopithecus.ExpandableThreads;
import com.yandex.xplat.testopithecus.ExpandableThreadsFeature;
import com.yandex.xplat.testopithecus.FolderNavigator;
import com.yandex.xplat.testopithecus.FolderNavigatorFeature;
import com.yandex.xplat.testopithecus.GroupMode;
import com.yandex.xplat.testopithecus.GroupModeFeature;
import com.yandex.xplat.testopithecus.MarkableImportant;
import com.yandex.xplat.testopithecus.MarkableImportantFeature;
import com.yandex.xplat.testopithecus.MarkableRead;
import com.yandex.xplat.testopithecus.MarkableReadFeature;
import com.yandex.xplat.testopithecus.MessageListDisplay;
import com.yandex.xplat.testopithecus.MessageListDisplayFeature;
import com.yandex.xplat.testopithecus.MovableToFolder;
import com.yandex.xplat.testopithecus.MovableToFolderFeature;
import com.yandex.xplat.testopithecus.MultiAccount;
import com.yandex.xplat.testopithecus.MultiAccountFeature;
import com.yandex.xplat.testopithecus.Spamable;
import com.yandex.xplat.testopithecus.SpamableFeature;
import com.yandex.xplat.testopithecus.WYSIWIG;
import com.yandex.xplat.testopithecus.WriteMessage;
import com.yandex.xplat.testopithecus.WriteMessageFeature;
import com.yandex.xplat.testopithecus.WysiwygFeature;
import com.yandex.xplat.testopithecus.YandexLogin;
import com.yandex.xplat.testopithecus.YandexLoginFeature;
import com.yandex.xplat.testopithecus.common.App;
import com.yandex.xplat.testopithecus.common.FeatureRegistry;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaComposeMessage;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaCreatableFolder;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaDeleteMessage;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaExpandableThreads;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaFolderNavigator;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaGroupMode;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaMarkable;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaMarkableImportant;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaMessageListDisplay;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaMovableToFolder;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaMultiAccount;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaSpamable;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaWriteMessage;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaWysiwyg;
import ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features.LizaYandexLogin;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author a-zoshchuk
 */
public class LizaMailApplication implements App {

    private InitStepsRule steps;
    private List<String> supportedFeatures;

    private ComposeMessage composeMessage;
    private ExpandableThreads expandableThreads;
    private GroupMode groupMode;
    private MarkableRead markable;
    private MarkableImportant markableImportant;
    private MessageListDisplay messageListDisplay;
    private WriteMessage writeMessage;
    private DeleteMessage deleteMessage;
    private Spamable spamable;
    private FolderNavigator folderNavigator;
    private CreatableFolder creatableFolder;
    private MovableToFolder movableToFolder;
    private WYSIWIG wysiwyg;
    private YandexLogin yandexLogin;
    private MultiAccount multiAccount;

    public static List<String> allSupportedFeatures() {
        return new ArrayList<>(Arrays.asList(
            ComposeMessageFeature.getGet().getName(),
            ExpandableThreadsFeature.getGet().getName(),
            GroupModeFeature.getGet().getName(),
            MarkableReadFeature.getGet().getName(),
            MarkableImportantFeature.getGet().getName(),
            MessageListDisplayFeature.getGet().getName(),
            WriteMessageFeature.getGet().getName(),
            DeleteMessageFeature.getGet().getName(),
            SpamableFeature.getGet().getName(),
            FolderNavigatorFeature.getGet().getName(),
            CreatableFolderFeature.getGet().getName(),
            MovableToFolderFeature.getGet().getName(),
            WysiwygFeature.getGet().getName(),
            YandexLoginFeature.getGet().getName(),
            MultiAccountFeature.getGet().getName()
        ));
    }

    public LizaMailApplication(InitStepsRule steps) {
        this.steps = steps;
    }

    @NotNull
    @Override
    public List<String> getSupportedFeatures() {
        if (supportedFeatures == null) {
            supportedFeatures = allSupportedFeatures();
        }
        return supportedFeatures;
    }

    @NotNull
    @Override
    public Object getFeature(@NotNull String feature) {
        return new FeatureRegistry()
            .register(ComposeMessageFeature.getGet(), composeMessage())
            .register(ExpandableThreadsFeature.getGet(), expandableThreads())
            .register(GroupModeFeature.getGet(), groupMode())
            .register(MarkableReadFeature.getGet(), markable())
            .register(MarkableImportantFeature.getGet(), markableImportant())
            .register(MessageListDisplayFeature.getGet(), messageListDisplay())
            .register(WriteMessageFeature.getGet(), writeMessage())
            .register(DeleteMessageFeature.getGet(), deleteMessage())
            .register(SpamableFeature.getGet(), spamable())
            .register(FolderNavigatorFeature.getGet(), folderNavigator())
            .register(CreatableFolderFeature.getGet(), creatableFolder())
            .register(MovableToFolderFeature.getGet(), movableToFolder())
            .register(WysiwygFeature.getGet(), wysiwyg())
            .register(YandexLoginFeature.getGet(), yandexLogin())
            .register(MultiAccountFeature.getGet(), multiAccount())
            .get(feature);
    }

    @Override
    public void setSupportedFeatures(@NotNull List<String> featuresList) {
        supportedFeatures = featuresList;
    }

    private ComposeMessage composeMessage() {
        if (composeMessage == null) {
            composeMessage = new LizaComposeMessage(steps);
        }
        return composeMessage;
    }

    private ExpandableThreads expandableThreads() {
        if (expandableThreads == null) {
            expandableThreads = new LizaExpandableThreads(steps);
        }
        return expandableThreads;
    }

    private GroupMode groupMode() {
        if (groupMode == null) {
            groupMode = new LizaGroupMode(steps);
        }
        return groupMode;
    }

    private MarkableRead markable() {
        if (markable == null) {
            markable = new LizaMarkable(steps);
        }
        return markable;
    }

    private MarkableImportant markableImportant() {
        if (markableImportant == null) {
            markableImportant = new LizaMarkableImportant(steps);
        }
        return markableImportant;
    }

    private MessageListDisplay messageListDisplay() {
        if (messageListDisplay == null) {
            messageListDisplay = new LizaMessageListDisplay(steps);
        }
        return messageListDisplay;
    }

    private WriteMessage writeMessage() {
        if (writeMessage == null) {
            writeMessage = new LizaWriteMessage(steps);
        }
        return writeMessage;
    }

    private DeleteMessage deleteMessage() {
        if (deleteMessage == null) {
            deleteMessage = new LizaDeleteMessage(steps);
        }
        return deleteMessage;
    }

    private Spamable spamable() {
        if (spamable == null) {
            spamable = new LizaSpamable(steps);
        }
        return spamable;
    }

    private FolderNavigator folderNavigator() {
        if (folderNavigator == null) {
            folderNavigator = new LizaFolderNavigator(steps);
        }
        return folderNavigator;
    }

    private CreatableFolder creatableFolder() {
        if (creatableFolder == null) {
            creatableFolder = new LizaCreatableFolder(steps);
        }
        return creatableFolder;
    }

    private MovableToFolder movableToFolder() {
        if (movableToFolder == null) {
            movableToFolder = new LizaMovableToFolder(steps);
        }
        return movableToFolder;
    }

    private WYSIWIG wysiwyg() {
        if (wysiwyg == null) {
            wysiwyg = new LizaWysiwyg(steps);
        }
        return wysiwyg;
    }

    private YandexLogin yandexLogin() {
        if (yandexLogin == null) {
            yandexLogin = new LizaYandexLogin(steps);
        }
        return yandexLogin;
    }

    private MultiAccount multiAccount() {
        if (multiAccount == null) {
            multiAccount = new LizaMultiAccount(steps);
        }
        return multiAccount;
    }

    @NotNull
    @Override
    public String dump(@NotNull App model) {
        return "Implement this";
    }
}
