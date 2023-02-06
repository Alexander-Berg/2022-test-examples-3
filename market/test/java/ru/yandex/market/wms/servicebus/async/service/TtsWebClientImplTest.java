package ru.yandex.market.wms.servicebus.async.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import ru.yandex.market.wms.common.spring.dto.IndirectActivityDto;
import ru.yandex.market.wms.common.spring.tvm.TvmTicketProviderStub;
import ru.yandex.market.wms.servicebus.api.external.tts.TtsWebClient;
import ru.yandex.market.wms.servicebus.api.external.tts.client.TtsWebClientImpl;

class TtsWebClientImplTest {

    private final String warehouseName = "SOF";

    @Test
    void canStartIndirectActivity() {

        final ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);

        Mockito.when(exchangeFunction.exchange(Mockito.any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        ClientResponse.create(HttpStatus.NO_CONTENT)
                                .build()));

        final WebClient webClient = getWebClient(exchangeFunction);

        final TtsWebClient serviceAsync =
                new TtsWebClientImpl(webClient, warehouseName, new TvmTicketProviderStub());

        IndirectActivityDto dto = IndirectActivityDto.builder()
                .build();

        serviceAsync.startIndirectActivity(dto);

        final ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);

        Mockito.verify(exchangeFunction, Mockito.times(1)).exchange(captor.capture());

        final ClientRequest value = captor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertEquals("indirect-activity/SOF", value.url().getPath()),
                () -> Assertions.assertEquals(HttpMethod.POST, value.method())
        );
    }

    @Test
    void consumeIndirectActivityWhenException() {

        final ExchangeFunction exchangeFunction = (clientRequest) ->
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

        final WebClient webClient = getWebClient(exchangeFunction);

        final TtsWebClient serviceAsync =
                new TtsWebClientImpl(webClient, warehouseName, new TvmTicketProviderStub());

        IndirectActivityDto dto = IndirectActivityDto.builder()
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> serviceAsync.startIndirectActivity(dto));
    }

    private WebClient getWebClient(ExchangeFunction exchangeFunction) {
        return Mockito.spy(WebClient.builder()
                .exchangeFunction(exchangeFunction).build());
    }
}
