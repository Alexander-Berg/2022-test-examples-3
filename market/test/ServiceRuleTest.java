package ru.yandex.market.jmf.module.ticket.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.ServiceRule;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class ServiceRuleTest {
    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private DbService dbService;

    //Ошибка. У очереди и у категории разные бренды
    @Test
    public void categoryAndServiceHasDifferentBrand() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand());

        Assertions.assertThrows(ValidationException.class,
                () -> createServiceRule(ctx.service0, ch1, category, 10, ServiceRule.STATUS_ACTIVE));
    }

    //Ошибка. 2 одинаковых правила
    @Test
    public void tryAddTheSameRule() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ACTIVE);
        Assertions.assertThrows(ValidationException.class,
                () -> createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ACTIVE));
    }

    //Ошибка. Валидация при редактировании категории правила
    @Test
    public void editCategory() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());
        var category2 = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand());

        Entity rule = createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ACTIVE);

        Assertions.assertThrows(ValidationException.class,
                () -> bcpService.edit(rule, Map.of(ServiceRule.CATEGORY, category2)));
    }

    //Ошибка. Валидация при редактировании сервиса правила
    @Test
    public void editService() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());
        Service serviceWithDifferenceBrand = ticketTestUtils.createService(ctx.team0,
                ctx.serviceTime24x7, ticketTestUtils.createBrand(), Optional.empty());

        Entity rule = createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ACTIVE);

        Assertions.assertThrows(ValidationException.class,
                () -> bcpService.edit(rule, Map.of(ServiceRule.SERVICE,
                        serviceWithDifferenceBrand)));
    }

    //Одно подходящее правило в ServiceRule -> очередь меняется на очередь из правила
    @Test
    public void oneSuitableRule() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ACTIVE);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, category));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service1);
    }

    //Два подходящее правило в ServiceRule с одинаковым приоритетом -> очередь меняется на очередь из правила
    // добавленного позже
    @Test
    public void twoSuitableRules_creationTime() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service0.getBrand());
        var category2 = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service0, ch1, category, 10, ServiceRule.STATUS_ACTIVE);
        createServiceRule(ctx.service1, ch1, category2, 10, ServiceRule.STATUS_ACTIVE);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, List.of(category, category2)));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service1);
    }

    //Два подходящее правило в ServiceRule с разными приоритетами -> очередь меняется на очередь из правила с большим
    // приоритетом
    @Test
    public void twoSuitableRules_priority() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());
        var category2 = ticketTestUtils.createTicketCategory(ctx.service0.getBrand());

        createServiceRule(ctx.service1, ch1, category, 20, ServiceRule.STATUS_ACTIVE);
        createServiceRule(ctx.service0, ch1, category2, 10, ServiceRule.STATUS_ACTIVE);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, List.of(category, category2)));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service1);
    }

    //Ни одно правило не подходит по категории -> очередь не меняется
    @Test
    public void unsuitableRule_category() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());
        var category2 = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service1, ch1, category2, 10, ServiceRule.STATUS_ACTIVE);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, category));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service0);
    }

    //Ни одно правило не подходит по каналу -> очередь не меняется
    @Test
    public void unsuitableRule_channel() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service1, ch2, category, 10, ServiceRule.STATUS_ACTIVE);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, category));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service0);
    }

    //Правило не подходит т.к в архиве -> очередь не меняется
    @Test
    public void unsuitableRule_archived() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        Channel ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        createServiceRule(ctx.service1, ch1, category, 10, ServiceRule.STATUS_ARCHIVED);

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, category));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service0);
    }

    //Нет ни одно правила -> очередь не меняется
    @Test
    public void dontExistAnyRules() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        final var ticketGid = createTicket(ctx.service0, TestChannels.CH1).getGid();
        var category = ticketTestUtils.createTicketCategory(ctx.service1.getBrand());

        bcpService.edit(ticketGid, Map.of(Ticket.CATEGORIES, category));

        Ticket ticket = dbService.get(ticketGid);
        Assertions.assertEquals(ticket.getService(), ctx.service0);
    }

    private Entity createServiceRule(Service service, Channel channel, TicketCategory category,
                                     Integer priority, String status) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ServiceRule.SERVICE, service);
        attributes.put(ServiceRule.CHANNEL, channel);
        attributes.put(ServiceRule.CATEGORY, category);
        attributes.put(ServiceRule.PRIORITY, priority);
        attributes.put(ServiceRule.STATUS, status);
        return createEntity(ServiceRule.FQN, attributes);
    }

    private Entity createTicket(Service service, String channel) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("title", Randoms.string());
        attributes.put(Ticket.SERVICE, service);
        attributes.put(Ticket.CHANNEL, channel);
        return createEntity(TicketTestConstants.TICKET_TEST_FQN, attributes);
    }

    private Entity createEntity(Fqn fqn, Map<String, Object> attributes) {
        return bcpService.create(fqn, attributes);
    }
}
