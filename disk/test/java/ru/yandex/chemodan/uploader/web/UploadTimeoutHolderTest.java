package ru.yandex.chemodan.uploader.web;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author akirakozov
 */
public class UploadTimeoutHolderTest {
    private final Duration EXTERNAL_TIMEOUT = Duration.standardMinutes(30);
    private final Duration MAX_TIMEOUT = Duration.standardMinutes(10);
    private final Duration MIN_TIMEOUT = Duration.standardMinutes(1);

    private final UploadTimeoutHolder holder = new UploadTimeoutHolder(EXTERNAL_TIMEOUT, MAX_TIMEOUT, MIN_TIMEOUT);
    private final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);

    @Test
    public void getWaitCommitFileUploadTimeout() {
        Assert.equals(MAX_TIMEOUT, holder.getWaitCommitFileUploadTimeout(true, true));
        Assert.equals(MIN_TIMEOUT, holder.getWaitCommitFileUploadTimeout(false, true));

        Assert.equals(EXTERNAL_TIMEOUT, holder.getWaitCommitFileUploadTimeout(false, false));
        Assert.equals(EXTERNAL_TIMEOUT, holder.getWaitCommitFileUploadTimeout(true, false));
    }

    @Test
    public void getUploadTimeoutWithoutCompleteHeader() {
        when(req.getHeader("User-Agent")).thenReturn("Yandex.Disk {}");
        Assert.equals(MAX_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithTrueCompleteHeader() {
        when(req.getHeader(UploadTimeoutHolder.COMPLETE_UPLOAD_HEADER)).thenReturn("true");
        when(req.getHeader("User-Agent")).thenReturn("Yandex.Disk {}");
        Assert.equals(MAX_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithFalseCompleteHeader() {
        when(req.getHeader(UploadTimeoutHolder.COMPLETE_UPLOAD_HEADER)).thenReturn("false");
        when(req.getHeader("User-Agent")).thenReturn("Yandex.Disk {}");
        Assert.equals(MIN_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithFalseCompleteParameter() {
        when(req.getQueryString()).thenReturn("wait-complete-upload=false");
        when(req.getHeader("User-Agent")).thenReturn("Yandex.Disk {}");
        Assert.equals(MIN_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithTrueCompleteParameter() {
        when(req.getParameter(ApiArgs.WAIT_COMPLETE_UPLOAD)).thenReturn("true");
        when(req.getHeader("User-Agent")).thenReturn("Yandex.Disk {}");
        Assert.equals(MAX_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithoutCompleteHeaderExternal() {
        Assert.equals(EXTERNAL_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithTrueCompleteHeaderExternal() {
        when(req.getHeader(UploadTimeoutHolder.COMPLETE_UPLOAD_HEADER)).thenReturn("true");
        Assert.equals(EXTERNAL_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithFalseCompleteHeaderExternal() {
        when(req.getHeader(UploadTimeoutHolder.COMPLETE_UPLOAD_HEADER)).thenReturn("false");
        Assert.equals(EXTERNAL_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithFalseCompleteParameterExternal() {
        when(req.getQueryString()).thenReturn("wait-complete-upload=false");
        Assert.equals(EXTERNAL_TIMEOUT, holder.getUploadTimeout(req));
    }

    @Test
    public void getUploadTimeoutWithTrueCompleteParameterExternal() {
        when(req.getParameter(ApiArgs.WAIT_COMPLETE_UPLOAD)).thenReturn("true");
        Assert.equals(EXTERNAL_TIMEOUT, holder.getUploadTimeout(req));
    }
}
