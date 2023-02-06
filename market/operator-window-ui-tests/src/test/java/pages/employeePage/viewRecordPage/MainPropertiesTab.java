package ui_tests.src.test.java.pages.employeePage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class MainPropertiesTab {

    private WebDriver webDriver;

    public MainPropertiesTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Полуучить Название
     *
     * @return
     */
    public String getTitle() {
        try {
            return Entity.properties(webDriver).getValueField("title");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Название\n" + throwable);
        }

    }

    /**
     * Получить Подразделение
     *
     * @return
     */
    public List<String> getOU() {
        try {
            return Entity.properties(webDriver).getValuesField("ou");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Подразделение\n" + throwable);
        }

    }

    /**
     * Получить Псевдоним
     *
     * @return
     */
    public String getAlias() {
        try {
            return Entity.properties(webDriver).getValueField("alias");
        } catch (Throwable throwable) {
            throw new Error("Не удалось полусить значение поля Псевдоним\n" + throwable);
        }
    }

    /**
     * Получить Роли
     *
     * @return
     */
    public List<String> getRoles() {
        try {
            return Entity.properties(webDriver).getValuesField("roles");
        } catch (Throwable throwable) {
            throw new Error("Не удалось полчуить значение поля Роли\n" + throwable);
        }
    }

    /**
     * Получить Линии
     *
     * @return
     */
    public List<String> getTeams() {
        try {
            return Entity.properties(webDriver).getValuesField("teams");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Линии\n" + throwable);
        }
    }

    /**
     * Получить Очереди
     *
     * @return
     */
    public List<String> getServices() {
        try {
            return Entity.properties(webDriver).getValuesField("services");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Очереди\n" + throwable);
        }
    }

    /**
     * Получить значение флага Требуется учетная запись телефонии
     *
     * @return
     */
    public boolean getVoximplantEnabled() {
        try {
            return Entity.properties(webDriver).getBooleanStateFlag("voximplantEnabled");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить занчение поля Требуется учетная запись телефонии\n" + throwable);
        }
    }

    /**
     * Нажать на кнопку изменения свойств
     */
    public void clickEditPropertiesButton() {
        try {
            Entity.buttons(webDriver).clickButton("//*[text()='Основные свойства']/..", null);
        } catch (Throwable error) {
            if (!error.getMessage().contains("не пропал со страницы")) {
                throw new Error("Не удалось нажть на кнопку изменения свойств (карандашик) \n" + error);
            }

        }
    }
}
