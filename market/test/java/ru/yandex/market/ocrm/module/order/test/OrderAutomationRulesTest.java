package ru.yandex.market.ocrm.module.order.test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.trigger.EntityEvent;
import ru.yandex.market.jmf.trigger.TriggerConstants;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.impl.event.OrderEventImpl;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class OrderAutomationRulesTest {

    private static final String PATH = "/automation_rules/attributeChangedRule.json";

    @Inject
    protected AutomationRuleTestUtils automationRuleTestUtils;
    @Inject
    OrderTestUtils orderTestUtils;
    @Inject
    DbService dbService;
    @Inject
    TriggerServiceImpl triggerService;
    @Inject
    EntityAdapterService entityAdapterService;
    @Inject
    OuTestUtils ouTestUtils;

    @BeforeEach
    void setUp() {
        ouTestUtils.createOu();
    }

    @AfterEach
    public void tearDown() {
        automationRuleTestUtils.deleteAll();
    }

    @Test
    public void conditionTrue() {
        Entity entity = dbService.get("configuration@1");
        automationRuleTestUtils.createApprovedEventRule(
                entity,
                PATH,
                "importOrder",
                Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu())
        );
        Order order = orderTestUtils.createOrder(Map.of(Order.BUYER_EMAIL, "bla@e1.ru"));

        EntityEvent entityEvent = new EntityEvent(
                order.getMetaclass(),
                TriggerConstants.IMPORTED,
                order,
                null
        );

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(1L);
        Entity orderEvent = entityAdapterService.wrap(new OrderEventImpl(Optional.of(orderHistoryEvent)));
        entityEvent.addVariable("event", orderEvent);

        triggerService.execute(entityEvent);

        Assertions.assertEquals("blabla@e1.ru", order.getBuyerEmail());
    }
}
