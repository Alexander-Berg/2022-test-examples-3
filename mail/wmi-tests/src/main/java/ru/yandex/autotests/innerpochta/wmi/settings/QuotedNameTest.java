package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.BackupSettingWithWmiRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * ВНИМАНИЕ! Берет из настроек имя отправителя только JSX ручка отправки!
 */
@Aqua.Test
@Title("Тестирование настроек. Тестирование различного эскейпинга кавычек")
@Description("Проверка эскейпинга кавычек в хедерах to и from")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = "QoutedName")
public class QuotedNameTest extends BaseTest {

    @Parameterized.Parameter
    public String fromName;

    @Parameterized.Parameter(1)
    public String fromNameExpected;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws Exception {
        return asList(new Object[]{"Петрович А. Г.", "Петрович А. Г."}, // [DARIA-19183]
                new Object[]{"!@#$%^&*() Qe.", "!@#$%^&*() Qe."},
                new Object[]{"А. Г. Петрович", "А. Г. Петрович"},
                new Object[]{"A. B. C.", "A. B. C."},
                new Object[]{"...", "..."},
                new Object[]{".", "."},
                new Object[]{".CC", ".CC"},
                new Object[]{"A.C", "A.C"},
                new Object[]{"Ш . A . C", "Ш . A . C"},
                new Object[]{"Петрович \"А. Г.\"", "Петрович \"А. Г.\""},
                new Object[]{"Группа компаний \"Абыр\"", "Группа компаний \"Абыр\""},
                new Object[]{"\"Superman (Абыр)\"", "Superman (Абыр)"},
                new Object[]{
                        "\"\"'sdfsdf@yaasdfndex-team.ru' (maisdfl-tasdfest@yanasdfdex-team.ru)\"\"",
                        "\"\"'sdfsdf@yaasdfndex-team.ru' (maisdfl-tasdfest@yanasdfdex-team.ru)\"\""
                });
    }

    private String subject;

    @Rule
    public BackupSettingWithWmiRule backup = BackupSettingWithWmiRule.with(authClient).backup("from_name");

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).inbox().outbox();

    @Test
    @Issues({@Issue("MAILPROTO-136"), @Issue("DARIA-19183")})
    @Title("Проверка, что нет дополнительного эскейпинга кавычек в русском имени отправителя " +
            "и не обрезаются инициалы")
    @Description("Чтобы воспроизвести, надо в настройках\n" +
            " \"Информация об отправителе\" поставить\n" +
            "Моё имя = Магазинчик \"У Зайца\" и отправить себе письмо.\n" +
            "В полученном письме кавычки будут эскейпиться в веб-интерфейсе, что неверно\n" +
            "Ставит в настройках подобное имя, отправляет письмо,\n" +
            " - Смотрит что в полученном письме имя отправителя не эскейпит дополнительно кавычек")
    public void testQuotedName() throws Exception {
        logger.warn("Проверка, что нет дополнительного эскейпинга кавычек в русском имени отправителя [MAILPROTO-136]" +
                " и не обрезаются инициалы [DARIA-19183]");
        // Подготовка имени:
        SettingsSetupUpdateSomeObj setupUpdateSome = SettingsSetupUpdateSomeObj.settings();
        setupUpdateSome.setUpdates("from_name", fromName);

        SettingsSetupUpdateSome settingsUpdOper = jsx(SettingsSetupUpdateSome.class).params(setupUpdateSome);
        settingsUpdOper.post().via(hc);

        assertThat(hc, withWaitFor(hasSetting("from_name", equalTo(fromName)), SECONDS.toMillis(5)));

        // Отправка письма с настроенным адресатом
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();
        SendMessage mailSendOper = jsx(SendMessage.class)
                .params(msg.setSend("Абра кадабра - Эскейпинг кавычек в поле От"));
        subject = msg.getSubj();
        clean.subject(subject);

        mailSendOper.post().via(hc);

        waitWith.subj(subject).waitDeliver();

        MailBoxList resp = jsx(MailBoxList.class).post().via(hc);
        String mid = resp.getMidOfMessage(subject);
        String nameWeGot = resp.getFromNameByMid(mid);
        assertThat("Имя отправителя не соответствует ожидаемому", nameWeGot, equalTo(fromNameExpected));
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Title("Проверка, что нет дополнительного эскейпинга кавычек в русском имени в поле КОМУ, и не обрезаются инициалы")
    @Issues({@Issue("DARIA-15086"), @Issue("DARIA-15528")})
    @Description("DARIA-15086\n" +
            "DARIA-15528  - адрес вида \"\"'sdfjksdf(sdfj)'\"\" <адрес> - считается некорректным\n" +
            "Отправляем письмо адресату, указывая в поле кому что то вроде \"Superman (Супермен)\" <email@y.ru>\n" +
            " - Проверяем, что в полученном письме в поле КОМУ будет указанное имя, без изменений\n" +
            "После выкладки 15528, выложится верстка, которая будет эскейпить кавычки получателя\n" +
            "Отправляемые адреса в целом правильные, но ожидать от wmi следует не того, что отправляем.\n" +
            "Wmi пилит по пробелам получателя, а потом склеивает заново.\n" +
            "При этом слово в кавычках в итоге остается без кавычек. Это оказывается нормальное поведение.")
    public void testQuotedToName() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();
        // Эскейпим кавычки и заключаем все это в еще одни кавычки, чтобы пробелы оказались внутри
        String name = "\"" + fromName.replaceAll("\\\"", "\\\\\"") + "\"";
        SendMessage mailSendOper = jsx(SendMessage.class)
                .params(msg.setTo(name + "<" + authClient.acc().getSelfEmail() + ">")
                        .setSend("Абра кадабра - Эскейпинг кавычек в поле Кому"));
        subject = msg.getSubj();
        clean.subject(subject);
        mailSendOper.post().via(hc);

        waitWith.subj(subject).waitDeliver();

        // Получение отправителя письма просмотром списка писем
        MailBoxList resp = jsx(MailBoxList.class).post().via(hc);
        String mid = resp.getMidOfMessage(subject);
        String nameWeGot = resp.getToNameByMid(mid);
        assertThat(String.format("Имя получателя не соответствует ожидаемому (отправлено: %s)", name),
                nameWeGot, equalTo(fromNameExpected));
    }
}
