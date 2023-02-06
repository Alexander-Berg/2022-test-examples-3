package ui_tests.src.test.java.pages.ticketPage.createTicketPage;


import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;
import unit.Config;

import java.util.List;

public class Properties {
    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        //Временно убрал
       //Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//div[@data-tid='f5bfce9 efa51d8d']/.."), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Заполнить поле Контактый Email
     *
     * @param clientEmail контактый email клиента
     * @return
     */
    public Properties setClientEmail(String clientEmail) {
        try {
            Entity.properties(webDriver).setInputField("clientEmail", clientEmail);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось заполнить поле Контактый Email \n" + t);
        }
    }

    /**
     * Заполнить поле Тема
     *
     * @param title назание темы
     * @return
     */
    public Properties setTitle(String title) {
        try {
            Entity.properties(webDriver).setInputField("title", title);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось заполнить поле Тема \n" + t);
        }
    }

    /**
     * Заполнить поле Номер телефона
     *
     * @param phoneNumber
     * @return
     */
    public Properties setContactPhoneNumber(String phoneNumber) {
        try {
            Entity.properties(webDriver).setInputField("clientPhone", phoneNumber);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось заполнить поле Номер телефона \n" + t);
        }
    }

    /**
     * Заполнить поле Очередь в поле с типом саджест
     *
     * @param service назание очереди
     * @return
     */
    public Properties setServiceSuggest(String service) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField("service", service);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле Очередь \n" + t);
        }
    }

    /**
     * Заполнить поле Очередь в поле с типом селект
     *
     * @param service назание очереди
     * @return
     */
    public Properties setServiceSelect(String service) {
        try {
//            Entity.properties(webDriver).setPropertiesOfSelectTypeField("service", service);
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField("service", service);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле Очередь \n" + t);
        }
    }

    /**
     * Заполнить поле Очередь
     *
     * @param service назание очереди
     * @return
     */
    public Properties setService(List<String> service) {
        try {
            Entity.properties(webDriver).setPropertiesOfTreeSelectTypeField("service", service);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле Очередь \n" + t);
        }
    }

    /**
     * Заполнить поле Категория
     *
     * @param categories категории
     * @return
     */
    public Properties setCategories(List<String> categories) {
        try {
            Entity.properties(webDriver).setPropertiesOfTreeSelectTypeField("categories", categories);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле категории \n" + t);
        }
    }

    /**
     * Заполнить поле Номер логистического заказа
     *
     * @param deliveryOrder номер логистического заказа
     * @return
     */
    public Properties setDeliveryOrder(String deliveryOrder) {
        try {
            Entity.properties(webDriver).setInputField("rawYaDeliveryOrderId", deliveryOrder);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле номер логистического заказа \n" + t);
        }
    }

    public Properties setOrder(String order) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField("order", order);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение в поле Заказ \n" + t);
        }
    }

    /**
     * Отметить чек-бокс "Назначить на себя?"
     */
    public Properties setAssignToAuthorCheckBox(boolean status) {
        try {
            Entity.properties(webDriver).setPropertiesOfCheckBoxTypeField("assignToAuthor", status);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось изменить значение чек-бокса 'Назначить на себя?': \n" + t);
        }
    }
}
