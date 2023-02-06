package ru.yandex.market.crm.operatorwindow;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.MessagingException;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruLogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.YandexDeliveryLogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder;
import ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.operations.AddCommentOperationHandler;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.html.Htmls;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;
import ru.yandex.market.ocrm.module.order.domain.SortingCenter;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;

import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_QUESTION;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_CLIENT_NAME;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_CLIENT_PROBLEM;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_DELIVERY_SERVICE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_ORDER_ID;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_REQUEST_FROM;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_RESPONSE_TO;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.DEFAULT_SORTING_CENTER;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_CATEGORY;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_BUYER_ADDRESS_PHONE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_DELIVERY_ADDRESS;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_DELIVERY_DATE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_DELIVERY_INDEX;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_DELIVERY_INTERVAL;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_NEW_RECIPIENT_FULL_NAME;
import static ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder.EXAMPLE_REGION;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.DATA_TO_CHANGE_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.DELIVERY_ORDER_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.DELIVERY_SERVICE_NAME_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.PICKUP_REGION_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.PLANNED_DELIVERY_DATE_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.SHIPMENT_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.SHIPMENT_TYPE_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.SHOP_ID_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.SORTING_CENTER_NAME_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.YA_DELIVERY_CLIENT_EMAIL_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.YA_DELIVERY_CLIENT_NAME_EXAMPLE;
import static ru.yandex.market.crm.operatorwindow.utils.mail.YaDeliveryLogisticMailBodyBuilder.YA_DELIVERY_REGION_EXAMPLE;

@Transactional
public class LogisticSupportMailProcessingTest extends AbstractLogisticSupportMailProcessingTest {

    private static final String DEFAULT_EMAIL_SUBJECT = "Тестовое письмо";
    private static final Long NOT_EXISTS_ORDER_ID = 100L;

    private static final String DEFAULT_EML_SENDER_NAME = "Виталий Дорогин";
    private static final String DEFAULT_EML_SENDER_EMAIL = "vdorogin@yandex-team.ru";
    // Используется по умолчанию, body из logisticSupportEmail.eml, однако, в тестах можно сгенерировать свой body
    // c помощью mailMessageBuilder.setBody(new LogisticMailBodyBuilder().build())
    // В тестах внизу часто используется этот body для эмуляции того, что тикет уже существует
    private static final String DEFAULT_EML_BODY = ""
            + "<div><div><div>Заказ: https://ow.market.yandex-team.ru/order/11989056</div><div>"
            + "</div><div>Тип клиента:</div><div>обычный_клиент</div><div>"
            + "</div><div>Имя клиента:</div><div>Григорий</div><div>"
            + "</div><div>Номер заказа:</div><div>11989056</div><div>"
            + "</div><div>Откуда запрос?:</div><div>почта_или_чат</div><div>"
            + "</div><div>Ссылка на тикет в Едином Окне:</div><div>https://ow.market.yandex-team"
            + ".ru/order/11989056</div><div>"
            + "</div><div>Проблема клиента:</div><div>Нарушены сроки доставки</div><div>"
            + "</div><div>Как удобно получить ответ?:</div><div>почта</div><div>"
            + "</div><div>e-mail клиента для ответа или отправки кассового чека:</div><div>galoktev&#64;yandex"
            + ".ru</div><div>"
            + "</div><div>Причина обращения покупателя по нарушению срока доставки:</div><div>Нарушены сроки: РДД"
            + " был вчера или раньше</div><div>"
            + "</div><div>Служба доставки:</div><div>ПЭК</div><div>"
            + "</div><div>Сортировочный центр:</div><div>Свердловск-сортировочный</div><div>"
            + "</div><div>Последняя дата доставки в интервале:</div><div>2020-01-08</div></div></div><div>"
            + "</div><div>--"
            + "</div><div>С уважением,</div><div>Виталий Дорогин</div><div>https://staff.yandex-team"
            + ".ru/vdorogin</div><div></div>";


    @Inject
    private Htmls htmls;

    @Disabled // TODO: Нужно научиться проходить этот тест
    public void testBadMail() throws MessagingException {
        processMessage(createBeruMailMessageBuilder("/mail_message/logisticSupportBadEmail.eml").build());
    }

