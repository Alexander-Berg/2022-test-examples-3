package ru.yandex.market.api.cpa.yam.service.impl;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestBalanceHelperService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.contract.PartnerContractService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.balance.BalanceService.ROBOT_MBI_BALANCE_UID;

class PrepayRequestBalanceHelperServiceTest {
    private static final long UID = 999L;
    private static final long ACTION_ID = 777L;
    private static final long PREPAYMENT_PERSON_ID = 555L;
    private static final long SUBSIDIES_PERSON_ID = 666L;
    private static final long SELLER_CLIENT_ID = 123L;
    private static final long DATASOURCE_ID = 774L;

    private final BalanceService balanceService = mock(BalanceService.class);
    private final PartnerService partnerService = mock(PartnerService.class);
    private final PartnerContractService partnerContractService = mock(PartnerContractService.class);
    private final ParamService paramService = mock(ParamService.class);
    private final PrepayRequestBalanceHelperService prepayRequestBalanceHelperService =
            new PrepayRequestBalanceHelperServiceImpl(
                    balanceService,
                    balanceService,
                    partnerService,
                    partnerContractService,
                    paramService
            );

    private static PrepayRequest createTestPrepayRequest() {
        PrepayRequest request = new PrepayRequest(123L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED,
                DATASOURCE_ID);
        request.setSellerClientId(SELLER_CLIENT_ID);
        request.setPersonId(PREPAYMENT_PERSON_ID);
        return request;
    }

    @BeforeEach
    void setUp() {
        when(partnerService.getCountryId(anyLong())).thenReturn(225L);
    }

    /**
     * Тест проверят, что при обновлении заявки на предоплату из ABO, в случае, если в балансе существует
     * плательщик для субсидий (т. е. субсидии были когда либо включены), то у него обновляется юридическая информация
     * по данным из заявки.
     */
    @Test
    void testExistingSubsidiesPersonUpdateOnPrepaymentRequestUpdate() {
        // given
        var existingSubsidiesPerson = new PersonStructure();
        var request = createTestPrepayRequest();
        existingSubsidiesPerson.setPersonId(SUBSIDIES_PERSON_ID);
        when(balanceService.getClientPersons(SELLER_CLIENT_ID, PersonStructure.TYPE_PARTNER))
                .thenReturn(Collections.singletonList(existingSubsidiesPerson));
        when(balanceService.createOrUpdatePerson(any(), eq(ROBOT_MBI_BALANCE_UID)))
                .then(a -> {
                    PersonStructure prepaymentPerson = a.getArgument(0);
                    assertThat(prepaymentPerson.getPersonId(), equalTo(request.getPersonId()));
                    return null;
                })
                .then(a -> {
                    PersonStructure subsidiesPerson = a.getArgument(0);
                    assertThat(subsidiesPerson.getPersonId(), equalTo(SUBSIDIES_PERSON_ID));
                    assertThat(subsidiesPerson.getIsPartner(), equalTo(true));
                    return null;
                });
        when(partnerService.getPartner(eq(DATASOURCE_ID)))
                .thenReturn(Optional.of(new PartnerId(CampaignType.SHOP, DATASOURCE_ID)));
        // when
        prepayRequestBalanceHelperService.updateOfferInfo(Collections.singletonList(request), UID, ACTION_ID);

        // then
        verify(balanceService, times(2)).createOrUpdatePerson(any(), eq(ROBOT_MBI_BALANCE_UID));
        verify(partnerContractService, never()).updateContracts(anyLong(), anyLong(), any());
    }

    /**
     * Тест проверят, что при обновлении заявки на предоплату из ABO, в случае, если плательщика для субсидий не
     * заведено в балансе, то никаких дополнительных операций для субсидий не производится.
     */
    @Test
    void testNoAbsentSubsidiesPersonUpdateOnPrepaymentRequestUpdate() {
        // given
        var request = createTestPrepayRequest();
        when(balanceService.createOrUpdatePerson(any(), eq(ROBOT_MBI_BALANCE_UID)))
                .then(a -> {
                    PersonStructure prepaymentPerson = a.getArgument(0);
                    assertThat(prepaymentPerson.getPersonId(), equalTo(request.getPersonId()));
                    return null;
                });
        when(partnerService.getPartner(eq(DATASOURCE_ID)))
                .thenReturn(Optional.of(new PartnerId(CampaignType.SHOP, DATASOURCE_ID)));
        // when
        prepayRequestBalanceHelperService.updateOfferInfo(Collections.singletonList(request), UID, ACTION_ID);

        // then
        verify(balanceService, times(1)).createOrUpdatePerson(any(), eq(ROBOT_MBI_BALANCE_UID));
        verify(partnerContractService, never()).updateContracts(anyLong(), anyLong(), any());
    }

    @Test
    void updateOfferInfoCallsPersonContractServiceForPartnerRequests() {
        // given
        var request = createTestPrepayRequest();
        request.setRequestType(RequestType.MARKETPLACE);
        when(partnerService.getPartner(eq(DATASOURCE_ID)))
                .thenReturn(Optional.of(new PartnerId(CampaignType.SUPPLIER, DATASOURCE_ID)));
        // when
        prepayRequestBalanceHelperService.updateOfferInfo(Collections.singletonList(request), UID, ACTION_ID);

        // then
        verify(partnerContractService).updateContracts(UID, ACTION_ID, request);
        verify(balanceService, never()).createOrUpdatePerson(any(), anyLong());
        verify(balanceService, never()).createClient(any(), anyLong(), anyLong());
    }
}
