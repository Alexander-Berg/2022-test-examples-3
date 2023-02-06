package ru.yandex.market.pvz.tms.executor.oebs_receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceipt;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptRepository;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOebsReceiptFactory;
import ru.yandex.market.pvz.tms.executor.oebs_receipt.model.OebsReceiptYtModel;
import ru.yandex.market.pvz.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@EmbeddedDbTmsTest
@Import({UploadOebsReceiptToYtExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UploadOebsReceiptToYtExecutorTest {

    private final OebsReceiptRepository oebsReceiptRepository;
    private final LegalPartnerRepository legalPartnerRepository;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestOebsReceiptFactory oebsReceiptFactory;
    private final UploadOebsReceiptToYtExecutor executor;

    private final Yt hahn;
    private final Yt arnold;

    private Map<Yt, ArgumentCaptor<Iterator<?>>> clusters;

    @BeforeEach
    void setup() {
        clusters = Map.of(
                hahn, ArgumentCaptor.forClass(Iterator.class),
                arnold, ArgumentCaptor.forClass(Iterator.class)
        );

        for (Yt cluster : clusters.keySet()) {
            Cypress cypress = cluster.cypress();
            when(cypress.exists(any(YPath.class))).thenReturn(false);
            when(cypress
                    .get(any(), any())
                    .getAttributeOrThrow(anyString())
                    .stringValue()
            ).thenReturn("mounted");

            YtTables tables = cluster.tables();
            doNothing().when(tables).insertRows(
                    any(), anyBoolean(), anyBoolean(),
                    any(YTableEntryType.class),
                    clusters.get(cluster).capture()
            );
        }
    }

    @Test
    void test() {
        OebsReceipt oebsReceipt = oebsReceiptFactory.create(TestOebsReceiptFactory.OebsReceiptTestParams.builder()
                .paymentOrderDate(LocalDate.of(2020, 11, 16))
                .sum(BigDecimal.TEN)
                .build());

        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        partner.setVirtualAccountNumber(oebsReceipt.getVirtualAccountNumber());
        legalPartnerRepository.saveAndFlush(partner);

        executor.doRealJob(null);

        for (ArgumentCaptor<?> captor : clusters.values()) {
            List<OebsReceiptYtModel> uploadedModels = (List) captor.getAllValues().stream()
                    .flatMap(arg -> Streams.stream((Iterator) arg))
                    .collect(Collectors.toList());

            assertThat(uploadedModels).containsExactlyInAnyOrderElementsOf(List.of(OebsReceiptYtModel.builder()
                    .oebsNumber(oebsReceipt.getOebsNumber())
                    .paymentOrderNumber(oebsReceipt.getPaymentOrderNumber())
                    .paymentOrderDate("16.11.2020")
                    .virtualAccountNumber(oebsReceipt.getVirtualAccountNumber())
                    .sum(10.0)
                    .legalPartnerId(partner.getId())
                    .legalPartnerName(partner.getOrganization().getFullName())
                    .build()));
        }

        oebsReceiptRepository.flush();
        executor.doRealJob(null);

        for (ArgumentCaptor<?> captor : clusters.values()) {
            List<OebsReceiptYtModel> models = (List) captor.getAllValues().stream()
                    .flatMap(arg -> Streams.stream((Iterator) arg))
                    .collect(Collectors.toList());

            assertThat(models).isEmpty();
        }
    }

    @Test
    void testNoReceiptWithoutPartner() {
        OebsReceipt oebsReceipt = oebsReceiptFactory.create(TestOebsReceiptFactory.OebsReceiptTestParams.builder()
                .paymentOrderDate(LocalDate.of(2020, 11, 16))
                .sum(BigDecimal.TEN)
                .build());

        oebsReceiptRepository.flush();
        executor.doRealJob(null);

        for (ArgumentCaptor<?> captor : clusters.values()) {
            List<OebsReceiptYtModel> uploadedModels = (List) captor.getAllValues().stream()
                    .flatMap(arg -> Streams.stream((Iterator) arg))
                    .collect(Collectors.toList());

            assertThat(uploadedModels).isEmpty();
        }
    }

}
