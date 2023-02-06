package ui_tests.src.test.java.tests.testsTickets;

import Classes.Comment;
import Classes.Email;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Critical;
import interfaces.testPriorities.Normal;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class TestsComments {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    ;
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsComments.class);

    @InfoTest(descriptionTest = "Проверка добавления внутреннего комментария",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-547")
    @Category({Blocker.class})
    @Test
    public void ocrm547_AddInternalComment() {
        Comment comment = new Comment();
        comment
                .setType("internal")
                .setText("Текст внутреннего комментария")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$beru')\n" +
                        ".withFilters{ eq('service', 'beruQuestion')\n" +
                        "             }\n" +
                        ".limit(1).get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickInternalMailTab();
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .setTextComment(comment.getText())
                .clickSaveANoteButton()
                .clickSendACommentActionButton();
        Tools.waitElement(webDriver).waitTime(2000);
        List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();
        boolean b = commentsFromPage.contains(comment);
        Assert.assertTrue(Tools.differ().format("Не добавился внешний комментарий или текст комментария не совпадает " +
                "с ожидаемым", comment, commentsFromPage), b);
    }

    @InfoTest(descriptionTest = "Проверка добавления внешнего комментария",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-548")
    @Category({Blocker.class})
    @Test
    public void ocrm548_AddPublicComment() {

        Comment comment = new Comment();
        comment
                .setType("public")
                .setText("Текст внешнего комментария")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("" +
                "def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters {\n" +
                "eq('status', 'registered')\n" +
                "eq('archived', false)\n" +
                "eq('service','beruQuestion')\n" +
                "}\n" +
                ".withOrders(api.db.orders.desc('creationTime'))\n" +
                ".limit(1)\n" +
                ".get()\n" +
                "api.bcp.edit(ticket, ['status' : 'processing'])\n" +
                "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        Pages.ticketPage(webDriver).properties().setCategory(Collections.singletonList("test sanity"));
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(comment.getText());

        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickSendAResponseActionButton();
        List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();
        boolean b = commentsFromPage.contains(comment);
        Assert.assertTrue("Не добавился внешний комментарий или текст комментария не совпадает с ожидаемым/", b);

    }

    @Ignore("выпилил из-за того что необходимо должго ждать комментария")
    @InfoTest(descriptionTest = "Проверяем добавление внутреннего комментария в обращение если по исходящему письму " +
            "из обращения не удалось отправить письмо",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-732")
    @Category({Blocker.class})
    @Test
    public void ocrm732_CheckingInfoMessageIfMessageWasNotSent() {
        // Создаём сообщение которое должно добавиться к обращению
        Comment expectedComment = new Comment()
                .setNameAndEmail("Система")
                .setText("Почтовый сервис не смог принять письмо. Возможно, указан неправильный адрес электронной " +
                        "почты.")
                .setType("internal");
        // Создаём письмо по которому создастся обращение
        Email email = new Email()
                .setSubject("ocrm732_CheckingInfoMessageIfMessageWasNotSent " + new GregorianCalendar().getTime())
                .setText(Tools.other().getRandomText())
                .setTo("beru")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");
        // Отправляем сообщение
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");

        // Ищем и открываем обращение созданное по письму
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(email.getSubject());

        boolean b;
        int i = 0;
        do {
            List<Comment> commentsFromPage = PageHelper.ticketPageHelper(webDriver).getAllComments();
            b = commentsFromPage.contains(expectedComment);
            if (b) {
                break;
            }
            if (i < 20) {
                Tools.waitElement(webDriver).waitTime(1000);
                webDriver.navigate().refresh();
                Tools.waitElement(webDriver).waitInvisibleLoadingElement();
                i++;
            } else {
                break;
            }
        } while (!b);
        Assert.assertTrue(b);
    }

    @InfoTest(
            descriptionTest = "Приоритет обращения сбрасывается на значение по умолчанию для очереди после отправки " +
                    "ответа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-970",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-295"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm970_TicketPrioritySetsToServiceDefaultAfterResponse() {
        String email = Tools.other().getRandomText() + "@yandex.ru";
        String currentPriorityGid;
        String ticketGid;
        String ticketPriorityGid;
        // Если у очереди "Покупки > Общие вопросы" приоритет по умолчанию не равен 50 - установить 50
        currentPriorityGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def obj = api.db.of('service').withFilters{\n" +
                        "  eq('gid', 'service@30013907')}\n" +
                        "  .get()\n" +
                        "return obj.defaultPriority");
        if (!currentPriorityGid.equals("priority@30014151")) {
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@30013907')\n" +
                            " api.bcp.edit(obj, ['defaultPriority' : 'priority@30014151'])");
        }

        // Найти в очереди обращение в статусе "Новый" с приоритетом не 50
        ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$firstLine').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "    not(eq('priority', '50'))\n" +
                        "}\n" +
                        ".limit(1).get()");

        // Установить в обращении тестовый email, категорию и перевести его в работу
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(String.format(
                "def obj = api.db.get('%s')\n" +
                        " api.bcp.edit(obj, ['clientEmail' : '%s'])\n" +
                        " api.bcp.edit(obj, ['status' : 'processing'])\n" +
                        " api.bcp.edit(obj, ['categories' : ['ticketCategory@76072202']])",
                ticketGid, email))
        );

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Отправить ответ с любым тестом через "Завершить -> Отправить ответ"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(Tools.other().getRandomText());
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickCloseButton().clickSendAResponseActionButton();

        // Получить приоритет обращения
        ticketPriorityGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "def obj = api.db.of('ticket$firstLine').withFilters{\n" +
                        "  eq('gid', '%s')}\n" +
                        "  .get()\n" +
                        "return obj.priority", ticketGid));

        // Сверить значения
        Assert.assertEquals("Приоритет тикета не сбросился значение по умолчанию после перехода в 'Решен'",
                "priority@30014151", ticketPriorityGid);
    }

    @InfoTest(
            descriptionTest = "Текст переносится между вкладками 'Внешнее письмо' и 'Внутренняя заметка'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-994",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-662"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm994_TransitTextBetweenOutgoingAndInternalTab() {
        String text = "ocrm994 " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");
        String textFromInternalTab;
        String textFromExternalTab;

        // Открыть чат
        Pages.navigate(webDriver).openPageByMetaClassAndID("ticket@136383006");

        // Переключиться на вкладку "Внешнее письмо"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickOutputMailTab();

        // Ввести текст в поле для ввода во вкладке "Внешнее письмо"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(text);

        // Переключиться на вкладку "Внутренняя заметка"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickInternalMailTab();
        textFromInternalTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();

        // Переключиться на вкладку "Внешнее письмо"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickOutputMailTab();
        textFromExternalTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();

        // Сверить значения
        Assert.assertTrue("При переключении влкадок 'Внешнее письмо' и 'Внутренняя заметка' написанный текст не " +
                        "сохраняется",
                text.equals(textFromInternalTab) && text.equals(textFromExternalTab));
    }

    @InfoTest(
            descriptionTest = "Текст не переносится между вкладками 'Сообщение в чат' и 'Внешнее письмо'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-995",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-662"
    )
    @Category({Blocker.class})
    @Ignore("Из тестов не проверить")
    @Test
    public void ocrm995_ClearTextBetweenChatAndOutgoingTab() {
        String chatText = "1111 ocrm995 1111";
        String outgoingText = "222 ocrm995 222";
        String textFromChatTab;
        String textFromOutgoingTab;

        // Открыть чат
        Pages.navigate(webDriver).openPageByMetaClassAndID("ticket@136383006");

        // Переключиться на вкладку "Сообщение в чат"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickChatTab();

        // Ввести текст в поле для ввода во вкладке "Сообщение в чат"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setChatTextComment(chatText);

        // Переключиться на вкладку "Внешнее письмо" и получить текст из нее
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickOutputMailTab();
        textFromOutgoingTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();
        // Вписать новый комментарий
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(outgoingText);

        // Переключиться на вкладку "Сообщение в чат" и получить текст из нее
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickChatTab();
        textFromChatTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredChatComment();

        // Сверить значения
        Assert.assertTrue("При переключении влкадок 'Сообщение в чат' и 'Внешнее письмо' написанный текст " +
                        "сохраняется, " +
                        "хотя не должен",
                !chatText.equals(textFromOutgoingTab) && !outgoingText.equals(textFromChatTab));
    }

    @InfoTest(
            descriptionTest = "Текст не переносится между вкладками 'Сообщение в чат' и 'Внутренняя заметка'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-996",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-662"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm996_ClearTextBetweenChatAndInternalTab() {
        String chatText = "1111 ocrm996 1111";
        String internalText = "222 ocrm996 222";
        String textFromChatTab;
        String textFromInternalTab;
        String ticket = "ticket@183087966";

        // Открыть чат
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticket);

        // Переключиться на вкладку "Сообщение в чат"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickChatTab();

        // Ввести текст в поле для ввода во вкладке "Сообщение в чат"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setChatTextComment(chatText);

        // Переключиться на вкладку "Внутренняя заметка" и получить текст из нее
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickInternalMailTab();
        textFromInternalTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();
        // Вписать новый комментарий
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(internalText);

        // Переключиться на вкладку "Сообщение в чат" и получить текст из нее
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickChatTab();
        textFromChatTab = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredChatComment();

        // Сверить значения
        Assert.assertTrue("При переключении влкадок 'Сообщение в чат' и 'Внутренняя заметка' написанный текст " +
                        "сохраняется, " +
                        "хотя не должен",
                !chatText.equals(textFromInternalTab) && !internalText.equals(textFromChatTab));
    }

    @InfoTest(
            descriptionTest = "Выбор очереди при создании исходящего обращения письменной коммуникации",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-991",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-821"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm991_ServiceSelectionForOutgoingTicket() {
        Ticket expectedTicket = new Ticket();
        Ticket createdTicket = new Ticket();

        expectedTicket
                .setSubject("Автотест ocrm821 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy " +
                        "HH:mm"))
                .setProperties(new Properties()
                        .setContactEmail(Tools.other().getRandomText() + "@yandex.ru")
                        .setCategory(Collections.singletonList("test sanity"))
                        .setService("Покупки > Жалобы на контент"))
                .setComments(Collections.singletonList((new Comment())
                        .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.")
                        .setNameAndEmail("Локи Одинсон (robot-loki-odinson)")
                        .setType("public"))
                );

        // Зайти в Письменная коммуникация-Обращения
        Pages.navigate(webDriver).openLVAllTickets();
        // Нажать кнопку "Добавить" -> "Покупки - исходящее"  и создать тикет
        PageHelper.tableHelper(webDriver).createNewTicket("Покупки - исходящее", expectedTicket);

        // Дождаться, пока откроется карточка обращения, проверяем видимостью блока комментариев
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//span[@data-ow-test-content='comments']"));

        // Собрать значения со страницы
        createdTicket
                .setSubject(Pages.ticketPage(webDriver).header().getSubject())
                .setProperties(new Properties()
                        .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                        .setCategory(Pages.ticketPage(webDriver).properties().getCategory())
                        .setService(Pages.ticketPage(webDriver).properties().getService()))
                .setComments(PageHelper.ticketPageHelper(webDriver).getAllComments());

        // Сверить значения
        Assert.assertEquals("Созданное обращение отличается от ожидаемого", expectedTicket, createdTicket);
    }


    @InfoTest(
            descriptionTest = "Валидация на отсутствие категории обращения при создании исходящего обращения " +
                    "письменной коммуникации",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1005",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-489"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1005_CategoriesValidationForOutgoingTicket() {
        Ticket ticket = new Ticket();

        ticket
                .setSubject("Автотест ocrm1005 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy " +
                        "HH:mm"))
                .setProperties(new Properties()
                        .setContactEmail(Tools.other().getRandomText() + "@yandex.ru"))
                .setComments(Collections.singletonList((new Comment())
                        .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.")
                        .setType("public"))
                );

        // Зайти в Письменная коммуникация-Обращения
        Pages.navigate(webDriver).openLVAllTickets();
        // Нажать кнопку "Добавить" -> "Покупки - исходящее"
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        Entity.entityTable(webDriver).toolBar().selectEntityOnSelectMenu("Покупки - исходящее");
        //Заполнить свойства обращения
        Pages.ticketPage(webDriver).createTicketPage().properties()
                .setClientEmail(ticket.getProperties().getContactEmail())
                .setTitle(ticket.getSubject());
        PageHelper.createTicketPageHelper(webDriver).setComment(ticket.getComments().get(0));
        // Сохранить
        Pages.ticketPage(webDriver).createTicketPage().header().clickButtonSaveForm("Добавить");

        // Проверить, что ошибка появилась
        List<String> alerts = Pages.alertDanger(webDriver).getAlertDangerMessages();
        for (String alert : alerts) {
            boolean b = Tools.other().isContainsSubstring("есть обязательные незаполненные поля.*Категории обращения"
                    , alert);

            if (b) {
                return;
            }
        }
        throw new AssertionError("Предупреждение о том, что категория не выбрана, не появилось");
    }


    @InfoTest(
            descriptionTest = "Предупреждение о несохранённых изменениях при нажатии на навигационные кнопки браузера",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1010",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-570"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1010_WarningAboutUnsavedChanges() {
        String ticketGid;

        // Найти в очереди "Покупки > Общие вопросы" обращение в статусе "Новый"
        ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$firstLine').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "}\n" +
                        ".limit(1).get()");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Нажать на кнопку "Изменить"
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Выбрать тэг в поле "Тэги"
        Pages.ticketPage(webDriver).properties().setTag(Collections.singletonList("Tag for autotests"));

        // Браузерной кнопкой "Назад" вернуться на карточку обращения
        webDriver.navigate().back();

        // Смотрим на текст появившегося браузерного алёрта и сравниваем с эталонным
        Assert.assertEquals("Данные будут утеряны. Вы уверены что хотите покинуть страницу?",
                (Tools.alerts(webDriver).getText()));
    }


    @InfoTest(
            descriptionTest = "Предупреждение о несохранённых изменениях при закрытии вкладки",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1033",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-398"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1033_WarningAboutUnsavedChangesWhenClosingTab() {
        String ticketGid;
        boolean alertIsPresent = true;

        // Найти в очереди "Покупки > Общие вопросы" обращение в статусе "Новый"
        ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$firstLine').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "    ne('channel','qualityManagement')\n" +
                        "}\n" +
                        ".limit(1).get()");

        // Создать новую вкладку и сделать фокус на ней (нужно, чтобы потом закрыть скриптом)
        Tools.tabsBrowser(webDriver).openUrlInNewTab("");
        Tools.tabsBrowser(webDriver).takeFocusNewTab();

        // Открыть карточку обращения в новой вкладке
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Нажать на кнопку "Изменить"
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Выбрать тэг в поле "Тэги"
        Pages.ticketPage(webDriver).properties().setTag(Collections.singletonList("Tag for autotests"));

        // Пытаемся закрыть вкладку с обращением скриптом, т.к. webDriver.close() убивает вкладку и алёрты не появляются
        Tools.scripts(webDriver).runScript("close()");

        // Ждём появления алёрта
        try {
            Tools.alerts(webDriver).accept();
        } catch (Throwable e) {
            if (!e.getMessage().contains("Не появился браузерный алерт")) {
                throw new Error(e);
            } else {
                alertIsPresent = false;
            }
        }
        Assert.assertTrue("Не появился браузерный алерт ", alertIsPresent);
    }


    @Test
    @InfoTest(descriptionTest = "Отправка письма клиенту без смены статуса обращения",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-671",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1041")
    public void ocrm1041_SendingAMessageToClientWithoutChangingStatusOfRequest() {
        //Получаем обращение очереди Рекламации Покупок > Входящие в статусе Новый
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def title = api.db.of('ticket$firstLine').withFilters{\n" +
                        "eq('service','beruQuestion')\n" +
                        "  eq('archived',false)\n" +
                        "  eq('status','registered')\n" +
                        "  eq('status','registered')\n" +
                        "\n" +
                        "}\n" +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        "  .limit(1)\n" +
                        "  .get()\n" +
                        "\n" +
                        "return title");
        Comment existComment = new Comment()
                .setText("Hi dude " + Tools.other().getRandomText())
                .setType("public")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");
        //открываем найденное обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
        //Отправляем внешнее сообщение
        Pages.ticketPage(webDriver).messageTab().comments().commentsCreation()
                .setTextComment(existComment.getText())
                .clickButton("Завершить")
                .clickButtonActionOnTicket("Отправить комментарий");
        Tools.waitElement(webDriver).waitTime(5000);
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        //получаем список сообщений обращения
        List<Comment> comments = PageHelper.ticketPageHelper(webDriver).getAllComments();
        //получаем статус обращения
        String status = Pages.ticketPage(webDriver).properties().getStatus();

        boolean b1 = comments.contains(existComment);
        boolean b2 = status.equals("Новый");


        Assert.assertTrue("При отправке письма клиенту без статуса:\n" +
                "статус обращения не поменялся - " + b2 + "\n" +
                "отправленное сообщение добавилось в список сообщений обращения - " + b1, b1 & b2);

    }

    @InfoTest(descriptionTest = "Ошибка при перекладывании обращения без смены очереди",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1049",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-453",
            requireYouToLogInUnderANewUser = true
    )
    @Category({Critical.class})
    @Test
    public void ocrm1049_ErrorWhenShiftingTickets() {
        // Список предупреждений со страницы
        List<String> dangerMessages;
        boolean b = false;

        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def employee = api.db.of('employee').withFilters{\n" +
                        "eq('staffLogin', '" + Config.getAdditionalUserLogin() + "')\n" +
                        "}.get()\n" +
                        "\n" +
                        "def services = api.db.of('service').withFilters{\n" +
                        "eq('brand','yandexDeliveryLogisticSupport')\n" +
                        "}.list()\n" +
                        "\n" +
                        "api.bcp.edit(employee,['services':services])"
        );
        Tools.waitElement(webDriver).waitTime(5000);

        // Перейти в статус "готов"
        PageHelper.mainMenuHelper(webDriver).switchUserToStatus("Готов");

        // Не вводя текст комментария, нажать на кнопку Завершить - Переложить
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickShiftActionButton();

        // Получить все предупреждения со страницы
        dangerMessages = Pages.ticketPage(webDriver).alertDanger().getAlertDangerMessages();
        for (String dangerMessage : dangerMessages) {
            if (dangerMessage.contains("Ошибка. Очередь должна быть изменена")) {
                b = true;
            }
        }
        Assert.assertTrue("Не появилось сообщение с ошибкой об изменении очереди", b);
    }

    @InfoTest(descriptionTest = "Перекладывание обращения в другую очередь",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1050",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-453",
            requireYouToLogInUnderANewUser = true
    )
    @Category({Critical.class})
    @Test
    public void ocrm1050_ShiftingTicketToOtherService() {

        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def employee = api.db.of('employee').withFilters{\n" +
                        "eq('staffLogin', '" + Config.getAdditionalUserLogin() + "')\n" +
                        "}.get()\n" +
                        "\n" +
                        "def services = api.db.of('service').withFilters{\n" +
                        "eq('code','beruQuestion')\n" +
                        "}.list()\n" +
                        "\n" +
                        "api.bcp.edit(employee,['services':services])"
        );
        Tools.waitElement(webDriver).waitTime(5000);

        // Перейти в статус "готов"
        PageHelper.mainMenuHelper(webDriver).switchUserToStatus("Готов");

        // Скопировать url открывшегося обращения
        String ticketGid = Tools.other().getGidFromCurrentPageUrl(webDriver);

        // Выбрать очередь "Покупки > Общие вопросы VIP" в поле "Очередь"
        Pages.ticketPage(webDriver).properties().setService("Покупки > Общие вопросы VIP");
//        Entity.properties(webDriver).setPropertiesOfTreeSelectTypeField("service", Arrays.asList("Покупки > Общие
//        вопросы VIP"));

        // Не вводя текст комментария, нажать на кнопку Завершить - Переложить
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickShiftActionButton();

        String result = "";
        for (int i = 0; i < 5; i++) {
            result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def ticket = api.db.get('" + ticketGid + "')\n" +
                            "\n" +
                            "def status = ticket.status == \"reopened\"\n" +
                            "def service = ticket.service == \"beruVipQuestion\"\n" +
                            "\n" +
                            "return status&&service");
            if (result.equals("true")) {
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }

        // Проверить, совпадают ли данные с ожидаемыми
        Assert.assertTrue("Статус или очередь отличаются от ожидаемых", result.equals("true"));
    }

    @InfoTest(descriptionTest = "Переложенное обращение не назначается на оператора, с которого только что было снято",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1052",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-453",
            requireYouToLogInUnderANewUser = true
    )
    @Category({Critical.class})
    @Test
    public void ocrm1052_ShiftingTicketNotAssignedToOperator() {
        // Перейти в статус "готов"
        PageHelper.mainMenuHelper(webDriver).switchUserToStatus("Готов");

        // Скопировать url открывшегося обращения
        String ticketUrl = webDriver.getCurrentUrl();

        // Выбрать очередь "Покупки > Общие вопросы VIP" в поле "Очередь"
        Pages.ticketPage(webDriver).properties().setService("Покупки > Общие вопросы VIP");

        // Не вводя текст комментария, нажать на кнопку Завершить - Переложить
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickShiftActionButton();

        // Скопировать url следующего назначившегося обращения
        String secondTicketUrl = webDriver.getCurrentUrl();

        // Сравнить url обращений, они должны быть разные
        Assert.assertFalse("Обращения одинаковые", ticketUrl == secondTicketUrl);
    }

    @Test
    @InfoTest(descriptionTest = "Валидация незаполненного поля для отправки комментария в обращении",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1211",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-56")
    @Category(Normal.class)
    public void ocrm1211_validationOfAnEmptyFieldForSendingACommentInARequest() {
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$firstLine').withFilters{\n" +
                        "eq('service','beruQuestion')\n" +
                        "eq('status','registered')\n" +
                        "}.withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1)\n" +
                        ".get()\n" +
                        "\n" +
                        "api.bcp.edit(ticket, ['status' : 'processing'])\n" +
                        "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickSendAResponseActionButton();

        Tools.waitElement(webDriver).waitTime(1500);

        List<String> alerts = Pages.alertDanger(webDriver).getAlertDangerMessages();
        Boolean b = false;

        for (String alert : alerts) {
            b = Tools.other().isContainsSubstring("есть обязательные незаполненные поля.*Комментарий", alert);
            if (b) {
                break;
            }
        }

        Assert.assertTrue("При попытке отправить ответ с незаполненным полем комментария," +
                "не появилось сообщение с ошибкой", b);
    }

}
