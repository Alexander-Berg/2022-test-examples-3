package ru.yandex.market.tsup.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.DriverLogDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfRunDto;
import ru.yandex.mj.generated.client.carrier.model.RunDto;
import ru.yandex.mj.generated.client.carrier.model.RunPointDto;
import ru.yandex.mj.generated.client.carrier.model.RunPriceStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunSubtypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunTypeDto;
import ru.yandex.mj.generated.client.carrier.model.TimestampDto;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class RunDtoUtils {

    public static final String DEFAULT_TIMEZONE = "Europe/Moscow";
    public static final String LOCAL_TIMEZONE = "Asia/Novosibirsk";

    public static PageOfRunDto page(RunDto... dtos) {
        return page(List.of(dtos));
    }

    public static PageOfRunDto page(List<RunDto> dtos) {
        PageOfRunDto page = new PageOfRunDto();
        page.setContent(dtos);
        page.setTotalElements((long) dtos.size());
        page.setTotalPages(0);
        page.setNumber(0);
        page.setSize(20);
        return page;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static RunDto runDto(
            long id,
            RunTypeDto type,
            RunStatusDto status,
            RunPointDto from,
            RunPointDto to,
            Long movementPartnerId,
            Instant startAt,
            Instant expectedArrivalTime,
            String defaultTimezone,
            String localTimezone,
            DriverLogDto driverLog,
            RunSubtypeDto subtypeDto,
            RunPriceStatusDto priceStatusDto
    ) {
        return new RunDto()
                .id(id)
                .externalId("TMM" + (1000 + id))
                .type(type)
                .subtype(subtypeDto)
                .status(status)
                .date(startAt.atZone(DateTimeUtils.MOSCOW_ZONE).toLocalDate())
                .startDateTime(startAt.atOffset(ZoneOffset.UTC))
                .priceStatus(priceStatusDto)
                .defaultStartDateTime(new TimestampDto()
                        .timestamp(startAt.atZone(ZoneId.of(defaultTimezone)).toOffsetDateTime())
                        .timezoneName(defaultTimezone))
                .localStartDateTime(new TimestampDto()
                        .timestamp(startAt.atZone(ZoneId.of(localTimezone)).toOffsetDateTime())
                        .timezoneName(localTimezone))
                .expectedDateTime(new TimestampDto()
                        .timestamp(expectedArrivalTime.atZone(ZoneId.of(localTimezone)).toOffsetDateTime())
                        .timezoneName(localTimezone))
                .points(List.of(from, to))
                .company(new CompanyDto().id(movementPartnerId))
                .driverLog(driverLog);
    }

    public static RunPointDto runPointDto(
            long id,
            String name
    ) {
        return new RunPointDto()
                .yandexId(id)
                .name(name)
                .latitude(BigDecimal.TEN.multiply(BigDecimal.valueOf(id)))
                .longitude(BigDecimal.ONE.multiply(BigDecimal.valueOf(id)));
    }

}
