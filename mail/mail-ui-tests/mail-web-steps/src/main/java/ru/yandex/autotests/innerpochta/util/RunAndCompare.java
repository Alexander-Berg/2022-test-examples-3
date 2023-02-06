package ru.yandex.autotests.innerpochta.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.steps.SaveScreenSteps;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.CORP_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.PRINT_URL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.GET_DEVICE_PIXEL_RATIO;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_REMOVE_CARET;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_SCROLL_CAL_TO_10AM;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SET_HEADER_CONTACTS_PADDING;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * @author cosmopanda
 */
public class RunAndCompare {

    private static final Logger LOGGER = Logger.getLogger(RunAndCompare.class);
    private static String PROD_URL = urlProps().getProdUri();
    private static String TEST_URL = urlProps().getBaseUri();
    private Consumer<InitStepsRule> actions;
    private InitStepsRule prodSteps;
    private InitStepsRule testSteps;
    private String urlPath = "";
    private Set<By> ignoredElements = new HashSet<>();
    private Boolean isDefaultIgnoredElementsRewrite = false;
    private Set<Coords> ignoredAreas = new HashSet<>();
    private Consumer<InitStepsRule> login = (st) -> {
    };
    private Consumer<InitStepsRule> closePromo = (st) -> {
    };
    private Consumer<InitStepsRule> scrollGrid = (st) -> {
    };
    private Consumer<InitStepsRule> prepareScreenActions = (st) -> {
    };
    private Consumer<InitStepsRule> yexp = (st) -> {
    };
    private SaveScreenSteps screenSteps = new SaveScreenSteps();
    private BiConsumer<Screenshot, Screenshot> saveDiff =
            (prodPage, testPage) -> {
                try {
                    JavascriptExecutor jsExecutor = (JavascriptExecutor) prodSteps.getDriver();
                    Integer pixelRatio =
                            Math.toIntExact(Math.round(
                                    Double.parseDouble(String.valueOf(jsExecutor.executeScript(GET_DEVICE_PIXEL_RATIO)))));
                    screenSteps.saveDiffScreenshot(prodPage, testPage, pixelRatio, PROD_URL, TEST_URL);
                } catch (IOException e) {
                    LOGGER.info(e.getMessage(), e);
                }
            };

    private RunAndCompare() {
    }

    public static RunAndCompare runAndCompare() {
        return new RunAndCompare();
    }

    public RunAndCompare withUrlPath(QuickFragments urlFragment) {
        this.urlPath = urlFragment.makeUrlPart();
        return this;
    }

