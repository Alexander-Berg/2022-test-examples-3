package ui_tests.src.test.java.pages.bonusReasonPage.editRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class Properties {

    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать причину
     *
     * @param title Причина
     * @return
     */
    public Properties setTitle(String title) {
        try {
            Entity.properties(webDriver).setInputField("title", title);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Название\n" + throwable);
        }
        return this;
    }

    /**
     * указать акцию по умолчанию
     *
     * @param loyaltyPromoValue Акция лояльности
     * @return
     */
    public Properties setDefaultPromoValue(String loyaltyPromoValue) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField("defaultPromo", loyaltyPromoValue);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Акция лояльности\n" + throwable);
        }
        return this;
    }

    /**
     * Добавить Дополнительные доступные акции
     *
     * @param additionalPromos Дополнительные доступные акции
     * @return
     */
    public Properties addAdditionalPromo(List<String> additionalPromos) {
        try {
            Entity.properties(webDriver).setPropertiesOfMultiSuggestTypeField("additionalPromo", additionalPromos);
        } catch (Throwable throwable) {
            throw new Error("Не удалось добавить значение поля Дополнительные доступные акции\n" + throwable);
        }

        return this;
    }

    /**
     * Указать новое значение в поле Дополнительные доступные акции
     *
     * @param additionalPromos новые дополнительные доступные акции
     * @return
     */
    public Properties setAdditionalPromo(List<String> additionalPromos) {
        try {
            Entity.properties(webDriver).clearPropertiesOfMultiSuggestTypeField("", "additionalPromo");
            Entity.properties(webDriver).setPropertiesOfMultiSuggestTypeField("additionalPromo", additionalPromos);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Дополнительные доступные акции лояльности \n" + throwable);
        }
        return this;
    }
}
