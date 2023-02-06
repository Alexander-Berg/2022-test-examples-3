package ru.yandex.market.api.partner.controllers.stats;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.MappingInfo;
import ru.yandex.market.api.partner.controllers.stats.model.TariffDTO;
import ru.yandex.market.api.partner.controllers.stats.model.TariffType;
import ru.yandex.market.core.fulfillment.calculator.TarifficatorService;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.TariffValue;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.core.partner.PartnerTypeAwareService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link TariffsService}
 */
@ExtendWith(MockitoExtension.class)
class TariffsServiceTest {

    private static final long SUPPLIER_ID = 10101L;

    private static final Map<String, MappingInfo> MAPPINGS = Stream.of(
            MappingInfo.newBuilder().setShopSku("someSku").setPrice(BigDecimal.valueOf(100.50)).build(),
            MappingInfo.newBuilder().setShopSku("andSku").setPrice(BigDecimal.ZERO).build(),
            MappingInfo.newBuilder().setShopSku("skuSku").build()
    ).collect(Collectors.toMap(MappingInfo::getShopSku, Function.identity()));

    private TariffsService tariffsService;

    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;

    @Mock
    private TarifficatorService tarifficatorService;

    @BeforeEach
    void setUp() {
        tariffsService = new TariffsService(tarifficatorService, partnerTypeAwareService);
        mockServices();
    }

    @Test
    void getTariffs() {
        Map<String, List<TariffDTO>> tariffs = tariffsService.getTariffs(SUPPLIER_ID, MAPPINGS);

        Assertions.assertEquals(tariffs.size(), 3);
        Assertions.assertEquals(tariffs.get("someSku").size(), 6);

        List<TariffDTO> skuTariffs = tariffs.get("someSku");
        Map<TariffType, TariffDTO> tariffByType =
                skuTariffs.stream().collect(Collectors.toMap(TariffDTO::getTariffType, Function.identity()));

        assertTariffEquals(
                new TariffDTO(TariffType.AGENCY_COMMISSION, new BigDecimal("2.00"), new BigDecimal("2.01")),
                tariffByType.get(TariffType.AGENCY_COMMISSION)
        );
        assertTariffEquals(
                new TariffDTO(TariffType.FULFILLMENT, new BigDecimal("10.00"), new BigDecimal("10.05")),
                tariffByType.get(TariffType.FULFILLMENT)
        );
        assertTariffEquals(
                new TariffDTO(TariffType.STORAGE, null, new BigDecimal("150.00")),
                tariffByType.get(TariffType.STORAGE)
        );
        assertTariffEquals(
                new TariffDTO(TariffType.WITHDRAW, new BigDecimal("7.50"), new BigDecimal("7.54")),
                tariffByType.get(TariffType.WITHDRAW)
        );
        assertTariffEquals(
                new TariffDTO(TariffType.SURPLUS, new BigDecimal("1.50"), new BigDecimal("1.51")),
                tariffByType.get(TariffType.SURPLUS)
        );
        assertTariffEquals(
                new TariffDTO(TariffType.FEE, new BigDecimal("5.00"), new BigDecimal("5.03")),
                tariffByType.get(TariffType.FEE)
        );
    }

    private void mockServices() {
        when(partnerTypeAwareService.isFulfillmentSupplier(anyLong())).thenReturn(true);
        when(partnerTypeAwareService.isDropship(anyLong())).thenReturn(false);
        when(partnerTypeAwareService.isCrossdockSupplier(anyLong())).thenReturn(false);

        when(tarifficatorService.getAgencyCommissions(anyLong()))
                .thenReturn(List.of(new TariffValue(200, ValueType.RELATIVE, BillingUnit.ITEM)));
        when(tarifficatorService.getFfProcessing(any(), any(), any(), any(), nullable(Long.class)))
                .thenReturn(
                        List.of(
                                new TariffValue(1000, ValueType.RELATIVE, BillingUnit.ITEM)
                        )
                );
        when(tarifficatorService.getFfStorageBillingTariffMeta(any(), any(), nullable(Long.class)))
                .thenReturn(new TariffValue(15000, ValueType.ABSOLUTE, BillingUnit.ITEM));

        TariffValue fee = new TariffValue(500, ValueType.RELATIVE, BillingUnit.ITEM);
        when(tarifficatorService.getFeeByShopSku(anyLong(), anyMap(), any()))
                .thenReturn(Map.of("someSku", fee, "andSku", fee, "skuSku", fee));

        when(tarifficatorService.getFfWithdraw(any(), any()))
                .thenReturn(new TariffValue(750, ValueType.RELATIVE, BillingUnit.ITEM));
        when(tarifficatorService.getFfSurplusSupply(any()))
                .thenReturn(new TariffValue(150, ValueType.RELATIVE, BillingUnit.ITEM));
    }

    private void assertTariffEquals(TariffDTO expected, TariffDTO actual) {
        Assertions.assertEquals(expected.getAmount(), actual.getAmount());
        Assertions.assertEquals(expected.getPercent(), actual.getPercent());
    }
}
