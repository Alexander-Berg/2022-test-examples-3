package ru.yandex.market.bank;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.bank.model.BankInfoCbrfResponseDTO;
import ru.yandex.market.bank.model.BankInfoDTO;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class BankInfoClientTest extends FunctionalTest {
    private static final String QUERY = "some_query";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private RestTemplate bankInfoClientRestTemplate;

    @BeforeEach
    void init() {
        bankInfoClientRestTemplate = Mockito.mock(RestTemplate.class);
    }

    @Test
    @DisplayName("При постраничной выдаче ручки учитываются все страницы")
    void testMultiPageDownload() {
        final int size1 = 12;
        final String uri1 = "multi_page_uri1";

        final int size2 = 16;
        final String uri2 = "multi_page_uri2";

        final int size3 = 11;
        final String uri3 = "multi_page_uri3";

        mockChainedResponse(size1, uri1, uri2);
        mockChainedResponse(size2, uri2, uri3);
        mockChainedResponse(size3, uri3, null);

        final int expectedSize = size1 + size2 + size3;
        checkResult(uri1, expectedSize);
    }

    @Test
    @DisplayName("Обработка возврата с ручки единственной страницей, отсутствует хедер Link")
    void testSinglePageDownloadWithNoLinkHeader() {
        final String uri = "single_page_uri";
        final int expectedSize = 10;

        final ResponseEntity<BankInfoCbrfResponseDTO> mockedResponse = generateMockedResponse(expectedSize, null);

        Mockito.when(bankInfoClientRestTemplate.postForEntity(eq(uri), any(), eq(BankInfoCbrfResponseDTO.class)))
                .thenReturn(mockedResponse);
        checkResult(uri, expectedSize);
    }

    @Test
    @DisplayName("Обработка возврата с ручки единственной страницей, есть хедер Link")
    void testSinglePageDownloadWithPresentLinkHeader() {
        final String uri = "single_page_uri";
        final int expectedSize = 15;

        mockChainedResponse(expectedSize, uri, null);
        checkResult(uri, expectedSize);
    }

    @Test
    @DisplayName("Негативный тест для RetryTemplate")
    void testRetryTemplate() {
        final String uri = "single_page_uri";
        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(
                MAX_RETRY_ATTEMPTS,
                ImmutableMap.of(RuntimeException.class, true)
        ));

        Mockito.when(bankInfoClientRestTemplate.postForEntity(eq(uri), any(), eq(BankInfoCbrfResponseDTO.class)))
                .thenThrow(new RuntimeException());

        final BankInfoClient client = new BankInfoClient(bankInfoClientRestTemplate, uri, QUERY, retryTemplate);

        Assertions.assertThrows(RuntimeException.class, client::downloadBankInfo);
        Mockito.verify(bankInfoClientRestTemplate, Mockito.times(MAX_RETRY_ATTEMPTS))
                .postForEntity(eq(uri), any(), eq(BankInfoCbrfResponseDTO.class));
    }

    private String generateLinkHeader(final String uri) {
        return uri != null ? String.format("<%s>; rel=\"next\"", uri) : "";
    }

    private List<BankInfoDTO> generateListing(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }

        // для каждой DTO-шки генерится уникальный БИК
        // (на случай, если, например, будет решено возвращать Set вместо List)
        int counter = 123_456_789;

        List<BankInfoDTO> result = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            result.add(new BankInfoDTO(
                    "" + counter++,
                    "name",
                    "place",
                    false,
                    false
            ));
        }

        return result;
    }

    private void mockChainedResponse(final int size,
                                     final String currentUri,
                                     @Nullable final String nextUri) {
        MultiValueMap<String, String> rawHeaders = new LinkedMultiValueMap<>();
        rawHeaders.add("Link", generateLinkHeader(nextUri));
        final ResponseEntity<BankInfoCbrfResponseDTO> response = generateMockedResponse(size, rawHeaders);
        Mockito.when(bankInfoClientRestTemplate.postForEntity(eq(currentUri), any(), eq(BankInfoCbrfResponseDTO.class)))
                .thenReturn(response);
    }

    private ResponseEntity<BankInfoCbrfResponseDTO> generateMockedResponse(final int responseSize,
                                                                           final MultiValueMap<String, String> headers) {
        final BankInfoCbrfResponseDTO mockedResponse =
                new BankInfoCbrfResponseDTO(new BankInfoCbrfResponseDTO.Data(generateListing(responseSize)));

        return new ResponseEntity<>(mockedResponse, headers, HttpStatus.OK);
    }

    private void checkResult(final String startUri, final int expectedSize) {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(1);
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        final BankInfoClient client = new BankInfoClient(bankInfoClientRestTemplate, startUri, QUERY, template);
        Assertions.assertEquals(expectedSize, client.downloadBankInfo().size());
    }
}
