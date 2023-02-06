package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.sanitizer.ImageProxy;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.UploadAttachmentXmlObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;
import java.io.IOException;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.attachInMessageShouldBeSameAs;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.IGNORE_HIGHLIGHT;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.OUTPUT_AS_CDATA;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.XML_STREAMER_MOBILE;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.XML_STREAMER_ON;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getInlineHtml;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getSmileHtml;

@Aqua.Test
@Title("Отправка писем. Инлайновые аттачи")
@Description("Встраивает в html письма смайлы и открытки")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.ATTACH})
@Issue("MAILADM-4048")
@Credentials(loginGroup = "InlineAttaSend")
public class MailSendWithInlineAttaches extends BaseTest {

    private MailSendMsgObj msg;
    private MailSend sendOper;
    private String subj;

    private String mid;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Before
    public void prepare() throws Exception {
        msg = msgFactory.getSimpleEmptySelfMsg().setTtypeHtml();
        sendOper = jsx(MailSend.class).params(msg);
        subj = msg.getSubj();
        clean.subject(subj);
    }

    @Test
    @Title("Отправка смайла в html письме")
    @Description("Отправляет html-письмо с картинкой\n" +
            "- Проверяет что отправленная картинка равна полученной")
    public void sendMailWithImage() throws Exception {
        logger.warn("Отправка смайла в html письме");
        int smileNumber = Util.getRandomShortInt();

        msg.setSend(getSmileHtml(smileNumber));
        sendOper.post().via(hc);

        mid = waitWith.subj(subj).waitDeliver().getMid();
        checkSmileInMessage();
    }

    @Test
    @Issue("DARIA-43044")
    @Title("Сохраняем инлайновый аттач в черновики")
    @Description("Отправляет html-письмо с картинкой\n" +
            "- Проверяет что отправленная картинка равна полученной")
    public void saveInDraftMailWithImage() throws Exception {
        logger.warn("Отправка смайла в html письме");
        int smileNumber = Util.getRandomShortInt();
        MailSendMsgObj msg = msgFactory.getSimpleEmptyMsgToDRAFT().setTtypeHtml().setSubj(subj).setSend(
                getSmileHtml(smileNumber));

        jsx(MailSend.class).params(msg).post().via(hc);
        mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
        checkSmileInMessage();

        smileNumber = Util.getRandomShortInt();
        msg.setIgnOverwrite("no").setOverwrite(mid).setSend(getSmileHtml(smileNumber));

        api(MailSend.class).params(msg).post().via(hc);
        mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        checkSmileInMessage();
        //отсылаем письмо из черновиков и проверяем картинку
        MailSendMsgObj msg2 = msgFactory.getSimpleEmptyMsgToSendFromDRAFT(mid)
                .setSubj(msg.getSubj()).setTtypeHtml().setSend(getSmileHtml(smileNumber));
        jsx(MailSend.class).params(msg2).post().via(hc).statusOk();
        mid = waitWith.subj(msg2.getSubj()).waitDeliver().getMid();
        checkSmileInMessage();
    }

    @Test
    @Title("Отправка открытки, смайла и аттача в html письме")
    @Description("Отправляет html-письмо, содержащее и открытку и картинку\n" +
            "- Проверяет что все отправленное равно полученному")
    public void sendMailWithSmileImageAndAttach() throws Exception {
        logger.warn("Отправка открытки, смайла и аттача в html письме");
        int smileNumber = Util.getRandomShortInt();
        File attach = Util.generateRandomShortFile("attach", 1024);

        msg.addAtts("application", attach);
        msg.setSend(
                "<div>" +
                        "<table " +
                        "class=\"yandexcardtable\" " +
                        "style=\"margin : 0px auto;\" " +
                        "border=\"0\">" +
                        "<tbody>" +
                        "<td class=\"congratulations\"" +
                        " style=\"text-align : center\"><br /><span>&nbsp;</span>" +
                        "</td>" +
                        "</tr>" +
                        "           </tbody>" +
                        "</table>" +
                        "<br/>test" +
                        "</div>" +
                        "<img class=\"yandex_smile_" + smileNumber + "\" " +
                        "src=\"http://img.yandex.net/i/smiles/small/smile_" + smileNumber + ".gif\" " +
                        "mce_src=\"http://img.yandex.net/i/smiles/small/smile_" + smileNumber + ".gif\">"
        );
        sendOper.post().via(hc);
        mid = waitWith.subj(subj).waitDeliver().getMid();

        attachInMessageShouldBeSameAs(attach, subj, hc);
        checkSmileInMessage();
    }

    @Test
    @Title("Отсылаем письмо с инлайновым аттачем")
    @Description("Загружаем аттач через ручку write_attachment")
    public void sendWithInlineAttach() throws Exception {
     String fileToCompare = Util.getRandomString();
     File inline = File.createTempFile(fileToCompare, null);

     asByteSink(inline).write(asByteSource(getResource("img/imgrotated/not_rotate.jpg")).read());
     UploadAttachmentXml upload = jsx(UploadAttachmentXml.class)
                .params(UploadAttachmentXmlObj.getObjToUploadFile(inline))
                .post().via(hc);

     String mid = sendWith.send(getInlineHtml(upload.getId(), upload.getViewLargeUrl())).tTypeHtml().waitDeliver().send().getMid();

     MessageObj msgObj = MessageObj.getMsg(mid).setFlags(IGNORE_HIGHLIGHT, OUTPUT_AS_CDATA, XML_STREAMER_ON, XML_STREAMER_MOBILE);

     Message msgTest = api(Message.class).params(msgObj).get().via(hc);
     msgTest.setHost(props().productionHost());
     Message msgBase = msgTest.get().via(hc);
     assertThat(msgTest.toDocument(), equalToDoc(msgBase.toDocument())
                        .urlcomment(mid)
                        .exclude("//timer_mulca")
                        .exclude("//timer_db")
                        .exclude("//timer_logic"));
    }

    private void checkSmileInMessage() throws Exception {
        String url = getUrlToFile();
        assertThat("Смайл не проксируется через кешер resize.yandex.net", url, startsWith(ImageProxy.RESIZE_MAIL));
    }

    /**
     * Получает ссылку на инлайн аттач для скачивания
     *
     * @return String       - ссылка на файл-вложение
     * @throws Exception *
     */
    private String getUrlToFile() throws Exception {
        Message mrsp = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid))
                .post().via(hc);
         return mrsp.getImg();
    }
}