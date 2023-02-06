package ru.yandex.market.ff.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.model.bo.AvailableRequestSize;
import ru.yandex.market.ff.model.bo.AvailableRequestSizeDefault;
import ru.yandex.market.ff.model.entity.DailySupplyLimit;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemError;
import ru.yandex.market.ff.model.entity.RequestItemErrorAttribute;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.ExternalRequestItemErrorRepository;
import ru.yandex.market.ff.repository.LogisticUnitRepository;
import ru.yandex.market.ff.repository.RequestItemErrorRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.ShopRequestValidatedDatesRepository;
import ru.yandex.market.ff.service.CalendaringService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.ErrorDocumentGenerationService;
import ru.yandex.market.ff.service.ExternalRequestItemErrorService;
import ru.yandex.market.ff.service.LimitService;
import ru.yandex.market.ff.service.MbiNotificationService;
import ru.yandex.market.ff.service.RequestDocumentService;
import ru.yandex.market.ff.service.RequestItemErrorService;
import ru.yandex.market.ff.service.RequestSizeCalculationService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TakenLimitsByRequestService;
import ru.yandex.market.ff.service.ValidationResultSavingService;
import ru.yandex.market.ff.service.exception.QuotaExceededException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValidationResultSavingServiceUnitTest {

    private static final ShopRequest EMPTY_REQUEST = request();
    private static final long ID_1 = 10L;
    private static final long ID_2 = 20L;
    private static final String ARTICLE_1 = "article1";
    private static final BigDecimal AVAILABLE_PALLETS = BigDecimal.valueOf(3L);
    private static final long MARSCHROUTE_ID = 145;

    private ShopRequestModificationService shopRequestModificationService;
    private RequestItemRepository requestItemRepository;
    private RequestDocumentService documentService;
    private ErrorDocumentGenerationService errorDocumentGenerationService;
    private MbiNotificationService notificationService;
    private TransactionTemplate transactionTemplate;
    private RequestItemErrorRepository requestItemErrorRepository;
    private ExternalRequestItemErrorRepository externalRequestItemErrorRepository;
    private CalendaringService calendaringService;
    private TakenLimitsByRequestService takenLimitsByRequestService;
    private RequestSizeCalculationService supplySizeCalculationService;
    private LimitService limitService;
    private ValidationResultSavingService validationResultSavingService;
    private RequestDocumentService requestDocumentService;
    private LogisticUnitRepository logisticUnitRepository;
    private ShopRequestValidatedDatesRepository shopRequestValidatedDatesRepository;
    private DateTimeService dateTimeService;

    @BeforeEach
    public void init() {
        shopRequestModificationService = Mockito.mock(ShopRequestModificationService.class);
        requestItemRepository = Mockito.mock(RequestItemRepository.class);
        documentService = Mockito.mock(RequestDocumentService.class);
        errorDocumentGenerationService = Mockito.mock(ErrorDocumentGenerationService.class);
        notificationService = Mockito.mock(MbiNotificationService.class);
        transactionTemplate = Mockito.mock(TransactionTemplate.class);
        requestItemErrorRepository = Mockito.mock(RequestItemErrorRepository.class);
        externalRequestItemErrorRepository = Mockito.mock(ExternalRequestItemErrorRepository.class);
        calendaringService = Mockito.mock(CalendaringService.class);
        takenLimitsByRequestService = Mockito.mock(TakenLimitsByRequestService.class);
        supplySizeCalculationService = Mockito.mock(RequestSizeCalculationService.class);
        limitService = Mockito.mock(LimitService.class);
        requestDocumentService = Mockito.mock(RequestDocumentService.class);
        logisticUnitRepository = Mockito.mock(LogisticUnitRepository.class);
        shopRequestValidatedDatesRepository = Mockito.mock(ShopRequestValidatedDatesRepository.class);
        dateTimeService = Mockito.mock(DateTimeService.class);
        RequestItemErrorService requestItemErrorService = Mockito.mock(RequestItemErrorService.class);
        ExternalRequestItemErrorService externalRequestItemErrorService =
                Mockito.mock(ExternalRequestItemErrorService.class);

        validationResultSavingService = new ValidationResultSavingServiceImpl(requestItemRepository,
                shopRequestModificationService, transactionTemplate, errorDocumentGenerationService,
                documentService, notificationService, requestItemErrorRepository, externalRequestItemErrorRepository,
                calendaringService, takenLimitsByRequestService, supplySizeCalculationService,
                requestDocumentService, logisticUnitRepository, shopRequestValidatedDatesRepository, dateTimeService,
                requestItemErrorService, externalRequestItemErrorService);

        when(limitService.getAvailableSupplySizeLockingQuota(any(Supplier.class), anyLong(), anyLong(),
            any(LocalDate.class), any(RequestType.class)))
            .thenReturn(new AvailableRequestSizeDefault());
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            ((TransactionCallback) invocation.getArgument(0)).doInTransaction(null));
        when(takenLimitsByRequestService.shouldTakeLimitsOnStatusChange(any(), any())).thenReturn(true);
    }

    @Test
    void testCheckLimitsValid() {
        final DailySupplyLimit supplyLimit = new DailySupplyLimit();
        supplyLimit.setItemsCount(3L);
        supplyLimit.setPalletsCount(3L);

        final RequestItem item = item(ID_1, VatRate.VAT_20, "100");
        List<RequestItem> items = Collections.singletonList(item);
        when(supplySizeCalculationService.calcRequiredPalletsForRequestItems(items))
                .thenReturn(Map.of(ID_1, BigDecimal.ZERO));
        validationResultSavingService.checkQuotaAndSave(EMPTY_REQUEST, items, Collections.emptyMap(), null);

        verify(requestItemErrorRepository, never()).save(anyIterable());
    }

    @Test
    void testCheckLimitsInvalidItemsCount() {
        RequestItem item1 = item(ID_1, VatRate.VAT_20, "100");
        RequestItem item2 = item(ID_2, VatRate.VAT_20, "100");
        item2.setCount(4);
        List<RequestItem> items = Arrays.asList(item1, item2);
        when(supplySizeCalculationService.calcRequiredPalletsForRequestItems(items))
            .thenReturn(Map.of(ID_1, BigDecimal.ONE, ID_2, BigDecimal.ONE));
        assertInvalidItemsOrPalletsCount(EMPTY_REQUEST, items, 3, 3, "4", "2", "1", "2");
    }

    @Test
    void testCheckLimitsInvalidItemsCountForFirstPartySupplier() {
        RequestItem item1 = item(ID_1, VatRate.VAT_20, "100");
        RequestItem item2 = item(ID_2, VatRate.VAT_20, "100");
        item2.setCount(4);
        List<RequestItem> items = Arrays.asList(item1, item2);
        when(supplySizeCalculationService.calcRequiredPalletsForRequestItems(items))
            .thenReturn(Map.of(ID_1, BigDecimal.ONE, ID_2, BigDecimal.ONE));
        ShopRequest request = request(SupplierType.FIRST_PARTY, null);
        assertInvalidItemsOrPalletsCount(request, items, 3, 3, "4", "2", "1", "2");
        Mockito.reset(requestItemErrorRepository);

        request.setCalendaringMode(CalendaringMode.REQUIRED);
        assertInvalidItemsOrPalletsCount(request, items, 3, 3, null, null, null, null);
        Mockito.reset(requestItemErrorRepository);

        request.setCalendaringMode(CalendaringMode.NOT_REQUIRED);
        assertInvalidItemsOrPalletsCount(request, items, 3, 3, null, null, null, null);
    }

    @Test
    void testCheckLimitsInvalidPalletsCount() {
        assertCheckPalletsLimitsForRequestWorksCorrect(EMPTY_REQUEST, "9.29", "1.15");
    }

    @Test
    void testCheckLimitsInvalidPalletsCountForFirstParty() {
        ShopRequest request = request(SupplierType.FIRST_PARTY, null);
        assertCheckPalletsLimitsForRequestWorksCorrect(request, "9.29", "1.15");
        Mockito.reset(requestItemErrorRepository);
        assertCheckPalletsLimitsForRequestWorksCorrectWithNulls(request, null, null);
    }

    private void assertCheckPalletsLimitsForRequestWorksCorrect(@Nonnull ShopRequest request,
                                                                @Nullable String actualSupplyPalletsCount,
                                                                @Nullable String availableSupplyPalletsCount
                                                                ) {
        assertCheckPalletsLimitsForRequestWorksCorrect(request, actualSupplyPalletsCount, availableSupplyPalletsCount,
                "2", "2");
    }

    private void assertCheckPalletsLimitsForRequestWorksCorrectWithNulls(@Nonnull ShopRequest request,
                                                                         @Nullable String actualSupplyPalletsCount,
                                                                         @Nullable String availableSupplyPalletsCount) {
        assertCheckPalletsLimitsForRequestWorksCorrect(request, actualSupplyPalletsCount, availableSupplyPalletsCount,
                null, null);
    }

    private void assertCheckPalletsLimitsForRequestWorksCorrect(@Nonnull ShopRequest request,
                                                                @Nullable String actualSupplyPalletsCount,
                                                                @Nullable String availableSupplyPalletsCount,
                                                                @Nullable String actualSupplyItemsCount,
                                                                @Nullable String availableSupplyItemsCount) {
        RequestItem item1 = item(ID_1, VatRate.VAT_20, "100");
        // 1.85 pallets
        item1.setWidth(BigDecimal.valueOf(200.).setScale(2, RoundingMode.HALF_UP));
        item1.setHeight(BigDecimal.valueOf(130.).setScale(2, RoundingMode.HALF_UP));
        item1.setLength(BigDecimal.valueOf(100.).setScale(2, RoundingMode.HALF_UP));

        RequestItem item2 = item(ID_2, VatRate.VAT_20, "100");
        // 9.29 pallets
        item2.setWidth(BigDecimal.valueOf(250.).setScale(2, RoundingMode.HALF_UP));
        item2.setHeight(BigDecimal.valueOf(260.).setScale(2, RoundingMode.HALF_UP));
        item2.setLength(BigDecimal.valueOf(100.).setScale(2, RoundingMode.HALF_UP));
        item2.setCount(2);
        List<RequestItem> items = Arrays.asList(item1, item2);
        when(supplySizeCalculationService.calcRequiredPalletsForRequestItems(items))
                .thenReturn(Map.of(ID_1, BigDecimal.valueOf(1.85), ID_2, BigDecimal.valueOf(9.29)));
        assertInvalidItemsOrPalletsCount(request, items, 3, 3, actualSupplyItemsCount, availableSupplyItemsCount,
                actualSupplyPalletsCount, availableSupplyPalletsCount);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertInvalidItemsOrPalletsCount(@Nonnull ShopRequest request,
                                                  @Nonnull List<RequestItem> requestItems,
                                                  long quotaItemsCount,
                                                  long quotaPalletsCount,
                                                  @Nullable String actualSupplyItemsCount,
                                                  @Nullable String availableSupplyItemsCount,
                                                  @Nullable String actualSupplyPalletsCount,
                                                  @Nullable String availableSupplyPalletsCount) {
        DailySupplyLimit supplyLimit = new DailySupplyLimit();
        supplyLimit.setItemsCount(quotaItemsCount);
        supplyLimit.setPalletsCount(quotaPalletsCount);

        if (actualSupplyItemsCount == null && actualSupplyPalletsCount == null) {
            when(shopRequestModificationService.updateStatus(any(ShopRequest.class), any(RequestStatus.class)))
                    .thenReturn(request);
        } else {
            LocalDate limitDate = LocalDate.of(2022, 5, 19);
            AvailableRequestSize availableSupplySize =
                    new AvailableRequestSizeDefault(supplyLimit, 3L, AVAILABLE_PALLETS, limitDate);
            when(shopRequestModificationService.updateStatus(request, RequestStatus.VALIDATED))
                    .thenThrow(new QuotaExceededException("Quota exceeded", availableSupplySize));
        }

        validationResultSavingService.checkQuotaAndSave(request, requestItems, Collections.emptyMap(), null);
        if (actualSupplyItemsCount == null && actualSupplyPalletsCount == null) {
            verify(requestItemErrorRepository, never()).save(anyIterable());
            return;
        }
        ArgumentCaptor<Set<RequestItemError>> errorsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(requestItemErrorRepository).save(errorsCaptor.capture());
        Set<RequestItemError> errors = errorsCaptor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors.size()).isEqualTo(1);
            assertQuotaErrorHasCorrectParameters(errors.iterator().next(), String.valueOf(quotaItemsCount),
                String.valueOf(quotaPalletsCount), actualSupplyItemsCount, availableSupplyItemsCount,
                actualSupplyPalletsCount, availableSupplyPalletsCount, softly);
        });
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertQuotaErrorHasCorrectParameters(@Nonnull RequestItemError error,
                                                      @Nonnull String supplyItemsQuota,
                                                      @Nonnull String supplyPalletsQuota,
                                                      @Nullable String actualSupplyItemsCount,
                                                      @Nullable String availableSupplyItemsCount,
                                                      @Nullable String actualSupplyPalletsCount,
                                                      @Nullable String availableSupplyPalletsCount,
                                                      @Nonnull SoftAssertions softly) {
        softly.assertThat(error.getErrorType()).isEqualTo(RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED);
        List<RequestItemErrorAttribute> errorAttributes = error.getAttributes();
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.SUPPLY_ITEMS_QUOTA,
            supplyItemsQuota, softly);
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.SUPPLY_PALETTES_QUOTA,
            supplyPalletsQuota, softly);
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.ACTUAL_SUPPLY_ITEMS_COUNT,
            actualSupplyItemsCount, softly);
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.AVAILABLE_TO_SUPPLY_ITEMS_COUNT,
            availableSupplyItemsCount, softly);
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.ACTUAL_SUPPLY_PALETTES_COUNT,
            actualSupplyPalletsCount, softly);
        assertErrorHasValue(errorAttributes, RequestItemErrorAttributeType.AVAILABLE_TO_SUPPLY_PALETTES_COUNT,
            availableSupplyPalletsCount, softly);
    }

    private void assertErrorHasValue(@Nonnull List<RequestItemErrorAttribute> errors,
                                     @Nonnull RequestItemErrorAttributeType attributeType,
                                     @Nullable String value,
                                     @Nonnull SoftAssertions softly) {
        List<RequestItemErrorAttribute> filtered = errors.stream()
            .filter(attribute -> attribute.getType() == attributeType)
            .collect(Collectors.toList());
        if (value == null) {
            softly.assertThat(filtered.size()).as(attributeType.toString()).isEqualTo(0);
            return;
        }
        softly.assertThat(filtered.size()).as(attributeType.toString()).isEqualTo(1);
        RequestItemErrorAttribute errorAttribute = filtered.get(0);
        softly.assertThat(errorAttribute.getValue()).as(attributeType.toString()).isEqualTo(value);
    }

    private static ShopRequest request() {
        ShopRequest request = new ShopRequest();
        request.setId(ID_1);
        request.setSupplier(new Supplier(1, "supplier1", null, null,
                SupplierType.THIRD_PARTY, new SupplierBusinessType()));
        request.setRequestedDate(LocalDateTime.of(1984, 1, 1, 0, 0));
        request.setServiceId(MARSCHROUTE_ID);
        request.setType(RequestType.SUPPLY);
        return request;
    }

    private static ShopRequest request(SupplierType supplierType, String comment) {
        ShopRequest request = new ShopRequest();
        request.setId(ID_1);
        request.setSupplier(new Supplier(1, "supplier1", null, null, supplierType, new SupplierBusinessType()));
        request.setRequestedDate(LocalDateTime.of(1984, 1, 1, 0, 0));
        request.setComment(comment);
        request.setServiceId(MARSCHROUTE_ID);
        request.setType(RequestType.SUPPLY);
        return request;
    }

    private static RequestItem item(long id, VatRate vatRate, String supplyPrice) {
        RequestItem requestItem = new RequestItem();
        requestItem.setId(id);
        requestItem.setArticle(ARTICLE_1);
        requestItem.setVatRate(vatRate);
        requestItem.setSupplyPrice(new BigDecimal(supplyPrice));
        requestItem.setCount(1);
        requestItem.setRequestId(ID_1);
        return requestItem;
    }
}
