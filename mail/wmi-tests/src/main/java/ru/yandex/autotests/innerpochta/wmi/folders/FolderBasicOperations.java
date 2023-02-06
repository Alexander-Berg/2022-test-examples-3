package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderClear;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDelete;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolderWithNameMatcher.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereSubfolderMatcher.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.purgeFid;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newChildFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs.forceDeleteWithMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderRename.renameFolder;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.DB_UNIQUE_CONSTRAINT_VIOLATED_1003;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.NO_SUCH_FOLDER_5002;

@Aqua.Test
@Title("Базовые операции с папками")
@Description("Различные элементарные операции над папками")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "FolderBasicOperations")
public class FolderBasicOperations extends BaseTest {

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .inbox().outbox().folder(WmiConsts.FOLDER_DELAYED);

    @Rule
    public DeleteFoldersRule clear = getDeleteFoldersRule();

    private DeleteFoldersRule getDeleteFoldersRule() {
        DeleteFoldersRule res = DeleteFoldersRule.with(authClient).all();
        if (props().testingScope().equals(Scopes.INTRANET_PRODUCTION.getName())) {
            res.exclude("1000");
        }
        return res;
    }

    @Test
    @Description("Должны увидеть все системные папки")
    public void systemFoldersShouldExist() {
        assertThat(hc, hasFolder(folderList.defaultName()));
        assertThat(hc, hasFolder(folderList.deletedName()));
        assertThat(hc, hasFolder(folderList.sentName()));
        assertThat(hc, hasFolder(folderList.spamName()));
        assertThat(hc, hasFolder(folderList.draftName()));
    }

    @Test
    @Title("Должны сначала создать, затем удалить папку")
    public void createAndDeleteFolder() throws Exception {
        String folderName = Util.getRandomString();
        newFolder(folderName).post().via(hc);

        assertThat(hc, hasFolder(folderName));
        deleteFolderByName(folderName);
        assertThat(hc, not(hasFolder(folderName)));
    }

    @Test
    @Title("Должны создать, затем удалить подпапку")
    public void createAndDeleteSubfolder() throws Exception {
        String parentFolderName = Util.getRandomString();
        String childFolderName = Util.getRandomString();
        // Родительская папка
        newFolder(parentFolderName).post().via(hc);

        createSubFolderWithNameOfParent(parentFolderName, childFolderName);

        assertThat(hc, hasSubfolder(childFolderName, parentFolderName));
        assertThat("Ожидалось наличие родительской папки", hc, hasFolder(parentFolderName));
        // Удаление
        deleteSubfolder(childFolderName, parentFolderName);
        deleteFolderByName(parentFolderName);
        assertThat("Ожидалось отсутствие подпапки", hc, not(hasSubfolder(childFolderName, parentFolderName)));
    }

    @Test
    @Title("Должны вернуть ошибку «нет папки» при удалении с несуществующим fid в этом ящике")
    public void cantDeleteFolderThatNotExist() throws Exception {
        Document folderListBeforeDeleting = api(FolderList.class).post().via(hc).toDocument();
        forceDeleteWithMsgs("2080000120000081623")
                .post().via(hc)
                .shouldBe().errorcode(NO_SUCH_FOLDER_5002);
//        Assert.assertEquals("Ожидалась ошибка отсутствия папки", NO_SUCH_FOLDER_ERROR_CODE, resp.getErrorCode());
        currentFolderListShouldBeEqualTo(folderListBeforeDeleting);
    }

    @Test
    @Title("Должны вернуть ошибку «нет папки» при удалении с невалидным fid")
    public void deleteFolderWithSymbolFid() throws Exception {
        Document folderListBeforeDeleting = api(FolderList.class).post().via(hc).toDocument();
        forceDeleteWithMsgs("zpq")
                .post().via(hc)
                .shouldBe().errorcode(NO_SUCH_FOLDER_5002);
        currentFolderListShouldBeEqualTo(folderListBeforeDeleting);
    }

    @Test
    @Title("Должны вернуть ошибку «нет папки» при удалении с пустым fid")
    public void cantDeleteNullFolder() throws Exception {
        Document folderListBeforeDeleting = api(FolderList.class).post().via(hc).toDocument();
        forceDeleteWithMsgs("").post().via(hc).shouldBe().errorcode(NO_SUCH_FOLDER_5002);
        currentFolderListShouldBeEqualTo(folderListBeforeDeleting);
    }

    @Test
    @Title("Должны вернуть ошибку «уже есть такая» при повторном создании папки")
    public void cantCreateTwoSimilarFolders() throws Exception {
        // Создание
        String randomName = Util.getRandomString();
        newFolder(randomName).post().via(hc);


        Document folderList = api(FolderList.class).post().via(hc).toDocument();
        Util.removeAllNodesFromXml(folderList, Node.ELEMENT_NODE, "scn");
        newFolder(randomName).post().via(hc).errorcode(DB_UNIQUE_CONSTRAINT_VIOLATED_1003);
        //2-02-12 Ответ не содержит причины, только код


        Document newFolderList = api(FolderList.class).post().via(hc).toDocument();
        assertThat(newFolderList, equalToDoc(folderList).exclude("//scn"));

        deleteFolderByName(randomName);
        assertThat(hc, not(hasFolder(randomName)));
    }

