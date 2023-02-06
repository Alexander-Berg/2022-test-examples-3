package ru.yandex.autotests.innerpochta.util.handlers;

/**
 * Created by mabelpines on 13.05.15.
 */
public class FoldersConstants {
    //Handlers:
    public static final String HANDLER_FOLDERS = "folders";
    public static final String HANDLER_DO_FOLDERS_ADD = "do-folders-add";
    public static final String HANDLER_DO_FOLDER_REMOVE = "do-folder-remove";
    public static final String HANDLER_DO_FOLDER_CLEAR = "do-folder-clear";
    public static final String HANDLER_DO_FOLDER_MOVE = "do-folder-move";
    public static final String HANDLER_DO_SET_SYMBOL = "do-folder-set-symbol";

    //Do-folders-add, folders, do-folder-remove, do-folder-move  params
    public static final String FOLDERS_ADD_PARAM_FOLDER_NAME = "folder_name.0";
    public static final String FOLDERS_PARAM_FID = "fid.0";
    public static final String FOLDERS_PARAM_PARENT_ID = "parent_id.0";

    //Do-folder-clear params
    public static final String FOLDERS_CLEAR_PARAM_FID = "fid.0";
    public static final String FOLDERS_CLEAR_PARAM_METHOD = "method.0";
    public static final String FOLDERS_CLEAR_PARAM_PURGE = "purge";
    public static final String FOLDERS_CLEAR_PARAM_CLEAR = "clear";
    public static final String FOLDERS_CLEAR_PARAM_SELECTED = "clearSelected";
    public static final String FOLDERS_CLEAR_PARAM_OLD_F = "old_f.0";
    public static final String FOLDERS_CLEAR_OLDER_THAN_1WEEK = "7";
    public static final String FOLDERS_CLEAR_OLDER_THAN_2WEEKS = "14";
    public static final String FOLDERS_CLEAR_OLDER_THAN_1MONTHS = "30";
    public static final String FOLDERS_CLEAR_OLDER_THAN_3MONTHS = "60";

    //Folders aliases
    public static final String FOLDERS_TRASH = "trash";
    public static final String FOLDERS_INBOX = "inbox";

    //do-folder-set-symbol params
    public static final String FOLDERS_SYMBOL = "symbol.0";
}
