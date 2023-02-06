package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Действия с ябблами в композе")
@Features({FeaturesConst.COMPOSE})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
@RunWith(DataProviderRunner.class)
@Description("В аккаунте заготовлены контакты с различными тестовыми данными. " +
    "Удалять нельзя, они нужны для полоски популярных контактов")
public class ComposeYabbleActionsTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    private static final String CONTACT = "robbiter-5550005552@yandex.ru";
    private static final String CONTACT_WITH_TWO_EMAILS = "Два Адреса";
    private static final String GROUP_TITLE = "yabblegroup";

    private String notAnEmail = Utils.getRandomString();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Проверяем выбор адреса в дропдауне ябла для контакта с несколькими емейлами")
    @TestCaseId("2812")
    public void shouldChooseEmailFromYabbleDropdownMenu() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT_WITH_TWO_EMAILS);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0)
            )
                .clicksOn(st.pages().mail().composePopup().suggestList().get(0))
                .clicksOn(st.pages().mail().composePopup().yabbleToList().get(0))
                .clicksOn(st.pages().mail().composePopup().yabbleDropdown().changeEmail().get(1));
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем написание одному адресату")
    @TestCaseId("2815")
    public void shouldLeaveSingleContact() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT)
                .addAnotherRecipient(DEV_NULL_EMAIL);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().defaultSteps().clicksOn(
                st.pages().mail().composePopup().yabbleToList().get(0),
                st.pages().mail().composePopup().yabbleDropdown().singleTarget()
            );
            st.user().composeSteps().shouldNotSeeSendToAreaHas(DEV_NULL_EMAIL);
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Удалить адресата из получателей")
    @TestCaseId("2012")
    public void shouldRemoveContact() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT)
                .addAnotherRecipient(DEV_NULL_EMAIL);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .clicksOn(st.pages().mail().composePopup().yabbleToList().get(0).yabbleDeleteBtn());
            st.user().composeSteps().shouldNotSeeSendToAreaHas(CONTACT);
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Редактирование получателя")
    @TestCaseId("2818")
    public void shouldChangeRecipientInYabble() {
        Consumer<InitStepsRule> actions = st -> {
            String changedContactName = "P";
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .clicksOn(
                    st.pages().mail().composePopup().yabbleToList().get(0),
                    st.pages().mail().composePopup().yabbleDropdown().editYabble()
                );
            st.user().hotkeySteps()
                .pressHotKeys(st.pages().mail().composePopup().expandedPopup().popupTo(), changedContactName)
                .pressSimpleHotKey(key(Keys.ENTER));
            st.user().defaultSteps().shouldSeeThatElementHasText(
                st.pages().mail().composePopup().yabbleToList().get(0).yabbleText(),
                changedContactName
            );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем цвет ябла")
    @TestCaseId("2757")
    public void shouldSeeYabbleColor() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps()
                .inputsAddressInFieldTo(CONTACT)
                .addAnotherRecipient(DEV_NULL_EMAIL)
                .addAnotherRecipient(notAnEmail);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем наличие дропдауна для разных яблов")
    @TestCaseId("2811")
    @DataProvider({CONTACT, DEV_NULL_EMAIL, GROUP_TITLE})
    public void shouldSeeYabbleDropdownMenu(String contact) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(contact);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().yabbleToList().get(0));
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Переходим в режим редактирования ябла через дропдаун меню")
    @TestCaseId("2814")
    public void shouldTurnOnEditMode() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().defaultSteps().clicksOn(
                st.pages().mail().composePopup().yabbleToList().get(0),
                st.pages().mail().composePopup().yabbleDropdown().editYabble()
            );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }
}
