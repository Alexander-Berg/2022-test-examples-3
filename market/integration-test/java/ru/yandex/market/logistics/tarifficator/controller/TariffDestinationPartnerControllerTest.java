package ru.yandex.market.logistics.tarifficator.controller;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест контроллера TariffDestinationController")
class TariffDestinationPartnerControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Добавление новой службе ПВЗ")
    @DatabaseSetup("/controller/tariffs/db/before/tariff_destination_partners.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/add_new_tariff_destination_partner.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void notifyNewPartnerIdActive() throws Exception {
        addTariffDestinationPartner();
    }

    @Test
    @DisplayName("Добавление недостающей связки для ПВЗ")
    @DatabaseSetup("/controller/tariffs/db/before/tariff_destination_partners.xml")
    @DatabaseSetup(
        value = "/controller/tariffs/db/before/tariff_destination_partners_single.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/add_missing_tariff_destination_partner.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void shouldAddOneMissingTariffDestinationPartner() throws Exception {
        addTariffDestinationPartner();
    }

    @Test
    @DisplayName("Добавлять в очередь задачу по генерации поколений только для включенных тарифов")
    @DatabaseSetup("/controller/tariffs/db/before/with_disabled_tariffs.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/add_task_only_with_enabled_tariffs.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void shouldAddTaskOnlyForEnabledTariffs() throws Exception {
        addTariffDestinationPartner();
    }

    @Test
    @DisplayName("Добавлять в очередь несколько задач по генерации поколений если тарифов много")
    @DatabaseSetup("/controller/tariffs/db/before/many_tariffs.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/add_several_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void shouldAddSeveralTasksForManyTariffsInBatch() throws Exception {
        addTariffDestinationPartner();
    }

    @Test
    @DisplayName("Добавление уже существующей службе ПВЗ")
    @DatabaseSetup("/controller/tariffs/db/before/tariff_destination_partners_exist_active.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/before/tariff_destination_partners_exist_active.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/empty_dbqueue.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void notifyExistingPartnerIdAlreadyActive() throws Exception {
        addTariffDestinationPartner();
    }

    private void addTariffDestinationPartner() throws Exception {
        mockMvc.perform(post("/tariffs/destinationPartners/4"))
            .andExpect(status().isOk());
    }
}
