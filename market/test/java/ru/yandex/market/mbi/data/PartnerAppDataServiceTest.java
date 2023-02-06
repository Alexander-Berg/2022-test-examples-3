package ru.yandex.market.mbi.data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.state.DataChangesEvent;
import ru.yandex.market.mbi.data.outer.DataOuterServiceUtil;
import ru.yandex.market.mbi.data.outer.PartnerAppDataOuterService;

import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Тесты для {@link PartnerAppDataService}.
 */
public class PartnerAppDataServiceTest extends FunctionalTest {
    @Autowired
    private PartnerAppDataService partnerAppDataService;

    @Test
    @DbUnitDataSet(before = "PartnerAppDataServiceTest.csv")
    public void testProvideDataForYt() {
        Consumer<Pair<Long, List<PrepayRequest>>> mock = Mockito.mock(Consumer.class);
        partnerAppDataService.provideDataForYt(mock);
        ArgumentCaptor<Pair<Long, List<PrepayRequest>>> requestCaptor = ArgumentCaptor.forClass(Pair.class);
        Mockito.verify(mock, times(2)).accept(requestCaptor.capture());
        List<Pair<Long, List<PrepayRequest>>> values = requestCaptor.getAllValues();
        Assertions.assertEquals(2, values.size());
        Map<Long, Set<Long>> valuesMap = values.stream().collect(Collectors.toMap(Pair::getKey,
                v -> v.getValue().stream().map(PrepayRequest::getDatasourceId).collect(Collectors.toSet())));
        //проверяем что партнеры в заявке группируют
        Assertions.assertEquals(valuesMap.get(555L), Set.of(1000L, 1001L));
        Assertions.assertEquals(valuesMap.get(666L), Set.of(1002L));
    }

    @Test
    public void testGetPartnerAppDataForExport() {
        Instant eventTime = Instant.now();
        PartnerAppDataOuterClass.PartnerAppData expected = PartnerAppDataOuterClass.PartnerAppData.newBuilder()
                .setGeneralInfo(DataOuterServiceUtil.getGeneralDataInfo(eventTime,
                        DataChangesEvent.PartnerDataOperation.READ))
                .addAllPartnerIds(List.of(5L, 6L))
                .setRequestId(500L)
                .setInn("53535")
                .setOgrn("2222")
                .setOrgName("testName")
                .setType(PartnerAppDataOuterClass.PartnerAppType.MARKETPLACE)
                .setStatus(PartnerAppDataOuterClass.PartnerAppStatus.COMPLETED)
                .build();

        PrepayRequest prepayRequest1 = createPrepayRequestFilled(5);
        PrepayRequest prepayRequest2 = createPrepayRequestFilled(6L);

        Assertions.assertEquals(expected,
                PartnerAppDataOuterService.buildPartnerAppDataForExport(500L, List.of(prepayRequest1, prepayRequest2),
                        eventTime, DataChangesEvent.PartnerDataOperation.READ));

    }

    private static PrepayRequest createPrepayRequestNotFilled(long partnerId,
                                                              PartnerApplicationStatus partnerApplicationStatus) {
        PrepayRequest prepayRequest = new PrepayRequest(555L,
                PrepayType.YANDEX_MONEY, partnerApplicationStatus, partnerId);
        prepayRequest.setFactAddressRegionId(0L);
        return prepayRequest;
    }

    private static PrepayRequest createPrepayRequestFilled(long partnerId) {
        PrepayRequest prepayRequest = new PrepayRequest(500L, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, partnerId);
        prepayRequest.setInn("53535");
        prepayRequest.setOgrn("2222");
        prepayRequest.setOrganizationName("testName");
        prepayRequest.setRequestType(RequestType.MARKETPLACE);
        return prepayRequest;
    }
}
