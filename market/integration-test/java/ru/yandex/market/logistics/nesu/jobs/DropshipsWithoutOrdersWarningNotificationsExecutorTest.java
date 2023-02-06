package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.DropshipsWithoutOrdersWarningNotificationsExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup({
    "/jobs/executors/dropship_without_orders_warning_notification/shop.xml",
    "/jobs/executors/dropship_without_orders_warning_notification/shop_partner_settings.xml",
    "/jobs/executors/dropship_without_orders_warning_notification/shop_notification.xml",
})
@DisplayName("Отправка писем дропшип-магазинам о необходимости смены точки сдачи в связи с отсутствием заказов")
public class DropshipsWithoutOrdersWarningNotificationsExecutorTest extends AbstractContextualTest {

    private static final String YQL_QUERY = "select * from `shop-dropoff-last-order-date` "
        + "where DateTime::MakeDatetime(DateTime::ParseIso8601(movement_updated)) "
        + "< CurrentUtcDatetime() - DateTime::IntervalFromDays(15) "
        + "and ("
        + "last_order_date is null "
        + "or DateTime::MakeDatetime(DateTime::ParseIso8601(last_order_date)) "
        + "< CurrentUtcDatetime() - DateTime::IntervalFromDays(15)"
        + ")";

    private static final LogisticsPointFilter LOGISTICS_POINT_FILTER_1 = LogisticsPointFilter.newBuilder()
        .type(PointType.WAREHOUSE)
        .partnerIds(Set.of(10L))
        .active(true)
        .build();

    private static final LogisticsPointFilter LOGISTICS_POINT_FILTER_2 = LogisticsPointFilter.newBuilder()
        .type(PointType.WAREHOUSE)
        .partnerIds(Set.of(20L))
        .active(true)
        .build();

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SendNotificationToShopProducer sendNotificationToShopProducer;

    @Autowired
    private DropshipsWithoutOrdersWarningNotificationsExecutor dropshipsWithoutOrdersWarningNotificationsExecutor;

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @BeforeEach
    void setup() {
        when(lmsClient.getLogisticsPoints(LOGISTICS_POINT_FILTER_1))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .partnerId(10L)
                    .name("Самый лучший склад")
                    .build()
            ));

        when(lmsClient.getLogisticsPoints(LOGISTICS_POINT_FILTER_2))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .partnerId(20L)
                    .name("Самый лучший склад 2")
                    .build()
            ));

        doNothing().when(sendNotificationToShopProducer)
            .produceTask(anyInt(), anyLong(), anyLong(), isNotNull());

    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, sendNotificationToShopProducer);
    }

    @Test
    @DisplayName("Успешный сценарий")
    void success() {
        doReturn(List.of(
            Map.of(
                "shop_id", 1L,
                "partner_id", 10L
            ),
            Map.of(
                "shop_id", 2L,
                "partner_id", 20L
            )
        ))
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);

        dropshipsWithoutOrdersWarningNotificationsExecutor.doJob(null);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("2 partners found")
            .contains("Processing partners [10]")
            .contains("Processing partners [20]");

        verify(lmsClient).getLogisticsPoints(LOGISTICS_POINT_FILTER_1);
        verify(lmsClient).getLogisticsPoints(LOGISTICS_POINT_FILTER_2);

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(1624962241),
            eq(1L),
            eq(10L),
            xmlCaptor.capture()
        );
        ArgumentCaptor<String> xmlCaptor2 = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(1624962241),
            eq(2L),
            eq(20L),
            xmlCaptor2.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent(
                "jobs/executors/dropship_without_orders_warning_notification/notification_data.xml"
            ));
        softly.assertThat(xmlCaptor2.getValue())
            .isXmlEqualTo(extractFileContent(
                "jobs/executors/dropship_without_orders_warning_notification/notification_data_2.xml"
            ));
    }

    @Test
    @DisplayName("Не найдено ни одного магазина")
    void noShopsFound() {
        doReturn(List.of())
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);

        dropshipsWithoutOrdersWarningNotificationsExecutor.doJob(null);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("0 partners found");
    }
}
