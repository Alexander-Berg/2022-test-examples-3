package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.dto.CarDto;
import ru.yandex.market.logistics.les.dto.CourierDto;
import ru.yandex.market.logistics.les.dto.PersonDto;
import ru.yandex.market.logistics.les.dto.PhoneDto;
import ru.yandex.market.logistics.les.tpl.CourierReceivedPickupReturnEvent;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.exception.ModelResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@ParametersAreNonnullByDefault
@DisplayName("Обработка получения события: Курьер забрал возвратный заказ из ПВЗ")
class CourierReceivedPickupReturnEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    private static final long PARTNER_ID = 100;
    private static final long WAREHOUSE_ID = 200;
    private static final String BOX_EXTERNAL_ID = "box-external-id";
    private static final String WAREHOUSE_EXTERNAL_ID = "300";

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-03-04T11:12:13.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное создание таски следующего сегмента для СЦ")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup.xml")
    @ExpectedDatabase(
        value = "/database/les/courier-received-pickup-return/after/create_sc_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createScFromPvzTaskCreated() {
        mockLms();
        processTask(BOX_EXTERNAL_ID);
        verifyLms();
    }

    @Test
    @DisplayName("Повторное получение события, таска не создается")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup_sc_exists.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void scAlreadyExists() {
        mockLms();
        processTask(BOX_EXTERNAL_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("У ПВЗ уже есть маршрут в СЦ, но с другими параметрами")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup.xml")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/segments_with_different_shipments.xml")
    @ExpectedDatabase(
        value = "/database/les/courier-received-pickup-return/after/segments_with_different_shipments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void pickupHasScWithDifferentData() {
        mockLms();
        int differentScDataCasesAmount = 3;
        for (int i = 1; i <= differentScDataCasesAmount; i++) {
            processTask(BOX_EXTERNAL_ID + "-" + i);
        }

        verify(lmsClient, times(differentScDataCasesAmount)).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup_sc_exists.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("LMS вернул ошибку")
    void lmsErrorScExists() {
        doThrow(new RuntimeException("LMS error"))
            .when(lmsClient).getLogisticsPoints(logisticsPointFilter());

        softly.assertThatCode(() -> processTask(BOX_EXTERNAL_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LMS error");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("Ошибка при обращении к LMS, но обращений нет, так как сц нет у пвз")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup.xml")
    @ExpectedDatabase(
        value = "/database/les/courier-received-pickup-return/after/create_sc_task_created_lms_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void lmsErrorNoScExistsTaskCreated() {
        doReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(PARTNER_ID)
                .name("partner name")
                .build()
        ))
            .when(lmsClient)
            .getPartner(PARTNER_ID);
        doThrow(new RuntimeException("LMS error"))
            .when(lmsClient).getLogisticsPoints(logisticsPointFilter());
        softly.assertThatCode(() -> processTask(BOX_EXTERNAL_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LMS error");
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("Несколько идентификаторов партнёров СЦ")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/pickup.xml")
    @ExpectedDatabase(
        value = "/database/les/courier-received-pickup-return/after/create_multiple_sc_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sortingCenterPartnerIdsOverride() {
        mockLms();
        processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID, List.of(110L, 120L))));
        verifyLms();
    }

    @Test
    @DisplayName("Возврат курьером")
    @DatabaseSetup("/database/les/courier-received-pickup-return/before/courier.xml")
    @ExpectedDatabase(
        value = "/database/les/courier-received-pickup-return/after/courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void courierSegment() {
        mockLms();
        processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID)));
        verifyLms();
    }

    @Test
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName("Коробки с указанным в событии идентификатором не существует")
    void eventReceivedForNonExistingSegment() {
        OrderSearchFilter searchByBox = OrderSearchFilter.builder()
            .unitId(BOX_EXTERNAL_ID)
            .build();
        when(lomClient.searchOrders(searchByBox, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));
        OrderSearchFilter searchByBarcode = OrderSearchFilter.builder()
            .barcodes(Set.of(BOX_EXTERNAL_ID))
            .build();
        when(lomClient.searchOrders(searchByBarcode, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));

        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID))))
            .isInstanceOf(ModelResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN_BOX with id box-external-id");

        verify(lomClient).searchOrders(searchByBox, Pageable.unpaged());
        verify(lomClient).searchOrders(searchByBarcode, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Коробки с указанным в событии идентификатором не существует. "
            + "Заказ существует, найден по шк коробки, но невыкуп по нему не создавался"
    )
    void eventReceivedForNonExistingSegmentOrderExists() {
        OrderSearchFilter orderSearchFilter = OrderSearchFilter.builder()
            .unitId(BOX_EXTERNAL_ID)
            .build();
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto(), new OrderDto()),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID))))
            .doesNotThrowAnyException();

        verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Коробки с указанным в событии идентификатором не существует. "
            + "Заказ существует, найден по шк заказа, но невыкуп по нему не создавался"
    )
    void eventReceivedForNonExistingSegmentOrderFoundByBarcode() {
        OrderSearchFilter searchByBox = OrderSearchFilter.builder()
            .unitId(BOX_EXTERNAL_ID)
            .build();
        when(lomClient.searchOrders(searchByBox, Pageable.unpaged()))
            .thenReturn(PageResult.empty(Pageable.unpaged()));
        OrderSearchFilter searchByBarcode = OrderSearchFilter.builder()
            .barcodes(Set.of(BOX_EXTERNAL_ID))
            .build();
        when(lomClient.searchOrders(searchByBarcode, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto(), new OrderDto()),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID))))
            .doesNotThrowAnyException();

        verify(lomClient).searchOrders(searchByBox, Pageable.unpaged());
        verify(lomClient).searchOrders(searchByBarcode, Pageable.unpaged());
    }

    @Test
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName(
        "Коробки с указанным в событии идентификатором не существует. "
            + "Заказ существует, невыкуп по нему создавался в LRM"
    )
    void eventReceivedForNonExistingSegmentOrderReturnExists() {
        OrderSearchFilter orderSearchFilter = OrderSearchFilter.builder()
            .unitId(BOX_EXTERNAL_ID)
            .build();
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged()))
            .thenReturn(PageResult.of(
                List.of(new OrderDto().setReturnsIds(List.of(1L)), new OrderDto().setReturnsIds(List.of(2L))),
                2,
                1,
                2
            ));

        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(getEventPayload(BOX_EXTERNAL_ID))))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to find RETURN_BOX with id box-external-id. Returns in LOM exist: [1, 2]");

        verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    private void processTask(String boxExternalId) {
        processor.execute(getDbQueuePayload(getEventPayload(boxExternalId)));
    }

    @Nonnull
    private CourierReceivedPickupReturnEvent getEventPayload(String boxExternalId) {
        return getEventPayload(boxExternalId, List.of());
    }

    @Nonnull
    private CourierReceivedPickupReturnEvent getEventPayload(String boxExternalId, List<Long> sortingCenterPartnerIds) {
        return new CourierReceivedPickupReturnEvent(
            boxExternalId,
            100,
            sortingCenterPartnerIds,
            Instant.parse("2021-09-21T12:30:00Z"),
            new CourierDto(
                123L,
                234L,
                200L,
                new PersonDto("name", null, null),
                new PhoneDto("phone", null),
                new CarDto("number", null),
                null
            )
        );
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter() {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(PARTNER_ID))
            .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }

    private void mockLms() {
        when(lmsClient.getLogisticsPoints(logisticsPointFilter()))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .id(WAREHOUSE_ID)
                    .partnerId(PARTNER_ID)
                    .externalId(WAREHOUSE_EXTERNAL_ID)
                    .name("склад сц")
                    .build()
            ));
        doReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(PARTNER_ID)
                .name("partner name")
                .build()
        ))
            .when(lmsClient)
            .getPartner(PARTNER_ID);
    }

    private void verifyLms() {
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
        verify(lmsClient).getPartner(PARTNER_ID);
    }
}
