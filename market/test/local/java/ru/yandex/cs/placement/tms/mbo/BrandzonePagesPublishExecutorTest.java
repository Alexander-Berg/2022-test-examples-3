package ru.yandex.cs.placement.tms.mbo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.cs.billing.cutoff.model.DsCutoff;
import ru.yandex.cs.billing.cutoff.model.DsCutoffType;
import ru.yandex.cs.billing.cutoff.service.CutoffService;
import ru.yandex.vendor.brandzone.service.VendorBrandzoneService;
import ru.yandex.vendor.cms.client.MboCmsClient;
import ru.yandex.vendor.cms.model.CmsPage;
import ru.yandex.vendor.cms.model.CmsPagePublicationStatus;
import ru.yandex.vendor.vendors.CampaignProductInfoRetriever;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.vendor.products.model.VendorProduct.BRANDZONE;

@RunWith(MockitoJUnitRunner.class)
public class BrandzonePagesPublishExecutorTest {

    private final long VENDOR_ID_1 = 1L;
    private final long CAMPAIGN_ID_1 = 11L;
    private final long DATASOURCE_ID_1 = 111L;
    private final long VENDOR_ID_2 = 2L;
    private final long CAMPAIGN_ID_2 = 22L;
    private final long DATASOURCE_ID_2 = 222L;
    private final long VENDOR_ID_3 = 3L;
    private final long CAMPAIGN_ID_3 = 33L;
    private final long DATASOURCE_ID_3 = 333L;
    private final long VENDOR_ID_4 = 4L;
    private final long CAMPAIGN_ID_4 = 44L;
    private final long DATASOURCE_ID_4 = 444L;

    private final CmsPage publishedPage =
            new CmsPage(VENDOR_ID_1, "123", null, true,
                    new Date(System.currentTimeMillis() - 24*60*60*1000 - 1), 100500L);
    private final CmsPage unpublishedPage =
            new CmsPage(VENDOR_ID_2, "456", null, false,
                    new Date(System.currentTimeMillis() - 24*60*60*1000 - 1), 100500L);
    private final CmsPage justPublishedPage =
            new CmsPage(VENDOR_ID_3, "789", null, true,
                    new Date(System.currentTimeMillis()-1), 100500L);
    private final CmsPage justUnpublishedPage =
            new CmsPage(VENDOR_ID_4, "1011", null, false,
                    new Date(System.currentTimeMillis() - 1), 100500L);


    private CmsPagePublicationStatus successfullPublicationStatus;

    @Mock
    private VendorBrandzoneService vendorBrandzoneService;

    @Mock
    private MboCmsClient mboCmsClient;

    @Mock
    private CutoffService cutoffService;

    @Mock
    private CampaignProductInfoRetriever campaignProductInfoRetriever;

    @InjectMocks
    private BrandzonePagesPublishExecutor brandzonePagesPublishExecutor;

    @Before
    public void before() {
        when(campaignProductInfoRetriever.getActiveCampaignAndDatasourcePair(VENDOR_ID_1, BRANDZONE))
                .thenReturn(Optional.of(Pair.of(CAMPAIGN_ID_1, DATASOURCE_ID_1)));
        when(campaignProductInfoRetriever.getActiveCampaignAndDatasourcePair(VENDOR_ID_2, BRANDZONE))
                .thenReturn(Optional.of(Pair.of(CAMPAIGN_ID_2, DATASOURCE_ID_2)));
        when(campaignProductInfoRetriever.getActiveCampaignAndDatasourcePair(VENDOR_ID_3, BRANDZONE))
                .thenReturn(Optional.of(Pair.of(CAMPAIGN_ID_3, DATASOURCE_ID_3)));
        when(campaignProductInfoRetriever.getActiveCampaignAndDatasourcePair(VENDOR_ID_4, BRANDZONE))
                .thenReturn(Optional.of(Pair.of(CAMPAIGN_ID_4, DATASOURCE_ID_4)));

        DsCutoff dsCutoff2 = new DsCutoff();
        dsCutoff2.setId(2L);
        dsCutoff2.setDatasourceId(DATASOURCE_ID_2);
        dsCutoff2.setFrom(new Date(System.currentTimeMillis() - 1));
        dsCutoff2.setType(DsCutoffType.FINANCE);

        when(cutoffService.findActive(BRANDZONE.getServiceId(), DATASOURCE_ID_2))
                .thenReturn(Collections.singletonList(dsCutoff2));

        DsCutoff dsCutoff4 = new DsCutoff();
        dsCutoff4.setId(4L);
        dsCutoff4.setDatasourceId(DATASOURCE_ID_4);
        dsCutoff4.setFrom(new Date(System.currentTimeMillis() - 1));
        dsCutoff4.setType(DsCutoffType.OFFER_DOCUMENTS);

        when(cutoffService.findActive(BRANDZONE.getServiceId(), DATASOURCE_ID_4))
                .thenReturn(Collections.singletonList(dsCutoff4));

        successfullPublicationStatus = new CmsPagePublicationStatus();
        successfullPublicationStatus.setStatus("SUCCESS");
    }

