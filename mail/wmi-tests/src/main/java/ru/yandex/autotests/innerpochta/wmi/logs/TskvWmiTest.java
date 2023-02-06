package ru.yandex.autotests.innerpochta.wmi.logs;

import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderUpdateObj;
import ru.yandex.autotests.innerpochta.wmi.core.rules.SSHRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.TargetTskv.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.OperTskv.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelThread;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToUnlabelOneLabel.messageToUnlabelOneLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderRename.renameFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderUpdate.settingsFolderUpdate;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelRename.renameLabel;
import static ru.yandex.autotests.innerpochta.wmi.labels.LabelBasicOperationsTest.DEFAULT_COLOR;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;


/**
 * User: lanwen
 */
@Aqua.Test
@Title("TSKV лог - обязательные поля и значения")
@Description("tskv wmi лог - " + TskvWmiTest.TSKV_LOG_PATH)
@Features({MyFeatures.WMI, MyFeatures.LOGGING})
@Stories({MyStories.JOURNAL, "TSKV", MyStories.LOGS})
@Credentials(loginGroup = "Group2")
public class TskvWmiTest extends BaseTest {
    public static final String TSKV_LOG_PATH = "/var/log/wmi/user_journal.tskv";

    private static String subject;

    @ClassRule
    public static SSHRule ssh = new SSHRule();

    @Rule
    public RuleChain clearLabels = new LogConfigRule().around(DeleteLabelsRule.with(authClient).all());

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @BeforeClass
    public static void send() throws Exception {
        subject = new SendUtils(authClient).folderList(folderList).composeCheck(composeCheck).send().getSubj();
    }

