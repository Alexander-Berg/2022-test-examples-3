package ru.yandex.market.api.partner.sec.impl;


import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.ContactAndAgencyUserService;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.client.remove.RemoveClientMigrationService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.security.AgencySecurityService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "CampaignSecurityChecker.before.csv")
class CampaignSecurityCheckerFunctionalTest extends FunctionalTest {

    private static final long BLUE_AGENCY_UID = 67282296L;
    private static final long NOT_FOUND_UID = 404L;
    private static final long SHOP_UID = 801L;
    private static final long SUPPLIER_UID = 809L;
    private static final long SHOP_SUBCLIENT_UID = 802L;
    private static final long SHOP_AGENCY_UID = 804L;
    private static final long SHOP_CAMPAIGN_ID = 10974L;
    private static final long SHOP_SUBCAMPAIGN_ID = 10975L;
    private static final long SUPPLIER_AGENCY_ENABLED_ID = 10877L;
    private static final long SUPPLIER_NOT_FOUND_ID = 10678L;
    private static final long SUPPLIER_ID = 10879L;

    //Магазин, у которого по различается клиент в балансе и клиент по запрашиваемому пользователю в базе
    private static final long PRICELABS_CHECK_USER = 812L;
    private static final long PRICELABS_CHECK_CAMPAIGN = 10976L;
    private static final long PRICELABS_CHECK_CLIENT = 9004L;

    private static final long DIRECT_CAMPAIGN_ID = 10135;

    @Autowired
    private BalanceContactService balanceContactService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private ContactService contactService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private ContactAndAgencyUserService contactAndAgencyUserService;
    @Autowired
    private AgencySecurityService agencySecurityService;
    @Autowired
    private PriceLabsFlags priceLabsFlags;
    @Autowired
    private RemoveClientMigrationService removeClientMigrationService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    private CampaignSecurityChecker campaignSecurityChecker;

    static Stream<Arguments> argsClient() {
        return Stream.of(
                //shop agency has access to subcampaign
                Arguments.of(SHOP_SUBCAMPAIGN_ID, SHOP_AGENCY_UID, true),
                //shop subclient has access to campaign
                Arguments.of(SHOP_SUBCAMPAIGN_ID, SHOP_SUBCLIENT_UID, true),
                //shop has access to campaign
                Arguments.of(SHOP_CAMPAIGN_ID, SHOP_UID, true),
                //shop not found
                Arguments.of(SHOP_CAMPAIGN_ID, NOT_FOUND_UID, false),
                //supplier with AGENCY_SUPPLIER_ACCESS has access to campaign
                Arguments.of(SUPPLIER_AGENCY_ENABLED_ID, BLUE_AGENCY_UID, true),
                //supplier not found
                Arguments.of(SUPPLIER_NOT_FOUND_ID, BLUE_AGENCY_UID, false),
                //Allow user campaign
                Arguments.of(SUPPLIER_ID, SUPPLIER_UID, true),
                //Deny agency user campaign does not belongs to agency
                Arguments.of(SUPPLIER_ID, BLUE_AGENCY_UID, false),
                //True if no campaign
                Arguments.of(null, BLUE_AGENCY_UID, true),
                //agency access, no contact in db
                Arguments.of(4000L, 8333L, true),
                //agency access, contact in db
                Arguments.of(4000L, 9333L, true)
        );
    }

    static Stream<Arguments> argsLinks() {
        return Stream.of(
                //contact not found
                Arguments.of(4000L, 404L, false),
                //no access
                Arguments.of(4000L, 5333L, false),
                //business link
                Arguments.of(5000L, 6333L, true),
                //straight link
                Arguments.of(4000L, 7333L, true)
        );
    }

    static Stream<Arguments> argsEnv() {
        return Stream.of(
                //доступ по клиенту, записываем
                Arguments.of(6000L, 123L, true),
                //cpc, не записываем
                Arguments.of(7000L, 123L, false),
                //без программы, не записываем
                Arguments.of(8000L, 123L, false),
                //дбс, записываем
                Arguments.of(10000L, 123L, true)
        );
    }

    static Stream<Arguments> priceLabsTypes() {
        return Stream.of(
                Arguments.of(ApiLimitType.PRICE_LABS),
                Arguments.of(ApiLimitType.PRICE_LABS_V2)
        );
    }

    static Stream<Arguments> nonPriceLabsTypes() {
        return Stream.of(
                Arguments.of(ApiLimitType.DEFAULT),
                Arguments.of(ApiLimitType.PARALLEL)
        );
    }

    @BeforeEach
    void before() {
        environmentService = Mockito.spy(environmentService);
        campaignSecurityChecker = new CampaignSecurityChecker(
                campaignService, contactService, businessService, contactAndAgencyUserService,
                removeClientMigrationService, agencySecurityService, priceLabsFlags);

        priceLabsFlags.resetFlags();
    }

