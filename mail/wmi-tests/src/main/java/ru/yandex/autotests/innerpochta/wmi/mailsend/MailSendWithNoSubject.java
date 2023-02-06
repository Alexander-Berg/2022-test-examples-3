package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessagePartObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;

import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.Files.hash;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.DeleteUtils.deleteMsgsBySubjFromInboxSent;

@Aqua.Test
@Title("Отправка писем. Письма без темы")
@Description("Различные письма без указания темы")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailSendNoSubj")
public class MailSendWithNoSubject extends BaseTest {
    private MailSendMsgObj msg;

    @Before
    public void prepare() throws Exception {
        // Предварительно убеждаемся что не будет сообщений без темы перед тестом
        deleteMsgsBySubjFromInboxSent(WmiConsts.NO_SUBJECT_TITLE);

        msg = msgFactory.getSimpleEmptySelfMsg();
    }

    private String mid;

    @Test
    @Title("Отправка письма с пустым телом и пустой темой")
    @Description("- Проверка что подставилась тема по-умолчанию\n" +
            "- Проверка что тело осталось пустым")
    public void sendEmptyMail() throws Exception {
        msg.setSend("").setSubj("");

        jsx(MailSend.class).params(msg).post().via(hc);
        waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver();

        assertThat("Firstline должна быть пустой! ", getFirstLine(), equalTo(""));
    }

    @Test
    @Title("Отправка письма с непустым телом и пустой темой")
    @Description("- Проверка соответствия темы дефолтной\n" +
            "- Первой строки - отправленному телу")
    public void sendMailWithNoSubject() throws Exception {
        String firstLineExpected = Util.getRandomString();
        msg.setSend(firstLineExpected).setSubj("");

        jsx(MailSend.class).params(msg).post().via(hc);
        waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver();

        assertThat("Первая строка не соответствует ожиданиям. ", getFirstLine(), equalTo(firstLineExpected));
    }

    @Test
    @Title("Отправка письма с аттачем и пустой темой")
    @Description("- Сравнение md5 скачанного загруженному,\n" +
            "- Сравнение темы с дефолтной")
    public void sendEmptyMailWithAttach() throws Exception {
        String fileName = "randomFile";
        File attach = Util.generateRandomShortFile(fileName, 64);
        msg.setSend("").setSubj("").addAtts("binary", attach);


        jsx(MailSend.class).params(msg).post().via(hc);

        waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver();
        String firstLine = getFirstLine();
        assertThat("Первая строка должна быть пустой. Получено: " + firstLine, firstLine, equalTo(""));

        Message resp = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid))
                .post().via(hc);
        String hid = resp.getHidOfAttach(attach.getName());
        String url = api(MessagePart.class)
                .params(MessagePartObj.getUrlObj(mid, hid, attach.getName()))
                .post().via(hc)
                .toString();
        File deliveredAttach = downloadFile(url, attach.getName(), hc);
        assertThat("MD5 хэши файлов не совпали", hash(deliveredAttach, md5()).toString(),
                equalTo(hash(attach, md5()).toString()));
    }


    /**
     * Получает mid письма и ищет его первую строку
     *
     * @return String - первая строка (значение тега firstline)
     * @throws IOException *
     */
    private String getFirstLine() throws IOException {
        mid = jsx(MailBoxList.class).post().via(hc).getMidOfMessage(WmiConsts.NO_SUBJECT_TITLE);
        return api(Message.class)
                .params(MessageObj.getMsg(mid)).post().via(hc).getFirstlineText();
    }

    @After
    public void deleteMessage() throws Exception {
        jsx(MailboxOper.class)
                .params(MailboxOperObj.deleteOneMsg(mid))
                .post().via(hc);
        deleteMsgsBySubjFromInboxSent(WmiConsts.NO_SUBJECT_TITLE);
    }

}
