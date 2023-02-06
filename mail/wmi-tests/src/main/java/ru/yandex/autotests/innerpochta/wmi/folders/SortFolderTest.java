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
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolderWithNameMatcher.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereSubfolderMatcher.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.PositionMatcher.hasOrder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetOptions.firstPlaceSortOptions;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetOptions.setSortOptions;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.FolderUtils.*;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INTERNAL_ERROR_31;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 7/15/13
 * Time: 5:45 PM
 * <p/>
 * settings_folder_set_options&fid=<fid>prev=<fid>&threaded=0&notify=0
 * [DARIA-1690]
 */
@Aqua.Test
@Title("Перемещение папки на одном уровне")
@Description("Перемещаем папки с письмами и без писем различными способами")
@Credentials(loginGroup = "Testsubfoldersort")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
public class SortFolderTest extends BaseTest {

    public static String childName;
    public static String childFid;

    public static String prevName;
    public static String prevFid;

    public static String parentName;
    public static String parentFid;

    public static String subject;

    //переменная для задания правильного порядка следования папок, то с чем будем сравнивать
    public List<String> rightOrder = new ArrayList<String>();

    public static final String NOT_EXIST_FID = "12424235141";

    public static final int SUBFOLDERS_AMOUNT = 3;

    private FolderList currList;

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).after(false).all();

    //создаем все папки которые в последствии будем сортировать
    @Before
    public void prepareFolders() throws Exception {
        subject = Util.getRandomString();

        parentName = Util.getRandomString();
        childName = Util.getRandomString();
        prevName = Util.getRandomString();

        newFolder(parentName).post().via(hc);
        parentFid = jsx(FolderList.class).post().via(hc).getFolderId(parentName);

        createFolderWithNameOfParent(parentName, childName);
        childFid = api(FolderList.class).post().via(hc).getFidSubfolder(childName, parentName);

        createFolderWithNameOfParent(parentName, prevName);
        prevFid = api(FolderList.class).post().via(hc).getFidSubfolder(prevName, parentName);

        assertThat(hc, hasFolder(parentName));
    }

    @Test
    @Issue("DARIA-27063")
    @Description("Перемещение папки\n" +
            "на вверх в текущей директории")
    public void moveFolderUp() throws Exception {
        logger.warn("[DARIA-27063]");
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);
        rightOrder = move(childFid, currList, rightOrder);
        assertThat(hc, hasSubfolder(childName, parentName));
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Description("Изменение места подпапки\n" +
            "в текущей директории\n" +
            "Ожидаемый результат:\n" +
            "папка стоит после prevSubfolder")
    public void moveFolderToPrev() throws Exception {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);
        rightOrder = move(childFid, prevFid, currList, rightOrder);
        assertThat(hc, hasSubfolder(childName, parentName));
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Description("Пытаемся переместить системные папки в начало\n")
    public void moveSystemFoldersUp() throws IOException {
        currList = api(FolderList.class).post().via(hc);
        rightOrder = move(currList.defaultFID(), currList, rightOrder);
        rightOrder = move(currList.sentFID(), currList, rightOrder);
        rightOrder = move(currList.deletedFID(), currList, rightOrder);
        rightOrder = move(currList.spamFID(), currList, rightOrder);
        rightOrder = move(currList.draftFID(), currList, rightOrder);
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Description("Пытаемся переместить системные папки друг за другом\n" +
            "Так как не знаем порядок, перемещаем образуя цепочку\n" +
            "Ожидаемый результат:\n" +
            "входящие\n" +
            "отправленные\n" +
            "удаленные\n" +
            "спам\n" +
            "черновики\n")
    public void moveDefaultFoldersInDefaultChain() throws IOException {
        rightOrder.add(folderList.defaultName());
        rightOrder.add(folderList.sentName());
        rightOrder.add(folderList.deletedName());
        rightOrder.add(folderList.spamName());
        rightOrder.add(folderList.draftName());

        Collections.sort(rightOrder);

        List<String> fids = api(FolderList.class).post().via(hc).getAllFolderIds();
        fids.remove(parentFid);
        fids.remove(childFid);
        fids.remove(prevFid);
        currList = api(FolderList.class).post().via(hc);

        //отправленные -> входящими
        rightOrder = move(currList.sentFID(), currList.defaultFID(), currList, rightOrder);
        //удаленные -> отправленные
        rightOrder = move(currList.deletedFID(), currList.sentFID(), currList, rightOrder);
        //спам -> удаленные
        rightOrder = move(currList.spamFID(), currList.deletedFID(), currList, rightOrder);
        //черновики -> спамом
        rightOrder = move(currList.draftFID(), currList.spamFID(), currList, rightOrder);

        api(FolderList.class).post().via(hc).withDebugPrint();
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Description("Пытаемся переместить системные папки друг за другом\n" +
            "Так как не знаем порядок, перемещаем образуя цепочку\n" +
            "Ожидаемый результат:\n" +
            "отправленные\n" +
            "входящие\n" +
            "спам\n" +
            "черновики\n" +
            "удаленные")
    public void moveDefaultFoldersInChain() throws IOException {
        rightOrder.add(folderList.defaultName());
        rightOrder.add(folderList.sentName());
        rightOrder.add(folderList.deletedName());
        rightOrder.add(folderList.spamName());
        rightOrder.add(folderList.draftName());

        Collections.sort(rightOrder);

        List<String> fids = api(FolderList.class).post().via(hc).getAllFolderIds();
        fids.remove(parentFid);
        fids.remove(childFid);
        fids.remove(prevFid);
        currList = api(FolderList.class).post().via(hc);

        //входящие -> отправленные
        rightOrder = move(currList.defaultFID(), currList.sentFID(), currList, rightOrder);
        //спам -> входящие
        rightOrder = move(currList.spamFID(), currList.defaultFID(), currList, rightOrder);
        //черновики -> спам
        rightOrder = move(currList.draftFID(), currList.spamFID(), currList, rightOrder);
        //удаленные -> черновики
        rightOrder = move(currList.deletedFID(), currList.draftFID(), currList, rightOrder);

        api(FolderList.class).post().via(hc).withDebugPrint();
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Issue("MAILPG-120")
    @Description("Попытка переместить несуществующую папку (с несуществующим fid в этом ящике)\n" +
            "Ожидаемый результат: ошибка, нет такой")
    public void cantMoveFolderThatNotExist() throws Exception {
        Document folderListBeforeMoving = api(FolderList.class).post().via(hc).toDocument();

        setSortOptions(NOT_EXIST_FID, parentFid).post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);

        firstPlaceSortOptions(NOT_EXIST_FID).post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);

        //проверяем, что подпапка осталась на месте
        assertThat(hc, hasSubfolder(childName, parentName));
        currentFolderListShouldBeEqualTo(folderListBeforeMoving);
    }

    @Test
    @Description("Пробуем переместить много папок в начало")
    public void moveAllFoldersUp() throws Exception {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);

        List<String> fids = api(FolderList.class).post().via(hc).getAllFolderIdsWithoutSystem();

        fids.remove(parentFid);
        currList = api(FolderList.class).post().via(hc);
        for (String fid : fids) {
            rightOrder = move(fid, currList, rightOrder);
        }
        assertThat(hc, hasSubfolder(childName, parentName));
        assertThat(hc, hasOrder(rightOrder));
    }

    @Test
    @Issue("DARIA-27083")
    @Description("Пробуем переместить папку на другой уровень\n" +
            "Ожидаемый результат: перемещаемая папка стала первой")
    public void moveFolderToAnotherLevel() throws Exception {
        logger.warn("[DARIA-27083]");
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);
        //порядок не должен измениться
        moveWithWrongLevel(childFid, folderList.defaultFID(), currList, rightOrder);

        assertThat(hc, hasSubfolder(childName, parentName));
        assertThat(hc, hasOrder(rightOrder));
    }
}