package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * 2 Тест кейса на проверку сохранения и отправки из черновика обычного письма
 *
 * @author jetliner
 */

@Aqua.Test
@Title("Отправка писем. Сохранение в черновик простого письма. Отправка из черновика")
@Description("Проверка что письмо нормально сохраняется в черновик и при отправке оттуда исчезает")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.DRAFT})
@Credentials(loginGroup = "SendFromDrafts")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SendFromDraftsTest extends BaseTest {
    private static String mid;
    private static String midDraftEmptyTo;
    private static String subj;


    @Rule
    public CleanMessagesRule clean = with(authClient);

    @Test
    @Title("Сохранение письма в черновики")
    @Description("Сохраняет простое письмо в черновик\n" +
            "Дополнительно проверяет WMI-371 - удаленный firstline при сохранении в черновики")
    public void aSimpleMailToDrafts() throws Exception {
        logger.warn("Сохранение письма в черновики");
        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToDRAFT().setSend("test");
        subj = msg.getSubj();

        api(MailSend.class).params(msg).post().via(hc);

        waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver();
        MailBoxList response = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.draftFID()))
                .post().via(hc);
        mid = response.getMidOfMessage(subj);

        // Заодно проверяем что мы сохранили в черновик
        Message mrsp = api(Message.class)
                .params(MessageObj.getMsg(mid))
                .post().via(hc);
        assertThat("Первая строка в письме не соотвествует содержимому! [WMI-371]", mrsp.getFirstlineText(),
                equalTo("test"));
        assertThat("Содержимое в письме не соответствует ожиданиям!",
                mrsp.getTextValueOfContentTag(), equalTo("test\n"));


        int before = response.countMessagesInFolder();
        logger.info("Сообщений в черновиках: " + before);
    }

    @Test
    @Title("Отправка письма из черновиков")
    @Description("Отправляет сохраненное письмо.\n" +
            "Проверяем что письмо не осталось в черновках")
    public void bSimpleMailSendFromDrafts() throws Exception {
        logger.warn("Отправка письма из черновиков");
        MailSendMsgObj msg = msgFactory
                .getSimpleEmptyMsgToSendFromDRAFT(mid).setSubj(subj);

        MailBoxList response = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.draftFID()))
                .post().via(hc);
        int before = response.countMessagesInFolder();

        logger.info("Сообщений в черновиках перед отправкой: " + before);

        //Отправка письма из черновиков
        api(MailSend.class).params(msg).post().via(hc);
        //Ожидание доставки во входящие
        waitWith.subj(subj).inFid(folderList.defaultFID()).waitDeliver();
        sleep(1000);
        response = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.draftFID()))
                .post().via(hc);
        int after = response.countMessagesInFolder();
        logger.info("Сообщений в черновиках после отправки: " + after);
        //before > after
        assertThat("Письма в черновиках не должны были остаться: ", before, greaterThan(after));
    }


    @Test
    @Title("Сохранение письма в черновики с отсутствующим <TO>")
    @Description("Сохраняем письмо в черновики без <TO>. Проеряем содержимое")
    public void dSimpleMailToDrafts() throws Exception {
        logger.warn("Сохранение письма в черновики с отсутствующим TO [DARIA-23337]");
        //чистка
        clean.all().inbox().outbox().draft();
        String sendBody = "test";
        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToDRAFT().setSend(sendBody).setTo("");
        api(MailSend.class).params(msg).post().via(hc);

        midDraftEmptyTo = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();

        logger.info("Проверяем что сохранили в черновик");
        Message mrsp = jsx(MessageBody.class)
                .params(MessageObj.getMsg(midDraftEmptyTo)
                        .setXmlVersion(MessageObj.XMLVERSION_DARIA2))
                .post().via(hc).as(Message.class).assertResponse(not(containsString("<error")));
        assertThat("Содержимое в черновике не соответствует ожиданиям! [DARIA-23337]",
                mrsp.getContentTagText(), equalTo(sendBody));

    }
}
