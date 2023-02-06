package ru.yandex.market.logistics.lms.client.controller.yt;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение актуальной версии данных в yt")
public class LmsLomYtControllerGetActualVersionTest extends LmsLomYtControllerAbstractTest {

    private static final String GET_VERSION_PATH = "/lms/test-yt/actual-version";

    @Override
    @BeforeEach
    void setUp() {
        doReturn(ytTables)
            .when(hahnYt).tables();
    }

    @Override
    @AfterEach
    void tearDown() {
        verify(hahnYt).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);

        verifyNoMoreInteractions(ytTables, hahnYt);
    }

    @Test
    @SneakyThrows
    @DisplayName("В yt нет актуальной версии")
    void noVersionInRedis() {
        softly.assertThatCode(this::getActualVersion)
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.NullPointerException: text");
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение актуальной версии")
    void successGetVersion() {
        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);

        getActualVersion()
            .andExpect(status().isOk())
            .andExpect(content().string("\"" + YT_ACTUAL_VERSION + "\""));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка парсинга значения актуальной версии")
    void parsingError() {
        String version = "wrong version format";
        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, version);

        softly.assertThatCode(this::getActualVersion)
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.time.format.DateTimeParseException: "
                    + "Text 'wrong version format' could not be parsed at index 0"
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка обращения к yt")
    void redisError() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            String.format("version FROM [%s] ORDER BY created_at DESC LIMIT 1", lmsYtProperties.getVersionPath()),
            new RuntimeException("some yt exception")
        );

        softly.assertThatCode(this::getActualVersion)
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: some yt exception"
            );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getActualVersion() {
        return mockMvc.perform(get(GET_VERSION_PATH));
    }
}
