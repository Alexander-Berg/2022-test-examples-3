package ui_tests.src.test.java.pages.loyaltyPromoPage.editRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Properties {

    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать id акции
     *
     * @param promoId id акции
     * @return
     */
    public Properties setPromoId(String promoId) {
        try {
            Entity.properties(webDriver).setInputField("promoId", promoId);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля id акции\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Номинал
     *
     * @param promoValue Номинал
     * @return
     */
    public Properties setPromoValue(String promoValue) {
        try {
            Entity.properties(webDriver).setInputField("promoValue", promoValue);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать начение поля Номинал\n" + throwable);
        }
        return this;
    }

    /**
     * Указать условие начисления
     *
     * @param condition условие
     * @return
     */
    public Properties setCondition(String condition) {
        try {
            Entity.properties(webDriver).setInputField("condition", condition);
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить занчение поля Условие\n" + throwable);
        }
        return this;
    }
}