    public RunAndCompare withUrlPath(String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public RunAndCompare withDomain(String domain) {
        PROD_URL = StringUtils.substringBeforeLast(PROD_URL, ".") + "." + domain;
        TEST_URL = StringUtils.substringBeforeLast(TEST_URL, ".") + "." + domain;
        return this;
    }

    public RunAndCompare withActions(Consumer<InitStepsRule> actions) {
        this.actions = actions;
        return this;
    }

    public RunAndCompare withIgnoredElements(Set<By> ignoredElements) {
        this.ignoredElements = ignoredElements;
        this.isDefaultIgnoredElementsRewrite = true;
        return this;
    }

    public RunAndCompare withAdditionalIgnoredElements(Set<By> ignoredElements) {
        this.ignoredElements.addAll(ignoredElements);
        this.isDefaultIgnoredElementsRewrite = false;
        return this;
    }

    public RunAndCompare withIgnoredAreas(Set<Coords> ignoredAreas) {
        this.ignoredAreas = ignoredAreas;
        return this;
    }

    public RunAndCompare withProdSteps(InitStepsRule steps) {
        this.prodSteps = steps;
        return this;
    }

    public RunAndCompare withTestSteps(InitStepsRule steps) {
        this.testSteps = steps;
        return this;
    }

    public RunAndCompare withAcc(Account acc) {
        this.login = st -> st.user().loginSteps().forAcc(acc).logins();
        return this;
    }

    public RunAndCompare withCorpAcc(Account acc) {
        this.login = st -> st.user().loginSteps().forAcc(acc).loginsToCorp();
        return this;
    }

    public RunAndCompare withClosePromo() {
        this.closePromo = st -> {
            if (PROD_URL.contains(CORP_BASE_URL))
                st.user().defaultSteps().shouldSee(st.pages().cal().home().oldCalHeaderBlock().oldCalLink());
            else st.user().defaultSteps().shouldSee(st.pages().cal().home().calHeaderBlock().calLink());

            st.user().defaultSteps().executesJavaScript( // скрываем полоску и прыщ для текущего времени
                    "if(document.querySelector('.qa-WeekGridCurrentTime-Time') != null) { " +
                        "document.querySelector('.qa-WeekGridCurrentTime-Time').parentNode.remove()" +
                        "}"
                );
        };
        return this;
    }

    public RunAndCompare withYexp(String... exp) {
        this.yexp = st -> st.user().defaultSteps().addExperimentsWithYexp(exp);
        return this;
    }

    public RunAndCompare withScrollGrid() {
        this.scrollGrid = st -> {
            if (PROD_URL.contains(CORP_BASE_URL))
                st.user().defaultSteps().shouldSee(st.pages().cal().home().oldCalHeaderBlock().oldCalLink());
            else st.user().defaultSteps().shouldSee(st.pages().cal().home().calHeaderBlock().calLink());

            st.user().defaultSteps().executesJavaScript(SCRIPT_SCROLL_CAL_TO_10AM);
        };
        return this;
    }

    public RunAndCompare withPrepareScreenAction(Consumer<InitStepsRule> prepActions) {
        this.prepareScreenActions = this.prepareScreenActions.andThen(prepActions);
        return this;
    }

    private Supplier<Screenshot> doAndScreen(InitStepsRule steps, String url) {
        return () -> ScreenActions.withScreenshot(actions.andThen(prepareScreenActions))
            .withIgnoredElements(ignoredElements)
            .withIgnoredAreas(ignoredAreas)
            .on(
                steps,
                login.andThen(st -> st.user().defaultSteps().opensUrl(url + urlPath))
                    .andThen(yexp)
                    .andThen(closePromo)
                    .andThen(scrollGrid)
            );

    }

    public RunAndCompare run() {
        setIgnoredElements();
        setIgnoredAreas();
        switchToDefaultContentBeforeScreen();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Screenshot> prodScreen = executorService.submit(() -> doAndScreen(prodSteps, PROD_URL).get());
        Future<Screenshot> testScreen = executorService.submit(() -> doAndScreen(testSteps, TEST_URL).get());

        try {
            saveDiff.accept(prodScreen.get(), testScreen.get());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {
        }

        return this;
    }

    public RunAndCompare runSequentially() {
        setIgnoredElements();
        setIgnoredAreas();
        switchToDefaultContentBeforeScreen();
        Screenshot prodScreen = doAndScreen(prodSteps, PROD_URL).get();
        Screenshot testScreen = doAndScreen(testSteps, TEST_URL).get();
        saveDiff.accept(prodScreen, testScreen);

        return this;
    }

    private void setIgnoredElements() {
        String project = urlProps().getProject();
        if (!isDefaultIgnoredElementsRewrite) {
            if (project.equals("touch")) {
                ignoredElements.addAll(TouchTestsIgnoredElements.IGNORED_ELEMENTS);
            } else if (project.equals("cal")) {
                ignoredElements.addAll(CalTestsIgnoredElements.IGNORED_ELEMENTS);
            } else {
                ignoredElements.addAll(TestConsts.IGNORED_ELEMENTS);
            }
        }
    }

    private void setIgnoredAreas() {
        String project = urlProps().getProject();
        if (project.equals("liza")) {
            ignoredAreas.addAll(TestConsts.REFRESH_BUTTON_PIXELS);
        }
    }

    private void addFixHeaderActionBeforeScreen() {
        if (urlProps().getProject().equals("liza")) {
            this.withPrepareScreenAction(
                (st) -> st.user().defaultSteps().executesJavaScript(SET_HEADER_CONTACTS_PADDING)
            );
        }
    }

    private void switchToDefaultContentBeforeScreen() {
        if (urlProps().getProject().equals("touch")) {
            this.withPrepareScreenAction((st) -> {
                    st.user().defaultSteps().executesJavaScript(SCRIPT_REMOVE_CARET);
                    st.getDriver().switchTo().defaultContent();
                }
            );
        }
    }
}
