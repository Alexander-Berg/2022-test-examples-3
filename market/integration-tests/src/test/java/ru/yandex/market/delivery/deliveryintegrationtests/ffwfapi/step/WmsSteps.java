package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin.SuppliesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.DriverFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui.UISteps;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import java.util.function.Supplier;

@Resource.Classpath("wms/test.properties")
public class WmsSteps {
    private final Logger log = LoggerFactory.getLogger(WmsSteps.class);
    private WebDriver driver;
    private UISteps uiSteps;

    private final int RETRIES = 3;

    @Property("test.storageCell")
    private String cell;

    private final String CART = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
    private final InboundTable inboundTable;

    private User user;

    public WmsSteps(InboundTable inboundTable) {
        PropertyLoader.newInstance().populate(this);
        this.inboundTable = inboundTable;
    }

    @Attachment(value = "Page screenshot", type = "image/png")
    public byte[] saveScreenshot(byte[] screenShot) {
        return screenShot;
    }

    public void screenshot() {
        if (driver == null) {
            log.info("Driver for screenshot not found");
            return;
        }

        saveScreenshot(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));

    }

    private void runSeleniumStep(Runnable func) {
        runSeleniumStep(() -> {
                    func.run();
                    return null;
                }
        );
    }

    private <T> T runSeleniumStep(Supplier<T> func) {

        T result = null;

        user = DatacreatorSteps
                .Users()
                .lockUser();

        try {
            for (int i = 1; i <= RETRIES; i++) {
                log.info("Selenium step try {}/{}...", i, RETRIES);

                openBrowser();

                try {
                    result = func.get();
                    driver.quit();
                    break;

                } catch (Throwable error) {

                    if (driver != null) {
                        screenshot();
                        driver.quit();
                    }

                    if (error instanceof WebDriverException && i < RETRIES) {
                        log.error(error.getMessage());
                    } else throw error;
                }
            }

        } finally {
            DatacreatorSteps
                    .Users()
                    .unlockUser(user);
        }

        return result;
    }

    private void openBrowser() {
        driver = new DriverFactory().getDriver();
        uiSteps = new UISteps(driver, user, inboundTable);
    }

    @Step("Закрываем поставку на стороне WMS")
    public void closeInbound(String fulfillmentId) {

        runSeleniumStep(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Receiving().findInboundInReceivingAdmin(fulfillmentId);
            SuppliesListPage suppliesListPage = uiSteps.Receiving().closeInboundOnSuppliesListPage();
            uiSteps.Receiving().approveCloseInboundOnSuppliesListPage(suppliesListPage);
        });

    }

    @Step("Принимаем айтемы из поставки {fulfillmentId} и размещаем в ячейку отбора")
    public void acceptItemAndMoveToPickingCell(String fulfillmentId, Item item) {
        runSeleniumStep(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Receiving().initialReceiveItem(fulfillmentId, 1);

            uiSteps.Login().PerformLogin();
            String pallet = uiSteps.Receiving().findPalletOfInbound(fulfillmentId);

            uiSteps.Login().PerformLogin();
            uiSteps.Receiving().receiveItem(item, pallet, CART);

            uiSteps.Login().PerformLogin();
            uiSteps.Placement().placeContainer(CART, cell);
        });
    }
}
