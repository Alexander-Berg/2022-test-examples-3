package ui_tests.src.test.java.entity.entityTable;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import pages.Pages;
import tools.Tools;

import java.util.List;

public final class ToolBar extends Entity {
    private WebDriver webDriver;

    public ToolBar(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    // Кнопка обновления контента в таблице
    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='update']/button")
    private WebElement updateContentButton;

    // Кнопка отображения архивных записей
    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='show-archive']/button//*[text()='Архив']")
    private WebElement displayArchivalEntityButton;

    // Кнопка добавления обращений
    @FindBy(xpath = "//*[contains(@*[starts-with(name(.),'data-ow-test')],'create')]//button")
    private WebElement addTicketButton;

    // Инпут быстрого поиска

    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='search-input']//input")
    WebElement quickSearchInput;

    // Инпут быстрого поиска
    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='search-input']//button")
    WebElement cleatQuickSearchButton;

    // Кнопка быстрого поиска
    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='search-button']//button")
    WebElement quickSearchButton;

    // Кнопка-селект предсохраненых фильтров
    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='table-view-select']//button")
    WebElement savedFilterButton;

    @FindBy(xpath = "//*[@class='react-datepicker-wrapper']//input")
    List<WebElement> timeArchivedInputs;

    /**
     * Ввести текст в поле быстрого поиска
     *
     * @param searchText
     */
    public ToolBar setQuickSearch(String searchText) {
        try {
            Tools.sendElement(webDriver).sendElement(quickSearchInput, searchText);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось ввести текст в поле ввода для быстрого поиска \n" + t);
        }
    }

    public ToolBar clearQuickSearch() {
        try {
            Tools.waitElement(webDriver).waitClickableElementTheTime(cleatQuickSearchButton, 2);
            cleatQuickSearchButton.click();

        } catch (Throwable t) {
            // throw new Error("Не удалось очистить поле быстрого поиска\n" + t);
        }
        return this;
    }

    /**
     * Показать архивные записи
     *
     * @return Content()
     */
    public void displayArchivalEntityButtonClick() {
        try {
            Tools.clickerElement(webDriver).clickElement(displayArchivalEntityButton);
        } catch (Throwable t) {
            throw new Error("Нажать на кнопку отображения рхивных записей \n" + t);
        }
    }

    /**
     * Указать Время архивирования с: в формате ddMMyyyy
     *
     * @return
     */
    public ToolBar setStartTimeArchived(String date) {
        Tools.sendElement(webDriver).sendElement(timeArchivedInputs.get(0), Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE, date);
        // Tools.other().sendElement(webDriver,timeArchivedInputs.get(0), date);
        return this;
    }

    /**
     * Нажать на кнопку применения фильтра архивных записей
     *
     * @return
     */
    public ToolBar applyingArchiveTimeFilter() {
        Entity.buttons(webDriver).clickButton("", "Применить");
        return this;
    }

    /**
     * Нажать на кнопку быстрого поиска
     *
     * @return Content()
     */
    public void quickSearchButtonClick() {
        try {
            Tools.clickerElement(webDriver).clickElement(quickSearchButton);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку быстрого поиска \n" + t);
        }
    }

    /**
     * Нажать на кнопку добавления элемента
     *
     * @return ListOfTypesForCreatingEntity()
     */
    public void clickOnAddTicketButton() {
        try {
            Tools.clickerElement(webDriver).clickElement(addTicketButton);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку добавления элемента \n" + t);
        }
    }

    /**
     * Нажать на кнопку обновления таблицы
     */
    public void updateContent() {

        try {
            Pages.ticketPage(webDriver).toast().hideNotificationError();
            Tools.clickerElement(webDriver).clickElement(updateContentButton);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку обновления страницы \n" + t);
        }
    }

    /**
     * Задать сохраненый фильтр
     *
     * @param filterName название фильтра
     * @return Content()
     */
    public void setSavedFilter(String filterName) {
        String xpath = Entity.properties(webDriver).getXPathElement(filterName);
        try {
            Pages.ticketPage(webDriver).toast().hideNotificationError();
            Tools.clickerElement(webDriver).clickElement(savedFilterButton);
            Tools.clickerElement(webDriver).clickElement(By.xpath(xpath+"/.."));
        } catch (Throwable t) {
            throw new Error("Не удалось применить предсохраненный фильтр " + filterName + " \n" + t);
        }
    }

    /**
     * Выбрать запись из выпадающего меню
     *
     * @param recordName - название типа
     */
    public void selectEntityOnSelectMenu(String recordName) {
        try {
            Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@role='tooltip']//button[text()='" + recordName + "']"));
        } catch (Throwable t) {
            throw new Error("Не удалось выбрать тип обращения который нужно создать \n" + t);
        }
    }
}
