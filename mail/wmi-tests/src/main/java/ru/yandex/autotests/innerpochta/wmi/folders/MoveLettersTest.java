package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.complexmove.ApiComplexMove;
import ru.yandex.autotests.innerpochta.wmi.core.mops.complexmove.ApiComplexMove.WithSentParam;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.TidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;
import java.util.Optional;

import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.move;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.moveTid;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs.deleteWithMsgs;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.09.14
 * Time: 17:52
 *
 * DARIA-35802
 */
@Aqua.Test
@Title("Проверяем ручку mailbox_oper c параметром move")
@Description("Перемещаем письмо по mid-ам и tid-ам")
@Features(MyFeatures.WMI)
@Stories(MyStories.LETTERS)
@Issue("DARIA-35802")
@Credentials(loginGroup = "MoveLettersTest")
public class MoveLettersTest extends BaseTest {

    private String userFolderName = Util.getRandomString();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all();

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .all().allfolders();

    public static final int COUNT_OF_MAILS = 3;


    /**
     * Перемещает письма из ВХОДЯЩИХ в заданную папку
     *
     * @param newFid - фид папки для перемещения
     * @throws Exception *
     */
    private void moveLettersToFolderOnMids(String newFid, String subject) throws Exception {
        List<String> mids = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        // Перемещение писем в новую папку
        move(mids, newFid, folderList.defaultFID()).post().via(hc).resultIdOk();
        waitWith.subj(subject).count(mids.size()).inFid(newFid).waitDeliver();

        // Проверка что все переместились
        int moved = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(newFid))
                .post().via(hc).countMessagesInFolderWithSubj(subject);

        assertEquals("Количество перемещенных сообщений не совпадает с ожидаемым. Ожидалось: " + mids.size(),
                mids.size(), moved);

        jsx(SettingsFolderClear.class).params(SettingsFolderClearObj
                .purgeFid(newFid))
                .post().via(hc);