    @Test
    @Issue("MAILDEV-349")
    @Title("При создании метки, должны писать действие в журнал")
    public void shouldSeeCreateLabelInJournal() throws IOException, InterruptedException {
        String name = Util.getRandomString();
        String lid = newLabel(name).post().via(hc).updated();
        List<Matcher<Map<? extends String, ? extends String>>> logMatchersCreate =
                newArrayList(
                        entry(OPERATION, equalTo(CREATE.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(TARGET, equalTo(LABELS.toString())),
                        entry(LABEL_NAME, equalTo(name)),
                        entry(LABEL_TYPE, equalTo("user")),
                        entry(LABEL_COLOR, equalTo(DEFAULT_COLOR)),
                        entry(LID, equalTo(lid))
                );

        shouldSeeLogLine(logMatchersCreate, name);
    }

    @Test
    @Issue("MAILDEV-349")
    @Title("При переименовании метки, должны писать действие в журнал")
    public void shouldSeeRenameLabelInJournal() throws IOException, InterruptedException {
        String name = Util.getRandomString();
        String lid = newLabel(name).post().via(hc).updated();
        String newName = Util.getRandomString();
        renameLabel(lid, newName).post().via(hc);
        List<Matcher<Map<? extends String, ? extends String>>> logMatchersRename =
                newArrayList(
                        entry(OPERATION, equalTo(RENAME.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(TARGET, equalTo(LABELS.toString())),
                        entry(LABEL_NAME, equalTo(newName)),
                        entry(LID, equalTo(lid))
                );
        shouldSeeLogLine(logMatchersRename, newName);
    }

    @Test
    @Issue("MAILDEV-349")
    @Title("При создании папки, должны писать в журнал")
    public void shouldSeeCreateFolderInJournal() throws IOException, InterruptedException {
        String name = Util.getRandomString();
        String fid = newFolder(name).post().via(hc).updated();
        List<Matcher<Map<? extends String, ? extends String>>> logMatchersCreate =
                newArrayList(
                        entry(OPERATION, equalTo(CREATE.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(TARGET, equalTo(FOLDERS.toString())),
                        entry(FOLDER_TYPE, equalTo("user")),
                        entry(FOLDER_NAME, equalTo(name)),
                        //у любой папки все равно есть ссылка на родительскую папку, если это корень, то parentFid=0
                        entry(PARENT_FID, equalTo("0")),
                        entry(FID, equalTo(fid))
                );
        shouldSeeLogLine(logMatchersCreate, name);
    }

    @Test
    @Issue("MAILDEV-349")
    @Title("При установке pop3, должны писать в журнал")
    public void shouldSeeSetPop3FolderInJournal() throws IOException, InterruptedException {
        settingsFolderUpdate(SettingsFolderUpdateObj.empty().addFid(folderList.defaultFID())).post().via(hc).ok();
        List<Matcher<Map<? extends String, ? extends String>>> logMatchersCreate =
                newArrayList(
                        entry(OPERATION, equalTo(SET_POP_3.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(TARGET, equalTo(FOLDERS.toString())),
                        entry(FIDS, equalTo(folderList.defaultFID()))
                );
        shouldSeeLogLine(logMatchersCreate, props().getCurrentRequestId());
    }

    @Test
    @Issue("MAILDEV-349")
    @Title("При переименовании папки, должны писать в журнал")
    public void shouldSeeRenameFolderInJournal() throws IOException, InterruptedException {
        String name = Util.getRandomString();
        String fid = newFolder(name).post().via(hc).updated();
        String newName = Util.getRandomString();
        renameFolder(fid, newName).post().via(hc);

        List<Matcher<Map<? extends String, ? extends String>>> logMatchersRename =
                newArrayList(
                        entry(OPERATION, equalTo(RENAME.toString())),
                        entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                        entry(TARGET, equalTo(FOLDERS.toString())),
                        entry(FOLDER_NAME, equalTo(newName)),
                        //у любой папки все равно есть ссылка на родительскую папку, если это корень, то parentFid=0
                        entry(PARENT_FID, equalTo("0")),
                        entry(FID, equalTo(fid))
                );
        shouldSeeLogLine(logMatchersRename, newName);
    }

    @Test
    @Issue("DARIA-49713")
    @Title("При установки метки важные hidden=0")
    public void shouldSeeWarmLabelInJournal() throws Exception {
        String mid = sendWith.viaProd().waitDeliver().send().getMid();
        messageToLabel(labelOne(mid, labels.priorityHigh())).post().via(hc).errorcodeShouldBeEmpty();

        List<Matcher<Map<? extends String, ? extends String>>> logMatchers =
                newArrayList(
                        entry(OPERATION, equalTo(LABEL.toString())),
                        //MAILDEV-349
                        entry(MIDS, equalTo(mid)),
                        entry(LIDS, equalTo(labels.priorityHigh())),
                        not(entry(HIDDEN, equalTo("0")))
                );

        shouldSeeLogLine(logMatchers, String.format("%s:=%s", mid, labels.priorityHigh()));
    }

    @Test
    @Issue("DARIA-47503")
    @Title("Не должны писать в лог авторизации от читающих запросов")
    public void shouldNotWriteAuthFromReadRequests() throws IOException, InterruptedException {
        List<Matcher<Map<? extends String, ? extends String>>> logMatchers =
                newArrayList(
                        not((Matcher) hasValue(equalTo("authorization")))
                );

        shouldSeeLogLine(logMatchers, "authorization");
    }

    @Test
    @Issue("MAILDEV-327")
    @Title("Должны отсылать репорт при пометке письма меткой для одного письма")
    public void shouldWriteReportFromLabelUnlabelMidRequest() throws Exception {
        String mid = sendWith.viaProd().waitDeliver().send().getMid();
        messageToLabel(labelOne(mid, labels.priorityHigh())).post().via(hc).errorcodeShouldBeEmpty();

        shouldSeeLogLine(getLabelMidLogMatcher(mid, labels.priorityHigh()),
                String.format("mids=%s:=%s", mid, labels.priorityHigh()));

        messageToUnlabelOneLabel(labelOne(mid, labels.priorityHigh())).post().via(hc);

        shouldSeeLogLine(getUnlabelMidLogMatcher(mid, labels.priorityHigh()),
                String.format("mids=%s!=%s", mid, labels.priorityHigh()));
    }

    @Test
    @Issue("MAILDEV-327")
    @Title("Должны отсылать репорт при пометке письма меткой для цепочек писем")
    public void shouldWriteReportFromLabelUnlabelTidRequest() throws Exception {
        String tid = sendWith.viaProd().waitDeliver().send().getTid();
        messageToLabel(labelThread(tid, labels.priorityHigh())).post().via(hc).errorcodeShouldBeEmpty();

        shouldSeeLogLine(getLabelTidLogMatcher(tid, labels.priorityHigh()),
                String.format("tids=%s:=%s", tid, labels.priorityHigh()));

        messageToUnlabelOneLabel(labelThread(tid, labels.priorityHigh())).post().via(hc);

        shouldSeeLogLine(getUnlabelTidLogMatcher(tid, labels.priorityHigh()),
                String.format("tids=%s!=%s", tid, labels.priorityHigh()));
    }

    //operation=label state=tids=157907461934678488:=18
    public List<Matcher<Map<? extends String, ? extends String>>> getLabelMidLogMatcher(String mid, String lid) {
        return newArrayList(entry(OPERATION, equalTo(LABEL.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, equalTo(mid)),
                entry(LIDS, equalTo(lid)),
                //MAILDEV-453
                entry(USER_AGENT, not(isEmptyOrNullString())),
                entry(STATE, containsString(String.format("mids=%s:=%s", mid, lid))));
    }

    public List<Matcher<Map<? extends String, ? extends String>>> getLabelTidLogMatcher(String tid, String lid) {
        return newArrayList(entry(OPERATION, equalTo(LABEL.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(TIDS, equalTo(tid)),
                entry(LIDS, equalTo(lid)),
                //MAILDEV-453
                entry(USER_AGENT, not(isEmptyOrNullString())),
                entry(STATE, containsString(String.format("tids=%s:=%s", tid, lid))));
    }

    //operation=unlabel	state=mids=159033361841531702!=1
    public List<Matcher<Map<? extends String, ? extends String>>> getUnlabelMidLogMatcher(String mid, String lid) {
        return newArrayList(entry(OPERATION, equalTo(UNLABEL.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(MIDS, equalTo(mid)),
                entry(LIDS, equalTo(lid)),
                //MAILDEV-453
                entry(USER_AGENT, not(isEmptyOrNullString())),
                entry(STATE, containsString(String.format("mids=%s!=%s", mid, lid))));
    }

    public List<Matcher<Map<? extends String, ? extends String>>> getUnlabelTidLogMatcher(String tid, String lid) {
        return newArrayList(entry(OPERATION, equalTo(UNLABEL.toString())),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(TIDS, equalTo(tid)),
                entry(LIDS, equalTo(lid)),
                //MAILDEV-453
                entry(USER_AGENT, not(isEmptyOrNullString())),
                entry(STATE, containsString(String.format("tids=%s!=%s", tid, lid))));
    }

    private Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1}")
    public static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers, String grep) {
        assertThat(ssh.conn(), should(logEntryShouldMatch(logMatchers, TSKV_LOG_PATH, grep))
                .whileWaitingUntil(timeoutHasExpired(3000).withPollingInterval(500)));
    }
}
