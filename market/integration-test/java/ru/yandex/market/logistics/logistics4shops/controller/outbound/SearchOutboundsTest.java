package ru.yandex.market.logistics.logistics4shops.controller.outbound;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.MdsFilePath;
import ru.yandex.market.logistics.logistics4shops.client.api.model.Outbound;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OutboundsListDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OutboundsSearchRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Поиск отправок")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/outbound/search/before/outbounds_search.xml")
class SearchOutboundsTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Поиск отправок по фильтру, все записи есть в базе")
    void searchOutbounds() {
        var searchOutboundsResponse = apiOperation(buildRequest(Set.of("1001", "1002", "1003")))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(searchOutboundsResponse)
            .isEqualTo(buildResponse(List.of(
                buildOutbound(
                    1,
                    "1001",
                    "2001",
                    List.of("100101"),
                    true,
                    new MdsFilePath().bucket("bucket").filename("filename")
                ),
                buildOutbound(2, "1002", "2002", List.of("100102")),
                buildOutbound(3, "1003", "2003", null)
            )));
    }

    @Test
    @MethodSource
    @DisplayName("Поиск отправок по фильтру, части записей нет в базе")
    void searchOutboundsWithFallback() {
        var searchOutboundsResponse = apiOperation(buildRequest(Set.of("1002", "1004")))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(searchOutboundsResponse).isEqualTo(
            buildResponse(List.of(buildOutbound(2, "1002", "2002", List.of("100102"))))
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Вернуть BAD_REQUEST, если некорректный фильтр")
    void exceptionOnIncorrectFilter(
        @SuppressWarnings("unused") String name,
        OutboundsSearchRequest outboundsSearchRequest,
        String message
    ) {
        ValidationError error = apiOperation(outboundsSearchRequest)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .containsExactly(new ValidationViolation().field("yandexIds").message(message));
    }

    @Test
    @DisplayName("Вернуть BAD_REQUEST, если фильтр содержит null в yandexIds")
    void exceptionOnNullYandexId() {
        var error = apiOperation(buildRequest(Collections.singleton(null)))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);
        softly.assertThat(error).isEqualTo(
            new ApiError().message(
                "Invalid outbound request. Violations: [Field 'yandexIds' is invalid: must not contain nulls]"
            )
        );
    }

    @Nonnull
    private OutboundApi.SearchOutboundsOper apiOperation(OutboundsSearchRequest request) {
        return apiClient.outbound().searchOutbounds().body(request);
    }

    @Nonnull
    static Stream<Arguments> exceptionOnIncorrectFilter() {
        return Stream.of(
            Arguments.of(
                "Пустое множество",
                buildRequest(Set.of()),
                "size must be between 1 and 2147483647"
            ),
            Arguments.of(
                "yandexIds не указан",
                buildRequest(null),
                "must not be null"
            )
        );
    }

    @Nonnull
    private static OutboundsSearchRequest buildRequest(@Nullable Set<String> yandexIds) {
        return new OutboundsSearchRequest().yandexIds(yandexIds);
    }

    @Nonnull
    private static Outbound buildOutbound(
        long id,
        String yandexId,
        String externalId,
        @Nullable List<String> orderIds
    ) {
        return buildOutbound(id, yandexId, externalId, orderIds, false, null);
    }

    @Nonnull
    private static Outbound buildOutbound(
        long id,
        String yandexId,
        String externalId,
        @Nullable List<String> orderIds,
        boolean discrepancyActIsReady,
        @Nullable MdsFilePath discrepancyActPath
    ) {
        return new Outbound()
            .id(id)
            .yandexId(yandexId)
            .externalId(externalId)
            .created(Instant.parse("2022-02-21T10:30:00.00Z"))
            .intervalFrom(Instant.parse("2022-02-21T11:30:00.00Z"))
            .intervalTo(Instant.parse("2022-02-21T13:30:00.00Z"))
            .orderIds(orderIds)
            .discrepancyActIsReady(discrepancyActIsReady)
            .discrepancyActPath(discrepancyActPath);
    }

    @Nonnull
    private static OutboundsListDto buildResponse(List<Outbound> outbounds) {
        return new OutboundsListDto().outbounds(outbounds);
    }
}