    @Test
    @DisplayName("Устаревшие страницы должны пытаться распубликоваться независимо от времени предыдущих попыток. " +
            "Повторная распубликация раз в сутки.")
    public void obsoletePagesShouldBeUnpublished() {
        when(vendorBrandzoneService.getObsoleteBrandzonePages())
                .thenReturn(Arrays.asList(
                        publishedPage,
                        unpublishedPage,
                        justPublishedPage,
                        justUnpublishedPage));
        brandzonePagesPublishExecutor.doJob(Mockito.mock(JobExecutionContext.class));
        verify(mboCmsClient).unpublishPage(publishedPage.getPageId());
        verify(mboCmsClient).unpublishPage(unpublishedPage.getPageId());
        verify(mboCmsClient).unpublishPage(justPublishedPage.getPageId());
        // Повторная распубликация раз в сутки
        // verify(mboCmsClient).unpublishPage(justUnpublishedPage.getPageId());
        verifyNoMoreInteractions(mboCmsClient);
    }

    @Test
    @DisplayName("Актуальные страницы должны пытаться публиковаться не чаще, чем раз в сутки")
    public void actualPublishedPagesShouldBePublishedOnceADay() {
        when(vendorBrandzoneService.getActualBrandzonePages())
                .thenReturn(Arrays.asList(
                        publishedPage,
                        justPublishedPage));

        when(mboCmsClient.publishPage(publishedPage.getPageId())).thenReturn(Optional.of(successfullPublicationStatus));

        brandzonePagesPublishExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        verify(mboCmsClient).publishPage(publishedPage.getPageId());
        verify(vendorBrandzoneService).updateBrandzonePagePublicationStatus(publishedPage.getPageId(), true);

        verifyNoMoreInteractions(mboCmsClient);
    }

    @Test
    @DisplayName("Актуальные страницы должны пытаться распубликоваться не чаще, чем раз в сутки")
    public void actualUnpublishedPagesShouldBeUnpublishedOnceADay() {
        when(vendorBrandzoneService.getActualBrandzonePages())
                .thenReturn(Arrays.asList(
                        unpublishedPage,
                        justUnpublishedPage));

        when(mboCmsClient.unpublishPage(unpublishedPage.getPageId())).thenReturn(Optional.of(successfullPublicationStatus));

        brandzonePagesPublishExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        verify(mboCmsClient).unpublishPage(unpublishedPage.getPageId());
        verify(vendorBrandzoneService).updateBrandzonePagePublicationStatus(unpublishedPage.getPageId(), false);

        verifyNoMoreInteractions(mboCmsClient);
    }

    @Test
    @DisplayName("Актуальные распубликованные страницы должны опубликоваться, если услуга активна")
    public void actualUnpublishedPagesShouldBePublishedIfNoCutoffs() {
        when(vendorBrandzoneService.getActualBrandzonePages())
                .thenReturn(Arrays.asList(
                        unpublishedPage,
                        justUnpublishedPage));

        when(mboCmsClient.publishPage(unpublishedPage.getPageId())).thenReturn(Optional.of(successfullPublicationStatus));
        when(cutoffService.findActive(BRANDZONE.getServiceId(), DATASOURCE_ID_2))
                .thenReturn(Collections.emptyList());

        brandzonePagesPublishExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        verify(mboCmsClient).publishPage(unpublishedPage.getPageId());
        verify(vendorBrandzoneService).updateBrandzonePagePublicationStatus(unpublishedPage.getPageId(), true);

        verifyNoMoreInteractions(mboCmsClient);
    }

    @Test
    @DisplayName("Актуальные опубликованные страницы должны распубликоваться, если услуга неактивна")
    public void actualPublishedPagesShouldBePublishedIfNoCutoffs() {
        DsCutoff financeCutoff = new DsCutoff();
        financeCutoff.setId(1L);
        financeCutoff.setDatasourceId(DATASOURCE_ID_1);
        financeCutoff.setFrom(new Date(System.currentTimeMillis() - 1));
        financeCutoff.setType(DsCutoffType.FINANCE);

        when(vendorBrandzoneService.getActualBrandzonePages())
                .thenReturn(Arrays.asList(
                        publishedPage,
                        justPublishedPage));

        when(mboCmsClient.unpublishPage(publishedPage.getPageId())).thenReturn(Optional.of(successfullPublicationStatus));
        when(cutoffService.findActive(BRANDZONE.getServiceId(), DATASOURCE_ID_1))
                .thenReturn(Collections.singletonList(financeCutoff));

        brandzonePagesPublishExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        verify(mboCmsClient).unpublishPage(publishedPage.getPageId());
        verify(vendorBrandzoneService).updateBrandzonePagePublicationStatus(publishedPage.getPageId(), false);

        verifyNoMoreInteractions(mboCmsClient);
    }

}
