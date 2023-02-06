package ru.yandex.market.delivery.gruzin.controller;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
class GruzinControllerTest extends AbstractContextualTest {
    @Autowired
    private MdsS3Client s3Client;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-05-04T12:00:00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(s3Client);
    }

    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/apply_cargo_units_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void pushState() {
        mockMvc.perform(put("/cargo_units/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/gruzin/cargo_units.json"))
            )
            .andExpect(status().isOk());

        ResourceLocation location =
            ResourceLocation.create(
                "gruzin-storage-test",
                "145_1651665600000_1059911605.json"
            );

        verify(s3Client).upload(
            Mockito.eq(location),
            Mockito.any()
        );

        verify(s3Client).getUrl(
            Mockito.eq(location)
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_ff_units.xml")
    void search() {
        mockMvc.perform(post("/cargo_units/search?limit=2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/gruzin/search.json"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/gruzin/search_success.json"));
    }
}
