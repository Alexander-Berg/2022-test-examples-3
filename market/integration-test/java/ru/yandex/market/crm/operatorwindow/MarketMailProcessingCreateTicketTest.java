package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruTicket;
import ru.yandex.market.crm.operatorwindow.utils.BeruComplaintsTicketUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;
import ru.yandex.market.ocrm.module.order.domain.Order;

@Transactional
public class MarketMailProcessingCreateTicketTest extends AbstractMailProcessingTest {

    private static final String MAIL_CONNECTION_BERU_PROMO = "beruPromo";
    private static final String MAIL_CONNECTION_BERU = "beru";
    private static final String MAIL_CONNECTION_MARKET = "market";

    @Inject
    private BeruComplaintsTicketUtils beruComplaintsTicketUtils;

    private static Stream<Arguments> attributeForCreateTicketDSBSFromEmail() {
        return Stream.of(
                Arguments.arguments("WHITE", MAIL_CONNECTION_BERU, "marketQuestion"),
                Arguments.arguments("WHITE", MAIL_CONNECTION_MARKET, "marketQuestion"),
                Arguments.arguments("BLUE", MAIL_CONNECTION_MARKET, "beruQuestion")
        );
    }

    private static Stream<String> argumentsForCreatingTicketForAnEmailWithReplyToHeader() {
        return Stream.of("", "test1 <test@test.te>", null);
    }

    @BeforeEach
    void setUp1() {
        var team = ticketTestUtils.createTeamIfNotExists("firstLineMail");
        var brand = ticketTestUtils.createBrand("beru");
        beruComplaintsTicketUtils.createService("beruQuestion", team, brand.getCode());
        beruComplaintsTicketUtils.createService("marketQuestion", team, brand.getCode());
        mailTestUtils.createMailConnection(MAIL_CONNECTION_BERU);
        mailTestUtils.createMailConnection(MAIL_CONNECTION_BERU_PROMO);
        mailTestUtils.createMailConnection(MAIL_CONNECTION_MARKET);
    }

    @Test
    @Description("""
            Создание обращений в очереди "Покупки > Промокоды"
            тест-кейс:
            - https://testpalm2.yandex-team.ru/testcase/ocrm-982
            - https://testpalm2.yandex-team.ru/testcase/ocrm-983
            """)
    void createTicketInBeruPromoService() {
        Order order = orderTestUtils.createOrder();
        List<Service> service = dbService.list(
                Query.of(Service.FQN)
                        .withFilters(
                                Filters.eq(Service.CODE, "beruPromo")
                        ));
        bcpService.edit(service.get(0), Map.of(Service.FILL_CUSTOMER_IN_FO_FROM_ORDER, true));

        var mail = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION_BERU_PROMO)
                .setFrom("robot-forms@yandex-team.ru")
                .setBody(String.format("""
                        Причина выдачи купонов: Нарушены сроки доставки
                        Номер заказа покупателя: %s
                        E-mail покупателя **ДЛЯ СВЯЗИ**: %s
                        Какой промокод выдать?: 100""", order.getOrderId(), Randoms.email())
                )
                .build();
        mailProcessingService.processInMessage(mail);

        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);
        Assertions.assertEquals(1, tickets.size());

        BeruTicket ticket = (BeruTicket) tickets.get(0);

        Assertions.assertEquals(Ticket.STATUS_REGISTERED, ticket.getStatus());
        Assertions.assertEquals("beruPromo", ticket.getService().getCode());
        Assertions.assertEquals(order.getOrderId(), ticket.getOrder().getOrderId());
        Assertions.assertEquals(order.getBuyerEmail(), ticket.getClientEmail());
    }

    @Test
    @Description("""
            Создание обращений в очереди 'Покупки > DSBS Еда'
            тест-кейс https://testpalm2.yandex-team.ru/testcase/ocrm-1167
            """)
    void createTicketInBeruDsbsFoodService() {
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.SHOP_ID, "1017176"
        ));

        var mail = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION_BERU)
                .setFrom(Randoms.email())
                .setBody(String.format("Номер заказа: %s\n", order.getTitle()))
                .build();
        mailProcessingService.processInMessage(mail);

        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);

        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(Ticket.STATUS_REGISTERED, tickets.get(0).getStatus());
        Assertions.assertEquals("beruDsbsFood", tickets.get(0).getService().getCode());
    }

    @Test
    @Description("""
            Перекладывание обращения в очередь "Покупки > Лавка" при совпадении id доставки в заказе
            Тест-кейс https://testpalm2.yandex-team.ru/testcase/ocrm-1132
            """)
    void moveTicketToBeruLavkaServiceWhenDeliveryServiceIs1005471() {
        DeliveryService deliveryService = bcpService.create(DeliveryService.FQN, Maps.of(
                DeliveryService.CODE, "1005471",
                DeliveryService.TITLE, Randoms.string()
        ));
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.DELIVERY_SERVICE, deliveryService
        ));

        var mail = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION_BERU)
                .setFrom(Randoms.email())
                .setBody(String.format("Номер заказа: %s\n", order.getTitle()))
                .build();
        mailProcessingService.processInMessage(mail);

        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);

        Assertions.assertEquals(1, tickets.size());

        Assertions.assertEquals("beruLavka", tickets.get(0).getService().getCode());
    }

    @ParameterizedTest
    @Description("""
            Создание обращений в нужной очереди из разных mailConnection
            https://testpalm2.yandex-team.ru/testcase/ocrm-689
            https://testpalm2.yandex-team.ru/testcase/ocrm-688
            https://testpalm2.yandex-team.ru/testcase/ocrm-691
            """)
    @MethodSource("attributeForCreateTicketDSBSFromEmail")
    void createTicketFromEmail(String orderColor, String mailConnection, String serviceCode) {
        Order order = orderTestUtils.createOrder(Map.of(Order.COLOR, orderColor));
        var mail = mailMessageBuilderService.getMailMessageBuilder(mailConnection)
                .setFrom(Randoms.email())
                .setBody(String.format("Номер заказа: %s", order.getOrderId()))
                .build();
        mailProcessingService.processInMessage(mail);

        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);

        Assertions.assertEquals(1, tickets.size());

        Assertions.assertEquals(serviceCode, tickets.get(0).getService().getCode());
    }

    @ParameterizedTest
    @Description("""
            Проверка заполнения поля "Контактный e-mail" при создании обращения из письма с заголовком Reply-To
            https://testpalm2.yandex-team.ru/testcase/ocrm-717
            https://testpalm2.yandex-team.ru/testcase/ocrm-716
            https://testpalm2.yandex-team.ru/testcase/ocrm-718
            """)
    @MethodSource("argumentsForCreatingTicketForAnEmailWithReplyToHeader")
    void creatingTicketForAnEmailWithReplyToHeader(String replyTo) {
        String alias = Randoms.email();
        String existEmail = alias;
        var mail = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION_BERU);

        if (replyTo != null) {
            existEmail = replyTo.equals("") ? alias : replyTo;
            mail.setReplyToList(replyTo);
        }

        mail
                .setFrom(alias)
                .setBody(Randoms.string());

        mailProcessingService.processInMessage(mail.build());

        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);

        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(existEmail, tickets.get(0).getClientEmail());
    }
}
