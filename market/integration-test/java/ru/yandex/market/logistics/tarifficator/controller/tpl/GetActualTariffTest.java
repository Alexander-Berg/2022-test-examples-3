package ru.yandex.market.logistics.tarifficator.controller.tpl;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = {
        "/controller/tpl/tariffs/db/before/search_prepare_mds_files.xml",
        "/controller/tpl/tariffs/db/before/search_prepare_tariffs.xml",
        "/controller/tpl/tariffs/db/before/search_prepare_tariffs_options.xml",
    },
    connection = "dbUnitQualifiedDatabaseConnection"
)
public class GetActualTariffTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Единственный активный тариф на заданную дату")
    void singleActiveTariffFound() throws Exception {
        performGetActualTariff("2020-01-01")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/tpl/tariffs/response/actual_tariff_id_1.json", NON_EXTENSIBLE));
    }

    @Test
    @DisplayName("Дата окончания действия тарифа включается во время активности тарифа")
    void toDateIsIncluded() throws Exception {
        performGetActualTariff("2020-01-31")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/tpl/tariffs/response/actual_tariff_id_1.json", NON_EXTENSIBLE));
    }

    @Test
    @DisplayName("На требуемую дату есть несколько тарифов, среди которых лишь один активный")
    void multipleTariffsFoundSingleTariffIsActive() throws Exception {
        performGetActualTariff("2020-02-01")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/tpl/tariffs/response/actual_tariff_id_2.json"));
    }

    @Test
    @DisplayName("На требуемую дату есть несколько тарифов, среди которых несколько активных")
    void multipleTariffsFoundMultipleTariffsAreActive() throws Exception {
        performGetActualTariff("2020-03-01")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/tpl/tariffs/response/actual_tariff_id_5.json"));
    }

    @Test
    @DisplayName("На требуемую дату есть один тариф, не являющийся активным")
    void singleNotActiveTariff() throws Exception {
        performGetActualTariff("2020-04-01")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find active [COURIER_TARIFF] for date 2020-04-01"));
    }

    @Test
    @DisplayName("На требуемую дату тарифов нет")
    void noTariffsFound() throws Exception {
        performGetActualTariff("2020-05-01")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find active [COURIER_TARIFF] for date 2020-05-01"));
    }

    @Nonnull
    private ResultActions performGetActualTariff(String dateAsString) throws Exception {
        return mockMvc.perform(get("/tpl-courier-tariff/actual").param("date", dateAsString));
    }
}
