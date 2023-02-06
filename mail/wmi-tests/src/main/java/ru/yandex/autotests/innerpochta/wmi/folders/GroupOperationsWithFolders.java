package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderMoveAllMessages;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderClear;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static java.lang.Integer.parseInt;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.FolderMoveAllMessagesObj.move;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.inFid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj.deleteMsges;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj.moveSomeMsges;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.clearFid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.getObjToClearByOld;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.clearBySubj;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.purgeFid;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.FolderMoveAllMessages.folderMoveAllMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.mailboxOper;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDelete.delete;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs.forceDeleteWithMsgs;
import static ru.yandex.autotests.innerpochta.wmicommon.Arbiter.compareMailboxes;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.NO_SUCH_FOLDER_5002;

/**
 * Групповые операции с письмами
 */
@Aqua.Test
@Title("Групповые операции с папками и письмами")
@Description("Различные действия по перемещению по папкам группы писем")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "FullFolderTest")
public class GroupOperationsWithFolders extends BaseTest {

    public static final String UNEXISTENT_FID = "-10";
    
    private String login;
    private String subject;

    public static final int COUNT_OF_MAILS = 3;

    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Before
    public void prepare() throws Exception {
        login = authClient.acc().getLogin();
        subject = Util.getRandomString();
        // Отправка тестовой пачки писем
        sendWith.viaProd().subj(subject).count(COUNT_OF_MAILS).waitDeliver().send();
    }

    @Test
    @Title("Удаления в папке удаленные")
    @Description("Отправляет пачку писем\n" +
            "Перемещает их после получения в удаленные\n" +
            "Удаляет из удаленных в удаленные\n" +
            "Очищает папку удаленных\n" +
            "- Проверяет что писем не осталось ни во входящих, ни в удаленных\n" +
            " [?] не совсем понятный тест")
    public void deleteLettersFromBin() throws Exception {
        String fidDeleted = folderList.deletedFID();

        // Получение всех отправленных
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesWithSubject(subject);

        // Перемещение писем в удаленные
        mailboxOper(moveSomeMsges(mids, fidDeleted, folderList.defaultFID())).post().via(hc);

        waitWith.subj(subject).count(mids.size()).inFid(fidDeleted).waitDeliver();

        logger.info("Метод удалить в удаленные из удаленных");
        jsx(SettingsFolderClear.class).params(clearFid(fidDeleted)).post().via(hc)
                .assertResponse(not(containsString("error")));

        // Проверка что письма удалены из ВХОДЯЩИХ
        assertThat("Письма остались во входящих. Ожидалось 0",
                mailboxListJsx().post().via(hc).countMessagesInFolderWithSubj(subject), equalTo(0));

        // Удаление из удаленных
        jsx(SettingsFolderClear.class).params(purgeFid(fidDeleted)).post().via(hc);

        assertThat("Письма остались в удаленных. Ожидалось в удаленных: 0",
                mailboxListJsx(inFid(fidDeleted))
                        .post().via(hc).countMessagesInFolderWithSubj(subject), equalTo(0));
    }

    @Test
    @Title("Удаление писем")
    @Description("Отправляет пачку писем\n" +
            "Перемещает их после получения в удаленные\n" +
            "- Проверяет что писем не осталось ни во входящих, ни в удаленных")
    @Issue("DARIA-22309")
    public void deleteLettersToBin() throws Exception {
        String fidDeleted = folderList.deletedFID();

        // Получение всех отправленных
        List<String> mids = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        logger.info("Метод удалить в удаленные");
        jsx(SettingsFolderClear.class).params(
                clearFid(folderList.defaultFID())).post().via(hc).assertResponse(not(containsString("error")));

        waitWith.subj(subject).count(mids.size()).inFid(fidDeleted).waitDeliver();

        // Проверка что письма удалены из ВХОДЯЩИХ
        assertThat("Письма остались во входящих",
                mailboxListJsx().post().via(hc).countMessagesInFolderWithSubj(subject), equalTo(0));

        // Удаление из удаленных
        jsx(SettingsFolderClear.class).params(purgeFid(fidDeleted)).post().via(hc);

        assertThat("Письма остались в удаленных",
                mailboxListJsx(inFid(fidDeleted))
                        .post().via(hc).countMessagesInFolderWithSubj(subject), equalTo(0));
    }

