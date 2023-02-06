package ru.yandex.direct.core.entity.deal.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.dssclient.DssClient;
import ru.yandex.direct.dssclient.http.certificates.Certificate;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.DSS_CERTIFICATE_SERIAL_NUMBER;

@ParametersAreNonnullByDefault
public class DealNotificationPdfSignerServiceTest {
    private static final Date CERTIFICATE_START_DATE =
            new Date(new GregorianCalendar(2018, 1, 1).toInstant().toEpochMilli());

    private static final Date CERTIFICATE_END_DATE =
            new Date(new GregorianCalendar(2018, 12, 1).toInstant().toEpochMilli());

    private static final long CERTIFICATE_ID = 123L;
    private static final String CERTIFICATE_PIN_CODE = "1234";

    private static final byte[] PDF_BYTES = {1, 2, 3};
    private static final byte[] SIGNED_PDF_BYTES = {4, 5, 6};

    private static final String PDF_FILE_NAME = "1.pdf";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private Certificate certificate;

    @Mock
    private DssClient dssClient;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    private DealNotificationPdfSignerService pdfSignerService;

    @Before
    public void setUp() throws Exception {
        when(certificate.isActive()).thenReturn(true);
        when(certificate.getId()).thenReturn(CERTIFICATE_ID);
        when(certificate.getStartDate()).thenReturn(CERTIFICATE_START_DATE);
        when(certificate.getEndDate()).thenReturn(CERTIFICATE_END_DATE);
        when(certificate.getCertificateSerialNumber()).thenReturn(BigInteger.valueOf(0xDEADBEEFL));
        when(certificate.getPinCode()).thenReturn(CERTIFICATE_PIN_CODE);

        when(dssClient.getCertificateList()).thenReturn(singletonList(certificate));

        when(dssClient.signPdf(eq(PDF_BYTES), eq(PDF_FILE_NAME), any(), eq(CERTIFICATE_ID), eq(CERTIFICATE_PIN_CODE)))
                .thenReturn(SIGNED_PDF_BYTES);

        when(ppcPropertiesSupport.get(DSS_CERTIFICATE_SERIAL_NUMBER.getName())).thenReturn(null);

        pdfSignerService = new DealNotificationPdfSignerService(dssClient, ppcPropertiesSupport);
    }

    @Test
    public void signPdf() {
        assertThat(pdfSignerService.signPdf(PDF_FILE_NAME, PDF_BYTES)).isEqualTo(SIGNED_PDF_BYTES);
        verifyNoMoreInteractions(dssClient, certificate);
    }
}
