package ru.yandex.autotests.innerpochta.wmi.threads;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MessagesWithLabelMatcher.hasThreadWithLabels;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.inFid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsLabelCreateObj.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToUnlabelOneLabel.messageToUnlabelOneLabel;
import static ru.yandex.qatools.matchers.collection.ContainsUniqueItems.containsUniqueItems;

@Aqua.Test
@Title("Тестирование тредов. Треды и метки")
@Description("Метим метками треды различным образом")
@Features(MyFeatures.WMI)
@Stories({MyStories.THREADS, MyStories.LABELS})
@Credentials(loginGroup = "ThreadAndLabel")
public class ThreadAndLabel extends BaseTest {

    @Rule
    public RuleChain clearing = outerRule(CleanMessagesRule.with(authClient).all().inbox().outbox())
            .around(DeleteLabelsRule.with(authClient).all());

    @Parameter("Имя метки")
    private String labelName;
    private String subject;
    private String lid;
    private String mid;
    
    @Rule
    public ExternalResource prepare = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            labelName = Util.getRandomString();
            subject = Util.getRandomString();
            lid = jsx(SettingsLabelCreate.class)
                    .params(newLabel(labelName))
                    .post().via(hc).updated();

            mails = Util.getRandomShortInt() + 2;
            mid = sendWith.subj(subject).viaProd().count(mails).waitDeliver().send().getMid();
        }
    };
    private int mails;

    @Test
    @Issues({@Issue("DARIA-24147"), @Issue("DARIA-21792")})
    @Title("Должны помечать тред меткой по множеству мидов и снимать метку если со всех писем сняли")
    public void shouldLabelAllMessagesInThread() throws Exception {
        markAllMessagesInThread(subject, lid);

        assertThat(hc, hasThreadWithLabels(subject, hasItem(lid)));

        List<String> mids = midsInboxOutbox(subject);

        messageToUnlabelOneLabel(labelMessages(mids, lid)).post().via(hc);

        assertThat(hc, hasThreadWithLabels(subject, not(hasItem(lid))));
    }

    @Test
    @Issue("MAILPG-296")
    @Title("Должны пометить тред одной меткой при пометке одного письма")
    public void shouldLabelThreadWhenLabelOneMessage() throws Exception {
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
        assumeThat(format("Письмо с mid:%s не пометилось меткой:%s", mid, labelName), hc, hasMsgWithLid(mid, lid));

        assertThat(hc, hasThreadWithLabels(subject, both(hasItem(lid)).and(containsUniqueItems())));
    }
    
    @Test
    @Issue("DARIA-21792")
    @Title("Не должны снимать метку с треда если сняли ее с одного письма в треде")
    @Description("Отправка нескольких писем с одинаковой темой\n" +
            "Пометка всех писем в папке входящих в треде\n" +
            "Проверка что ВСЕ помечены в треде\n" +
            "Снятие метки с одного письма, проверка что тред с меткой")
    public void shouldSaveLabelOnThreadEvenIfUnlabelOneMessage() throws Exception {
        List<String> mids = mailboxListJsx()
                .post().via(hc)
                .getMidsOfMessagesWithSubject(subject);

        messageToLabel(labelMessages(mids, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasThreadWithLabels(subject, hasItem(lid)));

        messageToUnlabelOneLabel(labelOne(mids.get(0), lid)).post().via(hc);

        assertThat("Тред не должен был потерять метку", hc, 
                hasThreadWithLabels(subject, both(hasItem(lid)).and(containsUniqueItems())));
    }

    @Test
    @Title("При приходе нового письма в помеченный тред, он не должен терять метку")
    public void shouldNotRemoveLabelIfAddOneMoreMsgToLabelledThread() throws Exception {
        markAllMessagesInThread(subject, lid);

        sendWith.subj(subject).count(1).viaProd().send();
        waitWith.subj(subject).count(mails + 1).waitDeliver();

        assertThat(hc, hasThreadWithLabels(subject, hasItem(lid)));
    }


    /**
     * Пометка всех сообщений в треде
     *
     * @param subject тема треда
     * @return ид метки
     * @throws Exception *
     */
    private void markAllMessagesInThread(String subject, String lid) throws IOException {
        List<String> mids = midsInboxOutbox(subject);
        messageToLabel(labelMessages(mids, lid)).post().via(hc).errorcodeShouldBeEmpty();
    }


    public List<String> midsInboxOutbox(String subject) throws IOException {
        List<String> mids = mailboxListJsx(inFid(folderList.defaultFID()))
                .post().via(hc)
                .getMidsOfMessagesWithSubject(subject);

        mids.addAll(mailboxListJsx(inFid(folderList.sentFID()))
                .post().via(hc)
                .getMidsOfMessagesWithSubject(subject));
        return mids;
    }
}

