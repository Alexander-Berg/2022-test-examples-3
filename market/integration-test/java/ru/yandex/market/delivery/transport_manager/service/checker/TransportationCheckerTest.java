package ru.yandex.market.delivery.transport_manager.service.checker;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.caledaring_service.CalendaringServiceClientConfig;
import ru.yandex.market.delivery.transport_manager.converter.TimeSlotConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitSendingStrategy;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.factory.LmsFactory;
import ru.yandex.market.delivery.transport_manager.factory.MarketIdFactory;
import ru.yandex.market.delivery.transport_manager.model.enums.ApiType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TagMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationLegalInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMetadataMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerMethodMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationValidationErrorMapper;
import ru.yandex.market.delivery.transport_manager.service.cache.CacheService;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.checker.exception.TransportationCouldNotBeCheckedException;
import ru.yandex.market.delivery.transport_manager.service.checker.validation.TransportationValidator;
import ru.yandex.market.delivery.transport_manager.service.external.cs.CalendaringServiceReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.LogisticsPointReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.PartnerReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.PartnerSettingsReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.LegalInfoReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.MarketIdService;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.service.ticket.StartrekDtoConverter;
import ru.yandex.market.delivery.transport_manager.util.BacklogCodes;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DatabaseSetup({
    "/repository/transportation/checker_multiple_transportations_deps.xml",
    "/repository/transportation/checker_multiple_transportations.xml",
    "/repository/transportation/checker_registers.xml"
})
public class TransportationCheckerTest extends AbstractContextualTest {
    public static final ZonedDateTime SLOT_DATE_FROM = Instant
        .parse("2022-01-10T10:00:00Z")
        .atZone(ZoneId.systemDefault());
    public static final ZonedDateTime SLOT_DATE_TO = Instant
        .parse("2022-01-10T11:00:00Z")
        .atZone(ZoneId.systemDefault());

    public static final long GATE_ID = 100L;
    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private TransportationValidator validator;

    @Autowired
    private TransportationMetadataMapper metadataMapper;

    @Autowired
    private TransportationPartnerMethodMapper partnerMethodMapper;

    @Autowired
    private TransportationValidationErrorMapper validationErrorMapper;

    @Autowired
    private TransportationLegalInfoMapper legalInfoMapper;

    @Autowired
    private TransportationPartnerInfoMapper transportationPartnerInfoMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TransportationUpdateFacade transportationUpdateFacade;

    @Autowired
    private StartrekDtoConverter startrekDtoConverter;

    @Autowired
    private PartnerMethodsCheckService partnerMethodsCheckService;

    @Autowired
    private RegisterService registerService;

    @Mock
    private LMSClient lmsClient;

    @Mock
    private MarketIdService marketIdService;

    private TransportationChecker checker;

