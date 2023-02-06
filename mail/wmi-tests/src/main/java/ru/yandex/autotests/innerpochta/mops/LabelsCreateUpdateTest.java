package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.labels.create.ApiLabelsCreate;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.IsThereLabel.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.internalError;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.labels.LabelBasicOperationsTest.DEFAULT_COLOR;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 13.11.15
 * Time: 18:24
 * <p>
 * https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface/#metodyredaktirovaniemetok
 */
@Aqua.Test
@Title("[MOPS] Создание/редактирование меток")
@Description("Создание/редактирование меток, различные тесты")
@Features(MyFeatures.MOPS)
@Stories(MyStories.LABELS)
@Issues({@Issue("DARIA-49310"), @Issue("MAILPG-795")})
@Credentials(loginGroup = "MopsLabelsCreateTest")
@RunWith(DataProviderRunner.class)
public class LabelsCreateUpdateTest extends MopsBaseTest {
    @Rule
    public DeleteLabelsMopsRule labelDel = new DeleteLabelsMopsRule(authClient).before(true).after(false);

    @Test
    @Title("Простое создание и удаление метки")
    public void createLabelShouldBeOk() throws Exception {
        String name = shouldCreateLabel();
        String lid = updatedLabels().lidByName(name);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Попытка создать метку с очень длинным именем")
    public void createLabelWithLongNameShouldSeeInvalidRequest() throws Exception {
        val longName = Util.getLongString();
        val expectedError = "Label name too long";
        createLabel().withName(longName).withColor(DEFAULT_COLOR)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat("Метки с длинным именем не должно было создаться", authClient, not(hasLabelName(longName)));
    }

    @Test
    @Title("Должны создать метку с пустым цветом")
    public void shouldCreateLabelWithEmptyColor() throws Exception {
        val lid = newLabelByName(getRandomString(), "");
        assertThat("Должны вернуть lid", lid, notNullValue());
    }

    @Test
    @Issue("DARIA-53513")
    @Title("Создание метки ВАЖНЫЕ")
    public void shouldCreateLabelWithSystemName() throws Exception {
        val lid = newLabelByName(WmiConsts.LABEL_IMPORTANT, DEFAULT_COLOR);
        shouldSeeLabel(WmiConsts.LABEL_IMPORTANT, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @DataProvider({
            "<tag>test<p/></tag>",
            "te&st",
            "4"
    })
    @Issue("DARIA-51878")
    @Title("Должны создать метку с системным именем, тегом или спецсимволом")
    public void shouldCreateLabelWithHtmlOrSpecChar(String name) throws Exception {
        val lid = newLabelByName(name, DEFAULT_COLOR);
        shouldSeeLabel(name, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Создание метки с пустым именем")
    public void createNullLabelShouldBeInvalidRequest() throws Exception {
        val expectedError = "can't create label with no name";
        createLabel().withName("").withColor(DEFAULT_COLOR)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
        assertThat("Ожидалось отсутствие метки с пустым именем", authClient, not(hasLabelName("")));
    }

    @Test
    @Title("Создание 2х одинаковых меток")
    public void shouldNotCreateTwoLabelsWithOneName() throws Exception {
        val name = getRandomString();
        val lid = newLabelByName(name, DEFAULT_COLOR);
        shouldSeeLabel(name, DEFAULT_COLOR);

        val expectedError = "already exist label";
        createLabel().withName(name).withColor(DEFAULT_COLOR)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Переименование метки")
    public void renameLabelShouldBeOk() throws Exception {
        val name = shouldCreateLabel();
        val newName = getRandomString();
        val lid = receiveLid(name);

        updateLabel(lid).withName(newName).withColor(DEFAULT_COLOR).post(shouldBe(okEmptyJson()));
        shouldSeeLabel(newName, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Переименование метки и изменение цвета")
    public void renameAndChangeColorLabelShouldBeOk() throws Exception {
        val name = shouldCreateLabel();
        val newName = getRandomString();
        val newColor = String.valueOf(Util.getRandomShortInt());
        val lid = receiveLid(name);

        updateLabel(lid).withName(newName).withColor(newColor).post(shouldBe(okEmptyJson()));
        shouldSeeLabel(newName, newColor);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Переименование метки в имя существующей метки")
    public void shouldNotRenameLabelToExistingName() throws Exception {
        val name = shouldCreateLabel();
        val name2 = shouldCreateLabel();
        val lid = receiveLid(name);
        val lid2 = receiveLid(name2);

        val expectedError = "ERROR:  duplicate key value violates unique constraint";
        updateLabel(lid2).withName(name).withColor(DEFAULT_COLOR)
                .post(shouldBe(internalError(containsString(expectedError))));
        shouldDeleteLabel(lid);
    }

    @Test
    @Issue("DARIA-53532")
    @Title("Передача пустого цвета при изменении")
    public void shouldNotChangeColorToEmptyString() throws Exception {
        val name = shouldCreateLabel();
        val lid = receiveLid(name);
        changeLabelColor(lid, "").post(shouldBe(invalidRequest()));
        shouldSeeLabel(name, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Проверка изменения цвета")
    public void changeColorShouldBeOk() throws Exception {
        val name = shouldCreateLabel();
        val lid = receiveLid(name);

        val newColor = "13966848";
        updateLabel(lid).withName(name).withColor(newColor).post(identity());

        shouldSeeLabel(name, newColor);
        shouldDeleteLabel(lid);
    }

    @Test
    @Title("Проверка изменения имени")
    @DataProvider({
            "<tag>test<p/></tag>",
            "te&st",
            "Важные",
            "4"
    })
    public void changeNameToHtmlOrSpecShouldBeOk(String newName) throws Exception {
        val name = shouldCreateLabel();
        val lid = receiveLid(name);

        updateLabel(lid).withName(newName).withColor(DEFAULT_COLOR).post(identity());

        shouldSeeLabel(newName, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @Issue("DARIA-53513")
    @Title("Переименование пользовательской метки в ВАЖНЫЕ")
    public void renameLabelToSystemName() throws Exception {
        val name = shouldCreateLabel();
        val lid = receiveLid(name);
        updateLabel(lid).withName(WmiConsts.LABEL_IMPORTANT).withColor(DEFAULT_COLOR).post(identity());
        shouldSeeLabel(WmiConsts.LABEL_IMPORTANT, DEFAULT_COLOR);
        shouldDeleteLabel(lid);
    }

    @Test
    @DataProvider({
            "mute_label",
            "attached_label",
            "spam_label",
            "answered_label",
            "recent_label",
            "draft_label",
            "deleted_label",
            "forwarded_label",
            "important_label",
            "seen_label",
            "remindMessage_label",
            "notifyMessage_label"
    })
    @Issue("DARIA-53513")
    @Title("Создаем системные метки")
    @Description("https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface/#post/labels/create")
    public void createLabelSymbolParamTest(String symbol) throws Exception {
        Mops.debugPrint(createLabel().withSymbol(symbol).post(shouldBe(invalidRequest())));
    }

    @Test
    @Issue("DARIA-53513")
    @Title("Создаем системные метки c пустым символом")
    public void createEmptySymbolLabelTest() {
        val expectedError = "invalid arguments: unknown symbol: ";
        createLabel().withSymbol("").post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Issue("DARIA-53513")
    @Title("Создаем системные метки c рандомным символом")
    public void createRandomSymbolLabelTest() {
        val randomSymbol = getRandomString();
        val expectedError = "invalid arguments: unknown symbol: " + randomSymbol;
        createLabel().withSymbol(randomSymbol).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Issue("MAILPG-3560")
    @Title("Создаем системную метку c пустым символом")
    public void createSystemLabelWithoutSymbol() {
        val expectedError = "invalid arguments: wrong combination of params: you must specify symbol on creating system label";

        createLabel()
                .withName(getRandomString())
                .withType("system")
                .post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Test
    @DataProvider({
            "mute_label"
    })
    @Issue("DARIA-53513")
    @Title("Создаем системные метки с именем. Должны увидеть ошибку")
    @Description("https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface/#post/labels/create")
    public void createLabelSymbolWithNameParamTest(String symbol) throws Exception {
        val expectedError = "invalid arguments: wrong combination of params: you can create label either " +
                "by symbol or by name + color + type";
        createLabel().withSymbol(symbol).withName(getRandomString()).
                post(shouldBe(invalidRequest(equalTo(expectedError))));
    }


    @Test
    @Issue("MAILDEV-789")
    @Title("Создание метки с параметром strict=0")
    public void testNonStrictCreateLabel() {
        val name = getRandomString();
        createLabel().withName(name).withColor("green").withType("user")
                    .withStrict(ApiLabelsCreate.StrictParam._0).post(shouldBe(okLid()));
        assertThat("Ожидалось, что метка будет создана", authClient, hasLabelName(name));
    }

    @Test
    @Issue("MAILDEV-789")
    @Title("Создание метки с параметром strict=1")
    public void testStrictCreateLabel() {
        val name = getRandomString();
        createLabel().withName(name).withColor("green").withType("user")
                .withStrict(ApiLabelsCreate.StrictParam._1).post(shouldBe(okLid()));
        assertThat("Ожидалось, что метка будет создана", authClient, hasLabelName(name));
    }

    @Test
    @Issue("MAILDEV-789")
    @Title("Дублирование метки с параметром strict=0")
    public void testNonStrictDuplicateLabel() {
        val name = getRandomString();
        createLabel().withName(name).withColor("green").withType("user")
                .post(shouldBe(okLid()));
        createLabel().withName(name).withColor("green").withType("user")
                .withStrict(ApiLabelsCreate.StrictParam._0).post(shouldBe(okLid()));
    }

    @Test
    @Issue("MAILDEV-789")
    @Title("Дублирование метки с параметром strict=1")
    public void testStrictDuplicateLabel() {
        val name = getRandomString();
        createLabel().withName(name).withColor("green").withType("user")
                .withStrict(ApiLabelsCreate.StrictParam._0).post(shouldBe(okLid()));
        createLabel().withName(name).withColor("green").withType("user")
                .withStrict(ApiLabelsCreate.StrictParam._1).post(shouldBe(invalidRequest()));
    }

    @Step("Должны успешно удалить метку")
    private void shouldDeleteLabel(String lid) throws Exception {
        deleteLabel(lid).post(shouldBe(okSync()));
        assertThat("Должны успешно удалить метку", authClient, not(hasLabel(lid)));
    }

    @Step("Должны успешно создать метку")
    private String shouldCreateLabel() throws Exception {
        val name = getRandomString();
        createLabel().withName(name).withColor(DEFAULT_COLOR).post(shouldBe(okLid()));
        shouldSeeLabel(name, DEFAULT_COLOR);
        return name;
    }

    @Step("Должны увидеть метку {0} с цветом {1}")
    private void shouldSeeLabel(String name, String color) {
        assertThat(authClient, hasLabelByNameAndColor(name, color));
    }

}
