package ru.yandex.market.delivery.transport_manager.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.caledaring_service.CalendaringServiceClientConfig;
import ru.yandex.market.delivery.transport_manager.config.properties.TmProperties;
import ru.yandex.market.delivery.transport_manager.converter.calendaring.BookSlotConverter;
import ru.yandex.market.delivery.transport_manager.converter.calendaring.CalendaringTransportationMetadataService;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDockTimeSlotDto;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookSlotConverterTest extends AbstractContextualTest {

    static ZonedDateTime d1 = ZonedDateTime.of(
        2021, 4, 22, 16, 0, 0, 0, ZoneId.of("Z")
    );
    static ZonedDateTime d2 = ZonedDateTime.of(
        2021, 4, 22, 17, 0, 0, 0, ZoneId.of("Z")
    );

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    BookSlotConverter bookSlotConverter;

    @Autowired
    private TmProperties tmProperties;

    private RegisterService registerService;

    @BeforeEach
    void init() {
        registerService = mock(RegisterService.class);
        bookSlotConverter = new BookSlotConverter(
            tmProperties,
            new CalendaringTransportationMetadataService(registerService),
            new IdPrefixConverter()
        );
    }

    @Test
    void convertToGetFreeSlotsRequestTest() {
        Transportation transportation = getTransportation();

        LocalDate quotaStartDate = LocalDate.of(2021, 7, 20);
        GetFreeSlotsRequest request =
            bookSlotConverter.toGetFreeSlotsRequest(
                transportation,
                transportation.getInboundUnit(),
                d2.toLocalDate().atStartOfDay(),
                d2.toLocalDate().atTime(LocalTime.MAX),
                quotaStartDate
            );

        softly.assertThat(request.getBookingId()).isNull();
        softly.assertThat(request.getTakenPallets()).isEqualTo(0);
        softly.assertThat(request.getSlotDurationMinutes()).isEqualTo(60);
        softly.assertThat(request.getFrom()).isEqualTo(d2.toLocalDate().atTime(0, 0));
        softly.assertThat(request.getTo()).isEqualTo(d2.toLocalDate().atTime(LocalTime.MAX));
        softly.assertThat(request.getBookingType()).isEqualTo(BookingType.MOVEMENT_SUPPLY);
        softly.assertThat(request.getWarehouseIds()).isEqualTo(Set.of(2L));
        softly.assertThat(request.getQuotaFrom()).isEqualTo(LocalDate.of(2021, 7, 21));
    }

    @Test
    void convertToBookSlotRequestTest() {
        Transportation transportation = getTransportation();
        when(registerService.findPlanWithUnits(1L))
            .thenReturn(getRegisters());

        LocalDate quotaStartDate = LocalDate.of(2021, 7, 20);
        BookSlotRequest request = bookSlotConverter.toBookSlotRequest(
            transportation,
            transportation.getInboundUnit(),
            d1.toLocalDateTime(),
            d2.toLocalDateTime(),
            quotaStartDate,
            null
        );

        softly.assertThat(request.getExternalId()).isEqualTo("TMU2");
        softly.assertThat(request.getTakenPallets()).isEqualTo(3);
        softly.assertThat(request.getTakenItems()).isEqualTo(8);
        softly.assertThat(request.getFrom()).isEqualTo(d1.toLocalDateTime());
        softly.assertThat(request.getTo()).isEqualTo(d2.toLocalDateTime());
        softly.assertThat(request.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        softly.assertThat(request.getType()).isEqualTo(BookingType.MOVEMENT_SUPPLY);
        softly.assertThat(request.getQuotaFrom()).isEqualTo(LocalDate.of(2021, 7, 21));
    }

    @Test
    void convertToTimeSlotTest() {
        TimeSlot request = bookSlotConverter.convertToTimeSlot(getBookSlotResponse());

        softly.assertThat(request.getCalendaringServiceId()).isEqualTo(1L);
        softly.assertThat(request.getGateId()).isEqualTo(42L);
        softly.assertThat(request.getZonedFromDate()).isEqualTo(d1);
        softly.assertThat(request.getZonedToDate()).isEqualTo(d2);
    }

    @Test
    void convertToXDockTimeSlotDto() {
        XDockTimeSlotDto request = bookSlotConverter.convertToXDockTimeSlotDto(getBookingResponse());

        softly.assertThat(request.getCalendaringServiceId()).isEqualTo(1L);
        softly.assertThat(request.getGateId()).isEqualTo(42L);
        softly.assertThat(request.getFromDate()).isEqualTo(d1);
        softly.assertThat(request.getToDate()).isEqualTo(d2);
    }

    @Test
    void copyInTrip() {
        softly.assertThat(
                bookSlotConverter.copyInTrip(
                    new BookingResponseV2(
                        1L,
                        CalendaringServiceClientConfig.SOURCE,
                        "TMU1",
                        "TMT1",
                        10L,
                        ZonedDateTime.of(2022, 6, 23, 12, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
                        ZonedDateTime.of(2022, 6, 23, 13, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2022, 6, 22, 10, 0),
                        172
                    ),
                    2L,
                    TransportationType.XDOC_TRANSPORT,
                    TransportationUnitType.INBOUND
                )
            )
            .extracting(this::writeValueAsString)
            .isEqualTo(this.writeValueAsString(
                    new BookSlotRequest(
                        SupplierType.FIRST_PARTY,
                        null,
                        172L,
                        null,
                        BookingType.XDOCK_TRANSPORT_SUPPLY,
                        LocalDateTime.of(2022, 6, 23, 12, 0),
                        LocalDateTime.of(2022, 6, 23, 13, 0),
                        "TMU2",
                        CalendaringServiceClientConfig.SOURCE,
                        "TMT1",
                        0,
                        0,
                        null,
                        null,
                        false,
                        null

                    )
                )
            );
    }

    @SneakyThrows
    private String writeValueAsString(Object object) {
        return new ObjectMapper().writeValueAsString(object);
    }

    private FreeSlotsForDayResponse getFreeSlotsForDayResponse() {
        TimeSlotResponse timeSlotResponse = getTimeSlotResponse();

        return new FreeSlotsForDayResponse(d1.toLocalDate(), ZoneOffset.UTC, List.of(timeSlotResponse));
    }

    private TimeSlotResponse getTimeSlotResponse() {
        return new TimeSlotResponse(d1.toLocalTime(), d2.toLocalTime());
    }

    private BookSlotResponse getBookSlotResponse() {
        return new BookSlotResponse(1L, 42L, d1, d2);
    }

    private BookingResponse getBookingResponse() {
        return new BookingResponse(
            1L,
            "FFWF",
            "1",
            null,
            42L,
            d1,
            d2,
            BookingStatus.ACTIVE,
            LocalDateTime.now(clock),
            1L
        );
    }

    private Transportation getTransportation() {
        return new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setStatus(TransportationStatus.SCHEDULED)
            .setOutboundUnit(new TransportationUnit()
                .setId(1L)
                .setPartnerId(1L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(10L)
                .setPlannedIntervalStart(d2.toLocalDateTime())
                .setPlannedIntervalEnd(d1.toLocalDateTime())
                .setRequestId(11L)
                .setBookedTimeSlot(new TimeSlot().setToDate(d2.toLocalDateTime()))
            )
            .setInboundUnit(new TransportationUnit()
                .setId(2L)
                .setExternalId("ID_AT_PARTNER_01")
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(2L)
                .setLogisticPointId(20L)
                .setPlannedIntervalStart(d1.toLocalDateTime())
                .setPlannedIntervalEnd(d2.toLocalDateTime())
            )
            .setMovement(new Movement()
                .setId(4L)
                .setExternalId("20L")
                .setPartnerId(2L)
                .setMaxPallet(2)
                .setPlannedIntervalStart(d1.toLocalDateTime())
                .setPlannedIntervalEnd(d2.toLocalDateTime())
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
            )
            .setScheme(TransportationScheme.NEW);
    }

    private List<Register> getRegisters() {
        return List.of(
            new Register().setItems(
                List.of(
                    new RegisterUnit().setCounts(
                        List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1))
                    ),
                    new RegisterUnit().setCounts(
                        List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(2))
                    )
                )
            ).setPallets(List.of(new RegisterUnit(), new RegisterUnit())),
            new Register().setItems(
                List.of(
                    new RegisterUnit().setCounts(
                        List.of(
                            new UnitCount().setCountType(CountType.FIT).setQuantity(2),
                            new UnitCount().setCountType(CountType.DEFECT).setQuantity(3)
                        )
                    )
                )
            ).setPallets(List.of(new RegisterUnit()))
        );
    }
}