    @BeforeEach
    void init() {
        var logisticsPointReceiver = new LogisticsPointReceiver(lmsClient);
        var partnerSettingsReceiver = new PartnerSettingsReceiver(lmsClient);
        var partnerReceiver = new PartnerReceiver(lmsClient);
        var legalInfoReceiver = new LegalInfoReceiver(marketIdService, partnerReceiver);
        var cacheService = mock(CacheService.class);
        var calendaringServiceReceiver = mock(CalendaringServiceReceiver.class);

        var enricher = new TransportationMetadataEnricher(
            logisticsPointReceiver,
            partnerSettingsReceiver,
            partnerReceiver,
            legalInfoReceiver,
            calendaringServiceReceiver,
            partnerMethodsCheckService,
            new TimeSlotConverter(),
            cacheService
        );

        when(lmsClient.getLogisticsPoint(3L)).
            thenReturn(LmsFactory.logisticsPointResponse(3L, "111111"));
        // подставляем 51 партнёра, якобы СДЭК, для теста конвертации региона
        when(lmsClient.getLogisticsPoint(4L)).
            thenReturn(LmsFactory.pointFromMoscowRegion(51L));
        when(lmsClient.getLogisticsPoint(8L)).
            thenReturn(LmsFactory.logisticsPointResponse(5L, "333333"));
        when(lmsClient.getLogisticsPoint(9L)).
            thenReturn(LmsFactory.logisticsPointResponse(6L, "444444"));

        when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any()))
            .thenAnswer((Answer<List<SettingsMethodDto>>) invocation -> {
                SettingsMethodFilter filter = invocation.getArgument(0);
                List<SettingsMethodDto> result = new ArrayList<>(LmsFactory.settingsMethods());
                if (filter.getPartnerIds().contains(8L)) {
                    result.addAll(outboundSettingsMethods(8L));
                }
                if (filter.getPartnerIds().containsAll(List.of(100L, 200L))) {
                    result.clear();
                }
                if (filter.getPartnerIds().containsAll(List.of(30L, 40L))) {
                    result.clear();
                    result.addAll(outboundSettingsMethods(30L));
                }
                if (filter.getPartnerIds().containsAll(List.of(31L, 41L))) {
                    result.clear();
                    result.add(LmsFactory.settingsMethodDto(3L, 41L, "createIntake", true));
                }
                return result;
            });

        when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(LmsFactory.settingsApiDtos());

        when(lmsClient.getPartner(4L))
            .thenReturn(LmsFactory.optionalPartnerResponse(4L, 40L, PartnerType.SORTING_CENTER));
        when(lmsClient.getPartner(5L))
            .thenReturn(LmsFactory.optionalPartnerResponse(5L, 50L, PartnerType.SORTING_CENTER));
        when(lmsClient.getPartner(666L))
            .thenReturn(LmsFactory.optionalPartnerResponse(666L, 6660L, PartnerType.SORTING_CENTER));
        when(lmsClient.getPartner(8L))
            .thenReturn(LmsFactory.optionalPartnerResponse(8L, 80L, PartnerType.DISTRIBUTION_CENTER));

        when(marketIdService.findAccountById(40L))
            .thenReturn(MarketIdFactory.optionalMarketAccount(40L, "4040"));
        when(marketIdService.findAccountById(50L))
            .thenReturn(MarketIdFactory.optionalMarketAccount(40L, "5050"));

        when(calendaringServiceReceiver.getByCalendaringServiceId(1L))
            .thenReturn(Optional.of(new BookingResponseV2(
                1L,
                CalendaringServiceClientConfig.SOURCE,
                "TM100",
                null,
                GATE_ID,
                SLOT_DATE_FROM,
                SLOT_DATE_TO,
                BookingStatus.ACTIVE,
                LocalDateTime.now(clock),
                172L
            )));

        checker = new TransportationChecker(
            enricher,
            validator,
            startrekDtoConverter
        );
    }

    @Test
    void testHappyPath() {
        check(2L);

        TransportationMetadata transportationMetadata = metadataMapper.get(2L);
        softly.assertThat(transportationMetadata.getAddressFrom().getZipCode())
            .isEqualTo("111111");
        softly.assertThat(transportationMetadata.getAddressTo().getZipCode())
            .isEqualTo("101000");
        softly.assertThat(transportationMetadata.getAddressTo().getRegion())
            .isEqualTo("Москва и Московская область");
        softly.assertThat(transportationMapper.getById(2L).getStatus())
            .isEqualTo(TransportationStatus.SCHEDULED);
        softly.assertThat(partnerMethodMapper.get(2L))
            .isNotEmpty();
        softly.assertThat(partnerMethodMapper.get(2L).get(0).getApiType())
            .isNotNull();

        Transportation transportation = transportationMapper.getById(transportationMetadata.getTransportationId());
        softly.assertThat(transportation.getOutboundUnit().getBookedTimeSlot())
            .isEqualToIgnoringGivenFields(
                new TimeSlot()
                    .setCalendaringServiceId(1L)
                    .setGateId(GATE_ID)
                    .setZoneId(ZoneId.systemDefault().getId())
                    .setFromDate(SLOT_DATE_FROM.toLocalDateTime())
                    .setToDate(SLOT_DATE_TO.toLocalDateTime()),
                "id"
            );
        softly.assertThat(transportation.getInboundUnit().getBookedTimeSlot()).isNull();
    }

    @Test
    void testMethodApiSaving() {
        check(2L);

        Assertions.assertNotNull(partnerMethodMapper.get(2L).get(0).getApiType());
    }

    @Test
    void testScheduledHappyPath() {
        Transportation newTransportation = transportationMapper.getById(2L).setStatus(TransportationStatus.SCHEDULED);
        transportationMapper.updateByIdAndStatus(newTransportation, TransportationStatus.CHECK_PREPARED);
        check(2L);

        TransportationMetadata transportationMetadata = metadataMapper.get(2L);
        softly.assertThat(transportationMetadata.getAddressFrom().getZipCode())
            .isEqualTo("111111");
        softly.assertThat(transportationMetadata.getAddressTo().getZipCode())
            .isEqualTo("101000");
        softly.assertThat(transportationMetadata.getAddressTo().getRegion())
            .isEqualTo("Москва и Московская область");
        softly.assertThat(transportationMapper.getById(2L).getStatus())
            .isEqualTo(TransportationStatus.SCHEDULED);
        softly.assertThat(partnerMethodMapper.get(2L))
            .isNotEmpty();
        softly.assertThat(partnerMethodMapper.get(2L).get(0).getApiType()).isEqualTo(ApiType.DELIVERY);
    }

    @Test
    void testPartnerInfo() {
        check(2L);
        TransportationPartnerInfo partnerInfo = transportationPartnerInfoMapper.get(2L, 4L);
        softly.assertThat(partnerInfo.getPartnerName()).isEqualTo("Partner market id#40");
    }

    @Test
    void testLegalInfo() {
        softly.assertThat(legalInfoMapper.getByTransportationAndPartnerId(2L, 4L)).isNull();

        check(2L);

        var info24 = legalInfoMapper.getByTransportationAndPartnerId(2L, 4L);
        var info25 = legalInfoMapper.getByTransportationAndPartnerId(2L, 5L);

        softly.assertThat(info24.getInn()).isEqualTo("4040");
        softly.assertThat(info25.getInn()).isEqualTo("5050");
        softly.assertThat(info25.getMarketId()).isEqualTo(40L);
        softly.assertThat(info25.getOgrn()).isEqualTo("regNumber");
        softly.assertThat(info25.getLegalName()).isEqualTo("name");
        softly.assertThat(info25.getLegalType()).isEqualTo("type");
        softly.assertThat(info25.getLegalAddress()).isEqualTo("address");
    }

    @Test
    void testBreakBulkXdockSubtypeSet() {
        when(lmsClient.getPartner(8L))
            .thenReturn(LmsFactory.optionalPartnerResponse(8L, 80L, PartnerType.FULFILLMENT));

        softly.assertThat(transportationMapper.getById(5L).getSubtype()).isNull();

        check(5L);

        softly.assertThat(transportationMapper.getById(5L).getInboundUnit().getStatus())
            .isEqualTo(TransportationUnitStatus.NEW);
        softly.assertThat(transportationMapper.getById(5L).getSubtype())
            .isEqualTo(TransportationSubtype.BREAK_BULK_XDOCK);
    }

    @Test
    void testXdockDoNotNeedToSendInbound() {
        check(5L);
        softly.assertThat(transportationMapper.getById(5L).getInboundUnit().getStatus())
            .isEqualTo(TransportationUnitStatus.NEVER_SEND);
    }

    @Test
    void testInvalidTransportation() {
        check(3L);
        softly.assertThat(transportationMapper.getById(3L).getStatus())
            .isEqualTo(TransportationStatus.CHECK_FAILED);
        softly.assertThat(metadataMapper.get(3L))
            .isNull();
        softly.assertThat(validationErrorMapper.get(3L))
            .isNotEmpty();
    }

    private void check(long l) {
        transportationMapper.findById(l).ifPresent(transportation -> {
            TransportationStatus prevStatus = transportation.getStatus();
            EnrichedTransportation enrichedTransportation = checker.check(transportation);
            transportationUpdateFacade.updateExistingTransportationAndRecheck(
                enrichedTransportation,
                prevStatus,
                BacklogCodes.TRANSPORTATION_CHECKER
            );
        });
    }

    @Test
    void testCheckRerun() {
        check(4L);
        TransportationMetadata transportationMetadata = metadataMapper.get(4L);

        softly.assertThat(transportationMetadata.getAddressFrom().getZipCode())
            .isEqualTo("333333");
        softly.assertThat(transportationMetadata.getAddressTo().getZipCode())
            .isEqualTo("444444");
        softly.assertThat(transportationMapper.getById(4L).getStatus())
            .isEqualTo(TransportationStatus.SCHEDULED);
        softly.assertThat(partnerMethodMapper.get(4L))
            .isNotEmpty();
        softly.assertThat(validationErrorMapper.get(4L))
            .isEmpty();
        softly.assertThat(partnerMethodMapper.get(4L).stream().map(TransportationPartnerMethod::getApiType))
            .anyMatch(t -> t == ApiType.FULFILLMENT);
    }

    @Test
    void testCheckTransportationInInvalidStatus() {
        Assertions.assertThrows(
            TransportationCouldNotBeCheckedException.class,
            () -> check(1L)
        );
    }

    @Test
    void testSendingStrategyFroSortingCenter() {
        Transportation newTransportation = transportationMapper.getById(2L).setStatus(TransportationStatus.SCHEDULED);
        transportationMapper.updateByIdAndStatus(newTransportation, TransportationStatus.CHECK_PREPARED);
        check(2L);

        Transportation transportation = transportationMapper.getById(2L);
        var outbound = transportation.getOutboundUnit();
        var inbound = transportation.getInboundUnit();

        Assertions.assertEquals(outbound.getSendingStrategy(), UnitSendingStrategy.DIRECTLY_TO_LGW);
        Assertions.assertEquals(inbound.getSendingStrategy(), UnitSendingStrategy.DIRECTLY_TO_LGW);
    }

    @Test
    void checkSchemeIsUnknown() {
        check(500L);

        var transportation = transportationMapper.getById(500L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.UNKNOWN);
    }

    @Test
    void checkInboundIsDoNotNeedSending() {
        check(6L);

        var transportation = transportationMapper.getById(6L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.NEW);
        softly.assertThat(transportation.getInboundUnit().getStatus())
            .isEqualTo(TransportationUnitStatus.DO_NOT_NEED_TO_SEND);
        softly.assertThat(transportation.getMovement().getStatus()).isEqualTo(MovementStatus.DO_NOT_NEED_TO_SEND);
        Register inboundPlan =
            registerService.getByTransportationUnitIdAndType(40L, RegisterType.PLAN);
        softly.assertThat(inboundPlan.getStatus()).isEqualTo(RegisterStatus.DO_NOT_NEED_TO_SEND);
    }

    @Test
    void checkSchemeIsCombined() {
        check(4L);

        var transportation = transportationMapper.getById(4L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.COMBINED);
    }

    @Test
    void checkSchemeIsOld() {
        check(7L);

        var transportation = transportationMapper.getById(7L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.OLD);
    }

    @Nonnull
    public List<SettingsMethodDto> outboundSettingsMethods(Long partnerId) {
        return List.of(
            LmsFactory.settingsMethodDto(4L, partnerId, "getOutbound", true),
            LmsFactory.settingsMethodDto(4L, partnerId, "getOutboundStatus", true),
            LmsFactory.settingsMethodDto(4L, partnerId, "getOutboundStatusHistory", true),
            LmsFactory.settingsMethodDto(4L, partnerId, "putOutbound", true),
            LmsFactory.settingsMethodDto(4L, partnerId, "putOutboundRegistry", true)
        );
    }

}
