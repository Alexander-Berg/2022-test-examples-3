package ru.yandex.market.communication.proxy.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.telephony.client.TelephonyClientException;
import ru.yandex.direct.telephony.client.model.TelephonyPhoneRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.dao.RealNumberDao;
import ru.yandex.market.communication.proxy.dao.RedirectInfoDao;
import ru.yandex.market.communication.proxy.model.ProxyNumberCreationResult;
import ru.yandex.market.communication.proxy.model.RedirectInfoType;
import ru.yandex.market.communication.proxy.service.environment.EnvironmentService;
import ru.yandex.market.communication.proxy.telephony.IsRedirectEnabledCalculator;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.CallRecordingScope;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 * @since 16.12.2021
 */
@DbUnitDataSet(
        before = "environment.csv"
)
public class ProxyNumberCreationServiceTest extends AbstractCommunicationProxyTest {

    private static final String CUSTOMER_PHONE_ID = "11111";
    private static final String CUSTOMER_PHONE = "+79261243987";
    private static final Long PARTNER_ID = 2L;
    private static final Long OBJECT_ID = 1L;
    private static final RedirectInfoType REDIRECT_INFO_TYPE = RedirectInfoType.DBS_ORDER;

    @Autowired
    ProxyNumberCreationService proxyNumberCreationService;
    @Autowired
    TelephonyClient telephonyClient;
    @Autowired
    TelephonyService telephonyService;
    @Autowired
    IsRedirectEnabledCalculator isRedirectEnabledCalculator;
    @Autowired
    RedirectInfoDao redirectInfoDao;
    @Autowired
    RealNumberDao realNumberDao;
    @Autowired
    EnvironmentService environmentService;

    @Test
    public void createRedirect() {
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum("+79100050648")
                        .setServiceNumberID("serviceNumberId")
                        .build());

        proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
    }

    @Test
    @DbUnitDataSet(
            before = "playbacks.csv"
    )
    public void createRedirectWithPlayback() {
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum("+79999999999")
                        .setServiceNumberID("serviceNumberId")
                        .build());

        ProxyNumberCreationResult proxyNumberCreationResult =
                proxyNumberCreationService.getProxyNumber(
                        "+71111111111", CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
                );

        verify(telephonyClient).getServiceNumber(isNull());

        TelephonyPhoneRequest request = new TelephonyPhoneRequest()
                .withRedirectPhone("+71111111111")
                .withTelephonyServiceId("serviceNumberId")
                .withCallRecordingScope(CallRecordingScope.ON_DIAL_STARTED)
                .withCounterId(0L);

        request.withBeforeConnectedPlaybackId("123");
        request.withPromptPlaybackId("987");
        request.setBeforeConversationPlaybackID("727");

        verify(telephonyClient).linkServiceNumber(
                eq(PARTNER_ID),
                eq(request),
                eq(0)
        );

        assertThat(proxyNumberCreationResult.getProxyNumber()).isEqualTo("+79999999999");
    }

    @Test
    public void alwaysReturnRealNumberId() {
        doThrow(new TelephonyClientException(new Exception()))
                .when(telephonyClient).linkServiceNumber(eq(PARTNER_ID), any(), anyInt());

        var result1 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(
                redirectInfoDao.findRedirectByObjectAndType(OBJECT_ID, REDIRECT_INFO_TYPE).isEmpty()
        ).isTrue();
        assertThat(realNumberDao.realNumberWasGiven(OBJECT_ID, REDIRECT_INFO_TYPE)).isTrue();
        assertThat(result1.getProxyNumber()).isNull();
        assertThat(result1.getRealNumberId()).isEqualTo(CUSTOMER_PHONE_ID);

        var result2 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(result1).isEqualTo(result2);
        assertThat(
                redirectInfoDao.findRedirectByObjectAndType(OBJECT_ID, REDIRECT_INFO_TYPE).isEmpty()
        ).isTrue();
        assertThat(realNumberDao.realNumberWasGiven(OBJECT_ID, REDIRECT_INFO_TYPE)).isTrue();
    }

    @Test
    public void alwaysReturnProxyNumber() {
        String redirectPhone = "+79100050648";
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum(redirectPhone)
                        .setServiceNumberID("serviceNumberId")
                        .build());

        var result1 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(
                redirectInfoDao.findRedirectByObjectAndType(OBJECT_ID, REDIRECT_INFO_TYPE).isPresent()
        ).isTrue();
        assertThat(realNumberDao.realNumberWasGiven(OBJECT_ID, REDIRECT_INFO_TYPE)).isFalse();

        var result2 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(result1).isEqualTo(result2);
        assertThat(
                redirectInfoDao.findRedirectByObjectAndType(OBJECT_ID, REDIRECT_INFO_TYPE).isPresent()
        ).isTrue();
        assertThat(realNumberDao.realNumberWasGiven(OBJECT_ID, REDIRECT_INFO_TYPE)).isFalse();

    }

    @Test
    @DbUnitDataSet(before = "alwaysReturnRealNumberForExclusions.before.csv")
    public void alwaysReturnRealNumberIdForExclusions() {
        var result1 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(result1.getProxyNumber()).isNull();
        assertThat(result1.getRealNumberId()).isEqualTo(CUSTOMER_PHONE_ID);
        var result2 = proxyNumberCreationService.getProxyNumber(
                CUSTOMER_PHONE, CUSTOMER_PHONE_ID, OBJECT_ID, REDIRECT_INFO_TYPE, PARTNER_ID
        );
        assertThat(result1).isEqualTo(result2);
    }
}
