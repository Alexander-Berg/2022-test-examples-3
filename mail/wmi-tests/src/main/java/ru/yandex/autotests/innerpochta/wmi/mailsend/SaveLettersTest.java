package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
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
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsSizeEqualMatcher.hasSameSizeAs;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabelName;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.mailsend.MailSendReplyTest.messageInDraftShouldBeSeen;

@Aqua.Test
@Title("Отправка писем. Сохранение письма")
@Description("Сохранение различных писем")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.DRAFT})
@Credentials(loginGroup = "Group1")
public class SaveLettersTest extends BaseTest {
    private static final int AMOUNT_OF_RECIPIENTS = 43;
    private static String subj;

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .all().draft();

    /**
     * Метод генерирует строчку, содержащую
     * AMOUNT_OF_RECIPIENTS адресов
     *
     * @throws Exception *
     */
    private String generateLongRecipientList(List<String> addresses) {
        String result = "";
        String randomMail;
        for (int i = 0; i < AMOUNT_OF_RECIPIENTS; i++) {
            randomMail = Util.getRandomAddress();
            addresses.add(randomMail);
            result += addresses.get(i) + ", ";
        }
        return result;
    }

    private void checkIfAllMailsEquals(List<String> addresses, List<String> savedAddresses) {
        assertThat(addresses, hasSameSizeAs(savedAddresses));
        for (String adr : addresses) {
            assertThat(String.format(" В сохраненных адрессах не нашлось %s ", adr),
                    savedAddresses, hasItem(adr));
        }
    }

    @Test
    @Issue("DARIA-2958")
    @Title("Сохранение в черновик с длинным получателем")
    @Description("Сохраняет письмо в черновик с длинными получателями\n" +
            "и проверяет что адреса не изменились\n" +
            "DARIA-2958")
    public void testLongRecipientList() throws Exception {
        List<String> addresses = new ArrayList<String>();
        String to = generateLongRecipientList(addresses);
        subj = Util.getRandomString();
        //сохраняем письмо в черновик
        MailSendMsgObj msg = msgFactory
                .getEmptyObj().setSubj(subj)
                .setNosend("yes")
                .setAutosave("yes")
                .setTo(to);

        api(MailSend.class).params(msg).post().via(hc);
        //достаем письмо из черновиков
        String mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        // заодно проверяем что мы сохранили в черновик
        Message msj = api(Message.class)
                .params(MessageObj.getMsg(mid))
                .post().via(hc);
        List<String> savedAddresses = msj.toEmailList();
        logger.warn("проверяем, что после сохранения в черновик,адресс сохранился");
        checkIfAllMailsEquals(addresses, savedAddresses);
    }

    //DARIA-38012 русские логины нельзя
    @Test
    @Issue("DARIA-38012")
    @Title("Сохранение в черновик с получателем с русским адрессом")
    @Description("Проверяем, что не ломаем кодировку в TO lol@админка.пдд, при сохранении в черновики [DARIA-36661]")
    public void saveLetterWithRussianAddressTo() throws Exception {
        String addressTo = "lol@админка.пдд";
        subj = Util.getRandomString();
        //сохраняем письмо в черновик
        MailSendMsgObj msg = msgFactory
                .getEmptyObj().setSubj(subj)
                .setNosend("yes")
                .setAutosave("yes")
                .setTo(addressTo);
        api(MailSend.class).params(msg).post().via(hc);

        //достаем письмо из черновиков
        String mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        Message msj = api(Message.class).params(MessageObj.getMsg(mid).setXmlVersion(XMLVERSION_DARIA2)).post().via(hc);
        List<String> savedAddresses = msj.toEmailList();

        MessageBody msgOper = jsx(MessageBody.class)
                .params(MessageObj.getMsg(mid)).filters(new VDirectCut()).get().via(hc);

        assertThat("Сломалась кодировка TO в message_body",
                msgOper.getToName(), equalTo(addressTo));

        assertThat("Сломалась кодировка EMAIL в messages_body",
                msgOper.getToEmail(), equalTo(addressTo));

        assertThat("Сломалась кодировка TO в message",
                msj.toEmail(), equalTo(addressTo));

        logger.warn("проверяем, что после сохранения в черновик,адресс сохранился");
        assertThat(String.format(" В сохраненных адрессах не нашлось %s ", addressTo),
                savedAddresses, hasItem(addressTo));
    }

    @Test
    @Issue("DARIA-38012")
    @Title("Сохранение в черновик с получателем с русским символами")
    @Description("Проверяем, что не ломаем кодировку в TO, при сохранении в черновики [DARIA-36661]")
    public void saveLetterWithRussianTo() throws Exception {
        String addressTo = "няняня";
        subj = Util.getRandomString();
        //сохраняем письмо в черновик
        MailSendMsgObj msg = msgFactory.getEmptyObj().setSubj(subj).setNosend("yes")
                .setAutosave("yes")
                .setTo(addressTo);
        api(MailSend.class).params(msg).post().via(hc);

        //достаем письмо из черновиков
        String mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        Message msj = api(Message.class)
                .params(MessageObj.getMsg(mid).setXmlVersion(XMLVERSION_DARIA2))
                .post().via(hc);

        MessageBody msgOper = jsx(MessageBody.class)
                .params(MessageObj.getMsg(mid)).filters(new VDirectCut()).get().via(hc);

        String to = msgOper.getToName();
        assertThat("Сломалась кодировка TO в message_body",
                to, equalTo(addressTo));

        String email = msgOper.getToEmail();
        assertThat("Сломалась кодировка EMAIL в messages_body",
                email, equalTo(""));

        assertThat("Сломалась кодировка TO в ответе wmi-ручки message",
                msj.toName(), equalTo(addressTo));
    }

