package ru.yandex.mail.things.utils;

import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.Folders;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.generated.Folder;
import ru.yandex.mail.tests.hound.generated.FolderSymbol;

import java.util.List;
import java.util.Map;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class FolderList {
    private Folders folders_ = null;
    private UserCredentials authClient;

    private Folders foldersInternal() {
        if (folders_ == null) {
            folders_ = Folders.folders(
                    HoundApi.apiHound(
                            HoundProperties.properties()
                                    .houndUri(),
                            props().getCurrentRequestId()
                    )
                            .folders()
                            .withUid(authClient.account().uid())
                            .post(shouldBe(HoundResponses.ok200()))
            );
        }

        return folders_;
    }

    public FolderList(UserCredentials rule) {
        this.authClient = rule;
    }

    public String fidByFolderSymbol(FolderSymbol FolderSymbol) {
        return foldersInternal().fid(FolderSymbol);
    }

    public String fidByName(String name) {
        return foldersInternal().fid(name);
    }

    public String nameByFolderSymbol(FolderSymbol FolderSymbol) {
        return foldersInternal().name(FolderSymbol);
    }

    public String nameByFid(String fid) {
        return foldersInternal().name(fid);
    }

    public String folderPop3(FolderSymbol FolderSymbol) {
        return foldersInternal().folderPop3(FolderSymbol);
    }

    public List<String> nonsystemFids() {
        return foldersInternal().nonsystemFids();
    }
    public Map<String, Folder> folders() {
        return foldersInternal().folders();
    }

    public Integer count(String fid) {
        return foldersInternal().count(fid);
    }

    public Integer newCount(String fid) {
        return foldersInternal().newCount(fid);
    }

    public String FolderSymbolByFid(String fid) {
        return foldersInternal().symbolName(fid);
    }

    public String inboxFID() {
        return fidByFolderSymbol(FolderSymbol.INBOX);
    }

    public String defaultFID() {
        return inboxFID();
    }

    public String sentFID() {
        return fidByFolderSymbol(FolderSymbol.SENT);
    }

    public String outgoingFID() {
        return fidByFolderSymbol(FolderSymbol.OUTBOX);
    }

    public String draftFID() {
        return fidByFolderSymbol(FolderSymbol.DRAFT);
    }

    public String templateFID() {
        return fidByFolderSymbol(FolderSymbol.TEMPLATE);
    }

    public String spamFID() {
        return fidByFolderSymbol(FolderSymbol.SPAM);
    }

    public String deletedFID() {
        return fidByFolderSymbol(FolderSymbol.TRASH);
    }
}
