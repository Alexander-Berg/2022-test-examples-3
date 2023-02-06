package ru.yandex.market.adv.b2bmonetization.client.http;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.client.exception.BadRequestException;
import ru.yandex.market.adv.b2bmonetization.client.http.model.BonusPartnerResponse;
import ru.yandex.market.adv.b2bmonetization.client.http.model.BonusPartnerResponseBatch;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12233)
public class MonetizationClientHttpTest extends AbstractMonetizationMockServerTest {

    @Autowired
    private MonetizationApiClient monetizationApiClient;

    public MonetizationClientHttpTest(MockServerClient server) {
        super(server);
    }

    @NotNull
    private static Stream<Arguments> getError() {
        return Stream.of(
                Arguments.of(400, BadRequestException.class, "Monetization response Bad Request error: Error"),
                Arguments.of(404, BadRequestException.class, "Monetization response Not Found error: Error"),
                Arguments.of(500, IllegalStateException.class, "Monetization response Internal Server Error error: " +
                        "Error")
        );
    }

    @Test
    @DisplayName("Проверяет, что для заданных партнеров, есть информация с бонусами")
    void getBonusInfo_exist_success() {
        initMock("GET",
                "/v1/bonus/info",
                null,
                "getBonusInfo_exist_success",
                Map.of("partner_ids", List.of("1", "2"))
        );

        Assertions.assertThat(monetizationApiClient.getBonusInfo(List.of(1L, 2L), null))
                .containsExactlyInAnyOrder(
                        createBonusInfo(1),
                        createBonusInfo(2)
                );
    }

    @DisplayName("Проверяет, что если партнеры не заданы, то упадет")
    @MethodSource("getError")
    @ParameterizedTest(name = "{0}")
    void getBonusInfo_incorrectResponse_exception(int code, Class<?> clazz, String message) {
        server.when(request()
                        .withMethod("GET")
                        .withPath("/v1/bonus/info")
                        .withQueryStringParameter("partner_ids", "1", "2")
                        .withQueryStringParameter("date", "2020-01-10"))
                .respond(response()
                        .withStatusCode(code)
                        .withBody("Error"));

        Assertions.assertThatThrownBy(
                        () -> monetizationApiClient.getBonusInfo(
                                List.of(1L, 2L),
                                LocalDate.of(2020, 1, 10)
                        )
                )
                .isInstanceOf(clazz)
                .hasMessage(message);
    }

    private BonusPartnerResponseBatch createBonusInfo(long partnerId) {
        return BonusPartnerResponseBatch.builder()
                .partnerId(partnerId)
                .bonusesList(
                        List.of(
                                BonusPartnerResponse.builder()
                                        .bonusId(partnerId)
                                        .partnerId(partnerId)
                                        .bonusProgram("NEWBIE")
                                        .bonusSum(1000)
                                        .bonusEnabled(true)
                                        .bonusStartAt(LocalDate.of(2022, 1, 10))
                                        .bonusExpiredAt(LocalDate.of(2022, 1, 15))
                                        .build()
                        )
                )
                .build();
    }
}
