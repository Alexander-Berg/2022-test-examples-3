package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.create.ApiFoldersCreate.StrictParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;
import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.folderlist.Symbol.OUTBOX;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolderWithNameAndSymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.internalError;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 13.11.15
 * Time: 17:50
 */
@Aqua.Test
@Title("[MOPS] Создание/редактирование папок")
@Description("Создание папок/редактирование, различные тесты")
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Issue("DARIA-49310")
@Credentials(loginGroup = "MopsFoldersCreateTest")
@RunWith(DataProviderRunner.class)
public class FoldersCreateTest extends MopsBaseTest {
    public static final String ARCHIVE_FOLDER = "Архив";

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @DataProvider({
            "<tag>test<p/></tag>",
            "te&st",
    })
    @Title("Должны сначала создать, затем удалить папку")
    public void createAndDeleteFolderTest(String name) throws Exception {
        val newFid = shouldCreateFolder(name);
        deleteFolder(newFid).post(shouldBe(okSync()));
        assertThat(authClient, not(hasFolder(name)));
    }

    @Test
    @Title("Должны создать, затем удалить подпапку")
    public void createAndDeleteSubfolderTest() throws Exception {
        val parentFolderName = getRandomString();
        val childFolderName = getRandomString();
        val newFid = shouldCreateFolder(parentFolderName);
        val newChildFid = newFolder(childFolderName, newFid);

        assertThat("Ожидалось наличие родительской папки", authClient, hasFolder(parentFolderName));
        assertThat(authClient, hasSubfolder(childFolderName, parentFolderName));
        // Удаление
        deleteFolder(newChildFid).post(shouldBe(okSync()));
        deleteFolder(newFid).post(shouldBe(okSync()));

        assertThat("Ожидалось отсутствие подпапки", authClient, not(hasSubfolder(childFolderName, parentFolderName)));
    }

    @Test
    @Issue("MAILPG-593")
    @Title("Должны вернуть ошибку «уже есть такая» при повторном создании папки")
    public void cantCreateTwoSimilarFoldersShouldSeeInvalidRequest() throws Exception {
        val folderName = getRandomString();
        val newFid = shouldCreateFolder(folderName);

        val expectedError = String.format("can't create folder with name \"%s\" parent 0: folder already exists", folderName);
        createFolder(folderName).post(shouldBe(invalidRequest(containsString(expectedError))));

        deleteFolder(newFid).post(shouldBe(okSync()));
        assertThat(authClient, not(hasFolder(folderName)));
    }

    @Test
    @Title("Не должны создать папку со слишком длинным именем")
    public void cantCreateFolderWithLongNameShouldSeeInvalidRequest() throws Exception {
        val longName = Util.getLongString();
        val expectedError = "system_error category: macs::error::Category code: 6";
        createFolder(longName).post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat(authClient, not(hasFolder(longName)));
    }

    @Test
    @Title("Должны вернуть ошибку при создании папки с пустым именем")
    public void cantCreateNullFolderShouldSeeInvalidRequest() throws Exception {
        val expectedError = "invalid arguments: parameter not found: name";
        createFolder("").post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat(authClient, not(hasFolder("")));
    }

    @Test
    @Issue("MAILPG-593")
    @Description("Попытка создать подпапку с некорректным fid родительской папки\n" +
            "Ожидаемый результат: ошибка создания")
    public void cantCreateSubfolderWithNotExistParentIdShouldSeeInvalidRequest() throws Exception {
        val folderName = getRandomString();
        val notExistFid = "11111111111";
        val expectedError = String.format("can't create folder with parent %s: no such folder", notExistFid);
        createFolder(folderName).withParentFid(notExistFid)
                .post(shouldBe(invalidRequest(containsString(expectedError))));

        assertThat("Ожидалось отсутвие папки с пустым именем после создания некорректной подпапки",
                authClient, not(hasFolder("")));
    }

    /**
     * Создание и удаление подпапки в папке c известным именем
     *
     * @param parentName - имя родительской папки
     * @param childFolderName  - имя дочерней папки
     *
     * @throws java.io.IOException *
     */
    private void tryToCreateAndDeleteSubFolder(String childFolderName, String parentName) throws Exception {
        val newChildFid = newFolder(childFolderName, folderList.fidByName(parentName));
        assertThat(authClient, hasSubfolder(childFolderName, parentName));

        deleteFolder(newChildFid).post(shouldBe(okSync()));
        assertThat(String.format("Ожидалось отсутствие подпапки %s после удаления", parentName),
                authClient, not(hasSubfolder(childFolderName, parentName)));
    }

    private void checkCantCreateSubFolder(String childName, String parentFid) {
        val expectedError = String.format("can't create folder with parent %s: folder can not be parent", parentFid);
        createFolder(childName).withParentFid(parentFid)
                .post(shouldBe(internalError(containsString(expectedError))));
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
        val childName = getRandomString();

        tryToCreateAndDeleteSubFolder(getRandomString(), folderList.nameBySymbol(Symbol.SENT));
        tryToCreateAndDeleteSubFolder(getRandomString(), folderList.nameBySymbol(Symbol.DRAFT));
        //MPROTO-2108
        tryToCreateAndDeleteSubFolder(getRandomString(), folderList.nameBySymbol(Symbol.TRASH));

        checkCantCreateSubFolder(childName, folderList.defaultFID());
        checkCantCreateSubFolder(childName, folderList.spamFID());
        checkCantCreateSubFolder(childName, folderList.fidBySymbol(OUTBOX));
    }

