package ui_tests.src.test.java.pages.employeePage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class ModalWindowCreateOrEditRecords {

    private WebDriver webDriver;

    public ModalWindowCreateOrEditRecords(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать Название
     *
     * @param title
     * @return
     */
    public ModalWindowCreateOrEditRecords setTitle(String title) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("title", title);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указаьт значение поля Название\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Подразделение
     *
     * @param ou
     * @return
     */
    public ModalWindowCreateOrEditRecords setOU(List<String> ou) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfMultiSuggestTypeField("ou", ou);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Подразделение\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Псевдоним
     *
     * @param alias
     * @return
     */
    public ModalWindowCreateOrEditRecords setAlias(String alias) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("alias", alias);
        } catch (Throwable throwable) {
            throw new Error("Не удалось значение поля Псевдоним\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Роли
     *
     * @param roles
     * @return
     */
    public ModalWindowCreateOrEditRecords setRoles(List<String> roles) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfTreeSelectTypeField("roles", roles);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Роли\n" + throwable);
        }
        return this;
    }

    /**
     * Указть Линии
     *
     * @param teams
     * @return
     */
    public ModalWindowCreateOrEditRecords setTeams(List<String> teams) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfMultiSuggestTypeField("teams", teams);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Линии\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Очереди
     *
     * @param services
     * @return
     */
    public ModalWindowCreateOrEditRecords setServices(List<String> services) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfTreeSelectTypeField("services", services);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Очереди\n" + throwable);
        }
        return this;
    }

    /**
     * Нажать на кнопку Сохранить
     */
    public void clickSaveRecordButton() {
        try {
            Entity.modalWindow(webDriver).controls().clickButton("Сохранить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Сохранить\n" + throwable);
        }

    }


}
