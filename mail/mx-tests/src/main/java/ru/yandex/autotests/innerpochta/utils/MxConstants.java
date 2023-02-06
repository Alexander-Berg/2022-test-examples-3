package ru.yandex.autotests.innerpochta.utils;

public class MxConstants {

    public static final String SPAM_TEXT = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    public static final String VIRUS_TEXT = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    public static final String PERSONAL_SPAM_TEXT = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-PERSONAL-ANTI-UBE-TEST-EMAIL*C.34X";

    public static final String PG_FOLDER_DEFAULT = "Inbox";
    public static final String PG_FOLDER_DRAFT = "Drafts";
    public static final String PG_FOLDER_SPAM = "Spam";
    public static final String PG_FOLDER_DELETED = "Trash";
    public static final String PG_FOLDER_DELAYED = "Outbox";
    public static final String PG_FOLDER_OUTBOX = "Sent";

    //нужно для оРакловых кейсов еще.
    public static final String FOLDER_DEFAULT = "Входящие";
    public static final String FOLDER_DRAFT = "Черновики";
    public static final String FOLDER_SPAM = "Спам";
    public static final String FOLDER_DELETED = "Удаленные";
    public static final String FOLDER_OUTBOX = "Отправленные";
    public static final String FOLDER_DELAYED = "Исходящие";


    public static final String OK_FASTSRV_SERVER_RESPONSE = "250 2.0.0 Ok";
    public static final String UNKNOWN_USER_FASTSRV_RESPONSE = "554 5.1.1 Unknown user;";
    public static final String UNKNOWN_ERROR_FASTSRV_RESPONSE = "554 5.3.0";
    public static final String NO_SUCH_USER_YET_FASTSRV_RESPONSE = "554 5.2.1 No such user yet!";
    public static final String OK_NWSMTP_SERVER_RESPONSE = "250 2.0.0 Ok: queued on ";
    public static final String OK_VIRUS_NWSMTP_SERVER_RESPONSE = "250 2.0.0 Message infected by virus: queued on ";
    public static final String TEMP_FAIL_NWSMTP_RESPONSE =
            "451 4.7.1 Sorry, the service is currently unavailable. Please come back later.";
    public static final String SENDER_REJECT_NWSMTP_RESPONSE =
            "553 5.7.1 Sender address rejected: not owned by auth user.";
    public static final String SPAM_REJECT_NWSMTP_RESPONSE_2 =
            "554 5.7.1 Message rejected under suspicion of SPAM; https://ya.cc/1IrBc";
    public static final String SPAM_REJECT_NWSMTP_RESPONSE_1 =
            "554 5.7.1 Message rejected under suspicion of SPAM; https://ya.cc/1IrBc";
    public static final String SPAM_REJECT_NWSMTP_RESPONSE =
            "554 5.7.1 Message rejected under suspicion of SPAM; " +
                    "https://yandex.ru/support/mail/spam/honest-mailers.xml";
    public static final String VIRUS_REJECT_NWSMTP_RESPONSE = "554 5.7.1 Message infected by virus ";
    public static final String NEED_FULLY_QUALIFIED_ADDRESS_NWSMTP_RESPONSE =
            "504 5.5.2 Recipient address rejected: need fully-qualified address";
    public static final String NO_SUCH_USER_NWSMTP_RESPONSE = "550 5.7.1 No such user!";
    public static final String UNKNOWN_USER_NWSMTP_RESPONSE = "554 5.1.1 Unknown user";
    public static final String BAD_RCPT_ADDRESS_SYNTAX_NWSMTP_RESPONSE = "550 5.1.3 Bad recipient address syntax ";
    public static final String BAD_RCPT_ADDRESS_SYNTAX_NWSMTP_RESPONSE_2 = "501 5.1.3 Bad recipient address syntax";
    public static final String MB_SIZE_EXCCEEDED_NWSMTP_RESPONSE = "552 5.2.2 Mailbox size limit exceeded";


    public enum SymbolLabels {
        DELETED_LABEL("deleted_label", "FAKE_DELETED_LBL"),
        SEEN_LABEL("seen_label", "FAKE_SEEN_LBL"),
        MUTE_LABEL("mute_label", ""),     //added label 2420000002114424270: (ignore_thisthread:mark) type: threadWide
        PINNED_LABEL("pinned_label", ""),     //added label 2420000002114410196: (_pinned_) type: system
        FORW_LABEL("forwarded_label", "FAKE_FORWARDED_LBL"),
        ANSW_LABEL("answered_label", "FAKE_ANSWERED_LBL"),
        RECENT_LABEL("recent_label", "FAKE_RECENT_LBL"),
        DRAFT_LABEL("draft_label", "FAKE_DRAFT_LBL"),
        IMPORTANT_LABEL("important_label", ""), //added label 2420000002114410198: (priority_high) type: system
        POSTMASTER_LABEL("postmaster_label", "FAKE_POSTMASTER"),
        MULCA_SHARED_LABEL("mulcaShared_label", ""),
        IMAP_LABEL("imap_label", "a"),
        APPEND_LABEL("append_label", "a"),
        COPY_LABEL("copy_label", "a"),
        SPAM_LABEL("spam_label", "FAKE_SPAM_LBL"),
        REMIND_NO_ANSWER_LABEL("remindNoAnswer_label", ""),   //added label 2420000002114410199: (remindme_threadabout:mark) type: system
        NOTIFY_NO_ANSWER_LABEL("notifyNoAnswer_label", ""),   // added label 2420000002114519292: (SystMetkaWJDT:NOTIFY) type: system
        REMIND_MSG_LABEL("remindMessage_label", ""),    //added label 2420000002114519346: (SystMetka:remindme_about_message) type: system
        ATTACHED_LABEL("attached_label", "FAKE_ATTACHED_LBL"),
        HAS_USER_LABELS_LABEL("hasUserLabels_label", "FAKE_HAS_USER_LABELS_LBL"),
        NOTIFY_MSG_LABEL("notifyMessage_label", ""),
        DELAY_LABEL("delayed_message", "");    //added label 2420000002115087904: (delayed_message) type: system

        private String name;
        private String wmiName;

        private SymbolLabels(String name, String wmiName) {
            this.name = name;
            this.wmiName = wmiName;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

