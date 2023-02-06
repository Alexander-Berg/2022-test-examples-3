package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.notification.PeriodicNotificationDao;
import ru.yandex.market.notification.PeriodicNotifierExecutor;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "claimLostOrdersNotificationTest.before.csv")
class ClaimLostOrdersNotificationTest extends FunctionalTest {

    @Autowired
    OrderService orderService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PeriodicNotificationDao dao;

    @Autowired
    NotificationService notificationService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private ClaimLostOrdersNotification notification;
    private CronNotificationSchedule schedule;
    private Clock clock;
    private static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");
    private static final Instant NOW = ZonedDateTime.parse("2022-02-10T05:30:00+03:00").toInstant();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void init() throws ParseException {
        schedule = new CronNotificationSchedule("0 0 0 1/3 * ? *", MSK_ZONE);
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        notification = new ClaimLostOrdersNotification(schedule, orderService, clock);
    }


    @Test
    @DisplayName("Проверка запуска шедулера раз в 3 дня")
    void testNextNotificationTimeAfter() {
        Instant nextTime = schedule.getNextNotificationTimeAfter(NOW);
        assertThat(nextTime.atZone(MSK_ZONE)).isEqualTo(ZonedDateTime.parse("2022-02-13T00:00:00+03:00"));

        Instant nextNextTime = schedule.getNextNotificationTimeAfter(nextTime);
        assertThat(nextNextTime.atZone(MSK_ZONE)).isEqualTo(ZonedDateTime.parse("2022-02-16T00:00:00+03:00"));
    }

    @Test
    @DisplayName("Проверка что получаемые в джобе данные соответствуют можели для преобразования")
    void testGetNotificationContext() throws JsonProcessingException {
        var partnerIds = notification.getPartnerIds();
        var expectedData = Map.of(
                1000001L, "{" +
                        "\"dateStart\":\"07.02.2022\"," +
                        "\"dateEnd\":\"10.02.2022\"," +
                        "\"campaignId\":21421810," +
                        "\"orderIds\":[26093216,26093222]" +
                        "}",
                1000002L, "{" +
                        "\"dateStart\":\"07.02.2022\"," +
                        "\"dateEnd\":\"10.02.2022\"," +
                        "\"campaignId\":21421814," +
                        "\"orderIds\":[26093227,26093229]" +
                        "}"
        );

        for (var id : partnerIds) {
            var data = notification.getPartnerNotification(id).orElseThrow().getData();
            assertThat(MAPPER.writeValueAsString(data.get(0))).isEqualTo(expectedData.get(id));
        }
    }

    @Test
    @DisplayName("Проверка отработки джобы - отправка уведомлений")
    void testCreateNotifications() {
        // given
        var executor = new PeriodicNotifierExecutor(
                List.of(notification),
                dao,
                notificationService,
                transactionTemplate,
                clock
        );

        // when
        executor.doJob(null);

        // then
        var reqCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient, times(2)).sendNotification(reqCaptor.capture());
        var contents = reqCaptor.getAllValues();
        assertThat(contents.stream().map(SendNotificationRequest::getData)).satisfiesExactly(
                req -> assertThat(req).contains("<claimData>" +
                        "<dateStart>07.02.2022</dateStart>" +
                        "<dateEnd>10.02.2022</dateEnd>" +
                        "<campaignId>21421814</campaignId>" +
                        "<orders><orderId>26093227</orderId><orderId>26093229</orderId></orders>" +
                        "</claimData>"),
                req -> assertThat(req).contains("<claimData>" +
                        "<dateStart>07.02.2022</dateStart>" +
                        "<dateEnd>10.02.2022</dateEnd>" +
                        "<campaignId>21421810</campaignId>" +
                        "<orders><orderId>26093216</orderId><orderId>26093222</orderId></orders>" +
                        "</claimData>")
        );
    }
}
