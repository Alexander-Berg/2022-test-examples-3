package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.attachInMessageShouldBeSameAs;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

@Aqua.Test
@Title("Отправка писем. Письмо с большим аттачем")
@Description("Тестируем реакцию почты на отправку аттачей в десяток МБ")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.ATTACH})
@Credentials(loginGroup = "BigAttachSend")
public class MailSendWithBigAttach extends BaseTest {

    private String subject;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Test
    @Title("Должны отправить большой аттач")
    @Description("Аттач (порядка 22Мб) - Сравнение md5 хэшей")
    public void mailWithBigAttach() throws Exception {
        String fileName = "indefinitely_attach.zip";
        logger.warn("Отсылка большого аттача " + fileName);
        File attach = prepareAttach(fileName);
        sendMailWithAttach(attach);
        Thread.sleep(60000);
        waitWith.subj(subject).waitDeliver();
        attachInMessageShouldBeSameAs(attach, subject, hc);
    }

    @Test
    @Title("Должны отправить большую картинку")
    @Description("Проверка md5")
    public void mailWithImage() throws Exception {
        String fileName = "indefinitely_IMG_2604.jpeg";
        logger.warn("Отсылка большой картинки " + fileName);
        File attach = prepareAttach(fileName);
        sendMailWithAttach(attach);
        waitWith.subj(subject).waitDeliver();
        attachInMessageShouldBeSameAs(attach, subject, hc);
    }


    /**
     * http://qa.yandex-team.ru/storage/get +
     * /ru/yandex/autotests/innerpochta/wmi/mailsend/MailSendWithBigAttach/attach.zip
     * /ru/yandex/autotests/innerpochta/wmi/mailsend/MailSendWithBigAttach/IMG_2604.jpeg
     * эти файлы уже лежат в сторадже. Предварительно залитые туда
     *
     * @param fileName - имя файла
     * @return скачанный в папку отчета файл
     * @throws Exception
     */
    private File prepareAttach(String fileName) throws Exception {
        String url = elliptics().path(this.getClass()).name(fileName).get().url();
        return downloadFile(url, fileName, hc);

    }


    /**
     * Отправка письма с указанным аттачем
     *
     * @param attach - файл аттача
     * @throws Exception *
     */
    private void sendMailWithAttach(File attach) throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .addAtts("binary", attach)
                .setSend("MailSendWithBigAttach :: " +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new Date()));
        subject = msg.getSubj();
        clean.subject(subject);
        jsx(MailSend.class).params(msg).post().via(hc);
    }
}

