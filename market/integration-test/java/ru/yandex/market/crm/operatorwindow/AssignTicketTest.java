package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategory;
import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategoryPriority;
import ru.yandex.market.crm.operatorwindow.utils.CategoryTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

@Transactional
public class AssignTicketTest extends AbstractModuleOwTest {

    private static final Fqn BERU_OUTGOING_TICKET_FQN = Fqn.of("ticket$beruOutgoing");
    private static final String TEST_CATEGORY_CODE = "testCategoryCode";
    private static final Long FIRST_USER_UID = 123L;
    private static final Long SECOND_USER_UID = 124L;

    @Inject
    private BcpService bcpService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private CategoryTestUtils categoryTestUtils;

    @BeforeEach
    public void setup() {
        categoryTestUtils.createCategoryPriority(TEST_CATEGORY_CODE, 110);
    }

    @Test
    public void outgoingTicketIsCreatedByOneEmployee__afterThatAnotherEmployeeTakeThisTicket__waitResponsibleEmployeeAttibuteIsUpdated() {
        final Ou organizationUnit = ticketTestUtils.createOu();
        final Employee employee1 = ticketTestUtils.createEmployee(
                organizationUnit,
                FIRST_USER_UID);
        final Employee employee2 = ticketTestUtils.createEmployee(
                organizationUnit,
                SECOND_USER_UID);

        final TicketCategoryPriority categoryPriority = categoryTestUtils.getCategoryPriority(TEST_CATEGORY_CODE);
        final TicketCategory ticketCategory = categoryTestUtils.createTicketCategory(Brands.BERU, categoryPriority);

        securityDataService.setInitialEmployee(employee1);
        final Ticket ticket = ticketTestUtils.createTicket(
                BERU_OUTGOING_TICKET_FQN,
                Map.of(
                        BeruTicket.CATEGORIES, List.of(ticketCategory),
                        "@comment", Map.of(
                                Comment.METACLASS, InternalComment.FQN,
                                Comment.BODY, "commentWhichIsRequiredForResolvedStatus")));
        Assertions.assertEquals(1, ticket.getResponsibleEmployees().size());
        Assertions.assertEquals(FIRST_USER_UID, ticket.getResponsibleEmployees().iterator().next().getUid());


        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_REOPENED
        ));

        securityDataService.setInitialEmployee(employee2);
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));
        Assertions.assertEquals(2, ticket.getResponsibleEmployees().size());
        Assertions.assertTrue(ticket.getResponsibleEmployees()
                .stream()
                .anyMatch(x -> SECOND_USER_UID.equals(x.getUid())));
    }

    @Test
    public void outgoingTicketIsCreatedByOneEmployee__afterThatTheSameEmployeeTakeThisTicket__waitResponsibleEmployeesDoesNotContainDuplicates() {
        final Ou organizationUnit = ticketTestUtils.createOu();
        final Employee employee1 = ticketTestUtils.createEmployee(
                organizationUnit,
                FIRST_USER_UID);

        final TicketCategoryPriority categoryPriority = categoryTestUtils.getCategoryPriority(TEST_CATEGORY_CODE);
        final TicketCategory ticketCategory = categoryTestUtils.createTicketCategory(Brands.BERU, categoryPriority);

        securityDataService.setInitialEmployee(employee1);
        final Ticket ticket = ticketTestUtils.createTicket(
                BERU_OUTGOING_TICKET_FQN,
                Map.of(
                        BeruTicket.CATEGORIES, List.of(ticketCategory),
                        "@comment", Map.of(
                                Comment.METACLASS, InternalComment.FQN,
                                Comment.BODY, "commentWhichIsRequiredForResolvedStatus")));
        Assertions.assertEquals(1, ticket.getResponsibleEmployees().size());
        Assertions.assertEquals(FIRST_USER_UID, ticket.getResponsibleEmployees().iterator().next().getUid());


        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_REOPENED
        ));

        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));
        Assertions.assertEquals(1, ticket.getResponsibleEmployees().size());
    }
}
