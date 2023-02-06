package ru.yandex.autotests.innerpochta.wmi.labels;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabelLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MessagesWithLabelMatcher.hasMessagesWithLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

@Aqua.Test
@Title("Метки. Различные функции")
@Description("Тестирование функционала, связанного с метками")
@Features(MyFeatures.WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "ExtLabelTest")
public class LabelTest extends BaseTest {

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox().all();

    @Rule
    public RuleChain clearLabels = new LogConfigRule().around(DeleteLabelsRule.with(authClient).all());

    @Test
    @Title("Проверяем ручку снятия метки со всех писем с этой меткой")
    @Issue("DARIA-2617")
    @Description("DARIA-2617\n" +
            "Проверяет работу метода, снимающего метку со всех писем с этой меткой\n" +
            "Посылаем несколько писем самому себе\n" +
            "Создаем метку\n" +
            "Помечаем меткой все пришедшие письма\n" +
            "Снимаем /api/settings_label_clear метку со всех писем\n" +
            "- Проверяем что метка снялась")
    public void testSettingsLabelClear() throws Exception {
        logger.warn("[DARIA-2617]: Проверяем ручку снятия метки со всех писем с этой меткой");
        // Сколько писем отсылать
        int msgToSendCnt = 3;

        // Создаем метку с рандомным именем и узнаем ее lid
        String labelName = Util.getRandomString();
        newLabel(labelName).post().via(hc);
        String labelId = jsx(Labels.class).post().via(hc).lidByName(labelName);

        // Получаем миды всех писем с данной темой
        String subject = Util.getRandomString();
        List<String> midsList = sendWith.viaProd().subj(subject).count(msgToSendCnt).waitDeliver()
                .send().getMids();

        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(midsList, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        // Тут же снимаем метку
        jsx(SettingsLabelClear.class).params(SettingsLabelClearObj.getObjToClearOneLabel(labelId)).post().via(hc);

        // Ждем пока все метки снимутся
        assertThat(hc, withWaitFor(not(hasMessagesWithLabel(labelId)), SECONDS.toMillis(3)));

        // Удаление созданной метки без force
        jsx(SettingsLabelDelete.class).params(SettingsLabelDeleteObj.oneLid(labelId)).post().via(hc);

        assertThat("Метка не удалилась", hc, not(hasLabelLid(labelId)));
    }

    @Test
    @Title("Проверяет простановку метки 'пересланное'")
    @Issue("DARIA-12631")
    @Description("DARIA-12631\n" +
            "Как воспроизвести вручную (при наличии баги)\n" +
            "Включаем в конфиге асинхронную отправку: <async_mail_send>yes</async_mail_send>\n" +
            "Перезапускаем wmi. Открываем письмо и нажимаем кнопку \"Переслать\". Указываем адресата\n" +
            "и нажимаем отправить. Возвращаемся во \"Входящие\". Пересылаемое письмо не пометелось стрелочкой.\n" +
            "Ожидается, что пересылаемое письмо пометилось стрелочкой.\n" +
            "Тест делает следующее:\n" +
            "Отсылает письмо самому себе (чтобы чистенькое было)\n" +
            "- Получает и проверяет что на нем нет пока метки пересылки\n" +
            "Пересылает полученное самому себе\n" +
            "- Проверяет пометку FAKE_FORWARDED_LBL (в морде это отображает стрелочка)")
    public void testAutoMarkMessageForwarded() throws Exception {
        logger.warn("[DARIA-12631]: Проверяет простановку метки 'пересланное'");
        String lid = labels.forwarded();
        // Посылка письма
        String subject = Util.getRandomString();
        String mid = sendWith.viaProd().waitDeliver().subj(subject).send().getMid();

        assertThat("Отправленное и полученное письмо уже почему-то помечено пересланным", hc,
                not(hasMsgWithLid(mid, lid)));

        forwardMessage(mid, subject);

        assertThat("Ожидалось что письмо будет помечено как пересылаемое, но письмо оказалось без пометки",
                hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("Проверяет простановку метки 'Прочитанное' (FAKE_SEEN_LBL)")
    @Description("Отправка сообщения,\n" +
            "- проверка что оно приходит непрочитанным\n" +
            "Метим прочитанным при помощи mailbox_oper?oper=mark_read\n" +
            "- проверка появления метки FAKE_SEEN_LBL\n" +
            "Метим непрочитанным\n" +
            "- проверяем что метка о прочитанности исчезла")
    public void testMarkMessageSeenUnseen() throws Exception {
        logger.warn("Проверяет простановку метки 'Прочитанное' (FAKE_SEEN_LBL)");

        List<String> lid = new ArrayList<String>();
        // Это действительно лид
        lid.add(WmiConsts.FAKE_SEEN_LBL);

        // Мид письма во входящих
        String subject = Util.getRandomString();
        String mid = sendWith.viaProd().subj(subject).waitDeliver().send().getMids().get(0);

        assertThat("Отправленное и полученное письмо уже почему-то помечено прочитанным", hc,
                not(hasMsgWithLids(mid, lid)));

        jsx(MailboxOper.class).params(MailboxOperObj.markReadOneMsg(mid)).post().via(hc);

        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", hc,
                hasMsgWithLids(mid, lid));

        jsx(MailboxOper.class).params(MailboxOperObj.markUNReadOneMsg(mid)).post().via(hc);

        assertThat("Ожидалось, что на письме не будет метки прочитанное, но письмо оказалось помеченным", hc,
                not(hasMsgWithLids(mid, lid)));
    }

    /**
     * Пересылка сообщения
     *
     * @param mid     - мид сообщения которое пересылаем
     * @param subject - тема пересылаемого сообщения
     * @return тема пересланного сообщения
     * @throws Exception *
     */
    private String forwardMessage(String mid, String subject) throws Exception {
        MailSendMsgObj msg = msgFactory.getForwardMsg(mid, subject);
        // Пересылаем сообщение
        jsx(MailSend.class).params(msg).post().via(hc).withDebugPrint();
        waitWith.subj(subject).count(2).waitDeliver();
        return msg.getSubj();
    }

    @Test
    @Title("Метка и снятие метки")
    @Description("Отправляем письмо\n" +
            "Создаем метку\n" +
            "Помечаем письмо меткой\n" +
            "Снимаем метку\n" +
            "- Проверяем что метка снялась")
    public void shouldLabelAndUnlabellMessage() throws Exception {
        logger.warn("Метка и снятие метки");

        String subject = Util.getRandomString();
        List<String> midsList = sendWith.viaProd().subj(subject).waitDeliver().send().getMids();

        // Создаем метку с рандомным именем и узнаем ее lid
        String labelId = jsx(SettingsLabelCreate.class)
                .params(SettingsLabelCreateObj.newLabel(Util.getRandomString()))
                .post().via(hc).updated();

        // Помечаем отправленное письмо(письма) меткой
        api(MessageToLabel.class)
                .params(labelMessages(midsList, labelId))
                .post().via(hc);

        // Снимаем метку с писем
        api(MessageToUnlabelOneLabel.class)
                .params(labelMessages(midsList, labelId))
                .post().via(hc);

        // Проверяем
        assertThat("Ожидалось, что на письме не будет меток", hc, not(hasMsgsWithLids(midsList, asList(labelId))));
    }
}
