package ui_tests.src.test.java.pageHelpers;

import Classes.SmsTemplate;
import entity.Entity;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class SmsTemplatePageHelper {
    private WebDriver webDriver;

    public SmsTemplatePageHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }
    /**
     * Создать новый шаблон сообщения.
     * Нажиаем на Добавить
     * Заполняем поля шаблона сообщения
     * Нажимаем на сохранение записи
     *
     * @param smsTemplate
     */
    public void createSmsTemplate(SmsTemplate smsTemplate) {
        //Нажимаем на добавление нового шаблона СМС
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        //Заполняем форму создания шаблона
        Pages.smsTemplatePage(webDriver).modalWindowCreatingRecord()
                .setTitle(smsTemplate.getTitle())
                .setCode(smsTemplate.getCode())
                .setSender(smsTemplate.getSender())
                .setText(smsTemplate.getText());
        //Нажимаем на кнопку сохранения записи
        Pages.smsTemplatePage(webDriver).modalWindowCreatingRecord().clickSaveRecordButton();
    }

    /**
     * получиь все свойства со страницы шаблона смс
     *
     * @return
     */
    public SmsTemplate getAllPropertiesFromPage() {
        SmsTemplate smsTemplate = new SmsTemplate();
        smsTemplate
                .setTitle(Pages.smsTemplatePage(webDriver).viewRecordPage().properties().getTitle())
                .setCode(Pages.smsTemplatePage(webDriver).viewRecordPage().properties().getCode())
                .setSender(Pages.smsTemplatePage(webDriver).viewRecordPage().properties().getSender())
                .setText(Pages.smsTemplatePage(webDriver).viewRecordPage().properties().getText());
        return smsTemplate;
    }

    /**
     * Открыть страницу редактирования, изменить данные и сохранить изменпния
     *
     * @param smsTemplate
     */
    public void editRecordSmsTemplate(SmsTemplate smsTemplate) {
        Pages.smsTemplatePage(webDriver).viewRecordPage().header().clickEditPageButton();
        if (smsTemplate.getText() != null) {
            Pages.smsTemplatePage(webDriver).editRecordPage().properties().setTitle(smsTemplate.getTitle());
        }
        if (smsTemplate.getSender() != null) {
            Pages.smsTemplatePage(webDriver).editRecordPage().properties().setSender(smsTemplate.getSender());
        }
        if (smsTemplate.getText() != null) {
            Pages.smsTemplatePage(webDriver).editRecordPage().properties().setText(smsTemplate.getText());
        }

        Pages.smsTemplatePage(webDriver).editRecordPage().header().clickSaveRecordButton();
    }
}
