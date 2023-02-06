package ru.yandex.market.logistics.tarifficator.admin.tpl.couriertariffzone;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.tpl.CourierTariffZoneDetailDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/controller/admin/tpl/couriertariffs/zones/db/before/search_prepare.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class UpdateCourierTariffZoneTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Обновление тарифной зоны — поля неизменны")
    @ExpectedDatabase(
        value = "/controller/admin/tpl/couriertariffs/zones/db/before/search_prepare.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffZoneNoUpdatedFields() throws Exception {
        updateTariffZone(
            defaultTariffZoneDetailDto(),
            "controller/admin/tpl/couriertariffs/zones/response/id_1_details.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифной зоны — изменение описания")
    @ExpectedDatabase(
        value = "/controller/admin/tpl/couriertariffs/zones/db/after/updated_description.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffZoneDescription() throws Exception {
        updateTariffZone(
            defaultTariffZoneDetailDto().setDescription("Описание обновлено"),
            "controller/admin/tpl/couriertariffs/zones/response/id_1_details_updated_description.json"
        );
    }

    void updateTariffZone(CourierTariffZoneDetailDto update, String path) throws Exception {
        mockMvc.perform(TestUtils.request(HttpMethod.PUT, "/admin/tpl-courier-tariffs/zones/1", update))
            .andExpect(status().isOk())
            .andExpect(jsonContent(path));
    }

    @Nonnull
    private CourierTariffZoneDetailDto defaultTariffZoneDetailDto() {
        return new CourierTariffZoneDetailDto()
            .setTitle("Тарифная зона № 1")
            .setId(1L)
            .setName("Москва")
            .setDescription("Москва и МО");
    }
}
