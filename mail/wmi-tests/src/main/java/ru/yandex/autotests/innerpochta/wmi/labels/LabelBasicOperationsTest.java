package ru.yandex.autotests.innerpochta.wmi.labels;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabelLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabelName;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsLabelRenameObj.getEmptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Labels.labels;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelDelete.deleteLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelRename.settingsLabelRename;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.*;

@Aqua.Test
@Title("[LABELS] Базовые операции с метками")
@Description("Элементарные операции над метками")
@Features(MyFeatures.WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "LabelBasicOperationsTest")
@RunWith(DataProviderRunner.class)
public class LabelBasicOperationsTest extends BaseTest {


    public static final String DEFAULT_COLOR = "3126463";

    @Rule
    public DeleteLabelsRule labelDel = DeleteLabelsRule.with(authClient);


    @BeforeClass
    public static void init() throws Exception {
        // Предварительная чистка всех меток
        DefaultHttpClient hc4 = authClient.authHC();
        labels().post().via(hc4).getAllLabelIdsExeptSomeSystem()
                .stream().forEach(lid -> deleteLabel(lid).post().via(hc4));
    }


    @Test
    @Title("Простое создание и удаление метки")
    public void createLabel() throws Exception {
        String name = shouldCreateLabel();
        String labelId = labels().post().via(hc).lidByName(name);
        deleteLabel(labelId).post().via(hc);

        assertThat("Метки не должно быть", hc, not(hasLabelLid(labelId)));
    }

    @Test
    @Issue("MAILPG-901")
    @Title("Попытка создать метку с очень длинным именем")
    public void createLabelWithLongName() throws Exception {
        String longName = Util.getLongString();
        newLabel(longName, DEFAULT_COLOR).post().via(hc).errorcode(INVALID_ARGUMENT_5001);
        assertThat("Метки с длинным именем не должно было создаться", hc, not(hasLabelName(longName)));
    }

    @Test
    @Title("Должны создать метку с пустым цветом")
    public void shouldCreateLabelWithEmptyColor() throws Exception {
        String lid = newLabel(Util.getRandomString(), "").post().via(hc).errorcodeShouldBeEmpty().updated();
        assertThat("Должны вернуть lid", lid, notNullValue());
    }

    @Test
    @Title("Создание метки ВАЖНЫЕ")
    public void createLabelWithSystemName() throws Exception {
        newLabel(WmiConsts.LABEL_IMPORTANT, DEFAULT_COLOR).post().via(hc)
                .errorcode(DB_UNKNOWN_ERROR_1000);
    }

    @Test
    @DataProvider({
            "<tag>test<p/></tag>",
            "te&st"
    })
    @Title("Должны создать метку с тегом или спецсимволом")
    public void shouldCreateLabelWithHtmlOrSpecChar(String name) throws Exception {
        String lid = newLabel(name, DEFAULT_COLOR).post().via(hc).updated();
        labelDel.lid(lid);

        shouldSeeLabel(name, DEFAULT_COLOR);
    }

    @Test
    @Title("Создание метки с пустым именем")
    public void createNullLabel() throws Exception {
        newLabel("", DEFAULT_COLOR).post().via(hc).errorcode(INVALID_ARGUMENT_5001);
        assertThat("Ожидалось отсутствие метки с пустым именем", hc, not(hasLabelName("")));
    }

    @Test
    @Title("Создание 2х одинаковых меток")
    public void createTwoLabelsWithOneName() throws Exception {
        String name = Util.getRandomString();
        String lid = newLabel(name, DEFAULT_COLOR).post().via(hc).errorcodeShouldBeEmpty().updated();
        labelDel.lid(lid);

        shouldSeeLabel(name, DEFAULT_COLOR);
        newLabel(name, DEFAULT_COLOR).post().via(hc).withDebugPrint().errorcode(DB_UNKNOWN_ERROR_1000);
    }

    @Test
    @Issue("DARIA-51878")
    @Title("Создание метки с именем 4")
    @Description("Падали с ошибкой в pg, так как имя совпадает с именем системной метки")
    public void createLabelWithName4() throws Exception {
        String name = "4";
        deleteLabel(labels.get().lidByName(name)).post().via(hc);
        String lid = newLabel(name, DEFAULT_COLOR).post().via(hc).errorcodeShouldBeEmpty().updated();
        shouldSeeLabel(name, DEFAULT_COLOR);
        labelDel.lid(lid);
    }

    @Test
    @Title("Переименование метки")
    public void renameLabel() throws Exception {
        String name = shouldCreateLabel();
        String newName = Util.getRandomString();
        String lid = labels().post().via(hc).lidByName(name);
        labelDel.lid(lid);

        settingsLabelRename(getEmptyObj().setLid(lid).setLabelName(newName)).post().via(hc);
        shouldSeeLabel(newName, DEFAULT_COLOR);
    }

    @Test
    @Title("Переименование метки в имя существующей метки")
    public void renameLabelToExistingName() throws Exception {
        String name = shouldCreateLabel();
        String name2 = shouldCreateLabel();
        String lid = jsx(Labels.class).post().via(hc).lidByName(name);
        String lid2 = labels().post().via(hc).lidByName(name2);
        labelDel.lid(lid, lid2);

        String resp = settingsLabelRename(getEmptyObj().setLid(lid2).setLabelName(name))
                .post().via(hc).toString();
        assertThat("Ответ должен содержать ошибку", resp, containsString(WmiConsts.ERROR));
    }

    @Test
    @Title("Передача невалидного цвета при изменении без имени метки. Ожидаемый результат: ошибка 5001")
    @DataProvider({
            "Zalphabetic",
            "1111111111111111111111111111",
            "FFFFFF",
            ""
    })
    public void changeColorToInvalidValue(String arg) throws Exception {
        String name = shouldCreateLabel();
        String lid = labels().post().via(hc).lidByName(name);
        labelDel.lid(lid);

        settingsLabelRename(getEmptyObj().setLid(lid).setLabelColor(arg)).post().via(hc)
               .errorcode(INVALID_ARGUMENT_5001);
    }

    @Test
    @Title("Проверка изменения цвета")
    public void changeColor() throws Exception {
        String name = shouldCreateLabel();
        String lid = labels().post().via(hc).lidByName(name);
        labelDel.lid(lid);

        String newColor = "13966848";

        settingsLabelRename(getEmptyObj().setLid(lid)
                .setLabelName(name)
                .setLabelColor(newColor))
                .post().via(hc);

        shouldSeeLabel(name, newColor);
    }

    @Step("Должны успешно создать метку")
    private String shouldCreateLabel() throws Exception {
        String name = Util.getRandomString();
        newLabel(name, DEFAULT_COLOR).post().via(hc);
        shouldSeeLabel(name, DEFAULT_COLOR);
        return name;
    }

    @Step("Должны увидеть метку {0} с цветом {1}")
    private void shouldSeeLabel(String name, String color) {
        assertThat(hc, hasLabel(name, color));
    }

}
