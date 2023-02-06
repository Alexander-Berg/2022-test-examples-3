package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.ff.FulfilmentBoxItemsReceivedEvent;
import ru.yandex.market.logistics.les.ff.dto.BoxItemDto;
import ru.yandex.market.logistics.les.ff.enums.UnitCountType;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.exception.ModelResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.FulfilmentReceivedBoxMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@ParametersAreNonnullByDefault
@DisplayName("Обработка получения события вторичной приемки")
@DatabaseSetup("/database/les/ff-box-items-received/before/prepare.xml")
class FulfilmentBoxItemsReceivedProcessorTest extends AbstractIntegrationYdbTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-11-11T11:11:11.00Z");

    @Autowired
    private AsyncLesEventProcessor processor;

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private EntityMetaService entityMetaService;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @Test
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/database/les/ff-box-items-received/after/single_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successEventProcessing() {
        execute("box-with-ff-segment");

        checkFulfilmentEventSavedToYdb(3, "box-with-ff-segment");
    }

    @Test
    @DisplayName("Пропускаем старые возвраты по маске")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreOldBox() {
        execute("VOZVRAT_SF_PVZ_123");
    }

    @Test
    @DisplayName("Получение события о приемке на складе для сегмента со статусом IN")
    @DatabaseSetup(
        value = "/database/les/ff-box-items-received/before/ff_segment_already_in_in_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void repeatedEventProcessing() {
        execute("box-with-ff-segment");

        checkFulfilmentEventSavedToYdb(3, "box-with-ff-segment");
        softly.assertThat(backLogCaptor.getResults()).isEmpty();
    }

    @Test
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName("Коробки с указанным в событии идентификатором не существует")
    void eventReceivedForNonExistingSegment() {
        OrderSearchFilter searchByBox = OrderSearchFilter.builder()
            .unitId("non-existing-box-id")
            .build();
        when(lomClient.searchOrders(searchByBox, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));
        OrderSearchFilter searchByBarcode = OrderSearchFilter.builder()
            .barcodes(Set.of("non-existing-box-id"))
            .build();
        when(lomClient.searchOrders(searchByBarcode, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));

        softly.assertThatCode(() -> execute("non-existing-box-id"))
            .isInstanceOf(ModelResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN_BOX with id non-existing-box-id");

        verify(lomClient).searchOrders(searchByBox, Pageable.unpaged());
        verify(lomClient).searchOrders(searchByBarcode, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Коробки не существует. Заказ существует, найден по шк коробки, но невыкуп по нему не создавался")
    void eventReceivedForNonExistingSegmentOrderExists() {
        OrderSearchFilter orderSearchFilter = OrderSearchFilter.builder()
            .unitId("non-existing-box-id")
            .build();
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto(), new OrderDto()),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> execute("non-existing-box-id"))
            .doesNotThrowAnyException();

        verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Коробки не существует. Заказ существует, найден по шк заказа, но невыкуп по нему не создавался")
    void eventReceivedForNonExistingSegmentOrderFoundByBarcode() {
        OrderSearchFilter searchByBox = OrderSearchFilter.builder()
            .unitId("non-existing-box-id")
            .build();
        when(lomClient.searchOrders(searchByBox, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));
        OrderSearchFilter searchByBarcode = OrderSearchFilter.builder()
            .barcodes(Set.of("non-existing-box-id"))
            .build();
        when(lomClient.searchOrders(searchByBarcode, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto(), new OrderDto()),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> execute("non-existing-box-id"))
            .doesNotThrowAnyException();

        verify(lomClient).searchOrders(searchByBox, Pageable.unpaged());
        verify(lomClient).searchOrders(searchByBarcode, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName("Коробки не существует. Заказ существует, невыкуп по нему создавался в LRM")
    void eventReceivedForNonExistingSegmentOrderReturnExists() {
        OrderSearchFilter orderSearchFilter = OrderSearchFilter.builder()
            .unitId("non-existing-box-id")
            .build();
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto().setReturnsIds(List.of(1L)), new OrderDto().setReturnsIds(List.of(2L))),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> execute("non-existing-box-id"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to find RETURN_BOX with id non-existing-box-id. Returns in LOM exist: [1, 2]");

        verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    @Test
    @DisplayName("У коробки нет ФФ сегментов")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void noSegment() {
        softly.assertThatCode(() -> execute("box-with-no-ff-segments"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Box box-with-no-ff-segments must have at least one not cancelled FF segment");
    }

    @Test
    @DisplayName("У коробки несколько ФФ сегментов")
    @ExpectedDatabase(
        value = "/database/les/ff-box-items-received/after/multiple_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleSegments() {
        execute("box-multiple-ff-segments");

        checkFulfilmentEventSavedToYdb(2, "box-multiple-ff-segments");
    }

    @Test
    @DisplayName("Два FF сегмента. Один валидный, второй отмененный, должны обновить статус валидного")
    @DatabaseSetup(
        value = "/database/les/ff-box-items-received/before/ff_segment_cancelled.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/les/ff-box-items-received/after/single_valid_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalFFSegments() {
        execute("box-with-ff-segment");

        checkFulfilmentEventSavedToYdb(3, "box-with-ff-segment");
    }

    @Test
    @DisplayName("Успешная обработка события. Нашли коробку по баркоду заказа")
    @DatabaseSetup(
        value = "/database/les/ff-box-items-received/before/box_with_order_barcode.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/les/ff-box-items-received/after/single_segment_box_with_order_barcode.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successEventProcessingBoxFoundByOrderId() {
        execute("box-with-order-barcode");

        checkFulfilmentEventSavedToYdb(4, "box-with-order-barcode");
    }

    private void checkFulfilmentEventSavedToYdb(long boxId, String boxExternalId) {
        softly.assertThat(entityMetaService.get(
                new DetachedTypedEntity(EntityType.RETURN_BOX, boxId),
                FulfilmentReceivedBoxMeta.class
            ))
            .usingRecursiveComparison()
            .isEqualTo(expectedSavedMeta(boxExternalId));
    }

    @Nonnull
    private List<BoxItemDto> items() {
        return List.of(
            new BoxItemDto(
                12345L,
                "vendor-code-1",
                UnitCountType.DEFECT,
                List.of("1-attr", "2-attr"),
                Map.of("UIT", "123456789")
            ),
            new BoxItemDto(
                234567L,
                "vendor-code-2",
                UnitCountType.FIT,
                List.of(),
                Map.of("UIT", "987654321")
            )
        );
    }

    @Nonnull
    private FulfilmentReceivedBoxMeta expectedSavedMeta(String boxExternalId) {
        return FulfilmentReceivedBoxMeta.builder()
            .boxExternalId(boxExternalId)
            .orderExternalId("order-barcode")
            .ffRequestId(1L)
            .timestamp(FIXED_TIME)
            .warehousePartnerId(123L)
            .deliveryServicePartnerId(0L)
            .items(itemsMeta())
            .build();
    }

    @Nonnull
    private List<FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta> itemsMeta() {
        return List.of(
            FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta.builder()
                .supplierId(12345L)
                .vendorCode("vendor-code-1")
                .stock(FulfilmentReceivedBoxMeta.UnitCountType.DEFECT)
                .attributes(List.of("1-attr", "2-attr"))
                .instances(Map.of("UIT", "123456789"))
                .build(),
            FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta.builder()
                .supplierId(234567L)
                .vendorCode("vendor-code-2")
                .stock(FulfilmentReceivedBoxMeta.UnitCountType.FIT)
                .attributes(List.of())
                .instances(Map.of("UIT", "987654321"))
                .build()
        );
    }

    private void execute(String boxId) {
        processor.execute(getDbQueuePayload(new FulfilmentBoxItemsReceivedEvent(
            boxId,
            "order-barcode",
            1L,
            FIXED_TIME,
            123L,
            0L,
            items()
        )));
    }
}
