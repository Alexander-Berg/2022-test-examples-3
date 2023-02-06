package ru.yandex.market.deepmind.common.services.statuses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thymeleaf.spring5.SpringTemplateEngine;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.AlmostDeadstockStatusRepository;
import ru.yandex.market.deepmind.common.services.DeepmindMailSenderHelper;
import ru.yandex.market.deepmind.common.services.statuses.pojo.AlmostDeadstockEmailInfo;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.AlmostDeadstockMeta;
import ru.yandex.market.deepmind.common.services.tracker_strategy.AlmostDeadstockStrategy;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketChangeType;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawDataHistory;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataHistoryRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mboc.common.services.mail.EmailService;

public class AlmostDeadstockMailSenderTest extends DeepmindBaseDbTestClass {
    @Resource
    private AlmostDeadstockStatusRepository repository;
    @Resource
    private TrackerApproverTicketRepository ticketRepository;
    @Resource
    private TrackerApproverDataHistoryRepository dataHistoryRepository;

    private AlmostDeadstockMailSender mailSender;
    private StorageKeyValueServiceMock keyValService;
    private DeepmindMailSenderHelper mailHelperSpy;

    @Before
    public void setUp() {
        keyValService = new StorageKeyValueServiceMock();
        keyValService.putValue(AlmostDeadstockMailSender.ALMOST_DEADSTOCK_MAIL_ENABLED, true);
        mailHelperSpy = Mockito.spy(new DeepmindMailSenderHelper(keyValService, Mockito.mock(EmailService.class)));
        mailSender = new AlmostDeadstockMailSender(repository, mailHelperSpy, new SpringTemplateEngine());
    }

    @Test
    public void testSendEmailsEmpty() {
        AtomicInteger count = new AtomicInteger();
        var processedLogins = new ArrayList<String>();
        Mockito.doAnswer(inv -> {
            var address = (String) inv.getArgument(0);
            var login = address.substring(0, address.indexOf("@"));
            if (count.get() <= 1) {
                processedLogins.add(login);
                count.incrementAndGet();
                return inv.callRealMethod();
            } else {
                throw new RuntimeException("Test email service error");
            }
        }).when(mailHelperSpy).sendMail(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString());
        mailSender.sendEmails();
        Assertions
            .assertThat(processedLogins).isEmpty();
    }

    @Test
    public void testSendEmails() {
        prepareData();
        AtomicInteger count = new AtomicInteger();
        var processedLogins = new ArrayList<String>();
        Mockito.doAnswer(inv -> {
            var address = (String) inv.getArgument(0);
            var login = address.substring(0, address.indexOf("@"));
            if (count.get() <= 1) {
                processedLogins.add(login);
                count.incrementAndGet();
                return inv.callRealMethod();
            } else {
                throw new RuntimeException("Test email service error");
            }
        }).when(mailHelperSpy).sendMail(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString());
        mailSender.sendEmails();
        Assertions
            .assertThat(processedLogins)
            .containsExactly("pavel");
    }

    @Test
    public void testInfosForOnlyPreviousWeekShouldBeSelected() {
        prepareData();
        var tickets = ticketRepository.findAll();
        Assertions.assertThat(tickets).hasSize(6);
        var infosMap = repository.findInfosToEmail();
        Assertions.assertThat(infosMap.keySet()).containsExactly("pavel");
        Assertions.assertThat(infosMap.values()).containsExactly(List.of(
            new AlmostDeadstockEmailInfo().setTicket("TEST-3").setShopSkuCount(5),
            new AlmostDeadstockEmailInfo().setTicket("TEST-4").setShopSkuCount(10))
        );
    }

    private void prepareData() {
        // rows for earlier than previous week
        createTicket("TEST-1", "petr", 7, Instant.now().minus(20, ChronoUnit.DAYS), TicketState.NEW);
        createTicket("TEST-2", "petr", 9, Instant.now().minus(20, ChronoUnit.DAYS), TicketState.NEW);

        // rows for previous week
        createTicket("TEST-3", "pavel", 5, Instant.now().minus(7, ChronoUnit.DAYS), TicketState.NEW);
        createTicket("TEST-4", "pavel", 10, Instant.now().minus(7, ChronoUnit.DAYS), TicketState.NEW);

        // rows for current week
        createTicket("TEST-5", "ivan", 3, Instant.now(), TicketState.NEW);
        createTicket("TEST-6", "ivan", 9, Instant.now(), TicketState.NEW);
    }

    public void createTicket(String ticket, String login, int sskuCount, Instant modifiedTs, TicketState state) {
        ticketRepository.save(new TrackerApproverTicketRawStatus()
            .setTicket(ticket)
            .setMeta(JsonWrapper.of(new AlmostDeadstockMeta().setAssignee(login)))
            .setState(state)
            .setType(AlmostDeadstockStrategy.TYPE)
            .setModifiedTs(modifiedTs)
        );

        for (int i = 1; i <= sskuCount; i++) {
            dataHistoryRepository.save(new TrackerApproverRawDataHistory()
                .setChangeType(TicketChangeType.INSERT)
                .setState(TicketState.NEW)
                .setTicket(ticket)
                .setKey(JsonWrapper.of(ticket + i))
                .setType(AlmostDeadstockStrategy.TYPE)
                .setModifiedTs(modifiedTs)
            );
        }
    }
}
