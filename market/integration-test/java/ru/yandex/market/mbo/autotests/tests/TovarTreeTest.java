package ru.yandex.market.mbo.autotests.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.autotests.requests.UrlProvider;
import ru.yandex.market.mbo.autotests.rules.UiRule;
import ru.yandex.market.mbo.autotests.rules.WebDriverWrapper;
import ru.yandex.market.mbo.autotests.steps.ParametersPageSteps;
import ru.yandex.market.mbo.autotests.steps.TovarTreeBlockingChangesSteps;
import ru.yandex.market.mbo.autotests.steps.TovarTreeKnowledgesPageSteps;
import ru.yandex.market.mbo.autotests.steps.TovarTreeTasksPageSteps;
import ru.yandex.market.mbo.autotests.user.UserProvider;
import ru.yandex.market.mbo.autotests.webdriver.WebDriverFactory;
import ru.yandex.qatools.htmlelements.element.TextInput;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

import static ru.yandex.market.mbo.autotests.matchers.WaiterMatcherFactory.TIMEOUT_AQUA;

@Epic("guru-пайплайн")
@Feature("Товарное дерево")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:property-configurer.xml"})
public class TovarTreeTest {
    @Resource(name = "urlProvider")
    private UrlProvider urlProvider;

    @Resource(name = "webDriverFactory")
    private WebDriverFactory webDriverFactory;

    @Rule
    public UiRule adminRuleChain;

    private WebDriverWrapper webDriverWrapper;

    public TovarTreeTest() {
    }

    @PostConstruct
    public void init() {
        webDriverWrapper = new WebDriverWrapper(webDriverFactory);

        adminRuleChain = new UiRule(webDriverWrapper, UserProvider.ADMIN);
    }

    @Test
    @Story("Изменение категории")
    @DisplayName("Редактирование настроек задач по логам для категории")
    public void testEdit() {
        TovarTreeTasksPageSteps tovarTreeTasksPageSteps = new TovarTreeTasksPageSteps(
            webDriverWrapper.getWebDriver()
        );
        tovarTreeTasksPageSteps.assertOpenUrl(urlProvider.getCatalog(), TIMEOUT_AQUA);
        tovarTreeTasksPageSteps.assertOpenUrl(urlProvider.getTovarTreeTasks());
        tovarTreeTasksPageSteps.assertPageFullyLoaded();
        tovarTreeTasksPageSteps.assertTaskFields();
        Map<TextInput, String> originalValues = tovarTreeTasksPageSteps.getTaskFields();
        Map<TextInput, String> newValues = tovarTreeTasksPageSteps.getNewTaskFields();
        tovarTreeTasksPageSteps.setTaskFields(newValues);
        tovarTreeTasksPageSteps.assertTaskFields(newValues);
        tovarTreeTasksPageSteps.assertTaskSave(newValues);
        tovarTreeTasksPageSteps.assertRevert(originalValues);
    }

    @Test
    @Ignore
    @Story("Работа с операторским шаблоном")
    @DisplayName("Редактирование, сохранение, генерация и публикация операторского шаблона")
    public void testModelFormOperations() {
        TovarTreeKnowledgesPageSteps pageSteps =
            new TovarTreeKnowledgesPageSteps(webDriverWrapper.getWebDriver());
        pageSteps.assertOpenUrl(urlProvider.getCatalog(), TIMEOUT_AQUA);
        pageSteps.assertOpenUrl(urlProvider.getKnowledges());
        pageSteps.assertFieldsAccessible();
        pageSteps.guessWidgetStateAndPerformTestSteps();
    }

    @Test
    @Ignore
    @Story("Блокировка изменения категории")
    @DisplayName("Блокировка и разблокировка изменений в категории")
    public void testBlockingChanges() {
        TovarTreeBlockingChangesSteps pageSteps =
            new TovarTreeBlockingChangesSteps(webDriverWrapper.getWebDriver());
        pageSteps.assertOpenUrl(urlProvider.getTovarTreeProperties());
        pageSteps.assertPageFullyLoaded();
        pageSteps.assertBlockingChanges();
        pageSteps.assertRevertBlocking();
    }

    @Test
    @Story("Изменение категории")
    @DisplayName("Попытка удалить Sku-определяющий параметр у категории")
    public void testRemoveSkuDefiningParams() throws Exception {
        ParametersPageSteps parametersPageSteps = new ParametersPageSteps(webDriverWrapper.getWebDriver());
        parametersPageSteps.assertOpenUrl(urlProvider.getTovarTreeParams(), TIMEOUT_AQUA);

        parametersPageSteps.assertCheckboxVendor();
        parametersPageSteps.assertCheckboxComment();
        parametersPageSteps.assertRemoveButtonByCheckBoxes();
        parametersPageSteps.assertRemoveButtonComment();
        parametersPageSteps.assertRemoveButtonVendor();
        parametersPageSteps.assertSkuDefiningParamsRemoving();
        parametersPageSteps.assertSkuDefiningParamsRemovingByCheckBox();
        parametersPageSteps.assertGroupRemovingWithSkuDefiningParam();
    }
}
