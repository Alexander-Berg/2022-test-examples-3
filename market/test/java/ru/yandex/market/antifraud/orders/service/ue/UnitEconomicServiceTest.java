package ru.yandex.market.antifraud.orders.service.ue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationRequest;
import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationResult;
import ru.yandex.market.antifraud.orders.entity.ue.UeAddressDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeBuyerDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeDeliveryDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeOrderDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelItemDto;
import ru.yandex.market.antifraud.orders.storage.dao.TariffDao;
import ru.yandex.market.antifraud.orders.storage.entity.ue.UeGlobalParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class UnitEconomicServiceTest {

    private TariffDao tariffDao;
    private DeliveryCalculatorService deliveryCalculatorService;
    private ParcelCalculator parcelCalculator1p;
    private ParcelCalculator parcelCalculator3p;

    private UnitEconomicService unitEconomicService;

    @Before
    public void init() {
        tariffDao = mock(TariffDao.class);
        deliveryCalculatorService = mock(DeliveryCalculatorService.class);
        parcelCalculator1p = mock(ParcelCalculator.class);
        parcelCalculator3p = mock(ParcelCalculator.class);
        unitEconomicService = new UnitEconomicService(
                tariffDao,
                deliveryCalculatorService,
                parcelCalculator1p,
                parcelCalculator3p
        );
    }

    @Test
    public void calculateUe() {
        when(tariffDao.findTariffs(
                anyLong(),
                anyLong(),
                anyLong(),
                eq("COURIER")
        )).thenReturn(List.of());
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
        when(deliveryCalculatorService.calculateDeliveryCost(any(), anyList()))
                .thenReturn(Optional.of(new BigDecimal("50")));
        when(parcelCalculator1p.calculateRevenue(any())).thenReturn(new BigDecimal("110"));
        when(parcelCalculator1p.calculateTotalCost(any())).thenReturn(new BigDecimal("100"));
        when(parcelCalculator3p.calculateRevenue(any())).thenReturn(new BigDecimal("210"));
        when(parcelCalculator3p.calculateTotalCost(any())).thenReturn(new BigDecimal("200"));
        OrderUeCalculationRequest request = new OrderUeCalculationRequest(
                List.of(
                        UeOrderDto.builder()
                                .id(1L)
                                .creationDate(Instant.now())
                                .buyer(UeBuyerDto.builder()
                                        .uid(123L)
                                        .build())
                                .delivery(UeDeliveryDto.builder()
                                        .deliveryServiceId(11L)
                                        .warehouseId(133)
                                        .deliveryPrice(new BigDecimal("500"))
                                        .shopAddress(UeAddressDto.builder()
                                                .regionId(3L)
                                                .build())
                                        .buyerAddress(UeAddressDto.builder()
                                                .regionId(4L)
                                                .build())
                                        .parcels(List.of(
                                                UeParcelDto.builder()
                                                        .parcelItems(List.of(
                                                                UeParcelItemDto.builder()
                                                                        .priceBeforeDiscount(new BigDecimal("110"))
                                                                        .price(new BigDecimal("100"))
                                                                        .count(3)
                                                                        .build(),
                                                            UeParcelItemDto.builder()
                                                                .priceBeforeDiscount(new BigDecimal("210"))
                                                                .price(new BigDecimal("200"))
                                                                .count(2)
                                                                .build()
                                                        ))
                                                    .build()
                                        ))
                                    .build())
                            .build()
                ),
            null);
        OrderUeCalculationResult result = unitEconomicService.calculateUe(request);
        assertThat(result.getCost()).isEqualByComparingTo(new BigDecimal("783"));
        assertThat(result.getRevenue()).isEqualByComparingTo(new BigDecimal("820"));
        assertThat(result.getUnitEconomic()).isEqualByComparingTo(new BigDecimal("0.048"));
    }

}
