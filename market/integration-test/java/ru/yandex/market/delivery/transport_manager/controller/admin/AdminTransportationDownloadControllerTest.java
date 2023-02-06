package ru.yandex.market.delivery.transport_manager.controller.admin;

import java.util.Arrays;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class AdminTransportationDownloadControllerTest extends AbstractContextualTest {

    private static final String CONTROLLER = "/admin/transportations/download/transportations-file-csv";

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Контроллер скачивания перемещений из админки")
    @MethodSource("provideArgs")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void csvIsDownloaded(String name, LinkedMultiValueMap<String, String> args, String file) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER).params(args))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent(file)));
    }

    static Stream<Arguments> provideArgs() {
        return Stream.of(
            Arguments.of(
                "Без фильтра (но не более download.transportations-max-rows)",
                toParams("", ""),
                "controller/admin/transportation/all.csv"
            ),
            Arguments.of(
                "С фильтром",
                toParams("adminTransportationStatus", "SCHEDULED"),
                "controller/admin/transportation/scheduled.csv"
            )
        );
    }

    private static LinkedMultiValueMap<String, String> toParams(String key, String... values) {
        var params = new LinkedMultiValueMap<String, String>();
        params.put(key, Arrays.asList(values));
        return params;
    }
}
