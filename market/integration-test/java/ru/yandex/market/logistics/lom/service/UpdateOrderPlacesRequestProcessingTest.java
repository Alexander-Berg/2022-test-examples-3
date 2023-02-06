package ru.yandex.market.logistics.lom.service;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.Mockito.when;

@DatabaseSetup("/service/update_order_places_request/before/prepare.xml")
class UpdateOrderPlacesRequestProcessingTest extends AbstractContextualTest {

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Autowired
    private LMSClient lmsClient;

    /**
     * В заказе 5 сегментов, заявка начинается с сегмента с индексом 1
     * У заказа одна коробка
     * На какие сегменты заявки и таски должны создаться, а на какие нет, и почему:
     * 0 - есть externalId и partnerSettings разрешает обновлять при любом количестве коробок
     *     полностью игнорируется т.к. не попадает в заявку
     * 1 - есть externalId и partnerSettings разрешает обновлять только при одной коробке
     *     создается заявка в PROCESSING, ставится таска - в партнере уже есть заказ и можно обновить коробки
     * 2 - есть externalId, partnerSettings разрешает обновлять только при 2+ коробок
     *     ничего не создается т.к. в партнере обновить коробки нельзя
     * 3 - нет externalId, в LMS параметр партнера разрешает обновлять только при одной коробке
     *     создается заявка в WAITING_FOR_PROCESSING_AVAILABILITY, таска не ставится - обновить можно, но заказа еще нет
     * 4 - нет externalId, в LMS параметр партнера разрешает обновлять только при 2+ коробок
     *     ничего не создается т.к. в партнере обновить коробки нельзя
     * 5 - есть externalId и partnerSettings разрешает обновлять при любом количестве коробок
     *     тип сегмента FULFILLMENT и есть единственный тег - RETURN
     *     ничего не создается, на возвратном FULFILLMENT не должно обновлять
     */
    @Test
    @DisplayName("Успешное создание запросов на сегменты и тасок, одна коробка")
    @DatabaseSetup("/service/update_order_places_request/before/good_request.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_request/after/change_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/update_order_places_request/after/segment_requests_created_one_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void segmentTasksCreatedOneBox() {
        setUpLmsPartners();
        processor.processPayload(new ChangeOrderRequestPayload(REQUEST_ID, 100L));
        queueTaskChecker.assertQueueTasksCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_PLACES, 1);
    }

    /**
     * В заказе 5 сегментов, заявка начинается с сегмента с индексом 1
     * У заказа две коробки
     * На какие сегменты заявки и таски должны создаться, а на какие нет, и почему:
     * 0 - есть externalId и partnerSettings разрешает обновлять при любом количестве коробок
     *     полностью игнорируется т.к. не попадает в заявку
     * 1 - есть externalId и partnerSettings разрешает обновлять только при одной коробке
     *     ничего не создается т.к. в партнере обновить коробки нельзя
     * 2 - есть externalId, partnerSettings разрешает обновлять только при 2+ коробок
     *     создается заявка в PROCESSING, ставится таска - в партнере уже есть заказ и можно обновить коробки
     * 3 - нет externalId, в LMS параметр партнера разрешает обновлять только при одной коробке
     *     ничего не создается т.к. в партнере обновить коробки нельзя
     * 4 - нет externalId, в LMS параметр партнера разрешает обновлять только при 2+ коробок
     *     создается заявка в WAITING_FOR_PROCESSING_AVAILABILITY, таска не ставится - обновить можно, но заказа еще нет
     * 5 - есть externalId и partnerSettings разрешает обновлять при любом количестве коробок
     *     тип сегмента FULFILLMENT и есть единственный тег - RETURN
     *     ничего не создается, на возвратном FULFILLMENT не должно обновлять
     */
    @Test
    @DisplayName("Успешное создание запросов на сегменты и тасок, 2+ коробок")
    @DatabaseSetup("/service/update_order_places_request/before/good_request.xml")
    @DatabaseSetup(
        value = "/service/update_order_places_request/before/extra_box.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/update_order_places_request/after/change_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/update_order_places_request/after/segment_requests_created_many_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void segmentTasksCreatedManyBoxes() {
        setUpLmsPartners();
        processor.processPayload(new ChangeOrderRequestPayload(REQUEST_ID, 100L));
        queueTaskChecker.assertQueueTasksCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_PLACES, 1);
    }

    @Test
    @DisplayName("Заявка на неактивном сегменте не создается")
    @DatabaseSetup("/service/update_order_places_request/before/good_request.xml")
    @DatabaseSetup(
        value = "/service/update_order_places_request/before/inactive_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    void doNotCreateTaskForInactiveSegment() {
        setUpLmsPartners();
        processor.processPayload(new ChangeOrderRequestPayload(REQUEST_ID, 100L));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_PLACES);
    }

    @Test
    @DisplayName("Запрос не в статусе INFO_RECEIVED")
    @DatabaseSetup("/service/update_order_places_request/before/wrong_request_status.xml")
    void wrongChangeRequestStatus() {
        processor.processPayload(new ChangeOrderRequestPayload(REQUEST_ID, 100L));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_PLACES);
    }

    @Test
    @DisplayName("По запросу нет сегментов, которые можно обновить")
    @DatabaseSetup("/service/update_order_places_request/before/no_updatable_segments_request.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_request/after/change_request_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noSegmentsToProcess() {
        when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1004L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder()
                    .params(List.of(new PartnerExternalParam("UPDATE_ORDER_WITH_MANY_BOXES_ENABLED", "", "1")))
                    .id(1004L)
                    .build()
            ));
        processor.processPayload(new ChangeOrderRequestPayload(REQUEST_ID, 100L));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_PLACES);
    }

    private void setUpLmsPartners() {
        when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1003L, 1004L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder()
                    .params(List.of(new PartnerExternalParam("UPDATE_ORDER_WITH_ONE_BOX_ENABLED", "", "1")))
                    .id(1003L)
                    .build(),
                PartnerResponse.newBuilder()
                    .params(List.of(new PartnerExternalParam("UPDATE_ORDER_WITH_MANY_BOXES_ENABLED", "", "1")))
                    .id(1004L)
                    .build()
            ));
    }
}
