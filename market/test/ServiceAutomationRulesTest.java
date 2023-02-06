package ru.yandex.market.jmf.module.ticket.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class ServiceAutomationRulesTest {

    private static final String PATH = "/automation_rules/attributeChangedRule.json";

    private static final Fqn TEST_FQN = Fqn.of("ticket$test");
    @Inject
    private AutomationRuleTestUtils automationRuleTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private CommentTestUtils commentTestUtils;
    @Inject
    private OuTestUtils ouTestUtils;

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
        Service service = ticketTestUtils.createService24x7();
        createEventRuleForEditTicketGroup(service, PATH);
        Ticket ticket = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.SERVICE, service
        ));
        bcpService.edit(ticket, Map.of(
                Ticket.TITLE, "automationRule",
                Ticket.DESCRIPTION, "start"
        ));
        assertComment(ticket, "true");
    }

    @Test
    public void conditionFalse() {
        Service service = ticketTestUtils.createService24x7();
        createEventRuleForEditTicketGroup(service, PATH);
        Ticket ticket = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.SERVICE, service,
                Ticket.TITLE, "automationRule0"
        ));
        bcpService.edit(ticket, Map.of(
                Ticket.TITLE, "automationRule",
                Ticket.DESCRIPTION, "start"
        ));
        assertComment(ticket, "false");
    }

    @Test
    public void changeServiceWithRule() {
        Service service1 = ticketTestUtils.createService24x7();
        Service service2 = ticketTestUtils.createService24x7();
        createEventRuleForEditTicketGroup(service2, PATH);
        Ticket ticket = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.SERVICE, service1
        ));
        bcpService.edit(ticket, Map.of(
                Ticket.TITLE, "automationRule",
                Ticket.SERVICE, service2,
                Ticket.DESCRIPTION, "start"
        ));
        assertComment(ticket, "true");
    }

    @Test
    public void changeServiceWithoutRule() {
        Service service1 = ticketTestUtils.createService24x7();
        Service service2 = ticketTestUtils.createService24x7();
        createEventRuleForEditTicketGroup(service1, PATH);
        Ticket ticket = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.SERVICE, service1
        ));
        bcpService.edit(ticket, Map.of(
                Ticket.TITLE, "automationRule",
                Ticket.SERVICE, service2,
                Ticket.DESCRIPTION, "start"
        ));
        assertEquals(0, commentTestUtils.getComments(ticket).size());
    }

    private void assertComment(Entity entity, String comment) {
        List<Comment> comments = commentTestUtils.getComments(entity);
        assertEquals(1, comments.size());
        assertEquals(comment, comments.get(0).getBody());
    }

    private void createEventRuleForEditTicketGroup(Entity entity, String configPath) {
        automationRuleTestUtils.createEventRuleForEditTicketGroup(entity, configPath,
                Set.of(), Set.of(ouTestUtils.getAnyCreatedOu()));
    }
}
