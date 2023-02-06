package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.time.ZoneId;
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

import ru.yandex.market.logistics.les.sc.UnknownBoxReceivedEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.UnknownBoxEventProcessedMeta;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Обработка получения события: СЦ получил неопознанную коробку")
class UnknownBoxReceivedEventProcessorTest extends AbstractIntegrationYdbTest {

    private static final Long IGNORED = 739L;
    private static final long PARTNER_ID = 100;
    private static final long WAREHOUSE_ID = 200;
    private static final String WAREHOUSE_EXTERNAL_ID = "300";
    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private AsyncLesEventProcessor processor;
    @Autowired
    private EntityMetaTableDescription entityMetaTableDescription;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTableDescription);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное создание сегмента СЦ. Предыдущий сегмент - ПВЗ")
    @DatabaseSetup("/database/les/unknown-box-received/before/pickup.xml")
    @ExpectedDatabase(
        value = "/database/les/unknown-box-received/after/pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successPickup() {
        mockLms();

        processTask(PARTNER_ID, "box-external-id");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(PARTNER_ID));
        verify(lmsClient).getPartner(PARTNER_ID);
        assertYdb();
    }

    @Test
    @DisplayName("Успешное создание сегмента СЦ. Предыдущий сегмент - Курьер")
    @DatabaseSetup("/database/les/unknown-box-received/before/courier.xml")
    @ExpectedDatabase(
        value = "/database/les/unknown-box-received/after/courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCourier() {
        mockLms();

        processTask(PARTNER_ID, "box-external-id");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(PARTNER_ID));
        verify(lmsClient).getPartner(PARTNER_ID);
        assertYdb();
    }

    @Test
    @DisplayName("Успешное создание сегмента СЦ. Заведено несколько сегментов. Предыдущий сегмент - СЦ")
    @DatabaseSetup("/database/les/unknown-box-received/before/multiple_segments.xml")
    @ExpectedDatabase(
        value = "/database/les/unknown-box-received/after/multiple_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successMultipleSegments() {
        mockLms();

        processTask(PARTNER_ID, "box-external-id");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(PARTNER_ID));
        verify(lmsClient).getPartner(PARTNER_ID);
        assertYdb();
    }

    @Test
    @DisplayName("Коробки с указанным в событии идентификатором не существует")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void eventReceivedForNonExistingBox() {
        processTask(IGNORED, "box-with-wrong-id");
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    payload=Failed to find RETURN_BOX with id box-with-wrong-id\t\
                    request_id=test-request-id\
                    """
            );
    }

    @Test
    @DisplayName("СЦ с указанным в событии идентификатором не существует")
    @DatabaseSetup("/database/les/unknown-box-received/before/pickup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void eventReceivedForNonExistingSc() {
        long nonExistingScId = 109L;

        softly.assertThatCode(() -> processTask(nonExistingScId, "box-external-id"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Partner 109 must have exactly one warehouse");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(nonExistingScId));
    }

    @Test
    @DisplayName("Повторное получение события, сегмент не создается")
    @DatabaseSetup("/database/les/unknown-box-received/before/sc_exists.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void scAlreadyExists() {
        mockLms();

        processTask(PARTNER_ID, "box-external-id");
    }

    private void processTask(long sortingCenterId, String boxExternalId) {
        processor.execute(LesEventFactory.getDbQueuePayload(
            new UnknownBoxReceivedEvent(sortingCenterId, boxExternalId)
        ));
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter(long partnerId) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }

    private void mockLms() {
        LogisticsPointResponse logisticPoint = LogisticsPointResponse.newBuilder()
            .id(WAREHOUSE_ID)
            .partnerId(PARTNER_ID)
            .externalId(WAREHOUSE_EXTERNAL_ID)
            .name("склад сц")
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(PARTNER_ID)))
            .thenReturn(List.of(logisticPoint));

        PartnerResponse partner = PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .name("partner name")
            .build();
        when(lmsClient.getPartner(PARTNER_ID))
            .thenReturn(Optional.of(partner));
    }

    private void assertYdb() {
        softly.assertThat(
            getEntityMetaRecord(RETURN_SEGMENT_1_HASH, "RETURN_SEGMENT", 1L, "unknown-box-event-processed")
                .map(EntityMetaTableDescription.EntityMetaRecord::value)
                .map(v -> readValue(v, UnknownBoxEventProcessedMeta.class))
        ).contains(
            UnknownBoxEventProcessedMeta.builder()
                .datetime(DATETIME)
                .build()
        );
    }

    private void assertEmptyYdb() {
        softly.assertThat(getEntityMetaRecord(
                RETURN_SEGMENT_1_HASH,
                "RETURN_SEGMENT",
                1L,
                "unknown-box-event-processed"
            ))
            .isEmpty();
    }
}
