package ru.yandex.autotests.innerpochta.wmi.threads;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView.threadsViewJsxDaria2;

@Aqua.Test
@Title("Тестирование тредов. Общее тестирование")
@Description("Перемещаем из тредов письма в разные папки, считаем оставшиеся")
@Features(MyFeatures.WMI)
@Stories(MyStories.THREADS)
@Credentials(loginGroup = "ThreadIntegrity")
@RunWith(DataProviderRunner.class)
public class ThreadIntegrity extends BaseTest {

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).all().inbox().outbox();

    @Test
    @Description("Проверяем различные свойства треда:\n" +
            "- При отправке самому себе - писем в треде в 2 раза больше отправленных\n" +
            "- Все письма в треде с одинаковой темой")
    public void shouldHaveTwiceAsManyAsSentToSelfMessagesInThread() throws Exception {
        int numberOfMessages = Util.getRandomShortInt() + 2;
        String subject = sendWith.viaProd().count(numberOfMessages).waitDeliver().send().getSubj();

        String threadId = jsx(ThreadsView.class).post().via(hc).getThreadId(subject);
        ThreadList respThreadList = jsx(ThreadList.class).params(ThreadListObj.getThread(threadId)).post().via(hc);

        assertThat("Количество писем в треде неверное, ожидалось: " + numberOfMessages * 2,
                respThreadList.countMessagesInThread(), equalTo(numberOfMessages * 2));
        assertTrue("Все письма в треде должны иметь одинаковую тему",
                respThreadList.allMessagesHasTheSameSubject(subject));
    }

    @Test
    @Description("Проверяем различные свойства треда:\n" +
            "- Количество писем в треде уменьшается при перемещении писем в спам")
    public void shouldReduceThreadCountWhenMoveMessageToSpam() throws Exception {
        int numberOfMessages = Util.getRandomShortInt() + 2;
        String subject = sendWith.viaProd().count(numberOfMessages).waitDeliver().send().getSubj();

        List<String> mids = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveOneMsg(mids.get(0), folderList.spamFID(), folderList.defaultFID()))
                .post().via(hc);
        waitWith.subj(subject).inFid(folderList.spamFID()).waitDeliver();

        String threadId = jsx(ThreadsView.class).post().via(hc).getThreadId(subject);
        ThreadList respThreadList = jsx(ThreadList.class).params(ThreadListObj.getThread(threadId)).post().via(hc);

        assertThat("Количество писем в треде должно было уменьшиться на 1 после перемещения в спам одного письма",
                respThreadList.countMessagesInThread(), equalTo(numberOfMessages * 2 - 1));
    }

    @Test
    @Description("Проверяем различные свойства треда:\n" +
            "- Количество писем в треде уменьшается при перемещении писем в удаленные")
    public void shouldReduceThreadCountWhenDeleteMessage() throws Exception {
        int numberOfMessages = Util.getRandomShortInt() + 2;
        String subject = sendWith.viaProd().count(numberOfMessages).waitDeliver().send().getSubj();

        List<String> mids = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveOneMsg(mids.get(1), folderList.deletedFID(), folderList.defaultFID()))
                .post().via(hc);
        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        String threadId = jsx(ThreadsView.class).post().via(hc).getThreadId(subject);
        ThreadList respThreadList = jsx(ThreadList.class).params(ThreadListObj.getThread(threadId)).post().via(hc);

        assertThat("Количество писем в треде должно было уменьшиться на 1 после перемещения в удаленные одного письма",
                respThreadList.countMessagesInThread(), equalTo(numberOfMessages * 2 - 1));
    }

    @Test
    @Issue("Не переехал из очереди WMI, утерян безвозвратно")
    @Description("Смотрит на атрибут att_count в разных методах\n" +
            "Серия из тестов с разным количеством аттачей\n" +
            "Несколько тестов из-за непонятной ошибки при доступе к threads_view и парсинга значения")
    @DataProvider({
        "0", "1", "3"
    })
    public void isAllMethodsReturnEqualAttCnt(int attCnt) throws Exception {
        logger.warn("Проверка правильного количества аттачей в att_count в thread_list, threads_view при attCnt="
                + attCnt);
        String subject = sendMailWithAtt(attCnt);

        MailBoxList respMBoxList = jsx(MailBoxList.class)
                .params(MailBoxListObj.empty().setPageNumber("1")).post().via(hc);
        String mid = respMBoxList.getMidOfMessage(subject);

        String threadId = jsx(ThreadsView.class).post().via(hc).getThreadId(subject);
        ThreadList respThreadList = jsx(ThreadList.class).params(ThreadListObj.getThread(threadId)).post().via(hc);

        int mboxAttCnt = respMBoxList.getAttCount(mid);
        int tListAttCnt = respThreadList.getAttCount(mid);
        int tViewAttCnt = jsx(ThreadsView.class).post().via(hc).getAttCount(mid);

        String errMsg = "Методы возвращают разное количество в параметре att_count, при количестве аттачей: ";
        assertThat(errMsg + attCnt + " (mailbox_list)", mboxAttCnt, equalTo(attCnt));
        assertThat(errMsg + attCnt + " (tread_list)", tListAttCnt, equalTo(attCnt));
        assertThat(errMsg + attCnt + " (treads_view)", tViewAttCnt, equalTo(attCnt));
    }

    /**
     * Отправка письма с аттачем
     *
     * @param attToSend - сколько аттачей прикрепить
     * @return - String - Тема письма
     * @throws Exception *
     */
    private String sendMailWithAtt(int attToSend) throws Exception {
        String fileName = "someAttach";
        File attach = Util.generateRandomShortFile(fileName, 64);

        MailSendMsgObj msgObj = msgFactory.getSimpleEmptySelfMsg()
                .setSend("MailSendAttForThread::sendMailWithAtt()" + Util.getRandomString());

        for (int i = 0; i < attToSend; i++) {
            msgObj.addAtts("application", attach);
        }

        api(MailSend.class).params(msgObj).post().via(hc);

        waitWith.subj(msgObj.getSubj()).waitDeliver();
        return msgObj.getSubj();
    }

    @Test
    @Issue("DARIA-44430")
    @Title("Должны менять scn треда при перемещении писем в папках")
    public void shouldChangeThreadScnWhenMoveLetter() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().send();
        String tid = sendUtils.getTid();
        String scn = threadsViewJsxDaria2().post().via(hc).getScn(tid);
        String mid = jsx(MailBoxList.class).params(MailBoxListObj.inFid(folderList.sentFID())).post().via(hc)
                .getMidOfMessage(sendUtils.getSubj());

        MailboxOper.move(mid, folderList.draftFID(), folderList.defaultFID()).post().via(hc);

        String scnAfterMove = threadsViewJsxDaria2().post().via(hc).getScn(tid);

        assertThat("Значение <thread_scn> не поменялось DARIA-44430", scn, not(CoreMatchers.equalTo(scnAfterMove)));
    }

    @Test
    @Issues({@Issue("DARIA-44430"), @Issue("MOBILEMAIL-5797")})
    @Title("Должны менять scn при ответе на письмо (одно письмо из треда помечено меткой hdr_status)")
    public void shouldChangeThreadScnWhenMarkIsReply() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().send();
        String tid = sendUtils.getTid();
        String scn = threadsViewJsxDaria2().post().via(hc).getScn(tid);
        String mid = mailboxListJsx().params(MailBoxListObj.inFid(folderList.sentFID())).post().via(hc)
                .getMidOfMessage(sendUtils.getSubj());

        Mops.mark(authClient, new MidsSource(mid), ApiMark.StatusParam.REPLIED)
                .post(shouldBe(okSync()));

        String scnAfterReplied = threadsViewJsxDaria2().post().via(hc).getScn(tid);

        assertThat("Значение <thread_scn> не поменялось DARIA-44430", scn, not(CoreMatchers.equalTo(scnAfterReplied)));
    }

    @Test
    @Issues({@Issue("DARIA-44430"), @Issue("MOBILEMAIL-5797")})
    @Title("Должны менять scn треда при форварде письма")
    public void shouldChangeThreadScnWhenMarkIsForwared() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().send();
        String tid = sendUtils.getTid();
        String scn = threadsViewJsxDaria2().post().via(hc).getScn(tid);
        String mid = mailboxListJsx().params(MailBoxListObj.inFid(folderList.sentFID())).post().via(hc)
                .getMidOfMessage(sendUtils.getSubj());

        Mops.mark(authClient, new MidsSource(mid), ApiMark.StatusParam.FORWARDED)
                .post(shouldBe(okSync()));

        String scnAfterReplied = threadsViewJsxDaria2().post().via(hc).getScn(tid);

        assertThat("Значение <thread_scn> не поменялось DARIA-44430", scn, not(CoreMatchers.equalTo(scnAfterReplied)));
    }

}
