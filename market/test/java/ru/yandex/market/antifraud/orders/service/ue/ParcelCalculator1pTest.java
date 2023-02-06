package ru.yandex.market.antifraud.orders.service.ue;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.ue.UeParcelDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelItemDto;
import ru.yandex.market.antifraud.orders.storage.dao.TariffDao;
import ru.yandex.market.antifraud.orders.storage.entity.ue.Cogs;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class ParcelCalculator1pTest {


    @Test
    public void calculate1p() {
        TariffDao tariffDao = mock(TariffDao.class);
        when(tariffDao.getCogs(anyList())).thenReturn(
                List.of(
                        new Cogs(1L, new BigDecimal("90")),
                        new Cogs(2L, new BigDecimal("160"))
                )
        );
        ParcelCalculator parcelCalculator = new ParcelCalculator1p(tariffDao);
        UeParcelDto parcel = UeParcelDto.builder()
                .parcelItems(
                        List.of(
                                UeParcelItemDto.builder()
                                        .supplierId(UnitEconomicService.SUPPLIER_1P)
                                        .msku(1L)
                                        .count(2)
                                        .price(new BigDecimal("100"))
                                        .build(),
                                UeParcelItemDto.builder()
                                        .supplierId(UnitEconomicService.SUPPLIER_1P)
                                        .msku(2L)
                                        .count(3)
                                        .price(new BigDecimal("200"))
                                        .build()
                        )
                )
                .build();
        BigDecimal cost = parcelCalculator.calculateTotalCost(parcel);
        System.out.println(cost);
        assertThat(cost).isEqualByComparingTo(new BigDecimal("672"));
        BigDecimal revenue = parcelCalculator.calculateRevenue(parcel);
        System.out.println(revenue);
        assertThat(revenue).isEqualByComparingTo(new BigDecimal("800"));
    }


    @Test
    public void calculate3p() {
        TariffDao tariffDao = mock(TariffDao.class);
        when(tariffDao.getCogs(anyList())).thenReturn(
                List.of(
                        new Cogs(1L, new BigDecimal("90")),
                        new Cogs(2L, new BigDecimal("160"))
                )
        );
        ParcelCalculator parcelCalculator = new ParcelCalculator1p(tariffDao);
        UeParcelDto parcel = UeParcelDto.builder()
                .parcelItems(
                        List.of(
                                UeParcelItemDto.builder()
                                        .supplierId(UnitEconomicService.SUPPLIER_1P + 1)
                                        .msku(1L)
                                        .count(2)
                                        .price(new BigDecimal("100"))
                                        .build(),
                                UeParcelItemDto.builder()
                                        .supplierId(UnitEconomicService.SUPPLIER_1P + 2)
                                        .msku(2L)
                                        .count(3)
                                        .price(new BigDecimal("200"))
                                        .build()
                        )
                )
                .build();
        BigDecimal cost = parcelCalculator.calculateTotalCost(parcel);
        assertThat(cost).isEqualTo(BigDecimal.ZERO);
        BigDecimal revenue = parcelCalculator.calculateRevenue(parcel);
        assertThat(revenue).isEqualTo(BigDecimal.ZERO);
    }

}
