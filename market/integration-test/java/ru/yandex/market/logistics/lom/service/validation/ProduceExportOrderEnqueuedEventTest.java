package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderExternalValidationAndEnrichingService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DatabaseSetup("/service/externalvalidation/before/produce_export_order_enqueued_event/yado_pvz_order.xml")
@DisplayName("Выставление таски на отправку события в LES")
class ProduceExportOrderEnqueuedEventTest extends AbstractOrderExternalValidationAndEnrichingServiceTest {
    private static final OrderIdPayload VALIDATION_PAYLOAD = createOrderIdPayload(ORDER_ID, "1");

    @Autowired
    QueueTaskChecker queueTaskChecker;

    @Autowired
    private OrderExternalValidationAndEnrichingService orderExternalValidationAndEnrichingService;

    @BeforeEach
    void setup() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
    }

    @Test
    @DisplayName("Таска создается: YADO заказ")
    void produceYadoOrderEnqueuedEvent() {
        validateOrderSuccess(PartnerSubtype.MARKET_OWN_PICKUP_POINT);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_ENQUEUED_LES_EVENT,
            PayloadFactory.lesOrderEventPayload(
                1,
                1,
                "2",
                2
            )
        );
    }

    @Test
    @DatabaseSetup("/service/externalvalidation/before/produce_export_order_enqueued_event/go_pvz_order.xml")
    @DisplayName("Таска создается: GO заказ")
    void produceGoOrderEnqueuedEvent() {
        validateOrderSuccess(PartnerSubtype.MARKET_OWN_PICKUP_POINT);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_ENQUEUED_LES_EVENT,
            PayloadFactory.lesOrderEventPayload(
                1,
                1,
                "2",
                2
            )
        );
    }

    @Test
    @DatabaseSetup(
        value = "/service/externalvalidation/before/produce_export_order_enqueued_event/beru_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Таска не создается: BERU заказ")
    void beruOrderEnqueuedEventNotProduced() {
        validateOrderSuccess(PartnerSubtype.MARKET_OWN_PICKUP_POINT);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ENQUEUED_LES_EVENT);
    }

    @Test
    @DisplayName("Таска не создается: не подходящий подтип партнера")
    void orderEnqueuedEventNotProduced() {
        validateOrderSuccess(null);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ENQUEUED_LES_EVENT);
    }

    private void validateOrderSuccess(PartnerSubtype partnerSubtype) {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();

        PartnerResponse partner = LmsFactory.createPartnerResponse(
            PARTNER_ID,
            PARTNER_MARKET_ID,
            PartnerType.DELIVERY,
            partnerSubtype
        );
        PartnerResponse returnPartner = LmsFactory.createPartnerResponse(
            RETURN_PARTNER,
            "SC",
            PARTNER_MARKET_ID
        );
        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER))))
            .thenReturn(List.of(partner, returnPartner));

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }
}
