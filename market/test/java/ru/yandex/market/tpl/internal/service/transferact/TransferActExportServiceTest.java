package ru.yandex.market.tpl.internal.service.transferact;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.transferact.client.api.DocumentApi;
import ru.yandex.market.tpl.common.util.exception.TplException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferActExportServiceTest {

    @InjectMocks
    private TransferActExportService transferActExportService;
    @Mock
    private DocumentApi documentApi;
    private final String orderExternalId = "test";

    @Test
    @SneakyThrows
    void export() {
        String content = "test-content";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ByteArrayResource zip = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
        when(documentApi.documentGetWithHttpInfo(orderExternalId, null))
                .thenReturn(ResponseEntity.ok(zip));

        transferActExportService.export(outputStream, orderExternalId);

        String result = outputStream.toString(Charset.defaultCharset());
        assertThat(result).isEqualTo(content);
    }

    @Test
    void export_notFound() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        when(documentApi.documentGetWithHttpInfo(orderExternalId, null))
                .thenReturn(ResponseEntity.notFound().build());

        assertThrows(
                TplEntityNotFoundException.class,
                () -> transferActExportService.export(outputStream, orderExternalId)
        );
    }

    @Test
    void export_internalError() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        when(documentApi.documentGetWithHttpInfo(orderExternalId, null))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        assertThrows(
                TplException.class,
                () -> transferActExportService.export(outputStream, orderExternalId)
        );
    }

}
