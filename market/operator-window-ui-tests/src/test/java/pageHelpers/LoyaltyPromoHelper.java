package ui_tests.src.test.java.pageHelpers;

import Classes.LoyaltyPromo;
import entity.Entity;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class LoyaltyPromoHelper {
    private WebDriver webDriver;

    public LoyaltyPromoHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }

    /**
     * Получить все свойства акции лояльности со страницы
     *
     * @return
     */
    public LoyaltyPromo getAllProperties() {
        LoyaltyPromo loyaltyPromo = new LoyaltyPromo();
        loyaltyPromo
                .setPromoId(Pages.loyaltyPromoPage(webDriver).viewRecordPage().properties().getPromoId())
                .setCode(Pages.loyaltyPromoPage(webDriver).viewRecordPage().properties().getCode())
                .setPromoValue(Pages.loyaltyPromoPage(webDriver).viewRecordPage().properties().getPromoValue())
                .setCondition(Pages.loyaltyPromoPage(webDriver).viewRecordPage().properties().getCondition());
        return loyaltyPromo;
    }

    /**
     * Создать новую акцию лояльности
     */
    public void createNewLoyaltyProperty(LoyaltyPromo loyaltyPromo) {
        //Нажимаем на кнопку создания записи
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        //Указываем код акции
        //указываем id акции
        //указываем номинал акции
        Pages.loyaltyPromoPage(webDriver).modalWindowCreatingRecord()
                .setCode(loyaltyPromo.getCode())
                .setIdLoyaltyPromo(loyaltyPromo.getPromoId())
                .setIdPromoValue(loyaltyPromo.getPromoValue())
                .setCondition(loyaltyPromo.getCondition());
        //Нажимаем на кнопку сохранить
        Pages.loyaltyPromoPage(webDriver).modalWindowCreatingRecord().clickSaveRecordButton();
    }

    /**
     * изменить акцию лоялности
     *
     * @param loyaltyPromo акция лояльности которая должна получиться
     */
    public void editLoyaltyPromo(LoyaltyPromo loyaltyPromo) {
        //Открываем страницу редактирования акции лоялности
        Pages.loyaltyPromoPage(webDriver).viewRecordPage().header().clickEditRecordButton();
        //Изменяем значения полей
        if (loyaltyPromo.getPromoId() != null)
            Pages.loyaltyPromoPage(webDriver).editRecordPage().properties()
                    .setPromoId(loyaltyPromo.getPromoId());
        if (loyaltyPromo.getPromoValue() != null) {
            Pages.loyaltyPromoPage(webDriver).editRecordPage().properties()
                    .setPromoValue(loyaltyPromo.getPromoValue());
        }
        if (loyaltyPromo.getCondition() != null) {
            Pages.loyaltyPromoPage(webDriver).editRecordPage().properties().setCondition(loyaltyPromo.getCondition());
        }
        //Сохранияем акцию
        Pages.loyaltyPromoPage(webDriver).editRecordPage().header().clickSaveRecordButton();

    }
}
