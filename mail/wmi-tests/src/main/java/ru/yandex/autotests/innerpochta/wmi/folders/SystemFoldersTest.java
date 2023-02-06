package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderDeleteObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDelete;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;

import java.util.Arrays;
import java.util.List;

import static java.util.function.Function.identity;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.FolderUtils.currentFolderListShouldBeEqualTo;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.DB_UNIQUE_CONSTRAINT_VIOLATED_1003;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.08.14
 * Time: 15:47
 */
@Aqua.Test
@Title("Системные папки. Различные тесты")
@Features(MyFeatures.WMI)
@Stories({MyStories.FOLDERS, MyStories.SYSTEM_FOLDERS})
@Description("Различные кейсы с системными папками: очистка, удаление, снятие символа")
@Credentials(loginGroup = "SystemFoldersTest")
@RunWith(Parameterized.class)
public class SystemFoldersTest extends BaseTest {

    @Parameterized.Parameter
    public Symbol symbol;

    @Parameterized.Parameters(name = "System folder-{0}")
    public static List<Symbol> systemFolders() throws Exception {
        return Arrays.asList(
                Symbol.INBOX,
                Symbol.TRASH,
                Symbol.SPAM,
                Symbol.SENT,
                Symbol.DRAFT);
    }

    @Test
    @Description("Пытаемся удалить системные папки\n" +
            "Ожидаемый результат: ошибка, неверный аргумент, 5001")
    public void cantDeleteSystemFolder() throws Exception {
        Document folderListBeforeTestCase = api(FolderList.class).post().via(hc).toDocument();
        String fid = folderList.get().fidBySymbol(symbol);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        currentFolderListShouldBeEqualTo(folderListBeforeTestCase);
    }

    @Test
    @Description("Проверка отсутствия создания системной папки\n" +
            "Ожидаемый результат: ошибка, такая папка уже есть\n")
    public void cantCreateSystemFolder() throws Exception {
        Document folderListBefore = api(FolderList.class).post().via(hc).toDocument();
        newFolder(folderList.get().nameBySymbol(symbol)).post().via(hc).shouldBe()
                .errorcode(DB_UNIQUE_CONSTRAINT_VIOLATED_1003);
        currentFolderListShouldBeEqualTo(folderListBefore);
    }

    @Test
    @Issue("DARIA-49310")
    @Stories(MyStories.MOPS)
    @Description("Пробуем создать системную через mops новый метод folders/create\n" +
            "Ожидаемый результат: ошибка, такая папка уже есть\n")
    public void cantCreateSystemFolderWithMops() throws Exception {
        Document folderListBefore = api(FolderList.class).post().via(hc).toDocument();
        Mops.debugPrint(Mops.createFolder(authClient, folderList.get().nameBySymbol(symbol)).post(identity()));
        currentFolderListShouldBeEqualTo(folderListBefore);
    }

}
