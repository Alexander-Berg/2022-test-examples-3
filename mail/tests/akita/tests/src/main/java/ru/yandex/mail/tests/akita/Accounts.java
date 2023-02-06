package ru.yandex.mail.tests.akita;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

import ru.yandex.mail.common.credentials.Account;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.BbResponse;

import static ru.yandex.mail.common.credentials.Account.login;
import static ru.yandex.mail.common.credentials.Account.password;
import static ru.yandex.mail.common.credentials.Account.uid;
import static ru.yandex.mail.common.properties.Scopes.DEVPACK;
import static ru.yandex.mail.common.properties.Scopes.INTRANET_PRODUCTION;
import static ru.yandex.mail.common.properties.Scopes.PRODUCTION;
import static ru.yandex.mail.common.properties.Scopes.TESTING;

class Accounts {
    public static AccountWithScope yplus = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.plMail"),    password(""),             uid("4012395410"),
                    BbResponse.from("fakebb/yndx.plMail.xml")),
            TESTING,             Account.of(login("yndx.plMail"),    password("simple123"),    uid("4012395410"))
    ));

    public static AccountWithScope authTest = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndxakita"),      password("simple123456"), uid("4011538550"),
                    BbResponse.from("fakebb/yndxakita.xml")),
            TESTING,             Account.of(login("yndxakita"),      password("simple123456"), uid("4011538550")),
            PRODUCTION,          Account.of(login("yndxakita"),      password("simple123456"), uid("527630846")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest1"),    password("testQ@123!"),   uid("1120000000010003"))
    ));

    public static AccountWithScope noMailboxTest = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.nomailbox"), password("simple123456"), uid("4011751934"),
                    BbResponse.from("fakebb/yndx.nomailbox.xml")),
            TESTING,             Account.of(login("yndx.nomailbox"), password("simple123456"), uid("4011751934")),
            PRODUCTION,          Account.of(login("yndx.nomailbox"), password("simple123456"), uid("638320877")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest2"),    password("testQ@123!"),   uid("1120000000010005"))
    ));

    public static AccountWithScope multiCookieAccount1 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndxnomailbox"),  password("simple123456"), uid("4011751536")),
            TESTING,             Account.of(login("yndxnomailbox"),  password("simple123456"), uid("4011751536")),
            PRODUCTION,          Account.of(login("yndxnomailbox"),  password("simple123456"), uid("641148450")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest3"),    password("testQ@123!"),   uid("1120000000010006"))
    ));

    public static AccountWithScope multiCookieAccount2 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndxakita2"),     password("simple123456"), uid("4011752296")),
            TESTING,             Account.of(login("yndxakita2"),     password("simple123456"), uid("4011752296")),
            PRODUCTION,          Account.of(login("yndxakita2"),     password("simple123456"), uid("527632366")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest4"),    password("testQ@123!"),   uid("1120000000010004"))
    ));

    public static AccountWithScope multiCookieAccount3 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi3"),    password("simple123456"), uid("4011752282")),
            TESTING,             Account.of(login("yndx.multi3"),    password("simple123456"), uid("4011752282")),
            PRODUCTION,          Account.of(login("yndx.multi3"),    password("simple123456"), uid("641148641")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest5"),    password("testQ@123!"),   uid("1120000000010007"))
    ));

    public static AccountWithScope multiCookieAccount4 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi4"),    password("simple123456"), uid("4011752288")),
            TESTING,             Account.of(login("yndx.multi4"),    password("simple123456"), uid("4011752288")),
            PRODUCTION,          Account.of(login("yndx.multi4"),    password("simple123456"), uid("641148908")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest6"),    password("testQ@123!"),   uid("1120000000010009"))
    ));

    public static AccountWithScope multiCookieAccount5 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi5"),    password("simple123456"), uid("4011752290")),
            TESTING,             Account.of(login("yndx.multi5"),    password("simple123456"), uid("4011752290")),
            PRODUCTION,          Account.of(login("yndx.multi5"),    password("simple123456"), uid("641149066")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest7"),    password("testQ@123!"),   uid("1120000000010010"))
    ));

    public static AccountWithScope multiCookieAccount6 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi6"),    password("simple123456"), uid("4011752292")),
            TESTING,             Account.of(login("yndx.multi6"),    password("simple123456"), uid("4011752292")),
            PRODUCTION,          Account.of(login("yndx.multi6"),    password("simple123456"), uid("641149227")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest8"),    password("testQ@123!"),   uid("1120000000010011"))
    ));

    public static AccountWithScope multiCookieAccount10 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi10"),   password("simple123456"), uid("4014140060")),
            TESTING,             Account.of(login("yndx.multi10"),   password("simple123456"), uid("4014140060")),
            PRODUCTION,          Account.of(login("yndx.multi10"),   password("simple123456"), uid("686431204")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest10"),   password("testQ@123!"),   uid("1120000000010008"))
    ));

    public static AccountWithScope multiCookieAccount11 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi11"),   password("simple123456"), uid("4014140044")),
            TESTING,             Account.of(login("yndx.multi11"),   password("simple123456"), uid("4014140044")),
            PRODUCTION,          Account.of(login("yndx.multi11"),   password("simple123456"), uid("686427669")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest11"),   password("testQ@123!"),   uid("1120000000010015"))
    ));

    public static AccountWithScope multiCookieAccount12 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi12"),   password("simple123456"), uid("4014140040")),
            TESTING,             Account.of(login("yndx.multi12"),   password("simple123456"), uid("4014140040")),
            PRODUCTION,          Account.of(login("yndx.multi12"),   password("simple123456"), uid("686425997")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest12"),   password("testQ@123!"),   uid("1120000000010013"))
    ));

    public static AccountWithScope multiCookieAccount13 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi15"),   password("simple123456"), uid("4014139724")),
            TESTING,             Account.of(login("yndx.multi15"),   password("simple123456"), uid("4014139724")),
            PRODUCTION,          Account.of(login("yndx.multi15"),   password("simple123456"), uid("686422580")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest13"),   password("testQ@123!"),   uid("1120000000010014"))
    ));

    public static AccountWithScope multiCookieAccount14 = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.multi14"),   password("simple123456"), uid("4014139788")),
            TESTING,             Account.of(login("yndx.multi14"),   password("simple123456"), uid("4014139788")),
            PRODUCTION,          Account.of(login("yndx.multi14"),   password("simple123456"), uid("686425082")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest14"),   password("testQ@123!"),   uid("1120000000010016"))
    ));

    public static AccountWithScope checkCookiesTest = new AccountWithScope(ImmutableMap.of(
            DEVPACK,             Account.of(login("yndx.akita.check.cookies"),      password("simple123456"), uid("4013998254"),
                    BbResponse.from("fakebb/yndx.akita.check.cookies.json")),
            TESTING,             Account.of(login("yndx.akita.check.cookies"),      password("simple123456"), uid("4013998254")),
            PRODUCTION,          Account.of(login("yndx.akita.check.cookies"),      password("6AnqPx2LA7SR"), uid("683749534")),
            INTRANET_PRODUCTION, Account.of(login("yamailtest1"),    password("testQ@123!"),   uid("1120000000010003"))
    ));
}
