package ru.yandex.autotests.innerpochta.imap.consts.flags;

import org.hamcrest.Matcher;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.structures.ListItem;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_NO_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.NO_INFERIORS;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;

public class SystemFolderFlags {

    private SystemFolderFlags() {
    }

    public static Matcher<ListItem> getINBOXItem() {
        return listItem(SystemFlags.DEFAULT_INBOX_ITEM.listItem());
    }

    public static Matcher<ListItem> getSentItem() {
        return listItem(SystemFlags.SENT_ITEM.listItem());
    }

    public static Matcher<ListItem> getTrashItem() {
        return listItem(SystemFlags.TRASH_ITEM.listItem());
    }

    public static Matcher<ListItem> getSpamItem() {
        return listItem(SystemFlags.SPAM_ITEM.listItem());
    }

    public static Matcher<ListItem> getDraftsItem() {
        return listItem(SystemFlags.DRAFTS_ITEM.listItem());
    }

    public static Matcher<ListItem> getOutgoingItem() {
        return listItem(SystemFlags.OUTGOING_FLAGS.listItem());
    }

    public static Matcher<ListItem> getSystemItem(String systemFolder) {
        for (SystemFlags flags : SystemFlags.values()) {
            if (flags.listItem().getName().equals(systemFolder)) {
                return listItem(flags.listItem());
            }
        }
        return null;
    }

    public static ListItem getSystemListItem(String systemFolder) {
        for (SystemFlags flags : SystemFlags.values()) {
            if (flags.listItem().getName().equals(systemFolder)) {
                return flags.listItem();
            }
        }
        return null;
    }

    public static enum SystemFlags {
        DEFAULT_INBOX_ITEM(Folders.INBOX, new ListItem("|", Folders.INBOX, NO_INFERIORS.value())),
        SENT_ITEM(systemFolders().getSent(), new ListItem("|", systemFolders().getSent(), HAS_NO_CHILDREN.value(), FolderFlags.SENT.value())),
        TRASH_ITEM(systemFolders().getDeleted(), new ListItem("|", systemFolders().getDeleted(), HAS_NO_CHILDREN.value(), FolderFlags.TRASH.value())),
        SPAM_ITEM(systemFolders().getSpam(), new ListItem("|", systemFolders().getSpam(), HAS_NO_CHILDREN.value(), FolderFlags.JUNK.value())),
        DRAFTS_ITEM(systemFolders().getDrafts(), new ListItem("|", systemFolders().getDrafts(), HAS_NO_CHILDREN.value(), FolderFlags.DRAFTS.value())),
        OUTGOING_FLAGS(systemFolders().getOutgoing(), new ListItem("|", systemFolders().getOutgoing(), HAS_NO_CHILDREN.value()));

        private ListItem item;
        private String folder;

        private SystemFlags(String folderName, ListItem listItem) {
            this.item = listItem;
            this.folder = folderName;
        }

        public ListItem listItem() {
            return item.clone();
        }
    }
}
