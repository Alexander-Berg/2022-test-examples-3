package ru.yandex.autotests.innerpochta.imap.consts.folders;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 07.05.14
 * Time: 20:16
 */
public enum FoldersRu {
    INBOX(systemFolders(Folders.RU).getInbox()),
    SENT(systemFolders(Folders.RU).getSent()),
    DELETED(systemFolders(Folders.RU).getDeleted()),
    SPAM(systemFolders(Folders.RU).getSpam()),
    DRAFT(systemFolders(Folders.RU).getDrafts()),
    OUTGOING(systemFolders(Folders.RU).getOutgoing());

    private String name;

    private FoldersRu(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
