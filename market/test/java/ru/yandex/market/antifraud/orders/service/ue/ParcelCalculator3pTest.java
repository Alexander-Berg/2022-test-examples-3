package ru.yandex.market.antifraud.orders.service.ue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.ue.UeParcelDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelItemDto;
import ru.yandex.market.antifraud.orders.storage.dao.TariffDao;
import ru.yandex.market.antifraud.orders.storage.entity.ue.UeGlobalParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class ParcelCalculator3pTest {

    private TariffDao tariffDao;


    @Before
    public void init() {
        tariffDao = mock(TariffDao.class);
        when(tariffDao.getLatestGlobalParams()).thenReturn(Optional.of(
                UeGlobalParams.builder()
                        .fee3p(new BigDecimal("50.49"))
                        .feeFf3p(new BigDecimal("50.49"))
                        .storageRevenue(new BigDecimal("17.39"))
                        .withdrawRevenue(new BigDecimal("2.3"))
                        .ffCost(new BigDecimal("386"))
                        .callCenterCost(new BigDecimal("47"))
                        .spasiboCost(new BigDecimal("0"))
                        .build()
        ));
    }

    @Test
    public void calculate1p() {
        ParcelCalculator parcelCalculator = new ParcelCalculator3p(tariffDao);
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
        assertThat(cost).isEqualTo(BigDecimal.ZERO);
        BigDecimal revenue = parcelCalculator.calculateRevenue(parcel);
        assertThat(revenue).isEqualTo(BigDecimal.ZERO);
    }


    @Test
    public void calculate3p() {
        ParcelCalculator parcelCalculator = new ParcelCalculator3p(tariffDao);
        UeParcelDto parcel = UeParcelDto.builder()
                .parcelItems(
                        List.of(
                                UeParcelItemDto.builder()
                                        .priceBeforeDiscount(new BigDecimal("110"))
                                        .supplierId(UnitEconomicService.SUPPLIER_1P + 1)
                                        .msku(1L)
                                        .count(2)
                                        .price(new BigDecimal("100"))
                                        .build(),
                                UeParcelItemDto.builder()
                                        .priceBeforeDiscount(new BigDecimal("220"))
                                        .supplierId(UnitEconomicService.SUPPLIER_1P + 2)
                                        .msku(2L)
                                        .count(3)
                                        .price(new BigDecimal("200"))
                                        .build()
                        )
                )
                .build();
        BigDecimal cost = parcelCalculator.calculateTotalCost(parcel);
        assertThat(cost).isEqualByComparingTo(new BigDecimal("92"));
        BigDecimal revenue = parcelCalculator.calculateRevenue(parcel);
        assertThat(revenue).isEqualByComparingTo(new BigDecimal("609.784"));
    }


}
