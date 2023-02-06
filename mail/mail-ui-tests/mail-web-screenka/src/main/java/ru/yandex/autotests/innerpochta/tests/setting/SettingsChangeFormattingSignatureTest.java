package ru.yandex.autotests.innerpochta.tests.setting;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SENDER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_AREAS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 **/
@Aqua.Test
@Title("Настройки - Добавление подписи со стилями")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SettingsChangeFormattingSignatureTest {

    private static final String IMG_LINK = "https://i.redd.it/abc1gkfjxb331.jpg";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".mail-LoadingBar-Container")
    );
    private String text = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREAS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @TestCaseId("3582")
    @Title("Добавлям подпись с картинкой")
    public void shouldSeeSignatureWithImage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().addImage())
                .inputsTextInElement(
                    st.pages().mail().settingsCommon().addImagePopup().inputForImage(),
                    IMG_LINK
                )
                .clicksOn(st.pages().mail().settingsCommon().addImagePopup().addImageButton())
                .shouldNotSee(st.pages().mail().settingsCommon().addImagePopup());
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем различные списки и выравнивания")
    @TestCaseId("2489")
    public void shouldSeeDifferentListsAndAligns() {
        Consumer<InitStepsRule> actions = st -> {
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().listSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextFormatItems(),
                0
            );
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().listSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextFormatItems(),
                1
            );
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().alignmentSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextFormatItems(),
                2
            );
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().alignmentSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextFormatItems(),
                3
            );
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().alignmentSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextFormatItems(),
                4
            );
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой цвет текста")
    @TestCaseId("2489")
    public void shouldSeeDifferentTextColor() {
        int textColor = Utils.getRandomNumber(39, 0);
        Consumer<InitStepsRule> actions = st -> {
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().colorSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextColorItems(),
                textColor
            );
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой цвет подложки текста")
    @TestCaseId("2489")
    public void shouldSeeDifferentBgColor() {
        int bgColor = Utils.getRandomNumber(39, 0);
        Consumer<InitStepsRule> actions = st -> {
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().bgColorSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureTextColorItems(),
                bgColor
            );
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой шрифт")
    @TestCaseId("2489")
    public void shouldSeeDifferentFont() {
        int font = Utils.getRandomNumber(3, 0);
        Consumer<InitStepsRule> actions = st -> {
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().fontTextSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureFontItems(),
                font
            );
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой размер шрифта")
    @TestCaseId("2489")
    public void shouldSeeDifferentFontSize() {
        int fontSize = Utils.getRandomNumber(3, 0);
        Consumer<InitStepsRule> actions = st -> {
            inputStyledTextWithParams(
                st,
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().fontSizeSelect(),
                st.user().pages().SenderInfoSettingsPage().signatureFontSizeItems(),
                fontSize
            );
            addAndCheckSign(st);
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Step("Применяем стили с параметрами и пишем строку в тело подписи")
    private void inputStyledTextWithParams(
        InitStepsRule st, WebElement button, ElementsCollection<MailElement> selector, int newValue
    ) {
        st.user().defaultSteps().clicksOn(button)
            .shouldSee(selector.get(newValue))
            .clicksOn(selector.get(newValue))
            .appendTextInElement(
                st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().input().get(0),
                text
            );
        st.user().hotkeySteps().pressHotKeys(
            st.user().pages().SenderInfoSettingsPage().blockSetupSender().signatures().input().get(0),
            ENTER
        );
    }

    @Step("Добаляем подпись и проверяем, что она появилась")
    private void addAndCheckSign(InitStepsRule st) {
        st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().addBtn())
            .shouldSee(st.pages().mail().settingsSender().blockSetupSender().signatures().signaturesList()
                .waitUntil(not(empty())).get(0));
    }
}