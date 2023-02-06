package ru.yandex.market.pvz.core.domain.legal_partner.offer;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferManager;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerOfferManagerTest {

    private static final String FILE_NAME = "file.pdf";
    private static final byte[] FILE_DATA = {4, 8, 15, 16, 23, 42};

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final LegalPartnerOfferManager offerManager;
    private final LegalPartnerOfferRepository legalPartnerOfferRepository;

    @BeforeEach
    void setup() {
        legalPartnerOfferRepository.deleteAll();
    }

    @Test
    void testUploadAndGet() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());
        LegalPartnerOfferParams offer = LegalPartnerOfferParams.builder()
                .legalPartnerId(legalPartner.getId())
                .legalPartnerExternalId(legalPartner.getPartnerId())
                .filename(FILE_NAME)
                .data(FILE_DATA)
                .published(false)
                .build();

        offerManager.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);

        assertSameParams(offerManager.getOffer(legalPartner.getPartnerId()), offer);
    }

    @Test
    void testReuploadAndGet() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());
        LegalPartnerOfferParams offer = LegalPartnerOfferParams.builder()
                .legalPartnerId(legalPartner.getId())
                .legalPartnerExternalId(legalPartner.getPartnerId())
                .filename(FILE_NAME)
                .data(FILE_DATA)
                .published(false)
                .build();

        LegalPartnerOfferParams anotherOffer = LegalPartnerOfferParams.builder()
                .legalPartnerId(legalPartner.getId())
                .legalPartnerExternalId(legalPartner.getPartnerId())
                .filename(FILE_NAME + "other")
                .data(new byte[]{1})
                .published(false)
                .build();

        offerManager.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);
        assertThatThrownBy(() -> offerManager.saveOffer(
                legalPartner.getPartnerId(), anotherOffer.getFilename(), anotherOffer.getData()));

        assertSameParams(offerManager.getOffer(legalPartner.getPartnerId()), offer);
    }

    @Test
    void testUploadAndDelete() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());

        offerManager.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);
        assertThat(offerManager.tryGetOffer(legalPartner.getId())).isPresent();

        offerManager.deleteOfferIfExists(legalPartner.getId());
        assertThat(offerManager.tryGetOffer(legalPartner.getId())).isEmpty();
    }

    @Test
    void testGetUnpublishedOffers() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());

        var anotherPreLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var anotherPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(anotherPreLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(anotherPreLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(anotherPreLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(anotherPreLegalPartner.getId());

        LegalPartnerOfferParams offer = LegalPartnerOfferParams.builder()
                .legalPartnerId(legalPartner.getId())
                .legalPartnerExternalId(legalPartner.getPartnerId())
                .filename(FILE_NAME)
                .data(FILE_DATA)
                .published(false)
                .build();

        LegalPartnerOfferParams anotherOffer = LegalPartnerOfferParams.builder()
                .legalPartnerId(anotherPartner.getId())
                .legalPartnerExternalId(anotherPartner.getPartnerId())
                .filename(FILE_NAME + "other")
                .data(new byte[]{1})
                .published(true)
                .build();

        offerManager.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);
        offerManager.saveOffer(anotherPartner.getPartnerId(), anotherOffer.getFilename(), anotherOffer.getData());

        LegalPartnerOfferParams savedAnotherOffer = offerManager.getOffer(anotherPartner.getPartnerId());
        offerManager.markPublished(savedAnotherOffer.getId(), Instant.EPOCH);

        List<LegalPartnerOfferParams> params = offerManager.getUnpublishedOffers();
        assertThat(params).hasSize(1);
        assertSameParams(params.get(0), offer);
    }

    private void assertSameParams(LegalPartnerOfferParams a, LegalPartnerOfferParams b) {
        assertThat(a.getLegalPartnerId()).isEqualTo(b.getLegalPartnerId());
        assertThat(a.getLegalPartnerExternalId()).isEqualTo(b.getLegalPartnerExternalId());
        assertThat(a.getFilename()).isEqualTo(b.getFilename());
        assertThat(a.getData()).isEqualTo(b.getData());
        assertThat(a.isPublished()).isEqualTo(b.isPublished());
    }

}
