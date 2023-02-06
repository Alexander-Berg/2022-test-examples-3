package ru.yandex.market.logistic.gateway.service.converter;

import java.io.InputStream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.model.entity.PdfLayout;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HtmlToPdfConverterTest extends BaseTest {

    private HtmlToPdfConverter htmlToPdfConverter;
    @Mock
    private WwClient wwClient;

    private static String HTML = StringEscapeUtils.escapeHtml4("<html><body><table cellspacing=\"0\" " +
        "cellpadding=\"100\" border=\"1\"><tr><td>something</td><tr></table></body></html>");
    private static byte[] EXPECTED = "Example output".getBytes();

    @Before
    public void setup() {
        htmlToPdfConverter = new HtmlToPdfConverter(wwClient);
    }

    @Test
    public void convertA4Success() throws Exception {
        when(wwClient.convertHtmlToPdf(any(), any(), any())).thenReturn(EXPECTED);
        byte[] bytes = htmlToPdfConverter.convertWithUnescape(HTML, PdfLayout.ATTACHED_DOC);

        assertions.assertThat(bytes).isEqualTo(EXPECTED);
        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(wwClient).convertHtmlToPdf(
            inputStreamArgumentCaptor.capture(),
            eq(PageSize.A4),
            eq(PageOrientation.PORTRAIT)
        );
    }

    @Test
    public void convertA6Success() throws Exception {
        when(wwClient.convertHtmlToPdf(any(), any(), any())).thenReturn(EXPECTED);
        byte[] bytes = htmlToPdfConverter.convertWithUnescape(HTML, PdfLayout.LABEL);

        assertions.assertThat(bytes).isEqualTo(EXPECTED);
        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(wwClient).convertHtmlToPdf(
            inputStreamArgumentCaptor.capture(),
            eq(PageSize.A6),
            eq(PageOrientation.PORTRAIT)
        );
    }

    @Test
    public void convertFailCall() {
        when(wwClient.convertHtmlToPdf(any(), any(), any())).thenThrow(new RuntimeException("error"));
        assertions.assertThatThrownBy(() ->  {
            htmlToPdfConverter.convertWithUnescape(HTML, PdfLayout.LABEL);
        }).hasMessage("Error interacting with converter");
    }
}
