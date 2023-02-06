package ru.yandex.market.ff.service.implementation;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.ReturnFlowType;
import ru.yandex.market.ff.event.LrmReturnEventConsumer;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.entity.ReturnBoxEntity;
import ru.yandex.market.ff.model.entity.ReturnEntity;
import ru.yandex.market.ff.model.entity.ReturnItemEntity;
import ru.yandex.market.ff.model.enums.ReturnReasonType;
import ru.yandex.market.ff.repository.ReturnRepository;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.lrm.LrmService;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload;

class ReturnEntityServiceTest extends IntegrationTest {
    @Autowired
    private ReturnEntityService returnEntityService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private LrmService lrmService;
    @Autowired
    private LrmReturnEventConsumer lrmReturnEventConsumer;
    @Autowired
    private ReturnRepository returnRepository;

    @Test
    @ExpectedDatabase(value = "/service/returns/after-create-return.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldSuccessfullyCreateReturnDbRecords() {
        returnEntityService.save(List.of(getReturnEntity(1), getReturnEntity(2)));
    }

    @Test
    @DatabaseSetup("/service/returns/before-remove_box_duplicate.xml")
    void shouldTakeBiggestOrderIdForBoxId() {
        Set<String> orderIdsByBoxIds = returnEntityService.getOrderIdsByBoxIds(
                Set.of("box2"),
                Set.of(ReturnFlowType.UNREDEEMED, ReturnFlowType.PARTIAL_UNREDEEMED));
        assertions.assertThat(1).isEqualTo(orderIdsByBoxIds.size());
        assertions.assertThat("8").isEqualTo(orderIdsByBoxIds.iterator().next());
    }

    @Test
    @DatabaseSetup("/service/returns/before-update-status-by-event.xml")
    void shouldUpdateStatusWhenGetReturnStatusChanged() {
        Instant now = Instant.now();

        lrmReturnEventConsumer.accept(List.of(getReturnStatusChangedEvent()));

        ReturnEntity actual = returnRepository.findOne(1L);

        assertions.assertThat(actual.getStatus()).isEqualTo(ReturnStatus.IN_TRANSIT);
        assertions.assertThat(actual.getUpdated()).isAfterOrEqualTo(now);
    }

    @Test
    @DatabaseSetup("/service/returns/before-update-status-by-event.xml")
    void shouldNotUpdateStatusWhenGotOldStatus() {
        Instant now = Instant.now();

        lrmReturnEventConsumer.accept(List.of(getReturnStatusChangedEvent()));
        lrmReturnEventConsumer.accept(List.of(getOldReturnStatusChangedEvent()));

        ReturnEntity actual = returnRepository.findOne(1L);

        assertions.assertThat(actual.getStatus()).isEqualTo(ReturnStatus.IN_TRANSIT);
        assertions.assertThat(actual.getUpdated()).isAfterOrEqualTo(now);
    }

    @NotNull
    private ReturnEntity getReturnEntity(int id) {
        ReturnBoxEntity box1 = ReturnBoxEntity.builder().externalId("box1").build();
        ReturnBoxEntity box2 = ReturnBoxEntity.builder().externalId("box2").build();
        ReturnBoxEntity box3 = ReturnBoxEntity.builder().externalId("box3").build();
        var items = Set.of(
                ReturnItemEntity.builder()
                        .box(box1)
                        .article("sku1")
                        .count(5)
                        .instances(List.of(getRegistryUnit("2489571_item1_cis1", "2489571_item1_uit1")))
                        .build(),
                ReturnItemEntity.builder()
                        .box(box1)
                        .article("sku2")
                        .count(3)
                        .instances(List.of(getRegistryUnit("2489571_item2_cis1", "2489571_item2_uit1")))
                        .build(),
                ReturnItemEntity.builder()
                        .box(box2)
                        .article("sku3")
                        .count(1)
                        .instances(List.of(getRegistryUnit("2489571_item3_cis1", "2489571_item3_uit1")))
                        .build(),
                ReturnItemEntity.builder()
                        .box(box3)
                        .article("sku4")
                        .supplierId(666L)
                        .count(1)
                        .instances(List.of(getRegistryUnit("2489571_item4_cis1", "2489571_item4_uit1"),
                                getRegistryUnit("2489571_item4_cis2", "2489571_item4_uit2")))
                        .reasonType(ReturnReasonType.BAD_QUALITY)
                        .returnReason("return reason")
                        .build()
        );
        return ReturnEntity.builder()
                .externalId("extId" + id)
                .orderExternalId("orderExtId" + id)
                .created(dateTimeService.localDateTimeNow().atZone(TimeZoneUtil.DEFAULT_OFFSET).toInstant())
                .boxes(Set.of(box1, box2, box3))
                .items(items)
                .build();
    }

    private RegistryUnitId getRegistryUnit(String cis, String uit) {
        return lrmService.getRegistryUnit(ImmutableMap.of("CIS", cis, "UIT", uit));
    }

    private ReturnEvent getReturnStatusChangedEvent() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_STATUS_CHANGED)
                .id(2L)
                .created(DateTime.now().toDate().toInstant())
                .returnId(88L)
                .orderExternalId("orderExtId1")
                .payload(new ReturnStatusChangedPayload().setStatus(ReturnStatus.IN_TRANSIT))
                .build();
    }

    private ReturnEvent getOldReturnStatusChangedEvent() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_STATUS_CHANGED)
                .id(1L)
                .created(DateTime.parse("2021-01-01").toDate().toInstant())
                .returnId(88L)
                .orderExternalId("orderExtId1")
                .payload(new ReturnStatusChangedPayload().setStatus(ReturnStatus.CREATED))
                .build();
    }
}
