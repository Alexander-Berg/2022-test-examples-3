package ui_tests.src.test.java.pageHelpers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.Pages;
import tools.Tools;
import unit.Config;

public class MainMenuHelper {
    private WebDriver webDriver;

    public MainMenuHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }

    /**
     * получить статус пользователя
     * @return
     */
    public String getUserStatus() {
       // Pages.mainMenuOfPage(webDriver).openPropertiesUser();
        return Pages.mainMenuOfPage(webDriver).getUserStatus();
    }

    /**
     * Получить номер тикета над которым работает оператор
     * @return
     */
    public String getTicketNumberInWork() {
        Pages.mainMenuOfPage(webDriver).openPropertiesUser();
        return Pages.mainMenuOfPage(webDriver).getTicketNumberInWork();
    }

    /**
     * Перевести оператора в статус
     * @param status статус6 в который необходимо перевести оператора
     */
    public void switchUserToStatus(String status) {
        Pages.mainMenuOfPage(webDriver).openPropertiesUser();
        Pages.mainMenuOfPage(webDriver).switchUserToStatus(status);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath("//*[@class='_1nYbmkbT' and @title='Не готов']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Получить ссылку на обращение находящегося в работе
     * @return номер тикета  String
     */
    public String getLinkOnTicketFromTicketInWork(){
        Pages.mainMenuOfPage(webDriver).openPropertiesUser();
        return Pages.mainMenuOfPage(webDriver).getLinkOnTicketInWork();

    }
}
