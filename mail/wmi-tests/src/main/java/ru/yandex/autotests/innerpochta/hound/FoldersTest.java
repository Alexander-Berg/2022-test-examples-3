package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.beans.yplatform.Folder;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Optional;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByLabelObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка folders")
@Description("Тесты на ручку folders")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "HoundFoldersTest")
public class FoldersTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all();

    @Test
    @Title("Ручка folders с системной и пользовательской папкой")
    @Description("Создаём папку." +
            "Проверяем, что ручка возвращает созданную папку и хотя бы одну системную.")
    public void testFoldersHandlerWithUserAndSystemFolder() {
        String folderName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);

        Folders folders = api(Folders.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid()))
                .get()
                .via(authClient).withDebugPrint();

        assertTrue("Не нашли ни одной системной папки", folders.folders().entrySet().stream()
                .anyMatch(folder -> folder.getValue().getIsSystem()));
        assertTrue("Не нашли созданную папку", Optional.ofNullable(folders.folders())
                .map(map -> map.get(fid))
                .filter(folder -> folder.getName().equals(folderName))
                .isPresent());
    }

    @Test
    @Title("Проверка обновления полей scn и revision пользовательской папки")
    @Description("Создаём папку, в цикле изменяем её содержимое и проверяем, что поля scn и revision изменяются.")
    public void testFoldersScnAndRevisionUpdate() {
        Folder initialFolder = GetSentFolder();
        sendWith(authClient).viaProd().send().strict().waitDeliver();
        Folder modifiedFolder = GetSentFolder();

        assertNotEquals("Поле 'revision' отсутствует", null,
                modifiedFolder.getRevision());
        assertNotEquals("Поле 'scn' отсутствует", null,
                modifiedFolder.getScn());

        assertEquals("Значения полей 'revision' и 'scn' различаются",
                modifiedFolder.getRevision().toString(),
                modifiedFolder.getScn());

        assertThat("Поле 'revision' не увеличилось",
                modifiedFolder.getRevision(),
                greaterThan(initialFolder.getRevision()));
        final int radix = 10;
        assertThat("Поле 'scn' не увеличилось",
                Long.parseLong(modifiedFolder.getScn(), radix),
                greaterThan(Long.parseLong(initialFolder.getScn(), radix)));
    }

    private Folder GetSentFolder() {
        Folders folders = api(Folders.class).setHost(props().houndUri())
                .params(empty().setUid(uid())).get().via(authClient).withDebugPrint();

        return Optional.ofNullable(folders.folders()).map(map -> map.get(folderList.sentFID())).get();
    }
}