        assertEquals("Количество писем в выдаче не соответсвует ожидаемому", 0,
                jsx(MailBoxList.class)
                        .params(MailBoxListObj.inFid(newFid))
                        .post().via(hc).countMessagesInFolderWithSubj(subject).intValue());
        assertEquals("Значение атрибута msg_count не соответствует ожидаемому", 0,
                jsx(MailBoxList.class)
                        .params(MailBoxListObj.inFid(newFid))
                        .post().via(hc).getMessageCounter().intValue());
    }

    @Test
    @Description("Создание новой папки и перемещение туда предварительно высланных писем\n" +
            "- Проверка что все переместились\n" +
            "- Проверяет заодно выдачу атрибута msg_count у мейлбокс-листа")
    public void moveLetters() throws Exception {
        logger.warn("Перемещение в новую папку");
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_MAILS).send();
        List<String> mids = sendUtils.getMids();
        String subject = sendUtils.getSubj();

        // Создание папки
        String folderName = Util.getRandomString();
        String newFid = newFolder(folderName).post().via(hc).updated();

        // Перемещение писем в новую папку
        move(mids, newFid, folderList.defaultFID()).post().via(hc).resultIdOk();
        waitWith.subj(subject).count(mids.size()).and().inFid(newFid).waitDeliver();

        // Проверка что все переместились
        // Заодно контроль атрибута msg_count
        int moved = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(newFid))
                .post().via(hc).getMessageCounter();

        assertEquals("Не все письма были перемещены. Ожидалось: " + mids.size() + " перемещено: " + moved,
                mids.size(), moved);
        // Чистка
        jsx(SettingsFolderClear.class).params(SettingsFolderClearObj
                .purgeFid(newFid)).post().via(hc);

        assertEquals("Количество писем в выдаче не соответсвует 0", 0,
                jsx(MailBoxList.class)
                        .params(MailBoxListObj.inFid(newFid))
                        .post().via(hc).countMessagesInFolderWithSubj(subject).intValue());

        assertEquals("Значение атрибута msg_count не соответствует ожидаемому", 0,
                jsx(MailBoxList.class)
                        .params(MailBoxListObj.inFid(newFid))
                        .post().via(hc).getMessageCounter().intValue());

        deleteWithMsgs(newFid).post().via(hc);

        assertFalse("Созданная папка не удалена. Папки " + newFid + " не должно было остаться",
                api(FolderList.class)
                        .post().via(hc).isThereFolder(folderName));
    }

    @Test
    @Description("Перемещения писем из ВХОДЯЩИХ в различные папки")
    public void moveLettersFromDefaultFolderToAnother() throws Exception {
        logger.warn("Перемещение писем в различные стандартные папки");
        // Доп отправка писем
        String subject1 = sendWith.viaProd().waitDeliver().count(COUNT_OF_MAILS).send().getSubj();
        moveLettersToFolderOnMids(folderList.draftFID(), subject1);
        // Еще отправка
        String subject2 = sendWith.viaProd().waitDeliver().count(COUNT_OF_MAILS).send().getSubj();
        moveLettersToFolderOnMids(folderList.spamFID(), subject2);
        // И еще отправка
        String subject3 = sendWith.viaProd().waitDeliver().count(COUNT_OF_MAILS).send().getSubj();
        moveLettersToFolderOnMids(folderList.deletedFID(), subject3);
    }

    @Test
    @Description("Перемещения писем из ВХОДЯЩИХ и ОТПРАВЛЕННЫХ по мидам в пользовательскую папку [DARIA-35802]")
    public void moveLettersFromDefaultFolderAndOutbox() throws Exception {
        logger.warn("Перемещение писем в Outbox");
        // Доп отправка писем
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_MAILS).send();
        String subject = sendUtils.getSubj();
        List<String> mids = sendUtils.getMids();

        // Проверка что все переместились
        List<String> midsInOutbox = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.sentFID()))
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        mids.addAll(midsInOutbox);

        String newFid = newFolder(userFolderName).post().via(hc).updated();
        move(mids, newFid, folderList.defaultFID()).post().via(hc).resultIdOk();

        assertThat(String.format("Неверное количество писем в пользовательской папке %s(%s) " +
                        "(возможно не перенесли письма из отправленных) [DARIA-35802]", userFolderName, newFid),
                hc, hasMsgsIn(sendUtils.getSubj(), mids.size(), newFid));

    }

    @Test
    @Issue("DARIA-50530")
    @Stories(MyStories.THREADS)
    @Description("Перемещаем тред из двух писем, которые раскиданы по разным папкам")
    public void moveLettersOnTidToUserFolder() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().subj("Thread: " + Util.getRandomString()).count(COUNT_OF_MAILS)
                .waitDeliver().send();
        List<String> mids = sendUtils.getMids();

        String fid = newFolder().post().via(hc).updated();

        String tid = jsx(ThreadsView.class).post().via(hc).getThreadId(sendUtils.getSubj());
        //переносим письмо
        moveTid(tid, fid, folderList.defaultFID()).post().via(hc).resultIdOk();

        waitWith.subj(sendUtils.getSubj()).count(0).waitDeliver();

        // Проверка что все переместились
        int moved = jsx(MailBoxList.class).params(MailBoxListObj.inFid(fid))
                .post().via(hc).countMessagesInFolderWithSubj(sendUtils.getSubj());

        assertThat("Количество перемещенных сообщений не совпадает с ожидаемым. Ожидалось: " + mids.size(),
                moved, equalTo(mids.size()));
    }

    @Test
    @Stories(MyStories.THREADS)
    @Description("Проверяем кейс при перемещении треда писем в Outbox")
    public void moveThreadFromInboxToSent() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().subj("Thread in sent: " + Util.getRandomString()).count(COUNT_OF_MAILS)
                .waitDeliver().send();
        List<String> mids = sendUtils.getMids();
        String tid = sendUtils.getTid();
        //переносим письмо
        moveTid(tid, folderList.sentFID(), folderList.defaultFID()).post().via(hc).resultIdOk();

        waitWith.subj(sendUtils.getSubj()).count(0).waitDeliver();

        // Проверка что все переместились
        int moved = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.sentFID()))
                .post().via(hc).countMessagesInFolderWithSubj(sendUtils.getSubj());

        assertThat("Количество перемещенных писем в \"Отправленные\" не совпадает с ожидаемым. [DARIA-35802]" + mids.size(),
                mids.size() * 2, equalTo(moved));
    }

    /**
     * @param withSent
     * @param mailsCount
     * @param destFid
     * @return moved messages count
     * @throws Exception
     */
    private int moveThread(Optional<WithSentParam> withSent, int mailsCount, String destFid) throws Exception {
        SendUtils sendUtils = sendWith.viaProd().subj("Thread: " + Util.getRandomString()).count(mailsCount)
                .waitDeliver().send();
        String tid = sendUtils.getTid();

        final ApiComplexMove api = Mops.complexMove(authClient, destFid, new TidsSource(tid));
        withSent.ifPresent(api::withWithSent);
        api.post(shouldBe(okSync()));

        waitWith.subj(sendUtils.getSubj()).count(0).waitDeliver();
        // Проверка что все переместились
        return jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(destFid))
                .post().via(hc).countMessagesInFolderWithSubj(sendUtils.getSubj());
    }

    private void moveThreadFromSentToDeleted(Optional<WithSentParam> withSent, int mailsCount, int expectedMovedCount) throws Exception {
        final int moved = moveThread(withSent, mailsCount, folderList.deletedFID());
        assertThat("Количество перемещенных писем в \"Удаленные\" не совпадает с ожидаемым. [DARIA-50530]",
                expectedMovedCount, equalTo(moved));
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Удаленные\" без параметра with_sent")
    @Issue("DARIA-50530")
    public void moveThreadFromSentToDeletedTest() throws Exception {
        moveThreadFromSentToDeleted(Optional.empty(), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Удаленные\" с параметром with_sent=1")
    @Issue("MAILDEV-292")
    public void moveThreadFromSentToDeletedUsingWithSentTrueTest() throws Exception {
        moveThreadFromSentToDeleted(Optional.of(WithSentParam._1), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Удаленные\" с параметром with_sent=0")
    @Issue("MAILDEV-292")
    public void moveThreadFromSentToDeletedUsingWithSentFalseTest() throws Exception {
        moveThreadFromSentToDeleted(Optional.of(WithSentParam._0), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }

    private void moveThreadFromSentToSpam(Optional<WithSentParam> withSent, int mailsCount, int expectedMovedCount) throws Exception {
        final int moved = moveThread(withSent, mailsCount, folderList.spamFID());
        assertThat("Количество перемещенных писем в \"Спам\" не совпадает с ожидаемым. [DARIA-50530]",
                expectedMovedCount, equalTo(moved));
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Спам\" без параметра with_sent")
    @Issue("DARIA-50530")
    public void moveThreadFromSentToSpamTest() throws Exception {
        moveThreadFromSentToSpam(Optional.empty(), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Спам\" с параметром with_sent=1")
    @Issue("MAILDEV-292")
    public void moveThreadFromSentToSpamUsingWithSentTrueTest() throws Exception {
        moveThreadFromSentToSpam(Optional.of(WithSentParam._1), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Отправленные\" в папку \"Спам\" с параметром with_sent=0")
    @Issue("MAILDEV-292")
    public void moveThreadFromSentToSpamUsingWithSentFalseTest() throws Exception {
        moveThreadFromSentToSpam(Optional.of(WithSentParam._0), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }

    @Test
    @Title("Переносим тред с письмом из папки \"Исходящие\" в папку \"Спам\"")
    @Issue("DARIA-50530")
    @Stories(MyStories.THREADS)
    public void moveThreadFromOutboxTest() throws Exception {
        MailSendMsgObj msg = msgFactory.getDelayedMsg(MINUTES.toMillis(5));
        String subj = msg.getSubj();
        jsx(SendMessage.class).params(msg).post().via(hc);
        String delayedFid = jsx(FolderList.class).post().via(hc).fidBySymbol(Symbol.OUTBOX);
        String tid = waitWith.subj(subj).inFid(delayedFid).count(1).waitDeliver().getTid();

        String userFid = newFolder().post().via(hc).updated();

        moveTid(tid, folderList.deletedFID(), delayedFid).post().via(hc).resultIdOk();
        moveTid(tid, folderList.spamFID(), delayedFid).post().via(hc).resultIdOk();
        moveTid(tid, folderList.draftFID(), delayedFid).post().via(hc).resultIdOk();
        moveTid(tid, folderList.defaultFID(), delayedFid).post().via(hc).resultIdOk();
        moveTid(tid, folderList.sentFID(), delayedFid).post().via(hc).resultIdOk();

        moveTid(tid, userFid, delayedFid).post().via(hc);

        int moved = jsx(MailBoxList.class).params(MailBoxListObj.inFid(delayedFid))
                .post().via(hc).countMessagesInFolderWithSubj(subj);

        assertThat("Переместили письмо из папки \"Исходящие\". Письмо должно было остаться в папке" ,
               moved, equalTo(1));
    }

    private void moveThreadFromUserFolderToDraft(Optional<WithSentParam> withSent, int mailsCount, int expectedMovedCount) throws Exception {
        final int moved = moveThread(withSent, mailsCount, folderList.draftFID());
        assertThat("Количество перемещенных писем в \"Черновики\" не совпадает с ожидаемым. [DARIA-50530]",
                expectedMovedCount, equalTo(moved));
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Черновики\"")
    @Issue("DARIA-50530")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToDraftWithoutWithSent() throws Exception {
        moveThreadFromUserFolderToDraft(Optional.empty(), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Черновики\"")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToDraftUsingWithSentTrue() throws Exception {
        moveThreadFromUserFolderToDraft(Optional.of(WithSentParam._1), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Черновики\"")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToDraftUsingWithSentFalse() throws Exception {
        moveThreadFromUserFolderToDraft(Optional.of(WithSentParam._0), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }

    private void moveThreadFromUserFolderToSpam(Optional<WithSentParam> withSent, int mailsCount, int expectedMovedCount) throws Exception {
        final int moved = moveThread(withSent, mailsCount, folderList.spamFID());
        assertThat("Количество перемещенных писем в \"Спам\" не совпадает с ожидаемым.",
                expectedMovedCount, equalTo(moved));
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Спам\" без параметра with_sent")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToSpamWithoutWithSent() throws Exception {
        moveThreadFromUserFolderToSpam(Optional.empty(), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Спам\" с параметром with_sent=1")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToSpamUsingWithSentTrue() throws Exception {
        moveThreadFromUserFolderToSpam(Optional.of(WithSentParam._1), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Спам\" с параметром with_sent=0")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToSpamUsingWithSentFalse() throws Exception {
        moveThreadFromUserFolderToSpam(Optional.of(WithSentParam._0), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }

    private void moveThreadFromUserFolderToTrash(Optional<WithSentParam> withSent, int mailsCount, int expectedMovedCount) throws Exception {
        final int moved = moveThread(withSent, mailsCount, folderList.deletedFID());
        assertThat("Количество перемещенных писем в \"Удаленные\" не совпадает с ожидаемым.",
                expectedMovedCount, equalTo(moved));
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Удаленные\" без параметра with_sent")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToTrashWithoutWithSent() throws Exception {
        moveThreadFromUserFolderToTrash(Optional.empty(), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Удаленные\" с параметром with_sent=1")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToTrashUsingWithSentTrue() throws Exception {
        moveThreadFromUserFolderToTrash(Optional.of(WithSentParam._1), COUNT_OF_MAILS, COUNT_OF_MAILS * 2);
    }

    @Test
    @Title("Переносим тред с письмом из пользовательской папки в папку \"Удаленные\" с параметром with_sent=0")
    @Issue("MAILDEV-292")
    @Stories(MyStories.THREADS)
    public void moveThreadFromUserFolderToTrashUsingWithSentFalse() throws Exception {
        moveThreadFromUserFolderToTrash(Optional.of(WithSentParam._0), COUNT_OF_MAILS, COUNT_OF_MAILS);
    }
}
