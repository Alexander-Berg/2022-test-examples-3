package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.structures.ListItem;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

public class ClearSteps {
    private final ImapClient imap;


    public ClearSteps(ImapClient imap) {
        this.imap = imap;
    }

    @Step("Чистим ящик")
    public void clearMailbox() {
        Iterable<ListItem> listResponse = imap.request(list("\"\"", "*")).getItems();
        for (ListItem listItem : listResponse) {
            if (!isSystemFolder(listItem.getName())) {
                imap.request(delete(literal(listItem.getName())));
            } else {
                clearFolder(listItem.getName());
            }
        }
        clearFolder(systemFolders().getDeleted());
    }

    private boolean isSystemFolder(String folderName) {
        for (String sysFolder : systemFolders().getSystemFoldersWithInbox()) {
            if (folderName.equals(Utils.quoted(sysFolder)) | folderName.equals(sysFolder)) {
                return true;
            }
        }
        return false;
    }

    @Step("Чистим папку <{0}>")
    public void clearFolder(String folderName) {
        Integer messageCount = imap.request(select(literal(folderName))).exist();
        if (messageCount != 0) {
            imap.request(store("1:" + String.valueOf(messageCount), StoreRequest.FLAGS,
                    roundBraceList(MessageFlags.DELETED.value())));
            imap.request(close());
        }
    }

    @Step("Отписываемся от всех папок")
    public void unsubscribeFromAllFolders() {
        Iterable<ListItem> lsubResponse = imap.request(lsub("\"\"", "*")).getItems();
        for (ListItem listItem : lsubResponse) {
            imap.request(unsubscribe(listItem.getName()));
        }
    }

    @Step("Удаляем все флаги у всех писем")
    public void clearFlags() {
        Iterable<ListItem> listResponse = imap.request(list("\"\"", "*")).getItems();
        for (ListItem listItem : listResponse) {
            clearFlagsInFolder(listItem.getName());
        }
    }

    @Step("Удаляем все флаги у всех писем в папке {0}")
    public void clearFlagsInFolder(String folder) {
        Integer messageCount = imap.request(select(literal(folder))).exist();
        if (messageCount != 0) {
            imap.request(store("1:" + String.valueOf(messageCount), StoreRequest.FLAGS, roundBraceList("")));
        }
    }

    @Step("Удаляем все флаги у всех писем в папках {0}")
    public void clearFlagsInFolders(List<String> folders) {
        for (String folder : folders) {
            clearFlagsInFolder(folder);
        }
    }
}
