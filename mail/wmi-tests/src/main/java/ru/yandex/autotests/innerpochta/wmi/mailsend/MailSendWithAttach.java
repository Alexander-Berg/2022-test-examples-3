package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
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
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 9/16/13
 * Time: 2:07 PM
 */
@Aqua.Test
@Title("Отправка писем. С различными аттачами")
@Description("Отправляем разные аттачи")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.ATTACH})
@Credentials(loginGroup = "Sendwithattach")
public class MailSendWithAttach extends BaseTest {

    private MailSendMsgObj msg;
    public static final String IMAGE_URl_JPEG =
            "http://img-fotki.yandex.ru/get/9300/219421176.0/0_d2a24_8d0e0fea_orig";
    public static final String FILE_DECODED = "add_decoded";

    public static String attacheName;
    public static final int COUNT_OF_ATTACH = 100;

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox();

    @Before
    public void prepare() throws Exception {
        attacheName = Util.getRandomString();
        msg = msgFactory.getSimpleEmptySelfMsg().setSend("Sendwithattach :: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date(System.currentTimeMillis())));
    }

    @Test
    @Description("* Отправляем письмо пользователю,\n" +
            "     * которое он не может принять из-за ограничений\n" +
            "     * maxsize сервера.\n" +
            "     * Смотри конфиг /etc/wmi/smtplimits.lst\n" +
            "     * Вид записи: ya.ru 200 (домен, пробел, число в байтах)\n" +
            "     * При превышении лимита, появляется нужный тег с содержимым")
    public void sendWithMaxsize() throws Exception {
        logger.warn("[DARIA-21681]");
        logger.warn("[DARIA-19939] к тегу recipient добавились аттрибуты login и limit");
        String login = Util.getRandomString();
        String fileName = Util.getRandomString();

        File bigFile = genFile(1024 * 1024 * 15);

        clean.subject(msg.getSubj());
        msg.addAtts(fileName, bigFile).setTo(login + "@016.ru");
        MailSend resp = jsx(MailSend.class).params(msg).post().via(hc);

        assertThat(resp.toDocument(), hasXPath("//limited/recipient", not(IsEqual.equalTo(""))));
        assertThat("неверный логин ", resp.getLimitedLogin(), equalTo(login));
        assertThat(resp.toDocument(), hasXPath("//limited/recipient/@limit", not("")));
    }

    @Test
    @Description("Прикрепляем 100 файлов\n" +
            "Проверяем что письмо приходит с этими аттачами\n" +
            "Жесткий тест")
    public void sendBigCountAttaches() throws Exception {
        String fileName = Util.getRandomString();
        logger.warn("Отсылка большого количества аттачей " + fileName);
        File attach = downloadFile(IMAGE_URl_JPEG, fileName, hc);

        clean.subject(msg.getSubj());
        addCountAtts(msg, attacheName, attach, COUNT_OF_ATTACH);

        jsx(MailSend.class).params(msg).post().via(hc);
        waitWith.subj(msg.getSubj()).waitDeliver();
        attachsInMessageShouldBeCountAs(msg.getSubj(), COUNT_OF_ATTACH, hc);
        attachsInMessageShouldBeSameAs(attach, msg.getSubj(), hc);
    }

    /**
     * Добавляем произвольное количество одинаковых аттачей.
     *
     * @param msg
     * @param name  имя аттача
     * @param file  аттач
     * @param count количество добавляемых аттачей
     * @return
     * @throws FileNotFoundException
     */
    public MailSendMsgObj addCountAtts(MailSendMsgObj msg, String name,
                                       File file, int count) throws FileNotFoundException {
        for (int i = 0; i < count; i++) {
            msg.addAtts(name, file);
        }
        return msg;
    }

    @Test
    @Issue("WMI-806")
    @Description("Тестируем на аттаче: add_decode\n" +
            "что прикрепляется меньше чем 3 минуты\n" +
            "[WMI-806]")
    public void sendWithAttachAddDecoded() throws Exception {
        File decode = File.createTempFile(FILE_DECODED, null);
        asByteSink(decode).write(asByteSource(getResource(FILE_DECODED)).read());
        msg.addAtts(attacheName, decode);
        jsx(MailSend.class).params(msg).post().via(hc);
        waitWith.subj(msg.getSubj()).waitDeliver();
        attachInMessageShouldBeSameAs(decode, msg.getSubj(), hc);
    }
}
