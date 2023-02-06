package ru.yandex.market.sc.core.external.transfermanager;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.sc.core.external.transfermanager.dto.TmOperationResponse;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmTableRequest;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmUploadRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TransferManagerClientTest {
    private final String transferId = "transfer-id-1";
    private final String operationId = "operation-id-1";
    private final String myToken = "my token";
    private final String baseUrl = "http://cdc.n.yandex-team.ru";
    private final TmOperationResponse operationResponse = new TmOperationResponse(operationId, "s", Instant.now(),
            Instant.now(), "", true, null);

    private final RestTemplate restMock = mock(RestTemplate.class);
    private final TransferManagerClient tmClient = new TransferManagerClient(restMock, myToken, baseUrl);


    @BeforeEach
    void beforeEach() {
        String urlUpload = baseUrl + "/" + TransferManagerClient.POST_UPLOAD;
        when(restMock.patchForObject(eq(urlUpload), any(), eq(TmOperationResponse.class)))
                .thenReturn(operationResponse);

        String urlGet = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(TransferManagerClient.GET_OPERATION)
                .buildAndExpand(operationId)
                .toUriString();
        when(restMock.exchange(eq(urlGet), eq(HttpMethod.GET), any(), eq(TmOperationResponse.class)))
                .thenReturn(new ResponseEntity<TmOperationResponse>(operationResponse, HttpStatus.OK));
    }

    @Test
    void testUploadRequest() {
        String tableName = "order_scan_log";
        int archiveId = 10;
        String filter = MessageFormat.format("archive_id in ({0})", archiveId);

        //tested method
        TmOperationResponse upload = tmClient.createUpload(new TmUploadRequest(
                        List.of(
                                new TmTableRequest(
                                        filter,
                                        tableName,
                                        TransferManagerServiceImpl.DEFAULT_SCHEMA
                                )
                        ),
                        transferId
                )
        );

        //verification
        TmUploadRequest tmUploadRequest = new TmUploadRequest(
                List.of(new TmTableRequest(
                        filter,
                        tableName,
                        TransferManagerServiceImpl.DEFAULT_SCHEMA
                )),
                transferId
        );
        var httpEntity = new HttpEntity<>(
                tmUploadRequest,
                getHttpHeaders(myToken)
        );

        verify(restMock, times(1)).patchForObject(
                baseUrl + "/" + TransferManagerClient.POST_UPLOAD, httpEntity, TmOperationResponse.class);


        assertThat(upload).isNotNull();
        assertThat(upload.getId()).isEqualTo(operationResponse.getId());
        assertThat(upload.getDone()).isEqualTo(operationResponse.getDone());
        assertThat(upload.getError()).isEqualTo(operationResponse.getError());
        assertThat(upload.getDescription()).isEqualTo(operationResponse.getDescription());
        assertThat(upload.getCreatedBy()).isEqualTo(operationResponse.getCreatedBy());
        assertThat(upload.getCreatedAt()).isEqualTo(operationResponse.getCreatedAt());
        assertThat(upload.getModifiedAt()).isEqualTo(operationResponse.getModifiedAt());
    }

    @Test
    void testGetOperationRequest() {
        //tested method
        TmOperationResponse upload = tmClient.getOperation(operationId);

        var httpEntity = new HttpEntity<>(
                getHttpHeaders(myToken)
        );

        var url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(TransferManagerClient.GET_OPERATION)
                .buildAndExpand(operationId)
                .toUriString();

        verify(restMock, times(1)).exchange(url, HttpMethod.GET, httpEntity, TmOperationResponse.class);

        assertThat(upload).isNotNull();
        assertThat(upload.getId()).isEqualTo(operationResponse.getId());
        assertThat(upload.getDone()).isEqualTo(operationResponse.getDone());
        assertThat(upload.getError()).isEqualTo(operationResponse.getError());
        assertThat(upload.getDescription()).isEqualTo(operationResponse.getDescription());
        assertThat(upload.getCreatedBy()).isEqualTo(operationResponse.getCreatedBy());
        assertThat(upload.getCreatedAt()).isEqualTo(operationResponse.getCreatedAt());
        assertThat(upload.getModifiedAt()).isEqualTo(operationResponse.getModifiedAt());
    }

    private HttpHeaders getHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "OAuth " + token);

        return headers;
    }

}
