package ru.yandex.market.logistics.lrm.les.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.lom.OrderBox;
import ru.yandex.market.logistics.les.lom.OrderBoxesChangedEvent;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.LocalsConfiguration;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Обработка изменения коробок заказа в LOM")
class LomOrderBoxesChangedProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private AsyncLesEventProcessor processor;
    @Autowired
    private LomClient lomClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private UuidGenerator uuidGenerator;

    @Test
    @DisplayName("Невыкуп не найден")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/no_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationNotFound() {
        execute("non-existent");
    }

    @Test
    @DisplayName("Два невыкупа по заказу")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/two_cancellations.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/no_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void twoCancellations() {
        softly.assertThatThrownBy(() -> execute("same-order"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expecting exactly one cancellation for order same-order");
    }

    @Test
    @DisplayName("Коробка с FAKEPARCEL")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/common.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/existing_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fakeParcel() {
        execute("order-id", "FAKEPARCEL-123456-789-0");
    }

    @Test
    @DisplayName("Коробка уже существует")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/common.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/existing_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existingBox() {
        execute("order-id", "box-external-id");
    }

    @Test
    @DisplayName("Коробка создана")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/common.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/new_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void newBox() throws Exception {
        executeBoxCreated("order-id", "box-external-id", "new-box-id");
    }

    @Test
    @DisplayName("Коробка была, потом была удалена, а теперь снова создана")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/box_removed_before.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/new_box_removed_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newBoxRemovedBefore() throws Exception {
        executeBoxCreated("order-id", "box-external-id", "box-removed-before");
    }

    @Test
    @DisplayName("Коробка удалена")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/two_boxes.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/box_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void removeBox() {
        execute("order-id", "box-external-id");
    }

    @Test
    @DisplayName("Коробка уже была удалена ранее")
    @DatabaseSetup("/database/les/lom-order-boxes-changed/before/box_removed_before.xml")
    @ExpectedDatabase(
        value = "/database/les/lom-order-boxes-changed/after/after_remove_removed_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyRemovedBox() {
        execute("order-id", "box-external-id");
    }

    @Nonnull
    private AutoCloseable mockGetOrder(String barcode, OrderDto order) {
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .barcodes(Set.of(barcode))
            .build();
        when(lomClient.searchOrders(filter, Pageable.unpaged())).thenReturn(PageResult.of(List.of(order), 1, 1, 1));
        return () -> verify(lomClient).searchOrders(filter, Pageable.unpaged());
    }

    @Nonnull
    private AutoCloseable mockLogisticPoints(Long... ids) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder().ids(Set.of(ids)).build();
        when(lmsClient.getLogisticsPoints(filter))
            .thenReturn(
                Arrays.stream(ids)
                    .map(id ->
                        LogisticsPointResponse.newBuilder()
                            .id(id)
                            .externalId("point-external-" + id)
                            .name("point-name-" + id)
                            .build()
                    )
                    .toList()
            );
        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    @Nonnull
    private AutoCloseable mockPartners(Long... ids) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(Set.of(ids)).build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(
                Arrays.stream(ids)
                    .map(id ->
                        PartnerResponse.newBuilder()
                            .id(id)
                            .partnerType(PartnerType.SORTING_CENTER)
                            .build()
                    )
                    .toList()
            );
        return () -> verify(lmsClient).searchPartners(filter);
    }

    private void executeBoxCreated(String barcode, String... boxIds) throws Exception {
        OrderDto order = new OrderDto()
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.SORTING_CENTER)
                    .partnerId(100L)
                    .warehouseLocation(LocationDto.builder().warehouseId(1000L).build())
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.SORTING_CENTER)
                    .partnerId(200L)
                    .warehouseLocation(LocationDto.builder().warehouseId(2000L).build())
                    .build()
            ));
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString(LocalsConfiguration.TEST_UUID),
            UUID.fromString(LocalsConfiguration.TEST_UUID2)
        );

        try (
            var ignored1 = mockGetOrder(barcode, order);
            var ignored2 = mockLogisticPoints(1000L, 2000L);
            var ignored3 = mockPartners(100L, 200L)
        ) {
            execute(barcode, boxIds);
        }
    }

    private void execute(String barcode, String... boxIds) {
        processor.execute(LesEventFactory.getDbQueuePayload(new OrderBoxesChangedEvent(
            "",
            barcode,
            Arrays.stream(boxIds)
                .map(OrderBox::new)
                .toList()
        )));
    }
}
