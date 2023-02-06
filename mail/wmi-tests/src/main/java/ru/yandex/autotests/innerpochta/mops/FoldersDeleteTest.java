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
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.INVALID_FID;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.NOT_EXIST_FID;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolderWithFid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.Symbols.ARCHIVE;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 03.12.15
 * Time: 18:00
 *
 * https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface/#post/labels/delete
 */
@Aqua.Test
@Title("[MOPS] Удаление папок")
@Description("Удаляем папку, проверяем что письма в ней содержащиеся и из вложенных папках," +
        " перемещаются в удаленные. " +
        "Проверяем, что нельзя удалить системную папку архивы, удаляя вложенную")
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "FoldersDeleteMopsTest")
public class FoldersDeleteTest extends MopsBaseTest {
    private static final long WAIT_TIME = MINUTES.toMillis(1);

    //количество писем, которые содержатся в папках
    private static final int NUMBER_OF_MAILS = 1;
    private static final String ARCHIVE_FOLDER_NAME = "Архив";
    private static final String parentName = getRandomString();
    private static final String subject = getRandomString();

    private String parentFid;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all().symbol(Symbol.ARCHIVE).before(true);

    @Before
    public void prepareFolder() throws Exception {
        parentFid = newFolder(parentName);
    }

    @Test
    @Issue("DARIA-25188")
    @Description("Удаляем родительскую папку с дочерней, которая содержит письма\n" +
            "Проверяем что все письма отправились в папку удаленные")
    public void deleteParentFolder() throws Exception {
        val childName = getRandomString();
        val childNameSub1 = getRandomString();

        val childFid = newFolder(childName, parentFid);
        fillFolder(NUMBER_OF_MAILS, childFid, subject);

        newFolder(childNameSub1, childFid);
        deleteFolder(parentFid).post(shouldBe(okSync()));

        assertThat(authClient, not(hasFolder(parentName)));
        assertThat(authClient, not(hasFolderWithFid(childFid)));

        assertThat("[DARIA-25188]  при удалении родительской папки, не все письма переместились в удаленные ", authClient,
                hasMsgsIn(subject, NUMBER_OF_MAILS, folderList.deletedFID()));
    }

    @Test
    @Description("Создаем структуру в виде дерева:\n" +
            "родительская папка - дочерняя папка = две дочерние папки\n" +
            "Удаляем родительскую папку, проверяем, что в удаленных 4 x NUMBER_OF_MAILS писем")
    public void deleteParentFolderWithHierarchyStructure() throws Exception {
        val childName = getRandomString();
        val childNameSub1 = getRandomString();
        val childNameSub2 = getRandomString();

        val childFid = newFolder(childName, parentFid);
        fillFolder(NUMBER_OF_MAILS, parentFid, subject);
        fillFolder(NUMBER_OF_MAILS, childFid, subject);

        val childFidSub1 = newFolder(childNameSub1, childFid);
        val childFidSub2 = newFolder(childNameSub2, childFid);

        fillFolder(NUMBER_OF_MAILS, childFidSub1, subject);
        fillFolder(NUMBER_OF_MAILS, childFidSub2, subject);

        deleteFolder(parentFid).post(shouldBe(okSync()));

        //проверяем что папки каскадно исчезли
        assertThat(authClient, withWaitFor(not(hasFolder(parentName)), WAIT_TIME));
        assertThat(authClient, not(hasFolder(childFid)));
        assertThat(authClient, not(hasFolderWithFid(childFidSub1)));
        assertThat(authClient, not(hasFolderWithFid(childFidSub2)));

        assertThat("[DARIA-25188] при удалении родительской папки, не все письма переместились в удаленные ", authClient,
                hasMsgsIn(subject, 4 * NUMBER_OF_MAILS, folderList.deletedFID()));
    }

    @Test
    @Description("Создаем структуру в виде дерева:\n" +
            "родительская папка - дочерняя папка = две дочерние папки\n" +
            "Удаляем дочернюю папку, проверяем, что в удаленных 3 x NUMBER_OF_MAILS писем")
    public void deleteChildFolderWithHierarchyStructure() throws Exception {
        val childName = getRandomString();
        val childNameSub1 = getRandomString();
        val childNameSub2 = getRandomString();
        val childFid = newFolder(childName, parentFid);

        fillFolder(NUMBER_OF_MAILS, parentFid, subject);
        fillFolder(NUMBER_OF_MAILS, childFid, subject);

        val childFidSub1 = newFolder(childNameSub1, childFid);
        val childFidSub2 = newFolder(childNameSub2, childFid);

        fillFolder(NUMBER_OF_MAILS, childFidSub1, subject);
        fillFolder(NUMBER_OF_MAILS, childFidSub2, subject);

        deleteFolder(childFid).post(shouldBe(okSync()));

        //проверяем что папки каскадно исчезли
        assertThat(authClient, withWaitFor(not(hasFolderWithFid(childFid)), WAIT_TIME));
        assertThat(authClient, hasFolder(parentName));
        assertThat(authClient, not(hasFolderWithFid(childFidSub1)));
        assertThat(authClient, not(hasFolderWithFid(childFidSub2)));

        assertThat("[DARIA-25188] при удалении дочерней папки, не все письма переместились в удаленные ", authClient,
                hasMsgsIn(subject, 3 * NUMBER_OF_MAILS, folderList.deletedFID()));
        //проверяем, что с родительской папкой ничего не произошло
        assertThat("При удалении дочерней папки, из родительской папки удалились письма ",
                authClient, hasMsgsIn(subject, NUMBER_OF_MAILS, parentFid));
    }

    @Test
    @Issues({@Issue("MAILDEV-647"), @Issue("DARIA-25484")})
    @Description("Создаем в родительской папке подпапку Архив\n" +
            "Удаляем родительскую папку\n" +
            "Проверяем что осталась папка Архив, содержащая письма")
    public void deleteArchiveFolder() throws Exception {
        val childFid = newFolder(ARCHIVE_FOLDER_NAME, parentFid);

        fillFolder(NUMBER_OF_MAILS, childFid, subject);

        updateFolderSymbol(childFid).withSymbol(ARCHIVE.value()).post(shouldBe(okEmptyJson()));

        deleteFolder(parentFid).post(shouldBe(okSync()));

        assertThat(authClient, not(hasFolder(parentName)));
        assertThat("[DARIA-25484] папка 'Архив' удалилась", authClient, hasFolder(ARCHIVE_FOLDER_NAME));
        assertThat("[DARIA-25484] письма из папки 'Архив' удалились", authClient,
                hasMsgsIn(subject, NUMBER_OF_MAILS, childFid));
    }

    @Test
    @Title("Удаляем папку с несуществующим fid")
    @Description("В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void foldersDeleteNotExistFolder() throws Exception {
        deleteFolder(NOT_EXIST_FID).post(shouldBe(okSync()));
    }

    @Test
    @Title("Удаляем папку с невалидным fid")
    public void foldersDeleteInvalidFid() throws Exception {
        deleteFolder(INVALID_FID).post(shouldBe(invalidRequest()));
    }
}
