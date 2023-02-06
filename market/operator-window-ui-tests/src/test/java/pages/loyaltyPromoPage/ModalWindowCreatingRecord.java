package ui_tests.src.test.java.pages.loyaltyPromoPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;


public class ModalWindowCreatingRecord {

    private WebDriver webDriver;

    public ModalWindowCreatingRecord(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку Сохранить
     */
    public void clickSaveRecordButton() {
        try {
            Entity.modalWindow(webDriver).controls().clickButton("Добавить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Добавить\n" + throwable);
        }
    }

    /**
     * Указать код акции
     *
     * @param code код акции
     */
    public ModalWindowCreatingRecord setCode(String code) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("code", code);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Код\n" + throwable);
        }
        return this;
    }

    /**
     * Указать id акции
     *
     * @param Id id акции
     */

    public ModalWindowCreatingRecord setIdLoyaltyPromo(String Id) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("promoId", Id);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать id акции\n" + throwable);
        }
        return this;
    }

    /**
     * Указать номинал
     *
     * @param PromoValue номинал
     */
    public ModalWindowCreatingRecord setIdPromoValue(String PromoValue) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("promoValue", PromoValue);
        } catch (Throwable throwable) {
            throw new Error("Не удалось Номинал\n" + throwable);
        }
        return this;
    }

    /**
     * Указать условие начисления
     *
     * @param condition условие
     * @return
     */
    public ModalWindowCreatingRecord setCondition(String condition) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("condition", condition);
        } catch (Throwable throwable) {
            throw new Error("Не удалось Услоние начисления\n" + throwable);
        }
        return this;
    }

}
