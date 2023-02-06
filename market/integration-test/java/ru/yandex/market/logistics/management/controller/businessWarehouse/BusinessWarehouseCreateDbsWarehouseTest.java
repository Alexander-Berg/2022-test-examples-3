package ru.yandex.market.logistics.management.controller.businessWarehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientStatusDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.queue.producer.DbsGraphCreationProducer;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/businessWarehouse/dbs/before/prepare_dbs.xml")
@ExpectedDatabase(
    value = "/data/controller/businessWarehouse/dbs/after/graph.xml",
    assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
)
class BusinessWarehouseCreateDbsWarehouseTest extends AbstractContextualAspectValidationTest {
    private static final Long DBS_PLATFORM_CLIENT_ID = 5L;

    @Autowired
    private DbsGraphCreationProducer dbsGraphCreationProducer;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        doNothing().when(dbsGraphCreationProducer).produceTask(anyLong());
        doNothing().when(logbrokerEventTaskProducer).produceTask(any());
        clock.setFixed(Instant.parse("2021-08-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(dbsGraphCreationProducer);
        clock.clearFixed();
    }

    @Test
    @DisplayName("В запросе на создание склада указаны настройки canSellMedicine и canDeliverMedicine")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/dbs/after/with_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void settingsPresent() throws Exception {
        createWarehouse(
            createRequestDtoBuilder()
                .partnerSettingDto(PartnerSettingDto.newBuilder()
                    .canSellMedicine(true)
                    .canDeliverMedicine(true)
                    .build()
                )
                .build()
        )
            .andExpect(status().isOk());

        verify(dbsGraphCreationProducer).produceTask(1L);
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @DisplayName("В запросе на создание склада не указаны настройки canSellMedicine и canDeliverMedicine")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/dbs/after/no_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noSettingsProvided() throws Exception {
        createWarehouse(createRequestDtoBuilder().partnerSettingDto(PartnerSettingDto.newBuilder().build()).build())
            .andExpect(status().isOk());

        verify(dbsGraphCreationProducer).produceTask(1L);
        checkBuildWarehouseSegmentTask(1L);
    }

    @Nonnull
    private ResultActions createWarehouse(CreateBusinessWarehouseDto request) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/business-warehouse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }

    @Nonnull
    private CreateBusinessWarehouseDto.Builder createRequestDtoBuilder() {
        return CreateBusinessWarehouseDto.newBuilder()
            .partnerType(PartnerType.DROPSHIP_BY_SELLER)
            .name("DBS Warehouse")
            .businessId(100L)
            .externalId("ext-id")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .country("Россия")
                    .settlement("Новосибирск")
                    .postCode("630111")
                    .latitude(BigDecimal.valueOf(0.123456789))
                    .longitude(BigDecimal.valueOf(0.987654321))
                    .street("Николаева")
                    .house("11")
                    .housing("1")
                    .building("1")
                    .apartment("1")
                    .comment("comment")
                    .region("Новосибирская область")
                    .subRegion("Новосибирский")
                    .addressString("Россия, Новосибирск, Николаева")
                    .shortAddressString("Россия, Новосибирск")
                    .exactLocationId(2)
                    .build()
            )
            .platformClients(Set.of(
                PlatformClientStatusDto.newBuilder()
                    .platformClientId(DBS_PLATFORM_CLIENT_ID)
                    .status(PartnerStatus.ACTIVE)
                    .build()
            ));
    }
}
