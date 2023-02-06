package ru.yandex.market.vendor.controllers.modelEdit;

import java.nio.charset.Charset;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

/**
 * Функциональные тесты на логику работы {@link FeedTemplateController}.
 *
 * @author fbokovikov
 */
@Disabled
public class FeedTemplateControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final long UID = 1000L;

    @Autowired
    private PartnerContentService partnerContentService;

    @Test
    @DisplayName("IR нашел файл по категории")
    void ok() {
        mockPartnerContentService(100);
        Assertions.assertDoesNotThrow(() -> getTemplate(100));
    }

    @Test
    @DisplayName("IR не сматчил категорию")
    void categoryNotFound() {
        Mockito.when(partnerContentService.getFileTemplate(ArgumentMatchers.argThat(arg -> arg.getCategoryId() == 50)))
                .thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.NOT_LEAF_CATEGORY)
                        .build());
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getTemplate(50)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName("IR упал с Internal error")
    void irInternalError() {
        Mockito.when(partnerContentService.getFileTemplate(ArgumentMatchers.argThat(arg -> arg.getCategoryId() == 500)))
                .thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.INTERNAL_ERROR)
                        .build());
        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> getTemplate(500)
        );
        Assertions.assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName("IR запросил GOOD CONTENT")
    void irTrueGoodContent() {
        mockPartnerContentService(100);
        ArgumentCaptor<PartnerContent.GetFileTemplateRequest> requestArg =
                ArgumentCaptor.forClass(PartnerContent.GetFileTemplateRequest.class);
        Assertions.assertDoesNotThrow(() -> getTemplate(100, true));
        Mockito.verify(partnerContentService).getFileTemplate(requestArg.capture());
        Assertions.assertSame(PartnerContent.FileContentType.GOOD_XLS, requestArg.getValue().getFileContentType());
    }

    @Test
    @DisplayName("IR запросил не GOOD CONTENT")
    void irFalseGoodContent() {
        mockPartnerContentService(50);
        ArgumentCaptor<PartnerContent.GetFileTemplateRequest> requestArg =
                ArgumentCaptor.forClass(PartnerContent.GetFileTemplateRequest.class);
        Assertions.assertDoesNotThrow(() -> getTemplate(50, false));
        Mockito.verify(partnerContentService).getFileTemplate(requestArg.capture());
        Assertions.assertSame(PartnerContent.FileContentType.BETTER_XLS, requestArg.getValue().getFileContentType());
    }

    @Test
    @DisplayName("IR без GOOD CONTENT")
    void irWithoutGoodContent() {
        mockPartnerContentService(150);
        ArgumentCaptor<PartnerContent.GetFileTemplateRequest> requestArg =
                ArgumentCaptor.forClass(PartnerContent.GetFileTemplateRequest.class);
        Assertions.assertDoesNotThrow(() -> getTemplate(150));
        Mockito.verify(partnerContentService).getFileTemplate(requestArg.capture());
        Assertions.assertSame(PartnerContent.FileContentType.BETTER_XLS, requestArg.getValue().getFileContentType());
    }

    private void mockPartnerContentService(int categoryId) {
        Mockito.when(partnerContentService.getFileTemplate(
                ArgumentMatchers.argThat(arg -> arg.getCategoryId() == categoryId))
        ).thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.OK)
                        .setContent(ByteString.copyFrom("qwerty", Charset.defaultCharset()))
                        .build());
    }

    private byte[] getTemplate(int categoryId) {
        String url = baseUrl + "/feeds/templates?categoryId={categoryId}&uid={uid}";
        return FunctionalTestHelper.get(url, byte[].class, categoryId, UID);
    }

    private byte[] getTemplate(int categoryId, boolean goodContent) {
        String url = baseUrl + "/feeds/templates?categoryId={categoryId}&uid={uid}&good_content={goodContent}";
        return FunctionalTestHelper.get(url, byte[].class, categoryId, UID, goodContent);
    }
}
