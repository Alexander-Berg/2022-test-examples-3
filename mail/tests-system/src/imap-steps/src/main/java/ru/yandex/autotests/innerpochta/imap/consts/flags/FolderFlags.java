package ru.yandex.autotests.innerpochta.imap.consts.flags;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 17:53
 */
public enum FolderFlags {
    UNMARKED("\\Unmarked"),
    MARKED("\\Marked"),
    HAS_CHILDREN("\\HasChildren"),
    HAS_NO_CHILDREN("\\HasNoChildren"),
    NO_INFERIORS("\\NoInferiors"),
    NO_SELECT("\\Noselect"),
    SENT("\\Sent"),
    JUNK("\\Junk"),
    TRASH("\\Trash"),
    DRAFTS("\\Drafts"),
    DELETED("\\Deleted");

    private String value;

    private FolderFlags(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
