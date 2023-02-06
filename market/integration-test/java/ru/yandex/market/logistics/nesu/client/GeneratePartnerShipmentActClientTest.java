package ru.yandex.market.logistics.nesu.client;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Генерация АПП в клиенте")
public class GeneratePartnerShipmentActClientTest extends AbstractClientTest {

    @MethodSource
    @DisplayName("Успех")
    @ParameterizedTest(name = "Успех " + "{0}")
    void success(@SuppressWarnings("unused") String name, String url, Function<NesuClient, byte[]> generatedAct) {
        byte[] content = {1, 2, 3};
        mock.expect(requestTo(startsWith(uri + url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content)
            );

        softly.assertThat(generatedAct.apply(client)).isEqualTo(content);
    }

    @MethodSource
    @DisplayName("Успех АПП отгрузки, список shopId")
    @ParameterizedTest(name = "Успех " + "{0}")
    void successWithShopIds(
        @SuppressWarnings("unused") String name,
        String url,
        Function<NesuClient, byte[]> generatedAct
    ) {
        byte[] content = {1, 2, 3};
        mock.expect(requestTo(startsWith(uri + url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopIds", String.valueOf(100L), String.valueOf(200L), String.valueOf(300L)))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content)
            );

        softly.assertThat(generatedAct.apply(client)).isEqualTo(content);
    }

    @Nonnull
    private static Stream<Arguments> successWithShopIds() {
        return Stream.of(
            Arguments.of(
                "АПП отгрузки",
                "/internal/partner/shipments/1000/act",
                (Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateAct(
                    100,
                    Set.of(100L, 200L, 300L),
                    1000
                )
            ),
            Arguments.of(
                "фактический АПП",
                "/internal/partner/shipments/1000/inbound/act",
                (Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateInboundAct(
                    100,
                    Set.of(100L, 200L, 300L),
                    1000
                )
            )
        );
    }

    @Test
    @DisplayName("Bad Request")
    void error() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/1000/act")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> client.generateAct(100, Set.of(), 1000)
        );
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of(
                "АПП отгрузки",
                "/internal/partner/shipments/1000/act",
                (Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateAct(100, 500, 1000)
            ),
            Arguments.of(
                "фактический АПП",
                "/internal/partner/shipments/1000/inbound/act",
                (Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateInboundAct(100, 500, 1000)
            )
        );
    }
}
