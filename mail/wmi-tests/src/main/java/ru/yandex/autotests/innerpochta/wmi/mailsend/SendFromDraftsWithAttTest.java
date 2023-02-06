package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.util.LogToFileUtils;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessagePartObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.ArrayList;

import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.Files.hash;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.inFid;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * 2 Тесткейса на сохранение и отправку из черновика письма с вложениями
 * DARIA-40468
 *
 * @author lanwen
 */
@Aqua.Test
@Title("Отправка писем. Письма с аттачем в черновик. Отправка письма с аттачем из черновика")
@Description("Сохраняет, затем отправляет в черновик письмо с аттачами. " +
                "Проверяет что в черновиках письма не осталось")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.DRAFT, MyStories.ATTACH})
@Credentials(loginGroup = "SendFromDraftsWithAtta")
public class SendFromDraftsWithAttTest extends BaseTest {
    private static String mid;
    private static String subj;

    private static ArrayList<File> attach = new ArrayList<>();

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox().draft();

    @Before
    @Step("Сохранение письма с аттачами в черновики")
    public void prepare() throws Exception {
        logger.warn("Сохранение письма с аттачами в черновики");
        attach.add(prepareAttach("indefinitely_postcard.jpg"));
        attach.add(prepareAttach("indefinitely_resource_3786..pdf"));

        subj = saveMailWithAttachToDraft(attach);
        mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
    }

    @Test
    @Title("Отправка письма с аттачами из черновиков")
    @Description("Открывает письмо из черновиков, и отправляет его себе\n" +
            "Проверяет все ли аттачи на месте")
    public void shouldSendMailSendWithAttFromDrafts() throws Exception {
        logger.warn("Отправка письма с аттачами из черновиков");
        //чистка
        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToSendFromDRAFT(mid).setSubj(subj);
        for (File att : attach) {
            msg.addAtts("binary", att);
        }
        msg.setSend("SendFromDraftsWithAttTest::mailSendWithAttFromDrafts()" + Util.getRandomString());

        Integer before = jsx(MailBoxList.class).params(inFid(folderList.draftFID())).post().via(hc)
                .countMessagesInFolder();
        assertThat("Должны быть черновики", before, greaterThan(0));
        logger.info("Сообщений в черновиках перед отправкой: " + before);

        //Отправка письма из черновиков
        api(MailSend.class).params(msg).post().via(hc);
        //Ожидание доставки во входящие
        waitWith.subj(msg.getSubj()).inFid(folderList.defaultFID()).waitDeliver();
        checkAttachInMessage(msg.getSubj(), attach);

        Thread.sleep(2000);

        Integer after = jsx(MailBoxList.class).params(inFid(folderList.draftFID())).post().via(hc)
                .countMessagesInFolder();
        logger.info("Сообщений в черновиках после отправки: " + after);

        //before > after
        assertThat("Письма в черновиках не должны были остаться: ", before, greaterThan(after));
    }

    /**
     * /ru/yandex/autotests/innerpochta/wmi/mailsend/SendFromDraftsWithAttTest/
     * - postcard.jpg
     * - resource_3786..pdf - уже загруженные в эллиптикс аттачи
     * Ищет в репо имена аттачей
     *
     * @param fileName имя аттача
     * @return объект файла
     * @throws Exception *
     */
    private File prepareAttach(String fileName) throws Exception {
        return elliptics()
                .path(this.getClass()).name(fileName)
                .get().asFile(LogToFileUtils.getLogFile("dowloaded_", fileName));
    }

    /**
     * Сохраняет письмо с пачкой аттачей (основной метод для теста)
     *
     * @param attach - файл аттача
     * @return Тема отправленного письма
     * @throws Exception *
     */
    private String saveMailWithAttachToDraft(ArrayList<File> attach) throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToDRAFT();
        for (File att : attach) {
            msg.addAtts("binary", att);
        }
        msg.setSend("SendFromDraftsWithAttTest::saveMailWithAttachToDraft()" + Util.getRandomString());
        api(MailSend.class).params(msg).post().via(hc);
        return msg.getSubj();
    }

    /**
     * Проверка соответствия аттачей скачанных залитым
     *
     * @param subj   - тема письма
     * @param attach - список аттачей
     * @throws Exception *
     */
    private void checkAttachInMessage(String subj, ArrayList<File> attach) throws Exception {
        String mid = jsx(MailBoxList.class)
                .post().via(hc).getMidOfMessage(subj);
        Message resp = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid))
                .post().via(hc);
        for (File att : attach) {
            String hid = resp.getHidOfAttach(att.getName());
            String url = api(MessagePart.class)
                    .params(MessagePartObj.getUrlObj(mid, hid, att.getName()))
                    .post().via(hc)
                    .toString();
            File deliveredAttach = downloadFile(url, att.getName(), hc);

            System.out.println("check: " + att.length() + "    " + deliveredAttach.length());
            assertThat("MD5 хэши файлов не совпали", hash(deliveredAttach, md5()).toString(),
                    equalTo(hash(att, md5()).toString()));
        }
    }
}