    @Test
    @Title("Не должны создать папку со слишком длинным именем")
    public void cantCreateFolderWithLongName() throws Exception {
        String longName = Util.getLongString();
        newFolder(longName).post().via(hc);
        assertThat(hc, not(hasFolder(longName)));
    }

    @Test
    @Title("Должны вернуть ошибку при создании папки с пустым именем")
    public void cantCreateNullFolder() throws Exception {
        newFolder("").post().via(hc).errorcode(INVALID_ARGUMENT_5001);
        assertThat(hc, not(hasFolder("")));
    }

    @Test
    @Title("Должны вернуть ошибку при удалении папки с длинным символьным fid")
    public void cantDeleteFolderWithLongName() throws Exception {
        String longName = Util.getLongString() + Util.getLongString() + Util.getLongString();
        for (int i = 0; i < 10; i++) {
            longName = longName + longName;
        }
        forceDeleteWithMsgs(longName).post().via(hc).shouldBe().errorcode(NO_SUCH_FOLDER_5002);
    }

    @Test
    @Issue("MAILPG-914")
    @Description("Попытка создать подпапку с некорректным fid родительской папки\n" +
            "Ожидаемый результат: ошибка создания")
    public void cantCreateSubfolderWithUncorrectParentId() throws Exception {
        String folderName = Util.getRandomString();

        newChildFolder(folderName, "11111111111").post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.NO_SUCH_FOLDER_5002);
//        Assert.assertEquals("Ожидалась ошибка отсутствия папки", INVALID_PARENT_ID_ERROR_CODE, resp.getErrorCode());
        assertThat("Ожидалось отсутвие папки с пустым именем после создания некорректной подпапки",
                hc, not(hasFolder("")));
    }

    /**
     * Создание и удаление подпапки в папке c известным именем
     *
     * @param parentFolderName - имя родительской папки
     * @param childFolderName  - имя дочерней папки
     *
     * @throws IOException *
     */
    private void tryToCreateAndDeleteSubFolder(String childFolderName, String parentFolderName) throws Exception {
        createSubFolderWithNameOfParent(parentFolderName, childFolderName);
        assertThat(hc, hasSubfolder(childFolderName, parentFolderName));
        deleteSubfolder(childFolderName, parentFolderName);
        assertThat(String.format("Ожидалось отсутствие подпапки %s после удаления", parentFolderName),
                hc, not(hasSubfolder(childFolderName, parentFolderName)));
    }

    @Test
    @Description("Создание и удаление подпапки в системных папках\n" +
            "settings_folder_move&fid=<фид>&parent_id=0 => ok, перемещаем на верхний уровень\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id входящие> => 5001 INVALID_ARGUMENT, MAILDEV-608\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id исходящие> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id отправленные> => ок, перемещаем в \"отправленные\"\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id спам> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id удаленные> => ok, MPROTO-2108\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id черновики> => ок, перемещаем в \"черновики\"")
    public void createSubFolderInSpamEtc() throws Exception {
        //папки исходящих пока не существует, отправляем отложенное письмо, чтобы она появилась
        MailSendMsgObj msg = msgFactory.getDelayedMsg(DAYS.toMillis(100)); //now + 100 days
        //отправляем письмо
        jsx(SendMessage.class).params(msg).post().via(hc);
        clean.subject(msg.getSubj());
        FolderList folderList = api(FolderList.class).post().via(hc);
        String childName = Util.getRandomString();
        tryToCreateAndDeleteSubFolder(Util.getRandomString(), folderList.sentName());
        tryToCreateAndDeleteSubFolder(Util.getRandomString(), folderList.draftName());
        //MPROTO-2108
        tryToCreateAndDeleteSubFolder(Util.getRandomString(), folderList.deletedName());

        newChildFolder(childName, folderList.fidBySymbol(Symbol.INBOX))
                .post().via(hc).shouldBe().errorcode(WmiErrorCodes.INVALID_ARGUMENT_5001);
        newChildFolder(childName, folderList.fidBySymbol(Symbol.SPAM))
                .post().via(hc).shouldBe().errorcode(WmiErrorCodes.INVALID_ARGUMENT_5001);
        newChildFolder(childName, folderList.fidBySymbol(Symbol.OUTBOX))
                .post().via(hc).shouldBe().errorcode(WmiErrorCodes.INVALID_ARGUMENT_5001);

    }

    @Test
    @Title("Должны переименовать папку")
    public void renameFolderTest() throws Exception {
        String folderName = Util.getRandomString();
        String fid = newFolder(folderName).post().via(hc).updated();
        assumeThat("Ожидалось наличие папки со старым именем", hc, hasFolder(folderName));

        String newName = Util.getRandomString();
        renameFolder(fid, newName).post().via(hc);

        assertThat("Ожидалось наличие переименованной папки", hc, hasFolder(newName));
        assertThat("Ожидалось отсутствие папки со старым именем", hc, not(hasFolder(folderName)));
    }

    @Test
    @Title("Не должны переименовать одну папку в имя другой")
    public void renameFolderShouldSeeError() throws Exception {
        String mainFolderName = Util.getRandomString();
        newFolder(mainFolderName).post().via(hc);
        String newFolder = Util.getRandomString();
        newFolder(newFolder).post().via(hc);


        renameFolder(api(FolderList.class)
                .post().via(hc).getFolderId(newFolder), mainFolderName)
                .post().via(hc).errorcode(DB_UNIQUE_CONSTRAINT_VIOLATED_1003);

        deleteFolderByName(newFolder);
        deleteFolderByName(mainFolderName);

        assertThat("Ожидалось отсутствие папки со старым именем", hc, not(hasFolder(newFolder)));
        assertThat("Ожидалось отсутствие папки со старым именем", hc, not(hasFolder(mainFolderName)));
    }

    @Test
    @Title("Не должны менять имя папки при использовании слишком длинного имени")
    public void cantRenameFolderToLongName() throws Exception {
        String randomName = Util.getRandomString();
        newFolder(randomName).post().via(hc);

        String longName = Util.getLongString();
        String fid = api(FolderList.class).post().via(hc).getFolderId(randomName);

        renameFolder(fid, longName).post().via(hc);

        assertThat("Ожидалось отсутствие папки с длинным именем", hc, not(hasFolder(longName)));
    }

    @Test
    @Title("Удаление папки без fid возвращает ошибку (нет такой)")
    public void cantDeleteFolderWithNoFid() throws Exception {
        jsx(SettingsFolderDelete.class).post().via(hc)
                .shouldBe().errorcode(NO_SUCH_FOLDER_5002);
    }

    @Test
    @Title("Удаление родительской папки удаляет все подпапки")
    public void deleteParentFolder() throws Exception {
        String parentName = Util.getRandomString();
        String childName1 = Util.getRandomString();
        String childName2 = Util.getRandomString();
        String parentFID = newFolder(parentName).post().via(hc).updated();

        newChildFolder(childName1, parentFID).post().via(hc);
        newChildFolder(childName2, parentFID).post().via(hc);

        forceDeleteWithMsgs(parentFID).post().via(hc);

        assertThat("Ожидалось отсутствие подпапки", hc, not(hasSubfolder(childName1, parentName)));
        assertThat("Ожидалось отсутствие подпапки", hc, not(hasSubfolder(childName2, parentName)));
        assertThat("Ожидалось отсутствие родительской папки", hc, not(hasFolder(parentName)));
    }

    @Test
    @Title("Не должно быть изменений при очистке пустой папки")
    public void cleanEmptyFolder() throws Exception {
        String folderName = Util.getRandomString();
        newFolder(folderName).post().via(hc);
        Document foldersListBefore = api(FolderList.class).post().via(hc).toDocument();

        SettingsFolderClearObj clearFolder =
                purgeFid(api(FolderList.class).post().via(hc).getFolderId(folderName));
        jsx(SettingsFolderClear.class).params(clearFolder).post().via(hc);

        currentFolderListShouldBeEqualTo(foldersListBefore);
    }

    /**
     * Сравнение ответов 2х списков папок
     *
     * @param folderListBefore - список папок до
     *
     * @throws Exception *
     */
    private void currentFolderListShouldBeEqualTo(Document folderListBefore) throws Exception {
        Document folderListAfter = api(FolderList.class).post().via(hc).toDocument();
        assertThat(folderListAfter, equalToDoc(folderListBefore).urlcomment("FolderListResp_").exclude("//scn"));
    }

    /**
     * Удаляет папку по имени
     *
     * @param folderName - имя папки
     *
     * @throws IOException
     */
    private void deleteFolderByName(String folderName) throws IOException {
        String newFid = api(FolderList.class).post().via(hc).getFolderId(folderName);
        forceDeleteWithMsgs(newFid).post().via(hc);
    }

    /**
     * Удаляет подпапку от папки
     *
     * @param childFolderName  - имя подпапки
     * @param parentFolderName - имя родительской папки
     *
     * @throws IOException
     */
    private void deleteSubfolder(String childFolderName, String parentFolderName) throws IOException {
        deleteFolderByName(parentFolderName + "|" + childFolderName);
    }

    /**
     * Создание родительской папки с вложенной папкой
     *
     * @param parentFolderName - имя родительской папки
     * @param childFolderName  - имя вложенной папки
     *
     * @throws Exception *
     */
    private void createSubFolderWithNameOfParent(String parentFolderName, String childFolderName) throws Exception {
        // Родительский фид
        String parentFolderID = api(FolderList.class)
                .post().via(hc).getFolderId(parentFolderName);
        // Дочерняя папка
        newChildFolder(childFolderName, parentFolderID).post().via(hc);
    }
}
