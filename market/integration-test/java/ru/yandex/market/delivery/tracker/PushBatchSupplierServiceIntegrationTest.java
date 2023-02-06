package ru.yandex.market.delivery.tracker;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.dto.PartnerCreatedBatchesDescription;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.delivery.tracker.domain.model.ResourceId;
import ru.yandex.market.delivery.tracker.domain.model.request.PushOrderStatusesRequest;
import ru.yandex.market.delivery.tracker.service.logger.PushOrderStatusLogger;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventLogData;
import ru.yandex.market.delivery.tracker.service.tracking.batching.supplier.PushBatchSupplierService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent.PUSH_RECEIVED;

/**
 * Тест для {@link PushBatchSupplierService}.
 */
class PushBatchSupplierServiceIntegrationTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeliveryTrackDao deliveryTrackDao;

    @Autowired
    private PushOrderStatusLogger pushOrderStatusLogger;

    @Test
    @DatabaseSetup("/database/states/batches/supply/push/before_supply_push_batches.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/supply/push/after_supply_push_batches.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyPushBatches() throws Exception {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(List.of(1L, 2L, 3L), List.of(1, 1, 1),
            RequestType.ORDER_HISTORY, 15);
        List<Partner> partners = List.of(new Partner(101L), new Partner(102L), new Partner(103L));
        List<ResourceId> orders = List.of(
            new ResourceId("ORDER_NOT_FOUND", "TRACK_CODE_1"),
            new ResourceId("ORDER_2", "TRACK_CODE_NOT_FOUND"),
            new ResourceId("ORDER_3", "TRACK_CODE_3")
        );

        pushOrders(partners, orders)
            .andExpect(status().isCreated());

        ArgumentCaptor<TrackEventLogData> trackEventCaptor = ArgumentCaptor.forClass(TrackEventLogData.class);
        verifyLogging(trackEventCaptor, 3);

        TrackEventLogData actualTrackEventLogData = trackEventCaptor.getValue();

        assertEquals("Check logged trackCode", "TRACK_CODE_3", actualTrackEventLogData.getTrackCode());
        assertEquals("Check logged orderId", "ORDER_3", actualTrackEventLogData.getOrderId());
        assertEquals("Check logged eventType", PUSH_RECEIVED.readableName(), actualTrackEventLogData.getEventType());
        assertEquals("Check logged serviceId", 103, actualTrackEventLogData.getServiceId());
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/push/before_supply_push_batches.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/supply/push/after_supply_push_batches.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyPushBatchesWithoutTrackCodes() throws Exception {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(List.of(1L, 2L, 3L), List.of(1, 1, 1),
            RequestType.ORDER_HISTORY, 15);
        List<Partner> partners = List.of(new Partner(101L), new Partner(102L), new Partner(103L));
        List<ResourceId> orders = List.of(
            new ResourceId("ORDER_1", "TRACK_CODE_NOT_FOUND"),
            new ResourceId("ORDER_2", "TRACK_CODE_NOT_FOUND"),
            new ResourceId("ORDER_3", "TRACK_CODE_NOT_FOUND")
        );

        pushOrders(partners, orders)
            .andExpect(status().isCreated());

        ArgumentCaptor<TrackEventLogData> trackEventCaptor = ArgumentCaptor.forClass(TrackEventLogData.class);
        verifyLogging(trackEventCaptor, 3);

        TrackEventLogData actualTrackEventLogData = trackEventCaptor.getValue();

        assertEquals("Check logged trackCode", "TRACK_CODE_3", actualTrackEventLogData.getTrackCode());
        assertEquals("Check logged orderId", "ORDER_3", actualTrackEventLogData.getOrderId());
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/push/before_supply_push_batches.xml")
    @ExpectedDatabase(
        value = "/database/states/batches/supply/push/before_supply_push_batches.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoBatchesWereCreatedException() throws Exception {
        List<Partner> partners = List.of(new Partner(101L));
        List<ResourceId> orders = List.of(new ResourceId("ORDER_NOT_FOUND", "TRACK_CODE_NOT_FOUND"));

        pushOrders(partners, orders)
            .andExpect(status().isOk());

        verify(businessDataLogger, never()).log(any());
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/push/before_supply_push_batches_multiple_api.xml")
    void testMultiplePartnerApi() throws Exception {
        List<Partner> partners = List.of(new Partner(101L), new Partner(102L), new Partner(103L));
        List<ResourceId> orders = List.of(
            new ResourceId("ORDER_1", "TRACK_CODE_1"),
            new ResourceId("ORDER_2", "TRACK_CODE_2"),
            new ResourceId("ORDER_3", "TRACK_CODE_3")
        );

        pushOrders(partners, orders)
            .andExpect(status().isCreated());

        // Батчи могут создаться в произвольном порядке, ссылки на них не получится проверить через @ExpectedDatabase
        assertThat(deliveryTrackDao.getBatchedTracks()).containsExactly(
            Pair.of(1L, ApiVersion.DS),
            Pair.of(2L, ApiVersion.FF),
            Pair.of(3L, null)
        );
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/push/before_supply_push_batches_description_logging.xml")
    void testDescriptionLogging() throws Exception {
        List<Partner> partners = List.of(new Partner(101L));
        List<ResourceId> orders = List.of(
            new ResourceId("ORDER_1", "TRACK_CODE_1"),
            new ResourceId("ORDER_2", "TRACK_CODE_2"),
            new ResourceId("ORDER_3", "TRACK_CODE_3"),
            new ResourceId("ORDER_4", "TRACK_CODE_4")
        );

        pushOrders(partners, orders)
            .andExpect(status().isCreated());
        verify(pushOrderStatusLogger).log(
            any(),
            any(),
            eq(new PartnerCreatedBatchesDescription(3, 2, 4, 101))
        );
    }

    @Nonnull
    private ResultActions pushOrders(List<Partner> partners, List<ResourceId> orders) throws Exception {
        return mockMvc.perform(put("/orders/status/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PushOrderStatusesRequest(partners, orders))));
    }

}
