package ru.yandex.autotests.innerpochta.api;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderCreateObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderDelete.delete;

@Aqua.Test
@Title("[API] Создание папки API ручкой")
@Description("Создание папки апи ручкой, создающей фильтры и папки. Необходимо для мобильной почты и пдд")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "RpopAndFilterApiOperations")
public class FolderCreationWithApiMethod extends BaseTest {

    @Test
    @Description("Создаем папку при помощи апи ручки на создание фильтров и папок (settings_rpop_filter_create)\n" +
            "- Проверяем что папка создалась\n" +
            "Удаляем папку")
    public void createAndDeleteFolder() throws Exception {
        String folderName = Util.getRandomString();

        api(SettingsFolderCreate.class).params(SettingsFolderCreateObj.newFolder(folderName)).post().via(hc);

        FolderList resp = api(FolderList.class).post().via(hc);
        assertTrue("Папки " + folderName + " не найдено.", resp.isThereFolder(folderName));
        String folderId = resp.getFolderId(folderName);
        delete(folderId).post().via(hc);
    }
}
