package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.CargoUnit;
import ru.yandex.market.logistics.les.tpl.StorageUnitSubscribeOnStatusesRequestEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessType;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.SubscribeSegmentOnStatusesInScProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Подписка грузоместа на статусы в СЦ")
@DatabaseSetup("/database/tasks/return-segment/subscribe-on-statuses-in-sc/request/before/prepare.xml")
class SubscribeSegmentOnStatusesInScProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @Autowired
    private SubscribeSegmentOnStatusesInScProcessor processor;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LesProducer lesProducer;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, lesProducer);
    }

    @Test
    @DisplayName("Успешная отправка события в LES")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/subscribe-on-statuses-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(1));
        }

        verify(lesProducer).send(
            new Event(
                SOURCE_FOR_LES,
                TEST_UUID,
                FIXED_TIME.toEpochMilli(),
                BusinessProcessType.SUBSCRIBE_SEGMENT_ON_STATUSES_IN_SC.name(),
                requestEvent(),
                ""
            ),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Неуспешная отправка события в LES")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lesFail() throws Exception {
        Event lesEvent = new Event(
            SOURCE_FOR_LES,
            TEST_UUID,
            FIXED_TIME.toEpochMilli(),
            BusinessProcessType.SUBSCRIBE_SEGMENT_ON_STATUSES_IN_SC.name(),
            requestEvent(),
            ""
        );

        doThrow(new RuntimeException("error")).when(lesProducer).send(lesEvent, OUT_LES_QUEUE);

        try (var ignored = mockPartnerToken()) {
            softly.assertThatCode(() -> processor.execute(payload(1)))
                .hasMessage("error")
                .isInstanceOf(RuntimeException.class);
        }

        verify(lesProducer).send(lesEvent, OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Настройки партнёра не найдены в LMS")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noApiSettings() throws Exception {
        try (var ignored = mockPartnerToken(List.of())) {
            softly.assertThatCode(() -> processor.execute(payload(1)))
                .hasMessage("No FF API token for partner 467")
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Nonnull
    private AutoCloseable mockPartnerToken() {
        return mockPartnerToken(List.of(SettingsApiDto.newBuilder().token("token467").build()));
    }

    @Nonnull
    private AutoCloseable mockPartnerToken(List<SettingsApiDto> result) {
        SettingsApiFilter filter = SettingsApiFilter.newBuilder()
            .partnerIds(Set.of(467L))
            .apiType(ApiType.FULFILLMENT)
            .build();

        when(lmsClient.searchPartnerApiSettings(filter)).thenReturn(result);

        return () -> verify(lmsClient).searchPartnerApiSettings(filter);
    }

    @Nonnull
    private ReturnSegmentIdPayload payload(long returnSegmentId) {
        return ReturnSegmentIdPayload.builder()
            .returnSegmentId(returnSegmentId)
            .requestId(TEST_REQUEST_ID)
            .build();
    }

    @Nonnull
    private StorageUnitSubscribeOnStatusesRequestEvent requestEvent() {
        return new StorageUnitSubscribeOnStatusesRequestEvent(
            TEST_REQUEST_ID,
            new PartnerDto(467L, "token467", "1234"),
            List.of(new CargoUnit(
                "1",
                "4c853d61-7a5f-4383-af32-cc56935f787d",
                new PointDto(PointType.SHOP, 1235, 900L, "lp1235name"),
                List.of(
                    new CodeDto("box-external-id", CodeType.CARGO_BARCODE),
                    new CodeDto("order-external-id", CodeType.ORDER_BARCODE)
                )
            ))
        );
    }
}
