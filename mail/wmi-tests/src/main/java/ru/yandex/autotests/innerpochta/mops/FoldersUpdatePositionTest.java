package ru.yandex.autotests.innerpochta.mops;

import com.google.common.base.Joiner;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.updateposition.ApiFoldersUpdatePosition;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.NOT_EXIST_FID;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasOrder;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.11.15
 * Time: 19:16
 */
@Aqua.Test
@Title("[MOPS] Перемещение папки на одном уровне")
@Description("Перемещаем папки с письмами и без писем различными способами")
@Credentials(loginGroup = "MopsFoldersUpdatePositionTest")
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Issues({@Issue("DARIA-53598"), @Issue("DARIA-49310")})
public class FoldersUpdatePositionTest extends MopsBaseTest {
    private static final String childName = getRandomString();
    private static final String prevName = getRandomString();
    private static final String parentName = getRandomString();

    private static final int SUBFOLDERS_AMOUNT = 3;
    private static final int COUNT = 8;

    private String childFid;
    private String prevFid;
    private String parentFid;

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).after(false).all();

    static String takeFullFolderName(String parentName, String childName) {
        return Joiner.on("|").join(parentName, childName);
    }

    static List<String> createSubfolders(String parentFid, String parentName, String prevName, int subfolderAmount) {
        List<String> result = new ArrayList<String>();
        String name;
        String fullName;

        for (int i = 0; i < subfolderAmount; i++) {
            name = Util.getRandomString();
            fullName = takeFullFolderName(parentName, name);
            result.add(fullName);

            createInnerFolderStructure(parentFid, name, 1);

            assertThat(authClient, hasSubfolder(name, parentName));
        }

        String fullPrefName = takeFullFolderName(parentName, prevName);
        result.add(fullPrefName);

        Collections.sort(result);
        return result;

    }

    static List<String> moveMops(ApiFoldersUpdatePosition api, String name, List<String> rightOrder) {
        api.post(shouldBe(okEmptyJson()));
        rightOrder.remove(name);
        rightOrder.add(0, name);
        return rightOrder;
    }

    static List<String> moveMops(ApiFoldersUpdatePosition api, String name,
                                        String namePrev, List<String> rightOrder){
        api.post(shouldBe(okEmptyJson()));

        rightOrder.remove(name);
        val index = rightOrder.indexOf(namePrev);
        rightOrder.add(index + 1, name);
        return rightOrder;
    }

    static List<String> moveMopsWithWrongLevel(ApiFoldersUpdatePosition api, String name,
                                               List<String> rightOrder) throws IOException {
        api.post(shouldBe(okEmptyJson()));
        rightOrder.remove(name);
        rightOrder.add(0, name);
        return rightOrder;
    }

    //создаем все папки которые в последствии будем сортировать
    @Before
    public void prepareFolders() throws Exception {
        parentFid = newFolder(parentName);
        childFid = newFolder(childName, parentFid);
        prevFid = newFolder(prevName, parentFid);
        assertThat(authClient, hasFolder(parentName));
    }

    @Test
    @Issues({@Issue("DARIA-27063"), @Issue("DARIA-53598")})
    @Description("Перемещение папки\n" +
            "на вверх в текущей директории")
    public void moveFolderUp() throws Exception {
        logger.warn("[DARIA-27063]");
        List<String> rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        for (int i = 0; i < COUNT; i++) {
            rightOrder = moveMops(updateFolderPosition(childFid), folderList.nameByFid(childFid), rightOrder);
            assertThat(authClient, hasSubfolder(childName, parentName));
            assertThat(authClient, hasOrder(rightOrder));
        }
    }

    @Test
    @Description("Изменение места подпапки\n" +
            "в текущей директории\n" +
            "Ожидаемый результат:\n" +
            "папка стоит после prevSubfolder")
    public void moveFolderToPrev() throws Exception {
        List<String> rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        for (int i = 0; i < COUNT; i++) {
            rightOrder = moveMops(updateFolderPosition(childFid).withPrevFid(prevFid),
                    folderList.nameByFid(childFid),
                    folderList.nameByFid(prevFid),
                    rightOrder);
            assertThat(authClient, hasSubfolder(childName, parentName));
            assertThat(authClient, hasOrder(rightOrder));
        }
    }

    @Test
    @Issue("DARIA-53598")
    @Description("Пытаемся переместить системные папки в начало\n")
    public void moveSystemFoldersUp() throws IOException {
        List<String> rightOrder = new ArrayList<>();
        rightOrder = moveMops(updateFolderPosition(folderList.defaultFID()),
                folderList.nameBySymbol(Symbol.INBOX), rightOrder);
        rightOrder = moveMops(updateFolderPosition(folderList.sentFID()),
                folderList.nameBySymbol(Symbol.SENT), rightOrder);
        rightOrder = moveMops(updateFolderPosition(folderList.deletedFID()),
                folderList.nameBySymbol(Symbol.TRASH), rightOrder);
        rightOrder = moveMops(updateFolderPosition(folderList.spamFID()),
                folderList.nameBySymbol(Symbol.SPAM), rightOrder);
        rightOrder = moveMops(updateFolderPosition(folderList.draftFID()),
                folderList.nameBySymbol(Symbol.DRAFT), rightOrder);
        assertThat(authClient, hasOrder(rightOrder));
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
        List<String> rightOrder = new ArrayList<>();
        rightOrder.add(folderList.nameBySymbol(Symbol.INBOX));
        rightOrder.add(folderList.nameBySymbol(Symbol.SENT));
        rightOrder.add(folderList.nameBySymbol(Symbol.TRASH));
        rightOrder.add(folderList.nameBySymbol(Symbol.SPAM));
        rightOrder.add(folderList.nameBySymbol(Symbol.DRAFT));

        Collections.sort(rightOrder);

        //отправленные -> входящими
        rightOrder = moveMops(updateFolderPosition(folderList.sentFID()).withPrevFid(folderList.defaultFID()),
                folderList.nameBySymbol(Symbol.SENT), folderList.nameBySymbol(Symbol.INBOX), rightOrder);
        //удаленные -> отправленные
        rightOrder = moveMops(updateFolderPosition(folderList.deletedFID()).withPrevFid(folderList.sentFID()),
                folderList.nameBySymbol(Symbol.TRASH), folderList.nameBySymbol(Symbol.SENT), rightOrder);
        //спам -> удаленные
        rightOrder = moveMops(updateFolderPosition(folderList.spamFID()).withPrevFid(folderList.deletedFID()),
                folderList.nameBySymbol(Symbol.SPAM), folderList.nameBySymbol(Symbol.TRASH), rightOrder);
        //черновики -> спамом
        rightOrder = moveMops(updateFolderPosition(folderList.draftFID()).withPrevFid(folderList.spamFID()),
                folderList.nameBySymbol(Symbol.DRAFT), folderList.nameBySymbol(Symbol.SPAM), rightOrder);

        assertThat(authClient, hasOrder(rightOrder));
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
        List<String> rightOrder = new ArrayList<>();
        rightOrder.add(folderList.nameBySymbol(Symbol.INBOX));
        rightOrder.add(folderList.nameBySymbol(Symbol.SENT));
        rightOrder.add(folderList.nameBySymbol(Symbol.TRASH));
        rightOrder.add(folderList.nameBySymbol(Symbol.SPAM));
        rightOrder.add(folderList.nameBySymbol(Symbol.DRAFT));

        Collections.sort(rightOrder);

        //входящие -> отправленные
        rightOrder = moveMops(updateFolderPosition(folderList.defaultFID()).withPrevFid(folderList.sentFID()),
                folderList.nameBySymbol(Symbol.INBOX), folderList.nameBySymbol(Symbol.SENT), rightOrder);
        //спам -> входящие
        rightOrder = moveMops(updateFolderPosition(folderList.spamFID()).withPrevFid(folderList.defaultFID()),
                folderList.nameBySymbol(Symbol.SPAM), folderList.nameBySymbol(Symbol.INBOX), rightOrder);
        //черновики -> спам
        rightOrder = moveMops(updateFolderPosition(folderList.draftFID()).withPrevFid(folderList.spamFID()),
                folderList.nameBySymbol(Symbol.DRAFT), folderList.nameBySymbol(Symbol.SPAM), rightOrder);
        //удаленные -> черновики
        rightOrder = moveMops(updateFolderPosition(folderList.deletedFID()).withPrevFid(folderList.draftFID()),
                folderList.nameBySymbol(Symbol.TRASH), folderList.nameBySymbol(Symbol.DRAFT), rightOrder);
        assertThat(authClient, hasOrder(rightOrder));
    }

    @Test
    @Issue("MAILPG-593")
    @Description("Попытка переместить несуществующую папку (с несуществующим fid в этом ящике)\n" +
            "Ожидаемый результат: ошибка, нет такой")
    public void cantMoveFolderThatNotExist() {
        updateFolderPosition(NOT_EXIST_FID).withPrevFid(parentFid).post(shouldBe(invalidRequest()));

        //проверяем, что подпапка осталась на месте
        assertThat(authClient, hasSubfolder(childName, parentName));

        FolderList newList = new FolderList(authClient);
        assertThat("Папка исчезла", newList.folders(), equalTo(folderList.folders()));
    }

    @Test
    @Issue("DARIA-53598")
    @Description("Пробуем переместить много папок в начало")
    public void moveAllFoldersUp() throws Exception {
        List<String> rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        val fids = folderList.nonsystemFids();

        fids.remove(parentFid);
        for (val fid : fids) {
            val api = updateFolderPosition(fid);
            rightOrder = moveMops(api, folderList.nameByFid(fid), rightOrder);
        }
        assertThat(authClient, hasSubfolder(childName, parentName));
        assertThat(authClient, hasOrder(rightOrder));
    }

    @Test
    @Issue("DARIA-27083")
    @Description("Пробуем переместить папку на другой уровень\n" +
            "Ожидаемый результат: перемещаемая папка стала первой")
    public void moveFolderToAnotherLevel() throws Exception {
        logger.warn("[DARIA-27083]");
        val rightOrder = createSubfolders(parentFid, parentName, prevName, SUBFOLDERS_AMOUNT);
        //порядок не должен измениться
        for (int i = 0; i < COUNT; i++) {
            val api = updateFolderPosition(childFid).withPrevFid(folderList.defaultFID());
            moveMopsWithWrongLevel(api, folderList.nameByFid(childFid), rightOrder);
            assertThat(authClient, hasSubfolder(childName, parentName));
            assertThat(authClient, hasOrder(rightOrder));
        }
    }
}