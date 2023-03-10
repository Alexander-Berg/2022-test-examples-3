package ru.yandex.autotests.innerpochta.wmi.labels;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToUnlabelOneLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelRename;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLids;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidsInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelThread;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsLabelRenameObj.renameLabelObj;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.move;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs.deleteWithMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.DeleteUtils.deleteMsgsBySubjFromInboxSent;

/**
 * ???????????? ???????????????? ?? ?????????????? (??????????????, ??????)
 * DARIA-3851
 */
@Aqua.Test
@Title("?????????????????? ???????????????? ?? ?????????????? ?? ????????????????")
@Description("?????????????????? ????????????????, ?????????????????? ?? ?????????????? ?? ?????????????? ??????????")
@Features(MyFeatures.WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "GroupOperationsWithLabels")
public class GroupOperationsWithLabels extends BaseTest {

    public static final int COUNT_OF_LETTERS = 2;

    private String subject;
    private String lid;

    @Parameter("?????? ??????????")
    private String labelName;

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Rule
    public RuleChain clearLabels = new LogConfigRule().around(DeleteLabelsRule.with(authClient).all());

    /**
     * ??????????????????????????
     * ???????????????? ?????????? ??????????
     *
     * @throws Exception *
     */
    @Before
    public void prepare() throws Exception {
        subject = Util.getRandomString();
        // ?????????? ??????????
        labelName = Util.getRandomString();
        lid = newLabel(labelName).post().via(hc).updated();
    }

