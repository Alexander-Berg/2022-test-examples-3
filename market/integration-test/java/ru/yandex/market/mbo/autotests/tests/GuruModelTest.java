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
import ru.yandex.autotests.common.request.data.HttpGetRequestData;
import ru.yandex.market.mbo.autotests.requests.UrlProvider;
import ru.yandex.market.mbo.autotests.rules.UiRule;
import ru.yandex.market.mbo.autotests.rules.WebDriverWrapper;
import ru.yandex.market.mbo.autotests.steps.CatalogPageSteps;
import ru.yandex.market.mbo.autotests.steps.ModelEditorPageSteps;
import ru.yandex.market.mbo.autotests.user.UserProvider;
import ru.yandex.market.mbo.autotests.webdriver.WebDriverFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static ru.yandex.market.mbo.autotests.matchers.WaiterMatcherFactory.TIMEOUT_AQUA;
import static ru.yandex.market.mbo.autotests.requests.UrlProvider.MODEL_NODE_TYPE;

@Epic("guru-пайплайн")
@Feature("Редактор модели")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:property-configurer.xml"})
public class GuruModelTest {
    @Resource(name = "urlProvider")
    private UrlProvider urlProvider;

    @Resource(name = "webDriverFactory")
    private WebDriverFactory webDriverFactory;

    private WebDriverWrapper operatorWebDriverWrapper;
    private WebDriverWrapper adminWebDriverWrapper;

    @Rule
    public UiRule operatorRuleChain;

    @Rule
    public UiRule adminRuleChain;

    public GuruModelTest() {
        // do nothing
    }

    @PostConstruct
    private void init() {
        operatorWebDriverWrapper = new WebDriverWrapper(webDriverFactory);

        adminWebDriverWrapper = new WebDriverWrapper(webDriverFactory);

        operatorRuleChain = new UiRule(operatorWebDriverWrapper, UserProvider.OPERATOR);
        adminRuleChain = new UiRule(adminWebDriverWrapper, UserProvider.ADMIN);
    }

    @Ignore
    @Test
    @Story("Действия с моделью")
    @DisplayName("Редактирование одного параметра гуру-модели")
    public void testEdit() throws Exception {
        ModelEditorPageSteps modelEditorPageSteps = new ModelEditorPageSteps(
            operatorWebDriverWrapper.getWebDriver()
        );
        modelEditorPageSteps.assertOpenUrl(urlProvider.getCatalog(), TIMEOUT_AQUA);
        modelEditorPageSteps.assertOpenUrl(urlProvider.getModelEdit());
        modelEditorPageSteps.assertSmartphone();
        modelEditorPageSteps.assertChangeSmartphone();
    }

    @Ignore
    @Test
    @Story("Действия с моделью")
    @DisplayName("guru-модели: создание")
    public void testCreate() throws Exception {
        String name = "autotest-create-model";
        ModelEditorPageSteps modelEditorPageSteps = new ModelEditorPageSteps(
            operatorWebDriverWrapper.getWebDriver()
        );
        CatalogPageSteps catalogPageSteps = new CatalogPageSteps(operatorWebDriverWrapper.getWebDriver());
        HttpGetRequestData catalogNode = urlProvider.getCatalogNode();
        catalogPageSteps.assertOpenUrl(catalogNode, TIMEOUT_AQUA);
        boolean refresh = preTestCreate(catalogNode, name);
        if (refresh) {
            catalogPageSteps.refresh();
        }
        catalogPageSteps.assertViewNodes();
        catalogPageSteps.openNewModel();
        catalogPageSteps.assertOpenUrl(urlProvider.getModelCreate());
        modelEditorPageSteps.create(name);
    }

    private boolean preTestCreate(HttpGetRequestData catalogNode, String name) {
        CatalogPageSteps catalogPageSteps = new CatalogPageSteps(adminWebDriverWrapper.getWebDriver());
        catalogPageSteps.assertOpenUrl(catalogNode, TIMEOUT_AQUA);
        catalogPageSteps.assertViewNodes();
        boolean hasNode = catalogPageSteps.hasNode(MODEL_NODE_TYPE, name);
        if (hasNode) {
            catalogPageSteps.deleteNode(MODEL_NODE_TYPE, name);
        }

        return hasNode;
    }

    @Ignore
    @Test
    @Story("Действия с моделью")
    @DisplayName("guru-модели: удаление")
    public void testDelete() throws Exception {
        String name = "autotest-delete-model";
        CatalogPageSteps catalogPageSteps = new CatalogPageSteps(adminWebDriverWrapper.getWebDriver());
        HttpGetRequestData catalogNode = urlProvider.getCatalogNode();
        catalogPageSteps.assertOpenUrl(catalogNode, TIMEOUT_AQUA);
        if (!catalogPageSteps.hasNode(MODEL_NODE_TYPE, name)) {
            preTestDelete(catalogPageSteps, name);
            catalogPageSteps.refresh();
        }
        catalogPageSteps.assertViewNodes();
        catalogPageSteps.deleteNode(MODEL_NODE_TYPE, name);
    }

    private void preTestDelete(CatalogPageSteps catalogPageSteps, String name) {
        ModelEditorPageSteps modelEditorPageSteps = new ModelEditorPageSteps(
            adminWebDriverWrapper.getWebDriver()
        );
        String mainHandle = adminWebDriverWrapper.getWebDriver().getWindowHandle();
        catalogPageSteps.openNewModel();
        modelEditorPageSteps.create(name);
        adminWebDriverWrapper.getWebDriver().close();
        adminWebDriverWrapper.getWebDriver().switchTo().window(mainHandle);
    }
}
