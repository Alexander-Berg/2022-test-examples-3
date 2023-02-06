package ru.yandex.market.jmf.module.ticket.test;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeChannelService;
import ru.yandex.market.jmf.module.ticket.impl.EmployeeChannelServiceImpl;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class EmployeeChannelServiceTest {

    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;

    private Channel channelMail;
    private Channel channelPhone;
    private EmployeeChannelService employeeChannelService;
    private TicketTestUtils.TestContext context;


    @BeforeEach
    public void setUp() {
        employeeChannelService = new EmployeeChannelServiceImpl(bcpService);

        channelMail = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        channelPhone = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        context = ticketTestUtils.create();
    }

    @Test
    public void addUnavailableChannel() {
        Employee employee = getEmployee();

        employeeChannelService.markChannelUnavailable(employee, channelMail);
        assertUnavailableChannel(getEmployee(employee), channelMail);
    }

    @Test
    public void removeUnavailableChannel() {
        Employee employee = getEmployee();

        bcpService.edit(employee, Map.of(
                Employee.UNAVAILABLE_CHANNELS, Set.of(TestChannels.CH2, TestChannels.CH1)
        ));

        employeeChannelService.markChannelAvailable(employee, channelMail);
        assertUnavailableChannel(employee, channelPhone);
    }

    @Test
    public void isChannelUnavailable() {
        Employee employee = getEmployee();

        bcpService.edit(employee, Map.of(
                Employee.UNAVAILABLE_CHANNELS, Set.of(TestChannels.CH2)
        ));

        Assertions.assertFalse(employeeChannelService.isChannelAvailable(employee, channelPhone));
        Assertions.assertTrue(employeeChannelService.isChannelAvailable(employee, channelMail));
    }

    private void assertUnavailableChannel(Employee employee, Channel channel) {
        Set<Channel> unavailableChannels = employee.getUnavailableChannels();
        Assertions.assertEquals(1, unavailableChannels.size());
        Assertions.assertEquals(channel, unavailableChannels.iterator().next());
    }

    private Employee getEmployee(Employee employee) {
        return dbService.get(employee.getGid());
    }

    private Employee getEmployee() {
        return getEmployee(context.employee0);
    }
}
