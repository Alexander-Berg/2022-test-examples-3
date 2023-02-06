package ru.yandex.autotests.innerpochta.imap.consts.folders;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 07.05.14
 * Time: 20:16
 */
public enum FoldersEn {
    INBOX(systemFolders(Folders.EN).getInbox()),
    SENT(systemFolders(Folders.EN).getSent()),
    DELETED(systemFolders(Folders.EN).getDeleted()),
    SPAM(systemFolders(Folders.EN).getSpam()),
    DRAFT(systemFolders(Folders.EN).getDrafts()),
    OUTGOING(systemFolders(Folders.EN).getOutgoing());

    private String name;

    private FoldersEn(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