    @Test
    public void testBeruLogisticSupport_mainAttributes() {
        // данные берем из почтового письма
        processMessage(beruMailMessageBuilder.build());

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        assertTicketTypeAndService(ticket, BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        Assertions.assertEquals(DEFAULT_EMAIL_SUBJECT, ticket.getTitle());
        Assertions.assertNotNull(ticket.getOrder());
        Assertions.assertEquals(DEFAULT_ORDER_ID, ticket.getOrder().getOrderId());

        Assertions.assertNotNull(ticket.getCategories());
        Assertions.assertEquals(1, ticket.getCategories().size());
        Assertions.assertEquals(DEFAULT_CLIENT_PROBLEM,
                ticket.getCategories().iterator().next().getTitle());

        Assertions.assertEquals(DEFAULT_REQUEST_FROM, ticket.getRequestFrom());
        Assertions.assertEquals(DEFAULT_RESPONSE_TO, ticket.getResponseTo());
        Assertions.assertEquals(DEFAULT_CLIENT_NAME, ticket.getClientName());
        Assertions.assertEquals(DEFAULT_CLIENT_EMAIL, ticket.getClientEmail());
        Assertions.assertNotNull(ticket.getDeliveryService());
        Assertions.assertEquals(DEFAULT_DELIVERY_SERVICE, ticket.getDeliveryService().getTitle());
        Assertions.assertNotNull(ticket.getSortingCenter());
        Assertions.assertEquals(DEFAULT_SORTING_CENTER, ticket.getSortingCenter().getTitle());

        assertSingleComment(ticket, InternalComment.FQN, DEFAULT_EML_BODY);
    }

    @Test
    public void testBeruLogisticSupport_additionalAttributes() {
        createTicketCategory(EXAMPLE_CATEGORY, Constants.Brand.BERU_LOGISTIC_SUPPORT);
        String category2 = "Жалоба на товар";
        createTicketCategory(category2, Constants.Brand.BERU_LOGISTIC_SUPPORT);

        processMessage(beruMailMessageBuilder
                .setBody(new LogisticMailBodyBuilder()
                        .setNewDeliveryAddress(EXAMPLE_NEW_DELIVERY_ADDRESS)
                        .setNewDeliveryInterval(EXAMPLE_NEW_DELIVERY_INTERVAL)
                        .setNewDeliveryDate(EXAMPLE_NEW_DELIVERY_DATE)
                        .setRegion(EXAMPLE_REGION)
                        .setCategory(EXAMPLE_CATEGORY, category2)
                        .setNewDeliveryIndex(EXAMPLE_NEW_DELIVERY_INDEX)
                        .setNewBuyerAddressPhone(EXAMPLE_NEW_BUYER_ADDRESS_PHONE)
                        .setNewRecipientFullName(EXAMPLE_NEW_RECIPIENT_FULL_NAME)
                        .build())
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        Assertions.assertNotNull(ticket.getCategories());
        Assertions.assertEquals(Set.of(EXAMPLE_CATEGORY, category2),
                Set.copyOf(CrmCollections.transform(ticket.getCategories(), TicketCategory::getTitle)));

        Assertions.assertEquals(EXAMPLE_NEW_DELIVERY_ADDRESS, ticket.getNewDeliveryAddress());
        Assertions.assertEquals(EXAMPLE_NEW_DELIVERY_INTERVAL, ticket.getNewDeliveryInterval());
        Assertions.assertEquals(EXAMPLE_NEW_DELIVERY_DATE, ticket.getNewDeliveryDate());
        Assertions.assertEquals(EXAMPLE_REGION, ticket.getRegion());
        Assertions.assertEquals(EXAMPLE_NEW_DELIVERY_INDEX, ticket.getNewDeliveryIndex());
        Assertions.assertEquals(Phone.fromRaw(EXAMPLE_NEW_BUYER_ADDRESS_PHONE), ticket.getNewBuyerAddressPhone());
        Assertions.assertEquals(EXAMPLE_NEW_RECIPIENT_FULL_NAME, ticket.getNewRecipientFullName());
    }

    private YaDeliveryLogisticMailBodyBuilder generateDefaultYaDeliveryLogisticMailBody(
            boolean includeLogisticSupportDefaultValues) {
        var bodyBuilder = new YaDeliveryLogisticMailBodyBuilder(includeLogisticSupportDefaultValues);

        // убираем orderId т.к. он имеет тот же key, что и deliveryOrderId
        bodyBuilder.setOrderId(null);

        return bodyBuilder
                .setYaDeliveryClientName(YA_DELIVERY_CLIENT_NAME_EXAMPLE)
                .setYaDeliveryClientEmail(YA_DELIVERY_CLIENT_EMAIL_EXAMPLE)
                .setYaDeliveryOrderText(DELIVERY_ORDER_EXAMPLE)
                .setDeliveryServiceName(DELIVERY_SERVICE_NAME_EXAMPLE)
                .setSortingCenterName(SORTING_CENTER_NAME_EXAMPLE)
                .setYaDeliveryRegion(YA_DELIVERY_REGION_EXAMPLE)
                .setPlannedDeliveryDate(PLANNED_DELIVERY_DATE_EXAMPLE)
                .setDataToChange(DATA_TO_CHANGE_EXAMPLE)
                .setShipment(SHIPMENT_EXAMPLE)
                .setShipmentType(SHIPMENT_TYPE_EXAMPLE)
                .setPickupRegion(PICKUP_REGION_EXAMPLE)
                .setShopId(SHOP_ID_EXAMPLE);
    }

    private void assertYaDeliveryLogisticAttributes(YaDeliveryLogisticMailBodyBuilder bodyBuilder,
                                                    YandexDeliveryLogisticSupportTicket ticket,
                                                    YaDeliveryOrder yaDeliveryOrder) {
        Assertions.assertEquals(bodyBuilder.getYaDeliveryClientName(), ticket.getClientName());
        Assertions.assertEquals(bodyBuilder.getYaDeliveryClientEmail(), ticket.getClientEmail());
        Assertions.assertNotNull(ticket.getYaDeliveryOrder());
        Assertions.assertEquals(bodyBuilder.getDeliveryOrder(), ticket.getRawYaDeliveryOrderId());
        Assertions.assertEquals(yaDeliveryOrder.getGid(), ticket.getYaDeliveryOrder().getGid());
        Assertions.assertEquals(bodyBuilder.getDeliveryServiceName(), ticket.getDeliveryService().getTitle());
        Assertions.assertEquals(bodyBuilder.getSortingCenterName(), ticket.getSortingCenter().getTitle());
        Assertions.assertEquals(bodyBuilder.getYaDeliveryRegion(), ticket.getRegion());
        Assertions.assertEquals(bodyBuilder.getPlannedDeliveryDate(), ticket.getPlannedDeliveryDate());
        Assertions.assertEquals(bodyBuilder.getDataToChange(), ticket.getDataToChange());
        Assertions.assertEquals(bodyBuilder.getShipment(), ticket.getShipment());
        Assertions.assertEquals(bodyBuilder.getShipmentType(), ticket.getShipmentType());
        Assertions.assertEquals(bodyBuilder.getShopId(), ticket.getShopId());
    }

    @Test
    public void testYaDeliveryLogisticSupport_with_default_logistic_attributes() {
        createTicketCategory(EXAMPLE_CATEGORY, Constants.Brand.YANDEX_DELIVERY_LOGISTIC_SUPPORT);

        var bodyBuilder = new YaDeliveryLogisticMailBodyBuilder();
        bodyBuilder.setRegion("regionOne");
        bodyBuilder.setPickupRegion("regionTwo");

        var msg = yaDeliveryMailMessageBuilder.setBody(
                bodyBuilder.build()
        ).build();

        processMessage(msg);

        var ticket = (YandexDeliveryLogisticSupportTicket) getSingleLogisticSupportTicket();

        Assertions.assertEquals(bodyBuilder.getPickupRegion(), ticket.getRegion());
    }

    @Test
    public void testYaDeliveryLogisticSupport_attributes() {
        createTicketCategory(EXAMPLE_CATEGORY, Constants.Brand.YANDEX_DELIVERY_LOGISTIC_SUPPORT);
        YaDeliveryOrder yaDeliveryOrder = yaDeliveryTestUtils.createYaDeliveryOrder(Map.of(
                YaDeliveryOrder.EXTERNAL_ID, Randoms.string()
        ));

        var bodyBuilder = generateDefaultYaDeliveryLogisticMailBody(false);

        var msg = yaDeliveryMailMessageBuilder.setBody(
                bodyBuilder.setYaDeliveryOrder(yaDeliveryOrder).build()
        ).build();

        processMessage(msg);

        var ticket = (YandexDeliveryLogisticSupportTicket) getSingleLogisticSupportTicket();

        assertYaDeliveryLogisticAttributes(bodyBuilder, ticket, yaDeliveryOrder);
    }

    @Test
    public void testYaDeliveryLogisticSupport_region_attributes() {
        var bodyBuilder = generateDefaultYaDeliveryLogisticMailBody(false);
        YaDeliveryOrder yaDeliveryOrder = yaDeliveryTestUtils.createYaDeliveryOrder(Map.of(
                YaDeliveryOrder.EXTERNAL_ID, Randoms.string()
        ));

        var msg = yaDeliveryMailMessageBuilder.setBody(
                bodyBuilder.setYaDeliveryOrder(yaDeliveryOrder).build()
        ).build();

        processMessage(msg);

        var ticket = (YandexDeliveryLogisticSupportTicket) getSingleLogisticSupportTicket();

        assertYaDeliveryLogisticAttributes(bodyBuilder, ticket, yaDeliveryOrder);
    }

    @Test
    public void testAddCommentToExistingOpenYaDeliveryLogisticSupportTicket_withOrder() {
        var bodyBuilder1 = generateDefaultYaDeliveryLogisticMailBody(false);
        YaDeliveryOrder yaDeliveryOrder = yaDeliveryTestUtils.createYaDeliveryOrder(Map.of(
                YaDeliveryOrder.EXTERNAL_ID, Randoms.string()
        ));

        var msg1 = yaDeliveryMailMessageBuilder.setBody(
                bodyBuilder1.setYaDeliveryOrder(yaDeliveryOrder).build()
        ).build();

        processMessage(msg1);

        var bodyBuilder2 = generateDefaultYaDeliveryLogisticMailBody(false);
        var msg2 = yaDeliveryMailMessageBuilder
                .newDeduplicationKey()
                .setBody(bodyBuilder2.setYaDeliveryOrder(yaDeliveryOrder).build())
                .build();
        processMessage(msg2);

        var ticket = (YandexDeliveryLogisticSupportTicket) getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());
    }

    @Test
    public void testAddCommentToExistingOpenYaDeliveryLogisticSupportTicket_withoutOrder() {
        var bodyBuilder1 = generateDefaultYaDeliveryLogisticMailBody(false);

        var msg1 = yaDeliveryMailMessageBuilder.setBody(bodyBuilder1.setYaDeliveryOrderText(null).build()).build();

        processMessage(msg1);

        var bodyBuilder2 = generateDefaultYaDeliveryLogisticMailBody(false);
        var msg2 = yaDeliveryMailMessageBuilder
                .newDeduplicationKey()
                .setBody(bodyBuilder2.setYaDeliveryOrderText(null).build())
                .build();
        processMessage(msg2);

        var ticket = (YandexDeliveryLogisticSupportTicket) getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());
    }

