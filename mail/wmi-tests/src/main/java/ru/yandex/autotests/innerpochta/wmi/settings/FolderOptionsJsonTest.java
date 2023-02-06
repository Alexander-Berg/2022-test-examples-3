package ru.yandex.autotests.innerpochta.wmi.settings;


/**
 * WMI-103
 */

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSetOptionsObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderUpdateObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetOptions;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderUpdate.settingsFolderUpdate;


@Aqua.Test
@Title("Тестирование настроек. Тэг folder_options и pop3on")
@Description("Тестируем ручку установки настроек и их выдачу в теге folder_options. Проверка вкл/выкл pop3")
@Credentials(loginGroup = "FolderOptionsJsonTest")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Issue("MAILPG-120")
public class FolderOptionsJsonTest extends BaseTest {

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    private ArrayList<SettingsFolderSetOptionsObj> optionSets = new ArrayList<SettingsFolderSetOptionsObj>();
    private ArrayList<SettingsFolderSetOptionsObj> optionSetsWithErr = new ArrayList<SettingsFolderSetOptionsObj>();

    @Before
    public void prepare() throws Exception {
        initOptSets(folderList.defaultFID());
        initOptSetsWithErr(folderList.defaultFID());
    }


    /**
     * Установка корректных значений настроек
     *
     * @param fid - фид папки, где будем назначать свойства
     */
    public void initOptSets(String fid) {
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "0", "0"));
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "1", "0"));
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "0", "?"));
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "?", "1"));
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "?", "?"));
        optionSets.add(SettingsFolderSetOptionsObj.setAllOptions(fid, "1", "1"));
    }

    /**
     * Установка некорректных настроек
     *
     * @param fid - фид папки, где будем назначать свойства
     */
    public void initOptSetsWithErr(String fid) {
        optionSetsWithErr.add(SettingsFolderSetOptionsObj.empty().setFid(fid).setNotify("0"));
        optionSetsWithErr.add(SettingsFolderSetOptionsObj.empty().setFid(fid).setNotify("11").setThreaded("0"));
        optionSetsWithErr.add(SettingsFolderSetOptionsObj.empty().setFid(fid).setNotify("0").setThreaded("??"));
        optionSetsWithErr.add(SettingsFolderSetOptionsObj.empty().setNotify("0").setThreaded("??"));
    }

    @Test
    @Issue("MAILPG-120")
    @Title("Изменение настроек folder_options для папок")
    @Description("Установка разных комбинаций настроек последовательно\n" +
            "не делал параметризованным чтобы был\n" +
            "четкий контроль последовательности установка-проверка\n" +
            "- Проверка что в ответе установщика нет ошибки\n" +
            "- Проверка что в итоге установилось то что мы хотели")
    public void testIsSettingsWork() throws Exception {
        logger.warn("Смотрим на изменение настроек folder_options для папок");
        FolderList currFldrList = api(FolderList.class);

        for (SettingsFolderSetOptionsObj obj : optionSets) {
            // Назначение настроек
            jsx(SettingsFolderSetOptions.class).params(obj)
                    .post().via(hc).assertResponse(not(containsString("error")));
            // Просмотр настроек
            JSONObject newSettings = currFldrList.post().via(hc).getFolderOptions(folderList.defaultFID());
            String position = "position";

            assertThat("Настройки <position>, которую пытались установить, не существует: ",
                    newSettings.has(position), is(true));
            assertThat("Настройки <threaded>, которую пытались установить, не существует",
                    newSettings.has("threaded"), is(false));
            assertThat("Настройки <notify>, которую пытались установить, не существует",
                    newSettings.has("notify"), is(false));

            assertThat("Настройка имеет неправильное значение" , newSettings.getString(position), equalTo("0"));
        }

    }

    @Test
    @Issue("MAILPG-120")
    @Title("folder_options не меняются при неправильном изменении")
    @Description("Проверка ошибочных комбинаций\n" +
            "- должен быть error в ответе\n" +
            "- настройки не должны измениться")
    public void testIsErrorCheckWorks() throws Exception {
        logger.warn("Смотрим что folder_options не меняются при неправильном изменении");
        FolderList currFldrList = api(FolderList.class);
        JSONObject before = currFldrList.post().via(hc).getFolderOptions(folderList.defaultFID());

        // Нехорошая логика для теста - цикл
        // Перебор всех установок
        //----------------------------------------------------------------
        for (SettingsFolderSetOptionsObj obj : optionSetsWithErr) {
            // Назначение настроек
            SettingsFolderSetOptions respFromSet = jsx(SettingsFolderSetOptions.class).params(obj)
                    .post().via(hc).shouldBe().shouldBe().updated(is("ok"));
            // Логирование ошибки
            logger.info(Util.getSingleNodeAttibute(respFromSet.toDocument(), "error", "user_message"));

            JSONObject after = currFldrList.post().via(hc).getFolderOptions(folderList.defaultFID());
            assertTrue("Settings was changed!", before.toString().equals(after.toString()));

        }//-------------------------------------------------------------
    }

    @Test
    @Title("Дважды включаем pop3 для папки входящие. Отключение pop3")
    @Issue("MAILPG-100")
    public void pop3OnTwiceTest() {
        settingsFolderUpdate(SettingsFolderUpdateObj.empty().addFid(folderList.defaultFID())).post().via(hc).ok();
        settingsFolderUpdate(SettingsFolderUpdateObj.empty().addFid(folderList.defaultFID())).post().via(hc).ok();
        api(FolderList.class).post().via(hc).pop3Yes(folderList.defaultFID()).pop3No(folderList.deletedFID())
                .pop3No(folderList.sentFID()).pop3No(folderList.draftFID()).pop3No(folderList.spamFID());

        //выключение:
        settingsFolderUpdate(SettingsFolderUpdateObj.empty()).post().via(hc).ok();
        api(FolderList.class).post().via(hc).pop3No(folderList.defaultFID()).pop3No(folderList.deletedFID())
                .pop3No(folderList.sentFID()).pop3No(folderList.draftFID()).pop3No(folderList.spamFID());
    }


    @Test
    @Title("Включаем pop3 для системных папок и для одной пользовательской. Отключение pop3")
    @Issue("MAILPG-100")
    public void pop3OnWithFoldersTest() {
        String fid = newFolder().post().via(hc).updated();
        settingsFolderUpdate(SettingsFolderUpdateObj.empty().addFid(fid).addFid(folderList.spamFID()).addFid(folderList.draftFID())
                .addFid(folderList.defaultFID()).addFid(folderList.sentFID()).addFid(folderList.deletedFID())).post().via(hc).ok();
        api(FolderList.class).post().via(hc).pop3Yes(fid).pop3Yes(folderList.spamFID()).pop3Yes(folderList.draftFID())
                .pop3Yes(folderList.defaultFID()).pop3Yes(folderList.sentFID()).pop3Yes(folderList.deletedFID());
        //выключение:
        settingsFolderUpdate(SettingsFolderUpdateObj.empty()).post().via(hc).ok();
        api(FolderList.class).post().via(hc).pop3No(fid).pop3No(folderList.defaultFID()).pop3No(folderList.deletedFID())
                .pop3No(folderList.sentFID()).pop3No(folderList.draftFID()).pop3No(folderList.spamFID());
    }
}
