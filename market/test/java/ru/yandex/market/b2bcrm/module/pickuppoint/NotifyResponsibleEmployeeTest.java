package ru.yandex.market.b2bcrm.module.pickuppoint;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.html.Encoding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTestConfig;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.chat.Ticket;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.PublicComment;
import ru.yandex.market.jmf.module.comment.operations.AddCommentOperationHandler;
import ru.yandex.market.jmf.module.notification.Notification;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.utils.html.SafeUrlService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@B2bPickupPointTests
@ContextConfiguration(classes = {B2bPickupPointTestConfig.class, NotifyResponsibleEmployeeTest.Config.class})
public class NotifyResponsibleEmployeeTest {

    public static final String BRAND = "b2bPickupPointSupport";

    private static final String LINK =
            "<a href=\"https://SBA_HOST/https://HOST/entity/%s\" target=\"_blank\" rel=\"noopener noreferrer\">%d</a>";

    @Inject
    private OuTestUtils ouTestUtils;

    @Inject
    private EmployeeTestUtils employeeTestUtils;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private MockSecurityDataService securityDataService;

    @Inject
    private DbService dbService;

    @Inject
    private BcpService bcpService;

    @Inject
    private SafeUrlService safeUrlService;

    private Ou ou;

    private Employee employee;

    @BeforeEach
    public void setUp() throws Exception {
        ou = ouTestUtils.createOu();
        employee = createAndSetCurrentEmployee();
        when(safeUrlService.toSafeUrl(anyString())).then(inv -> "https://SBA_HOST/" + inv.getArgument(0));
    }

    @Test
    public void shouldNotifyResponsibleEmployee() throws Exception {
        createAndSetCurrentEmployee();
        PickupPointTicket ticket = createTicket(BRAND);
        bcpService.edit(ticket, Map.of(Ticket.TITLE, "someOtherTitle"));
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Notification.FQN)))
                .hasSize(2)
                .anyHasAttributes(
                        Notification.EMPLOYEE, employee.getGid(),
                        Notification.MESSAGE, "В обращении " + getLink(ticket) + " изменились атрибуты: Название"
                )
                .anyHasAttributes(
                Notification.EMPLOYEE, employee.getGid(),
                Notification.MESSAGE, "В обращении " + getLink(ticket) + " изменились атрибуты: Канал поступления"
        );
    }

    @Test
    public void shouldNotNotifyResponsibleEmployeeWithSameEmployee() {
        bcpService.edit(createTicket(BRAND), Map.of(Ticket.TITLE, "someOtherTitle"));
        assertThat(dbService.list(Query.of(Notification.FQN))).hasSize(0);
    }

    @Test
    public void shouldNotNotifyResponsibleEmployeeWithOtherBrand() {
        createAndSetCurrentEmployee();
        ticketTestUtils.createBrand("otherBrand");
        bcpService.edit(createTicket("otherBrand"), Map.of(Ticket.TITLE, "someOtherTitle"));
        assertThat(dbService.list(Query.of(Notification.FQN))).hasSize(0);
    }

    @Test
    public void shouldNotifyResponsibleEmployeeOnComment() throws Exception {
        createAndSetCurrentEmployee();
        PickupPointTicket ticket = createTicket(BRAND);
        bcpService.edit(ticket, Map.of(AddCommentOperationHandler.ID, Map.of(
                Comment.METACLASS, PublicComment.FQN,
                Comment.BODY, "text"
        )));
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Notification.FQN)))
                .hasSize(2)
                .anyHasAttributes(
                        Notification.EMPLOYEE, employee.getGid(),
                        Notification.MESSAGE, "Новый комментарий в обращении " + getLink(ticket)
                )
                .anyHasAttributes(
                        Notification.EMPLOYEE, employee.getGid(),
                        Notification.MESSAGE, "В обращении " + getLink(ticket) + " изменились атрибуты: Канал поступления"
                );
    }

    private PickupPointTicket createTicket(String brand) {
        return ticketTestUtils.createTicket(PickupPointTicket.FQN, Map.of(
                Ticket.BRAND, brand,
                Ticket.RESPONSIBLE_EMPLOYEE, employee
        ));
    }

    private Employee createAndSetCurrentEmployee() {
        Employee employee = employeeTestUtils.createEmployee(ou);
        securityDataService.setInitialEmployee(employee);
        return employee;
    }

    private String getLink(PickupPointTicket ticket) throws IOException {
        StringBuilder encodedGid = new StringBuilder();
        Encoding.encodeRcdataOnto(ticket.getGid(), encodedGid);
        return String.format(LINK, encodedGid, ticket.getTicketNumber());
    }

    @Configuration
    public static class Config {
        @Bean
        @Primary
        public SafeUrlService mockSafeUrlService() {
            return Mockito.mock(SafeUrlService.class);
        }
    }
}
