package ru.yandex.market.pvz.core.domain.dbqueue.fetch_agreement_data;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.ContractDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FetchPartnerAgreementDataServiceTest {

    private static final String TEST_REQUEST_ID = "request/id";
    private static final String AGREEMENT_NUMBER = "123";
    private static final String VIRTUAL_ACCOUNT_NUMBER = "DOSTAVKA_P_123";

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final LegalPartnerRepository legalPartnerRepository;
    private final FetchPartnerAgreementDataService fetchPartnerAgreementDataService;

    @MockBean
    private LMSClient lmsClient;

    @Test
    void testFetchAgreementData() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .agreementNo(null)
                                .agreementDate(null)
                                .virtualAccountNumber(null)
                                .build())
                        .build());

        when(lmsClient.getPartner(eq(legalPartner.getDeliveryService().getId())))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .oebsVirtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                        .balanceContract(ContractDto.newBuilder()
                                .externalId(AGREEMENT_NUMBER)
                                .build())
                        .build()));

        var delayO = fetchPartnerAgreementDataService.reenqueueDelay(
                new FetchPartnerAgreementDataPayload(TEST_REQUEST_ID, legalPartner.getId()));

        legalPartner = legalPartnerRepository.findByIdOrThrow(legalPartner.getId());

        assertThat(legalPartner.getAgreementNo()).isEqualTo(AGREEMENT_NUMBER);
        assertThat(legalPartner.getAgreementDate()).isNull();
        assertThat(legalPartner.getVirtualAccountNumber()).isEqualTo(VIRTUAL_ACCOUNT_NUMBER);
        assertThat(delayO).isEmpty();
    }

    @Test
    void testFetchNotAllAgreementData() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .agreementNo(null)
                                .agreementDate(null)
                                .virtualAccountNumber(null)
                                .build())
                        .build());

        when(lmsClient.getPartner(eq(legalPartner.getDeliveryService().getId())))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .oebsVirtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                        .balanceContract(ContractDto.newBuilder()
                                .externalId(null)
                                .build())
                        .build()));

        var delayO = fetchPartnerAgreementDataService.reenqueueDelay(
                new FetchPartnerAgreementDataPayload(TEST_REQUEST_ID, legalPartner.getId()));

        legalPartner = legalPartnerRepository.findByIdOrThrow(legalPartner.getId());

        assertThat(legalPartner.getVirtualAccountNumber()).isEqualTo(VIRTUAL_ACCOUNT_NUMBER);
        assertThat(delayO).isPresent();
    }

}
