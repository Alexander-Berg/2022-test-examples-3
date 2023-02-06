package ru.yandex.market.logistics.tarifficator.admin.pricelistfile;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.base.AbstractPriceListFileActivationTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Активация файла прайс-листа через грид в админке")
class ActivatePriceListFileFromGridTest extends AbstractPriceListFileActivationTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource
    @DisplayName("Попытка запланировать активацию с невалидным запросом")
    void invalidActivation(@SuppressWarnings("unused") String name, String body, String error) throws Exception {
        mockMvc.perform(
                post("/admin/price-list-files/from-grid/activate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(error));
    }

    @Nonnull
    private static Stream<Arguments> invalidActivation() {
        return Stream.of(
            Arguments.of(
                "Нет идентификатора файла",
                "{\"ids\":[{}]}",
                "{\"message\": \"Request does not contain file id\"}}"
            ),
            Arguments.of("Пустой запрос", "{}", "{\"message\": \"Request mush contain exactly one id\"}}"),
            Arguments.of(
                "Пустой массив идентификаторов",
                "{\"ids\":[]}",
                "{\"message\": \"Request mush contain exactly one id\"}}"
            ),
            Arguments.of(
                "Больше одного идентификатора",
                "{\"ids\":[{\"priceListFileId\":1},{\"priceListFileId\":2}]}",
                "{\"message\": \"Request mush contain exactly one id\"}}"
            )
        );
    }

    @Nonnull
    @Override
    protected ResultActions performPriceListFileActivation() throws Exception {
        return mockMvc.perform(
            post("/admin/price-list-files/from-grid/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[{\"priceListFileId\":1}]}")
        );
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.noContent();
    }
}
