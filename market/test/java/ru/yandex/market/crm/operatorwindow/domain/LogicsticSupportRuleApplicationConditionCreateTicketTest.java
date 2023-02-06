package ru.yandex.market.crm.operatorwindow.domain;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.operatorwindow.domain.logistic.LogicsticSupportRuleApplicationCondition;
import ru.yandex.market.crm.operatorwindow.jmf.catalog.ListProcessingMode;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportRuleCreate;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.TicketTag;
import ru.yandex.market.jmf.utils.html.Htmls;
import ru.yandex.market.jmf.utils.html.SafeUrlService;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderStatus;


@ExtendWith(MockitoExtension.class)
public class LogicsticSupportRuleApplicationConditionCreateTicketTest {
    private static final LogicsticSupportRuleApplicationCondition condition =
            new LogicsticSupportRuleApplicationCondition(new Htmls(Mockito.mock(SafeUrlService.class)));

    private static final char NBSP = (char) 160;

    @Mock
    private LogisticSupportTicket ticket;

    @Mock
    private LogisticSupportRuleCreate rule;

    @Test
    public void conditionSatisfiedByTicketStatus() {
        Mockito.when(rule.getTicketStatusConditionValue())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(ticket.getStatus())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByTicketStatus() {
        Mockito.when(rule.getTicketStatusConditionValue())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(ticket.getStatus())
                .thenReturn(Ticket.STATUS_RESOLVED);
        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByDescription() {
        Mockito.when(rule.getDescriptionContainsConditionValue())
                .thenReturn(List.of("one", "two", "three", "four"));
        Mockito.when(ticket.getDescription())
                .thenReturn("Test OnE world");
        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByDescription__ignoreNonBreakingWhitespace() {
        Mockito.when(rule.getDescriptionContainsConditionValue())
                .thenReturn(List.of("one" + NBSP + "World"));
        Mockito.when(ticket.getDescription())
                .thenReturn("Test OnE world");
        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByDescription() {
        Mockito.when(rule.getDescriptionContainsConditionValue())
                .thenReturn(List.of("one", "two", "three", "four"));
        Mockito.when(ticket.getDescription())
                .thenReturn("Hello world!");
        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedBySubject() {
        Mockito.when(rule.getSubjectContainsConditionValue())
                .thenReturn(List.of("one", "two", "three", "four"));
        Mockito.when(ticket.getTitle())
                .thenReturn("Test OnE world");
        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedBySubject__ignoreNonBreakingWhitespace() {
        Mockito.when(rule.getSubjectContainsConditionValue())
                .thenReturn(List.of("one" + NBSP + "World"));
        Mockito.when(ticket.getTitle())
                .thenReturn("Test OnE world");
        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedBySubject() {
        Mockito.when(rule.getSubjectContainsConditionValue())
                .thenReturn(List.of("one", "two", "three", "four"));
        Mockito.when(ticket.getTitle())
                .thenReturn("Hello world!");
        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByTags() {
        final Set<CatalogItem> ruleTags = Set.of(
                getTag("1"),
                getTag("2"),
                getTag("3")
        );
        Mockito.when(rule.getTagsOneOfConditionValue())
                .thenReturn(ruleTags);

        final Set<TicketTag> ticketTags = Set.of(
                getTag("0"),
                getTag("2"),
                getTag("4")
        );
        Mockito.when(ticket.getTags())
                .thenReturn(ticketTags);

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByTags() {
        final Set<CatalogItem> ruleTags = Set.of(
                getTag("1"),
                getTag("2"),
                getTag("3")
        );
        Mockito.when(rule.getTagsOneOfConditionValue())
                .thenReturn(ruleTags);

        final Set<TicketTag> ticketTags = Set.of(
                getTag("4"),
                getTag("5"),
                getTag("6")
        );
        Mockito.when(ticket.getTags())
                .thenReturn(ticketTags);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByDeliveryServices() {
        final Set<DeliveryService> ruleDeliveryServices = Set.of(
                getDeliveryService("1", false),
                getDeliveryService("2"),
                getDeliveryService("3", false));
        Mockito.when(rule.getDeliveryServiceConditionValue())
                .thenReturn(ruleDeliveryServices);

        final DeliveryService ticketDeliveryService = getDeliveryService("2");
        Mockito.when(ticket.getDeliveryService())
                .thenReturn(ticketDeliveryService);

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByDeliveryServices() {
        final Set<DeliveryService> ruleDeliveryServices = Set.of(
                getDeliveryService("1"),
                getDeliveryService("2"),
                getDeliveryService("3"));
        Mockito.when(rule.getDeliveryServiceConditionValue())
                .thenReturn(ruleDeliveryServices);

        final DeliveryService ticketDeliveryService = getDeliveryService("4");
        Mockito.when(ticket.getDeliveryService())
                .thenReturn(ticketDeliveryService);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByAnyOfCategory() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("0"),
                getTicketCategory("2"),
                getTicketCategory("4"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        // начальное заполнение условия обработки списка категорий - ANY
        // будет выполнено миграцией
        // сущностей с null быть не должно
        // для сущностей с null в режиме обработки списка категорий
        // не считаем что правило логистической поддержки выполнено
        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByAnyOfCategory() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("4"),
                getTicketCategory("5"),
                getTicketCategory("6"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByAnyOfCategoryAndExplicitlyDefinedListProcessingMode() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);
        final ListProcessingMode listProcessingMode =
                getListProcessingMode(ListProcessingMode.ANY_OF_THE_CONDITIONS_ARE_MET);
        Mockito.when(rule.getCategoryListProcessingMode())
                .thenReturn(listProcessingMode);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("0"),
                getTicketCategory("2"),
                getTicketCategory("4"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByAnyOfCategoryAndExplicitlyDefinedListProcessingMode() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);
        final ListProcessingMode listProcessingMode =
                getListProcessingMode(ListProcessingMode.ANY_OF_THE_CONDITIONS_ARE_MET);
        Mockito.when(rule.getCategoryListProcessingMode())
                .thenReturn(listProcessingMode);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("4"),
                getTicketCategory("5"),
                getTicketCategory("6"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByAllOfCategoriesAndExplicitlyDefinedListProcessingMode() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);
        final ListProcessingMode listProcessingMode = getListProcessingMode(ListProcessingMode.ALL_CONDITIONS_ARE_MET);
        Mockito.when(rule.getCategoryListProcessingMode())
                .thenReturn(listProcessingMode);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("0"),
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"),
                getTicketCategory("4"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByAllOfCategoriesAndExplicitlyDefinedListProcessingMode() {
        final Set<TicketCategory> ruleCategories = Set.of(
                getTicketCategory("1"),
                getTicketCategory("2"),
                getTicketCategory("3"));
        Mockito.when(rule.getCategoriesOneOfConditionValue())
                .thenReturn(ruleCategories);
        final ListProcessingMode listProcessingMode = getListProcessingMode(ListProcessingMode.ALL_CONDITIONS_ARE_MET);
        Mockito.when(rule.getCategoryListProcessingMode())
                .thenReturn(listProcessingMode);

        final Set<TicketCategory> ticketCategories = Set.of(
                getTicketCategory("0"),
                getTicketCategory("1"),
                getTicketCategory("3"));
        Mockito.when(ticket.getCategories())
                .thenReturn(ticketCategories);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByOrderStatus() {
        final Set<OrderStatus> ruleOrderStatuses = Set.of(
                getOrderStatus("status1", false),
                getOrderStatus("status2"),
                getOrderStatus("status3", false));
        Mockito.when(rule.getOrderStatusConditionValue())
                .thenReturn(ruleOrderStatuses);

        Order order = getOrder("status2");
        Mockito.when(ticket.getOrder())
                .thenReturn(order);

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByOrderStatus() {
        final Set<OrderStatus> ruleOrderStatuses = Set.of(
                getOrderStatus("status1"),
                getOrderStatus("status2"),
                getOrderStatus("status3"));
        Mockito.when(rule.getOrderStatusConditionValue())
                .thenReturn(ruleOrderStatuses);

        Order order = getOrder("status4");
        Mockito.when(ticket.getOrder())
                .thenReturn(order);

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionSatisfiedByTicketStatusAndDescription() {
        Mockito.when(rule.getTicketStatusConditionValue())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(rule.getDescriptionContainsConditionValue())
                .thenReturn(List.of("one", "two", "three"));

        Mockito.when(ticket.getStatus())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(ticket.getDescription())
                .thenReturn("foo two bar");

        Assertions.assertTrue(condition.isSatisfied(rule, ticket, null));
    }

    @Test
    public void conditionNotSatisfiedByTicketStatusAndDescription() {
        Mockito.when(rule.getTicketStatusConditionValue())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(rule.getDescriptionContainsConditionValue())
                .thenReturn(List.of("one", "two", "three"));

        Mockito.when(ticket.getStatus())
                .thenReturn(Ticket.STATUS_PROCESSING);
        Mockito.when(ticket.getDescription())
                .thenReturn("hello world");

        Assertions.assertFalse(condition.isSatisfied(rule, ticket, null));
    }

    private Order getOrder(String status) {
        OrderStatus orderStatus = getOrderStatus(status);

        final Order obj = Mockito.mock(Order.class);
        Mockito.when(obj.getStatus())
                .thenReturn(orderStatus);
        return obj;
    }

    private OrderStatus getOrderStatus(String code) {
        return getOrderStatus(code, true);
    }

    private OrderStatus getOrderStatus(String code, boolean mockGetCode) {
        final OrderStatus obj = Mockito.mock(OrderStatus.class);
        if (mockGetCode) {
            Mockito.when(obj.getCode()).thenReturn(code);
        }
        return obj;
    }

    private TicketCategory getTicketCategory(String code) {
        final TicketCategory obj = Mockito.mock(TicketCategory.class);
        Mockito.when(obj.getCode())
                .thenReturn(code);
        return obj;
    }

    private TicketTag getTag(String code) {
        final TicketTag obj = Mockito.mock(TicketTag.class);
        Mockito.when(obj.getCode())
                .thenReturn(code);
        return obj;
    }

    private DeliveryService getDeliveryService(String code) {
        return getDeliveryService(code, true);
    }

    private DeliveryService getDeliveryService(String code, boolean mockGetDeliveryService) {
        final DeliveryService obj = Mockito.mock(DeliveryService.class);
        if (mockGetDeliveryService) {
            Mockito.when(obj.getCode())
                    .thenReturn(code);
        }
        return obj;
    }

    private ListProcessingMode getListProcessingMode(String code) {
        final ListProcessingMode obj = Mockito.mock(ListProcessingMode.class);
        Mockito.when(obj.getCode())
                .thenReturn(code);
        return obj;
    }
}
