package ru.yandex.market.logistics.management.service.serialization;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteCsvGridDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteCsvGridDto.PartnerRouteCsvGridDtoBuilder;
import ru.yandex.market.logistics.management.util.CsvUtil;
import ru.yandex.market.logistics.management.util.UnitTestUtil;

@DisplayName("Десериализация csv")
class CsvDeserializerTest extends AbstractTest {

    public static final String CONTENT =
        "partner,locationFrom,locationTo,monday,tuesday,wednesday,thursday,friday,saturday,sunday"
            + ",pickupInboundSchedule.mondayFrom,pickupInboundSchedule.mondayTo"
            + ",pickupInboundSchedule.tuesdayFrom,pickupInboundSchedule.tuesdayTo"
            + ",pickupInboundSchedule.wednesdayFrom,pickupInboundSchedule.wednesdayTo"
            + ",pickupInboundSchedule.thursdayFrom,pickupInboundSchedule.thursdayTo"
            + ",pickupInboundSchedule.fridayFrom,pickupInboundSchedule.fridayTo"
            + ",pickupInboundSchedule.saturdayFrom,pickupInboundSchedule.saturdayTo"
            + ",pickupInboundSchedule.sundayFrom,pickupInboundSchedule.sundayTo"
            + ",korobyteRestrictionKey\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,08:00,09:00,,,,,,,,,,,,,\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,10:00,11:00,,,,,,,,,,,,,\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,12:00,,,,,,,,,,,,,,\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,13:00,,,,,,,,,,,,,,\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,14:00,,,,,,,,,,,,,\n"
            + "51,2,225,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,08:00,09:00,,,,,,,,,,,,,EXAMPLE\n";
    private final CsvDeserializer csvDeserializer = new CsvDeserializer(UnitTestUtil.MAPPER);

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("BOM игнорируется")
    @MethodSource("arguments")
    void ignoreBom(@SuppressWarnings("unused") String caseName, String content) {
        List<PartnerRouteCsvGridDto> dtoList = csvDeserializer.deserializeList(
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
            PartnerRouteCsvGridDto.class
        );
        softly.assertThat(dtoList)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                partnerRouteCsvGridDto().build(),
                partnerRouteCsvGridDto()
                    .pickupInboundSchedule(
                        new ScheduleDto().setMondayFrom(LocalTime.of(10, 0)).setMondayTo(LocalTime.of(11, 0))
                    )
                    .build(),
                partnerRouteCsvGridDto()
                    .pickupInboundSchedule(new ScheduleDto().setMondayFrom(LocalTime.of(12, 0)))
                    .build(),
                partnerRouteCsvGridDto()
                    .pickupInboundSchedule(new ScheduleDto().setMondayFrom(LocalTime.of(13, 0)))
                    .build(),
                partnerRouteCsvGridDto()
                    .pickupInboundSchedule(new ScheduleDto().setMondayTo(LocalTime.of(14, 0)))
                    .build(),
                partnerRouteCsvGridDto()
                    .korobyteRestrictionKey("EXAMPLE")
                    .build()
            );
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of("Без BOM", CONTENT),
            Arguments.of("С BOM", CsvUtil.UTF8_BOM + CONTENT)
        );
    }

    @Nonnull
    private static PartnerRouteCsvGridDtoBuilder<?, ?> partnerRouteCsvGridDto() {
        return PartnerRouteCsvGridDto.builder()
            .partner(51L)
            .locationFrom(2)
            .locationTo(225)
            .monday(true)
            .pickupInboundSchedule(
                new ScheduleDto().setMondayFrom(LocalTime.of(8, 0)).setMondayTo(LocalTime.of(9, 0))
            );
    }
}