    @ParameterizedTest
    @DisplayName("Тест доступа клиентов, субклиентов и агентств к партнерскому апи")
    @MethodSource("argsClient")
    void testIsAllowedClient(Long campaignId, long userId, boolean expected) {

        PartnerServletRequest request = new PartnerServletRequest(mock(HttpServletRequest.class), Integer.MAX_VALUE);
        request.initCampaignId(campaignId);

        final AuthPrincipal user = new AuthPrincipal(userId) {
        };
        request.initUserPrincipal(user);
        Assertions.assertEquals(expected, campaignSecurityChecker.isAllowed(request));
    }

    @ParameterizedTest
    @DisplayName("Тест доступа по линкам к партнерскому апи")
    @MethodSource("argsLinks")
    void testIsAllowedLinks(long campaignId, long userId, boolean expected) {

        CampaignInfo campaignInfo = campaignService.getMarketCampaign(campaignId);
        Assertions.assertEquals(expected, campaignSecurityChecker.authorizeByLinks(campaignInfo, userId));
    }

    @ParameterizedTest
    @DisplayName("Тест доступа для PriceLabs к магазину с другим балансовым клиентом - флаг включен, доступ есть")
    @MethodSource("priceLabsTypes")
    void testPriceLabsRequest(ApiLimitType priceLabsType) {
        setPricelabsCheckClientEnabled(true);
        when(balanceContactService.getClientIdByUid(PRICELABS_CHECK_USER)).thenReturn(PRICELABS_CHECK_CLIENT);

        PartnerServletRequest request = new PartnerServletRequest(mock(HttpServletRequest.class), Integer.MAX_VALUE);
        request.setApiLimitType(priceLabsType);
        request.initCampaignId(PRICELABS_CHECK_CAMPAIGN);

        final AuthPrincipal user = new AuthPrincipal(PRICELABS_CHECK_USER) {
        };
        request.initUserPrincipal(user);
        Assertions.assertTrue(campaignSecurityChecker.isAllowed(request));
    }

    @ParameterizedTest
    @DisplayName("Тест доступа к магазину с другим балансовым клиентом - флаг PriceLabs включен, " +
            "но запрос не от PriceLabs, доступа нет")
    @MethodSource("nonPriceLabsTypes")
    void testNotPriceLabs(ApiLimitType nonPriceLabsType) {
        setPricelabsCheckClientEnabled(true);
        when(balanceContactService.getClientIdByUid(PRICELABS_CHECK_USER)).thenReturn(PRICELABS_CHECK_CLIENT);

        PartnerServletRequest request = new PartnerServletRequest(mock(HttpServletRequest.class), Integer.MAX_VALUE);
        request.setApiLimitType(nonPriceLabsType);
        request.initCampaignId(PRICELABS_CHECK_CAMPAIGN);

        final AuthPrincipal user = new AuthPrincipal(PRICELABS_CHECK_USER) {
        };
        request.initUserPrincipal(user);
        Assertions.assertFalse(campaignSecurityChecker.isAllowed(request));
    }

    @ParameterizedTest
    @DisplayName("Тест доступа для PriceLabs к магазину с другим балансовым клиентом - флаг выключен, доступа нет")
    @MethodSource("priceLabsTypes")
    void testPriceLabsRequestWithFlagDisabled(ApiLimitType priceLabsType) {
        setPricelabsCheckClientEnabled(false);
        when(balanceContactService.getClientIdByUid(PRICELABS_CHECK_USER)).thenReturn(PRICELABS_CHECK_CLIENT);

        PartnerServletRequest request = new PartnerServletRequest(mock(HttpServletRequest.class), Integer.MAX_VALUE);
        request.setApiLimitType(priceLabsType);
        request.initCampaignId(PRICELABS_CHECK_CAMPAIGN);

        final AuthPrincipal user = new AuthPrincipal(PRICELABS_CHECK_USER) {
        };
        request.initUserPrincipal(user);
        Assertions.assertFalse(campaignSecurityChecker.isAllowed(request));
    }

    @Test
    @DisplayName("Тест доступа в API партнеров директа")
    void testDirectPartnerHasAccessToPapi() {
        PartnerServletRequest request = new PartnerServletRequest(mock(HttpServletRequest.class), Integer.MAX_VALUE);
        request.setApiLimitType(ApiLimitType.DEFAULT);
        request.initCampaignId(DIRECT_CAMPAIGN_ID);

        final AuthPrincipal user = new AuthPrincipal(PRICELABS_CHECK_USER) {
        };
        request.initUserPrincipal(user);

        Assertions.assertTrue(campaignSecurityChecker.isAllowed(request));
    }

    private void setPricelabsCheckClientEnabled(boolean value) {
        environmentService.setValue(PriceLabsFlags.CLIENT_IGNORED_VAR, Boolean.toString(value));
    }
}
