package ru.yandex.market.pvz.core.domain.legal_partner.offer;

import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.tpl.common.util.logging.Tracer;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxUser;
import ru.yandex.market.tpl.report.core.ReportService;
import ru.yandex.market.tpl.report.core.ReportType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager.YANDEX_MARKET_INFO;
import static ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager.YANDEX_MARKET_ORGANIZATION;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerOfferGeneratorTest {

    private final LegalPartnerQueryService legalPartnerQueryService;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final LegalPartnerOfferGenerator generator;
    private final TestableClock clock;

    @MockBean
    private ReportService reportService;

    @MockBean
    private BlackboxClient blackboxClient;

    @Captor
    private ArgumentCaptor<Map<String, Object>> parametersCaptor;

    @Test
    void testGenerate() {
        BlackboxUser user = new BlackboxUser();
        user.setLogin("login");

        Tracer.putUidToStatic(1L);
        clock.setFixed(Instant.EPOCH, clock.getZone());
        when(blackboxClient.invokeUserinfo(anyLong())).thenReturn(user);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        LegalPartnerParams partnerParams = legalPartnerQueryService.get(legalPartner.getId());
        generator.generate(legalPartner.getPartnerId());

        verify(reportService, times(1))
                .makeReport(anyString(), eq(ReportType.PDF), parametersCaptor.capture(), any(), any(), eq(Map.of(
                        JRPdfExporterParameter.METADATA_TITLE,
                        "Заявление " + legalPartner.getOrganization().getFullName()
                )));

        assertThat(parametersCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("legalPartnerId", partnerParams.getId()),
                Map.entry("date", "01.01.1970"),
                Map.entry("orgName", partnerParams.getOrganization().getFullName()),
                Map.entry("orgType", partnerParams.getOrganization().getLegalType().getDescription()),
                Map.entry("ogrn", partnerParams.getOrganization().getOgrn()),
                Map.entry("inn", partnerParams.getOrganization().getTaxpayerNumber()),
                Map.entry("kpp", partnerParams.getOrganization().getKpp()),
                Map.entry("legalAddress", partnerParams.getBusinessAddress().toString()),
                Map.entry("email", partnerParams.getDelegate().getDelegateEmail()),
                Map.entry("phone", partnerParams.getDelegate().getDelegatePhone()),
                Map.entry("accountantName", partnerParams.getAccountant().getAccountantFio()),
                Map.entry("accountantEmail", partnerParams.getAccountant().getAccountantEmail()),
                Map.entry("accountantPhone", partnerParams.getAccountant().getAccountantPhone()),
                Map.entry("checkingAccountNumber", partnerParams.getBank().getCheckingAccountNumber()),
                Map.entry("bankName", partnerParams.getBank().getBank()),
                Map.entry("rcbic", partnerParams.getBank().getRcbic()),
                Map.entry("correspondentAccountNumber", partnerParams.getBank().getCorrespondentAccountNumber()),
                Map.entry("commissionerName", partnerParams.getCommissioner().getCommissionerFio()),
                Map.entry("commissionerDocumentGenitive", partnerParams.getCommissioner().getCommissionerDocument()
                        .getDescriptionGenitive()),
                Map.entry("partnerYandexLogin", user.getLogin()),
                Map.entry("yandexName", YANDEX_MARKET_ORGANIZATION),
                Map.entry("yandexInfo", YANDEX_MARKET_INFO)
        ));
    }

}
