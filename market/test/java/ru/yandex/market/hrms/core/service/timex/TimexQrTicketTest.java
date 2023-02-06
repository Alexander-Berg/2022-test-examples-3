package ru.yandex.market.hrms.core.service.timex;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.timex.ticket.TimexQrTicketService;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.ComponentsClient;
import ru.yandex.startrek.client.QueuesClient;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.UsersClient;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.Queue;
import ru.yandex.startrek.client.model.User;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class TimexQrTicketTest extends AbstractCoreTest {

    @Autowired
    private TimexQrTicketService timexQrTicketService;

    @MockBean
    @Autowired
    private Session session;

    @SpyBean
    @MockBean
    @Autowired
    private StartrekService startrekService;

    @Captor
    private ArgumentCaptor<IssueCreate> captor;


    @BeforeEach
    public void init() {
        ComponentsClient componentsMock = Mockito.mock(ComponentsClient.class);
        Component component = Mockito.mock(Component.class);
        QueuesClient queuesMock = Mockito.mock(QueuesClient.class);
        Queue queue = Mockito.mock(Queue.class);
        UsersClient userMock = Mockito.mock(UsersClient.class);
        User user = Mockito.mock(User.class);
        when(component.getDisplay()).thenReturn("hrms");
        when(component.getName()).thenReturn("hrms");
        when(componentsMock.get(116524L)).thenReturn(component);
        when(session.components()).thenReturn(componentsMock);
        when(session.queues()).thenReturn(queuesMock);
        when(session.users()).thenReturn(userMock);
        when(queue.getKey()).thenReturn("TESTACVS");
        when(queuesMock.get("TESTACVS")).thenReturn(queue);
        when(userMock.get("gjmrd")).thenReturn(user);
        var ticketMock = Mockito.mock(StartrekTicket.class);
        when(ticketMock.getKey()).thenReturn("TESTACVS-123");
        when(startrekService.createTicket(captor.capture())).thenReturn(ticketMock);
    }

    @Test
    @DbUnitDataSet(before = "TimexQrTicketTest.outstaff.before.csv", after = "TimexQrTicketTest.after.csv")
    public void ticketShouldBeCreatedForOutstaff() {

        timexQrTicketService.create(123L);
            var values = captor.getValue().getValues();
        assertThat(values.getO("operationDate").get(), Matchers.is("2022-06-16"));
        assertThat(values.getO("description").get(), Matchers.is("""
                                Был напечатан QR код для сотрудника
                                ФИО: васильев константин петрович,
                                Логин на стаффе: ,
                                Должность: Оператор прт,
                                Площадка: СЦ Екатеринбург,
                                Дата активации: 2022-06-16
                                """));
        assertThat(values.getO("queue").get(), Matchers.is("TESTACVS"));
        assertThat(values.getO("tags").get(), Matchers.is(new String[] { "QR" }));
        assertThat(values.getO("components"), Matchers.hasSize(1));
        assertThat(((long[])values.getO("components").get())[0], Matchers.is(116524L));
    }

    @Test
    @DbUnitDataSet(before = "TimexQrTicketTest.staff.before.csv", after = "TimexQrTicketTest.after.csv")
    public void ticketShouldBeCreatedForStaff() {
        timexQrTicketService.create(123L);
        var values = captor.getValue().getValues();
        assertThat(values.getO("operationDate").get(), Matchers.is("2022-06-16"));
        assertThat(values.getO("description").get(), Matchers.is("""
                                Был напечатан QR код для сотрудника
                                ФИО: Магомедов Гаджимурад,
                                Логин на стаффе: gjmrd,
                                Должность: Кладовщик,
                                Площадка: Софьино,
                                Дата активации: 2022-06-16
                                """));
        assertThat(values.getO("queue").get(), Matchers.is("TESTACVS"));
        assertThat(values.getO("tags").get(), Matchers.is(new String[] { "QR" }));
        assertThat(values.getO("components"), Matchers.hasSize(1));
        assertThat(((long[])values.getO("components").get())[0], Matchers.is(116524L));
    }
}
