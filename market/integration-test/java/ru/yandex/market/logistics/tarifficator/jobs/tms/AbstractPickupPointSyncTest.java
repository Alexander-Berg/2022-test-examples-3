package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSortedMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.model.QueueType;
import ru.yandex.market.logistics.tarifficator.util.PayloadFactory;
import ru.yandex.market.logistics.tarifficator.util.QueueTasksTestUtil;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.mockito.Mockito.mock;

abstract class AbstractPickupPointSyncTest extends AbstractContextualTest {

    private static final long INPUT_ROWS_LIMIT = 1000000;
    private static final long OUTPUT_ROWS_LIMIT = 1000000;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    static final String SELECT_LAST_UPDATED_POINTS_QUERY = "lms_id, delivery_service_id, " +
        "delivery_service_outlet_code, region_id, exact_region_id, is_active, max_weight, max_width, max_length, " +
        "max_height, max_sides_sum, updated_at " +
        "FROM [//home/local/yt_outlet] " +
        "WHERE updated_at >= '2020-03-05T21:05:17.8102' " +
        "ORDER BY updated_at " +
        "LIMIT 3";

    final JobExecutionContext context = mock(JobExecutionContext.class);

    @Nonnull
    CompletableFuture<UnversionedRowset> createResponse(List<UnversionedRow> pointRows) {
        TableSchema schema = createTableSchema();
        CompletableFuture<UnversionedRowset> completableFuture = new CompletableFuture<>();
        completableFuture.complete(new UnversionedRowset(schema, pointRows));
        return completableFuture;
    }

    @Nonnull
    UnversionedRow newPointRow(
        long deliveryServiceId,
        long pointId,
        long locationId,
        long exactRegionId,
        boolean active
    ) {
        int columnIndex = -1;
        return new UnversionedRow(List.of(
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, pointId),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, deliveryServiceId),
            new UnversionedValue(++columnIndex, ColumnValueType.STRING, false, getPointCode(pointId)),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, locationId),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, exactRegionId),
            new UnversionedValue(++columnIndex, ColumnValueType.BOOLEAN, false, active),
            new UnversionedValue(++columnIndex, ColumnValueType.DOUBLE, false, pointId * 1.5D),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, pointId * 8),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, pointId * 6),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, pointId * 2),
            new UnversionedValue(++columnIndex, ColumnValueType.INT64, false, pointId * pointId),
            new UnversionedValue(++columnIndex, ColumnValueType.STRING, false, getUpdatedAt(pointId))
        ));
    }

    @Nonnull
    protected SelectRowsRequest getSelectRowsRequest(String query) {
        return SelectRowsRequest.of(query)
            .setInputRowsLimit(INPUT_ROWS_LIMIT)
            .setOutputRowsLimit(OUTPUT_ROWS_LIMIT);
    }

    @Nonnull
    protected SelectRowsRequest getSelectRowsRequestForPointsAfterIdQuery(long id) {
        return getSelectRowsRequest(getPointsAfterIdQuery(id));
    }

    @Nonnull
    protected String getPointsAfterIdQuery(long id) {
        return "lms_id, delivery_service_id, delivery_service_outlet_code, region_id, exact_region_id, " +
            "is_active, max_weight, max_width, max_length, max_height, max_sides_sum, updated_at " +
            "FROM [//home/local/yt_outlet] " +
            "WHERE lms_id > " + id + " " +
            "ORDER BY lms_id " +
            "LIMIT 3";
    }

    @Nonnull
    private static TableSchema createTableSchema() {
        return new TableSchema.Builder()
            .addKey("lms_id", ColumnValueType.INT64)
            .addAll(getColumnsSchema())
            .build();
    }

    @Nonnull
    private static List<ColumnSchema> getColumnsSchema() {
        return List.of(
            new ColumnSchema("delivery_service_id", ColumnValueType.INT64),
            new ColumnSchema("delivery_service_outlet_code", ColumnValueType.STRING),
            new ColumnSchema("region_id", ColumnValueType.INT64),
            new ColumnSchema("exact_region_id", ColumnValueType.INT64),
            new ColumnSchema("is_active", ColumnValueType.BOOLEAN),
            new ColumnSchema("max_weight", ColumnValueType.DOUBLE),
            new ColumnSchema("max_width", ColumnValueType.INT64),
            new ColumnSchema("max_length", ColumnValueType.INT64),
            new ColumnSchema("max_height", ColumnValueType.INT64),
            new ColumnSchema("max_sides_sum", ColumnValueType.INT64),
            new ColumnSchema("updated_at", ColumnValueType.STRING)
        );
    }

    @Nonnull
    private byte[] getPointCode(long pointId) {
        return String.format("code-%d", pointId).getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    private byte[] getUpdatedAt(long pointId) {
        return String.format("2020-03-05 10:15:18.8%d", pointId).getBytes(StandardCharsets.UTF_8);
    }

    void assertGenerationScheduledForPriceLists(Set<Long> changedPriceListIds) {
        QueueTasksTestUtil.assertQueueTasks(
            softly,
            objectMapper,
            jdbcTemplate,
            ImmutableSortedMap.of(
                QueueType.GENERATE_REVISION,
                PayloadFactory.createGenerateRevisionPayload(Set.of(), changedPriceListIds, 1)
            )
        );
    }

    void assertNoGenerationScheduled() {
        QueueTasksTestUtil.assertQueueTasks(softly, objectMapper, jdbcTemplate, ImmutableSortedMap.of());
    }
}
