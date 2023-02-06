package ru.yandex.market.notification.notifications.question;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.notification.PeriodicNotificationDao;
import ru.yandex.market.notification.PeriodicNotifierExecutor;
import ru.yandex.market.notification.notifications.PeriodicNotification;
import ru.yandex.market.notification.notifications.question.QuestionReportNotificationDao.Row;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class QuestionReportNotificationTest extends FunctionalTest {
    public static final Set<String> IGNORED_ATTRIBUTES = Set.of("servant", "hostname");
    @Autowired
    private PeriodicNotification<QuestionReportNotificationData> questionReportNotification;

    @Autowired
    private PeriodicNotificationDao periodicNotificationDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("yqlNamedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate;

    @Test
    @DbUnitDataSet(before = "QuestionReportNotificationTest.before.csv")
    public void testNotifications() {
        mockYqlResult(List.of(
                new Row("157", 11570, 5),
                new Row("157", 11580, 1),
                new Row("159", 11590, 1)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(questionReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        var requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient, times(2)).sendNotification(requestCaptor.capture());

        List<SendNotificationRequest> requests = requestCaptor.getAllValues();

        SendNotificationRequest request1 = requests.get(0);
        assertThat(request1.getTypeId()).isEqualTo(1647268281L);
        assertThat(request1.getDestination()).isNotNull();
        assertThat(request1.getDestination().getBusinessId()).isEqualTo(157L);
        assertThat(request1.getRenderOnly()).isFalse();
        MbiAsserts.assertXmlEquals("<data><filterDate>2022-04-19</filterDate>" +
                "<displayDate>19 апреля</displayDate>" +
                "<businessData><shop-data><shop-name>TestShop</shop-name><campaign-id>1157</campaign-id>" +
                "<questions-count>5</questions-count></shop-data><shop-data><shop-name>Supplier 11580</shop-name>" +
                "<campaign-id>1158</campaign-id><questions-count>1</questions-count></shop-data></businessData></data>",
                request1.getData(), IGNORED_ATTRIBUTES);

        SendNotificationRequest request2 = requests.get(1);
        assertThat(request2.getTypeId()).isEqualTo(1647268281L);
        assertThat(request2.getDestination()).isNotNull();
        assertThat(request2.getDestination().getBusinessId()).isEqualTo(159L);
        assertThat(request2.getRenderOnly()).isFalse();
        MbiAsserts.assertXmlEquals("<data><filterDate>2022-04-19</filterDate>" +
                "<displayDate>19 апреля</displayDate>" +
                "<businessData><shop-data><shop-name>Supplier 11590</shop-name><campaign-id>1159</campaign-id>" +
                "<questions-count>1</questions-count></shop-data></businessData></data>",
                request2.getData(), IGNORED_ATTRIBUTES);
        verifyNoMoreInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "QuestionReportNotificationTest.before.csv")
    public void testNoNotificationsForEmptyQuestions() {
        mockYqlResult(List.of());

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(questionReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "QuestionReportNotificationTest.businessesIdsFilter.before.csv")
    public void testBusinessesIdsFilter() {
        mockYqlResult(List.of(
                new Row("157", 11570, 5),
                new Row("157", 11580, 1),
                new Row("159", 11590, 1)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(questionReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        var requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient).sendNotification(requestCaptor.capture());

        List<SendNotificationRequest> requests = requestCaptor.getAllValues();

        SendNotificationRequest request = requests.get(0);
        assertThat(request.getTypeId()).isEqualTo(1647268281L);
        assertThat(request.getDestination()).isNotNull();
        assertThat(request.getDestination().getBusinessId()).isEqualTo(157L);

        verifyNoMoreInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "QuestionReportNotificationTest.excludedBusinessesIdsFilter.before.csv")
    public void testExcludedBusinessesIdsFilter() {
        mockYqlResult(List.of(
                new Row("157", 11570, 5),
                new Row("157", 11580, 1),
                new Row("159", 11590, 1)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(questionReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        var requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient).sendNotification(requestCaptor.capture());

        List<SendNotificationRequest> requests = requestCaptor.getAllValues();

        SendNotificationRequest request = requests.get(0);
        assertThat(request.getTypeId()).isEqualTo(1647268281L);
        assertThat(request.getDestination()).isNotNull();
        assertThat(request.getDestination().getBusinessId()).isEqualTo(159L);

        verifyNoMoreInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "QuestionReportNotificationTest.noFilters.before.csv")
    public void testNoNotificationsWithoutFilters() {
        mockYqlResult(List.of(
                new Row("157", 11570, 5),
                new Row("157", 11580, 1),
                new Row("159", 11590, 1)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(questionReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    private void mockYqlResult(List<Row> rows) {
        when(yqlNamedParameterJdbcTemplate.query(
                anyString(), any(SqlParameterSource.class), any(ResultSetExtractor.class)
        )).thenReturn(rows);
    }

}
