package ui_tests.src.test.java.pages.ticketPage.properties;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.Arrays;
import java.util.List;

public class Properties {
    public String block = "//*[@style=\"grid-column: span 3 / auto;\"]";
    private final WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        // Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//*[@data-tid=\"745ad2f\" and contains(@style,'span 3')]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    public void waitStatus(String textStatus) {
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[contains(@data-tid,\"f5bfce9\")]//*[text()='Статус']/../../*[2]//*[text()='" + textStatus + "']))"));
    }

    /**
     * Получить Контактный Email
     *
     * @return
     */
    public String getContactEmail() {
        try {
            return Entity.properties(webDriver).getValueField(block, "clientEmail");
        } catch (Throwable t) {
            throw new Error("Не удалось получить контактный email клиента из обращения:\n" + t);
        }
    }

    /**
     * Получить Приоритет
     *
     * @return
     */
    public String getPriority() {
        try {
            return Entity.properties(webDriver).getValueField(block, "priority");
        } catch (Throwable t) {
            throw new Error("Не удалось получить приоритет обращения:\n" + t);
        }
    }

    /**
     * Получить заказ
     *
     * @return
     */
    public String getOrderNumber() {
        try {
            return Entity.properties(webDriver).getValueField(block, "order");
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Указать заказ
     *
     * @param order номер заказа
     * @return
     */
    public Properties setOrderNumber(String order) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField(block, "order", order);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать номер заказа в обращении:\n" + t);
        }
    }

    /**
     * Получить Линию
     *
     * @return
     */
    public String getTeam() {
        try {
            return Entity.properties(webDriver).getValueField(block, "responsibleTeam");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Линию обращения:\n" + t);
        }
    }

    /**
     * Получить Очередь
     *
     * @return
     */
    public String getService() {
        try {
            return Entity.properties(webDriver).getValueField(block, "service");
        } catch (Throwable t) {
            throw new Error("Не удалось получить очередь обращения\n" + t);
        }
    }

    public Properties setService(String service) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField(block, "service", service);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось получить очередь обращения:\n" + t);
        }
    }

    /**
     * Получить Категорию
     *
     * @return
     */
    public List<String> getCategory() {
        try {
            return Entity.properties(webDriver).getValuesField("categories");
        } catch (Throwable t) {
            throw new Error("Не удалось получить категорию обращения:\n" + t);
        }
    }

    public Properties setCategory(List<String> categories) {
        try {
            Entity.properties(webDriver).setPropertiesOfTreeSelectTypeField("categories", categories);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать категорию обращения:\n" + t);
        }
    }

    /**
     * Получить Статус
     *
     * @return
     */
    public String getStatus() {
        try {
            return Entity.properties(webDriver).getValueField(block, "status");
        } catch (Throwable t) {
            throw new Error("Не удалось получить статус обращения:\n" + t);
        }
    }

    /**
     * Получить Контактный телефон
     *
     * @return
     */
    public String getContactPhoneNumber() {
        try {
            String phoneNumber = Entity.properties(webDriver).getValueField(block, "clientPhone");
            phoneNumber = phoneNumber.replaceAll("\\D", "");
            if (phoneNumber.length() > 11) {
                return phoneNumber.substring(1, 11);
            }
            return phoneNumber.substring(1);
        } catch (Throwable t) {
            throw new Error("Не удалось получить контактный телефон клиента:\n" + t);
        }
    }

    public String getAdditionalContactPhoneNumber() {
        try {
            String phoneNumber = Entity.properties(webDriver).getValueField(block, "clientPhone");
            phoneNumber = phoneNumber.replaceAll("\\D", "");
            if (phoneNumber.length() > 11) {
                return phoneNumber.substring(11);
            }
            return phoneNumber;
        } catch (Throwable t) {
            throw new Error("Не удалось получить контактный телефон клиента:\n" + t);
        }
    }

    /**
     * Получить Категорию клиента
     */
    public List<String> getClientCategory() {
        try {
            return Entity.properties(webDriver).getValuesField("tags");
        } catch (Throwable e) {
            throw new Error("Не удалось получить категорию клиента:\n " + e);
        }
    }

    /**
     * Получить Службу доставки
     */
    public String getDeliveryService() {
        try {
            return Entity.properties(webDriver).getValueField("deliveryService");
        } catch (Throwable e) {
            throw new Error("Не удалось получить служду доставки: \n" + e);
        }
    }

    /**
     * Получить тэг
     *
     * @return
     */
    public List<String> getTag() {
        try {
            return Entity.properties(webDriver).getValuesField("tags");
        } catch (Throwable t) {
            throw new Error("Не удалось получить тэги в обращении:\n" + t);
        }
    }

    public Properties setTag(List<String> tags) {
        try {
            Entity.properties(webDriver).setPropertiesOfMultiSuggestTypeField("tags", tags);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать тэг в обращении:\n" + t);
        }
    }

    /**
     * Получить значение поля ответственный сотрудник
     *
     * @return
     */
    public String getResponsibleEmployee() {
        try {
            return Entity.properties(webDriver).getValueField("", "responsibleEmployee");
        } catch (Throwable t) {
            throw new Error("Не получить ответственного сотрудника за обращение\n" + t);
        }
    }

    /**
     * Получить партнёра
     *
     * @return
     */
    public String getPartner() {
        try {
            return Entity.properties(webDriver).getValueField(block, "partner");
        } catch (Throwable t) {
            throw new Error("Не получить партнёра в обращении\n" + t);
        }
    }

    /**
     * Указать партнёра
     *
     * @param partner название партнёра
     * @return
     */
    public Properties setPartner(String partner) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField(block, "partner", partner);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать партнёра в обращении:\n" + t);
        }
    }

    /**
     * Указать партнёра по id
     *
     * @param partner id партнёра
     * @param name    имя партнёра
     * @return
     */
    public Properties setPartnerId(String partner, String name) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeFieldWhenSearchById(block, "partner", partner, name);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать партнёра в обращении:\n" + t);
        }
    }
}
