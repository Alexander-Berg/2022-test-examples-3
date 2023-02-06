package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.filter.RegistrySearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение реестров")
@DatabaseSetup("/controller/admin/registries/prepare.xml")
class GetRegistriesTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск реестров")
    void search(String displayName, RegistrySearchFilter filter, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/registries").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Triple.of(
                "Пустой фильтр",
                new RegistrySearchFilter(),
                "controller/admin/registries/all.json"
            ),
            Triple.of(
                "По идентификатору отгрузки",
                new RegistrySearchFilter().setShipmentId(1L),
                "controller/admin/registries/id_1.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Получение деталей реестра")
    void getOne() throws Exception {
        mockMvc.perform(get("/admin/registries/200"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/registries/id_2.json"));
    }

    @Test
    @DisplayName("Реестр не найден")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/registries/3"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [REGISTRY] with id [3]"));
    }
}
