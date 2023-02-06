package ru.yandex.autotests.innerpochta.mops;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import lombok.val;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.tskv.SoTskv;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreSshTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Headers;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.junitextensions.rules.retry.RetryRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.innerpochta.beans.tskv.AbuseTypeTskv.UNREAD_TRASH;
import static ru.yandex.autotests.innerpochta.beans.tskv.MsgStatusTskv.READ;
import static ru.yandex.autotests.innerpochta.beans.tskv.OperTskv.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.TargetTskv.FOLDERS;
import static ru.yandex.autotests.innerpochta.beans.tskv.TargetTskv.MESSAGE;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.ShinglerPattern.Sources.TEST;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.08.15
 * Time: 13:47
 */
@Aqua.Test
@Title("[LOGS] TSKV лог для mops - репорт в спам")
@Description("tskv mops лог - " + TskvMopsTest.TSKV_MOPS_LOG_PATH)
@Features({MyFeatures.MOPS, MyFeatures.LOGGING})
@Stories({MyStories.JOURNAL, "TSKV", MyStories.LOGS})
@Credentials(loginGroup = "TskvMopsTest")
@Issues({@Issue("DARIA-46776"), @Issue("DARIA-53336")})
@RunWith(DataProviderRunner.class)
@IgnoreSshTest
public class TskvMopsTest extends MopsBaseTest {
    @ClassRule
    public static SSHAuthRule sshMopsAuthRule = sshOn(URI.create(props().mopsHost()), props().getRobotGerritWebmailTeamSshKey());

    @Rule
    public RetryRule retry = RetryRule.retry().ifException(IndexOutOfBoundsException.class)
            .times(5).every(5, TimeUnit.SECONDS);

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    public static final String TSKV_MOPS_LOG_PATH = "/app/log/user_journal.tskv";
    public static final int COUNT_OF_LETTERS = 2;

