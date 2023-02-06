package ru.yandex.autotests.innerpochta.util;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 19:45
 */

public interface MailConst {

    String INBOX_RU = "Входящие";
    String UNREAD_RU = "Непрочитанные";
    String SPAM_RU = "Спам";
    String SENT_RU = "Отправленные";
    String OUTBOX_RU = "Исходящие";
    String ARCHIVE_RU = "Архив";
    String IMPORTANT_LABEL_NAME_RU = "Важные";
    String UNREAD_LABEL_NAME_RU = "Непрочитанные";
    String FOR_PREFIX = "/for/";
    String FOR = "for";
    String MAIL_BASE_URL = "https://mail.yandex.ru";
    String CORP_BASE_URL = "https://mail.yandex-team.ru";
    String CORP_URL_PART = "yandex-team";
    String DOMAIN_YARU = "@ya.ru";
    String DOMAIN_YANDEXRU = "@yandex.ru";
    String DEV_NULL_EMAIL = "testbotauto@yandex.ru";
    String DEV_NULL_EMAIL_2 = "yndx-oleshko-c3drej@yandex.ru";
    String MAIL_URL_WITHOUT_DOMAIN = "mail.yandex.";
    String LITE_URL = ".yandex.ru/lite/";
    String PDD_URL_FOR = "https://mail.yandex.ru/for/";
    String PDD_URL = "https://mail.yandex.ru/?pdd_domain=";
    String PASSPORT_URL = "passport.yandex.";
    String FORWARD_URL = "?oper=forward";
    String FORWARD_PREFIX = "Fwd: ";
    String REPLY_PREFIX = "Re: ";
    String REPLY_URL = "?oper=reply";
    String AD_DEBUG = "?ad_debug=1";
    String AD_CRYPROX = "?ad_debug=1&adblock_enabled=1";
    String PASSPORT_AUTH_URL = "passport.yandex.ru/auth";
    String CLASS_GET_PARAM = "/?message-widget=confirm-classification";
    String MORDA_URL = "https://yandex.ru/";
    String DISK_URL = "https://disk.yandex.ru/mail";
    String PRINT_URL = "mail.yandex.ru/u2709/print";
    String NEWS_TAB_RU = "Рассылки";
    String SOCIAL_TAB_RU = "Социальные сети";
    String NEWS_TAB = "News";
    String SOCIAL_TAB = "Social";
    String USER_WITH_AVATAR_EMAIL = "robbitter-3067925075@yandex.ru";
    String YA_DISK_URL = "https://disk.yandex.ru";

    // пользователь для настройки сборщика
    String MAIL_COLLECTOR = "ns-collectorforsearch@yandex.ru";
    String PASS_COLLECTOR = "testQA1520";
    String SERVER_COLLECTOR = "imap.yandex.ru";

    //Xiva
    int XIVA_TIMEOUT = 60000;

    //Названия аттачей в папке resources
    String IMAGE_ATTACHMENT = "attach.png";
    String WIDE_IMAGE_ATTACHMENT = "wide.png";
    String PDF_ATTACHMENT = "doc.pdf";
    String EXCEL_ATTACHMENT = "test excel.xlsx";
    String WORD_ATTACHMENT = "test word.docx";
    String TXT_ATTACHMENT = "test txt.txt";
    String BIG_SIZE = "big_size.exe";
    String HEAVY_IMAGE = "heavy_image.png";
    String WRONG_EXTENSION = "extension.lol";
    String LONG_NAME = "offer with the longest name world has ever seen.pdf";
    String SPECIFIC_FORM = "specific form.png";
    String NOTIFICATION_FILE_NAME = "notification.eml";

    String LEFT_PANEL_FULL_SIZE = "220";
    String LEFT_PANEL_COMPACT_SIZE = "60";
    String LEFT_PANEL_HALF_COMPACT_SIZE = "160";

    String REMIND_LABEL_5DAYS = "Напомнить через 5 дней";

    //юзеры, которым больше 14 дней
    String OLD_USER_TAG = "default1";
    String PDD_USER_TAG = "pdduser";
    String PDD_FREE_USER_TAG = "pddfreeuser";
    String DISK_USER_TAG = "disk_tag";
    String COLLECTOR = "with_collector";
    String DISK_BIG = "disk_big";
    String MAIL360_PAID = "mail360_subscription";
    String SID_123 = "sid_123";
    String SAVE_TO_DISK_TAG = "save_to_disk";
}
