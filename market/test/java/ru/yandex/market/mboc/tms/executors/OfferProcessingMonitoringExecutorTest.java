package ru.yandex.market.mboc.tms.executors;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.offers.model.Offer.MappingDestination;
import ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingDestination.BLUE;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingDestination.WHITE;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_CLASSIFICATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_MODERATION;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.COUNT_METRIC;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.MONITORED_OFFER_DESTINATIONS;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.MONITORED_PROCESSING_STATUSES;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.OFFER_DESTINATION_TAG;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.PERCENTILE_TAG;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.PROCESSING_STATUS_TAG;
import static ru.yandex.market.mboc.tms.executors.OfferProcessingMonitoringExecutor.TIME_METRIC;

public class OfferProcessingMonitoringExecutorTest {
    private static final String YT_TABLE = "/test/table/";

    private SolomonPushService solomonPushService;
    private NamedParameterJdbcTemplate yqlJdbcTemplate;
    private OfferProcessingMonitoringExecutor executor;

    @Before
    public void setUp() {
        solomonPushService = mock(SolomonPushService.class);
        yqlJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        executor = new OfferProcessingMonitoringExecutor(solomonPushService, yqlJdbcTemplate, YT_TABLE);
    }

    @Test
    public void pushesCollectedMetricsToSolomon() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        doReturn(true, true, true, true, false)
                .when(resultSet).next();
        doReturn(WHITE.name(), WHITE.name(), BLUE.name())
                .when(resultSet).getString(eq("offer_destination"));
        doReturn(IN_CLASSIFICATION.name(), IN_MODERATION.name(), IN_MODERATION.name())
                .when(resultSet).getString(eq("processing_status"));
        doReturn(100L, 200L, 300L)
                .when(resultSet).getLong(eq("offer_count"));
        doReturn(195.5D, 295.5D, 395.5D)
                .when(resultSet).getDouble(eq("max"));
        doReturn(190.5D, 290.5D, 390.5D)
                .when(resultSet).getDouble(eq("p90"));
        doReturn(180.5D, 280.5D, 380.5D)
                .when(resultSet).getDouble(eq("p80"));
        doReturn(170.5D, 270.5D, 370.5D)
                .when(resultSet).getDouble(eq("p70"));
        doReturn(150.5D, 250.5D, 350.5D)
                .when(resultSet).getDouble(eq("p50"));

        doAnswer(invocation -> {
            var rch = (RowCallbackHandler) invocation.getArgument(2);
            rch.processRow(resultSet);
            return null;
        }).when(yqlJdbcTemplate).query(anyString(), ArgumentMatchers.<MapSqlParameterSource>argThat(params -> {
            return requireNonNull(params.getValue("offer_destinations"))
                    .equals(MONITORED_OFFER_DESTINATIONS)
                    && requireNonNull(params.getValue("processing_statuses"))
                    .equals(MONITORED_PROCESSING_STATUSES);
        }), any(RowCallbackHandler.class));

        executor.execute();

        @SuppressWarnings("unchecked")
        var sensorsCaptor = (ArgumentCaptor<List<Sensor>>) (Object) ArgumentCaptor.forClass(List.class);
        verify(solomonPushService).push(sensorsCaptor.capture());
        var sensors = sensorsCaptor.getValue();
        sensors.forEach(sensor -> {
            switch (MappingDestination.valueOf(sensor.labels.get(OFFER_DESTINATION_TAG))) {
                case WHITE:
                    switch (ProcessingStatus.valueOf(sensor.labels.get(PROCESSING_STATUS_TAG))) {
                        case IN_CLASSIFICATION:
                            assertSensor(sensor, 100);
                            break;
                        case IN_MODERATION:
                            assertSensor(sensor, 200);
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
                case BLUE:
                    switch (ProcessingStatus.valueOf(sensor.labels.get(PROCESSING_STATUS_TAG))) {
                        case IN_MODERATION:
                            assertSensor(sensor, 300);
                            break;
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        });
    }

    private void assertSensor(Sensor sensor, int base) {
        switch (sensor.labels.get(SolomonPushService.SENSOR_TAG)) {
            case COUNT_METRIC:
                assertThat(sensor.value).isEqualTo(base);
                break;
            case TIME_METRIC:
                assertPercentile(sensor, base);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void assertPercentile(Sensor sensor, int base) {
        String metric = sensor.labels.get(PERCENTILE_TAG);
        switch (metric) {
            case "max":
                assertThat(sensor.value).isCloseTo(base + 95.5, withPercentage(0.999));
                break;
            case "p90":
                assertThat(sensor.value).isCloseTo(base + 90.5, withPercentage(0.999));
                break;
            case "p80":
                assertThat(sensor.value).isCloseTo(base + 80.5, withPercentage(0.999));
                break;
            case "p70":
                assertThat(sensor.value).isCloseTo(base + 70.5, withPercentage(0.999));
                break;
            case "p50":
                assertThat(sensor.value).isCloseTo(base + 50.5, withPercentage(0.999));
                break;
            default:
                throw new AssertionError("Unknown metric " + metric);
        }
    }
}
