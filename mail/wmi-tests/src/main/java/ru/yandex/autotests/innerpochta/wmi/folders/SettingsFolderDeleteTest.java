package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderDeleteObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDelete;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDeleteWithMsgs;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolderWithNameMatcher.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.08.14
 * Time: 16:31
 */
@Aqua.Test
@Title("Проверяем две ручки: settings_folder_delete и settings_folder_delete_with_msgs")
@Description("Удаление пользовательских папок с письмами и без, с некорректными данными\n" +
        "Удаляем с параметром forсe=true, forсe=false и без него")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "SettingsFolderDeleteTest")
public class SettingsFolderDeleteTest extends BaseTest {

    private static String folderName = Util.getRandomString();
    public static final String NOT_EXIST_FID = "12345678";
    public static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .all().allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete без force\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolder() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc);

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete с параметром force=true\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolderWithForceTrue() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc).withDebugPrint();

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete с параметром force=false\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolderWithForceFalse() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc);

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolderWithMsgs() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc)
                .withDebugPrint().shouldBe().withDebugPrint();

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs c параметром force=true\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolderWithMsgsAndForceTrue() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc).withDebugPrint();

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs c параметром force=false\n" +
            "Ожидаемый результат: папка должна удалиться")
    public void deleteEmptyFolderWithMsgsAndForceFalse() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc);

        assertThat("Пустая пользовательская папка не удалилась", hc, not(hasFolder(folderName)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete c различными параметрами\n" +
            "Ожидаемый результат: папка НЕ должна удалиться, ошибка 5012")
    public void deleteNotEmptyFolder() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        List<String> mids = sendUtils.getMids();
        String subj = sendUtils.getSubj();
        // Перемещение писем в новую папку
        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveSomeMsges(mids, fid, folderList.defaultFID()))
                .post().via(hc);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.FOLDER_NOT_EMPTY_5012);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.FOLDER_NOT_EMPTY_5012);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.FOLDER_NOT_EMPTY_5012);

        assertThat("Непустая пользовательская папка удалилась, при использовании setting_folder_delete",
                hc, hasFolder(folderName));
        assertThat("Из пользовательской папки исчезли сообщения", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS, fid)));

    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs\n" +
            "Ожидаемый результат: папка НЕ должна удалиться, ошибка 5012")
    public void deleteWithMsgsNotEmptyFolder() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        List<String> mids = sendUtils.getMids();
        String subj = sendUtils.getSubj();
        // Перемещение писем в новую папку
        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveSomeMsges(mids, fid, folderList.defaultFID()))
                .post().via(hc);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid)).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.FOLDER_NOT_EMPTY_5012);

        assertThat("Непустая пользовательская папка удалилась, без force", hc, hasFolder(folderName));
        assertThat("Из папки исчезли сообщения", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS, fid)));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs c force=true\n" +
            "Ожидаемый результат: папка должна удалиться, письма в удаленные")
    public void deleteWithMsgsAndForceTrueNotEmptyFolder() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        List<String> mids = sendUtils.getMids();
        String subj = sendUtils.getSubj();
        // Перемещение писем в новую папку
        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveSomeMsges(mids, fid, folderList.defaultFID()))
                .post().via(hc);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("true")).post().via(hc);

        assertThat("Непустая пользовательская папка НЕ удалилась", hc, not(hasFolder(folderName)));
        assertThat("Сообщения не появились в папке \"Удаленные\"", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS, folderList.deletedFID())));
    }

    @Test
    @Description("Удаляем пустую папку с помощью setting_folder_delete_with_msgs c force=false\n" +
            "Ожидаемый результат: папка НЕ должна удалиться, ошибка 5012")
    public void deleteWithMsgsAndForceFalseNotEmptyFolder() throws Exception {
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        List<String> mids = sendUtils.getMids();
        String subj = sendUtils.getSubj();
        // Перемещение писем в новую папку
        jsx(MailboxOper.class)
                .params(MailboxOperObj.moveSomeMsges(mids, fid, folderList.defaultFID()))
                .post().via(hc);

        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(fid).setForce("false")).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.FOLDER_NOT_EMPTY_5012);

        assertThat("Непустая пользователькая папка удалилась c параметром force=false", hc, hasFolder(folderName));
        assertThat("Из пользовательской папки исчезли сообщения", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS, fid)));
    }

    @Test
    @Description("Дергаем setting_folder_delete и settings_folder_delete_with_msgs с несуществубщей папкой\n" +
            "Ожидаемый результат: 5002")
    public void deleteNotExistFolder() throws IOException {
        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(NOT_EXIST_FID)).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.NO_SUCH_FOLDER_5002);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder(NOT_EXIST_FID)).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.NO_SUCH_FOLDER_5002);
    }

    @Test
    @Description("Дергаем setting_folder_delete и settings_folder_delete_with_msgs с нулевым fid\n" +
            "Ожидаемый результат: 5002")
    public void deleteFolderWithNullFid() throws IOException {
        jsx(SettingsFolderDelete.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder("0")).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.NO_SUCH_FOLDER_5002);

        jsx(SettingsFolderDeleteWithMsgs.class)
                .params(SettingsFolderDeleteObj.deleteOneFolder("0")).post().via(hc)
                .shouldBe().errorcode(WmiErrorCodes.NO_SUCH_FOLDER_5002);
    }


}
