package ru.yandex.autotests.innerpochta.utils;

import java.util.HashMap;
import java.util.regex.Pattern;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.*;

public class HintData {
    public static final String X_YANDEX_HINT = "X-Yandex-Hint";
    public static final String STID_LOG_SIGN = "mulcaid=";
    public static final Pattern STID_LOG_PATTERN = Pattern.compile(" mulcaid=([\\d.a-zE:]+) ");

    /**
     * Конвертирует обычный (т.е. с разделителем '/') folderPath header-а X-Yandex-Hint
     * в folder_path пригодный для поиска с помощью WMI
     * Примеры:
     * "\Drafts/sdf" должен быть сконвертирован в "Черновики|sdf"
     * "Drafts/sdf" должен быть сконвертирован в "Drafts|sdf"
     * "Drafts/sdf/ggg" должен быть сконвертирован в "Drafts|sdf|ggg"
     * "" должен быть сконвертирован в "Входящие"
     *
     * @param hintFolderPath - обычный folderPath, который указывается в параметре folder_path header-а X-Yandex-Hint
     * @return folder_path пригодный для поиска с помощью WMI
     */
    public static String convertXYandexHintStandardFolderPathToWmiFolderPath(String hintFolderPath) {
        if (isEmpty(hintFolderPath)) {
            return PG_FOLDER_DEFAULT;
        }
        String formWmiFolderPath = hintFolderPath;
        for (String systemFolderPath : PATH_AND_NAME_FOLDER_MAP.keySet()) {
            formWmiFolderPath = formWmiFolderPath.replace(systemFolderPath, PATH_AND_NAME_FOLDER_MAP.get(systemFolderPath));
        }
        return formWmiFolderPath.replace("/", "|");
    }

    private static final HashMap<String, String> PATH_AND_NAME_FOLDER_MAP = new HashMap<String, String>() {{
        put("\\Inbox", PG_FOLDER_DEFAULT);
        put("\\Sent", PG_FOLDER_OUTBOX);
        put("\\Spam", PG_FOLDER_SPAM);
        put("\\Drafts", PG_FOLDER_DRAFT);
        put("\\Trash", PG_FOLDER_DELETED);
        put("\\Archive", "archive");
    }};

    private static final HashMap<String, String> PATH_AND_NAME_FOLDER_MAP_ORA = new HashMap<String, String>() {{
        put("\\Inbox", FOLDER_DEFAULT);
        put("\\Sent", FOLDER_OUTBOX);
        put("\\Spam", FOLDER_SPAM);
        put("\\Drafts", FOLDER_DRAFT);
        put("\\Trash", FOLDER_DELETED);
        put("\\Archive", "archive");
    }};

    public static class XYandexHintValue {
        //http://wiki.yandex-team.ru/Users/ctor/xyandexhint
        private String currentHintValue;

        private XYandexHintValue(String currentHintValue) {
            this.currentHintValue = currentHintValue;
        }

        public static XYandexHintValue createHintValue() {
            return new XYandexHintValue("");
        }

        public static XYandexHintValue createHintValue(String currentHintValue) {
            return new XYandexHintValue(currentHintValue);
        }

        public XYandexHintValue addCopyToInbox(String flag) {
            currentHintValue = currentHintValue.concat("copy_to_inbox=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addBcc(String flag) {
            currentHintValue = currentHintValue.concat("bcc=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addHdrDate(long date) {
            currentHintValue = currentHintValue.concat("hdr_date=" + date + "\n");
            return this;
        }

        public XYandexHintValue addRcvDate(long date) {
            currentHintValue = currentHintValue.concat("received_date=" + date + "\n");
            return this;
        }

        public XYandexHintValue addFid(String fid) {
            currentHintValue = currentHintValue.concat("fid=" + fid + "\n");
            return this;
        }

        public XYandexHintValue addFolder(String folder) {
            currentHintValue = currentHintValue.concat("folder=" + folder + "\n");
            return this;
        }

        public XYandexHintValue addFolderPath(String folderPath) {
            currentHintValue = currentHintValue.concat("folder_path=" + folderPath + "\n");
            return this;
        }

        public XYandexHintValue addFolderSpamPath(String folderPath) {
            currentHintValue = currentHintValue.concat("folder_spam_path=" + folderPath + "\n");
            return this;
        }

        public XYandexHintValue addFolderPathDelim(String folderPathDelimiter) {
            currentHintValue = currentHintValue.concat("folder_path_delim=" + folderPathDelimiter + "\n");
            return this;
        }


        public XYandexHintValue addLid(String lid) {
            currentHintValue = currentHintValue.concat("lid=" + lid + "\n");
            return this;
        }

        public XYandexHintValue addLabel(String label) {
            currentHintValue = currentHintValue.concat("label=" + label + "\n");
            return this;
        }

        public XYandexHintValue addUserLabel(String label) {
            currentHintValue = currentHintValue.concat("userlabel=" + label + "\n");
            return this;
        }

        public XYandexHintValue addImapLabel(String label) {
            currentHintValue = currentHintValue.concat("imaplabel=" + label + "\n");
            return this;
        }

        public XYandexHintValue addEmail(String email) {
            currentHintValue = currentHintValue.concat("email=" + email + "\n");
            return this;
        }

        public XYandexHintValue addSkipLoopPrevention(String email) {
            currentHintValue = currentHintValue.concat("skip_loop_prevention=" + email + "\n");
            return this;
        }

        public XYandexHintValue addFilters(String flag) {
            currentHintValue = currentHintValue.concat("filters=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addForward(String flag) {
            currentHintValue = currentHintValue.concat("forward=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addNotify(String flag) {
            currentHintValue = currentHintValue.concat("notify=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addSourceStid(String stid) {
            currentHintValue = currentHintValue.concat("source_stid=" + stid + "\n");
            return this;
        }

        public XYandexHintValue addMixed(long mixed) {
            currentHintValue = currentHintValue.concat("mixed=" + mixed + "\n");
            return this;
        }

        public XYandexHintValue addImap(String flag) {
            currentHintValue = currentHintValue.concat("imap=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addMid(String flag) {
            currentHintValue = currentHintValue.concat("mid=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addHost(String flag) {
            currentHintValue = currentHintValue.concat("host=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addFinalHeadersLen(String flag) {
            currentHintValue = currentHintValue.concat("final_headers_len=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addBodyMd5(String flag) {
            currentHintValue = currentHintValue.concat("body_md5=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addSaveToSent(String flag) {
            currentHintValue = currentHintValue.concat("save_to_sent=" + flag + "\n");
            return this;
        }

        public XYandexHintValue addExternalImapId(String flag) {
            currentHintValue = currentHintValue.concat("external_imap_id=" + flag + "\n");
            return this;
        }

        public String encode() {
            return encodeBase64String(currentHintValue.getBytes());
        }

        @Override
        public String toString() {
            return currentHintValue.replaceAll("\n", " ");
        }
    }

}

