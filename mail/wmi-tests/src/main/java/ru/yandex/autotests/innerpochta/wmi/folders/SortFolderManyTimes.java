package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

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
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetOptions.setSortOptions;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.FolderUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 04.10.13
 * Time: 19:39
 * <p/>
 * DARIA-32466
 * DARIA-32441
 * DARIA-1690
 */
@Aqua.Test
@Title("Перемещение папки на одном уровне много раз")
@Description("Перемещаем папки с письмами и без писем различными способами")
@Credentials(loginGroup = "Sortmanytimes")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
public class SortFolderManyTimes extends BaseTest {

    public static String childName;

    public static String childFid;
    public static String prevName;

    public static String prevFid;
    public static String parentName;

    public static String parentFid;

    public static String subject;

    //переменная для задания правильного порядка следования папок, то с чем будем сравнивать
    public List<String> rightOrder = new ArrayList<String>();

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
    @Description("Вызвать с prev=0 много раз")
    public void moveCurrentFolderUpManyTimes() throws IOException {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);

        for (int i = 0; i < 8; i++) {
            rightOrder = move(childFid, currList, rightOrder);
            assertThat(hc, hasSubfolder(childName, parentName));
            assertThat(hc, hasOrder(rightOrder));
        }
    }

    @Test
    @Description("Вызвать с prev=fid много раз")
    public void moveCurrentFolderToPrevManyTimes() throws IOException {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);
        for (int i = 0; i < 8; i++) {
            rightOrder = move(childFid, prevFid, currList, rightOrder);
            assertThat(hc, hasSubfolder(childName, parentName));
            assertThat(hc, hasOrder(rightOrder));
        }

    }

    @Test
    @Description("Пробуем переместить папку на другой уровень много раз\n" +
            "Ожидаемый результат: перемещаемая папка все равно стала в начало")
    public void moveFolderToAnotherLevelManyTimes() throws Exception {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);

        for (int i = 0; i < 8; i++) {
            //порядок не должен измениться
            moveWithWrongLevel(childFid, folderList.defaultFID(), currList, rightOrder);
            assertThat(hc, hasSubfolder(childName, parentName));
            assertThat(hc, hasOrder(rightOrder));
        }
    }

    @Test
    @Description("Пробуем поставить папку перед этой же папкой\n" +
            "много раз")
    public void moveFolderWithPrevThisFolder() throws IOException {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        currList = api(FolderList.class).post().via(hc);
        for (int i = 0; i < 8; i++) {
            setSortOptions(childFid, childFid).post().via(hc).shouldBe();
            assertThat(hc, hasSubfolder(childName, parentName));
            assertThat(hc, hasOrder(rightOrder));
        }


    }

    @Test
    @Description("Пробуем переместить много папок друг за другом.")
    public void moveFolderToPrevManyTimes() throws Exception {
        rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        rightOrder.add(takeFullFolderName(parentName, childName));
        Collections.sort(rightOrder);

        List<String> fids = api(FolderList.class).post().via(hc).getAllFolderIdsWithoutSystem();
        fids.remove(parentFid);
        currList = api(FolderList.class).post().via(hc);
        for (int i = 0; i < fids.size() - 1; i++) {
            rightOrder = move(fids.get(i), fids.get(i + 1), currList, rightOrder);
            assertThat(hc, hasSubfolder(childName, parentName));
            assertThat(hc, hasOrder(rightOrder));
        }

    }
}

