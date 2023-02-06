package ru.yandex.autotests.innerpochta.imap.consts.folders;

import java.util.List;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.02.14
 * Time: 20:52
 */
public class Folders {

    public static final String INBOX = "INBOX";
    public static final String EN = "en";
    public static final String RU = "ru";

    private Folders() {
    }

    public static List<String> systemFoldersEng() {
        return systemFolders(EN).getSystemFolders();
    }

    public static List<String> systemFoldersRus() {
        return systemFolders(RU).getSystemFolders();
    }

}