    @Test
    @Title("Должны переименовать папку")
    public void renameFolderTest() throws Exception {
        val name = getRandomString();
        val fid = shouldCreateFolder(name);

        val newName = getRandomString();
        Mops.debugPrint(renameFolder(fid, newName).post(identity()));

        assertThat("Ожидалось наличие переименованной папки", authClient, hasFolder(newName));
        assertThat("Ожидалось отсутствие папки со старым именем", authClient, not(hasFolder(name)));
    }

    @Test
    @Issue("MAILPG-593")
    @Title("Не должны переименовать одну папку в имя другой")
    public void renameFolderToExistingNameShouldSeeInvalidRequest() throws Exception {
        val name = getRandomString();
        val fid = shouldCreateFolder(name);

        val newName = getRandomString();
        val newFid = shouldCreateFolder(newName);

        val expectedString = String.format("can't rename folder %s with name \"%s\": folder already exists", fid, newName);
        renameFolder(fid, newName).post(shouldBe(invalidRequest(containsString(expectedString))));

        deleteFolder(newFid).post(shouldBe(okSync()));
    }

    @Test
    @Title("Не должны менять имя папки при использовании слишком длинного имени")
    public void cantRenameFolderToLongNameShouldSeeInvalidRequest() throws Exception {
        val name = getRandomString();
        val fid = shouldCreateFolder(name);
        val longName = Util.getLongString();

        val expectedString = "name is too long: invalid argument";
        renameFolder(fid, longName).post(shouldBe(invalidRequest(containsString(expectedString))));

        assertThat("Ожидалось отсутствие папки с длинным именем", authClient, not(hasFolder(longName)));
        deleteFolder(fid).post(shouldBe(okSync()));
    }

    @Step("Должны успешно создать метку")
    private String shouldCreateFolder(String name) throws Exception {
        val fid = newFolder(name);
        shouldSeeFolder(name);
        return fid;
    }

    @Step("Должны увидеть папку {0}")
    private void shouldSeeFolder(String name) {
        assertThat(authClient, hasFolder(name));
    }

    @Test
    @Issue("MAILDEV-908")
    @Title("Должны создать системную папку \"Архив\"")
    public void shouldCreateSystemFolder() {
        createFolder(ARCHIVE_FOLDER).withSymbol(Symbol.ARCHIVE.toString()).post(shouldBe(okFid()));
        assertThat("Ожидалось, что системная папка \"Архив\" будет создана", authClient,
                hasFolderWithNameAndSymbol(ARCHIVE_FOLDER, Symbol.ARCHIVE));
    }

    @Test
    @Issue("MAILDEV-908")
    @Title("Не должны создавать системную папку с невалидным символом")
    public void shouldNotCreateSystemFolderWithInvalidSymbol() {
        val expectedError = "no such symbol";
        createFolder(ARCHIVE_FOLDER).withSymbol(Symbol.UNKNOWN_TEST_SYMBOL.toString())
                .post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat("Ожидалось, что системная папка не будет создана", authClient,
                not(hasFolder(ARCHIVE_FOLDER)));
    }

    @Test
    @Issue("MAILDEV-915")
    @Title("Создание папки с параметром strict=0")
    public void testNonStrictCreateFolder() {
        val name = getRandomString();
        createFolder(name).withStrict(StrictParam._0).post(shouldBe(okFid()));
        assertThat("Ожидалось, что папка будет создана", authClient, hasFolder(name));
    }

    @Test
    @Issue("MAILDEV-915")
    @Title("Создание папки с параметром strict=1")
    public void testStrictCreateFolder() {
        val name = getRandomString();
        createFolder(name).withStrict(StrictParam._1).post(shouldBe(okFid()));
        assertThat("Ожидалось, что папка будет создана", authClient, hasFolder(name));
    }

    @Test
    @Issue("MAILDEV-915")
    @Title("Создание существующей папки с параметром strict=0")
    public void testNonStrictCreateExistentFolder() throws Exception {
        val name = getRandomString();
        val fid = shouldCreateFolder(name);

        val nextFid = createFolder(name).withStrict(StrictParam._0).post(shouldBe(okFid()))
                .then().extract().body().path("fid");
        assertThat("Ожидалось, что fid совпадет с исходным", fid, equalTo(nextFid));
    }

    @Test
    @Issue("MAILDEV-915")
    @Title("Создание существующей папки с параметром strict=1")
    public void testStrictCreateExistentFolder() throws Exception {
        val name = getRandomString();
        shouldCreateFolder(name);

        val expectedError = "folder already exists";
        createFolder(name).withStrict(StrictParam._1).post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Test
    @Issue("MAILDEV-915")
    @Title("Создание существующей папки без параметра strict")
    public void testCreateExistentFolderWithoutStrict() throws Exception {
        val name = getRandomString();
        shouldCreateFolder(name);

        val expectedError = "folder already exists";
        createFolder(name).post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Test
    @Title("Должны создать папку с пустым parentFid")
    public void createFolderWithEmptyParent() throws Exception {
        createFolder(getRandomString()).withParentFid("").post(shouldBe(okFid()));
    }

    @Test
    @Title("Должны создать папку с нулевым parentFid")
    public void createFolderWithZeroParent() throws Exception {
        createFolder(getRandomString()).withParentFid("0").post(shouldBe(okFid()));
    }

    @Test
    @Title("Должны создать папку без parentFid")
    public void createFolderWithoutParent() throws Exception {
        createFolder(getRandomString()).post(shouldBe(okFid()));
    }
}
