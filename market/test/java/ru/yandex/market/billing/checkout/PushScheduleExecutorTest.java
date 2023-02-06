package ru.yandex.market.billing.checkout;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.partner.PartnerService;

/**
 * Тесты для {@link PushScheduleExecutor}.
 */
public class PushScheduleExecutorTest {

    private static PushScheduleExecutor pushScheduleExecutor;
    private static CPADataPusher cpaDataPusher;

    @BeforeAll
    static void setUp() {
        ParamService paramService = Mockito.mock(ParamService.class);
        cpaDataPusher = Mockito.mock(CPADataPusher.class);
        PartnerService partnerService = Mockito.mock(PartnerService.class);
        pushScheduleExecutor = new PushScheduleExecutor(paramService, cpaDataPusher, partnerService);

        ParamType paramType = ParamType.CPA_IS_PARTNER_INTERFACE;

        MultiMap<Long, ParamValue> params = new MultiMap<>();
        params.append(1L, new BooleanParamValue(paramType, 1, true));
        params.append(2L, new BooleanParamValue(paramType, 2, true));
        params.append(3L, new BooleanParamValue(paramType, 3, true));
        params.append(4L, new BooleanParamValue(paramType, 4, true));
        params.append(5L, new BooleanParamValue(paramType, 5, true));
        params.append(6L, new BooleanParamValue(paramType, 6, true));

        Mockito.when(paramService.getParams(paramType)).thenReturn(params);

        PartnerId partnerId1 = PartnerId.datasourceId(1L);
        PartnerId partnerId2 = PartnerId.supplierId(2L);
        PartnerId partnerId4 = PartnerId.fmcgId(4L);
        PartnerId partnerId5 = PartnerId.deliveryId(5L);
        PartnerId partnerId6 = PartnerId.distributionId(6L);
        PartnerId partnerId7 = PartnerId.tplId(7L);
        PartnerId partnerId8 = PartnerId.sortingCenterId(8L);
        PartnerId partnerId9 = PartnerId.tplOutletId(9L);
        PartnerId partnerId10 = PartnerId.tplPartnerId(10L);

        Mockito.when(partnerService.getPartner(1L)).thenReturn(Optional.of(partnerId1));
        Mockito.when(partnerService.getPartner(2L)).thenReturn(Optional.of(partnerId2));
        Mockito.when(partnerService.getPartner(4L)).thenReturn(Optional.of(partnerId4));
        Mockito.when(partnerService.getPartner(5L)).thenReturn(Optional.of(partnerId5));
        Mockito.when(partnerService.getPartner(6L)).thenReturn(Optional.of(partnerId6));
        Mockito.when(partnerService.getPartner(7L)).thenReturn(Optional.of(partnerId7));
        Mockito.when(partnerService.getPartner(8L)).thenReturn(Optional.of(partnerId8));
        Mockito.when(partnerService.getPartner(9L)).thenReturn(Optional.of(partnerId9));
        Mockito.when(partnerService.getPartner(10L)).thenReturn(Optional.of(partnerId10));
    }

    @DisplayName("Проверяем, что расписания пушатся только для поставщиков")
    @Test
    void testExecutor() {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        pushScheduleExecutor.doJob(null);
        Mockito.verify(cpaDataPusher).pushSchedules(captor.capture());
        Collection<Long> args = captor.getValue();
        Assertions.assertEquals(1, args.size());
        Assertions.assertEquals(2L, args.iterator().next().longValue());
    }
}
