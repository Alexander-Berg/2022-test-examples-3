package ui_tests.src.test.java.pageHelpers;

import Classes.BonusReason;
import entity.Entity;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class BonusReasonHelper {
    private WebDriver webDriver;

    public BonusReasonHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }
    /**
     * Создать новую акцию лояльности
     */
    public void createNewBonusReason(BonusReason bonusReason) {
        // Нажимаем на кнопку создания записи
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        // Указываем код акции
        // Указываем id акции
        // Указываем номинал акции
        Pages.bonusReasonPage(webDriver).modalWindowCreatingRecord()
                .setCode(bonusReason.getCode())
                .setDefaultPromoValue(bonusReason.getDefaultPromoValue())
                .setTitle(bonusReason.getTitle());
        // Нажимаем на кнопку сохранить
        Pages.bonusReasonPage(webDriver).modalWindowCreatingRecord().clickSaveRecordButton();
    }

    /**
     * Получить все свойства акции лояльности со страницы
     *
     * @return
     */
    public BonusReason getAllProperties() {
        BonusReason bonusReason = new BonusReason();
        bonusReason
                .setCode(Pages.bonusReasonPage(webDriver).viewRecordPage().properties().getCode())
                .setTitle(Pages.bonusReasonPage(webDriver).viewRecordPage().properties().getTitle())
                .setDefaultPromoValue(Pages.bonusReasonPage(webDriver).viewRecordPage().properties().getDefaultPromoValue())
                .setAdditionalPromoValue(Pages.bonusReasonPage(webDriver).viewRecordPage().properties().getAdditionalPromoValue());
        return bonusReason;
    }

    /**
     * Изменить Причину начисления бонуса
     * Открываем страницу редактирования, изменяем значения полей и сохраняем данные
     *
     * @param expectedBonusReason
     */
    public void editBonusReason(BonusReason expectedBonusReason) {
        // Нажимаем на кнопку редактирования записи
        Pages.bonusReasonPage(webDriver).viewRecordPage().toolBar().clickEditRecordButton();
        // Указываем новые значения в полях:
        // - Причина
        // - Акция по умолчанию
        // - Дополнительные доступные акции
        if (expectedBonusReason.getTitle() != null) {
            Pages.bonusReasonPage(webDriver).editRecordPage().properties().setTitle(expectedBonusReason.getTitle());
        }
        if (expectedBonusReason.getDefaultPromoValue() != null) {
            Pages.bonusReasonPage(webDriver).editRecordPage().properties().setDefaultPromoValue(expectedBonusReason.getDefaultPromoValue());
        }
        if (expectedBonusReason.getAdditionalPromoValue() != null) {

            Pages.bonusReasonPage(webDriver).editRecordPage().properties().setAdditionalPromo(expectedBonusReason.getAdditionalPromoValue());
        }
        //Сохраняем изменения
        Pages.bonusReasonPage(webDriver).editRecordPage().header().clickSaveRecordButton();

    }
}
