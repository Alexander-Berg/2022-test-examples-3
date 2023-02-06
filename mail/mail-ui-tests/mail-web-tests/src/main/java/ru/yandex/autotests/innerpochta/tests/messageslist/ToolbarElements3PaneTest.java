package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Тулбар в просмотре писем в 3Pane")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarElements3PaneTest extends BaseTest {

    private static final String[] TOOLBAR_ITEMS = {
        "Ответить",
        "Переслать",
        "Удалить",
        "Ещё"
    };

    private static final String[] MORE_DROPDOWN_ITEMS = {
        "Это спам!",
        "Напомнить позже",
        "Архивировать",
        "Метка",
        "В папку",
        "Закрепить",
        "Распечатать",
        "Создать правило",
        "Свойства письма"
    };

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private Message msg;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomString());
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем 3Pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем элементы тулбара в просмотре письма")
    @TestCaseId("357")
    public void shouldSeeToolbarItemsInMessageView() {
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().shouldSeeAllElementsInList(onMessageView().contentToolbarBlock().allItems(), TOOLBAR_ITEMS);
    }

    @Test
    @Title("Проверяем элементы выпадушки «Ещё» в тулбаре в просмотре письма")
    @TestCaseId("357")
    public void shouldSeeToolbarMoreDropdownItemsInMessageView() {
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSeeAllElementsInList(onMessageView().miscField().allItems(), MORE_DROPDOWN_ITEMS);
    }
}
