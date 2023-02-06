package ui_tests.src.test.java.pages.loyaltyPromoPage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Properties {

    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить код акции
     *
     * @return
     */

    public String getCode() {
        try {
            return Entity.properties(webDriver).getValueField("code");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Код\n" + throwable);
        }
    }

    /**
     * получить id акции
     *
     * @return
     */
    public String getPromoId() {
        try {
            return Entity.properties(webDriver).getValueField("promoId");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля id акции\n" + throwable);
        }
    }

    /**
     * получить номинал акции
     *
     * @return
     */
    public String getPromoValue() {
        try {
            String value = Entity.properties(webDriver).getValueField("promoValue");
            value = value.replace(" ", "");
            value = value.subSequence(0, value.indexOf(',')).toString();
            return value;
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Номинал\n" + throwable);
        }
    }

    /**
     * Получить условие назначения
     *
     * @return
     */
    public String getCondition() {
        try {
            return Entity.properties(webDriver).getValueField("condition");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Условие\n" + throwable);
        }
    }

}