    @Test
    @Title("???????????? ???????????????? ???????? ???????????? ????????????")
    @Description("?????????????? 1 ????????????, ?????????????????? ?????? ??????\n" +
            "???????????????? ?????????? ???? ?????????????????? ???????????? ?? ?????????????????? lid\n" +
            "?????????????? ?????????????????? ????????????\n" +
            "- ???????????????? ?????? ???????????? ???????????????????? ????????????\n" +
            "????????????")
    public void shouldLabelOneMessage() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
        assertThat(format("???????????? ?? mid:%s ???? ???????????????????? ????????????:%s", mid, labelName), hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("???????????? ???????????????? ???????????? ?????????????????? ???????? ?????????? ?????????????????? ??????????")
    @Description("???????????????? ???????????????????? ???????????????????? ???????????????????? ??????????\n" +
            "???????????????? ?????????? ???? ?????????????????? ????????????\n" +
            "?????????????? ???????? ?????????? ?????????? ????????????\n" +
            "- ???????????????? ?????? ?????? ???????????? ????????????????????")
    public void shouldLabelThreadWithMidsArray() throws Exception {
        List<String> mids = sendWith.subj(subject).count(COUNT_OF_LETTERS).viaProd().waitDeliver()
                .send().getMids();

        messageToLabel(labelMessages(mids, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("???? ?????? ???????????? ?? ??????????:%s ???????????????????? ????????????:%s", subject, labelName), hc,
                hasMsgsWithLid(mids, lid));
    }

    @Test
    @Title("???????????? ???????????????? ???????? ???????? ???????????? ???? ????????")
    @Description("???????????????? ???????????????????? ???????????????????? ???????????????????? ??????????\n" +
            "???????????????? ?????????? ???? ?????????????????? ????????????\n" +
            "?????????????? ???????? ?????????? ?????????? ???????????? ???? tid-??\n" +
            "- ???????????????? ?????? ?????? ???????????? ????????????????????")
    @Issue("MAILPG-263")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldLabelThreadByTid() throws Exception {
        List<String> mids = sendWith.subj(subject).count(COUNT_OF_LETTERS).viaProd().waitDeliver()
                .send().getMids();

        String tid = api(ThreadsView.class).post().via(hc).getThreadId(subject);
        messageToLabel(labelThread(tid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("???? ?????? ???????????? ?? ??????????:%s ???????????????????? ????????????:%s", subject, labelName), hc,
                hasMsgsWithLid(mids, lid));
    }

    @Test
    @Title("???????????? ?????????????????? ?????????? ???? ???????????? ?????? ???? ????????????????????????????")
    @Description("???????????????? ?????????? ???? ?????????????????? ???????????? ?? ?????????????????? ???? lid\n" +
            "???????????????? 1 ???????????????? ???????????? ?? ?????????????????? ?????? ??????\n" +
            "?????????????? ???????????? ????????????\n" +
            "???????????????????????????? ?????????? ?? ?????????? ?????????????????? ??????\n" +
            "- ???????????????? ?????? ???????????? ?????? ?????? ???????????????? ??????????")
    public void shouldSaveLabelOnRenameIt() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        String newLabelName = Util.getRandomString();

        jsx(SettingsLabelRename.class).params(renameLabelObj(lid, newLabelName))
                .post().via(hc);

        assertThat(format("???????????? ?? mid:%s ???? ???????????????????? ????????????:%s", mid, labelName), hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("???????????? ?????????????????? ?????????? ???? ???????????? ?????? ?????? ?????????????????????? ?? ???????????? ??????????")
    @Description("???????????????? ?????????? ???? ?????????????????? ???????????? ?? ?????????????????? ???? lid\n" +
            "???????????????? 1 ???????????? ?? ?????????????????? ?????? mid\n" +
            "???????????????? ?????????? ???? ?????????????????? ???????????? ?? ?????????????????? ???? fid\n" +
            "?????????????????????? ?????????????????????? ???????????? ?? ?????????? ??????????\n" +
            "- ???????????????? ?????? ?????????? ???? ???????????? ?????? ?????? ????????")
    public void shouldSaveLabelOnMoveMessageToAnotherFolder() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        String folderName = Util.getRandomString();
        String newFid = newFolder(folderName).post().via(hc).updated();

        move(mid, newFid, folderList.defaultFID()).post().via(hc);

        assertThat(format("???????????? ?? mid:%s ???? ???????????????? ??????????:%s ?????????? ?????????????????????? ?? ??????????:%s", mid, labelName, newFid),
                hc, hasMsgWithLidsInFolder(mid, newFid, lid));

        deleteWithMsgs(newFid);
    }

    @Test
    @Title("???????????? ??????????????/?????????????????????????????? ?????????? ?????????????????????? ?? ???????? ?? ??????????????")
    @Description("???????????????? ??????????\n" +
            "?????????????? 1 ????????????\n" +
            "?????????????? ?????? ?????????????????????????? ????????????\n" +
            "?????????????????????? ???????????? ?? ?????????? ????????\n" +
            "- ???????????????? ?????? ?????????? ???? ???????????? ??????\n" +
            "?????????????? ???????????? ?????????????? ???? ????????????????\n" +
            "???????????????? ?????????????????????? ????????????\n" +
            "- ???????????????? ?????? ???????????? ?? ????????????")
    public void shouldRemoveAndRestoreLabelAfterMovingToSpam() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        // ?????????? ????????????
        String warnLabel = jsx(Labels.class).post().via(hc).lidByName(WmiConsts.LABEL_PRIORITY_HIGH);

        // ???????????????????????????????? ??????????
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
        // ???????????????? ????????????
        messageToLabel(labelOne(mid, warnLabel)).post().via(hc).errorcodeShouldBeEmpty();

        move(mid, folderList.spamFID(), folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.spamFID()).waitDeliver();

        assertThat(format("???????????? ?? mid:%s ?????????? ?????????????????????? ?? ???????? ???????????????? ??????????:%s", mid, labelName),
                hc, hasMsgWithLidInFolder(mid, folderList.spamFID(), lid));


        move(mid, folderList.defaultFID(), folderList.spamFID()).post().via(hc);


        waitWith.subj(subject).waitDeliver();

        assertThat(format("???????????? ?? mid:%s ?????????? ???????????????????????????? ???? ?????????? ???? ?????????????? ??????????:%s", mid, labelName),
                hc, hasMsgWithLids(mid, newArrayList(lid, warnLabel)));
    }

    @Test
    @Issue("DARIA-3851")
    @Title("???????????? ??????????????/?????????????????????????????? ?????????? ?????????????????????? ?? ?????????????????? ?? ??????????????")
    @Description("???????????????? ???????????? ????????????\n" +
            "?????????????? ?????? ?????????????????????????? ????????????\n" +
            "?????????????????????? ?? ?????????? ??????????????????\n" +
            "- ???????????????? ?????? ?????????? ??????????????\n" +
            "?????????????????????? ???????????? ???? ????????????????\n" +
            "- ???????????????? ?????? ?????????? ??????????????????\n" +
            "DARIA-3851")
    public void shouldRemoveAndRestoreLabelAfterMoveToTrash() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        api(MessageToLabel.class).params(labelOne(mid, lid)).post().via(hc);

        String newFid = folderList.deletedFID();

        move(mid, newFid, folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        assertThat(format("???????????? ?? mid:%s ?????????? ?????????????????????? ?? ?????????????????? ???????????????? ??????????:%s", mid, labelName),
                hc, hasMsgWithLidInFolder(mid, folderList.deletedFID(), lid));

        move(mid, folderList.defaultFID(), newFid).post().via(hc);

        waitWith.subj(subject).waitDeliver();

        assertThat(format("???????????? ?? mid:%s ?????????? ???????????????????????????? ???? ?????????? ???? ?????????????? ??????????:%s", mid, labelName),
                hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Issue("DARIA-4359")
    @Title("???????????? ?????????????? ?????????? ???? ???? ??????, ?????????? ???????????????????? ??????????????????, ?????? ?????????????? ???????????????????? ??????????")
    @Description("???????????? ?????????? ???? ???????????? ?????????????????? ?? ?????? ?????????? ?? ???? ?????????????????? ?????????????????? " +
            "-> ???????????? ?????????????????? ?????????? ???? ?????????????????? ???? ?????? ?????????? ???????????????????? [DARIA-4359]")
    public void shouldNotLabelMessageInDeletedWhenLabelWholeThread() throws Exception {
        SendUtils sendUtils = sendWith.subj(subject).count(COUNT_OF_LETTERS).waitDeliver().viaProd().send();

        String mid = sendUtils.getMids().get(0);
        logger.warn("MID ???????????? ?? \"??????????????????\"" + mid);
        List<String> midsWithLabel = sendUtils.getMids();
        midsWithLabel.remove(0);

        move(mid, folderList.deletedFID(), folderList.defaultFID()).post().via(hc);
        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        messageToLabel(labelMessages(sendUtils.getMids(), lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("???? ???????????? ?? mid: %s ???? ?????????? \"??????????????????\" ?????????????????? ?????????? %s [DARIA-4359]", mid, labelName),
                hc, not(hasMsgWithLidsInFolder(mid, folderList.deletedFID(), lid)));
        assertThat("???? ?????????????? ???? ?????????? \"????????????????\" ?????????? ???? ?????????????????? [DARIA-4359] ",
                hc, hasMsgsWithLid(midsWithLabel, lid));
    }

    @Test
    @Title("???????????? ?????????????? ?????????? ?? ???????????? ??????????????????")
    @Description("???????????????? ???????????? ????????????\n" +
            "?????????????? ?????? ?????????????????????????? ????????????\n" +
            "???????????? ???????? ??????????\n" +
            "- ???????????????? ?????? ?????????? ??????????????")
    public void shouldUnlabelOneMessage() throws Exception {
        logger.warn("???????????????? ???????????? ??????????");
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        api(MessageToUnlabelOneLabel.class)
                .params(labelOne(mid, lid))
                .post().via(hc);

        assertThat(format("???????????? ?? mid:%s ???? ???????????????? ??????????:%s", mid, labelName), hc,
                not(hasMsgWithLid(mid, lid)));
    }

    @After
    public void deleteMsg() throws Exception {
        deleteMsgsBySubjFromInboxSent(subject);
    }

}
