package ru.yandex.market.logistics.management.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.CargoType;
import ru.yandex.market.logistics.management.domain.entity.PartnerTariff;
import ru.yandex.market.logistics.management.domain.entity.TariffLocation;
import ru.yandex.market.logistics.management.repository.CargoTypeRepository;
import ru.yandex.market.logistics.management.repository.PartnerTariffRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanDatabase
@Sql("/data/controller/tariff/prepare_data.sql")
@SuppressWarnings({"checkstyle:MagicNumber"})
class TariffCargoTypeControllerTest extends AbstractContextualTest {

    @Autowired
    private PartnerTariffRepository partnerTariffRepository;

    @Autowired
    private CargoTypeRepository cargoTypeRepository;

    @Test
    void testCreateCargoTypeOk() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/cargo-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/tariff/cargo_type.json"))
        )
            .andExpect(status().isCreated());

        List<CargoType> cargoTypes = cargoTypeRepository.findAll();
        softly.assertThat(cargoTypes).as("One cargo type should be created").hasSize(1);
        softly.assertThat(cargoTypes).extracting(CargoType::getCargoType)
            .as("Cargo type 500 saved").containsOnly(500);
    }

    @Test
    void testCreateTariffOk() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partner/1/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/tariff/partner_tariff.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson("data/controller/tariff/partner_tariff.json"));

        List<PartnerTariff> partnerTariffs = partnerTariffRepository.findAll();
        softly.assertThat(partnerTariffs).as("One tariff should be created").hasSize(1);
        softly.assertThat(partnerTariffs).extracting(PartnerTariff::getTariffId)
            .as("Cargo type 500 saved").containsOnly(1110011L);
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql"
    })
    void testDeleteTariffOk() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/tariffs/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent());

        PartnerTariff tariff = partnerTariffRepository.findById(1L).orElse(null);
        softly.assertThat(tariff).as("Tariff should be deleted").isNull();
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql"
    })
    void testGetPartnersTariffs() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partner/1/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/tariff/partner_tariffs.json"));
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql"
    })
    void testGetTariff() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/tariffs/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/tariff/partner_tariff.json"));
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql",
        "/data/controller/tariff/cargo_type.sql"
    })
    void testAddRestrictionToTariff() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/tariffs/1/cargo-types/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk());

        PartnerTariff partnerTariff = getTariff();

        CargoType cargoType = partnerTariff.getCargoTypes().stream().findFirst()
            .orElse(null);
        softly.assertThat(cargoType)
            .as("Cargo type should exist").isNotNull()
            .extracting(CargoType::getCargoType)
            .as("Cargo type should have right type").isEqualTo(500);
    }

    private PartnerTariff getTariff() {
        PartnerTariff partnerTariff = partnerTariffRepository.findByTariffId(1110011L)
            .orElse(null);
        softly.assertThat(partnerTariff).as("Tariff should exist").isNotNull();
        return partnerTariff;
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql",
        "/data/controller/tariff/cargo_type.sql",
        "/data/controller/tariff/tariff_cargo_type.sql"
    })
    void testRemoveRestrictionFromTariff() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/tariffs/1/cargo-types/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent());

        PartnerTariff partnerTariff = getTariff();

        CargoType cargoType = partnerTariff.getCargoTypes().stream().findFirst()
            .orElse(null);
        softly.assertThat(cargoType)
            .as("Tariff should have no restrictions").isNull();
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql",
        "/data/controller/tariff/cargo_type.sql"
    })
    void testAddRestrictionToLocation() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/tariffs/1/cargo-types/1/locations/213")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk());

        PartnerTariff partnerTariff = getTariff();

        TariffLocation tariffLocation = partnerTariff.getTariffLocations().stream().findFirst()
            .orElse(null);
        softly.assertThat(tariffLocation)
            .as("Tariff location should exist").isNotNull()
            .extracting(TariffLocation::getLocationId)
            .as("Should be right location id").isEqualTo(213);
    }

    @Test
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql",
        "/data/controller/tariff/cargo_type.sql",
        "/data/controller/tariff/tariff_location.sql"
    })
    void testRemoveRestrictionFromLocation() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/tariffs/1/cargo-types/1/locations/213")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent());

        PartnerTariff partnerTariff = getTariff();

        TariffLocation tariffLocation = partnerTariff.getTariffLocations().stream().findFirst()
            .orElse(null);
        softly.assertThat(tariffLocation)
            .as("Tariff location should have no restrictions").isNull();
    }

    @TestFactory
    @Sql({
        "/data/controller/tariff/prepare_data.sql",
        "/data/controller/tariff/partner_tariff.sql",
        "/data/controller/tariff/cargo_type.sql",
        "/data/controller/tariff/tariff_location.sql"
    })
    Collection<DynamicTest> testErrorCases() {
        return Arrays.asList(
            DynamicTest.dynamicTest("GET tariffs of non-existing partner results in 404",
                () -> errorsTestTemplate(HttpMethod.GET,
                    "/partner/8/tariffs",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("GET non-existing tariff results in 404",
                () -> errorsTestTemplate(HttpMethod.GET,
                    "/tariffs/2",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("POST existing cargo type results in conflict",
                () -> errorsTestTemplate(HttpMethod.POST,
                    "/cargo-types",
                    TestUtil.pathToJson("data/controller/tariff/cargo_type.json"),
                    status().isConflict())),

            DynamicTest.dynamicTest("POST existing tariff results in conflict",
                () -> errorsTestTemplate(HttpMethod.POST,
                    "/partner/1/tariffs",
                    TestUtil.pathToJson("data/controller/tariff/partner_tariff.json"),
                    status().isConflict())),

            DynamicTest.dynamicTest("POST tariff to non-existing partner results in 404",
                () -> errorsTestTemplate(HttpMethod.POST,
                    "/partner/8/tariffs",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("DELETE non-existing tariff results in NO CONTENT",
                () -> errorsTestTemplate(HttpMethod.DELETE,
                    "/tariffs/8",
                    TestUtil.emptyJson(),
                    status().isNoContent())),

            DynamicTest.dynamicTest("PUT tariff restriction when tariff not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.PUT,
                    "/tariffs/2/cargo-types/1",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("PUT tariff restriction when cargo-type not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.PUT,
                    "/tariffs/1/cargo-types/2",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("PUT location restriction when tariff not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.PUT,
                    "/tariffs/2/cargo-types/1/locations/213",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("PUT location restriction when cargo-type not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.PUT,
                    "/tariffs/1/cargo-types/2/locations/213",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("PUT location restriction when already exists result in CONFLICT",
                () -> errorsTestTemplate(HttpMethod.PUT,
                    "/tariffs/1/cargo-types/1/locations/213",
                    TestUtil.emptyJson(),
                    status().isConflict())),

            DynamicTest.dynamicTest("DELETE location restriction when tariff not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.DELETE,
                    "/tariffs/2/cargo-types/1/locations/213",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("DELETE location restriction when cargo-type not exist " +
                    "results in NO CONTENT",
                () -> errorsTestTemplate(HttpMethod.DELETE,
                    "/tariffs/1/cargo-types/2/locations/213",
                    TestUtil.emptyJson(),
                    status().isNoContent())),

            DynamicTest.dynamicTest("DELETE tariff restriction when tariff not exist results in 404",
                () -> errorsTestTemplate(HttpMethod.DELETE,
                    "/tariffs/2/cargo-types/1",
                    TestUtil.emptyJson(),
                    status().isNotFound())),

            DynamicTest.dynamicTest("DELETE location restriction when not exist results in NO CONTENT",
                () -> errorsTestTemplate(HttpMethod.DELETE,
                    "/tariffs/1/cargo-types/1/locations/222",
                    TestUtil.emptyJson(),
                    status().isNoContent()))
        );

    }


    private void errorsTestTemplate(HttpMethod method,
                                    String requestUri,
                                    String content,
                                    ResultMatcher matcher) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .request(method, requestUri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(matcher);
    }
}
