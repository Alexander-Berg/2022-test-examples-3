package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class PackingPage extends AbstractPage {

    private final NotificationDialog notificationDialog;

    @FindBy(xpath = "//div[@data-e2e='item-scan']//input")
    private SelenideElement input;

    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement additionalMenuButton;

    @FindBy(xpath = "//button[@data-e2e='Context_packing_lost_items']")
    private SelenideElement shortItems;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement yesButton;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement confirmButton;

    @FindBy(xpath = "//div[@data-e2e='popup-text-field']//input")
    private SelenideElement cancelledContainerInput;

    @FindBy(xpath = "//div[@data-e2e='popup-text-field']//input")
    private SelenideElement containerInput;

    @FindBy(xpath = "//button[@data-e2e='refresh-task-button']")
    private SelenideElement getOtherTaskButton;

    @FindBy(xpath = "//span[starts-with(@data-e2e,'clickable_item_title_')]")
    private SelenideElement sortCellName;

    @FindBy(xpath = "//div[@data-e2e='carton-scan']/div/input")
    private SelenideElement packTypeInput;

    @FindBy(xpath = "//span[@data-e2e='no-task-text']")
    private SelenideElement noTaskText;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement packButton;

    @FindBy(xpath = "//span[@data-e2e='sortingCell']")
    private SelenideElement containerName;

    private final By suggestedPackTypeDialog = byXpath("//span[starts-with(text(), 'Сканируйте Kоробку')]/span");
    private final By containerLabelInput = byXpath("//div[@data-e2e='popup-text-field']//input");

    public PackingPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("packingPage"));
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Вводим контейнер, если требуется")
    public PackingPage enterContainerIfNecessary(String containerLabel) {
         if (containerLabel == null) {
             return this;
         }

        performInputInActiveElement(containerInput, containerLabel);
        return this;
    }

    @Step("Вводим предложенный контейнер")
    public String enterSuggestedContainer() {
        String containerLabel = StringUtils.substringBetween(containerName.getText(), "(", ")");

        performInputInActiveElement(containerInput, containerLabel);
        return containerLabel;
    }

    @Step("Вводим УИТ товара")
    public void enterUits(List<String> serials) {
        serials.forEach(sn -> {
            performInputInActiveElement(input, sn);
        });

        enterSuggestedPackType();

        try {
            noTaskText.shouldBe(visible);
        } catch (ElementNotFound e) {
            log.info("No task text was not found, trying to find cancelledContainerInput");
            cancelledContainerInput.shouldBe(visible);
        }
    }

    @Step("Вводим уиты товара, проверяя наличие следующего задания")
    public void enterUitsAndCheckForNextTask(List<String> serials) {
        serials.forEach(sn -> {
            performInputInActiveElement(input, sn);
        });

        enterSuggestedPackType();

        if (isElementPresent(containerLabelInput)) {
            return;
        }

        noTaskText.shouldBe(visible);
    }

    @Step("Вводим УИТ товара")
    public void enterUit(List<String> serials) {
        serials.forEach(sn -> {
            performInputInActiveElement(input, sn);
        });

        enterSuggestedPackType(true);
    }

    @Step("Вводим УИТы товара, с шортированием")
    public void enterUitsAndDoShortage(List<String> serials, boolean isLastTask) {
        if (serials.isEmpty()) {
            waitElementHasFocus(input);//дождёмся, пока страница загрузилась и получили задание
            shortSerials();
            if (isLastTask) {
                noTaskText.shouldBe(visible);
            }
        } else {
            serials.forEach(sn -> {
                performInputInActiveElement(input, sn);
            });
            waitElementHasFocus(input);
            shortOtherSerials();
            // тут сканированные УИТы могут быть или снова в путволе или в контейнере отменёнки
            // по системе может быть выбрано или свежесозданное STUCK-задание или следующее живое задание
            // что именно будет на экране - нет способа гарантировать.
        }
    }

    @Step("Вводим УИТ товара")
    public void enterUits(Map<String, String> itemSerialsToSortingCellsMapping) {
        var serialsGroupedBySortCell = new HashMap<String, List<String>>();
        for (var entry : itemSerialsToSortingCellsMapping.entrySet()) {
            var itemSerial = entry.getKey();
            var cell = entry.getValue();
            var itemSerials = serialsGroupedBySortCell.computeIfAbsent(cell, x -> new ArrayList<>());
            itemSerials.add(itemSerial);
        }

        var i = 0;
        while (i < itemSerialsToSortingCellsMapping.size()) {
            var sortCell = sortCellName.getText();
            var itemSerials = serialsGroupedBySortCell.get(sortCell);

            for (var sn : itemSerials) {
                Allure.step("Entering sn: " + sn + " cell: " + sortCell);
                performInputInActiveElement(input, sn);
                i++;
            }

            enterSuggestedPackType();
        }

        try {
            noTaskText.shouldBe(visible);
        } catch (ElementNotFound e) {
            log.info("No task text was not found, trying to find cancelledContainerInput");
            cancelledContainerInput.shouldBe(visible);
        }
    }

    @Step("Вводим УИТ товара")
    public void enterUitOnePerPack(List<String> serials) {
        serials.forEach(sn -> {
            performInputInActiveElement(input, sn);
            enterSuggestedPackType();
        });

        try {
            noTaskText.shouldBe(visible);
        } catch (ElementNotFound e) {
            log.info("No task text was not found, trying to find cancelledContainerInput");
            cancelledContainerInput.shouldBe(visible);
        }
    }

    @Step("Шортим оставшиеся УИТы заказа")
    public void shortOtherSerials() {
        SeleniumUtil.jsClick(additionalMenuButton, driver);
        SeleniumUtil.jsClick(shortItems, driver);
        clickForwardInModal(yesButton, "Вы уверены что товаров больше нет");
        clickForwardInModal(confirmButton, "Положите отсканированные УИТы");
    }

    @Step("Соглашаемся упаковать по НЗН")
    public void packId(String id) {
        clickForwardInModal(packButton, "Вы действительно хотите разом упаковать весь контейнер () с 1 товаром");
    }

    @Step("Шортим все УИТы заказа")
    public void shortSerials() {
        SeleniumUtil.jsClick(additionalMenuButton, driver);
        SeleniumUtil.jsClick(shortItems, driver);
        clickForwardInModal(yesButton, "Вы уверены что товаров больше нет");
    }

    @Step("Ждем появления модалки, жмем кнопку продолжить и ждем, пока она исчезнет")
    public void clickForwardInModal(SelenideElement button, String text) {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        SeleniumUtil.jsClick(button, driver);
        modalWindow.waitModalHiddenWithText(text);
    }

    @Step("Вводим тип посылки")
    public void enterSuggestedPackType() {
        if ($$(suggestedPackTypeDialog).size() == 0) {
            return;
        }
        performInputInActiveElement(packTypeInput, getSuggestedPackType());
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Посылка создана"),
                "Не появилось сообщения о создании посылки");
        waitElementHidden(suggestedPackTypeDialog, true);
    }

    @Step("Вводим тип посылки")
    public void enterSuggestedPackType(boolean userSelectsTask) {
        if ($$(suggestedPackTypeDialog).size() == 0) {
            return;
        }
        performInputInActiveElement(packTypeInput, getSuggestedPackType());

        if (!userSelectsTask) {
            Assertions.assertTrue(notificationDialog.isPresentWithTitle("Посылка создана"),
                    "Не появилось сообщения о создании посылки");
            waitElementHidden(suggestedPackTypeDialog, true);
        }
    }

    private String getSuggestedPackType() {
        return $(suggestedPackTypeDialog).getText();
    }

    @Step("Переносим в контейнер для отмененки")
    public void moveCancelledItems(List<String> stuckItems, String containerForCancelledLabel) {
        try {
            cancelledContainerInput.shouldBe(visible);
        } catch (ElementNotFound e) {
            // если такое случилось, то будет ожидание в 1.5 минуты от shouldBe(visible). Для оптимизации вариант
            // попытаться считать данные, вместо поиска диалога, но такое надо отлавливать.
            Allure.step("Не видим диалог про указание тары отменёнки. Пробуем сменить задание.");
            SeleniumUtil.jsClick(getOtherTaskButton, driver);
        }
        performInputInActiveElement(cancelledContainerInput, containerForCancelledLabel);
        stuckItems.forEach(sn -> performInputInActiveElement(input, sn));
    }

    @Step("Подбираем задание на упаковку, чтобы из ячейки {putwallCell}")
    public PackingPage pickTaskForCell(String putwallCell) {
        // Задания на упаковку подгружаются в JVM каждые 10 секунд
        Retrier.retry(() -> {
            var seenCells = new HashSet();
            waitElementHasFocus(input);//дождёмся, пока страница загрузилась и получили задание
            var currentSortCell = sortCellName.getText();
            while (!seenCells.contains(currentSortCell)) {
                seenCells.add(currentSortCell);
                if (currentSortCell.equals(putwallCell)) {
                   break;
                }
                Allure.step(String.format("Задание для ячейки %s, берём следующее задание", currentSortCell));
                SeleniumUtil.jsClick(getOtherTaskButton, driver);
                waitElementHasFocus(input);//дождёмся, пока страница загрузилась и получили задание
                currentSortCell = sortCellName.getText();
            }
            Assertions.assertTrue(seenCells.contains(putwallCell),
                    "Не получилось найти задание для нужной ячейки путвола");
            Allure.step(String.format("Видим на экране задание для ячейки %s", currentSortCell));
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        return this;
    }
}
