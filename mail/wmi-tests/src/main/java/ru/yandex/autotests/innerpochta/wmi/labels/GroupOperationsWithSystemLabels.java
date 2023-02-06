package ru.yandex.autotests.innerpochta.wmi.labels;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidsInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Labels.labels;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.move;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs.deleteWithMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelDelete.deleteLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.DeleteUtils.deleteMsgsBySubjFromInboxSent;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 06.04.15
 * Time: 18:54
 */
@Aqua.Test
@Title("[LABELS] Действия с системными метками")
@Description("Проверяем что не можем удалить/создать/переименовать системные метки")
@Features(MyFeatures.WMI)
@Stories({MyStories.LABELS, MyStories.PINS})
@Credentials(loginGroup = "GroupOperationsWithSystemLabels")
@RunWith(Parameterized.class)
public class GroupOperationsWithSystemLabels extends BaseTest {

    public static final int COUNT_OF_LETTERS = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<LabelSymbol> data() {
        return asList(LabelSymbol.DELIVERY_CONFIRMATION,
                LabelSymbol.REMINDME_AVIAETICKET,
                LabelSymbol.REMINDME_THREADABOUT_MARK,
                LabelSymbol.PRIORITY_HIGH,
                //DARIA-44825
                LabelSymbol.PINNED_LABEL
        );
    }

    @Parameterized.Parameter
    public LabelSymbol labelSymbol;

    private String subject;
    private String lid;

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Rule
    public RuleChain clearLabels = new LogConfigRule().around(DeleteLabelsRule.with(authClient).all());

    @Before
    public void prepare() throws Exception {
        lid = labels().post().via(hc).lidBySymbol(labelSymbol);
        subject = getRandomString();
    }


    @Test
    @Title("Метка должна оставаться на письме при перемещении в другую папку")
    public void moveLabeledMessageToFolder() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        String newFid = newFolder(getRandomString()).post().via(hc).updated();

        move(mid, newFid, folderList.defaultFID()).post().via(hc);

        assertThat(format("Письмо с mid: %s не содержит метку: %s после перемещения в папку: %s", mid, lid, newFid),
                hc, hasMsgWithLidsInFolder(mid, newFid, lid));

        deleteWithMsgs(newFid);
    }

    @Test
    @Issue("MAILPG-317")
    @Title("Письмо должно сбросить метку в спаме и восстановить обратно по возвращении")
    public void moveLabeledMessageToSpam() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        move(mid, folderList.spamFID(), folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.spamFID()).waitDeliver();
        assertThat(format("Письмо с mid:%s после перемещения в спам сбросило метку:%s", mid, lid),
                hc, hasMsgWithLidInFolder(mid, folderList.spamFID(), lid));

        //перемещаем обратно:
        move(mid, folderList.defaultFID(), folderList.spamFID()).post().via(hc);

        waitWith.subj(subject).waitDeliver();

        assertThat(format("Письмо с mid:%s после восстановления из спама не вернуло метку:%s", mid, lid),
                hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Issues({@Issue("DARIA-3851"), @Issue("MAILPG-317")})
    @Title("Письмо должно сбросить метку в удаленных и восстановить обратно по возвращении")
    public void moveLabeledMessageToDeleted() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        move(mid, folderList.deletedFID(), folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        assertThat(format("Письмо с mid: %s после перемещения в удаленные сбросило метку: %s", mid, lid),
                hc, hasMsgWithLidInFolder(mid, folderList.deletedFID(), lid));

        //перемещаем обратно:
        move(mid, folderList.defaultFID(), folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).waitDeliver();

        assertThat("Письмо с mid:" + mid + " после восстановления из спама не вернуло метку: " + lid,
                hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Issue("DARIA-4359")
    @Title("Метка на треде ставится на все письма кроме тех что в папке удаленных")
    public void markWithDeletedMessage() throws Exception {
        SendUtils sendUtils = sendWith.subj(subject).count(COUNT_OF_LETTERS).waitDeliver().viaProd().send();
        String deletedFid = folderList.deletedFID();

        String mid = sendUtils.getMids().get(0);
        logger.warn("MID письма в \"Удаленных\"" + mid);
        List<String> midsWithLabel = sendUtils.getMids();
        midsWithLabel.remove(0);

        move(mid, deletedFid, folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        messageToLabel(labelMessages(sendUtils.getMids(), lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("На письмо с mid: %s из папки <Удаленные> поставили метку %s [DARIA-4359]", mid, lid),
                hc, not(hasMsgWithLidsInFolder(mid, deletedFid, lid)));
        assertThat("На письмах из папки <Входящих> метка не появилась [DARIA-4359] ",
                hc, hasMsgsWithLid(midsWithLabel, lid));
    }

    @After
    public void deleteMsg() throws Exception {
        deleteMsgsBySubjFromInboxSent(subject);
        // удаление метки
        deleteLabel(lid).post().via(hc);
    }
}
