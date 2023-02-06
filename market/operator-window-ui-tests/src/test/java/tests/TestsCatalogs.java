package ui_tests.src.test.java.tests;

import Classes.BonusReason;
import Classes.LoyaltyPromo;
import Classes.SmsTemplate;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Critical;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Функциональность Справочники ЕО
 */
public class TestsCatalogs {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCatalogs.class);

    @InfoTest(descriptionTest = "Проверка создания акции лояльности",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-727")
    @Category({Blocker.class})
    @Test
    public void ocrm727_CheckingCreationLoyaltyPromo() {
        LoyaltyPromo loyaltyPromo = new LoyaltyPromo()
                .setPromoValue(String.valueOf(Tools.other().getRandomNumber(1, 99999)))
                .setPromoId(String.valueOf(Tools.other().getRandomNumber(1, 999999)))
                .setCode(Tools.other().getRandomText())
                .setCondition(Tools.other().getRandomText());

        // Переходим на страницу справочника Акции лояльности
        Pages.navigate(webDriver).openCatalogPage("loyaltyPromo");
        // Создаём акцию лояльности
        PageHelper.loyaltyPromoHelper(webDriver).createNewLoyaltyProperty(loyaltyPromo);

        String idLoyalty = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('loyaltyPromo').withFilters{ eq('code', '" + loyaltyPromo.getCode() + "') }.get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(idLoyalty);

        LoyaltyPromo loyaltyPromoFromPage = PageHelper.loyaltyPromoHelper(webDriver).getAllProperties();
        // Получаем id созданной записи
        String idEntity = Tools.other().getGidFromCurrentPageUrl(webDriver);
        // Удаляем только что созданную запись
        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(idEntity, true);

        Assert.assertEquals("Созданная акция лояльности не равна акции лояльности которую мы ожидали", loyaltyPromo, loyaltyPromoFromPage);
    }

    @InfoTest(descriptionTest = "Проверяем редактирование акции лояльности",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-731")
    @Category({Blocker.class})
    @Test
    public void ocrm731_CheckingEditLoyaltyPromo() {
        LoyaltyPromo actualLoyaltyPromo = new LoyaltyPromo();
        // Создаём акцию которая должна получиться после редактирования
        LoyaltyPromo expectedLoyaltyPromo = new LoyaltyPromo()
                .setPromoValue(String.valueOf(Tools.other().getRandomNumber(1, 999)))
                .setPromoId(String.valueOf(Tools.other().getRandomNumber(1, 999999)))
                .setCode(Tools.other().getRandomText());
        // Создаём рандомную акцию лояльности и получаем ее id
        String idCreatedRecord = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.bcp.create('loyaltyPromo',['condition':'" + Tools.other().getRandomText() + "','code':'" + expectedLoyaltyPromo.getCode() + "','promoId':'" + Tools.other().getRandomNumber(1, 999999) + "','promoValue':'" + Tools.other().getRandomNumber(1, 999999) + "','status':'archived'])");

        // Открываем созданную акцию лояльности
        Pages.navigate(webDriver).openPageByMetaClassAndID(idCreatedRecord);
        // Открываем, изменяем и сохраняем изменения
        PageHelper.loyaltyPromoHelper(webDriver).editLoyaltyPromo(expectedLoyaltyPromo);
        int i = 0;
        do {
            if (i < 3) {
                i++;
                Tools.waitElement(webDriver).waitTime(2000);
                webDriver.navigate().refresh();
                //Получаем свойства изменённой акции лояльности
                actualLoyaltyPromo = PageHelper.loyaltyPromoHelper(webDriver).getAllProperties();
            } else {
                break;
            }
        } while (!expectedLoyaltyPromo.equals(actualLoyaltyPromo));

        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(idCreatedRecord, true);

        Assert.assertEquals("Акция лояльности после редактирования неравна акции лояльности которую мы ожидаем. Ожидаем " + expectedLoyaltyPromo + " а вывели " + actualLoyaltyPromo, expectedLoyaltyPromo, actualLoyaltyPromo);
    }

    @InfoTest(descriptionTest = "Проверка создания Причины начисления бонусов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-737")
    @Category({Blocker.class})
    @Test
    public void ocrm737_CheckingCreationBonusReason() {
        // Эталонная причина начисления бонуса
        BonusReason expectedBonusReason = new BonusReason()
                .setCode(Tools.other().getRandomText())
                .setTitle("ocrm737" + Tools.other().getRandomText())
                .setDefaultPromoValue(
                        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                                "api.db.of('loyaltyPromo')\n" +
                                        ".withFilters{\n" +
                                        "eq('archived',false)\n" +
                                        "}.limit(1).get().title"));


        // Переходим на страницу справочника Причины начисления бонусов
        Pages.navigate(webDriver).openCatalogPage("bonusReason");
        // Создаём причину начисления бонуса
        PageHelper.bonusReasonHelper(webDriver).createNewBonusReason(expectedBonusReason);
        // Получаем gid причины начисления бонуса
        String gidBonusReason = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('bonusReason')\\n.withFilters{ eq('title', '" + expectedBonusReason.getTitle() + "') }\\n.get()");
        // Открываем страницу созданной причины начисления бонуса
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidBonusReason);
        // Получаем с-ва причины начисления бонуса
        BonusReason bonusReasonFromPage = PageHelper.bonusReasonHelper(webDriver).getAllProperties();
        // Удаляем только что созданную запись Причины начисления бонусов
        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(gidBonusReason, true);

        Assert.assertEquals("Созданная причина начисления бонусов не равна причине начисления которую мы ожидали. \n Должна была быть " + expectedBonusReason + " а получилась " + bonusReasonFromPage, expectedBonusReason, bonusReasonFromPage);
    }

    @InfoTest(descriptionTest = "Проверка редактирования Причины начисления бонусов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-738")
    @Category({Blocker.class})
    @Test
    public void ocrm738_CheckingEditBonusReason() {

        BonusReason bonusReasonFromPage = new BonusReason();
        // Эталонная причина начисления бонуса
        BonusReason expectedBonusReason = new BonusReason()
                .setCode("ocrm738")
                .setTitle(Tools.other().getRandomText())
                .setDefaultPromoValue("123 (Акция для автотеста)");

        // Создаём причину начисления бонуса
        String idCreatedRecord = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def bonusReasonCode = api.db.of('bonusReason').withFilters{\n" +
                        "eq('code','"+expectedBonusReason.getCode()+"')}.list()\n" +
                        "if (bonusReasonCode.size()>0){\n" +
                        "  api.bcp.edit(bonusReasonCode[0],['code':'" + expectedBonusReason.getCode() + "'," +
                        "    'title':'" + expectedBonusReason.getTitle() + Tools.other().getRandomText() + "'," +
                        "    'defaultPromo':'loyaltyPromo@45267701','additionalPromo':['loyaltyPromo@45267701'],'status':'archived'])  \n" +
                        "} else{\n" +
                        "api.bcp.create('bonusReason'," +
                        "['code':'" + expectedBonusReason.getCode() + "'," +
                        "'title':'" + expectedBonusReason.getTitle() + Tools.other().getRandomText() + "'," +
                        "'defaultPromo':'loyaltyPromo@45267701','additionalPromo':['loyaltyPromo@45267701'],'status':'archived'])\n" +
                        "}");

        // Открываем страницу с причиной начисления бонуса
        Pages.navigate(webDriver).openPageByMetaClassAndID(idCreatedRecord);
        // Открываем, изменяем и сохраняем изменения
        PageHelper.bonusReasonHelper(webDriver).editBonusReason(expectedBonusReason);
        int i = 0;
        do {
            if (i < 4) {
                i++;
                Tools.waitElement(webDriver).waitTime(2000);
                webDriver.navigate().refresh();

                // Получаем с-ва причины начисления бонуса
                bonusReasonFromPage = PageHelper.bonusReasonHelper(webDriver).getAllProperties();
            } else {
                break;
            }

        } while (!expectedBonusReason.equals(bonusReasonFromPage));

        Assert.assertEquals("Редактированная причина начисления бонусов не равна причине начисления которую мы ожидали. \n Должна была быть " + expectedBonusReason + " а получилась " + bonusReasonFromPage, expectedBonusReason, bonusReasonFromPage);
    }

    @InfoTest(descriptionTest = "Проверка добавления акций лояльности в дополнительные акции лояльности Причины начисления бонусов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-740")
    @Category({Blocker.class})
    @Test
    public void ocrm740_CheckingAdditionalAdditionalPromoValueBonusReason() {

        // Эталонная причина начисления бонуса
        BonusReason expectedBonusReason = new BonusReason()
                .setAdditionalPromoValue(Arrays.asList("300.00 (Актуальная акция)", "123 (Акция для автотеста)"));
        String bonusReasonCode = "ocrm740";

        // Создаём причину начисления бонуса
        String idCreatedRecord = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def bonusReasonCode = api.db.of('bonusReason').withFilters{\n" +
                        "eq('code','" + bonusReasonCode + "')}.list()\n" +
                        "if (bonusReasonCode.size()>0){\n" +
                        "  api.bcp.edit(bonusReasonCode[0],['status' : 'active'," +
                        "'title':'ocrm740_CheckingAdditionalAdditionalPromoValueBonusReason " + Tools.other().getRandomText() + "'," +
                        "'defaultPromo':'loyaltyPromo@122600188'," +
                        "'additionalPromo':['loyaltyPromo@122600188']])\n" +
                        "  return bonusReasonCode[0]\n" +
                        "} else{\n" +
                "api.bcp.create('bonusReason'," +
                        "['code':'" + bonusReasonCode + "'," +
                        "'title':'ocrm740_CheckingAdditionalAdditionalPromoValueBonusReason " + Tools.other().getRandomText() + "'," +
                        "'defaultPromo':'loyaltyPromo@122600188'," +
                        "'additionalPromo':['loyaltyPromo@122600188']," +
                        "'status':'archived'])\n" +
                        "}");

        // Открываем страницу с причиной начисления бонуса для редактирования
        Pages.navigate(webDriver).openPageByMetaClassAndID(idCreatedRecord + "/edit");

//        // Открываем Причину начисления бонусов для редактирования
//        Pages.bonusReasonPage(webDriver).viewRecordPage().toolBar().clickEditRecordButton();
        // Добавляем акции лояльности в поле Дополнительные акции лояльности
        Pages.bonusReasonPage(webDriver).editRecordPage().properties().addAdditionalPromo(Arrays.asList("300.00 (Актуальная акция)"));
        // Сохраняем изменения
        Pages.bonusReasonPage(webDriver).editRecordPage().header().clickSaveRecordButton();

        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(idCreatedRecord, true);

        // Получаем значение из поля причина начисления бонуса
        BonusReason bonusReasonFromPage = new BonusReason().setAdditionalPromoValue(Pages.bonusReasonPage(webDriver).viewRecordPage().properties().getAdditionalPromoValue());

        Assert.assertEquals("Добавление дополнительной акции лояльности не работает. \n Должна была быть " + expectedBonusReason + " а получилась " + bonusReasonFromPage, expectedBonusReason, bonusReasonFromPage);
    }

    @InfoTest(descriptionTest = "Проверка архивирования Причины начисления бонусов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-741")
    @Category({Critical.class})
    @Test
    public void ocrm741_CheckingArchivedBonusReason() {
        boolean b = false;
        // Создаём причину начисления бонуса
        String idCreatedRecord = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.bcp.create('bonusReason',['code':'" + Tools.other().getRandomText() + "','title':'ocrm741_CheckingAdditionalAdditionalPromoValueBonusReason " + Tools.other().getRandomText() + "','defaultPromo':'loyaltyPromo@122600188','additionalPromo':['loyaltyPromo@122600188']]);");

        // Открываем страницу с причиной начисления бонуса
        Pages.navigate(webDriver).openPageByMetaClassAndID(idCreatedRecord);
        Pages.bonusReasonPage(webDriver).viewRecordPage().toolBar().clickArchiveRecordButton();

        int i = 0;
        do {
            if (i < 6) {
                Tools.waitElement(webDriver).waitTime(2000);
                b = Boolean.parseBoolean(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.get('" + idCreatedRecord + "').archived"));
                i++;
            } else {
                break;
            }
        } while (!b);

        Assert.assertTrue("Не удалось архивировать Причину начисления бонусов", b);
    }

    @InfoTest(descriptionTest = "Проверка создания шаблона сообщения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-742")
    @Category({Blocker.class})
    @Test
    public void ocrm742_CheckingCreateSMSTemplate() {
        //Создаём шаблон который должен получиться
        SmsTemplate expectedSmsTemplate = new SmsTemplate()
                .setCode(Tools.other().getRandomText())
                .setSender("Ya.Market")
                .setText(Tools.other().getRandomText())
                .setTitle(Tools.other().getRandomText());
        // Создаём переменную для шаблона полученного со страницы
        SmsTemplate smsTemplateFromPage = new SmsTemplate();
        // Переходим на страницу справочника шаблонов смс
        Pages.navigate(webDriver).openPageByMetaClassAndID("catalog@smsTemplate");
        // Создаём шаблон сообщений
        PageHelper.smsTemplatePageHelper(webDriver).createSmsTemplate(expectedSmsTemplate);
        // Получаем gid созданной записи
        String gidRecord = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('smsTemplate').withFilters{ eq('code', '" + expectedSmsTemplate.getCode() + "') }.get()");
        int i = 0;
        do {
            if (i < 3) {
                Tools.waitElement(webDriver).waitTime(2000);
                // Открываем только что созданный шаблон смс
                Pages.navigate(webDriver).openPageByMetaClassAndID(gidRecord);
                // Получаем свойства шаблона
                smsTemplateFromPage = PageHelper.smsTemplatePageHelper(webDriver).getAllPropertiesFromPage();
                i++;
            } else {
                break;
            }
        } while (!expectedSmsTemplate.equals(smsTemplateFromPage));
        // Архивируем запись
        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(gidRecord, true);

        Assert.assertEquals("Созданный шаблон сообщения не равен шаблону который мы ожидаем", expectedSmsTemplate, smsTemplateFromPage);
    }

    @InfoTest(descriptionTest = "Проверка редактирования шаблона сообщения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-756")
    @Category({Blocker.class})
    @Test
    public void ocrm756_CheckingEditSMSTemplate() {
        // Создаём шаблон который должен получиться
        SmsTemplate expectedSmsTemplate = new SmsTemplate()
                .setCode(Tools.other().getRandomText())
                .setSender("Ya.Market")
                .setText(Tools.other().getRandomText())
                .setTitle(Tools.other().getRandomText());
        // Создаём переменную для шаблона полученного со страницы
        SmsTemplate smsTemplateFromPage = new SmsTemplate();
        // Создаём шаблон СМС
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.bcp.create('smsTemplate',['code':'" + expectedSmsTemplate.getCode() + "','title':'" + expectedSmsTemplate.getTitle() + Tools.other().getRandomText() + "','sender':'smsSender@94245501','text':'" + expectedSmsTemplate.getText() + Tools.other().getRandomText() + "','status' : 'archived'])");

        // Переходим на страницу  шаблона смс
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
        // Изменить шаблон смс
        PageHelper.smsTemplatePageHelper(webDriver).editRecordSmsTemplate(expectedSmsTemplate);

        int i = 0;
        do {
            if (i < 8) {
                Tools.waitElement(webDriver).waitTime(1000);
                // Открываем только что созданный шаблон смс
                Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
                // Получаем свойства шаблона
                smsTemplateFromPage = PageHelper.smsTemplatePageHelper(webDriver).getAllPropertiesFromPage();
                i++;
            } else {
                break;
            }
        } while (!expectedSmsTemplate.equals(smsTemplateFromPage));

        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(gid, true);

        Assert.assertEquals("Созданный шаблон сообщения не равен шаблону который мы ожидаем", expectedSmsTemplate, smsTemplateFromPage);
    }

}
