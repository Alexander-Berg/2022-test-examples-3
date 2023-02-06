package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.data.B2BMessages.excludeNodes;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.04.14
 * Time: 14:44
 * https://verstka9-qa.yandex.ru/jsxapi/simple.jsx?wmi-method=message_body&ids=2500000002107774167
 * &flags=XmlStreamerOn&hid=1.2.1&xml_version=daria2
 * [DARIA-35043]
 */
@Aqua.Test
@Title("Отправка писем. Письмо с eml аттачем")
@Description("Отправляем письмо с eml аттачем, сравниваем с продакшеном")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailSendWithEml")
public class MailSendWithEml extends BaseTest {

    private String mid;
    private String subj;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox().all();

    @Test
    @Description("Отсылаем письмо\n" +
            "Прикрепляем это письмо к следующему в виде eml аттача\n" +
            "Проверяем, что письмо в полностью аттаче совпадает с отправленным\n"
            + "[DARIA-35043]")
    @Issue("DARIA-35043")
    public void sendEmlAttachShouldSeeEml() throws Exception {
        File attach = downloadFile(MailSendWithAttach.IMAGE_URl_JPEG, Util.getRandomString(), hc);
        SendUtils sendProd = sendWith.send(Util.getRandomString()).addAtts(attach).viaProd().waitDeliver()
                .send();
        mid = sendProd.getMid();
        subj = sendProd.getSubj();

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().addIds(mid).setSend(Util.getRandomString());
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String midLetterWithEml = waitWith.subj(msg.getSubj()).waitDeliver().getMid();

        String bid = api(Message.class).params(MessageObj.getMsgWithContentFlag(midLetterWithEml))
                .post().via(hc).getBidOfAttach(subj);

        Message msgOper = api(Message.class)
                .params(MessageObj.getMsg(midLetterWithEml).setHid(bid)).filters(new VDirectCut());

        Document testingEml = msgOper.get().via(hc).toDocument();
        Document prodEml = msgOper.setHost(props().productionHost()).get().via(hc).toDocument();

        assertThat("Сообщение в eml аттаче не совпадает с тем, которое прикрепляли [DARIA-35043]",
                testingEml, excludeNodes(equalToDoc(prodEml)
                        .exclude("//lang")));
    }
}
