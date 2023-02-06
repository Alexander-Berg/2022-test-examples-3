package ui_tests.src.test.java.pages.bonusReasonPage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

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
     * получить  Причину
     *
     * @return
     */
    public String getTitle() {
        try {
            return Entity.properties(webDriver).getValueField("title");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить заначение поля Причина\n" + throwable);
        }
    }

    /**
     * получить Акцию по умолчанию
     *
     * @return
     */
    public String getDefaultPromoValue() {
        try {
            return Entity.properties(webDriver).getValueField("defaultPromo");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Акция по умолчанию\n" + throwable);
        }
    }

    /**
     * Получить значение из поля "Дополнительные доступные акции"
     *
     * @return
     */
    public List<String> getAdditionalPromoValue() {
        try {
            return Entity.properties(webDriver).getValuesField("additionalPromo");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Дополнительные доступные акции\n" + throwable);
        }
    }

}
