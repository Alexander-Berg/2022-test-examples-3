package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Оформление текста писем")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ComposeCkeditorFormatTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".composeHeader-Text"),
        cssSelector(".ComposeStack"),
        cssSelector(".ns-view-right-box"),
        cssSelector(".mail-NestedList-Item-Info"),
        cssSelector(".js-messages-pager-scroll")
    );
    private static final String WRAP_TEXT = "По дорогам страны ездит несколько миллионов автомобилей, которые могут " +
        "быть подключены к интернету с помощью мультимедийных систем. Мы даём их владельцам возможность использовать" +
        " в пути современные онлайн-сервисы, например строить маршруты, искать парковки и слушать любимую музыку, —" +
        " говорит Андрей Василевский, директор по развитию бизнеса Яндекс.Авто. — Автомобилисты получат не только " +
        "готовое к использованию устройство, но и техническую поддержку: бортовой компьютер бесплатно установят и " +
        "будут обслуживать по гарантии";
    private String text = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Переносим по словам в визивиге")
    @TestCaseId("2011")
    public void shouldSeeCkeditorWordsWrap() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), WRAP_TEXT);
            st.user().composeSteps().shouldSeeFormattedTextAreaContains(WRAP_TEXT);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем различные стили к тексту")
    @TestCaseId("923")
    public void shouldSeeDifferentStylesForText() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledTextWithHover(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().bold(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().italic(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().underline(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().strike()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем различные списки и выравнивания")
    @TestCaseId("923")
    public void shouldSeeDifferentListsAndAligns() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledText(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().numberedlist(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().bulletedlist()
            );
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().alignment(),
                st.pages().mail().composePopup().expandedPopup().mailTextAlignment(),
                0
            );
            hotkeyEnterInMailBody(st);
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().alignment(),
                st.pages().mail().composePopup().expandedPopup().mailTextAlignment(),
                1
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой цвет текста")
    @TestCaseId("923")
    public void shouldSeeDifferentTextColor() {
        int textColor = Utils.getRandomNumber(39, 0);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailtextcolor(),
                st.pages().mail().composePopup().expandedPopup().mailTextColorArray(),
                textColor
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой цвет подложки текста")
    @TestCaseId("923")
    public void shouldSeeDifferentBgColor() {
        int bgColor = Utils.getRandomNumber(39, 0);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailbgcolor(),
                st.pages().mail().composePopup().expandedPopup().mailTextColorArray(),
                bgColor
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой шрифт")
    @TestCaseId("923")
    public void shouldSeeDifferentFont() {
        int font = Utils.getRandomNumber(3, 0);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailfont(),
                st.pages().mail().composePopup().expandedPopup().mailFontArray(),
                font
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем другой размер шрифта")
    @TestCaseId("923")
    public void shouldSeeDifferentFontSize() {
        int fontSize = Utils.getRandomNumber(3, 0);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailfontsize(),
                st.pages().mail().composePopup().expandedPopup().mailFontSizeArray(),
                fontSize
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вставляем цитату")
    @TestCaseId("3356")
    public void shouldSeeQuote() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            inputStyledText(st, st.pages().mail().composePopup().expandedPopup().toolbarBlock().blockquote());
            st.user().defaultSteps()
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), WRAP_TEXT);
            hotkeyEnterInMailBody(st);
            st.user().defaultSteps()
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
            hotkeyEnterInMailBody(st);
            inputStyledText(st, st.pages().mail().composePopup().expandedPopup().toolbarBlock().blockquote());
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().toolbarBlock().bold())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
            hotkeyEnterInMailBody(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вставляем скопированный текст в цитату")
    @TestCaseId("3356")
    public void shouldSeeQuoteForCopiedText() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
            st.user().composeSteps().shouldSeeFormattedTextAreaContains(text);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().hotkeySteps().pressHotKeysWithDestination(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                Keys.chord(Keys.CONTROL, "a")
            );
            st.user().hotkeySteps().pressHotKeys(Keys.chord(Keys.CONTROL, "c"));
            hotkeyEnterInMailBody(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().composePopup().expandedPopup().toolbarBlock().blockquote());
            st.user().hotkeySteps().pressHotKeysWithDestination(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                Keys.chord(Keys.CONTROL, "v")
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Применяем один шрифт к разным отрывкам текста")
    @TestCaseId("5155")
    public void shouldChangeFontInDifferentLines() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
            inputStyledTextWithParams(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailfont(),
                st.pages().mail().composePopup().expandedPopup().mailFontArray(),
                1
            );
            st.user().hotkeySteps().pressCombinationOfHotKeys(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                key(Keys.CONTROL),
                "a"
            );
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().composePopup().expandedPopup().toolbarBlock().mailfont())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().mailFontArray().get(2))
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Применяем стиль и пишем строку в тело письма")
    private void inputStyledTextWithHover(InitStepsRule st, WebElement... buttons) {
        for (WebElement button : buttons) {
            st.user().defaultSteps().onMouseHoverAndClick(button)
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text)
                .onMouseHoverAndClick(button);
            hotkeyEnterInMailBody(st);
        }
    }

    @Step("Применяем стиль и пишем строку в тело письма")
    private void inputStyledText(InitStepsRule st, WebElement... buttons) {
        for (WebElement button : buttons) {
            st.user().defaultSteps().clicksOn(button)
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
            hotkeyEnterInMailBody(st);
            st.user().defaultSteps().clicksOn(button);
        }
    }

    @Step("Применяем стили с параметрами и пишем строку в тело письма")
    private void inputStyledTextWithParams(
        InitStepsRule st, WebElement button, ElementsCollection<MailElement> selector, int newValue
    ) {
        st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
            .clicksOn(button)
            .shouldSee(selector.get(newValue))
            .clicksOn(selector.get(newValue));
        st.user().defaultSteps()
            .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text);
    }

    @Step("Нажимаем комбинацию клавиш CTRL + Enter в тело письма")
    private void hotkeyEnterInMailBody(InitStepsRule st) {
        st.user().hotkeySteps().pressHotKeys(st.pages().mail().composePopup().expandedPopup().bodyInput(), ENTER);
    }
}
