package ru.yandex.market.wms.autostart.autostartlogic.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuLocDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;
    @Mock
    private SkuLocDao skuLocDao;
    @Mock
    private LotLocIdDao lotLocIdDao;
    @Mock
    private LotDao lotDao;

    @Test
    void reserveItemTestWhenDisableFixBalance() {
        PickSku pickSku = PickSku.builder()
                .skuId(SkuId.of("100", "001"))
                .location(
                        PickSkuLocation.builder()
                                .loc("1001")
                                .lot("0010")
                                .build()
                )
                .build();
        boolean isFixBalancesWhenReserveItemsInAOS = false;

        when(skuLocDao.updateSkuXLocAllocatedQty(anyInt(), any(), any(), any(), any()))
                .thenReturn(1);
        when(lotLocIdDao.updateLOTxLOCxIDAllocatedQty(anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(lotDao.updateLocAllocatedQty(anyInt(), any(), any(), any(), any()))
                .thenReturn(1);

        reservationService.writeReservation(pickSku, isFixBalancesWhenReserveItemsInAOS);

        verify(skuLocDao, times(1))
                .updateSkuXLocAllocatedQty(anyInt(), any(), any(), any(), any());
        verify(lotLocIdDao, times(1))
                .updateLOTxLOCxIDAllocatedQty(anyInt(), any(), any(), any(), any(), any(), any());
        verify(lotDao, times(1))
                .updateLocAllocatedQty(anyInt(), any(), any(), any(), any());
        verify(lotDao, never()).updateLocAllocatedQtyWithSelfHeal(anyInt(), any(), any(), any(), any());
    }

    @Test
    void reserveItemTestWhenEnableFixBalance() {
        PickSku pickSku = PickSku.builder()
                .skuId(SkuId.of("100", "001"))
                .location(
                        PickSkuLocation.builder()
                                .loc("1001")
                                .lot("0010")
                                .build()
                )
                .build();
        boolean isFixBalancesWhenReserveItemsInAOS = true;

        when(skuLocDao.updateSkuXLocAllocatedQty(anyInt(), any(), any(), any(), any()))
                .thenReturn(1);
        when(lotLocIdDao.updateLOTxLOCxIDAllocatedQty(anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(lotDao.updateLocAllocatedQtyWithSelfHeal(anyInt(), any(), any(), any(), any()))
                .thenReturn(1);

        reservationService.writeReservation(pickSku, isFixBalancesWhenReserveItemsInAOS);

        verify(skuLocDao, times(1))
                .updateSkuXLocAllocatedQty(anyInt(), any(), any(), any(), any());
        verify(lotLocIdDao, times(1))
                .updateLOTxLOCxIDAllocatedQty(anyInt(), any(), any(), any(), any(), any(), any());
        verify(lotDao, never()).updateLocAllocatedQty(anyInt(), any(), any(), any(), any());
        verify(lotDao, times(1))
                .updateLocAllocatedQtyWithSelfHeal(anyInt(), any(), any(), any(), any());
    }
}
