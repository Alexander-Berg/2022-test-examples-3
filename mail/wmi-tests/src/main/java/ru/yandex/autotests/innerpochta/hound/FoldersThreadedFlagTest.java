package ru.yandex.autotests.innerpochta.hound;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;

@Aqua.Test
@Title("Тест флага тредируемости папки")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "FoldersThreadedFlagTest")
@Issue("MAILPG-1049")
public class FoldersThreadedFlagTest extends BaseHoundTest {
    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all();

    @Test
    @Description("Проверка тредируемости дефолтных папок")
    public void testSystemFolders() {
        Folders foldersCommand = folders(empty().setUid(uid())).get().via(authClient);

        assertTrue("Отправленные должны тредироваться", foldersCommand.isFolderThreaded(folderList.sentFID()));
        assertTrue("Черновики должны тредироваться", foldersCommand.isFolderThreaded(folderList.draftFID()));
        assertTrue("Входящие должны тредироваться", foldersCommand.isFolderThreaded(folderList.defaultFID()));

        assertFalse("Спам должен не тредироваться", foldersCommand.isFolderThreaded(folderList.spamFID()));
        assertFalse("Удалённые должны не тредироваться", foldersCommand.isFolderThreaded(folderList.deletedFID()));
    }

    @Test
    @Description("Проверка тредируемости новой папки")
    public void testNewFolder() {
        String folderName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);

        Folders foldersCommand = folders(empty().setUid(uid()))
                .get().via(authClient).statusCodeShouldBe(HttpStatus.OK_200);

        assertTrue("Новая папка должна тредироваться", foldersCommand.isFolderThreaded(fid));
    }
}
