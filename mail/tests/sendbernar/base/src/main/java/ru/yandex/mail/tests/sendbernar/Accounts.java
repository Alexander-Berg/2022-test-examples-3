package ru.yandex.mail.tests.sendbernar;

import jersey.repackaged.com.google.common.collect.ImmutableMap;
import ru.yandex.mail.common.credentials.Account;
import ru.yandex.mail.common.credentials.AccountWithScope;

import static ru.yandex.mail.common.credentials.Account.domain;
import static ru.yandex.mail.common.credentials.Account.login;
import static ru.yandex.mail.common.credentials.Account.password;
import static ru.yandex.mail.common.credentials.Account.uid;
import static ru.yandex.mail.common.properties.Scopes.TESTING;
import static ru.yandex.mail.common.properties.Scopes.PRODUCTION;

public class Accounts {
    static AccountWithScope xeno = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.xeno"),   password("simple123456"),   uid("4012523648"), domain("mail.ru")),
            PRODUCTION,          Account.of(login("yndx.xeno"),   password("simple123456"),   uid("647137773"),  domain("mail.ru"))
    ));

    static AccountWithScope delayedMailSend = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.delayedmailsend"),   password("simple123"),      uid("4012400666")),
            PRODUCTION,          Account.of(login("super.qweqwa-pg"),        password("simple123456"),   uid("320179405"))
    ));

    static AccountWithScope undoMailSend = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.undomailsend"),   password("simple123"),      uid("4016648836")),
            PRODUCTION,          Account.of(login("yndx.undomailsend"),   password("simple123456"),   uid("748978673"))
    ));

    static AccountWithScope attaches = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.inlineattasend"),   password("simple123"),      uid("4012400634")),
            PRODUCTION,          Account.of(login("inlineattasend-pg"),     password("simple123456"),   uid("320176059"))
    ));

    static AccountWithScope bcc = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.bccmailsend"),   password("simple123"),      uid("4012400642")),
            PRODUCTION,          Account.of(login("bccmailsend-pg"),     password("simple123456"),   uid("320177700"))
    ));

    static AccountWithScope bccReceiverTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.dotnamed"),   password("simple123"),      uid("4012524660")),
            PRODUCTION,          Account.of(login("dot.lamer-pg"),    password("simple123456"),   uid("320177555"))
    ));

    static AccountWithScope bccReceiverBcc = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.bccmailsendbcc"),   password("simple123"),      uid("4012524662")),
            PRODUCTION,          Account.of(login("bccmailsendbcc-pg"),     password("simple123456"),   uid("320177719"))
    ));

    static AccountWithScope cc = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.mailsendcc"),   password("simple123"),      uid("4012400658")),
            PRODUCTION,          Account.of(login("mailsendcc-pg"),     password("simple123456"),   uid("320178506"))
    ));

    static AccountWithScope ccReceiverTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.adminkapdd"),   password("simple123"),   uid("4012523456")),
            PRODUCTION,          Account.of(login("vicdev"),            password("testqa"),      uid("1130000009785926"), domain("админкапдд.рф"))
    ));

    static AccountWithScope digitLoginTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.digitalloginto"),   password("simple123"),   uid("4012522534")),
            PRODUCTION,          Account.of(login("aliasowitch"),           password("testqa"),      uid("238281545"))
    ));

    static AccountWithScope digitLogin = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.digitlogin"),   password("simple123"),      uid("4012400670")),
            PRODUCTION,          Account.of(login("aliasowitch-pg"),    password("simple123456"),   uid("320178675"))
    ));

    static AccountWithScope headers = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.headerstest"),     password("simple123"),      uid("4012400676")),
            PRODUCTION,          Account.of(login("ya-autotest-303-pg"),   password("simple123456"),   uid("320177592"))
    ));

    static AccountWithScope headersFrom = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.sendfrommetest"),   password("simple123"),      uid("4012522550")),
            PRODUCTION,          Account.of(login("sendfrommetest-pg"),     password("simple123456"),   uid("320177612"))
    ));

    static AccountWithScope labels = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.mailsendreply"),   password("simple123"),      uid("4012400680")),
            PRODUCTION,          Account.of(login("mailsendreply-pg"),     password("simple123456"),   uid("320178988"))
    ));

    static AccountWithScope labelsTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndxakita"),       password("simple123456"),   uid("4011538550")),
            PRODUCTION,          Account.of(login("mailsendreply"),   password("testqa"),         uid("259689622"))
    ));

    static AccountWithScope listUnsubscribe = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.listunsubscribetest"),   password("simple123"),      uid("4012400686")),
            PRODUCTION,          Account.of(login("yndxlistunsubscribe"),        password("simple123456"),   uid("522094138"))
    ));

    static AccountWithScope markAs = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.mailsendmops"),   password("simple123"),   uid("4012400690")),
            PRODUCTION,          Account.of(login("mailsendmops"),        password("testqa"),      uid("450256842"))
    ));

    static AccountWithScope markAsTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.mailsendreply"),   password("simple123"),      uid("4012400680")),
            PRODUCTION,          Account.of(login("mailsendreply-pg"),     password("simple123456"),   uid("320178988"))
    ));

    static AccountWithScope noAnswer = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.noanswerreminder"),   password("simple123"),   uid("4012400696")),
            PRODUCTION,          Account.of(login("remindMeTest1"),           password("testqa"),      uid("246364876"))
    ));

    static AccountWithScope noAnswerTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.NoAnswerReminderReciever"),   password("simple123"),   uid("4012523472")),
            PRODUCTION,          Account.of(login("remindReciever1"),                 password("testqa"),      uid("246365072"))
    ));

    static AccountWithScope noAnswerLang = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.remindme"),     password("simple123"),      uid("4012400696")),
            PRODUCTION,          Account.of(login("remindmetest-pg"),   password("simple123456"),   uid("320178797"))
    ));

    static AccountWithScope noAnswerLangTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.remindreciever"),   password("simple123"),   uid("4012523472")),
            PRODUCTION,          Account.of(login("yndx.remindreciever"),   password("simple123"),   uid("655832771"))
    ));

    static AccountWithScope otherMailSystem = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.sendtoenemymailtest"),   password("simple123"),      uid("4012400708")),
            PRODUCTION,          Account.of(login("foregnmailtest-pg"),          password("simple123456"),   uid("320179468"))
    ));

    static AccountWithScope pdd = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.pddusual"),   password("simple123"),   uid("4012400712")),
            PRODUCTION,          Account.of(login("lanwen"),          password("simple123"),   uid("1130000002644591"), domain("kida-lo-vo.name"))
    ));

    static AccountWithScope pddTo = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.adminkapdd"),   password("simple123"),   uid("4012523456")),
            PRODUCTION,          Account.of(login("vicdev"),            password("testqa"),      uid("1130000009785926"), domain("админкапдд.рф"))
    ));

    static AccountWithScope remind = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.remindmessagetest"),   password("simple123"),   uid("4012400716")),
            PRODUCTION,          Account.of(login("remander.olo"),             password("testqa123"),   uid("332191023"))
    ));

    static AccountWithScope saveDraft = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.savedraft"),   password("simple123"),      uid("4012400728")),
            PRODUCTION,          Account.of(login("savedrafter"),      password("simple123456"),   uid("424816870"))
    ));

    static AccountWithScope saveTemplate = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.savetemplate"),   password("simple123"),      uid("4012400732")),
            PRODUCTION,          Account.of(login("yndxsavetemplate"),    password("simple123456"),   uid("479158379"))
    ));

    static AccountWithScope sendMessage = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.group1"),        password("simple123"),      uid("4012400740")),
            PRODUCTION,          Account.of(login("mdb350testbox-pg"),   password("simple123456"),   uid("320175832"))
    ));

    static AccountWithScope sendMessageWithoutDots = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndxsendmsgwithoutdots"),   password("simple123"),   uid("4018268022")),
            PRODUCTION,          Account.of(login("labelmopstest"),            password("testqa"),      uid("292732317"))
    ));

    static AccountWithScope virusAndSpam = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.group1"),        password("simple123"),      uid("4012400740")),
            PRODUCTION,          Account.of(login("mdb350testbox-pg"),   password("simple123456"),   uid("320175832"))
    ));

    static AccountWithScope tskvSendbernar = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.tskvsendbernartest"),   password("simple123"),   uid("4012400754")),
            PRODUCTION,          Account.of(login("tskv-sendbernar-test"),      password("testqa"),      uid("526904936"))
    ));

    static AccountWithScope writeAttachment = new AccountWithScope(ImmutableMap.of(
            TESTING,             Account.of(login("yndx.writeattachmentsendbernar"),   password("simple123"),      uid("4012400758")),
            PRODUCTION,          Account.of(login("writeattachmentsendbernar-pg"),     password("simple123456"),   uid("499261334"))
    ));
}
