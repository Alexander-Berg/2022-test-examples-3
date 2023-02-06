package ru.yandex.autotests.innerpochta.sendbernar;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.CancelSendUndoResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendDelayedResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendMessageResponse;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreSshTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.qatools.allure.annotations.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cthul.matchers.CthulMatchers.all;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.EMAIL_BCC;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.EMAIL_CC;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.EMAIL_TO;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.MID;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.MSG_ID;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.OPERATION;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.STATE;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule.withGrepAllLogsFor;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

@Aqua.Test
@Title("[LOGS] TSKV лог для sendbernar")
@Description("tskv лог - " + TskvSendbernarTest.TSKV_LOG_PATH)
@Features({MyFeatures.SENDBERNAR, MyFeatures.LOGGING})
@Stories({MyStories.JOURNAL, "TSKV", MyStories.LOGS})
@Credentials(loginGroup = "TskvSendbernarTest")
@Issues({@Issue("MPROTO-1871"), @Issue("MAILPG-2466")})
@IgnoreSshTest
public class TskvSendbernarTest extends BaseSendbernarClass {

    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(URI.create(props().sendbernarUri()), props().getRobotGerritWebmailTeamSshKey());

    @Rule
    public LogCollectRule logs = withGrepAllLogsFor(sshAuthRule);

    static final String TSKV_LOG_PATH = "/var/log/sendbernar/user_journal.tskv";

    @Test
    @Title("При send операции должны писать message-id, операцию и пустой mid")
    public void shouldLogMessageIdAndEmptyMidOnSend() {
        String msgId = sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        waitWith.inbox().subj(subj).waitDeliver();

        List<Matcher<Map<? extends String, ? extends String>>> logMatchers =
                newArrayList(
                        entry(STATE, all(
                                containsString("message-id="),
                                containsString("to="),
                                containsString("source_mid="),
                                containsString("subject="+subj))),
                        entry(MSG_ID, is(msgId)),
                        not(entry(MID, is(""))),
                        entry(OPERATION, is("send"))
                );

        List<String> greps = Arrays.asList(getUid(), subj);
        shouldSeeLogLine(logMatchers, greps);
    }

    @Test
    @Title("При отправке письма пишем поля to, cc, bcc, mid и msgId")
    public void shouldLogSourceMidAndToCcBccAddresses() {
        String to = authClient.acc().getSelfEmail();
        String cc = authClient.acc().getLogin()+"@yandex.kz_";
        String bcc = authClient.acc().getLogin()+"@yandex.by_";

        String sourceMid = saveDraft()
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        String msgId = sendMessage()
                .withTo(to)
                .withCc(cc)
                .withBcc(bcc)
                .withSourceMid(sourceMid)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        waitWith.inbox().subj(subj).waitDeliver();


        List<Matcher<Map<? extends String, ? extends String>>> logMatchers =
                newArrayList(
                        entry(MSG_ID, is(msgId)),
                        entry(EMAIL_TO, is(to)),
                        entry(EMAIL_CC, is(cc)),
                        entry(EMAIL_BCC, is(bcc)),
                        entry(MID, is(sourceMid))
                );

        List<String> greps = Arrays.asList(getUid(), subj);
        shouldSeeLogLine(logMatchers, greps);
    }

    @Test
    @Title("Пишем mid отменяемого письма при отмене отправки")
    public void shouldLogMidOnUndo() {
        String mid = sendUndo()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withSendTime(String.valueOf(SECONDS.toSeconds(5)))
                .post(shouldBe(ok200()))
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();


        boolean allDone = cancelSendUndo()
                .withMid(mid)
                .post(shouldBe(ok200()))
                .as(CancelSendUndoResponse.class)
                .getAllDone();

        assertTrue("allDone должно содержать значение true", allDone);


        List<Matcher<Map<? extends String, ? extends String>>> logMatchers =
                newArrayList(
                        entry(MID, is(mid)),
                        entry(OPERATION, is("undo_sending"))
                );

        List<String> greps = Arrays.asList(getUid(), "target=mailbox", mid);
        shouldSeeLogLine(logMatchers, greps);
    }

    private Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1}")
    private static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers,
                                         List<String> greps) {
        assertThat(sshAuthRule.ssh().conn(), should(logEntryShouldMatch(logMatchers, TSKV_LOG_PATH, greps))
                .whileWaitingUntil(timeoutHasExpired(3000).withPollingInterval(500)));
    }
}
