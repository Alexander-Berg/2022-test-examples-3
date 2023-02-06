package ru.yandex.market.delivery.transport_manager.admin.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.admin.dto.GridStatusHistoryDto;
import ru.yandex.market.delivery.transport_manager.config.properties.LmsExtraProperties;
import ru.yandex.market.delivery.transport_manager.config.startrek.StartrekProperties;
import ru.yandex.market.delivery.transport_manager.config.tpl.TplProperties;
import ru.yandex.market.delivery.transport_manager.config.tsum.TsumProperties;
import ru.yandex.market.delivery.transport_manager.config.tsup.TsupProperties;
import ru.yandex.market.delivery.transport_manager.config.yard.YardProperties;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Status;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHistoryInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.provider.service.component.TransportationDataSupplier;
import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;

class AdminStatusHistoryDtoConverterTest {

    public static final Instant CHANGED_AT = Instant.ofEpochMilli(1000L);
    private static final String TSUM_URL = "https://tsum.yandex-team.ru";
    private static final String TRACE_REQUEST_ID_STUB = "stub";
    private StatusHistoryInfo statusHistoryInfo;
    private GridStatusHistoryDto<Status> expected;
    private AdminStatusHistoryDtoConverter adminStatusHistoryDtoConverter;

    @BeforeEach
    void setUp() {
        statusHistoryInfo = new StatusHistoryInfo()
            .setNewStatus("CHECK_FAILED")
            .setOldStatus("DEPARTED")
            .setId(1L)
            .setEntityId(2L)
            .setType(EntityType.TRANSPORTATION)
            .setSubType("abcd")
            .setPublished(false)
            .setTraceRequestId(TRACE_REQUEST_ID_STUB)
            .setChangedAt(CHANGED_AT);
        expected = new GridStatusHistoryDto<>()
            .setNewStatus(TransportationStatus.CHECK_FAILED)
            .setOldStatus(TransportationStatus.DEPARTED)
            .setId(1L)
            .setTraceRequestId(new ExternalReferenceObject(TRACE_REQUEST_ID_STUB,
                "https://tsum.yandex-team.ru/trace/" + TRACE_REQUEST_ID_STUB,
                true))
            .setChangedAt(LocalDateTime.ofInstant(CHANGED_AT, ZoneId.systemDefault()));
        adminStatusHistoryDtoConverter = new AdminStatusHistoryDtoConverter(
            new AdminExternalLinkConverter(
                new LmsExtraProperties().setAdminUrl("stub"),
                new TplProperties().setVirtualLinehaul(50000L),
                new StartrekProperties().setWebUrl("stub"),
                new YardProperties().setFrontUrl("stub"),
                new TsupProperties().setHost("stub"),
                new TsumProperties().setHost(TSUM_URL)
            )
        );
    }

    @Test
    void convert() {
        GridStatusHistoryDto<TransportationStatus> gridStatusHistoryDto =
            adminStatusHistoryDtoConverter.convert(statusHistoryInfo, TransportationStatus.class);

        Assertions.assertEquals(expected, gridStatusHistoryDto);
    }

    @Test
    void nulls() {
        statusHistoryInfo.setSubType(null);
        statusHistoryInfo.setOldStatus(null);

        GridStatusHistoryDto<TransportationStatus> gridStatusHistoryDto =
            adminStatusHistoryDtoConverter.convert(statusHistoryInfo, TransportationStatus.class);

        expected.setOldStatus(null);
        Assertions.assertEquals(expected, gridStatusHistoryDto);
    }

    @Test
    void convertWithDataProvider() {
        GridStatusHistoryDto<TransportationStatus> gridStatusHistoryDto =
            adminStatusHistoryDtoConverter.convert(statusHistoryInfo, new TransportationDataSupplier(null));

        Assertions.assertEquals(expected, gridStatusHistoryDto);
    }

    @Test
    void nullsWithDataProvider() {
        statusHistoryInfo.setSubType(null);
        statusHistoryInfo.setOldStatus(null);

        GridStatusHistoryDto<TransportationStatus> gridStatusHistoryDto =
            adminStatusHistoryDtoConverter.convert(statusHistoryInfo, new TransportationDataSupplier(null));

        expected.setOldStatus(null);
        Assertions.assertEquals(expected, gridStatusHistoryDto);
    }

}
