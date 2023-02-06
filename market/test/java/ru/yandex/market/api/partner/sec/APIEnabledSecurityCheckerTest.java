package ru.yandex.market.api.partner.sec;

import java.security.AccessControlException;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.market.api.partner.auth.impl.oauth.OAuthPrincipal;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.partner.sec.impl.APIEnabledSecurityChecker;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.core.campaign.model.PartnerId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class APIEnabledSecurityCheckerTest {

    private static final long DELIVERY_CAMPAIGN_ID = 777L;

    @Mock
    private HttpServletRequest requestMock;
    private APIEnabledSecurityChecker apiEnabledSecurityChecker;
    private PartnerServletRequest partnerRequest;

    private static Object[] limitTypesExceptDefault() {
        return Stream.of(ApiLimitType.values())
                .filter(type -> type != ApiLimitType.DEFAULT && type != ApiLimitType.PARALLEL)
                .toArray();
    }

    @BeforeEach
    void init() {
        apiEnabledSecurityChecker = new APIEnabledSecurityChecker();
        partnerRequest = new PartnerServletRequest(requestMock, 100);
    }

    /**
     * В запросе пришел клиент с кампанией
     */
    @Test
    void checkAllowedClientCampaign() {
        initKnownAllowedClient();
        initKnownAllowedCampaign();

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * В запросе пришел не разрешенный клиент без кампании.
     * Проверка выключена. Возвращаем true.
     */
    @Test
    void checkNotEmptyNotAllowedClientEmptyCampaign() {
        partnerRequest.initClientId(123L);
        partnerRequest.initCampaignId(null);

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * В запросе пришел разрешенный клиент без кампании
     */
    @Test
    void checkAllowedClientEmptyCampaign() {
        initKnownAllowedClient();
        partnerRequest.initCampaignId(null);

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Доставка не должна ходить в папи
     */
    @Test
    void checkDeliveryCampaign() {
        initKnownAllowedClient();
        initDeliveryCampaign();

        assertFalse(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Запрос без клиента, но с известной и разрешенной кампанией
     */
    @Test
    void checkEmptyClientNotEmptyAllowedCampaign() {
        partnerRequest.initClientId(null);
        initKnownAllowedCampaign();

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Запрос без клиента, но с неизвестной кампанией
     */
    @Test
    void checkEmptyClientNotEmptyUnknownCampaign() {
        partnerRequest.initClientId(null);
        partnerRequest.initCampaignId(10774L);

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Запрос без клиента и без кампании
     */
    @Test
    void checkEmptyClientEmptyCampaign() {
        partnerRequest.initClientId(null);
        partnerRequest.initCampaignId(null);

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Запрос из PriceLabs с неизвестной кампанией
     */
    @ParameterizedTest
    @MethodSource("limitTypesExceptDefault")
    void checkFromPLWithUnknownCampaign(ApiLimitType apiLimitType) {
        partnerRequest.setApiLimitType(apiLimitType);
        partnerRequest.initCampaignId(10774L);

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Запрос из PriceLabs с разрешенной кампанией
     */
    @ParameterizedTest
    @MethodSource("limitTypesExceptDefault")
    void checkFromPLWithAllowedCampaign(ApiLimitType apiLimitType) {
        partnerRequest.setApiLimitType(apiLimitType);
        initKnownAllowedCampaign();

        assertTrue(apiEnabledSecurityChecker.isAllowed(partnerRequest));
    }

    /**
     * Проверяем, что excel-приложение не имеет доступа к не своим ручкам papi.
     */
    @Test
    void testConstrainExcelApplication() {
        when(requestMock.getRequestURL()).thenReturn(new StringBuffer("/delivery/services"));
        initUserPrincipal();

        assertEquals("Access denied for excel application: 5d982bbd1c9f46738ec3ec3fcd9c776f",
                assertThrows(AccessControlException.class, () ->
                        apiEnabledSecurityChecker.isAllowed(partnerRequest)).getMessage());
    }

    private void initUserPrincipal() {
        partnerRequest.initUserPrincipal(new OAuthPrincipal(1L, "TOKEN", "5d982bbd1c9f46738ec3ec3fcd9c776f"));
    }

    private void initKnownAllowedClient() {
        partnerRequest.initClientId(123L);
    }

    private void initKnownAllowedCampaign() {
        partnerRequest.initCampaignId(10774L);
        partnerRequest.setPartnerId(PartnerId.supplierId(774L));

    }

    private void initDeliveryCampaign() {
        partnerRequest.initCampaignId(DELIVERY_CAMPAIGN_ID);
        partnerRequest.setPartnerId(PartnerId.deliveryId(774L));
    }

}