    @Test
    @Title("Очистка писем по параметру: тема")
    public void deleteLettersBySubject() throws Exception {
        jsx(SettingsFolderClear.class)
                .params(clearBySubj(folderList.defaultFID(), subject))
                .post().via(hc);

        assertFalse("Во ВХОДЯЩИХ найдены письма с темой: " + subject + ", ожидалось: отсутствие писем с данной темой",
                mailboxListJsx().post().via(hc).isThereMessage(subject));
    }

    @Test
    @Title("Очистка писем по параметру: отправитель")
    public void deleteLettersBySender() throws Exception {
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj
                        .getObjToClearByFrom(folderList.defaultFID(), login.toLowerCase()))
                .post().via(hc);

        assertFalse("Во ВХОДЯЩИХ найдены письма с темой: " + subject + ", ожидалось: отсутствие писем с данной темой",
                mailboxListJsx().post().via(hc).isThereMessage(subject));
    }

    @Test
    @Title("Очистка писем по параметру: старше даты с неправильной подстановкой параметра.")
    @Description("Очистка писем по параметру: старше даты\n" +
            "Попытка подставить в дату произвольную строку\n" +
            "- Проверка что ничего не изменилось. Генерирует в логах ORA-01722")
    public void deleteLettersByInvalidDate() throws Exception {
        Document mailboxListBefore = mailboxListJsx().post().via(hc).toDocument();

        // Из-за этой строчки в логах ORA-01722: invalid number - это нормально
        jsx(SettingsFolderClear.class)
                .params(
                        getObjToClearByOld(folderList.defaultFID(), Util.getRandomString()))
                .post().via(hc);

        Document mailboxListAfter = mailboxListJsx().post().via(hc).toDocument();
        // Убеждаемся что ничего не изменилось
        compareMailboxes(mailboxListBefore, mailboxListAfter);

        // Чистка
        jsx(SettingsFolderClear.class)
                .params(clearBySubj(folderList.defaultFID(), subject))
                .post().via(hc);

        assertFalse("Во ВХОДЯЩИХ найдены письма с темой: " + subject +
                        ", ожидалось: отсутствие писем с данной темой " +
                        "после использования очистки по теме",
                mailboxListJsx().post().via(hc).isThereMessage(subject));
    }

    @Test
    @Title("Удаление ручкой mailbox_oper")
    @Description("Удаление ручкой mailbox_oper сначала в удаленные, потом совсем\n" +
            "- Проверка что все удалено")
    public void deleteMessagesWithMailboxOperMethod() throws Exception {
        List<String> mids = mailboxListJsx()
                .post().via(hc).getMidsOfMessagesWithSubject(subject);

        // Del
        mailboxOper(deleteMsges(mids)).post().via(hc);

        // Проверка перемещения в удаленные
        waitWith.subj(subject).count(mids.size()).inFid(folderList.deletedFID()).waitDeliver();

        // Повторное удаление и проверка
        mailboxOper(deleteMsges(mids)).post().via(hc);

        assertFalse("В УДАЛЕННЫХ найдены письма с темой: " + subject + ", ожидалось: отсутствие писем с данной темой",
                mailboxListJsx(inFid(folderList.deletedFID()))
                        .post().via(hc).isThereMessage(subject));
    }

    @Test
    @Issue("DARIA-4576")
    @Title("Проверка работы ручки folder_move_all_messages")
    @Description("[DARIA-4576 (wmi18)] Проверка ручки folder_move_all_messages")
    public void moveAll() throws Exception {
        jsx(SettingsFolderClear.class).params(
                purgeFid(folderList.defaultFID())).post().via(hc);

        sendWith.viaProd().subj(subject).count(COUNT_OF_MAILS).waitDeliver().send();

        // Создание папки
        String name = Util.getRandomString();
        String fid = newFolder(name).post().via(hc).updated();

        // Перемещение
        folderMoveAllMessages(folderList.defaultFID(), fid).post().via(hc);

        waitWith.subj(subject).count(COUNT_OF_MAILS).inFid(fid).waitDeliver();

        assertThat("ВХОДЯЩИЕ содержит письма. Ожидалось: все письма перемещены в папку: " + fid,
                mailboxListJsx().post().via(hc).countMessagesInFolder().intValue(), equalTo(0));

        assertEquals("В папке " + fid + " неверное количество писем",
                COUNT_OF_MAILS,
                mailboxListJsx(inFid(fid))
                        .post().via(hc).countMessagesInFolder().intValue());
    }

    @Test
    @Title("Проверка ручки folder_move_all_messages на ошибки")
    @Description("Проверка ручки folder_move_all_messages на ошибки\n")
    public void moveAllWithNegativeArgs() throws Exception {
        FolderMoveAllMessages moveOp = jsx(FolderMoveAllMessages.class);

        moveOp.params(move(UNEXISTENT_FID, folderList.defaultFID()))
                .post().via(hc).shouldBe().errorcode(NO_SUCH_FOLDER_5002);

        moveOp.params(move(folderList.defaultFID(), UNEXISTENT_FID)).post().via(hc)
                .shouldBe().errorcode(NO_SUCH_FOLDER_5002);

         moveOp.params(move(folderList.defaultFID(), ""))
                .post().via(hc).shouldBe().errorcode(NO_SUCH_FOLDER_5002);
    }

    @Test
    @Title("Удаление папки нефорс и форс методами")
    @Description("Попытка удалить папку с письмами мягким методом\n" +
            "- Проверка что папка на месте\n" +
            "Попытка форс-удаления папки с письмами\n" +
            "- Проверка что папки не осталось")
    public void testDeletingFolderWithMessages() throws Exception {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesWithSubject(subject);

        String folderName = Util.getRandomString();
        String newFid = newFolder(folderName).post().via(hc).updated();

        // Перемещение писем в новую папку и попытка мягкого удаления
        mailboxOper(moveSomeMsges(mids, newFid, folderList.defaultFID()))
                .post().via(hc);

        waitWith.subj(subject).count(mids.size()).inFid(newFid).waitDeliver();

        delete(newFid);
        assertTrue("Удалилась папка с сообщениями при мягком удалении! Ожидалось: наличие папки с именем: "
                        + folderName
                        + " и количеством сообщений в ней: " + mids.size(),
                api(FolderList.class)
                        .post().via(hc).isThereFolder(folderName));

        // Форс удаление
        forceDeleteWithMsgs(newFid)
                .post().via(hc);
        assertFalse("Папка " + folderName + " не была удалена. Ожидалось: отсутствие данной папки",
                api(FolderList.class)
                        .post().via(hc).isThereFolder(folderName));
    }

    /**
     * Чистка
     *
     * @throws Exception *
     */
    @After
    public void clearing() throws Exception {
        clearOutbox();
    }


    /**
     * Очистка отправленных и проверка что ничего не осталось
     *
     * @throws Exception *
     */
    private void clearOutbox() throws Exception {
        String folderIdOutbox = folderList.sentFID();

        jsx(SettingsFolderClear.class).params(purgeFid(folderIdOutbox)).post().via(hc);

        // Проверка через атрибут msg_count
        assertThat("Значение атрибута msg_count не соответствует ожидаемому",
                mailboxListJsx(inFid(folderIdOutbox))
                        .post().via(hc).getMessageCounter(), equalTo(0));

        // Проверка подсчетом писем в выдаче
        assertThat("Количество писем в выдаче не соответсвует ожидаемому",
                mailboxListJsx(inFid(folderIdOutbox))
                        .post().via(hc).countMessagesInFolder(), equalTo(0));
    }

}