    @Test
    public void testNewYaDeliveryLogisticSupportTicket_whenMailWithOrder() throws MessagingException {
        var bodyBuilder1 = generateDefaultYaDeliveryLogisticMailBody(false);
        var msg1 = yaDeliveryMailMessageBuilder.setBody(bodyBuilder1.setYaDeliveryOrderText(null).build()).build();
        processMessage(msg1);

        YaDeliveryOrder yaDeliveryOrder = yaDeliveryTestUtils.createYaDeliveryOrder(Map.of(
                YaDeliveryOrder.EXTERNAL_ID, Randoms.string()
        ));

        var bodyBuilder2 = generateDefaultYaDeliveryLogisticMailBody(false);
        var msg2 = createDefaultYaDeliveryMailMessageBuilder()
                .setBody(
                        bodyBuilder2
                                .setYaDeliveryOrder(yaDeliveryOrder)
                                .build()
                )
                .newDeduplicationKey()
                .build();

        processMessage(msg2);

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());
    }

    @Test
    public void testNewYaDeliveryLogisticSupportTicket_whenMailWithoutOrder() throws MessagingException {
        var bodyBuilder1 = generateDefaultYaDeliveryLogisticMailBody(false);
        YaDeliveryOrder yaDeliveryOrder = yaDeliveryTestUtils.createYaDeliveryOrder(Map.of(
                YaDeliveryOrder.EXTERNAL_ID, Randoms.string()
        ));

        var msg1 = yaDeliveryMailMessageBuilder.setBody(
                bodyBuilder1.setYaDeliveryOrder(yaDeliveryOrder).build()
        ).build();
        processMessage(msg1);

        var bodyBuilder2 = generateDefaultYaDeliveryLogisticMailBody(false);
        var msg2 = createDefaultYaDeliveryMailMessageBuilder()
                .setBody(bodyBuilder2.setYaDeliveryOrderText(null).build())
                .newDeduplicationKey()
                .build();

        processMessage(msg2);

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());
    }

    @Test
    public void testFindCategoryByClientProblemAndBrand() {
        Brand testBrand = ticketTestUtils.createBrand();
        String clientProblem1 = Randoms.string();
        createTicketCategory(clientProblem1, testBrand.getCode());
        TicketCategory expectedCategory1 = createTicketCategory(clientProblem1,
                Constants.Brand.BERU_LOGISTIC_SUPPORT);
        String clientProblem2 = Randoms.string();
        TicketCategory expectedCategory2 = createTicketCategory(clientProblem2,
                Constants.Brand.BERU_LOGISTIC_SUPPORT);

        processMessage(beruMailMessageBuilder
                .setBody(new LogisticMailBodyBuilder()
                        .setClientProblem(clientProblem1, clientProblem2)
                        .build())
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        final Set<TicketCategory> actualCategories = ticket.getCategories();
        Assertions.assertNotNull(actualCategories);
        Assertions.assertEquals(2, actualCategories.size());
        Assertions.assertEquals(Set.of(expectedCategory1.getCode(), expectedCategory2.getCode()),
                Set.copyOf(CrmCollections.transform(actualCategories, TicketCategory::getCode)));
        Assertions.assertEquals(Set.of(Constants.Brand.BERU_LOGISTIC_SUPPORT),
                Set.copyOf(CrmCollections.transform(actualCategories, x -> x.getBrand().getCode())));
    }

    @Test
    public void testChooseCategoryFromWrittenCategoryNotClientProblem() {
        String categoryTitle = Randoms.string();
        String clientProblem = Randoms.string();

        createTicketCategory(clientProblem, Constants.Brand.BERU_LOGISTIC_SUPPORT);
        TicketCategory expectedCategory = createTicketCategory(categoryTitle,
                Constants.Brand.BERU_LOGISTIC_SUPPORT);

        processMessage(beruMailMessageBuilder
                .setBody(new LogisticMailBodyBuilder()
                        .setClientProblem(clientProblem)
                        .setCategory(categoryTitle)
                        .build())
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        Assertions.assertNotNull(ticket.getCategories());
        Assertions.assertEquals(1, ticket.getCategories().size());
        Assertions.assertEquals(expectedCategory.getCode(),
                ticket.getCategories().iterator().next().getCode());
    }


    @Test
    public void testAddUserCommentToExistingOpenTicket() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        String userEmailBody = new LogisticMailBodyBuilder()
                .setOrderId(DEFAULT_ORDER_ID)
                .build();
        processMessage(createDefaultBeruMailMessageBuilder()
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(Comment.FQN_USER, comments.get(1).getFqn());

        Assertions.assertEquals(DEFAULT_EML_BODY, comments.get(0).getBody());
        Assertions.assertEquals(htmls.hideQuotes(userEmailBody), comments.get(1).getBody());
    }

    @Test
    public void testAddCommentByEmailWithoutOrderToTicketWithoutOrder() throws MessagingException {
        String emailBody = new LogisticMailBodyBuilder(true)
                .setOrderId(NOT_EXISTS_ORDER_ID)
                .build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).build());

        String userEmailBody = "test";
        processMessage(createDefaultBeruMailMessageBuilder()
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(Comment.FQN_USER, comments.get(1).getFqn());

        Assertions.assertEquals(htmls.hideQuotes(emailBody), comments.get(0).getBody());
        Assertions.assertEquals(userEmailBody, comments.get(1).getBody());
    }

    @Test
    public void testCreateTicketByEmailWithoutOrderWhenExistsTicketWithOrder() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());

        String userEmailBody = "test";
        processMessage(createDefaultBeruMailMessageBuilder()
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());
    }

    @Test
    public void testCreateTicketByEmailWithOrderWhenExistsTicketWithoutOrder() throws MessagingException {
        String emailBody = new LogisticMailBodyBuilder().setOrderId(NOT_EXISTS_ORDER_ID).build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).build());

        processMessage(createDefaultBeruMailMessageBuilder()
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .build()
        );

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());

        assertTicketTypeAndService(tickets.get(0), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        assertSingleComment(tickets.get(0), InternalComment.FQN, htmls.hideQuotes(emailBody));

        assertTicketTypeAndService(tickets.get(1), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        assertSingleComment(tickets.get(1), InternalComment.FQN, DEFAULT_EML_BODY);

        assertNoLinkedRelation(tickets.get(1), tickets.get(0));
    }

    @Test
    public void testCreateTicketByEmailWhenExistsTicketWithOtherTitle() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());

        String newSubject = Randoms.string();
        processMessage(createDefaultBeruMailMessageBuilder()
                .setSubject(newSubject)
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .build()
        );

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());

        assertTicketTypeAndService(tickets.get(0), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        Assertions.assertEquals(DEFAULT_EMAIL_SUBJECT, tickets.get(0).getTitle());

        assertTicketTypeAndService(tickets.get(1), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        Assertions.assertEquals(newSubject, tickets.get(1).getTitle());

        assertNoLinkedRelation(tickets.get(1), tickets.get(0));
    }

    @Test
    public void testAddContactInCommentToExistingOpenTicket() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        String userEmailBody = new LogisticMailBodyBuilder()
                .setOrderId(DEFAULT_ORDER_ID)
                .build();
        processMessage(createDefaultBeruMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());

        Assertions.assertEquals(DEFAULT_EML_BODY, comments.get(0).getBody());
        Assertions.assertEquals(htmls.hideQuotes(userEmailBody), comments.get(1).getBody());

        TicketContactInComment contactInComment = (TicketContactInComment) comments.get(1);
        Assertions.assertEquals(DEFAULT_EML_SENDER_NAME, contactInComment.getUserName());
        Assertions.assertEquals(DEFAULT_EML_SENDER_EMAIL, contactInComment.getUserEmail());
    }

    @Test
    public void testAddContactInCommentByTicketNumber() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        processMessage(createDefaultBeruMailMessageBuilder()
                .setSubject(ticket.getTitle() + ", № " + ticket.getId())
                .newDeduplicationKey()
                .setBody(new LogisticMailBodyBuilder().build())
                .build()
        );

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
    }

    @Test
    public void testAddCommentToExistingOpenTicketWhenSubjectStartsWithRe() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        String userEmailBody = new LogisticMailBodyBuilder()
                .setOrderId(DEFAULT_ORDER_ID)
                .build();
        processMessage(createDefaultBeruMailMessageBuilder()
                .setSubject("rE: " + DEFAULT_EMAIL_SUBJECT)
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());
    }

    @Test
    public void testAddCommentToExistingOpenTicketWhenSubjectStartsWithFwd() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        String userEmailBody = new LogisticMailBodyBuilder()
                .setOrderId(DEFAULT_ORDER_ID)
                .build();
        processMessage(createDefaultBeruMailMessageBuilder()
                .setSubject("fWd: " + DEFAULT_EMAIL_SUBJECT)
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());
    }

    @Test
    public void testAddRelationToClosedTicket() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket ticket1 = getSingleLogisticSupportTicket();
        closeTicket(ticket1);

        String userEmailBody = new LogisticMailBodyBuilder()
                .setOrderId(DEFAULT_ORDER_ID)
                .build();
        processMessage(createDefaultBeruMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(userEmailBody)
                .build()
        );

        List<LogisticSupportTicket> tickets = getLogisticSupportTickets();
        Assertions.assertEquals(2, tickets.size());

        assertTicketTypeAndService(tickets.get(0), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        List<Comment> comments = getComments(tickets.get(0));
        Comment firstComment = Iterables.getFirst(comments, null);
        Assertions.assertNotNull(firstComment);
        Assertions.assertEquals(DEFAULT_EML_BODY, firstComment.getBody());

        assertTicketTypeAndService(tickets.get(1), BeruLogisticSupportTicket.FQN, BERU_LOGISTIC_SUPPORT_QUESTION);
        assertSingleComment(tickets.get(1), InternalComment.FQN, htmls.hideQuotes(userEmailBody));

        assertLinkedRelation(tickets.get(1), tickets.get(0));
    }

    @Test
    public void testReopenTicketByClient() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        resolveTicket(ticket);

        processMessage(createDefaultBeruMailMessageBuilder()
                .setFrom(DEFAULT_CLIENT_EMAIL)
                .newDeduplicationKey()
                .setBody(new LogisticMailBodyBuilder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .build())
                .build()
        );

        ticket = getSingleLogisticSupportTicket();
        Assertions.assertEquals(LogisticSupportTicket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Resolution.RESPONSE_FROM_DELIVERY_CLIENT, ticket.getResolution().getCode());
    }

    @Test
    public void testReopenTicketByDeliveryMessage() throws MessagingException {
        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        resolveTicket(ticket);

        DeliveryService deliveryService = orderTestUtils.createDeliveryService(Randoms.string());
        String email = Randoms.email();
        moduleDefaultTestUtils.createContact(deliveryService, List.of(Randoms.email(), email));

        processMessage(createDefaultBeruMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(new LogisticMailBodyBuilder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setClientEmail(null)
                        .build())
                .setFrom(email)
                .build());

        ticket = getSingleLogisticSupportTicket();
        Assertions.assertEquals(LogisticSupportTicket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Resolution.RESPONSE_FROM_DELIVERY_SERVICE, ticket.getResolution().getCode());
    }

    // TODO перенести тест в module_ticket (тестируется функциональность триггера, который расположен в module_ticket и
    // никак не зависит от обработки писем логистики)
    @Test
    public void testInvalidClientEmail() {
        String invalidEmail = "invalid e-mail";
        processMessage(beruMailMessageBuilder
                .setBody(new LogisticMailBodyBuilder()
                        .setClientEmail(invalidEmail)
                        .build()
                )
                .build()
        );
        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(1).getFqn());
        String commentBody = "В обращении указан некорректный e-mail адрес: &#39;" + invalidEmail + "&#39;. "
                + "Пожалуйста, поправьте его в поле &#39;Контактный e-mail&#39;.";
        Assertions.assertEquals(commentBody, comments.get(1).getBody());
    }

    @Test
    public void testEmptyClientEmail_fillClientEmailFromSender() {
        String emailBody = new LogisticMailBodyBuilder().setClientEmail(null).build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).build());

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        Assertions.assertEquals(DEFAULT_EML_SENDER_EMAIL, ticket.getClientEmail());
        assertSingleComment(ticket, InternalComment.FQN, htmls.hideQuotes(emailBody));
    }

    @Test
    public void testEmptyClientEmail_fillDeliveryService() {
        DeliveryService deliveryService = orderTestUtils.createDeliveryService(Randoms.string());
        String email = Randoms.email();
        moduleDefaultTestUtils.createContact(deliveryService, List.of(Randoms.email(), email));

        String emailBody = new LogisticMailBodyBuilder().setClientEmail(null).build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).setFrom(email).build());

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        Assertions.assertNull(ticket.getClientEmail());
        assertSingleComment(ticket, TicketContactInComment.FQN, htmls.hideQuotes(emailBody));
        Assertions.assertEquals(deliveryService.getGid(), ticket.getDeliveryService().getGid());
    }

    @Test
    public void testEmptyClientEmail_fillSortingCenter() {
        SortingCenter sortingCenter = orderTestUtils.createSortingCenter(Randoms.string());
        String email = Randoms.email();
        moduleDefaultTestUtils.createContact(sortingCenter, List.of(Randoms.email(), email));

        String emailBody = new LogisticMailBodyBuilder().setClientEmail(null).build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).setFrom(email).build());

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();

        Assertions.assertNull(ticket.getClientEmail());
        assertSingleComment(ticket, TicketContactInComment.FQN, htmls.hideQuotes(emailBody));
        Assertions.assertEquals(sortingCenter.getGid(), ticket.getSortingCenter().getGid());
    }

    @Test
    public void testReopenBeruComplaintsTicket() {
        Ticket beruComplaintsTicket = ticketTestUtils.createTicket(
                BeruComplaintsTicket.FQN,
                Map.of(
                        BeruComplaintsTicket.STATUS, BeruComplaintsTicket.STATUS_ON_HOLD,
                        BeruComplaintsTicket.SERVICE, createBeruComplaintsService()
                )
        );

        String emailBody = new LogisticMailBodyBuilder()
                .setBeruComplaintsTicket(beruComplaintsTicket)
                .build();
        processMessage(beruMailMessageBuilder.setBody(emailBody).build());

        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        Assertions.assertEquals(beruComplaintsTicket, ticket.getSourceTicket());

        resolveTicket(ticket, "beruLogisticSupportTestCategory");
        Assertions.assertEquals(BeruComplaintsTicket.STATUS_REOPENED, beruComplaintsTicket.getStatus());

        Assertions.assertNotNull(beruComplaintsTicket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_ADJACENT_DEPARTMENT,
                beruComplaintsTicket.getResolution().getCode());
    }

    private Service createBeruComplaintsService() {
        return ticketTestUtils.createService(Fqn.of("service$telephony"), Map.of(
                Service.SERVICE_TIME, "08_22",
                Service.SUPPORT_TIME, "08_22"
        ));
    }

    private void closeTicket(LogisticSupportTicket ticket) {
        resolveTicket(ticket);

        bcpService.edit(ticket, Map.of(
                LogisticSupportTicket.STATUS, LogisticSupportTicket.STATUS_CLOSED
        ));
    }

    private void resolveTicket(LogisticSupportTicket ticket) {
        resolveTicket(ticket, null);
    }

    private void resolveTicket(LogisticSupportTicket ticket, String category) {
        if (Ticket.STATUS_REGISTERED.equals(ticket.getStatus())) {
            bcpService.edit(ticket, Map.of(
                    LogisticSupportTicket.STATUS, LogisticSupportTicket.STATUS_PROCESSING
            ));
        }

        Map<String, Object> comment = Maps.of(
                TicketContactInComment.METACLASS, InternalComment.FQN,
                TicketContactInComment.BODY, "test resolve"
        );
        Map<String, Object> properties = Maps.of(
                LogisticSupportTicket.STATUS, LogisticSupportTicket.STATUS_RESOLVED,
                AddCommentOperationHandler.ID, comment
        );
        if (!CrmStrings.isNullOrBlank(category)) {
            properties.put("categories", category);
        }
        bcpService.edit(ticket, properties);
    }

    private void assertLinkedRelation(LogisticSupportTicket source, LogisticSupportTicket target) {
        assertLinkedRelationCount(source, target, 1);
    }

    private void assertNoLinkedRelation(LogisticSupportTicket source, LogisticSupportTicket target) {
        assertLinkedRelationCount(source, target, 0);
    }

    private void assertLinkedRelationCount(LogisticSupportTicket source, LogisticSupportTicket target, int count) {
        long actualCount = dbService.count(Query.of(LinkedRelation.FQN)
                .withFilters(
                        Filters.eq(LinkedRelation.SOURCE, source),
                        Filters.eq(LinkedRelation.TARGET, target)
                ));
        Assertions.assertEquals(count, actualCount);
    }

    private List<LogisticSupportTicket> getLogisticSupportTickets() {
        return dbService.list(Query.of(LogisticSupportTicket.FQN))
                .stream()
                .map(x -> (LogisticSupportTicket) x)
                .sorted(Comparator.comparing(LogisticSupportTicket::getGid))
                .collect(Collectors.toList());
    }

    private List<Comment> getComments(LogisticSupportTicket ticket) {
        return commentTestUtils.getComments(ticket)
                .stream()
                .sorted(Comparator.comparing(Comment::getGid))
                .collect(Collectors.toList());
    }

    private void assertSingleComment(LogisticSupportTicket ticket, Fqn fqn, String expectedBody) {
        List<Comment> comments = dbService.list(Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, ticket)));

        Assertions.assertEquals(1, comments.size());

        Comment comment = comments.get(0);
        Assertions.assertEquals(fqn, comment.getFqn());
        Assertions.assertEquals(expectedBody, comment.getBody());
    }
}
