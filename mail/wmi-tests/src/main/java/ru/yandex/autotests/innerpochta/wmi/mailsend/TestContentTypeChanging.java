package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;

import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsgWithContentFlag;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

@Aqua.Test
@Title("Отправка писем. Письмо с вложением определенного типа. Должен быть фильтр!")
@Description("Проверка что аттач при получении имеет нужный content-type")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Group2")
public class TestContentTypeChanging extends BaseTest {

    private static final String FOLDER_NAME = "TestContentTypeChanging";

    private static String subject;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Test
    @Title("Проверка contenttype apk аттача")
    @Description("Отправка специализированного файла с получением content-type\n" +
            "Внимание, для теста требуется фильтр, пересылающий все письма, содержащие\n" +
            "в теме contenttype в папку TestContentTypeChanging")
    public void testContentTypeChangingAPK() throws Exception {

        subject = Util.getRandomString() + "contenttype";
        clean.subject(subject);

        sendMailWithSpecifiedAttach(subject, "apk");
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(FOLDER_NAME);
        waitWith.subj(subject).inFid(fid).waitDeliver();

        assertThat("ContentType не соответствует ожидаемому",
                getAppSubType(subject), equalTo("vnd.android.package-archive"));
    }


    /**
     * Отправка письма с аттачем
     *
     * @param subject тема письма k
     * @throws Exception *
     */
    private void sendMailWithSpecifiedAttach(String subject, String ext) throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSubj(subject)
                .setSend(ext)
                .addAtts(randomFile(ext));
        jsx(MailSend.class).params(msg).post().via(hc).withDebugPrint();
    }

    private String getAppSubType(String subject) throws Exception {
        String folderId = jsx(FolderList.class).post().via(hc).getFolderId(FOLDER_NAME);
        String mid = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderId))
                .post().via(hc)
                .getMidOfMessage(subject);
        // Метим прочитанным
        return api(Message.class)
                .params(getMsgWithContentFlag(mid))
                .post().via(hc)
                .firstSubTypeOf("application");
    }

    private File randomFile(String ext) throws Exception {
        File randomFile = File.createTempFile(randomAlphabetic(8), "." + ext);
        FileUtils.writeStringToFile(randomFile, random(1024));
        return randomFile;
    }
}