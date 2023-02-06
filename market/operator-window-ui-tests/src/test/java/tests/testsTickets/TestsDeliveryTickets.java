package ui_tests.src.test.java.tests.testsTickets;

import Classes.Comment;
import Classes.Email;
import entity.Entity;
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
import unit.Config;

import java.util.GregorianCalendar;
import java.util.List;

public class TestsDeliveryTickets {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsDeliveryTickets.class);

    @InfoTest(
            descriptionTest = "Брать СД из заказа при создании обращений логистики Покупок (в ФОС указана СД)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-950",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-771"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm950_GetDeliveryServiceFromOrderWhenDSPresentInForm() {
        String subject = "Автотест ocrm-950 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");

        String expectedDeliveryService = "DPD";
        String deliveryServiceFromPage;

        // Включить в очереди "Логистическая поддержка Гран Покупки > Общие"
        // чек-бокс "Брать службу доставки из заказа, если она не введена"
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.bcp.edit('service@77989902', ['fillDeliveryServiceFromOrder' : true])", false);

        // Подождать, пока изменения применятся
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(1000);
            String checkBoxValue = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@77989902').fillDeliveryServiceFromOrder\n" +
                            "return obj");
            if (checkBoxValue.equals("true")) {
                break;
            } else if (i == 9) {
                throw new Error("Не удалось проставить в очереди чек-бокс " +
                        "'Брать службу доставки из заказа, если она не введена'");
            }
        }

        // Создать письмо, в котором указана СД = DPD и заказ 7364479 с СД = СДЭК
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("logisticSupport")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.\n" +
                        "Номер заказа: 7364479 \n" +
                        "Служба доставки: DPD ")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        //Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "logisticSupport");

        //Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Получить СД из обращения
        deliveryServiceFromPage = Pages.ticketPage(webDriver).properties().getDeliveryService();

        // Сверить значения
        Assert.assertEquals("Служба доставки определилась неверно",
                expectedDeliveryService, deliveryServiceFromPage);
    }

    @InfoTest(
            descriptionTest = "Брать СД из заказа при создании обращений логистики Покупок (в ФОС не указана СД)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-952",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-771"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm952_GetDeliveryServiceFromOrderWhenDSNotPresentInForm() {
        String subject = "Автотест ocrm-952 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");
        String expectedDeliveryService = "СДЭК";
        String deliveryServiceFromPage;

        // Включить в очереди "Логистическая поддержка Гран Покупки > Общие"
        // чек-бокс "Брать службу доставки из заказа, если она не введена"
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.bcp.edit('service@77989902', ['fillDeliveryServiceFromOrder' : true])", false);

        // Подождать, пока изменения применятся
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(1000);
            String checkBoxValue = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@77989902').fillDeliveryServiceFromOrder\n" +
                            "return obj");
            if (checkBoxValue.equals("true")) {
                break;
            } else if (i == 9) {
                throw new Error("Не удалось проставить в очереди чек-бокс " +
                        "'Брать службу доставки из заказа, если она не введена'");
            }
        }

        // Создать письмо, в котором не указана СД и указан заказ 7364479 с СД = СДЭК
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("logisticSupport")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.\n" +
                        "Номер заказа: 7364479 \n" +
                        "Служба доставки: ")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        //Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "logisticSupport");

        //Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Получить СД из обращения
        deliveryServiceFromPage = Pages.ticketPage(webDriver).properties().getDeliveryService();

        // Сверить значения
        Assert.assertEquals("Служба доставки определилась неверно",
                expectedDeliveryService, deliveryServiceFromPage);
    }

    @InfoTest(
            descriptionTest = "Брать СД из заказа при создании обращений логистики Покупок (в ФОС указана СД, настройка в очереди выключена)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-953",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-773"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm953_DontGetDeliveryServiceFromOrderWhenDSPresentInForm() {
        String subject = "Автотест ocrm-953 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm") +
                " Последний день доставки";
        String expectedDeliveryService = "DPD";
        String deliveryServiceFromPage;

        // Выключить в очереди "Логистическая поддержка Гран Покупки > Приоритетные"
        // чек-бокс "Брать службу доставки из заказа, если она не введена"
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.bcp.edit('service@77989903', ['fillDeliveryServiceFromOrder' : false])", false);

        // Подождать, пока изменения применятся
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(1000);
            String checkBoxValue = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@77989903').fillDeliveryServiceFromOrder\n" +
                            "return obj");
            if (checkBoxValue.equals("false")) {
                break;
            } else if (i == 9) {
                throw new Error("Не удалось убрать в очереди чек-бокс " +
                        "'Брать службу доставки из заказа, если она не введена'");
            }
        }

        // Создать письмо, в котором указана СД = DPD и заказ 7364479 с СД = СДЭК
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("logisticSupport")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.\n" +
                        "Номер заказа: 7364479 \n" +
                        "Служба доставки: DPD ")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        //Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "logisticSupport");

        //Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Получить СД из обращения
        deliveryServiceFromPage = Pages.ticketPage(webDriver).properties().getDeliveryService();

        // Сверить значения
        Assert.assertEquals("Служба доставки определилась неверно",
                expectedDeliveryService, deliveryServiceFromPage);
    }

    @InfoTest(
            descriptionTest = "Брать СД из заказа при создании обращений логистики Покупок (в ФОС не указана СД, настройка в очереди выключена)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-954",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-772"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm954_DontGetDeliveryServiceFromOrderWhenDSNotPresentInForm() {
        String subject = "Автотест ocrm-954 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm") +
                " Последний день доставки";

        // Выключить в очереди "Логистическая поддержка Гран Покупки > Приоритетные"
        // чек-бокс "Брать службу доставки из заказа, если она не введена"
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.bcp.edit('service@77989903', ['fillDeliveryServiceFromOrder' : false])", false);

        // Подождать, пока изменения применятся
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(1000);
            String checkBoxValue = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@77989903').fillDeliveryServiceFromOrder\n" +
                            "return obj\n");
            if (checkBoxValue.equals("false")) {
                break;
            } else if (i == 9) {
                throw new Error("Не удалось убрать в очереди чек-бокс " +
                        "'Брать службу доставки из заказа, если она не введена'");
            }
        }

        // Создать письмо, в котором не указана СД и указан заказ 7364479 с СД = СДЭК
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("logisticSupport")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.\n" +
                        "Номер заказа: 7364479 \n" +
                        "Служба доставки: ")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        // Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "logisticSupport");

        // Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Проверить, что в обращении не проставилась СД
        Assert.assertTrue("Служба доставки определилась, хотя поле должно было остаться пустым",
                Entity.properties(webDriver).checkEmptyValueField("deliveryService"));

    }

    @InfoTest(descriptionTest = "Проверка работы правил автоматизации логистической поддержки",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-693",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-963")
    @Category({Blocker.class})
    @Test
    public void ocrm963_AutomationRulesOfLogisticSupport() {
        // Создаём письмо которое отправим на почту логистики
        Email email = new Email()
                .setTo("logisticSupport")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setSubject("ocrm963_AutomationRulesOfLogisticSupport() " + Tools.other().getRandomText())
                .setText("Категория обращения:\n" +
                        "Нарушены сроки доставки");
        // Создаём эталонный комментарий
        Comment expectedComment = new Comment()
                .setType("internal")
                .setText("Сработало правило " + Config.getProjectURL() + "/entity/logisticSupportRules@83905002\n" +
                        "\n" +
                        "\"Правило на создание обращение с совпадением по всем категориям обращения\"")
                .setNameAndEmail("Корбен Даллас (robot-lilucrm-prod)");
        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "logisticSupport");

        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(email.getSubject());
        // Получаем список всех сообщений
        List<Comment> comments = PageHelper.ticketPageHelper(webDriver).getAllComments();


        Assert.assertEquals("В обращение не добавилось сообщение из автоматизации, ссылка на обращение из теста - "
                + webDriver.getCurrentUrl(), expectedComment, comments.get(comments.size() - 1));
    }

    @InfoTest(
            descriptionTest = "Создание обращений в очереди 'Логистическая поддержка Покупок > Запросы B2B Забор'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1006",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-988"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1006_checkCreateTicketInBeruLogisticSupportB2B() {
        String targetQueue = "Логистическая поддержка Покупок > Запросы B2B Забор";

        Email email = new Email();
        email.setSubject("Запрос от b2b: Нужно решить вопрос по забору дропшип-партнера - 14456483 " + (new GregorianCalendar()).getTime().toString() + " " + Tools.other().getRandomText())
                .setText("Рандомный текст в теле письма " + Tools.other().getRandomText())
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("logisticSupport");
        // Создаём обращение через скрипт на основе письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "logisticSupport");

        // Находим обращение через скрипт и открываем его
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(email.getSubject());

        // В открывшемся обращении переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Со страницы обращения получаем свойства, тему, сообщения
        String ticketService = Pages.ticketPage(webDriver).properties().getService();

        Assert.assertEquals("Обращение в теме которого есть 'Запрос от b2b' не попал в очередь", targetQueue, ticketService);
    }
}
