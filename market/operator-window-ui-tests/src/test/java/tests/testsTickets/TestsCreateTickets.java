package ui_tests.src.test.java.tests.testsTickets;

import Classes.Comment;
import Classes.Email;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Critical;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Orders;

import java.io.File;
import java.util.*;

public class TestsCreateTickets {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCreateTickets.class);

    @InfoTest(linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-433",
            descriptionTest = "Проверка создания обращения через почту")
    @Category({Blocker.class})
    @Test
    public void ocrm433_checkCreateTicket() {

        Email newEmail = new Email();
        Ticket ticket = new Ticket();
        Ticket ticketFromPage = new Ticket();
        Properties properties = new Properties();

        // Создаём исходящее письмо
        newEmail
                .setSubject("ocrm433_checkCreateTicket Проверка создания обращений " + Tools.other().getRandomText() + " от " + (new GregorianCalendar()).getTime().toString())
                .setTo("beru")
                .setText("простой текст сообщения для проверки создания обращения")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        properties
                .setService("Покупки > Общие вопросы")
                .setTeam("1ая линия поддержки")
                .setContactEmail(newEmail.getFromAlias())
                .setPriority("50 Нормальный");

        // Создаём тикет, который должен получиться
        ticket.setSubject(newEmail.getSubject())
                .setComments(Collections.singletonList((new Comment()).setText(newEmail.getText()).setNameAndEmail(newEmail.getFromAlias()).setType("public")))
                .setProperties(properties);

        // Отправляем созданное исходящее письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        // Открываем обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(newEmail.getSubject());

        // В открывшемся обращении переходим на вкладку Сообщения
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Со страницы обращения получаем свойства, тему, сообщения
        ticketFromPage.setProperties(PageHelper.ticketPageHelper(webDriver).getAllProperties())
                .setSubject(Pages.ticketPage(webDriver).header().getSubject())
                .setComments(PageHelper.ticketPageHelper(webDriver).getAllComments());

        Assert.assertEquals(ticket, ticketFromPage);
    }

    @Ignore("выпилили ЯДО")
    @InfoTest(linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-437",
            descriptionTest = "Проверка создания тикетов в очередях \"Я.До\"")
    @Category({Blocker.class})
    @Test
    public void ocrm437_checkCreateTicketInDeliveryLogisticSupportPriority() {

        String emailAlias = Tools.other().getRandomText() + "@yandex.ru";
        String emailAlias2 = Tools.other().getRandomText() + Tools.other().getRandomText() + "@gnom.land";

        List<Email> emails = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        emails.add(new Email().setSubject("2 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "VIP\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("3 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Срочно\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("4 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Канал поступления соцсети\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("5 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Запрос от рекламаций\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n" + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("6 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999) + " Задержка доставки подарка и 3-е обращение клиента")
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Запрос\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("7 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999) + " Последний день доставки")
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Запрос\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("9 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Ваше имя:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Запрос\n" + "\n" + "Email, на который будет отправлен ответ:\n" + emailAlias2 + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("10 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Имя клиента:\n" + Tools.other().getRandomText() + "\n" + "\n" + "Тип клиента:\n" + "Обычный клиент\n" + "\n" + "e-mail клиента для ответа или отправки кассового чека:\n" + emailAlias + "\n")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("11 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Текст обращения который должен попасть в обычную очередь")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));
        emails.add(new Email().setSubject("12 Дата " + (new GregorianCalendar()).getTime().toString() + " рандомное обращение под номером " + Tools.other().getRandomNumber(100, 9999))
                .setText("Текст обращения который должен попасть в Срочную очередь\n" + "Очередь:\n Срочные запросы")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("yaDeliveryLogisticSupport"));

        for (Email email : emails) {
            // Tools.email().sendAnEmail(email);
            PageHelper.otherHelper(webDriver).createTicketFromMail(email, "yaDeliveryLogisticSupport");
        }

        for (Email email : emails) {
            // Получаю очередь у созданного обращения
            String ticketService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    // Ищем бренд Логистическая поддержка Я.До
                    "def brand = api.db.of('brand').withFilters{\n" +
                            "eq('title','Логистическая поддержка Я.До')\n" +
                            "}.get()\n" +
                            // Ищем обращение с нашим брендом и получаем у обращения очередь
                            "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                            "eq('title','" + email.getSubject() + "')\n" +
                            "  eq('brand',brand)\n" +
                            "}.get().service.title");

            // Определяем очередь, куда должно попасть и куда попало
            if (email.getText().toLowerCase().contains("vip") || email.getText().toLowerCase().contains("вип")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение с типом клиента ВИП не попало в приоритетную очередь." + " тема письма: " + email.getSubject() + "\n");
                }
            } else if (email.getText().toLowerCase().contains("срочно")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение в теле которого есть 'Срочно' не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getText().toLowerCase().contains("канал поступления соцсети")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение в теле которого есть 'Канал поступления соцсети' не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getText().toLowerCase().contains("запрос от рекламаций")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение в теле которого есть 'Запрос от рекламаций' не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getSubject().toLowerCase().contains("задержка доставки подарка и 3-е обращение клиента")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение в теме которого есть 'Задержка доставка подарка и 3-е обращение клиента' не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getSubject().toLowerCase().contains("последний день доставки")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение в теме которого есть 'Последний день доставки' не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getFromAlias().toLowerCase().contains("@gnom.land") || email.getFromAlias().toLowerCase().equals("dreamskin@yandex.ru") || email.getFromAlias().toLowerCase().equals("agentne007@yandex.ru") || email.getFromAlias().toLowerCase().equals("statnikroman@gmail.com")) {
                if (!ticketService.equals("Логистическая поддержка Т Я.До > Приоритетные")) {
                    messages.add("Обращение у которого отправитель от ящика '*@gnom.land', 'dreamskin@yandex.ru', 'agentne007@yandex.ru' или `statnikroman@gmail.com` не попало в приоритетную очередь." + " тема письма: " + email.getSubject());
                }
            } else if (email.getText().toLowerCase().contains("очередь: срочные запросы")) {
                if (!ticketService.equals("Логистическая поддержка ТК Я.До > Срочные запросы")) {
                    messages.add("Обращение в теле которого есть 'Очередь: Срочные запросы' отправленное на почту обычной логистики не попало в очередь Срочные запросы" + " тема письма: " + email.getSubject());
                }
            } else if (!ticketService.equals("Логистическая поддержка Т Я.До > Общие")) {
                messages.add("Обращение для обычной очереди не попало в обычную очередь" + " тема письма: " + email.getSubject());
            }
        }

        if (messages.size() > 0) {
            Assert.fail(messages.toString());
        } else {
            Assert.assertTrue(true);
        }
    }

    @InfoTest(descriptionTest = "Создание обращения из письма только с вложением",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-444")
    @Category({Blocker.class})
    @Test
    public void ocrm444_CheckingCreationOfTicketWithAnAttachmentOnly() {
        Email email = new Email();
        File newFile = new File(Tools.file().createFile(Tools.other().getRandomText() + ".txt", Tools.other().getRandomText()));
        File newFile2 = new File(Tools.file().createFile(Tools.other().getRandomText() + ".txt", Tools.other().getRandomText()));
        Comment comment = new Comment();

        // Создаём письмо для отправки
        email.setFile(Arrays.asList(newFile, newFile2))
                .setTo("beru")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setSubject("ocrm444 Проверка создания обращения с вложением" + (new GregorianCalendar()).getTime().toString());

        // Создаём тикет, который должен получиться
        comment.setNameAndEmail(email.getFromAlias())
                .setText("--не содержит текста--")
                .setFiles(Arrays.asList(newFile.getName(), newFile2.getName()))
                .setType("public");

        // Отправляем письмо
        Tools.email().sendAnEmail(email);

        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(email.getSubject());
        // Со страницы обращения получаем список комментариев
        List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();

        Assert.assertTrue(commentsFromPage.contains(comment));

    }

    @InfoTest(descriptionTest = "Проверка создания ручного обращения в очередь Исходящая телефония",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-553")
    @Category({Blocker.class})
    @Test
    @Ignore
    public void ocrm553_CreateTicketInServiceBeruOutgoingCall() {
        Ticket actualTicket = new Ticket();
        Ticket ticketFromPage = new Ticket();
        String statusFromPage;
        List<String> acceptableStatuses = new ArrayList<String>();
        acceptableStatuses.add("Отложен");
        acceptableStatuses.add("Новый");

        // Создаём эталонный тикет
        actualTicket
                .setProperties(new Properties()
                        .setContactPhoneNumber("4999384362")
                        .setService("Покупки > Исходящие звонки"))
                .setSubject(Tools.other().getRandomText() + new GregorianCalendar().getTime().toString());

        // Открыть страницу очереди Покупки Общие вопросы
        Pages.navigate(webDriver).openLVAllTickets();
        // Создаём обращение через интерфейс
        PageHelper.tableHelper(webDriver).createNewTicket("Покупки - исходящий звонок", actualTicket);

        // Получаем свойства со страницы просмотра
        Properties propertiesFromPage = new Properties();
        propertiesFromPage
                .setContactPhoneNumber(Pages.ticketPage(webDriver).properties().getContactPhoneNumber())
                .setService(Pages.ticketPage(webDriver).properties().getService());
        // Получаем данные по созданному тикету
        ticketFromPage
                .setProperties(propertiesFromPage)
                .setSubject(Pages.ticketPage(webDriver).header().getSubject());
        statusFromPage = Pages.ticketPage(webDriver).properties().getStatus();


        Assert.assertTrue(
                Tools.differ().format("Не все поля, заполненные на странице создания тикета, совпадают со значениями на странице просмотра тикета", actualTicket, ticketFromPage) +
                        Tools.differ().format("", acceptableStatuses, statusFromPage),
                actualTicket.equals(ticketFromPage) && acceptableStatuses.contains(statusFromPage));
    }

    @InfoTest(descriptionTest = "Создание обращения вручную в очередь Покупки - Общие вопросы",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-554")
    @Category({Blocker.class})
    @Test
    public void ocrm554_CreateTicketInServiceBeruQuestion() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def obj = api.db.get('service@74019802') \napi.bcp.edit(obj, ['serviceOnResolved' : 'service@30013907'])");

        Ticket actualTicket = new Ticket();
        Ticket ticketFromPage = new Ticket();
        Properties properties = new Properties()
                .setCategory(Collections.singletonList("test sanity"))
                .setService("Покупки > Общие вопросы")
                .setContactEmail(Tools.other().getRandomText() + Tools.other().getRandomText() + "@yandex.ru")
                .setStatus("Решен");
        // Создаём эталонный тикет
        actualTicket.setProperties(properties);
        actualTicket.setSubject(Tools.other().getRandomText() + new GregorianCalendar().getTime().toString());
        Comment comment = new Comment();
        comment.setType("public")
                .setText("Текст внешнего комментария")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");
        actualTicket.setComments(Collections.singletonList(comment));

        // Открыть страницу очереди Покупки Общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();

        // Создать обращение метакласса "Покупки - исходящее"
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        Entity.entityTable(webDriver).toolBar().selectEntityOnSelectMenu("Покупки - исходящее");

        // Заполнить свойства обращения
        Pages.ticketPage(webDriver).createTicketPage().properties()
                .setClientEmail(actualTicket.getProperties().getContactEmail())
                .setCategories(actualTicket.getProperties().getCategory())
                .setTitle(actualTicket.getSubject());
        PageHelper.createTicketPageHelper(webDriver).setComment(actualTicket.getComments().get(0));
        // Сохранить
        Pages.ticketPage(webDriver).createTicketPage().header().clickButtonSaveForm("Добавить");

        // Получаем свойства со страницы просмотра
        Properties propertiesFromPage = new Properties();
        propertiesFromPage
                .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                .setService(Pages.ticketPage(webDriver).properties().getService())
                .setCategory(Pages.ticketPage(webDriver).properties().getCategory())
                .setStatus(Pages.ticketPage(webDriver).properties().getStatus());
        // Получаем данные по созданному тикету
        ticketFromPage
                .setProperties(propertiesFromPage)
                .setComments(PageHelper.ticketPageHelper(webDriver).getAllComments())
                .setSubject(Pages.ticketPage(webDriver).header().getSubject());

        Assert.assertEquals("Не все заполненные поля на странице создания тикета, совпадают с значениями на странице просмотра тикета", actualTicket, ticketFromPage);
    }

    @Ignore("выпилили ЯДО")
    @InfoTest(descriptionTest = "Проверка ручного создания тикета Логистическая поддержка Покупки",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-587")
    @Category({Blocker.class})
    @Test
    public void ocrm587_CreateTicketInServiceBeruLogisticSupportTkQuestion() {

        Ticket actualTicket = new Ticket();
        Ticket ticketFromPage = new Ticket();
        // Создаём эталонный тикет
        actualTicket.setProperties((new Properties()
                .setService("Логистическая поддержка ТК Покупки > Общие")
                .setContactEmail(Tools.other().getRandomText() + "@yandex.ru")
                .setStatus("Решен")
        ));
        Comment comment = new Comment();
        comment.setType("public")
                .setText("Текст внешнего комментария")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");
        actualTicket.setComments(Collections.singletonList(comment));
        actualTicket.setSubject("Ручное обращение логистики ТК Покупки от " + Tools.other().getRandomText() + new GregorianCalendar().getTime().toString());

        // Открыть страницу очереди Покупки Общие вопросы
        Pages.navigate(webDriver).openLVAllTickets();
        // Нажимаем на кнопку создания тикета
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        // Выбираем тип создаваемого тикета
        Entity.entityTable(webDriver).toolBar().selectEntityOnSelectMenu("Логистическая поддержка Покупок");
        // Заполняем поля на странице создания тикета
        Pages.ticketPage(webDriver).createTicketPage().properties()
                .setClientEmail(actualTicket.getProperties().getContactEmail())
                .setTitle(actualTicket.getSubject())
                .setService(Collections.singletonList(actualTicket.getProperties().getService()));
        PageHelper.ticketPageHelper(webDriver).openOutputMailTabOnMailTab();
        Pages.ticketPage(webDriver).createTicketPage().commentsCreation().setTextComment(actualTicket.getComments().get(0).getText());
        // Нажимаем на кнопку сохранения тикета
        Pages.ticketPage(webDriver).createTicketPage().header().clickButtonSaveForm("Добавить");

        // Получаем свойства со страницы просмотра
        Properties propertiesFromPage = new Properties();
        propertiesFromPage
                .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                .setService(Pages.ticketPage(webDriver).properties().getService())
                .setStatus(Pages.ticketPage(webDriver).properties().getStatus());
        // Получаем данные по созданному тикету
        ticketFromPage
                .setProperties(propertiesFromPage)
                .setComments(PageHelper.ticketPageHelper(webDriver).getAllComments())
                .setSubject(Pages.ticketPage(webDriver).header().getSubject());

        Assert.assertEquals("Не все заполненные поля на странице создания тикета, совпадают с значениями на странице просмотра тикета", actualTicket, ticketFromPage);
    }

    @Ignore("выпилили ЯДО")
    @InfoTest(descriptionTest = "Проверка ручного создания тикета Логистическая поддержка Т Я.До",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-588")
    @Category({Blocker.class})
    @Test
    public void ocrm588_CreateTicketInServiceYandexDeliveryLogisticSupportQuestion() {

        Ticket actualTicket = new Ticket();
        Ticket ticketFromPage = new Ticket();
        // Создаём эталонный тикет
        actualTicket.setProperties((new Properties()
                .setService("Логистическая поддержка Т Я.До > Общие")
                .setContactEmail(Tools.other().getRandomText() + Tools.other().getRandomText() + "@yandex.ru")
                .setStatus("Решен")
        ));
        actualTicket.setSubject("Ручное обращение логистики Я.До от " + Tools.other().getRandomText() + new GregorianCalendar().getTime().toString());
        Comment comment = new Comment();
        comment.setType("public")
                .setText("Текст внешнего комментария")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");
        actualTicket.setComments(Collections.singletonList(comment));

        // Открыть страницу очереди Покупки Общие вопросы
        Pages.navigate(webDriver).openLVAllTickets();

        // PageHelper.entityHelper().createNewTicket("Логистическая поддержка Я.До",actualTicket);
        // Нажимаем на кнопку создания тикета
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        // Выбираем тип создаваемого тикета
        Entity.entityTable(webDriver).toolBar().selectEntityOnSelectMenu("Логистическая поддержка Я.До");
        // Заполняем поля на странице создания тикета
        Pages.ticketPage(webDriver).createTicketPage().properties()
                .setTitle(actualTicket.getSubject())
                .setClientEmail(actualTicket.getProperties().getContactEmail())
                .setService(Collections.singletonList(actualTicket.getProperties().getService()));
        PageHelper.ticketPageHelper(webDriver).openOutputMailTabOnMailTab();
        Pages.ticketPage(webDriver).createTicketPage().commentsCreation().setTextComment(actualTicket.getComments().get(0).getText());
        // Нажимаем на кнопку сохранения тикета
        Pages.ticketPage(webDriver).createTicketPage().header().clickButtonSaveForm("Добавить");

        // Получаем свойства со страницы просмотра
        Properties propertiesFromPage = new Properties();
        propertiesFromPage
                .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                .setService(Pages.ticketPage(webDriver).properties().getService())
                .setStatus(Pages.ticketPage(webDriver).properties().getStatus());
        // Получаем данные по созданному тикету
        ticketFromPage
                .setProperties(propertiesFromPage)
                .setComments(PageHelper.ticketPageHelper(webDriver).getAllComments())
                .setSubject(Pages.ticketPage(webDriver).header().getSubject());

        Assert.assertEquals("Не все заполненные поля на странице создания тикета, совпадают с значениями на странице просмотра тикета", actualTicket, ticketFromPage);
    }

    @InfoTest(descriptionTest = "Разбор письма не падает, если в письме пришел заголовок x-market-shopid, " +
            "но не удалось определить партнера",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1060",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1116")
    @Category({Blocker.class})
    @Test
    public void ocrm1116_EmailProcessingWhenCannotDetermineShopUsingHeader() {
        // Сгенерировать тему
        String subject = "Автотест ocrm-1116 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();
        String numberOfMailMessages = "0";

        // Проверить, что тестовые данные не испорчены (что для supplier ID 1102211038 нет подходящего партнера)
        String numberOfSuppliers = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('account$shop').withFilters{\n" +
                        "  eq('shopId', '1102211232')\n" +
                        "}\n" +
                        ".count()");
        if (!numberOfSuppliers.equals("0")) {
            throw new Error("Для теста 1116 испорчены тестовые данные, нужно исправить shop ID на несуществующий");
        }

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-shopid", "1102211232");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // Отправляем созданное письмо
        Tools.email().sendAnEmail(newEmail);

        // Сгенерировать запрос
        String script = String.format("api.db.of('mailMessage').withFilters{ \n" +
                        "  eq('title', '%s')}.count()",
                subject);

        // Получить количество загруженных в систему email-писем
        /*
        Большой запас времени на ожидание обусловлен тем, что возможен довольно большой лаг со стороны почтового сервиса:
        код Tools.email().sendAnEmail(newEmail) отработает, но письмо в течение пары не минут не поступит на
        ящик-сборщик. Других решений, кроме как ждать его дольше, не вижу.
         */
        for (int i = 0; i < 20; i++) {
            Tools.waitElement(webDriver).waitTime(5000);
            numberOfMailMessages = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(script);
            if (!numberOfMailMessages.equals("0")) break;
        }

        Assert.assertEquals("Письмо, у которого в заголовке x-market-shopid пришел ShopID " +
                        "несуществующего партнера, не попало в систему, либо было импортировано больше 1 раза.",
                "1", numberOfMailMessages);
    }

    @Test
    @InfoTest(descriptionTest = "Разбор письма со ссылкой в теле",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-714",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1199")
    @Category(Blocker.class)
    public void ocrm1199_aMailMessageIsCreatedBasedOnAnEmailWithALinkToTicket() {
        int order = Tools.other().getRandomNumber(1000000, 9999999);
        Email email = new Email()
                .setSubject("ocrm_1199 Запрос от b2b: Нужно получить ПП  - заказ(ы): " + order)
                .setTo("logisticSupport")
                .setText("Очередь: ASAP \n" +
                        "Номер заказа: " + order + "\n" +
                        "Служба доставки: \n" +
                        "Номер обращения в SuiteCRM: https://crmtest.yandex-team.ru/index.php?module=Cases&offset=2&stamp=1602062112011917800&return_module=Cases&action=DetailView&record=8383bf93-812b-6dd4-1c5c-5f7c1f6afbc9\n" +
                        " \n" +
                        "Какой вопрос к СД?:\n" +
                        "Нужно получить ПП\n" +
                        "\n" +
                        "Логин сотрудника b2b: ekbstudent Ахметов Денис\n" +
                        "\n" +
                        "Отправлено из ФОС 56027")
                .setFromAlias("robot-forms@yandex-team.ru");

        Tools.email().sendAnEmail(email);

        String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("mailMessage", email.getSubject());
        Assert.assertNotNull("Не импортировалось письмо в теле которого есть ссылка на тикет в шугаре", gid);
    }

    @Test
    @InfoTest(descriptionTest = "Перекладывание обращения в очередь 'Покупки > Лавка'" +
            " при совпадении id доставки в заказе - 1006419",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1132",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1359")
    @Category({Critical.class})
    public void ocrm1359_MoveTicketToBeruLavkaServiceWhenDeliveryServiceIs1006419() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";
        // Сгенерировать тему
        String subject = "Автотест ocrm-1359 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Получить заказ
        String orderGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('order').withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.limit(1).get()"
        );
        String orderNumber = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').title",
                orderGid)
        );

        // Проставить у заказа службу доставки 1006419
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.bcp.edit('%s',['deliveryService':'1006419'])",
                orderGid)
        );

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.\n" +
                        String.format("Номер заказа: %s", orderNumber))
                .setFromAlias(emailAddress);

        // Получить gid ожидаемой очереди
        String expectedService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('service').withFilters{\n" +
                        "  eq('code', 'beruLavka')\n" +
                        "}.get()"
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", gid));

        Assert.assertEquals("Обращение не было переложено в очередь 'Покупки > Лавка'",
                expectedService, serviceFromPage);
    }

    @Test
    @InfoTest(descriptionTest = "Создание обращения в очереди \"Покупки > Обратная связь с Лавкой\" " +
            "при поступлении письма от такси",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1133",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1420")
    @Category({Critical.class})
    public void ocrm1420_CreateTicketInBeruLavkaServiceWhenReceivingEmailFromTaxi() {
        String gid = "null";
        // Ожидаемый комментарий
        Comment comment = new Comment();
        comment
                .setType("contact")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setNameAndEmail("sup@yandex-taxi.yaconnect.com");
        // Сгенерировать тему
        String subject = "Автотест ocrm-1420 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias("sup@yandex-taxi.yaconnect.com");

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "marketLavkafl");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", subject);
            if (gid != null) break;
        }

        // Зайти на страницу созданного обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        // Получить комментарии
        List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();
        boolean commentBool = commentsFromPage.contains(comment);

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "def service = api.db.get('%s').service.code", gid));
        boolean serviceBool = serviceFromPage.equals("beruLavka");

        Assert.assertTrue(String.format("Обращение было переложено в очередь 'Покупки > Лавка': %s,\n" +
                        "В обращении верно отобразился комментарий: %s", serviceBool, commentBool),
                serviceBool & commentBool);
    }

    @Test
    @InfoTest(descriptionTest = "Создание обращения в очереди \"Покупки > Обратная связь с Лавкой\" " +
            "при поступлении письма не от такси",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1134",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1421")
    @Category({Critical.class})
    public void ocrm1421_CreateTicketInBeruLavkaServiceWhenReceivingEmailNotFromTaxi() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";
        // Ожидаемый комментарий
        Comment comment = new Comment();
        comment
                .setType("internal")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setNameAndEmail("Система");
        // Сгенерировать тему
        String subject = "Автотест ocrm-1420 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress);

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "marketLavkafl");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", subject);
            if (gid != null) break;
        }

        // Зайти на страницу созданного обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        // Получить комментарии
        List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();
        boolean  commentBool = commentsFromPage.contains(comment);

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "def service = api.db.get('%s').service.code", gid));
        boolean serviceBool = serviceFromPage.equals("beruLavka");

        Assert.assertTrue(String.format("Обращение было переложено в очередь 'Покупки > Лавка': %s,\n" +
                        "В обращении верно отобразился комментарий: %s", serviceBool, commentBool),
                serviceBool&commentBool);
    }
}
