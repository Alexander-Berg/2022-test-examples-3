package ru.yandex.market.pricelabs.client.http;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.client.exception.BadRequestException;
import ru.yandex.market.pricelabs.client.http.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.client.http.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.client.http.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.client.http.model.AutostrategySave;
import ru.yandex.market.pricelabs.client.http.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.client.http.model.AutostrategySettings;
import ru.yandex.market.pricelabs.client.http.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.client.http.model.ProgramActivationRequest;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12233)
class PricelabsClientHttpTest extends AbstractPricelabsMockServerTest {

    private static final OffsetDateTime CREATED_TIME = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String AUTOSTRATEGY_NAME = "Автостратегия_%s";

    @Autowired
    private PricelabsApiClient pricelabsApiClient;

    PricelabsClientHttpTest(MockServerClient server) {
        super(server);
    }

    @Nonnull
    private static Stream<Arguments> getError() {
        return Stream.of(
                Arguments.of(400, BadRequestException.class, "Pricelabs response Bad Request error: Error"),
                Arguments.of(404, BadRequestException.class, "Pricelabs response Not Found error: Error"),
                Arguments.of(500, IllegalStateException.class, "Pricelabs response Internal Server Error error: " +
                        "Error")
        );
    }

    @Test
    @DisplayName("Проверяет, что создаются АС по их настройкам и в ответе есть ID созданных АС")
    void autostrategyBatchPost_create_success() {
        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/autostrategy/batch")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(loadFile("json/persisted_autostrategies.json"), StandardCharsets.UTF_8)
                );

        AutostrategySaveWithId strategy1 = createAutostrategySave(1);
        AutostrategySaveWithId strategy2 = createAutostrategySave(2);

        Assertions.assertThat(
                        pricelabsApiClient.createAutostrategies(1, List.of(strategy1, strategy2), "white", 400L, true)
                )
                .containsExactlyInAnyOrder(
                        createAutostrategyLoad(1),
                        createAutostrategyLoad(2)
                );
    }

    @DisplayName("Проверяет, что если партнеры не заданы, то упадет")
    @MethodSource("getError")
    @ParameterizedTest(name = "{0}")
    void autostrategyBatchPost_incorrectResponse_exception(int code, Class<?> clazz, String message) {
        server.when(request()
                        .withMethod("POST")
                        .withPath("/autostrategy/batch"))
                .respond(response()
                        .withStatusCode(code)
                        .withBody("Error"));

        Assertions.assertThatThrownBy(() -> pricelabsApiClient.createAutostrategies(1, List.of(), "white", null, null))
                .isInstanceOf(clazz)
                .hasMessage(message);
    }

    @Test
    @DisplayName("Проверяет, что отправляются заявки на активацию программ")
    void programsActivationPost_send_success() {
        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/programs/activation")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"status\":\"OK\"}")
                );

        ProgramActivationRequest program1 = createProgramRequest(1);
        ProgramActivationRequest program2 = createProgramRequest(2);
        Assertions.assertThat(pricelabsApiClient.enqueueProgram(List.of(program1, program2)))
                .isNotNull()
                .extracting("status")
                .isEqualTo("OK");
    }

    @Nonnull
    private ProgramActivationRequest createProgramRequest(int shopId) {
        ProgramActivationRequest request = new ProgramActivationRequest();
        request.setProgramType("NEWBIE");
        request.setShopId(shopId);
        request.setProgramId(1L);
        return request;
    }

    @Nonnull
    private AutostrategyLoad createAutostrategyLoad(int id) {
        return new AutostrategyLoad()
                .name(String.format(AUTOSTRATEGY_NAME, id))
                .createdAt(CREATED_TIME)
                .enabled(true)
                .recommendationType(AutostrategyLoad.RecommendationTypeEnum.MAXIMUM)
                .filter(
                        new AutostrategyFilter()
                                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                                .simple(new AutostrategyFilterSimple().offerIds(List.of("1", "2")))
                )
                .settings(
                        new AutostrategySettings()
                                .type(AutostrategySettings.TypeEnum.CPA)
                                .cpa(new AutostrategySettingsCPA().drrBid(100L)))
                .id(id);
    }

    private AutostrategySaveWithId createAutostrategySave(int id) {
        return new AutostrategySaveWithId()
                .autostrategy(
                        new AutostrategySave()
                                .name(String.format(AUTOSTRATEGY_NAME, id))
                                .createdAt(CREATED_TIME)
                                .enabled(true)
                                .recommendationType(AutostrategySave.RecommendationTypeEnum.MAXIMUM)
                                .filter(
                                        new AutostrategyFilter()
                                                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                                                .simple(new AutostrategyFilterSimple().offerIds(List.of("1", "2")))
                                )
                                .settings(
                                        new AutostrategySettings()
                                                .type(AutostrategySettings.TypeEnum.CPA)
                                                .cpa(new AutostrategySettingsCPA().drrBid(100L))
                                )
                );
    }
}
