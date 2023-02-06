package ru.yandex.market.logistics.tarifficator.admin.tariff;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.TariffDetailDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Форсированная генерация ревизии через админку")
public class GenerateRevisionTariffTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешная генерация ревизии тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-errors.xml")
    @DatabaseSetup(
        value = "/controller/price-list-files/db/before/price_lists_are_activated.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/price-list-generate-revision-success.xml",
        assertionMode = NON_STRICT
    )
    void generateRevisionSuccessful() throws Exception {

        performRevisionGeneration(makeDetailDto(1L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Генерация ревизии для несуществующего тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-errors.xml")
    void generateRevisionNotFound() throws Exception {

        performRevisionGeneration(makeDetailDto(3L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[3]]"));
    }

    @Test
    @DisplayName("Генерация ревизии для неактивного тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-errors.xml")
    @DatabaseSetup(
        value = "/controller/admin/tariffs/before/disable_tariff.xml",
        type = DatabaseOperation.REFRESH
    )
    void generateRevisionDisabled() throws Exception {

        performRevisionGeneration(makeDetailDto(1L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Can't generate revision for disabled tariff with id 1."));
    }

    @Nonnull
    private TariffDetailDto makeDetailDto(long tariffId) {
        return (TariffDetailDto) new TariffDetailDto()
            .setCode("Код тарифа (не проверяется в операции)")
            .setTariffId(tariffId);
    }

    @Nonnull
    private ResultActions performRevisionGeneration(TariffDetailDto detailDto) throws Exception {

        return mockMvc.perform(
            TestUtils.request(HttpMethod.POST,
                "/admin/tariffs/generateRevision/",
                detailDto)
        );
    }
}
