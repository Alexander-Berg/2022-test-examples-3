package ru.yandex.common.services.fs.storage.versioned.elliptics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.services.fs.EllipticFileStorageService;
import ru.yandex.common.services.fs.storage.versioned.model.DeletionMode;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.date.DateUtil.getFullString;

/**
 * Created by kudrale on 24.12.14.
 */
@RunWith(MockitoJUnitRunner.class)
public class EllipticsVersionedFileStorageServiceTest {

    @Mock
    private EllipticFileStorageService ellipticFileStorageService;
    private EllipticsVersionedFileStorageService ellipticsVersionedFileStorageService;
    private String date = getFullString(new Date());
    private String oldDateString = "2013-01-01";
    private Date oldDate;

    @Before
    public void setUp() {
        ellipticsVersionedFileStorageService = new EllipticsVersionedFileStorageService();
        ellipticsVersionedFileStorageService.setEllipticFileStorageService(ellipticFileStorageService);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2013, 1, 1);
        oldDate = calendar.getTime();
    }

    @Test
    public void testPutNewFile() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenThrow(new FileNotFoundException());
        when(ellipticFileStorageService.getInputStream(eq("log_" + date))).thenThrow(new FileNotFoundException());
        when(ellipticFileStorageService.getInputStream(eq("meta"))).thenThrow(new FileNotFoundException());
        when(ellipticFileStorageService.saveFile(eq("tmp1"), eq("tmp2_" + date + "_0"))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        ellipticsVersionedFileStorageService.putFile("tmp1", "tmp2");
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("logs"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + date));
        verify(ellipticFileStorageService).saveFile(eq("tmp1"), eq("tmp2_" + date + "_0"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("meta"));
    }

    @Test
    public void testPutSecondFile() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("log_" + date))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + date + "_0 " + date + "T00:03:36").getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("meta"))).thenReturn(new ByteArrayInputStream(("tmp2 tmp2_" + date + "_0").getBytes()));
        when(ellipticFileStorageService.saveFile(eq("tmp1"), eq("tmp2_" + date + "_1"))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        ellipticsVersionedFileStorageService.putFile("tmp1", "tmp2");
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + date));
        verify(ellipticFileStorageService).saveFile(eq("tmp1"), eq("tmp2_" + date + "_1"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("meta"));
    }

    @Test
    public void testPutNewDate() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("logs_" + oldDateString).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + date))).thenThrow(new FileNotFoundException());
        when(ellipticFileStorageService.getInputStream(eq("meta"))).thenReturn(new ByteArrayInputStream(("tmp2 tmp2_" + oldDateString + "_0").getBytes()));
        when(ellipticFileStorageService.saveFile(eq("tmp1"), eq("tmp2_" + date + "_1"))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        ellipticsVersionedFileStorageService.putFile("tmp1", "tmp2");
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("logs"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + date));
        verify(ellipticFileStorageService).saveFile(eq("tmp1"), eq("tmp2_" + date + "_1"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("meta"));
    }

    @Test
    public void testCleanLogs() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("meta"))).thenReturn(new ByteArrayInputStream(("tmp2 tmp2_" + date + "_1").getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + oldDateString))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + oldDateString + "_1 " + oldDateString + "T01:03:36\nDEL tmp2_" + oldDateString + "_1 " + oldDateString + "T23:03:36\n").getBytes()));
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        when(ellipticFileStorageService.fileExists(eq("log_" + oldDateString))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.deleteFile(eq("log_" + oldDateString))).thenReturn(Boolean.TRUE);
        ellipticsVersionedFileStorageService.cleanLogs(oldDate);
        InOrder order = inOrder(ellipticFileStorageService);
        order.verify(ellipticFileStorageService).deleteFile(eq("log_" + oldDateString));
        order.verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("logs"));
    }

    @Test
    public void testCleanLogsWithFile() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + oldDateString))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + oldDateString + "_0 " + oldDateString + "T00:03:36\nINS tmp2_" + oldDateString + "_1 " + oldDateString + "T01:03:36\nDEL tmp2_" + oldDateString + "_1 " + oldDateString + "T23:03:36\n").getBytes()));
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        when(ellipticFileStorageService.fileExists(eq("log_" + oldDateString))).thenReturn(Boolean.TRUE);
        ellipticsVersionedFileStorageService.cleanLogs(oldDate);
        verify(ellipticFileStorageService, never()).deleteFile(eq("log_" + oldDateString));
        verify(ellipticFileStorageService, never()).saveInputStreamAndGetSize(any(InputStream.class), eq("logs"));
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + oldDateString));
    }

    @Test
    public void testCleanLogNotFound() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("meta"))).thenReturn(new ByteArrayInputStream(("tmp2 tmp2_" + date + "_1").getBytes()));
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        when(ellipticFileStorageService.fileExists(eq("log_" + oldDateString))).thenReturn(Boolean.FALSE);
        ellipticsVersionedFileStorageService.cleanLogs(oldDate);
        verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("logs"));
    }

    @Test
    public void testDelete() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + oldDateString))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + oldDateString + "_0 " + oldDateString + "T00:03:36\n").getBytes()));
        when(ellipticFileStorageService.saveInputStreamAndGetSize(any(InputStream.class), anyString())).thenReturn(1);
        when(ellipticFileStorageService.fileExists(eq("tmp2_" + oldDateString + "_0"))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.deleteFile(eq("tmp2_" + oldDateString + "_0"))).thenReturn(Boolean.TRUE);
        ellipticsVersionedFileStorageService.deleteFileVersions("tmp2", new Date(0), oldDate, DeletionMode.ALL);
        InOrder order = inOrder(ellipticFileStorageService);
        order.verify(ellipticFileStorageService).fileExists(eq("tmp2_" + oldDateString + "_0"));
        order.verify(ellipticFileStorageService).deleteFile(eq("tmp2_" + oldDateString + "_0"));
        order.verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + oldDateString));
    }

    @Test(expected = IOException.class)
    public void testDeleteEx() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + oldDateString))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + oldDateString + "_0 " + oldDateString + "T00:03:36\n").getBytes()));
        when(ellipticFileStorageService.fileExists(eq("tmp2_" + oldDateString + "_0"))).thenReturn(Boolean.TRUE);
        when(ellipticFileStorageService.deleteFile(eq("tmp2_" + oldDateString + "_0"))).thenReturn(Boolean.FALSE);
        ellipticsVersionedFileStorageService.deleteFileVersions("tmp2", new Date(0), oldDate, DeletionMode.ALL);
        InOrder order = inOrder(ellipticFileStorageService);
        order.verify(ellipticFileStorageService).fileExists(eq("tmp2_" + oldDateString + "_0"));
        order.verify(ellipticFileStorageService).deleteFile(eq("tmp2_" + oldDateString + "_0"));
        order.verify(ellipticFileStorageService).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + oldDateString));
    }

    @Test
    public void testDeleted() throws IOException {
        when(ellipticFileStorageService.getInputStream(eq("logs"))).thenReturn(new ByteArrayInputStream(("log_" + oldDateString + "\nlog_" + date).getBytes()));
        when(ellipticFileStorageService.getInputStream(eq("log_" + oldDateString))).thenReturn(new ByteArrayInputStream(("INS tmp2_" + oldDateString + "_0 " + oldDateString + "T00:03:36\nDEL tmp2_" + oldDateString + "_0 " + oldDateString + "T23:00:00\n").getBytes()));
        ellipticsVersionedFileStorageService.deleteFileVersions("tmp2", new Date(0), oldDate, DeletionMode.ALL);
        verify(ellipticFileStorageService, never()).fileExists(eq("tmp2_" + oldDateString + "_0"));
        verify(ellipticFileStorageService, never()).deleteFile(eq("tmp2_" + oldDateString + "_0"));
        verify(ellipticFileStorageService, never()).saveInputStreamAndGetSize(any(InputStream.class), eq("log_" + oldDateString));
    }
}
