package ru.yandex.market.ff4shops.api.json.outbound;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.api.model.SearchOutboundsFilter;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.jsonHeaders;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.putForEntity;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public abstract class AbstractOutboundsTest extends AbstractJsonControllerFunctionalTest {
    @DbUnitDataSet
    @MethodSource
    @ParameterizedTest(name = "[{index}] {0} {1}")
    @DisplayName("Валидация параметров")
    void validateGetDtoArguments(String field, String message, List<String> yandexIds) {
        HttpClientErrorException.BadRequest exception = catchThrowableOfType(
            () -> perform(yandexIds),
            HttpClientErrorException.BadRequest.class
        );

        assertThat(exception).hasMessage("400 Bad Request");
        assertThat(exception.getResponseBodyAsString())
            .contains("Following validation errors occurred:")
            .contains(String.format("Field: '%s', message: '%s'", field, message));
    }

    @Nonnull
    private static Stream<Arguments> validateGetDtoArguments() {
        return Stream.of(
            Arguments.of(
                "outboundYandexIds",
                "must not be empty",
                List.of()
            ),
            Arguments.of(
                "outboundYandexIds[0]",
                "must not be blank",
                List.of("", "   ")
            )
        );
    }

    @Nonnull
    public abstract String getUrl();

    private void perform(List<String> yandexIds) throws JsonProcessingException {
        FunctionalTestHelper.putForEntity(
            getUrl(),
            MAPPER.writeValueAsString(new SearchOutboundsFilter().setOutboundYandexIds(yandexIds)),
            FunctionalTestHelper.jsonHeaders()
        );
    }

    public void makeCallAndCheckResponse(String responsePath) {
        assertResponseBody(
            putForEntity(
                getUrl(),
                extractFileContent("ru/yandex/market/ff4shops/api/json/outbound/outboundFilter.json"),
                jsonHeaders()
            ).getBody(),
            responsePath
        );
    }
}
