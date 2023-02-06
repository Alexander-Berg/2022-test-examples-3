package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.PositionMatcher.hasOrder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newChildFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderMove.move;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetOptions.setSortOptions;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 24.02.14
 * Time: 15:30
 * Для начальной инициализации, по хорошему нужен новый юзер,
 * где порядок папок еще не инициализирован
 * [DARIA-34559]
 */
@Aqua.Test
@Title("Начальная инициализация порядка следования папок")
@Description("Проверяем лексикографический порядок при инициализации")
@Credentials(loginGroup = "SortFolderInitTest")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Issue("DARIA-34559")
public class SortFolderInitTest extends BaseTest {
    //Тут задаем папки, которые необходимо создать, в порядке, в котором будем проверять
    public static final List<String> expected = Arrays.asList(
            "1",
            "A",
            "b",
            "C",
            "d",
            "e",
            "~"
    );

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Before
    public void prepareFolders() throws IOException {
        createFolders(expected);
    }

    public void createFolders(List<String> folders) throws IOException {
        for (String folder : folders) {
            newFolder(folder).post().via(hc);
        }
    }

    @Test
    @Issue("DARIA-34559")
    @Description("Создаем папки.\n" +
            "Дергаем ручку sort_folders,\n" +
            "тем самым инициализируя порядок папок\n" +
            "Ожидаемый результат: должны быть в лексикографическом порядке\n" +
            "[DARIA-34559]\n" +
            "(бага заключалось в том, что сравнивали без приведения к одному регистру)")
    public void initFoldersTest() throws Exception {
        logger.warn("[DARIA-34559]");
        setSortOptions(folderList.defaultFID(), folderList.draftFID()).post().via(hc);
        assertThat("Папки при инициализации не имееют лексикографический порядок [DARIA-34559]",
                hc, hasOrder(expected));
    }

    @Test
    @Description("Создаем папку, проверяем, что она встала в конец")
    public void testWithCreateFolder() throws Exception {
        setSortOptions(folderList.defaultFID(), folderList.draftFID()).post().via(hc);
        String folder = Util.getRandomString();
        newFolder(folder).post().via(hc);

        List<String> exp = new ArrayList<>();
        exp.addAll(expected);
        exp.add(folder);

        assertThat(String.format("Созданная папка <%s> не встала в конец ", folder), hc, hasOrder(exp));
    }

    @Test
    @IgnoreForPg
    @Issues({@Issue("MAILPG-120"), @Issue("MAILPG-743")})
    @Description("Переносим подпапку в корень,\n" +
            "где уже есть отсортированные папки,\n" +
            "проверяем, что встала в конец (для pg вначало)")
    public void testWithSettingsFolderMove() throws Exception {
        setSortOptions(folderList.defaultFID(), folderList.draftFID()).post().via(hc);
        String parent = Util.getRandomString();
        String child = Util.getRandomString();
        newFolder(parent).post().via(hc);
        newChildFolder(child, jsx(FolderList.class).post().via(hc).getFolderId(parent))
                .post().via(hc);
        String fid = api(FolderList.class).post().via(hc).getFidSubfolder(child, parent);
        //перетаскиваем на вверхний уровень
        move(fid, "0").post().via(hc);
        List<String> exp = new ArrayList<>();
        exp.addAll(expected);
        exp.add(parent);
        exp.add(child);

        assertThat(String.format("Перенесенная в корень папка с именем <%s> не встала в конец ", child),
                hc, hasOrder(exp));
    }
}
