package ru.yandex.market.logistics.werewolf.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.werewolf.AbstractTest;
import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HtmlToPdfConverter")
class HtmlToPdfConverterTest extends AbstractTest {
    @Autowired
    private HtmlToPdfConverter converter;

    @Autowired
    private ExternalProcessBuilder wkhtmltopdfProcess;

    @DisplayName("выброс исключения при exit code != 0")
    @Test
    @SneakyThrows
    void convertErrorExit() {
        InputStream htmlStream = ByteArrayInputStream.nullInputStream();
        OutputStream stdIn = ByteArrayOutputStream.nullOutputStream();
        InputStream stdOut = ByteArrayInputStream.nullInputStream();
        String stderr = "error";
        InputStream stdErr = new ByteArrayInputStream(stderr.getBytes());

        Process fakeProcess = createFakeProcess(stdIn, stdOut, stdErr);
        when(fakeProcess.exitValue()).thenReturn(-1);
        when(fakeProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(wkhtmltopdfProcess.startProcess(any())).thenReturn(fakeProcess);

        IOException exception = Assertions.catchThrowableOfType(() ->
            converter.convert(htmlStream, defaultWriterOptions()),
        IOException.class);

        softly.assertThat(exception)
            .hasCauseInstanceOf(HtmlToPdfConverter.ConversionErrorException.class);
        softly.assertThat(exception.getCause())
            .hasMessage("Conversion error: %s", stderr);
    }

    @DisplayName("выброс исключения при таймауте процесса")
    @Test
    @SneakyThrows
    void convertErrorTimeout() {
        InputStream htmlStream = ByteArrayInputStream.nullInputStream();
        OutputStream stdIn = ByteArrayOutputStream.nullOutputStream();
        InputStream stdOut = ByteArrayInputStream.nullInputStream();
        InputStream stdErr = ByteArrayInputStream.nullInputStream();

        Process fakeProcess = createFakeProcess(stdIn, stdOut, stdErr);
        when(fakeProcess.waitFor(anyLong(), any())).thenReturn(false);
        when(wkhtmltopdfProcess.startProcess(any())).thenReturn(fakeProcess);

        IOException exception = Assertions.catchThrowableOfType(() ->
            converter.convert(htmlStream, defaultWriterOptions()),
        IOException.class);

        softly.assertThat(exception)
            .hasCauseInstanceOf(TimeoutException.class);
        softly.assertThat(exception.getCause())
            .hasMessageStartingWith("Conversion timeout after");

        verify(fakeProcess).destroy();
    }

    @DisplayName("успешный сценарий, exit code = 0")
    @Test
    @SneakyThrows
    void convertSuccess() {
        String output = "tstoutput";
        InputStream htmlStream = ByteArrayInputStream.nullInputStream();
        OutputStream stdIn = ByteArrayOutputStream.nullOutputStream();
        InputStream stdOut = new ByteArrayInputStream(output.getBytes());
        InputStream stdErr = ByteArrayInputStream.nullInputStream();

        Process fakeProcess = createFakeProcess(stdIn, stdOut, stdErr);
        when(fakeProcess.exitValue()).thenReturn(HtmlToPdfConverter.SUCCESS_CODE);
        when(fakeProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(wkhtmltopdfProcess.startProcess(any())).thenReturn(fakeProcess);

        byte[] result = converter.convert(htmlStream, defaultWriterOptions());
        softly.assertThat(result).containsExactly(output.getBytes());
    }

    @SneakyThrows
    private Process createFakeProcess(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
        Process fakeProcess = mock(Process.class);
        when(fakeProcess.getOutputStream()).thenReturn(stdIn);
        when(fakeProcess.getErrorStream()).thenReturn(stdErr);
        when(fakeProcess.getInputStream()).thenReturn(stdOut);

        return fakeProcess;
    }

    private WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A4, PageOrientation.PORTRAIT);
    }
}
