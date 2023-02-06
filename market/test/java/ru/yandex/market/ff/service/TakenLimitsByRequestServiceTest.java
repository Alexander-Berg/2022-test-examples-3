package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.dto.quota.TakeQuotaDto;
import ru.yandex.market.ff.client.dto.quota.UpdateQuotaDto;
import ru.yandex.market.ff.client.enums.DailyLimitsType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.config.ServiceConfiguration;
import ru.yandex.market.ff.exception.http.ConflictException;
import ru.yandex.market.ff.model.bo.AvailableRequestSize;
import ru.yandex.market.ff.model.bo.AvailableRequestSizeDefault;
import ru.yandex.market.ff.model.bo.TakenSupplyLimits;
import ru.yandex.market.ff.model.bo.TakenSupplyLimitsForDate;
import ru.yandex.market.ff.model.dto.DailyLimitDto;
import ru.yandex.market.ff.model.entity.DailySupplyLimit;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.TakenLimitsByRequest;
import ru.yandex.market.ff.repository.TakenLimitsByRequestRepository;
import ru.yandex.market.ff.service.implementation.PublishToLogbrokerCalendarShopRequestChangeService;
import ru.yandex.market.ff.service.implementation.TakenLimitsByRequestServiceImpl;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TakenLimitsByRequestServiceTest {

    private TakenLimitsByRequestServiceImpl service;
    private TakenLimitsByRequestRepository repository;
    private RequestSizeCalculationService requestSizeCalculationService;
    private SoftAssertions assertions;
    private TransactionTemplate transactionTemplate;
    private LimitService limitService;
    private PublishToLogbrokerCalendarShopRequestChangeService publishToLogbrokerCalendarShopRequestChangeService;
    private CalendarBookingService calendarBookingService;

    @BeforeEach
    public void init() {

        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        Map<RequestType, DailyLimitsType> requestTypeDailyLimitsTypeMap =
                serviceConfiguration.requestTypeToDailyLimitsTypeMap();

        repository = Mockito.mock(TakenLimitsByRequestRepository.class);
        requestSizeCalculationService = Mockito.mock(RequestSizeCalculationService.class);
        transactionTemplate = Mockito.mock(TransactionTemplate.class);
        limitService = Mockito.mock(LimitService.class);
        publishToLogbrokerCalendarShopRequestChangeService =
                mock(PublishToLogbrokerCalendarShopRequestChangeService.class);
        calendarBookingService = mock(CalendarBookingService.class);
        service = new TakenLimitsByRequestServiceImpl(repository, requestSizeCalculationService,
            ImmutableSet.of(RequestType.SUPPLY), ImmutableMap.of(RequestStatus.CREATED,
            ImmutableSet.of(RequestStatus.WAITING_FOR_CONFIRMATION, RequestStatus.VALIDATED)),
            ImmutableSet.of(RequestStatus.CANCELLED, RequestStatus.REJECTED_BY_SERVICE),
                transactionTemplate, limitService, publishToLogbrokerCalendarShopRequestChangeService,
                calendarBookingService,
                requestTypeDailyLimitsTypeMap);
        assertions = new SoftAssertions();

        Mockito.when(requestSizeCalculationService.countTakenPallets(anyLong())).thenReturn(2L);
        Mockito.when(requestSizeCalculationService.countTakenItems(anyLong())).thenReturn(6L);
        Mockito.when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback) invocation.getArgument(0)).doInTransaction(null));
        Mockito.when(limitService
                .getAvailableSupplySizeLockingQuota(any(Supplier.class), anyLong(), any(), any(LocalDate.class),
                        any(RequestType.class)))
                .thenReturn(new AvailableRequestSizeDefault());
        Mockito.when(limitService
                .getAvailableSupplySizeLockingQuota(any(SupplierType.class), anyLong(), any(),
                        any(LocalDate.class), any(DailyLimitsType.class)))
                .thenReturn(new AvailableRequestSizeDefault());
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void saveTakenLimitsWorksCorrectWithEnoughQuota() {
        LocalDateTime requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        ShopRequest request = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);
        service.saveTakenLimits(request, requestedDate.toLocalDate());

        ArgumentCaptor<TakenLimitsByRequest> captor = ArgumentCaptor.forClass(TakenLimitsByRequest.class);
        verify(repository).save(captor.capture());

        TakenLimitsByRequest captured = captor.getValue();
        assertions.assertThat(captured.getShopRequest()).isEqualTo(request);
        assertions.assertThat(captured.getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
        assertions.assertThat(captured.getTakenItems()).isEqualTo(6);
        assertions.assertThat(captured.getTakenPallets()).isEqualTo(2);
    }

    @Test
    public void saveTakenLimitsForBookingWorksCorrectWithEnoughQuota() {
        LocalDate limitDate = LocalDate.of(2020, 11, 11);
        AvailableRequestSize quota = new AvailableRequestSizeDefault(new DailySupplyLimit(),
                0L, BigDecimal.ZERO, limitDate);
        when(limitService
                .getAvailableSupplySizeLockingQuota(any(SupplierType.class), anyLong(), any(),
                        eq(limitDate), any(DailyLimitsType.class)))
                .thenReturn(quota);

        TakeQuotaDto takeQuotaDto = getTakeQuotaDto();
        service.getOrSaveTakenLimits(takeQuotaDto);

        ArgumentCaptor<TakenLimitsByRequest> captor = ArgumentCaptor.forClass(TakenLimitsByRequest.class);
        verify(repository).save(captor.capture());

        TakenLimitsByRequest captured = captor.getValue();
        assertions.assertThat(captured.getLimitDate()).isEqualTo("2020-11-12");
        assertions.assertThat(captured.getShopRequest()).isNull();
        assertions.assertThat(captured.getBookingId()).isEqualTo(takeQuotaDto.getBookingId());
        assertions.assertThat(captured.getSupplierType()).isEqualTo(takeQuotaDto.getSupplierType());
        assertions.assertThat(captured.getTakenItems()).isEqualTo(takeQuotaDto.getItems());
        assertions.assertThat(captured.getTakenPallets()).isEqualTo(takeQuotaDto.getPallets());
        assertions.assertThat(captured.getTakenMeasurements()).isEqualTo(takeQuotaDto.getMeasurements());
    }

    @Test
    public void saveTakenLimitsIsProhibitedWithQuotaExceededForThirdParty() {

        LocalDateTime requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        AvailableRequestSize quota = new AvailableRequestSizeDefault(new DailySupplyLimit(),
                0L, BigDecimal.ZERO, requestedDate.toLocalDate());
        when(limitService
                .getAvailableSupplySizeLockingQuota(any(Supplier.class), anyLong(), any(), any(LocalDate.class),
                        any(RequestType.class)))
                .thenReturn(quota);

        ShopRequest request = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                service.saveTakenLimits(request, requestedDate.toLocalDate()));

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).contains(
                        "Limits for THIRD_PARTY were exceeded by request 22"
                ));
    }

    @Test
    public void saveTakenLimitsForBookingIsProhibitedWithQuotaExceededForThirdParty() {
        LocalDate limitDate = LocalDate.of(2020, 11, 11);
        AvailableRequestSize quota = new AvailableRequestSizeDefault(new DailySupplyLimit(),
                0L, BigDecimal.ZERO, limitDate);
        when(limitService
                .getAvailableSupplySizeLockingQuota(any(SupplierType.class), anyLong(), any(),
                        any(LocalDate.class), any(DailyLimitsType.class)))
                .thenReturn(quota);

        TakeQuotaDto takeQuotaDto = getTakeQuotaDto();

        ConflictException exception = assertThrows(ConflictException.class, () ->
                service.getOrSaveTakenLimits(takeQuotaDto));

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).contains(
                        "Limits for THIRD_PARTY were exceeded by booking 55"
                ));
    }

    @Test
    public void saveTakenLimitsIsProhibitedWithQuotaExceededForFirstParty() {

        LocalDateTime requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();

        AvailableRequestSize quota = new AvailableRequestSizeDefault(new DailySupplyLimit(),
                0L, BigDecimal.ZERO, requestedDate.toLocalDate());
        when(limitService
                .getAvailableSupplySizeLockingQuota(any(Supplier.class), anyLong(), any(), any(LocalDate.class),
                        any(RequestType.class)))
                .thenReturn(quota);

        ShopRequest request = getShopRequest(requestedDate, SupplierType.FIRST_PARTY);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                service.saveTakenLimits(request, requestedDate.toLocalDate()));

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).contains(
                        "Limits for FIRST_PARTY were exceeded by request 22"
                ));
    }

    @Test
    public void getTakenLimitsWorksCorrect() {
        LocalDate date = LocalDate.of(2019, 11, 11);
        TakenSupplyLimits expectedResult = new TakenSupplyLimits(100L, 10L, 0L);
        Set<RequestType> requestTypes = EnumSet.of(RequestType.SUPPLY, RequestType.SHADOW_SUPPLY);
        Mockito.when(repository.getTakenLimits(100, null, date, SupplierType.FIRST_PARTY, requestTypes,
                EnumSet.of(DailyLimitsType.SUPPLY)))
            .thenReturn(expectedResult);

        TakenSupplyLimits takenLimits =
                service.getTakenLimits(100, null, date, SupplierType.FIRST_PARTY, requestTypes);
        assertions.assertThat(takenLimits).isEqualTo(expectedResult);
    }

    @Test
    public void getTakenLimitsForDatesWorksCorrect() {
        LocalDate firstDate = LocalDate.of(2019, 11, 12);
        LocalDate secondDate = LocalDate.of(2019, 11, 13);

        TakenSupplyLimitsForDate limitsForDate1 = new TakenSupplyLimitsForDate(0L, 10L, 1L, firstDate);
        TakenSupplyLimitsForDate limitsForDate2 = new TakenSupplyLimitsForDate(0L, 22L, 2L, firstDate);
        TakenSupplyLimitsForDate limitsForDate3 = new TakenSupplyLimitsForDate(0L, 11L, 4L, secondDate);
        TakenSupplyLimitsForDate limitsForDate4 = new TakenSupplyLimitsForDate(0L, 25L, 5L, secondDate);
        Set<RequestType> requestTypes = EnumSet.of(RequestType.SUPPLY, RequestType.SHADOW_SUPPLY);
        Mockito.when(repository.getTakenLimitsForDates(100, null, List.of(firstDate, secondDate),
                SupplierType.THIRD_PARTY, requestTypes, EnumSet.of(DailyLimitsType.SUPPLY)))
            .thenReturn(asList(limitsForDate1, limitsForDate2, limitsForDate3));
        Mockito.when(repository.getTakenLimitsForDates(100, null, List.of(firstDate, secondDate),
                SupplierType.THIRD_PARTY, EnumSet.of(RequestType.WITHDRAW), EnumSet.of(DailyLimitsType.WITHDRAW)))
                .thenReturn(Collections.singletonList(limitsForDate4));

        Map<LocalDate, TakenSupplyLimits> takenLimitsForDates =
            service.getTakenLimitsForDates(100, SupplierType.THIRD_PARTY, asList(firstDate, secondDate), requestTypes);
        assertions.assertThat(takenLimitsForDates.size()).isEqualTo(2);
        TakenSupplyLimits firstLimit = takenLimitsForDates.get(firstDate);
        assertions.assertThat(firstLimit).isNotNull();
        assertions.assertThat(firstLimit.getTakenItems()).isEqualTo(32);
        assertions.assertThat(firstLimit.getTakenPallets()).isEqualTo(3);

        TakenSupplyLimits secondLimit = takenLimitsForDates.get(secondDate);
        assertions.assertThat(secondLimit).isNotNull();
        assertions.assertThat(secondLimit.getTakenItems()).isEqualTo(11);
        assertions.assertThat(secondLimit.getTakenPallets()).isEqualTo(4);
    }

    @Test
    public void findByRequestIdWorksCorrect() {
        LocalDate limitDate = LocalDate.of(2020, 12, 12);
        TakenLimitsByRequest expected = new TakenLimitsByRequest();
        expected.setId(100L);
        expected.setLimitDate(limitDate);
        Mockito.when(repository.findByRequestIdAndLimitDateInRequests(100, limitDate))
                .thenReturn(Optional.of(expected));
        Optional<TakenLimitsByRequest> actual = service.findByRequestIdAndLimitDate(100, limitDate);
        assertions.assertThat(actual).isPresent();
        assertions.assertThat(expected).isEqualTo(actual.get());
    }

    @Test
    public void deleteByRequestIdWorksCorrect() {
        service.deleteByRequestId(10);
        verify(repository).deleteByRequestId(10);
    }

    @Test
    void truncateTakenLimitsWithExceededTakenItems() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        var takenPallets = 10L;
        var takenItems = 100L;

        TakenLimitsByRequest takenLimitsByRequest = createTakenLimits(shopRequest, takenPallets, takenItems);

        when(requestSizeCalculationService.countTakenPallets(shopRequest.getId())).thenReturn(takenPallets);
        when(requestSizeCalculationService.countTakenItems(shopRequest.getId())).thenReturn(takenItems + 1);

        when(repository.findAllByRequestIdInRequests(shopRequest.getId())).thenReturn(List.of(takenLimitsByRequest));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.truncateTakenLimitsIfPresent(shopRequest)
        );

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).isEqualTo(
                        "Taken items count have been exceeded for truncating limits request " +
                                "with requestId or bookingId=22. " +
                                "101 (requested taken items) > 100 (current taken items)."
                ));

        verify(repository, times(0)).save(any(TakenLimitsByRequest.class));
    }

    @Test
    void truncateTakenLimitsWithExceededTakenPallets() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        var takenPallets = 10L;
        var takenItems = 100L;

        TakenLimitsByRequest takenLimitsByRequest = createTakenLimits(shopRequest, takenPallets, takenItems);

        when(requestSizeCalculationService.countTakenPallets(shopRequest.getId())).thenReturn(takenPallets + 1);
        when(requestSizeCalculationService.countTakenItems(shopRequest.getId())).thenReturn(takenItems);

        when(repository.findAllByRequestIdInRequests(shopRequest.getId())).thenReturn(List.of(takenLimitsByRequest));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.truncateTakenLimitsIfPresent(shopRequest)
        );

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).isEqualTo(
                        "Taken pallets count have been exceeded for truncating limits request " +
                                "with requestId or bookingId=22. " +
                                "11 (requested taken pallets) > 10 (current taken pallets)."
                ));

        verify(repository, times(0)).save(any(TakenLimitsByRequest.class));
    }

    @Test
    void truncateTakenLimitsForRequestWithMultipleTakenLimits() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        var takenPallets = 10L;
        var takenItems = 100L;

        TakenLimitsByRequest takenLimitsByRequest1 = createTakenLimits(shopRequest, takenPallets, takenItems);
        TakenLimitsByRequest takenLimitsByRequest2 = createTakenLimits(shopRequest, takenPallets, takenItems);

        when(repository.findAllByRequestIdInRequests(shopRequest.getId()))
                .thenReturn(List.of(takenLimitsByRequest1, takenLimitsByRequest2));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.truncateTakenLimitsIfPresent(shopRequest)
        );

        SoftAssertions.assertSoftly(softly ->
                softly.assertThat(exception.getMessage()).isEqualTo("Updating request must have only one taken slot"));

        verify(repository, times(0)).save(any(TakenLimitsByRequest.class));
    }

    @Test
    void truncateTakenLimits() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        var takenPallets = 10L;
        var takenItems = 100L;

        var expectedTakenPallets = 5L;
        var expectedTakenItems = 50L;

        TakenLimitsByRequest takenLimitsByRequest = createTakenLimits(shopRequest, takenPallets, takenItems);

        when(requestSizeCalculationService.countTakenPallets(shopRequest.getId())).thenReturn(expectedTakenPallets);
        when(requestSizeCalculationService.countTakenItems(shopRequest.getId())).thenReturn(expectedTakenItems);

        when(repository.findAllByRequestIdInRequests(shopRequest.getId())).thenReturn(List.of(takenLimitsByRequest));

        service.truncateTakenLimitsIfPresent(shopRequest);

        ArgumentCaptor<TakenLimitsByRequest> argumentCaptor = ArgumentCaptor.forClass(TakenLimitsByRequest.class);
        verify(repository, times(1)).save(argumentCaptor.capture());

        var actualLimits = argumentCaptor.getValue();

        assertions.assertThat(actualLimits.getTakenItems()).isEqualTo(expectedTakenItems);
        assertions.assertThat(actualLimits.getTakenPallets()).isEqualTo(expectedTakenPallets);
        assertions.assertThat(actualLimits.getShopRequest().getId()).isEqualTo(shopRequest.getId());
        assertions.assertThat(actualLimits.getLimitDate()).isEqualTo(takenLimitsByRequest.getLimitDate());
        assertions.assertThat(actualLimits.getSupplierType()).isEqualTo(shopRequest.getSupplier().getSupplierType());
    }

    @Test
    void truncateOnAbsentTakenLimits() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        when(repository.findAllByRequestIdInRequests(shopRequest.getId())).thenReturn(List.of());

        service.truncateTakenLimitsIfPresent(shopRequest);

        verify(requestSizeCalculationService, never()).countTakenPallets(anyLong());
        verify(requestSizeCalculationService, never()).countTakenItems(anyLong());
        verify(repository, never()).save(any(TakenLimitsByRequest.class));
    }

    @Test
    void testUpdateQuotaDecreasing() {
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        var shopRequest = getShopRequest(requestedDate, SupplierType.THIRD_PARTY);

        long bookingId = 1L;
        var takenPallets = 10L;
        var takenItems = 100L;

        TakenLimitsByRequest takenLimitsByRequest = createTakenLimits(shopRequest, takenPallets, takenItems);
        when(repository.findByBookingId(bookingId)).thenReturn(Optional.of(takenLimitsByRequest));

        service.updateQuota(new UpdateQuotaDto(bookingId, 10, 1, true));
    }

    @Test
    void testUpdateQuotaIncreasing() {
        long bookingId = 1L;
        var requestedDate = LocalDate.of(2020, 11, 11).atStartOfDay();
        SupplierType supplierType = SupplierType.THIRD_PARTY;
        var shopRequest = getShopRequest(requestedDate, supplierType);
        long updatingItemsCount = 10;
        long updatingPalletsCount = 10;
        TakenLimitsByRequest takenLimits = createTakenLimits(shopRequest, 1L, 1L);
        takenLimits.setBookingId(bookingId);
        LocalDate limitDate = LocalDate.of(2020, 6, 6);


        when(repository.findByBookingId(bookingId)).thenReturn(Optional.of(takenLimits));
        when(repository.save(any(TakenLimitsByRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByBookingIdAndLimitDate(bookingId, limitDate))
                .thenReturn(Optional.of(takenLimits));
        when(limitService.getAvailableSupplySizeLockingQuota(any(SupplierType.class), any(), any(), any(),
                any(DailyLimitsType.class)))
                .thenReturn(new AvailableRequestSizeDefault(new DailyLimitDto(), updatingItemsCount,
                        BigDecimal.valueOf(updatingPalletsCount), limitDate));

        service.updateQuota(new UpdateQuotaDto(bookingId, updatingItemsCount, updatingPalletsCount, false));

        ArgumentCaptor<TakenLimitsByRequest> captor = ArgumentCaptor.forClass(TakenLimitsByRequest.class);
        verify(repository).save(captor.capture());

        TakenLimitsByRequest captured = captor.getValue();
        assertions.assertThat(captured.getShopRequest()).isEqualTo(shopRequest);
        assertions.assertThat(captured.getSupplierType()).isEqualTo(supplierType);
        assertions.assertThat(captured.getTakenItems()).isEqualTo(updatingItemsCount);
        assertions.assertThat(captured.getTakenPallets()).isEqualTo(updatingPalletsCount);
    }

    @Nonnull
    private TakenLimitsByRequest createTakenLimits(ShopRequest request, long takenPallets, long takenItems) {
        var takenLimitsByRequest = new TakenLimitsByRequest();
        takenLimitsByRequest.setTakenPallets(takenPallets);
        takenLimitsByRequest.setTakenItems(takenItems);
        takenLimitsByRequest.setShopRequest(request);
        takenLimitsByRequest.setSupplierType(request.getSupplier().getSupplierType());
        takenLimitsByRequest.setLimitDate(LocalDate.of(2020, 6, 6));
        takenLimitsByRequest.setLimitType(DailyLimitsType.SUPPLY);

        return takenLimitsByRequest;
    }

    @Nonnull
    private ShopRequest getShopRequest(LocalDateTime requestedDate, SupplierType supplierType) {
        ShopRequest request = new ShopRequest();
        request.setId(22L);

        RequestItem item = new RequestItem();
        request.setItems(asList(item, item, item));

        Supplier supplier = new Supplier();
        supplier.setSupplierType(supplierType);

        request.setSupplier(supplier);
        request.setRequestedDate(requestedDate);
        request.setServiceId(100500L);
        request.setType(RequestType.SUPPLY);
        return request;
    }

    @Nonnull
    private TakeQuotaDto getTakeQuotaDto() {
        return TakeQuotaDto.builder()
                .quotaType(DailyLimitsType.WITHDRAW)
                .bookingId(55L)
                .quotaType(DailyLimitsType.MOVEMENT_SUPPLY)
                .serviceId(300L)
                .supplierType(SupplierType.THIRD_PARTY)
                .items(100L)
                .pallets(10L)
                .measurements(50L)
                .possibleDates(List.of(LocalDate.of(2020, 11, 11), LocalDate.of(2020, 11, 12)))
                .build();
    }
}
