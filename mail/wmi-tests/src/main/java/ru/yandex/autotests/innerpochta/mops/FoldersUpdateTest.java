package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.NOT_EXIST_FID;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.internalError;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.beans.folderlist.Symbol.OUTBOX;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.11.15
 * Time: 20:07
 */
@Aqua.Test
@Title("[MOPS] Редактирование папок. Перенос папки внутрь другой папки")
@Description("Перенос папок, различные тесты")
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Issue("DARIA-49310")
@Credentials(loginGroup = "MopsFoldersUpdateTest")
public class FoldersUpdateTest extends MopsBaseTest {
    private static final String childName = getRandomString();
    private static final String parentNameFrom = getRandomString();
    private static final String parentNameTo = getRandomString();
    private static final int LEVEL = 3;

    private String childFid;
    private String parentFidFrom;
    private String parentFidTo;

    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).after(false).all();

    @Before
    public void prepareFolders() {
        parentFidFrom = newFolder(parentNameFrom);
        parentFidTo = newFolder(parentNameTo);
        childFid = newFolder(childName, parentFidFrom);
        assertThat(authClient, hasFolder(parentNameFrom));
        assertThat(authClient, hasFolder(parentNameTo));
    }

    @Test
    @Description("Перемещение подпапки из одной\n" +
            "папки в другую")
    public void moveSubfolderToFolder() throws Exception {
        Mops.debugPrint(updateFolder(childFid).withParentFid(parentFidTo).post(shouldBe(okEmptyJson())));
        assertThat(authClient, hasSubfolder(childName, parentNameTo));
        assertThat(authClient, not(hasSubfolder(childName, parentNameFrom)));
    }

    @Test
    @Description("Перемещение с неправильным fid подпапки\n" +
            "и с неправильным fid папки\n" +
            "Ожидаемый результат: ничего не переместилось,\n" +
            "возвращаемая ошибка INVALID_ARGUMENT_5001")
    public void moveNullSubfolderShouldSeeInvalidRequest() throws Exception {
        val expectedError = "invalid arguments: parameter not found: fid";
        updateFolder("").withParentFid(parentFidFrom).post(shouldBe(invalidRequest(equalTo(expectedError))));
        //проверяем, что подпапка осталась на месте
        assertThat(authClient, hasSubfolder(childName, parentNameFrom));
        assertThat(authClient, not(hasSubfolder(childName, parentNameTo)));
    }

    @Test
    @Issue("MAILPG-562")
    @Description("Попытка перемещения подпапки в папку,\n" +
            "где уже есть такая подпапка\n" +
            "Ожидаемый результат: ничего не переместилось,\n")
    public void moveToFolderWithSameSubfolder() throws Exception {
        createFolder(childName).withParentFid(parentFidTo).post(shouldBe(okEmptyJson()));
        updateFolder(childFid).withParentFid(parentFidTo).post(shouldBe(invalidRequest()));
        assertThat(authClient, hasSubfolder(childName, parentNameTo));
        assertThat(authClient, hasSubfolder(childName, parentNameFrom));
    }

    private void checkCantSetChildlessParent(String childFid, String parentFid) {
        String expectedError = String.format("can't move folder %s to %s: folder can not be parent",
                childFid, parentFid);
        updateFolder(childFid).withParentFid(parentFid)
                .post(shouldBe(internalError(containsString(expectedError))));
    }

    @Test
    @Issue("DARIA-1690")
    @Description("Перемещение подпапки в системную папку\n" +
            "settings_folder_move&fid=<фид>&parent_id=0 => ok, перемещаем на верхний уровень\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id входящие> => 5001 INVALID_ARGUMENT, MAILDEV-608\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id исходящие> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id отправленные> => ок, перемещаем в \"отправленные\"\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id спам> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id удаленные> => ok, MPROTO-2108\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id черновики> => ок, перемещаем в \"черновики\"")
    public void moveSubfolderToSystemFolder() {
        updateFolder(childFid).withParentFid(folderList.sentFID()).post(shouldBe(okEmptyJson()));
        assertThat(authClient, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(authClient, hasSubfolder(childName, folderList.nameBySymbol(Symbol.SENT)));

        updateFolder(childFid).withParentFid(folderList.draftFID()).post(shouldBe(okEmptyJson()));
        assertThat(authClient, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(authClient, hasSubfolder(childName, folderList.nameBySymbol(Symbol.DRAFT)));

        updateFolder(childFid).withParentFid(folderList.deletedFID()).post(shouldBe(okEmptyJson()));
        assertThat(authClient, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(authClient, hasSubfolder(childName, folderList.nameBySymbol(Symbol.TRASH)));

        checkCantSetChildlessParent(childFid, folderList.defaultFID());
        checkCantSetChildlessParent(childFid, folderList.spamFID());
        val fidDelayed = folderList.fidBySymbol(OUTBOX);
        checkCantSetChildlessParent(childFid, fidDelayed);
    }

    @Test
    @Description("Попытка переместить несуществующую папку (с несуществующим fid в этом ящике)\n" +
            "Ожидаемый результат: ошибка, нет такой")
    public void cantMoveFolderThatNotExist() throws Exception {
        val expectedError = String.format("access to nonexistent folder '%s': no such folder", NOT_EXIST_FID);
        updateFolder(NOT_EXIST_FID).withParentFid(parentFidTo)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat(authClient, hasSubfolder(childName, parentNameFrom));
        assertThat(authClient, not(hasSubfolder(childName, parentNameTo)));
    }

    @Test
    @Issue("DARIA-27280")
    @Description("Перемещение пустой папки со\n" +
            "произвольным уровнем вложенности\n" +
            "проверка, что папка переместилась\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveEmptySubfolderHierarchyLevel(){
        val movedName = getRandomString();
        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = updatedFolderList().nameByFid(fromFid);

        val toFid = createInnerFolderStructure(parentFidTo, LEVEL);
        val toName = updatedFolderList().nameByFid(toFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withName(movedName).withParentFid(toFid).post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasSubfolder(movedName, toName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение пустой папки в папку с\n" +
            "неправильным уровнем вложенности\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveSubfolderWrongHierarchyLevel() {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL + 2);
        val fromName = updatedFolderList().nameByFid(fromFid);

        val toFid = createInnerFolderStructure(parentFidTo, LEVEL);
        val toName = updatedFolderList().nameByFid(toFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withName(movedName).withParentFid(toFid).post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasSubfolder(movedName, toName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move с parent_id = 0 и сменой имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveSubfolderUpHierarchyLevelWithZeroParentIdAndRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withName(movedName).withParentFid("0").post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move с parent_id = \"\" и сменой имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveSubfolderUpHierarchyLevelWithEmptyParentIdAndRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withName(movedName).withParentFid("").post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move без параметра parent_id и со сменой имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveSubfolderUpHierarchyLevelWithoutParentIdAndRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withName(movedName).post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move с parent_id = 0 и без смены имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "без изменения имени")
    public void moveSubfolderUpHierarchyLevelWithZeroParentIdWithoutRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withParentFid("0").post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move с parent_id = \"\" и без смены имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "без изменения имени")
    public void moveSubfolderUpHierarchyLevelWithEmptyParentIdWithoutRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).withParentFid("").post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень.\n" +
            "вызываем move без параметра parent_id и без смены имени\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "без изменения имени")
    public void moveSubfolderUpHierarchyLevelWithoutParentIdWithoutRename() throws IOException {
        val movedName = getRandomString();

        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        val movedFid = newFolder(movedName, fromFid);
        updateFolder(movedFid).post(shouldBe(okEmptyJson()));

        assertThat(authClient, hasFolder(movedName));
        assertThat(authClient, not(hasSubfolder(movedName, fromName)));
    }


    @Test
    @Description("Перемещение папки на верхний уровень\n" +
            "ожидаемый результат: ничего не изменилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveFolderUpHierarchyLevel() throws IOException {
        val fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        val fromName = folderList.nameByFid(fromFid);
        updateFolder(parentFidFrom).withName(parentNameFrom).withParentFid("").post(shouldBe(okEmptyJson()));
        assertThat(authClient, hasFolder(parentNameFrom));
        assertThat(authClient, not(hasSubfolder(fromName, parentFidFrom)));
    }
}
