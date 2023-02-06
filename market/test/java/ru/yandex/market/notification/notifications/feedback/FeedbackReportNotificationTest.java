package ru.yandex.market.notification.notifications.feedback;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

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
import ru.yandex.market.notification.PeriodicNotificationDao;
import ru.yandex.market.notification.PeriodicNotifierExecutor;
import ru.yandex.market.notification.notifications.PeriodicNotification;
import ru.yandex.market.notification.notifications.feedback.FeedbackReportNotificationData.Record;
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

public class FeedbackReportNotificationTest extends FunctionalTest {
    @Autowired
    private PeriodicNotification<FeedbackReportNotificationData> feedbackReportNotification;

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
    @DbUnitDataSet(before = "FeedbackReportNotificationTest.before.csv")
    public void testNotificationsForNonEmptyGrades() {
        mockYqlResult(List.of(
                new Record(11570, 5, 2),
                new Record(11580, 3, 0)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(feedbackReportNotification),
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
        assertThat(request1.getTypeId()).isEqualTo(1644411076L);
        assertThat(request1.getDestination()).isNotNull();
        assertThat(request1.getDestination().getShopId()).isEqualTo(11570L);

        SendNotificationRequest request2 = requests.get(1);
        assertThat(request2.getTypeId()).isEqualTo(1644411076L);
        assertThat(request2.getDestination()).isNotNull();
        assertThat(request2.getDestination().getShopId()).isEqualTo(11580L);

        verifyNoMoreInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "FeedbackReportNotificationTest.before.csv")
    public void testNoNotificationsForZeroGrades() {
        mockYqlResult(List.of(new Record(11570, 0, 0),
                new Record(11580, 0, 0)
                ));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(feedbackReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "FeedbackReportNotificationTest.partnersIdsFilter.before.csv")
    public void testPartnersIdsFilter() {
        mockYqlResult(List.of(
                new Record(11570, 5, 2),
                new Record(11580, 3, 0)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(feedbackReportNotification),
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
        assertThat(request.getTypeId()).isEqualTo(1644411076L);
        assertThat(request.getDestination()).isNotNull();
        assertThat(request.getDestination().getShopId()).isEqualTo(11570L);

        verifyNoMoreInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "FeedbackReportNotificationTest.noFilters.before.csv")
    public void testNoNotificationsWithoutFilters() {
        mockYqlResult(List.of(
                new Record(11570, 5, 2),
                new Record(11580, 3, 0)));

        Instant now = ZonedDateTime.parse("2021-02-04T14:59:59+03:00").toInstant();

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        Executor executor = new PeriodicNotifierExecutor(
                List.of(feedbackReportNotification),
                periodicNotificationDao,
                notificationService,
                transactionTemplate,
                clock
        );

        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    private void mockYqlResult(List<Record> records) {
        when(yqlNamedParameterJdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenReturn(records);
    }


}
