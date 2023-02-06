package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolderWithNameMatcher.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereSubfolderMatcher.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newChildFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderMove.move;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.FolderUtils.*;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FOLDER_DELAYED;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.DB_UNKNOWN_ERROR_1000;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 7/8/13
 * Time: 7:50 PM
 * <p/>
 * settings_folder_move&fid=<fid>&parent_id=<fid>
 * Пользователь testsubfolder-pg запасной
 */
@Aqua.Test
@Title("Перемещение пустой папки с одного уровня на другой")
@Description("Проверка ручки settings_folder_move")
@Credentials(loginGroup = "SubfolderTest")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
public class MoveEmptyFolder extends BaseTest {

    public static String childName;

    public static String childFid;
    public static String parentNameFrom;

    public static String parentFidFrom;
    public static String parentNameTo;

    public static String parentFidTo;

    public static String subject;
    public static final String NOT_EXIST_FID = "2080000120000081623";

    public static final int LEVEL = 3;

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient);

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).after(false).all();

    @Before
    public void prepareFolders() throws Exception {
        parentNameFrom = Util.getRandomString();
        parentNameTo = Util.getRandomString();
        childName = Util.getRandomString();

        newFolder(parentNameFrom).post().via(hc);
        parentFidFrom = jsx(FolderList.class).post().via(hc).getFolderId(parentNameFrom);
        newFolder(parentNameTo).post().via(hc);
        parentFidTo = jsx(FolderList.class).post().via(hc).getFolderId(parentNameTo);

        createSubfolderWithNameOfParent(parentNameFrom, childName);
        childFid = api(FolderList.class).post().via(hc).getFidSubfolder(childName, parentNameFrom);

        assertThat(hc, hasFolder(parentNameFrom));
        assertThat(hc, hasFolder(parentNameTo));
    }

    @Test
    @Description("Перемещение подпапки из одной\n" +
            "папки в другую")
    public void moveSubfolderToFolder() throws Exception {
        move(childFid, parentFidTo).post().via(hc);
        assertThat(hc, hasSubfolder(childName, parentNameTo));
        assertThat(hc, not(hasSubfolder(childName, parentNameFrom)));
    }

    @Test
    @Description("Перемещение подпапки из одной\n" +
            "папки в другую и обратно")
    public void moveSubfolderAndBack() throws Exception {
        move(childFid, parentFidTo).post().via(hc);
        move(childFid, parentFidFrom).post().via(hc);
        //ничего не изменилось
        assertThat(hc, hasSubfolder(childName, parentNameFrom));
        assertThat(hc, not(hasSubfolder(childName, parentNameTo)));

    }

    @Test
    @Issue("MAILPG-914")
    @Description("Перемещение с неправильным fid подпапки\n" +
            "и с неправильным fid папки\n" +
            "Ожидаемый результат: ничего не переместилось,\n" +
            "возвращаемая ошибка INVALID_ARGUMENT_5001")
    public void moveNullSubfolder() throws Exception {
        move("", parentFidTo).post().via(hc).shouldBe().errorcode(INVALID_ARGUMENT_5001);
        move(childFid, "").post().via(hc).shouldBe().errorcode(INVALID_ARGUMENT_5001);
        move("", "").post().via(hc).shouldBe().errorcode(INVALID_ARGUMENT_5001);
        //проверяем, что подпапка осталась на месте
        assertThat(hc, hasSubfolder(childName, parentNameFrom));
        assertThat(hc, not(hasSubfolder(childName, parentNameTo)));
    }

    @Test
    @Issue("DARIA-27281")
    @Description("Попытка перемещения подпапки в папку,\n" +
            "где уже есть такая подпапка\n" +
            "Ожидаемый результат: ничего не переместилось,\n" +
            "возвращаемая ошибка DB_UNKNOWN_ERROR_1000")
    public void moveToFolderWithSameSubfolder() throws Exception {
        logger.warn("[DARIA-27281]");
        createSubfolderWithNameOfParent(parentNameTo, childName);
        move(childFid, parentFidTo).post().via(hc).shouldBe().errorcode(DB_UNKNOWN_ERROR_1000);

        assertThat(hc, hasSubfolder(childName, parentNameTo));
        assertThat(hc, hasSubfolder(childName, parentNameFrom));
    }

    @Test
    @Issue("DARIA-1690")
    @Description("Перемещение подпапки в системную папку\n" +
            "settings_folder_move&fid=<фид>&parent_id=0 => ok, перемещаем на верхний уровень\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id входящие> => ok, перемещаем в \"входящие\"\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id исходящие> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id отправленные> => ок, перемещаем в \"отправленные\"\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id спам> => 5001 INVALID_ARGUMENT\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id удаленные> => ok, MPROTO-2108\n" +
            "settings_folder_move&fid=<фид>&parent_id=<id черновики> => ок, перемещаем в \"черновики\"")
    public void moveSubfolderToSystemFolder() throws Exception {
        logger.warn("DARIA-1690");
        //папки исходящих пока не существует, отправляем отложенное письмо, чтобы она появилась
        //now + 100 days
        MailSendMsgObj msg = msgFactory.getDelayedMsg(DAYS.toMillis(100));
        clean.subject(msg.getSubj());
        //отправляем письмо
        jsx(SendMessage.class).params(msg).post().via(hc);

        move(childFid, folderList.sentFID()).post().via(hc);
        assertThat(hc, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(hc, hasSubfolder(childName, folderList.sentName()));

        move(childFid, folderList.draftFID()).post().via(hc);
        assertThat(hc, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(hc, hasSubfolder(childName, folderList.draftName()));

        move(childFid, folderList.deletedFID()).post().via(hc);
        assertThat(hc, not(hasSubfolder(childName, parentNameFrom)));
        assertThat(hc, hasSubfolder(childName, folderList.deletedName()));

        move(childFid, folderList.defaultFID()).post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);
        move(childFid, folderList.spamFID()).post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);
        move(childFid, jsx(FolderList.class).post().via(hc).getFolderId(FOLDER_DELAYED))
                .post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);
    }

    @Test
    @Issue("MAILPG-914")
    @Description("Попытка переместить несуществующую папку (с несуществующим fid в этом ящике)\n" +
            "Ожидаемый результат: ошибка, нет такой")
    public void cantMoveFolderThatNotExist() throws Exception {
        Document folderListBeforeDeleting = api(FolderList.class).post().via(hc).toDocument();
        move(NOT_EXIST_FID, parentFidTo).post().via(hc).shouldBe().errorcode(INVALID_ARGUMENT_5001);
        assertThat(hc, hasSubfolder(childName, parentNameFrom));
        assertThat(hc, not(hasSubfolder(childName, parentNameTo)));
        currentFolderListShouldBeEqualTo(folderListBeforeDeleting);
    }

    @Test
    @Issue("DARIA-27280")
    @Description("Перемещение пустой папки со\n" +
            "произвольным уровнем вложенности\n" +
            "проверка, что папка переместилась\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveEmptySubfolderHierarchyLevel() throws IOException {
        logger.warn("[DARIA-27280]");
        String movedName = Util.getRandomString();

        String fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        String fromName = api(FolderList.class).post().via(hc).getFolderName(fromFid);

        String toFid = createInnerFolderStructure(parentFidTo, LEVEL);
        String toName = api(FolderList.class).post().via(hc).getFolderName(toFid);
        newChildFolder(movedName, fromFid).post().via(hc);
        String movedFid = api(FolderList.class).post().via(hc).getFidSubfolder(movedName, fromName);

        move(movedFid, toFid).post().via(hc);

        assertThat(hc, hasSubfolder(movedName, toName));
        assertThat(hc, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение пустой папки в папку с\n" +
            "неправильным уровнем вложенности\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveEmptySubfolderWrongHierarchyLevel() throws IOException {
        String movedName = Util.getRandomString();

        String fromFid = createInnerFolderStructure(parentFidFrom, LEVEL + 2);
        String fromName = api(FolderList.class).post().via(hc).getFolderName(fromFid);

        String toFid = createInnerFolderStructure(parentFidTo, LEVEL);
        String toName = api(FolderList.class).post().via(hc).getFolderName(toFid);
        newChildFolder(movedName, fromFid).post().via(hc);
        String movedFid = api(FolderList.class).post().via(hc).getFidSubfolder(movedName, fromName);

        move(movedFid, toFid).post().via(hc);

        assertThat(hc, hasSubfolder(movedName, toName));
        assertThat(hc, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень\n" +
            "т.е. вызываем move с parent_id = 0\n" +
            "ожидаемый результат: все переместилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveEmptySubfolderUpHierarchyLevel() throws IOException {
        String movedName = Util.getRandomString();

        String fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        String fromName = api(FolderList.class).post().via(hc).getFolderName(fromFid);

        newChildFolder(movedName, fromFid).post().via(hc);
        String movedFid = api(FolderList.class).post().via(hc).getFidSubfolder(movedName, fromName);

        move(movedFid, "0").post().via(hc);

        assertThat(hc, hasFolder(movedName));
        assertThat(hc, not(hasSubfolder(movedName, fromName)));
    }

    @Test
    @Description("Перемещение папки на верхний уровень\n" +
            "ожидаемый результат: ничего не изменилось\n" +
            "Проверяет, что можно перемещать папки\n" +
            "с именем превышающим 80 символов")
    public void moveEmptyFolderUpHierarchyLevel() throws IOException {
        String fromFid = createInnerFolderStructure(parentFidFrom, LEVEL);
        String fromName = api(FolderList.class).post().via(hc).getFolderName(fromFid);

        move(parentFidFrom, "0").post().via(hc);
        assertThat(hc, hasFolder(parentNameFrom));
        assertThat(hc, not(hasSubfolder(fromName, parentFidFrom)));
    }
}
