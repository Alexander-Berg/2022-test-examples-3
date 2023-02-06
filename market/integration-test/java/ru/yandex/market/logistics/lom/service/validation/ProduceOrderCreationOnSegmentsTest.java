package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderExternalValidationAndEnrichingService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DatabaseSetup("/service/externalvalidation/before/order_in_segments_creation.xml")
@DisplayName("Выставление тасок на создание заказа на сегментах после успешной валидации")
class ProduceOrderCreationOnSegmentsTest extends AbstractOrderExternalValidationAndEnrichingServiceTest {

    private static final OrderIdPayload VALIDATION_PAYLOAD = createOrderIdPayload(ORDER_ID, "1");

    @Autowired
    private OrderExternalValidationAndEnrichingService orderExternalValidationAndEnrichingService;

    @BeforeEach
    void setup() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
    }

    @Test
    @DisplayName("FF(Dropship) -> SC -> SD: таска создается")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/orders_in_segments_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipScSdTaskCreated() {
        mockDsSegment();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verifyGetPoints();
        verifySearchPartners();

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(PICKUP_POINT_MARKET_ID);
    }

    @Test
    @DisplayName("FF(Dropship) -> SC -> SD: для дропшипа уже создан заказ, таска создается только на последний сегмент")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/order_created_in_ds.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/orders_in_ff_segments_tasks_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipScSdTaskCreatedOrderCreatedInDs() {
        mockDsSegment();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verifyGetPoints();
        verifySearchPartners();

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(PICKUP_POINT_MARKET_ID);
    }

    @Test
    @DisplayName("FF(Dropship) -> SC -> SD: для последнего сегмента уже создан заказ, таска не создается")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/order_created_in_last_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/orders_in_middle_segment_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipScSdTaskCreatedOrderCreatedInLastSegment() {
        mockDsSegment();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verifyGetPoints();
        verifySearchPartners();

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(PICKUP_POINT_MARKET_ID);
    }

    @Test
    @DisplayName("FF(Dropship) -> SD: таска создается")
    @DatabaseSetup(value = "/service/externalvalidation/before/sc_segment.xml", type = DatabaseOperation.DELETE)
    @DatabaseSetup(value = "/service/externalvalidation/before/last_segment_index.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/orders_in_segments_tasks_created_ff_sd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipSdTaskCreated() {
        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID))))
            .thenReturn(
                List.of(
                    LmsFactory.createPartnerResponse(PARTNER_ID, PARTNER_MARKET_ID, PartnerType.DROPSHIP),
                    LmsFactory.createPartnerResponse(RETURN_PARTNER, "FF", RETURN_PARTNER_MARKET_ID),
                    LmsFactory.createPartnerResponse(PICKUP_POINT_PARTNER_ID, "FF", PICKUP_POINT_MARKET_ID)
                )
            );

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verifyGetPoints();

        verify(lmsClient, times(2))
            .searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(2)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(PICKUP_POINT_MARKET_ID);
    }

    @Test
    @DisplayName("FF(FF) -> SC -> SD: для ff партнера таска не создается")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/orders_in_segments_tasks_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void ffTaskNotCreated() {
        mockFfSegment();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_CREATE_ORDER,
            PayloadFactory.createWaybillSegmentPayload(1L, 3L, "1", 1)
        );

        verifyGetPoints();
        verifySearchPartners();

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(PICKUP_POINT_MARKET_ID);
    }

    private void mockFfSegment() {
        when(lmsClient.searchPartners(partnerFilter(
            Set.of(PARTNER_ID, SORTING_CENTER_PARTNER, PICKUP_POINT_PARTNER_ID, RETURN_PARTNER)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartnerResponse(PARTNER_ID, PARTNER_MARKET_ID, PartnerType.FULFILLMENT),
                LmsFactory.createPartnerResponse(SORTING_CENTER_PARTNER, PARTNER_MARKET_ID),
                LmsFactory.createPartnerResponse(RETURN_PARTNER, RETURN_PARTNER_MARKET_ID),
                LmsFactory.createPartnerResponse(PICKUP_POINT_PARTNER_ID, PICKUP_POINT_MARKET_ID)
            ));
    }

    private void mockDsSegment() {
        when(lmsClient.searchPartners(partnerFilter(
            Set.of(PARTNER_ID, SORTING_CENTER_PARTNER, PICKUP_POINT_PARTNER_ID, RETURN_PARTNER)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartnerResponse(PARTNER_ID, PARTNER_MARKET_ID, PartnerType.DROPSHIP),
                LmsFactory.createPartnerResponse(SORTING_CENTER_PARTNER, PARTNER_MARKET_ID),
                LmsFactory.createPartnerResponse(RETURN_PARTNER, RETURN_PARTNER_MARKET_ID),
                LmsFactory.createPartnerResponse(PICKUP_POINT_PARTNER_ID, PICKUP_POINT_MARKET_ID)
            ));
    }

    private void verifyGetPoints() {
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
    }

    private void verifySearchPartners() {
        verify(lmsClient).searchPartners(partnerFilter(
            Set.of(PARTNER_ID, SORTING_CENTER_PARTNER, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)
        ));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));
    }
}