    private static Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    private static Matcher<Map<? extends String, ? extends String>> entry(SoTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    private static Matcher<Map<? extends String, ? extends String>> optEntry(WmiTskv key, Matcher<String> value) {
        return anyOf(hasEntry(is(key.toString()), value), not(hasKey(key.toString())));
    }

    private static ArrayList<String> operation(String uid, String mid, String operationName) {
        return newArrayList(uid, mid, "operation="+operationName);
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Должны отсылать репорт при удалении двух непрочитанных писем")
    public void shouldWriteReportsFromDeleteRequests() throws Exception {
        final List<String> mids = sendMail(COUNT_OF_LETTERS).mids();
        final String uid = authClient.account().uid();
        final String mid1 = mids.get(0);
        final String mid2 = mids.get(1);
        remove(new MidsSource(mids)).post(shouldBe(okSync()));

        shouldSeeLogLine(getMoveLogMatcher(mid1), operation(uid, mid1, MOVE.toString()));
        shouldSeeLogLine(getMoveLogMatcher(mid2), operation(uid, mid2, MOVE.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid1), operation(uid, mid1, TRASH.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid2), operation(uid, mid2, TRASH.toString()));
        shouldSeeLogLine(getAbuseLogMatcher(mid1), operation(uid, mid1, ABUSE.toString()));
        shouldSeeLogLine(getAbuseLogMatcher(mid2), operation(uid, mid2, ABUSE.toString()));
    }

    @Test
    @Issue("MAILDEV-32")
    @Title("Удаление папки должно логироваться как delete")
    public void shouldLogDeleteOperationWhenDeleteFolder() {
        final String folderName = getRandomString();
        final String uid = authClient.account().uid();
        final String fid = createFolder(folderName).post(shouldBe(okFid())).then().extract().body().path("fid");
        deleteFolder(fid).post(shouldBe(okSync()));

        shouldSeeLogLine(getCreateLogMatcher(fid, folderName), newArrayList(uid, "fid=" + fid), 0);
        shouldSeeLogLine(getDeleteLogMatcher(fid), newArrayList(uid, "fid=" + fid), 1);
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Не должны отсылать репорт при удалении двух прочитанных писем")
    public void shouldNotWriteReportsFromDeleteRequests() throws Exception {
        final List<String> mids = sendMail(COUNT_OF_LETTERS).mids();
        mark(new MidsSource(mids), ApiMark.StatusParam.READ).post(shouldBe(okSync()));

        final String uid = authClient.account().uid();
        final String mid1 = mids.get(0);
        final String mid2 = mids.get(1);
        remove(new MidsSource(mids)).post(shouldBe(okSync()));

        shouldSeeLogLine(getMarkReadLogMatcher(mid1), operation(uid, mid1, MARK.toString()));
        shouldSeeLogLine(getMarkReadLogMatcher(mid2), operation(uid, mid2, MARK.toString()));

        shouldSeeLogLine(getMoveLogMatcher(mid1), operation(uid, mid1, MOVE.toString()));
        shouldSeeLogLine(getMoveLogMatcher(mid2), operation(uid, mid2, MOVE.toString()));

        shouldSeeLogLine(getTrashLogMatcher(mid1), operation(uid, mid1, TRASH.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid2), operation(uid, mid2, TRASH.toString()));

        shouldNotSeeLogLine("unread_trash;" + mid1);
        shouldNotSeeLogLine("unread_trash;" + mid2);
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Должны отсылать репорт при удалении 1 прочитанного и одного непрочитанного письма")
    public void shouldWriteReportWithTwoLetters() throws Exception {
        final List<String> mids = sendMail(COUNT_OF_LETTERS).mids();
        final String mid1 = mids.get(0);
        final String mid2 = mids.get(1);
        final String uid = authClient.account().uid();

        mark(new MidsSource(mid1), ApiMark.StatusParam.READ).post(shouldBe(okSync()));
        remove(new MidsSource(mids)).post(shouldBe(okSync()));

        shouldSeeLogLine(getMarkReadLogMatcher(mid1), operation(uid, mid1, MARK.toString()));
        shouldSeeLogLine(getMoveLogMatcher(mid1), operation(uid, mid1, MOVE.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid1), operation(uid, mid1, TRASH.toString()));

        shouldSeeLogLine(getMoveLogMatcher(mid2), operation(uid, mid2, MOVE.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid2), operation(uid, mid2, TRASH.toString()));
        shouldSeeLogLine(getAbuseLogMatcher(mid2), operation(uid, mid2, ABUSE.toString()));

        shouldNotSeeLogLine("unread_trash;" + mid1);
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Не должны отсылать репорт при удалении прочитанного письма")
    public void shouldNotWriteReportFromDeleteRequest() throws Exception {
        final String mid = sendMail().firstMid();
        final String uid = authClient.account().uid();
        mark(new MidsSource(mid), ApiMark.StatusParam.READ).post(shouldBe(okSync()));
        remove(new MidsSource(mid)).post(shouldBe(okSync()));

        shouldSeeLogLine(getMarkReadLogMatcher(mid), operation(uid, mid, MARK.toString()));
        shouldSeeLogLine(getMoveLogMatcher(mid), operation(uid, mid, MOVE.toString()));
        shouldSeeLogLine(getTrashLogMatcher(mid), operation(uid, mid, TRASH.toString()));
        shouldNotSeeLogLine("unread_trash;" + mid);
    }


    @Test
    @Issue("DARIA-53895")
    @Title("Удаляем письмо через mops. Пробрасываем хэдер эксперимента")
    public void shouldWriteExpInMops() throws Exception {
        final String mid = sendMail().firstMid();
        final String exp = "experiment_" + getRandomString();
        final String uid = authClient.account().uid();

        remove(new MidsSource(mid))
            .withReq(req -> req.addHeader(Headers.EXP_BOXES, exp))
            .post(shouldBe(okSync()));

        shouldSeeLogLine(getMoveHeadLogMatcher(mid, TEST_BUCKETS, exp), operation(uid, mid, MOVE.toString()));
        shouldSeeLogLine(getAbuseExpLogMatcher(mid, TEST_BUCKETS, exp), operation(uid, mid, ABUSE.toString()));
        shouldSeeLogLine(getTrashExpLogMatcher(mid, TEST_BUCKETS, exp), operation(uid, mid, TRASH.toString()));
    }

    @Test
    @Issue("MAILDEV-291")
    @Title("Удаляем письмо через mops. Пробрасываем хэдер enabled эксперимента")
    public void shouldWriteEnableExpInMops() throws Exception {
        final String mid = sendMail().firstMid();
        final String exp = "experiment_" + getRandomString();
        final String uid = authClient.account().uid();

        remove(new MidsSource(mid))
            .withReq(req -> req.addHeader(Headers.ENABLED_EXP_BOXES, exp))
            .post(shouldBe(okSync()));

        shouldSeeLogLine(getMoveHeadLogMatcher(mid, ENABLED_TEST_BUCKETS, exp), operation(uid, mid, MOVE.toString()));
        shouldSeeLogLine(getTrashExpLogMatcher(mid, ENABLED_TEST_BUCKETS, exp), operation(uid, mid, TRASH.toString()));
        shouldSeeLogLine(getAbuseExpLogMatcher(mid, ENABLED_TEST_BUCKETS, exp), operation(uid, mid, ABUSE.toString()));
    }

    @Test
    @Issue("MAILDEV-8")
    @Title("Удаляем письмо через MOPS. Пробрасываем хэдер модуля. Проверяем user_journal.tskv MOPS-а")
    public void shouldWriteClientTypeInMops() throws Exception {
        final String mid = sendMail().firstMid();
        final String moduleHead = getRandomString();
        final String uid = authClient.account().uid();

        remove(new MidsSource(mid))
            .withReq(req -> req.addHeader(Headers.CLIENT_TYPE, moduleHead))
            .post(shouldBe(okSync()));

        shouldSeeLogLine(getMoveHeadLogMatcher(mid, CLIENT_TYPE, moduleHead), operation(uid, mid, MOVE.toString()));
        shouldSeeLogLine(getTrashExpLogMatcher(mid, CLIENT_TYPE, moduleHead), operation(uid, mid, TRASH.toString()));
        shouldSeeLogLine(getAbuseExpLogMatcher(mid, CLIENT_TYPE, moduleHead), operation(uid, mid, ABUSE.toString()));
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Должны отсылать репорт при удалении непрочитанного письма")
    public void shouldWriteReportFromDeleteRequest() throws Exception {
        final String mid = sendMail().firstMid();
        final String uid = authClient.account().uid();
        remove(new MidsSource(mid)).post(shouldBe(okSync()));

        shouldSeeLogLine(getMoveLogMatcher(mid), operation(uid, mid, MOVE.toString()));
        //DARIA-53336
        shouldSeeLogLine(getTrashLogMatcher(mid), operation(uid, mid, TRASH.toString()));
        shouldSeeLogLine(getAbuseLogMatcher(mid), operation(uid, mid, ABUSE.toString()));
    }

    @Test
    @Issue("MAILDEV-986")
    @Title("Проверяем, что в лог пробрасывается параметр icookie")
    public void shouldWriteICookie() throws Exception {
        final String mid = sendMail().firstMid();
        final String uid = authClient.account().uid();
        final String icookie = getRandomString();
        mark(new MidsSource(mid), ApiMark.StatusParam.READ)
            .withReq(req -> req.addQueryParam("icookie", icookie))
            .post(shouldBe(okSync()));

        shouldSeeLogLine(getIcookieLogMatcher(mid, icookie), newArrayList(uid, mid), 0);
    }

    @Test
    @Issue("MAILPG-2574")
    @Title("Должны не логировать фидбэк о смене таба при пометке спамом")
    public void shouldNotWriteSoReportForChangeTabOnMarkSpam() throws Exception {
        Envelope message = sendMail().message();
        Mops.spam(authClient, new MidsSource(message.getMid())).withSource(TEST.getSource())
                .post(shouldBe(okSync()));

        shouldNotSeeLogLine(Arrays.asList(CHANGE_SO_TYPE.toString(), message.getStid()));
    }

    @Test
    @Issue("MAILPG-2574")
    @Title("Должны не логировать фидбэк о смене таба при удалении письма")
    public void shouldNotWriteSoReportForChangeTabOnRemove() throws Exception {
        Envelope message = sendMail().message();
        Mops.remove(authClient, new MidsSource(message.getMid())).withSource(TEST.getSource())
                .post(shouldBe(okSync()));

        shouldNotSeeLogLine(Arrays.asList(CHANGE_SO_TYPE.toString(), message.getStid()));
    }

    @Test
    @Issue("MAILPG-2574")
    @Title("Должны залогировать фидбэк о смене таба при пометке неспамом")
    public void shouldWriteSoReportForChangeTabOnUnspam() throws Exception {
        final String uid = authClient.account().uid();
        Envelope message = sendMail().message();
        String dstTab = Tabs.Tab.RELEVANT.getName();
        Mops.spam(authClient, new MidsSource(message.getMid())).withSource(TEST.getSource())
                .post(shouldBe(okSync()));
        Mops.unspam(authClient, new MidsSource(message.getMid()))
                .withSource(TEST.getSource())
                .withDestTab(dstTab)
                .post(shouldBe(okSync()));

        List<String> grepPatterns = newArrayList(uid, message.getMid(), message.getStid(), DEST_TAB.toString());
        shouldSeeLogLine(getChangeSoTypeLogMatcher(uid, message, Optional.empty(), Optional.of(dstTab)), grepPatterns, 0);
    }

    @Test
    @Issue("MAILPG-2574")
    @Title("Должны залогировать фидбэк о смене таба при смене таба при муве")
    public void shouldWriteSoReportForChangeTabOnMoveToTab() throws Exception {
        final String uid = authClient.account().uid();
        Envelope message = sendMail().message();
        String dstTab = Tabs.Tab.SOCIAL.getName();
        Mops.complexMove(authClient, folderList.defaultFID(), new MidsSource(message.getMid()))
                .withSource(TEST.getSource())
                .withDestTab(dstTab)
                .post(shouldBe(okSync()));

        List<String> grepPatterns = newArrayList(uid, message.getMid(), message.getStid(), DEST_TAB.toString());
        val matchers = getChangeSoTypeLogMatcher(uid, message, Optional.of(message.getTab()), Optional.of(dstTab));
        shouldSeeLogLine(matchers, grepPatterns, 0);
    }

    @Test
    @Issue("MAILPG-2574")
    @Title("Должны не залогировать фидбэк о смене таба при complexMove без смены таба")
    public void shouldNotWriteSoReportForChangeTabOnMoveWithoutDestTab() throws Exception {
        Envelope message = sendMail().message();
        Mops.complexMove(authClient, folderList.defaultFID(), new MidsSource(message.getMid()))
                .withSource(TEST.getSource())
                .post(shouldBe(okSync()));

        shouldNotSeeLogLine(Arrays.asList(CHANGE_SO_TYPE.toString(), message.getStid()));
    }

    @Issue("DARIA-47290")
    private static List<Matcher<Map<? extends String, ? extends String>>> getRequiredFields() {
        return newArrayList(
            entry(TSKV_FORMAT, is("mail-user-journal-tskv-log")),
            entry(UNIXTIME, notNullValue(String.class))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getMarkReadLogMatcher(String mid) {
        return joinLogMatchers(getRequiredFields(),
            newArrayList(entry(OPERATION, equalTo(MARK.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, containsString(mid)),
                entry(MSG_STATUS, equalTo(READ.toString())),
                entry(STATE, containsString("read;")),
                entry(STATE, containsString(mid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getMoveLogMatcher(String mid) {
        return joinLogMatchers(getRequiredFields(),
            newArrayList(entry(OPERATION, equalTo(MOVE.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, containsString(mid)),
                entry(DEST_FID, equalTo(folderList.deletedFID())),
                entry(STATE, containsString(String.format("fid=%s;", folderList.deletedFID()))),
                entry(STATE, containsString(mid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getTrashLogMatcher(String mid) {
        return joinLogMatchers(getRequiredFields(),
            newArrayList(entry(OPERATION, equalTo("trash")),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, containsString(mid)),
                entry(STATE, containsString(mid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getAbuseLogMatcher(String mid) {
        return joinLogMatchers(getRequiredFields(),
            newArrayList(entry(OPERATION, equalTo(ABUSE.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, containsString(mid)),
                entry(ABUSE_TYPE, equalTo(UNREAD_TRASH.toString())))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getCreateLogMatcher(String fid, String folderName) {
        return joinLogMatchers(getRequiredFields(),
                newArrayList(entry(OPERATION, equalTo(CREATE.toString())),
                        entry(TARGET, containsString(FOLDERS.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(STATE, equalTo(folderName)),
                        entry(FID, equalTo(fid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getDeleteLogMatcher(String fid) {
        return joinLogMatchers(getRequiredFields(),
                newArrayList(entry(OPERATION, equalTo(DELETE.toString())),
                        entry(TARGET, containsString(FOLDERS.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(STATE, equalTo(fid)),
                        entry(FID, equalTo(fid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getChangeSoTypeLogMatcher(String uid,
            Envelope message, Optional<String> srcTab, Optional<String> dstTab) {

        List<Matcher<Map<? extends String, ? extends String>>> matchers = new ArrayList<>(Arrays.asList(
                entry(OPERATION, equalTo(CHANGE_SO_TYPE.toString())),
                entry(UID, containsString(uid)),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(STATE, equalTo(message.getMid())),
                entry(STID, equalTo(message.getStid())),
                entry(RECEIVED_DATE, equalTo(message.getReceiveDate().toString())),
                entry(MID, equalTo(message.getMid()))
        ));

        srcTab.ifPresent(tab -> matchers.add(entry(SRC_TAB, equalTo(tab))));
        dstTab.ifPresent(tab -> matchers.add(entry(DEST_TAB, equalTo(tab))));

        return joinLogMatchers(getRequiredFields(), matchers);
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getMoveHeadLogMatcher(String mid,
                                                                                         WmiTskv header, String exp) {
        List<Matcher<Map<? extends String, ? extends String>>> result = getMoveLogMatcher(mid);
        result.add(entry(header, containsString(exp)));
        return result;
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getTrashExpLogMatcher(String mid,
                                                                                         WmiTskv header, String exp) {
        List<Matcher<Map<? extends String, ? extends String>>> result = getTrashLogMatcher(mid);
        result.add(entry(header, containsString(exp)));
        return result;
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getAbuseExpLogMatcher(String mid,
                                                                                         WmiTskv header, String exp) {
        List<Matcher<Map<? extends String, ? extends String>>> result = getAbuseLogMatcher(mid);
        result.add(entry(header, containsString(exp)));
        return result;
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getIcookieLogMatcher(
            String mid, String icookie) {
        return joinLogMatchers(getRequiredFields(),
            newArrayList(entry(I_COOKIE, equalTo(icookie)),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, containsString(mid)),
                entry(STATE, containsString(mid)))
        );
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getChangeTabLogMatcher(
            Envelope message, List<String> removedSoTypes, List<String> addSoTypes) {
        return joinLogMatchers(getRequiredFields(),
                newArrayList(entry(OPERATION, equalTo(CHANGE_SO_TYPE.toString())),
                        entry(TARGET, containsString(MESSAGE.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(MID, equalTo(message.getMid())),
                        entry(STID, equalTo(message.getStid())),
                        entry(RECEIVED_DATE, equalTo(message.getReceiveDate().toString())),
                        optEntry(REMOVE_SO_TYPES, containsAllStrings(removedSoTypes)),
                        optEntry(ADD_SO_TYPES, containsAllStrings(addSoTypes)),
                        entry(STATE, containsString("change_so_type")),
                        entry(STATE, containsString(message.getMid())))
        );
    }

    private static Matcher<String> containsAllStrings(List<String> substrings) {
        return allOf(substrings.stream()
                .map(Matchers::containsString)
                .toArray(Matcher[]::new));
    }

    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1} (номер записи {2})")
    private static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers,
                                         List<String> greps, Integer entry) {
        assertThat(sshMopsAuthRule.ssh().conn(), should(logEntryShouldMatch(logMatchers, TSKV_MOPS_LOG_PATH, greps, entry))
            .whileWaitingUntil(timeoutHasExpired(3000).withPollingInterval(500)));
    }

    private static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers,
                                         List<String> greps) {
        shouldSeeLogLine(logMatchers, greps, 0);
    }

    @Step("[SSH]: Не должно быть записей в логе {0}, грепая по {1}")
    private static void shouldNotSeeLogLine(String grep) throws IOException {
        String res = sshMopsAuthRule.ssh().cmd(String.format("less %s | grep -c \"%s\"", TSKV_MOPS_LOG_PATH, grep));
        assertThat("Не должно быть записей в логе", res, containsString("0"));
    }

    @Step("[SSH]: Не должно быть записей в логе {0}, грепая по {1}")
    private static void shouldNotSeeLogLine(List<String> grep) throws IOException {
        String args = grep.stream()
                .reduce("", (a, p) -> a + "-e \" " + p + "\" ");

        String res = sshMopsAuthRule.ssh().cmd(String.format("less %s | grep -c %s", TSKV_MOPS_LOG_PATH, args));
        assertThat("Не должно быть записей в логе", res, containsString("0"));
    }

    private static List<Matcher<Map<? extends String,? extends String>>> joinLogMatchers(
        List<Matcher<Map<? extends String, ? extends String>>> first,
        List<Matcher<Map<? extends String, ? extends String>>> second)
    {
        List<Matcher<Map<? extends String, ? extends String>>> result = newArrayList();
        result.addAll(first);
        result.addAll(second);
        return result;
    }

}

