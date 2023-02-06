package ru.yandex.market.ff.service.implementation;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.AvailableRequestSize;
import ru.yandex.market.ff.model.bo.AvailableRequestSizeDefault;
import ru.yandex.market.ff.model.entity.DailyLimitIdentifier;
import ru.yandex.market.ff.model.entity.DailySupplyLimit;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.supplier.model.SupplierType.THIRD_PARTY;

@ExtendWith(MockitoExtension.class)
public class LimitServiceImplUnitTest extends SoftAssertionSupport {

    private static final LocalDate NOW = LocalDate.now();
    private static final long SERVICE_ID = 1L;

    @Mock
    private ConcreteEnvironmentParamService environmentParamService;
    @Mock
    private DateTimeService dateTimeService;

    @InjectMocks
    @Spy
    private LimitServiceImpl supplyLimitService;

    @BeforeEach
    public void setup() {
        when(environmentParamService.getSecondaryQuotaRequirementForService(SERVICE_ID)).thenReturn(100L);
        when(dateTimeService.localDateNow()).thenReturn(NOW);
    }

    @Test
    public void remainingQuotaEqualThreshold() {
        setupAvailableSupplySizeForDate(100L, 100L);

        List<LocalDate> result = supplyLimitService.getNearestDatesWithEnoughSecondaryQuotaLeft(
                SERVICE_ID, SupplierType.THIRD_PARTY, RequestType.SUPPLY);

        assertions.assertThat(result).contains(NOW);
    }

    @Test
    public void noRemainingQuotaProvided() {
        setupAvailableSupplySizeForDate(null, null);

        var result = supplyLimitService.getNearestDatesWithEnoughSecondaryQuotaLeft(
                SERVICE_ID, SupplierType.THIRD_PARTY, RequestType.SUPPLY);

        assertions.assertThat(result).contains(NOW);
    }

    @Test
    public void remainingQuotaLessThanThreshold() {
        setupAvailableSupplySizeForDate(100L, 99L);

        var result = supplyLimitService.getNearestDatesWithEnoughSecondaryQuotaLeft(
                SERVICE_ID, SupplierType.THIRD_PARTY, RequestType.SUPPLY);

        assertions.assertThat(result).isEmpty();
    }

    @Test
    public void multipleDaysNotEnoughQuotaSecondDay() {
        setupAvailableSupplySizeForDate(
                availableSupplySize(100L, 100L),
                availableSupplySize(100L, 99L),
                availableSupplySize(null, null)
        );

        var result = supplyLimitService.getNearestDatesWithEnoughSecondaryQuotaLeft(1,
                SupplierType.THIRD_PARTY, RequestType.SUPPLY);

        assertions.assertThat(result).contains(NOW, NOW.plusDays(2));
        assertions.assertThat(result).doesNotContain(NOW.plusDays(1));

    }

    private void setupAvailableSupplySizeForDate(Long itemsCount, Long availableItemsCount) {
        var toBeReturned = ImmutableMap.of(NOW, availableSupplySize(itemsCount, availableItemsCount));
        doReturn(toBeReturned).when(supplyLimitService).getAvailableRequestSizeForDate(
                eq(THIRD_PARTY), eq(SERVICE_ID), any(), eq(RequestType.SUPPLY));
        doReturn(Map.of()).when(supplyLimitService).getAvailableRequestSizeForDate(
                eq(THIRD_PARTY), eq(SERVICE_ID), any(), eq(RequestType.WITHDRAW));
    }

    private void setupAvailableSupplySizeForDate(AvailableRequestSize... values) {
        var builder = ImmutableMap.<LocalDate, AvailableRequestSize>builder();
        for (int i = 0; i < values.length; i++) {
            builder.put(NOW.plusDays(i), values[i]);
        }
        var toBeReturned = builder.build();

        doReturn(toBeReturned).when(supplyLimitService).getAvailableRequestSizeForDate(
                eq(THIRD_PARTY), eq(SERVICE_ID), any(), eq(RequestType.SUPPLY));
        doReturn(Map.of()).when(supplyLimitService).getAvailableRequestSizeForDate(
                eq(THIRD_PARTY), eq(SERVICE_ID), any(), eq(RequestType.WITHDRAW));
    }

    @NotNull
    private AvailableRequestSize availableSupplySize(Long itemsCount, Long availableItemsCount) {
        return new AvailableRequestSizeDefault(dailySupplyLimit(
                new DailyLimitIdentifier(SERVICE_ID, NOW, THIRD_PARTY), itemsCount, 0L),
                availableItemsCount, null, null);
    }

    private DailySupplyLimit dailySupplyLimit(DailyLimitIdentifier identifier, Long items, Long pallets) {
        DailySupplyLimit limit = new DailySupplyLimit();
        limit.setDailyLimitIdentifier(identifier);
        limit.setItemsCount(items);
        limit.setPalletsCount(pallets);
        return limit;
    }
}
