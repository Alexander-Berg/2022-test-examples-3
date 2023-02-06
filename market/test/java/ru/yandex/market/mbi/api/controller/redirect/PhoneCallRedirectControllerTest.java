package ru.yandex.market.mbi.api.controller.redirect;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectResponse;
import ru.yandex.market.communication.proxy.client.model.LightCreateRedirectRequest;
import ru.yandex.market.communication.proxy.client.model.RedirectInfoResponse;
import ru.yandex.market.communication.proxy.client.model.RedirectType;
import ru.yandex.market.communication.proxy.exception.CommunicationProxyClientException;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class PhoneCallRedirectControllerTest extends FunctionalTest {

    @Autowired
    private CommunicationProxyClient communicationProxyClient;

    @Test
    void testActiveRedirectNotFound() {
        String proxyNumber = "+79011231212";

        when(communicationProxyClient.getRedirect(proxyNumber))
                .thenThrow(new CommunicationProxyClientException("Not found", HttpStatus.SC_NOT_FOUND));

        assertThatExceptionOfType(MbiOpenApiClientResponseException.class)
                .isThrownBy(() -> getMbiOpenApiClient().getActiveRedirect(proxyNumber))
                .satisfies(e -> assertThat(e.getHttpErrorCode()).isEqualTo(HttpStatus.SC_NOT_FOUND));
    }

    @Test
    void testActiveRedirectSuccess() {
        Long orderId = 1L;
        String proxyNumber = "+79011231212";
        String actualNumber = "+79213453445";

        when(communicationProxyClient.getRedirect(proxyNumber))
                .thenReturn(new RedirectInfoResponse()
                        .orderId(orderId)
                        .redirectType(RedirectType.DBS_ORDER)
                        .sourceNumber(proxyNumber)
                        .targetNumber(actualNumber)
                );

        assertThat(getMbiOpenApiClient().getActiveRedirect(proxyNumber))
                .usingRecursiveComparison()
                .isEqualTo(new ru.yandex.market.mbi.open.api.client.model.RedirectInfoResponse()
                        .sourcePhoneNumber(proxyNumber)
                        .targetPhoneNumber(actualNumber)
                        .keyType(ru.yandex.market.mbi.open.api.client.model.RedirectInfoResponse.KeyTypeEnum.ORDER_ID)
                        .keyValue(orderId)
                );
    }

    @Test
    void testLightRedirectCreation() {
        Long orderId = 1L;
        Long partnerId = 2L;
        String proxyNumber = "+79011231212";
        String actualNumber = "+79213453445";

        when(communicationProxyClient.lightCreateRedirect(new LightCreateRedirectRequest()
                .orderId(orderId)
                .sourcePhoneNumber(actualNumber)
                .partnerId(partnerId)))
                .thenReturn(new CreateRedirectResponse().proxyNumber(proxyNumber));

        assertThat(getMbiOpenApiClient().getProxyNumber(
                new ru.yandex.market.mbi.open.api.client.model.ProxyNumberRequest()
                        .orderId(orderId)
                        .customerPhoneNumber(actualNumber)
                        .partnerId(partnerId)))
                .usingRecursiveComparison()
                .isEqualTo(new ru.yandex.market.mbi.open.api.client.model.ProxyNumberResponse()
                        .phoneNumber(proxyNumber));
    }
}
