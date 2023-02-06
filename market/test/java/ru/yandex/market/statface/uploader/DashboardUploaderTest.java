package ru.yandex.market.statface.uploader;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.statface.StatfaceClient;
import ru.yandex.market.statface.StatfaceData;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 29.09.17.
 */
public class DashboardUploaderTest {
    private static final int MAX_UPLOAD_DAYS = 19;
    private static final String REPORT_NAME = "uploadMe";
    private static final Date LAST_UPLOAD = DateUtils.addDays(new Date(), -MAX_UPLOAD_DAYS / 2);

    @InjectMocks
    private DashboardUploader dashboardUploader;
    @Mock
    private StatfaceClient statfaceClient;
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private IDashboardReport report;
    @Mock
    private StatfaceData statfaceData;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        dashboardUploader.setReports(Collections.singletonList(report));
        dashboardUploader.setMaxUploadDays(MAX_UPLOAD_DAYS);
        when(report.getReportName()).thenReturn(REPORT_NAME);
        when(report.getData(any(), any())).thenReturn(statfaceData);
        when(configurationService.getValueAsDate(contains(REPORT_NAME))).thenReturn(LAST_UPLOAD);
    }

    @Test
    public void upload() throws Exception {
        dashboardUploader.uploadReports();
        checkUploadWasSuccessful();
    }

    @Test
    public void uploadWithTimeout() throws Exception {
        doThrow(new SocketTimeoutException()).when(statfaceClient).sendData(any());
        dashboardUploader.uploadReports();
        checkUploadWasSuccessful();
    }

    @Test
    public void uploadFailed() throws Exception {
        doThrow(new IOException()).when(statfaceClient).sendData(any());
        dashboardUploader.uploadReports();
        verify(configurationService, never()).mergeValueAsDate(any(), any());
    }

    private void checkUploadWasSuccessful() throws IOException {
        verify(report).getData(eq(LAST_UPLOAD), gt(LAST_UPLOAD));
        verify(statfaceClient).sendData(statfaceData);
        verify(configurationService).mergeValueAsDate(contains(REPORT_NAME), gt(LAST_UPLOAD));
    }
}
