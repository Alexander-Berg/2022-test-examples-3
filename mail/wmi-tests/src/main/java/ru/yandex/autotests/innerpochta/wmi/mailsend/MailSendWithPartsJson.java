package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mailsend.PartsJson;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessagePartObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessagePart;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.UNKNOWN_ERROR_0;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.07.14
 * Time: 20:05
 * <p/>
 * DARIA-29427
 * DARIA-40870
 */
@Title("Отправка писем. Параметр part_json")
@Description("Проверяем, что аттачи отображаются, проверяем конфликты")
@Aqua.Test
@Credentials(loginGroup = "MailSendWithPartsJson")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Issue("MAILPG-747")
public class MailSendWithPartsJson extends BaseTest {

    public static final String EMPTY_KEY_JSON = "{\"\": \"\"}";

    public static final String IMAGE_PNG = "img/imgrotated/not_rotate.png";

    public static final String IMAGE_URl_JPEG = "http://img-fotki.yandex.ru/get/9300/219421176.0/0_d2a24_8d0e0fea_orig";
    public static final String IMAGE_URl_PNG = "http://img-fotki.yandex.ru/get/9315/219421176.0/0_d2a36_73c443_orig";

    public static void shouldSeeImageInMessage(File attach, String path, String subj,
                                               String fid, DefaultHttpClient hc) throws Exception {
        // Берем из ресурсов уже заранее заготовленную картинку
        String fileToCompare = Util.getRandomString();
        File rotatedImage = File.createTempFile(fileToCompare, null);
        asByteSink(rotatedImage).write(asByteSource(getResource(path)).read());

        // Получаем мид
        String mid = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(fid))
                .post().via(hc).getMidOfMessage(subj);
        // Получаем вывод письма
        Message resp = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid))
                .post().via(hc);
        String hid = resp.getHidOfAttach(attach.getName());
        String url = api(MessagePart.class)
                .params(MessagePartObj.getUrlObj(mid, hid, attach.getName()))
                .post().via(hc).assertResponse("Ожидается корректная ссылка", containsString("http"))
                .toString();
        File deliveredAttach = downloadFile(url, attach.getName(), hc);
        assertThat("MD5 хэши файлов не совпали", deliveredAttach, hasSameMd5As(rotatedImage));
    }

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox().draft();

    private static File imageJpeg;
    private static File imagePng;

    @BeforeClass
    public static void prepare() throws IOException {
        imageJpeg = downloadFile(IMAGE_URl_JPEG, getRandomString(), authClient.notAuthHC());
        imagePng = downloadFile(IMAGE_URl_PNG, getRandomString(), authClient.notAuthHC());
    }

    @Test
    @Description("Можем отправить письмо с вложенным аттачем")
    public void shouldSendWithAttachAndMidHidRotate() throws Exception {
        String mid = sendWith.waitDeliver().addAtts(imagePng).send().getMid();

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();
        String hid = api(Message.class).params(getMsg(mid)).post().via(hc).getHidOfAttach(imagePng.getName());

        msg.withPartsJson(new PartsJson().withMid(parseLong(mid)).withHid(parseDouble(hid)));
        jsx(MailSend.class).params(msg).post().via(hc).shouldBe().statusOk();

        waitWith.subj(msg.getSubj()).waitDeliver();
        shouldSeeImageInMessage(imagePng, IMAGE_PNG, msg.getSubj(), folderList.defaultFID(), authClient.authHC());
    }


    @Test
    @Title("Можем отправить письмо с двумя параметрами parts_json")
    public void shouldSendWithTwoPartsJson() throws Exception {
        String mid = sendWith.waitDeliver().addAtts(imagePng).addAtts(imagePng).send().getMid();

        String hid = api(Message.class).params(getMsg(mid)).post().via(hc).getHidOfAttach(imagePng.getName());
        String hid2 = api(Message.class).params(getMsg(mid)).post().via(hc).getHidOfAttach(imagePng.getName());

        MailSendMsgObj sendMess = msgFactory
                .getSimpleEmptyMsgToSendFromDRAFT(mid);

        sendMess.withPartsJson(
                new PartsJson().withMid(parseLong(mid)).withHid(parseDouble(hid)),
                new PartsJson().withMid(parseLong(mid)).withHid(parseDouble(hid2))
        );

        jsx(MailSend.class).params(sendMess).post().via(hc).shouldBe().statusOk();

        waitWith.subj(sendMess.getSubj()).waitDeliver();

        shouldSeeImageInMessage(imagePng, IMAGE_PNG, sendMess.getSubj(), folderList.defaultFID(), authClient.authHC());

        shouldSeeImageInMessage(imagePng, IMAGE_PNG, sendMess.getSubj(), folderList.defaultFID(), authClient.authHC());
    }

    @Test
    @Title("Не можем отправить письмо без hid")
    public void cantSendWithoutHid() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();

        msg.withPartsJson(new PartsJson().withMid(1L));

        jsx(MailSend.class).params(msg).post().via(hc)
                .shouldBe().errorcode(UNKNOWN_ERROR_0);
    }


    @Test
    @Title("Не можем отправить письмо без mid")
    public void cantSendWithoutMid() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();

        msg.withPartsJson(new PartsJson().withHid(1.1));

        jsx(MailSend.class).params(msg).post().via(hc).shouldBe().errorcode(UNKNOWN_ERROR_0);
    }

    @Test
    @Title("Должна быть ошибка при запросе несуществующего mid")
    @Issue("MAILWEB-786")
    public void shouldNotSendWithNotExistsMid() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();

        msg.withPartsJson(new PartsJson().withMid(123L).withHid(1.1));

        String status = jsx(MailSend.class).params(msg).post().via(hc).shouldBe().getStatusValue();
        assertThat("Отвечаем с неверным статусом", status, is("parts_json_invalid"));
    }

    @Test
    @Title("Не должны отправлять письмо с пустым json")
    @Description("Вида {\"\": \"\"} и [{\"\": \"\"}]")
    @Issue("DARIA-40870")
    public void cantSendWithWrongJson() throws Exception {
        SendUtils sendUtils = sendWith.waitDeliver().addAtts(imageJpeg).addAtts(imagePng).send();
        String mid = sendUtils.getMid();

        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToSendFromDRAFT(mid);

        jsx(MailSend.class).params(msg.withPartsJson(EMPTY_KEY_JSON))
                .post().via(hc).errorcode(UNKNOWN_ERROR_0);
        assertThat("Письмо с нулевым JSON пришло [DARIA-40870]", hc, not(hasMsg(msg.getSubj())));

        jsx(MailSend.class).params(msg.withPartsJson(format("[%s]", EMPTY_KEY_JSON)))
                .post().via(hc).errorcode(UNKNOWN_ERROR_0);
        assertThat("Письмо с нулевым массивом JSON пришло [DARIA-40870]", hc, not(hasMsg(msg.getSubj())));
    }


}