    @Test
    @Issue("DARIA-2900")
    @Title("Уведомление об отправке")
    @Description("Сохраняет письмо в черновик с уведомлением об отправке\n" +
            "и проверяет, что есть метка.\n" +
            "DARIA-2900")
    public void testSavingNotificationOption() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("test")
                .setAutosave("yes")
                .setNosend("yes")
                .setNotifyOnSend("yes");
        subj = msg.getSubj();

        api(MailSend.class).params(msg).post().via(hc);
        String mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        assertThat("Нет метки delivery_confirmation", hc, hasLabelName("delivery_confirmation"));
    }

    @Test
    @Issue("DARIA-33159")
    @Title("Проверка ids, при сохранении с уведомлением")
    @Description("Сохраняет письмо в черновик с уведомлением об отправке\n" +
            "и проверяет, что правильно отображается ids\n" +
            "[DARIA-33159]")
    public void testWithNoSendShouldSeeIds() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("test")
                .setNosend("yes");
        String ids = api(MailSend.class).params(msg).post().via(hc).getIdsValue();
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        assertThat("[DARIA-33159] Неправильно отображаем <ids> ", ids, equalTo(mid));
    }

    @Test
    @Issues({@Issue("DARIA-45255"), @Issue("MAILPG-383"), @Issue("MAILPG-383")})
    @Title("Облегченное сохранение в черновик")
    @Description("Проверка облегченного сохранения в черновики.\n" +
            "Дважды сохраняем письмо в черновик.\n" +
            "Меняем текст при сохранении в черновики (облегченное сохранение).\n" +
            "Падали с internal_error")
    public void lightweightSaveToDraft() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSubj("double save in draft (1) " + Util.getRandomString()).setSend(Util.getRandomString())
                .setNosend("yes").setTtypePlain();

        jsx(MailSend.class).params(msg).post().via(hc);
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        msg.setIgnOverwrite("no").setOverwrite(mid).setSend(Util.getRandomString());

        api(MailSend.class).params(msg).post().via(hc).resultOk("Падали с internal_error. [DARIA-44745]");
        String midAfterApiSave = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(midAfterApiSave, folderList.draftFID(), hc);
        assertThat("Изменился мид письма после облегченного сохранения методом api [MAILPG-383]", midAfterApiSave, equalTo(mid));

        jsx(MailSend.class).params(msg).post().via(hc).statusOk("Падали с internal_error. [DARIA-44745]");
        String midAfterJsxSave = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(midAfterJsxSave, folderList.draftFID(), hc);
        assertThat("Изменился мид письма после облегченного сохранения методом jsx [MAILPG-383]", midAfterJsxSave, equalTo(mid));
    }

    @Test
    @Issues({@Issue("DARIA-45255"), @Issue("MAILPG-383")})
    @Title("Повторное сохранение в черновики")
    @Description("Проверка сохранения в черновики.\n" +
            "Дважды сохраняем письмо в черновик.")
    public void doubleSaveToDraft() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSubj("double save in draft (1) " + Util.getRandomString()).setSend(Util.getRandomString())
                .setNosend("yes").setTtypePlain();

        jsx(MailSend.class).params(msg).post().via(hc);
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);
        msg.setIgnOverwrite("no").setOverwrite(mid).setSubj("double save in draft (2) " + Util.getRandomString())
                .setSend(Util.getRandomString());
        api(MailSend.class).params(msg).post().via(hc).resultOk();
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String mid2 = waitWith.subj(msg.getSubj()).count(2).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid2, folderList.draftFID(), hc);
    }

    @Test
    @Issue("DARIA-44745")
    @Title("Сохранение в черновики множества раз")
    @Description("Сохраняем письмо в черновики 10 раз.\n" +
            "Каждый раз проверяем письмо в черновиках.")
    public void saveToDraftManyTimes() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setNosend("yes").setTtypePlain();
        api(MailSend.class).params(msg).post().via(hc);
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);

        for (int i = 0; i < 10; i++) {
            msg.setIgnOverwrite("no").setOverwrite(mid).setSubj("subj " + i);
            api(MailSend.class).params(msg).post().via(hc).resultOk();
            jsx(MailSend.class).params(msg).post().via(hc).statusOk();
            waitWith.subj(msg.getSubj()).count(2).inFid(folderList.draftFID()).waitDeliver();
        }
    }

    @Test
    @Issues({@Issue("DARIA-45255"), @Issue("MAILPG-383")})
    @Title("Облегченное сохранение в черновики множества раз")
    @Description("Сохраняем письмо в черновики 10 раз.\n" +
            "Каждый раз проверяем письмо в черновиках, сохраняем с новым текстов (облегченное сохранение).\n" +
            "Падали с internal_error")
    public void lightweightSaveToDraftManyTimes() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setNosend("yes").setTtypePlain();
        api(MailSend.class).params(msg).post().via(hc);
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(mid, folderList.draftFID(), hc);

        for (int i = 0; i < 10; i++) {
            msg.setIgnOverwrite("no").setOverwrite(mid).setSend(Util.getRandomString());

            api(MailSend.class).params(msg).post().via(hc).resultOk("Падали с internal_error. [DARIA-44745]");
            waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver();
            String midAfterApiSave = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
            assertThat("Изменился мид письма после облегченного сохранения методом api [MAILPG-383]", midAfterApiSave, equalTo(mid));

            jsx(MailSend.class).params(msg).post().via(hc).statusOk("Падали с internal_error. [DARIA-44745]");
            waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver();
            String midAfterJsxSave = waitWith.subj(msg.getSubj()).inFid(folderList.draftFID()).waitDeliver().getMid();
            assertThat("Изменился мид письма после облегченного сохранения методом jsx [MAILPG-383]", midAfterJsxSave, equalTo(mid));
        }

    }

}