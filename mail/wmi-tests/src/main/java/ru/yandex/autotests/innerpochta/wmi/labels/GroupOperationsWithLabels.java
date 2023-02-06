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
 * Группо операций с метками (спасибо, кэп)
 * DARIA-3851
 */
@Aqua.Test
@Title("Групповые операции с метками и письмами")
@Description("Различные действия, связанные с метками и группой писем")
@Features(MyFeatures.WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "GroupOperationsWithLabels")
public class GroupOperationsWithLabels extends BaseTest {

    public static final int COUNT_OF_LETTERS = 2;

    private String subject;
    private String lid;

    @Parameter("Имя метки")
    private String labelName;

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Rule
    public RuleChain clearLabels = new LogConfigRule().around(DeleteLabelsRule.with(authClient).all());

    /**
     * Инициализация
     * Создание новой метки
     *
     * @throws Exception *
     */
    @Before
    public void prepare() throws Exception {
        subject = Util.getRandomString();
        // Новая метка
        labelName = Util.getRandomString();
        lid = newLabel(labelName).post().via(hc).updated();
    }

    @Test
    @Title("Должны пометить одно письмо меткой")
    @Description("Посылка 1 письма, получение его мид\n" +
            "Создание метки со случайным именем и получение lid\n" +
            "Пометка сообщения меткой\n" +
            "- Проверка что письмо пометилось меткой\n" +
            "Чистка")
    public void shouldLabelOneMessage() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
        assertThat(format("Письмо с mid:%s не пометилось меткой:%s", mid, labelName), hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("Должны пометить меткой целиковый тред через множество мидов")
    @Description("Отправка случайного небольшого количества писем\n" +
            "Создание метки со случайным именем\n" +
            "Пометка всех писем треда меткой\n" +
            "- Проверка что все письма пометились")
    public void shouldLabelThreadWithMidsArray() throws Exception {
        List<String> mids = sendWith.subj(subject).count(COUNT_OF_LETTERS).viaProd().waitDeliver()
                .send().getMids();

        messageToLabel(labelMessages(mids, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("Не все письма с темой:%s пометились меткой:%s", subject, labelName), hc,
                hasMsgsWithLid(mids, lid));
    }

    @Test
    @Title("Должны помечать весь тред меткой по тиду")
    @Description("Отправка случайного небольшого количества писем\n" +
            "Создание метки со случайным именем\n" +
            "Пометка всех писем треда меткой ПО tid-у\n" +
            "- Проверка что все письма пометились")
    @Issue("MAILPG-263")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldLabelThreadByTid() throws Exception {
        List<String> mids = sendWith.subj(subject).count(COUNT_OF_LETTERS).viaProd().waitDeliver()
                .send().getMids();

        String tid = api(ThreadsView.class).post().via(hc).getThreadId(subject);
        messageToLabel(labelThread(tid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("Не все письма с темой:%s пометились меткой:%s", subject, labelName), hc,
                hasMsgsWithLid(mids, lid));
    }

    @Test
    @Title("Должны сохранять метку на письме при ее переименовании")
    @Description("Создание метки со случайным именем и получение ее lid\n" +
            "Отправка 1 простого письма и получение его мид\n" +
            "Пометка письма меткой\n" +
            "Переименование метки в новое случайное имя\n" +
            "- Проверка что письма все еще содержат метку")
    public void shouldSaveLabelOnRenameIt() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        String newLabelName = Util.getRandomString();

        jsx(SettingsLabelRename.class).params(renameLabelObj(lid, newLabelName))
                .post().via(hc);

        assertThat(format("Письмо с mid:%s не пометилось меткой:%s", mid, labelName), hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("Должны сохранять метку на письме при его перемещении в другую папку")
    @Description("Создание метки со случайным именем и получение ее lid\n" +
            "Отправка 1 письма и получение его mid\n" +
            "Создание папки со случайным именем и получение ее fid\n" +
            "Перемещение полученного письма в новую папку\n" +
            "- Проверка что метка на письме все еще есть")
    public void shouldSaveLabelOnMoveMessageToAnotherFolder() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        String folderName = Util.getRandomString();
        String newFid = newFolder(folderName).post().via(hc).updated();

        move(mid, newFid, folderList.defaultFID()).post().via(hc);

        assertThat(format("Письмо с mid:%s не содержит метку:%s после перемещения в папку:%s", mid, labelName, newFid),
                hc, hasMsgWithLidsInFolder(mid, newFid, lid));

        deleteWithMsgs(newFid);
    }

    @Test
    @Title("Должны снимать/восстанавливать метку перемещении в спам и обратно")
    @Description("Создание метки\n" +
            "Посылка 1 письма\n" +
            "Пометка его новосозданной меткой\n" +
            "Перемещение письма в папку спам\n" +
            "- Проверка что метки на письме НЕТ\n" +
            "Возврат письма обратно во входящие\n" +
            "Ожидание перемещения письма\n" +
            "- Проверка что письмо с меткой")
    public void shouldRemoveAndRestoreLabelAfterMovingToSpam() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        // Метка важные
        String warnLabel = jsx(Labels.class).post().via(hc).lidByName(WmiConsts.LABEL_PRIORITY_HIGH);

        // Пользовательская метка
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
        // Помечаем важным
        messageToLabel(labelOne(mid, warnLabel)).post().via(hc).errorcodeShouldBeEmpty();

        move(mid, folderList.spamFID(), folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.spamFID()).waitDeliver();

        assertThat(format("Письмо с mid:%s после перемещения в спам сбросило метку:%s", mid, labelName),
                hc, hasMsgWithLidInFolder(mid, folderList.spamFID(), lid));


        move(mid, folderList.defaultFID(), folderList.spamFID()).post().via(hc);


        waitWith.subj(subject).waitDeliver();

        assertThat(format("Письмо с mid:%s после восстановления из спама не вернуло метку:%s", mid, labelName),
                hc, hasMsgWithLids(mid, newArrayList(lid, warnLabel)));
    }

    @Test
    @Issue("DARIA-3851")
    @Title("Должны снимать/восстанавливать метку перемещении в удаленные и обратно")
    @Description("Отправка одного письма\n" +
            "Пометка его новосозданной меткой\n" +
            "Перемещение в папку удаленные\n" +
            "- Проверка что метка снялась\n" +
            "Возвращение письма во ВХОДЯЩИЕ\n" +
            "- Проверка что метка вернулась\n" +
            "DARIA-3851")
    public void shouldRemoveAndRestoreLabelAfterMoveToTrash() throws Exception {
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        api(MessageToLabel.class).params(labelOne(mid, lid)).post().via(hc);

        String newFid = folderList.deletedFID();

        move(mid, newFid, folderList.defaultFID()).post().via(hc);

        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        assertThat(format("Письмо с mid:%s после перемещения в удаленные сбросило метку:%s", mid, labelName),
                hc, hasMsgWithLidInFolder(mid, folderList.deletedFID(), lid));

        move(mid, folderList.defaultFID(), newFid).post().via(hc);

        waitWith.subj(subject).waitDeliver();

        assertThat(format("Письмо с mid:%s после восстановления из спама не вернуло метку:%s", mid, labelName),
                hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Issue("DARIA-4359")
    @Title("Должны ставить метку на на все, кроме удаленного сообщения, при пометке целикового треда")
    @Description("Ставим метку на группу сообщений в том числе и на удаленное сообщение " +
            "-> должны поставить метку на сообщения на все кроме удаленного [DARIA-4359]")
    public void shouldNotLabelMessageInDeletedWhenLabelWholeThread() throws Exception {
        SendUtils sendUtils = sendWith.subj(subject).count(COUNT_OF_LETTERS).waitDeliver().viaProd().send();

        String mid = sendUtils.getMids().get(0);
        logger.warn("MID письма в \"Удаленных\"" + mid);
        List<String> midsWithLabel = sendUtils.getMids();
        midsWithLabel.remove(0);

        move(mid, folderList.deletedFID(), folderList.defaultFID()).post().via(hc);
        waitWith.subj(subject).inFid(folderList.deletedFID()).waitDeliver();

        messageToLabel(labelMessages(sendUtils.getMids(), lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat(format("На письмо с mid: %s из папки \"Удаленные\" поставили метку %s [DARIA-4359]", mid, labelName),
                hc, not(hasMsgWithLidsInFolder(mid, folderList.deletedFID(), lid)));
        assertThat("На письмах из папки \"Входящих\" метка не появилась [DARIA-4359] ",
                hc, hasMsgsWithLid(midsWithLabel, lid));
    }

    @Test
    @Title("Должны снимать метку с одного сообщения")
    @Description("Отправка одного письма\n" +
            "Пометка его новосозданной меткой\n" +
            "Снятие этой метки\n" +
            "- Проверка что метка снялась")
    public void shouldUnlabelOneMessage() throws Exception {
        logger.warn("Проверка снятия метки");
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        api(MessageToUnlabelOneLabel.class)
                .params(labelOne(mid, lid))
                .post().via(hc);

        assertThat(format("Письмо с mid:%s не сбросило метку:%s", mid, labelName), hc,
                not(hasMsgWithLid(mid, lid)));
    }

    @After
    public void deleteMsg() throws Exception {
        deleteMsgsBySubjFromInboxSent(subject);
    }

}
