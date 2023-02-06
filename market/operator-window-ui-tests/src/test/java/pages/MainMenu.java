package ui_tests.src.test.java.pages;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class MainMenu {
    private WebDriver webDriver;

    MainMenu(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "//div[@data-tid='e1d27e09']/button[@title='развернуть']")
    private WebElement maximizeButtonMenu;

    @FindBy(xpath = "//div[@data-tid='e1d27e09']/button[@title='свернуть']")
    private WebElement minimizeMenuButton;

    @FindBy(xpath = "//menu//a[@title='Письменная коммуникация']")
    private WebElement rootPagesButton;

    @FindBy(xpath = "//menu//a[@title='Поиск']")
    private WebElement searchPageButton;

    @FindBy(xpath = "//*[@class='_p4p5hTc lb_uDcSY']")
    private WebElement buttonForOpeningUserProperties;

    @FindBy(xpath = "//*[@class='_1nYbmkbT']")
    private WebElement statusUser;

    @FindBy(xpath = "//*[text()='Работает над обращением:']/a")
    private WebElement ticketNumberInWork;

    @FindBy(xpath = "//li[@title='Второй монитор']")
    private WebElement secondScreenButton;

    @FindBy(xpath = "//li[@title='Просьбы о помощи']")
    private WebElement supervisorHelpButton;


    /**
     * Открыть страницу Письменной Коммуникации
     *
     * @return MainMenu
     */
    public void openRootPage() {
        try {
            Tools.clickerElement(webDriver).clickElement(rootPagesButton);
        } catch (Throwable e) {
            throw new Error("Не получилось открыть страницу письменной коммуникации" + " \n" + e);
        }
    }

    /**
     * Открыть страницу поиска
     */
    public void openSearchPage() {
        try {
            Tools.clickerElement(webDriver).clickElement(searchPageButton);
        } catch (Throwable e) {
            throw new Error("Не удалось открыть страницу поиска: \n" + e);
        }
    }

    /**
     * Нажать на кнопку разворачивания основного меню
     *
     * @return MainMenu
     */
    public MainMenu clickMaximizeButtonMenu() {
        try {
            maximizeButtonMenu.click();
            return this;
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на кнопку разворачивания бокового меню" + " \n" + e);
        }
    }

    /**
     * Нажать на кнопку сворачивания основного меню
     *
     * @return MainMenu
     */
    public MainMenu clickMinimizeMenuButton() {
        try {
            minimizeMenuButton.click();
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на кнопку сворачивания бокового меню" + " \n" + e);
        }
        return this;
    }

    /**
     * Открыть свойства пользвоателя (всплывашка со сменой статуса и ссылкой на обращение в работе)
     *
     * @return MainMenu
     */
    public MainMenu openPropertiesUser() {
        try {
//            Tools.waitElement(webDriver).waitVisibilityElement(buttonForOpeningUserProperties);
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
//            buttonForOpeningUserProperties.click();
            Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@class='_p4p5hTc lb_uDcSY']"));
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на кнопку открытия свойства пользователя (всплывашка со сменой статуса оператора)" + "\n" + e);
        }
        return this;
    }

    /**
     * Получить текущий статус оператора
     *
     * @return текущий статус оператора
     */
    public String getUserStatus() {
        try {
            return statusUser.getText();
        } catch (Throwable t) {
            if (!t.toString().contains("Не получилось найти элемент на странице")) {
                throw new Error("Не удалось получить текуущий статус оператора\n" + t);
            } else {
                return "";
            }
        }
    }

    /**
     * Получить номер тикета над которым сейчас работает оператор
     *
     * @return номер тикета, над которым работает оператор
     */
    public String getTicketNumberInWork() {
        try {
            return ticketNumberInWork.getText();
        } catch (Throwable t) {
            if (!t.toString().contains("Не получилось найти элемент на странице")) {
                throw new Error("Не удалось получить номер обращения в работе\n" + t);
            } else {
                return "";
            }
        }
    }

    /**
     * Получить ссылку на тикет в работе у оператора
     *
     * @return ссылка на тикет, над которым работает оператор
     */
    public String getLinkOnTicketInWork() {
        try {
            return ticketNumberInWork.getAttribute("href");
        } catch (Throwable t) {
            if (!t.toString().contains("Не получилось найти элемент на странице")) {
                throw new Error("Не удалось получить ссылку на обращение в работе\n" + t);
            } else {
                return "";
            }
        }
    }

    /**
     * Перевести пользователя в статус
     *
     * @param status статус в который нужно перевести пользвоателя
     */
    public void switchUserToStatus(String status) {
        try {
            By statusBy = By.xpath("//menu[@data-tid='35fd9ae9']/li[text()='" + status + "']");
            Tools.clickerElement(webDriver).clickElement(statusBy);
        } catch (Throwable e) {
            throw new Error("Не получилос перевести пользвоателя в статус " + status + " \n" + e);
        }
    }

    /**
     * Получить номер тикета в работе
     *
     * @return номер тикета String
     */
    public String getTicketNumberFromTicketInWork() {
        By numberTicketBy = By.xpath("//*[@id='ow-popper-portal']//*[contains(@href,'/entity/ticket')]");
        try {
            return Tools.findElement(webDriver).findVisibleElement(numberTicketBy).getText();
        } catch (Throwable t) {
            if (!t.toString().contains("Не получилось найти элемент на странице")) {
                throw new Error("Не удалось получить нлмер обращения в работе\n" + t);
            } else {
                return "";
            }
        }
    }

    /**
     * Нажать на ссылку ведущую на страницу текущего пользователя
     */
    public void openEmployeePage() {
        try {
            Pages.mainMenuOfPage(webDriver).openPropertiesUser();
            Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@id='ow-popper-portal']//a[contains(@href,'employee')]"));
            Tools.clickerElement(webDriver).clickElement(statusUser);
        } catch (Throwable throwable) {
            throw new Error("Не получилось нажать на ссылку ведущую на страницу текущего пользователя \n" + throwable);
        }
    }

    /**
     * Нажать на кнопку "Второй монитор"
     */
    public void openSecondScreen() {
        try {
            Tools.scripts(webDriver).scrollToBottom();
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            secondScreenButton.click();
        } catch (Throwable e) {
            throw new Error("Не удалось нажать на кнопку второго монитора" + "\n" + e);
        }
        Tools.tabsBrowser(webDriver).takeFocusNewTab();
    }

    /**
     * Нажать на кнопку Просьбы о помощи
     */
    public void clickSupervisorHelp() {
        try {
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            supervisorHelpButton.click();
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку 'Просьбы о помощи'");
        }
    }

    /**
     * получить список запросов пользователей на помощь находящихся в работе
     *
     * @return
     */
    public List<String> getListEmployeeWhoRequestedHelp() {
        List<String> employees = new ArrayList<>();
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[@role='tooltip']"));
        List<WebElement> webElementList = Tools.findElement(webDriver).findElements(By.xpath("//*[@role='tooltip']//div/button[@title='Помощь оказана']/../div"));
        for (WebElement webElement : webElementList) {
            employees.add(webElement.getText());
        }
        return employees;
    }

    /**
     * получить список пользователей запросивших помощь у супервизира
     *
     * @return
     */
    public List<String> getHelpRequestsInProgress() {
        List<String> employees = new ArrayList<>();
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[@role='tooltip']"));
        List<WebElement> webElementList = Tools.findElement(webDriver).findElements(By.xpath("//*[@role='tooltip']//div/button[@title='Пойти на помощь']/../div"));
        for (WebElement webElement : webElementList) {
            employees.add(webElement.getText());
        }
        return employees;
    }

    /**
     * взять запрос о помощи в работу
     *
     * @param userName
     * @return
     */
    public MainMenu helpTheUser(String userName) {
        Tools.findElement(webDriver).findElementInDOM(By.xpath("//*[@role='tooltip']//a[contains(text(),'" + userName + "')]/../../..//button[@title='Пойти на помощь']")).click();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        return this;
    }
}
