package ru.yandex.direct.core.entity.promocodes.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.request.TearOffPromocodeRequest;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.model.TearOffReason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPromocodeInfo.createPromocodeInfo;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class PromocodesAntiFraudServiceTearOffPromocodesTest {
    private static final int SERVICE_ID = 7;
    private static final long CAMPAIGN_ID = 123L;
    private static final List<PromocodeInfo> PROMOCODE_INFOS =
            Collections.singletonList(createPromocodeInfo());
    private static final List<PromocodeInfo> PROMOCODE_INFOS2 =
            Arrays.asList(createPromocodeInfo(), createPromocodeInfo());

    @Mock
    private PromocodesTearOffMailSenderService mailSenderService;

    @Mock
    private BalanceClient balanceClient;

    private PromocodesAntiFraudService antiFraudService;

    @Captor
    private ArgumentCaptor<TearOffPromocodeRequest> requestCaptor;

    @Before
    public void before() {
        initMocks(this);

        antiFraudService = new PromocodesAntiFraudServiceBuilder()
                .withBalanceClient(balanceClient)
                .withMailSenderService(mailSenderService)
                .build();
    }

    @Test
    public void tearOffOnePromocodeTest() {
        TearOffPromocodeRequest request = new TearOffPromocodeRequest()
                .withOperatorUid(0L)
                .withServiceId(SERVICE_ID)
                .withServiceOrderId(CAMPAIGN_ID)
                .withPromocodeId(PROMOCODE_INFOS.get(0).getId());

        antiFraudService.tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, PROMOCODE_INFOS,
                TearOffReason.UNIT_TESTING);

        verify(balanceClient).tearOffPromocode(requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .is(matchedBy(beanDiffer(request)));

        verify(mailSenderService).sendPromocodesTearOffMail(eq(CAMPAIGN_ID));

        verifyNoMoreInteractions(mailSenderService);
    }

    @Test
    public void tearOffMultiplePromocodesTest() {
        TearOffPromocodeRequest request1 = new TearOffPromocodeRequest()
                .withOperatorUid(0L)
                .withServiceId(SERVICE_ID)
                .withServiceOrderId(CAMPAIGN_ID)
                .withPromocodeId(PROMOCODE_INFOS2.get(0).getId());
        TearOffPromocodeRequest request2 = new TearOffPromocodeRequest()
                .withOperatorUid(0L)
                .withServiceId(SERVICE_ID)
                .withServiceOrderId(CAMPAIGN_ID)
                .withPromocodeId(PROMOCODE_INFOS2.get(1).getId());

        antiFraudService.tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, PROMOCODE_INFOS2,
                TearOffReason.UNIT_TESTING);

        verify(balanceClient, times(PROMOCODE_INFOS2.size())).tearOffPromocode(requestCaptor.capture());

        List<TearOffPromocodeRequest> requestValues = requestCaptor.getAllValues();
        assertThat(requestValues.get(0)).is(matchedBy(beanDiffer(request1)));
        assertThat(requestValues.get(1)).is(matchedBy(beanDiffer(request2)));

        verify(mailSenderService).sendPromocodesTearOffMail(eq(CAMPAIGN_ID));

        verifyNoMoreInteractions(mailSenderService);
    }
}
