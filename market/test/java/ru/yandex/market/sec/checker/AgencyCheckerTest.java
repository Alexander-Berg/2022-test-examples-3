package ru.yandex.market.sec.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.security.BusinessCampaignable;
import ru.yandex.market.core.security.Campaignable;
import ru.yandex.market.core.security.checker.AgencyChecker;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "AgencyCheckerTest.csv")
class AgencyCheckerTest extends FunctionalTest {

    @Autowired
    private AgencyChecker agencyChecker;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private BalanceService balanceService;

    private PartnerDefaultRequestHandler.PartnerHttpServRequest mockRequestWithBusinessAgency(
            long clientId, long campaignId, Long businessId
    ) {
        when(balanceContactService.getClientIdByUid(clientId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.PHYSICAL, true,
                clientId));
        PartnerDefaultRequestHandler.PartnerHttpServRequest request =
                mock(PartnerDefaultRequestHandler.PartnerHttpServRequest.class);
        when(request.getCampaignId()).thenReturn(campaignId);
        when(request.getUid()).thenReturn(clientId);
        when(request.getBusinessId()).thenReturn(businessId);
        return request;
    }

    private PartnerDefaultRequestHandler.PartnerHttpServRequest mockRequestWithoutBusinessAgency(
            long clientId, long campaignId
    ) {
        when(balanceContactService.getClientIdByUid(clientId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.PHYSICAL, true,
                clientId));
        PartnerDefaultRequestHandler.PartnerHttpServRequest request =
                mock(PartnerDefaultRequestHandler.PartnerHttpServRequest.class);
        when(request.getCampaignId()).thenReturn(campaignId);
        when(request.getUid()).thenReturn(clientId);
        when(request.getBusinessId()).thenReturn(null);
        return request;
    }

    private BusinessCampaignable mockBusinessCampaignableAgency(long clientId, long campaignId, Long businessId) {
        when(balanceContactService.getClientIdByUid(clientId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.PHYSICAL, true,
                clientId));
        BusinessCampaignable businessCampaignable = mock(BusinessCampaignable.class);
        when(businessCampaignable.getCampaignId()).thenReturn(campaignId);
        when(businessCampaignable.getUid()).thenReturn(clientId);
        when(businessCampaignable.getBusinessId()).thenReturn(businessId);
        return businessCampaignable;
    }

    private Campaignable mockCampaignableAgency(long clientId, long campaignId) {
        when(balanceContactService.getClientIdByUid(clientId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.PHYSICAL, true,
                clientId));
        Campaignable campaignable = mock(Campaignable.class);
        when(campaignable.getCampaignId()).thenReturn(campaignId);
        when(campaignable.getUid()).thenReturn(clientId);
        return campaignable;
    }

    private Campaignable mockCampaignableNotAgency(long campaignId, Long businessId) {
        when(balanceContactService.getClientIdByUid(21L)).thenReturn(3L);
        when(balanceService.getClient(3L)).thenReturn(new ClientInfo(3L, ClientType.PHYSICAL, false, 2L));
        Campaignable campaignable = mock(BusinessCampaignable.class);
        when(campaignable.getCampaignId()).thenReturn(campaignId);
        when(campaignable.getUid()).thenReturn(21L);
        return campaignable;
    }

    private BusinessCampaignable mockCampaignableWithoutAgency(long clientId, long campaignId, Long businessId) {
        when(balanceContactService.getClientIdByUid(21L)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.PHYSICAL, false, -1));
        BusinessCampaignable businessCampaignable = mock(BusinessCampaignable.class);
        when(businessCampaignable.getCampaignId()).thenReturn(campaignId);
        when(businessCampaignable.getBusinessId()).thenReturn(businessId);
        when(businessCampaignable.getUid()).thenReturn(21L);
        return businessCampaignable;
    }

    @Test
    void agencyWithoutAccessToCampaignTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(1L, 1234L, null);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void agencyWithAccessToCampaignTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(1L, 1235L, null);
        Assertions.assertTrue(agencyChecker.checkTyped(businessCampaignable, new Authority()));
        PartnerDefaultRequestHandler.PartnerHttpServRequest request = mockRequestWithBusinessAgency(1L, 1235L, null);
        Assertions.assertTrue(agencyChecker.checkTyped(request, new Authority()));
        PartnerDefaultRequestHandler.PartnerHttpServRequest requestWithoutBusiness =
                mockRequestWithoutBusinessAgency(1L, 1235L);
        Assertions.assertTrue(agencyChecker.checkTyped(requestWithoutBusiness, new Authority()));
        Campaignable campaignable = mockCampaignableAgency(1L, 1235L);
        Assertions.assertTrue(agencyChecker.checkTyped(campaignable, new Authority()));
    }

    @Test
    void shopAgencyTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(2L, 1236L, null);
        Assertions.assertTrue(agencyChecker.checkTyped(businessCampaignable, new Authority()));
        PartnerDefaultRequestHandler.PartnerHttpServRequest request = mockRequestWithBusinessAgency(2L, 1236L, null);
        Assertions.assertTrue(agencyChecker.checkTyped(request, new Authority()));
        PartnerDefaultRequestHandler.PartnerHttpServRequest requestWithoutBusiness =
                mockRequestWithoutBusinessAgency(2L, 1236L);
        Assertions.assertTrue(agencyChecker.checkTyped(requestWithoutBusiness, new Authority()));
        Campaignable campaignable = mockCampaignableAgency(2L, 1236L);
        Assertions.assertTrue(agencyChecker.checkTyped(campaignable, new Authority()));
    }

    @Test
    void agencyNotValidCampaignTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(1L, 1L, null);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void supplierTest() {
        BusinessCampaignable businessCampaignable = mockCampaignableWithoutAgency(103L, 1237, null);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void shopTest() {
        BusinessCampaignable businessCampaignable = mockCampaignableWithoutAgency(101L, 1236, null);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void agencyHasBusinessAccessTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(1L, 0, 1000L);
        Assertions.assertTrue(agencyChecker.checkTyped(businessCampaignable, new Authority()));
        PartnerDefaultRequestHandler.PartnerHttpServRequest request = mockRequestWithBusinessAgency(1L, 0, 1000L);
        Assertions.assertTrue(agencyChecker.checkTyped(request, new Authority()));
    }

    @Test
    void agencyHasNoAcesssToNonAgencyBusinessTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(1L, 0, 999L);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void agencyHasNoBusinessAcesssTest() {
        BusinessCampaignable businessCampaignable = mockBusinessCampaignableAgency(2L, 0, 1000L);
        Assertions.assertFalse(agencyChecker.checkTyped(businessCampaignable, new Authority()));
    }

    @Test
    void notAgencyValidCampaignTest() {
        Campaignable campaignable = mockCampaignableNotAgency(1236L, null);
        Assertions.assertFalse(agencyChecker.checkTyped(campaignable, new Authority()));
    }

    @Test
    void notAgencyNotValidCampaignTest() {
        Campaignable campaignable = mockCampaignableNotAgency(0L, null);
        Assertions.assertFalse(agencyChecker.checkTyped(campaignable, new Authority()));
    }
}
