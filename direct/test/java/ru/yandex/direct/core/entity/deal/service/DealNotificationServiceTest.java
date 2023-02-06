package ru.yandex.direct.core.entity.deal.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.deal.model.BalancePrivateDealInfo;
import ru.yandex.direct.core.entity.deal.repository.DealNotificationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.DealNotificationsRecord;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.dssclient.DssClient;
import ru.yandex.direct.dssclient.http.certificates.Certificate;
import ru.yandex.direct.pdfgen.PdfBuilder;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mds.MdsHosts;
import ru.yandex.inside.mds.MdsNamespace;
import ru.yandex.inside.mds.MdsPostResponse;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.ip.HostPort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbschema.ppc.Tables.DEAL_NOTIFICATIONS;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealNotificationServiceTest {
    private static final byte[] PDF_BYTES = {1, 2, 3};
    private static final byte[] SIGNED_PDF_BYTES = {4, 5, 6};

    private static final HostPort LOCALHOST = new HostPort("localhost", 80);
    private static final String MDS_NAMESPACE_NAME = "direct-files";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private DealNotificationRepository dealNotificationRepository;

    @Autowired
    private Steps steps;

    @Mock
    private PdfBuilder pdfBuilder;

    @Mock
    private DssClient dssClient;

    @Mock
    private MdsHolder mdsHolder;

    @Mock
    private MdsPostResponse mdsPostResponse;

    @Mock
    private MdsHosts mdsHosts;

    @Mock
    private MdsNamespace mdsNamespace;

    @Mock
    private Certificate certificate;

    @Mock
    private DealNotificationPdfSignerService pdfSignerService;

    @Mock
    private DealNotificationMailSenderService mailSenderService;

    @Mock
    private BalancePrivateDealInfoService balancePrivateDealInfoService;

    @Mock
    private AgencyService agencyService;

    private int shard;
    private ClientInfo client;
    private DealInfo deal;
    private DealNotificationService dealNotificationService;

    @Before
    public void setUp() {
        client = steps.clientSteps().createDefaultClient();
        deal = steps.dealSteps().addRandomDeals(client, 1).iterator().next();

        shard = client.getShard();

        when(pdfBuilder.buildPdf(anyString(), any(DealNotificationPdfParameters.class))).thenReturn(PDF_BYTES);

        when(pdfSignerService.signPdf(any(), eq(PDF_BYTES))).thenReturn(SIGNED_PDF_BYTES);

        when(mdsPostResponse.getKey()).thenReturn(new MdsFileKey(1234, "567.pdf"));

        when(mdsHosts.getHostPortForRead()).thenReturn(LOCALHOST);
        when(mdsNamespace.getName()).thenReturn(MDS_NAMESPACE_NAME);

        when(mdsHolder.upload(anyString(), any(InputStreamSource.class))).thenReturn(mdsPostResponse);
        when(mdsHolder.getHosts()).thenReturn(mdsHosts);
        when(mdsHolder.getNamespace()).thenReturn(mdsNamespace);

        when(balancePrivateDealInfoService.getBalancePrivateDealInfo(eq(client.getClientId().asLong())))
                .thenReturn(new BalancePrivateDealInfo("2018/01", LocalDate.now(), "03", BigDecimal.valueOf(15L)));

        dealNotificationService = new DealNotificationService(mdsHolder, pdfBuilder,
                agencyService, dealNotificationRepository, pdfSignerService,
                mailSenderService, balancePrivateDealInfoService, shardHelper);
    }

    @Test
    public void sendDealNotification() {
        dealNotificationService.sendDealNotification(deal.getDeal());

        String mdsUrl = "http://localhost/get-direct-files/1234/567.pdf";

        DealNotificationsRecord fetchedRecord = dslContextProvider.ppc(shard)
                .select(DEAL_NOTIFICATIONS.DEAL_NOTIFICATION_ID,
                        DEAL_NOTIFICATIONS.CLIENT_ID,
                        DEAL_NOTIFICATIONS.CLIENT_NOTIFICATION_ID,
                        DEAL_NOTIFICATIONS.DEAL_ID,
                        DEAL_NOTIFICATIONS.PDF_MDS_KEY,
                        DEAL_NOTIFICATIONS.PDF_MDS_URL)
                .from(DEAL_NOTIFICATIONS)
                .where(DEAL_NOTIFICATIONS.CLIENT_ID.eq(client.getClientId().asLong()))
                .fetchOneInto(DEAL_NOTIFICATIONS);

        String notificationId = DATE_FORMATTER.format(LocalDate.now()) + "-" + deal.getDealId();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(fetchedRecord.getClientid()).isEqualTo(client.getClientId().asLong());
            softly.assertThat(fetchedRecord.getClientNotificationId()).isEqualTo(notificationId);
            softly.assertThat(fetchedRecord.getDealId()).isEqualTo(deal.getDealId());
            softly.assertThat(fetchedRecord.getPdfMdsKey()).isEqualTo("1234/567.pdf");
            softly.assertThat(fetchedRecord.getPdfMdsUrl()).isEqualTo(mdsUrl);
        });

        verify(mailSenderService).sendDealNotificationEmail(any(), eq(mdsUrl));

        verifyNoMoreInteractions(pdfBuilder,
                dssClient, certificate,
                mdsHolder, mdsHolder, mdsPostResponse, mdsHosts, mdsNamespace,
                pdfSignerService, mailSenderService, balancePrivateDealInfoService);
    }
}
