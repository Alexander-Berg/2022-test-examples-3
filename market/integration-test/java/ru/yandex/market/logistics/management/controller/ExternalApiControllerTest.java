package ru.yandex.market.logistics.management.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.entity.request.partner.UpdateLegalInfoDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.management.util.TestUtil.noActiveAsyncThreads;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@CleanDatabase
class ExternalApiControllerTest extends AbstractContextualTest {

    @Autowired
    private PartnerRelationRepository partnerRelationRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    @Qualifier("timedCommonExecutor")
    private ThreadPoolTaskExecutor asyncExecutor;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logbrokerEventTaskProducer).produceTask(Mockito.any());
        Mockito.doNothing().when(logisticSegmentValidationProducer).produceTask(Mockito.anyLong());
        clock.setFixed(Instant.parse("2021-08-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        Mockito.verifyNoMoreInteractions(logbrokerEventTaskProducer);
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testGetExistPartnerExistLegalInfo() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partner/1/legalInfo")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/legal_info.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testGetExistPartnerWithNullLegalInfo() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partner/2/legalInfo")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("null"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testGetNotExistingPartnerLegalInfo() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partner/666/legalInfo")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("null"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testGetAllExistLegalInfo() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/legalInfo")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/legal_info_list.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @DatabaseSetup(
        value = "/data/controller/externalApi/legal_info_ogrn_null.xml",
        type = DatabaseOperation.REFRESH
    )
    void testGetAllLegalInfoWhenOgrnNull() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/legalInfo")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/legal_info_list_ogrn_null.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testUpdateLegalInfoPartnerNotFound() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/300/legalInfo",
                defaultLegalInfoUpdateDtoBuilder().build()
            )
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=300"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/legal_info_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/balance_tasks_not_generated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testUpdateLegalInfoNoChanges() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/1/legalInfo",
                defaultLegalInfoUpdateDtoBuilder().build()
            )
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/taxation_changed_without_vat_type_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/balance_tasks_not_generated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testUpdateLegalInfoTaxationSystemChangedWithoutVatType() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/1/legalInfo",
                defaultLegalInfoUpdateDtoBuilder()
                    .taxationSystem(TaxationSystem.SIMPLIFIED_INCOME_MINUS_EXPENSE)
                    .build()
            )
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/taxation_and_vat_type_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/update_offer_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testUpdateLegalInfoTaxationSystemChangedAndVatTypeChanged() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/1/legalInfo",
                defaultLegalInfoUpdateDtoBuilder()
                    .taxationSystem(TaxationSystem.COMMON)
                    .build()
            )
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/bik_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/update_person_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testUpdateLegalInfoBikChanged() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/1/legalInfo",
                defaultLegalInfoUpdateDtoBuilder()
                    .bik("445566")
                    .build()
            )
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/all_fields_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/update_offer_and_person_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testUpdateLegalInfoAllFields() throws Exception {
        mockMvc.perform(
            TestUtil.request(
                HttpMethod.PUT,
                "/externalApi/partner/1/legalInfo",
                defaultLegalInfoUpdateDtoBuilder()
                    .taxationSystem(TaxationSystem.COMMON)
                    .bik("445566")
                    .kpp("665544")
                    .account("account1")
                    .legalAddress(defaultAddressBuilder().country("Не Россия").build())
                    .postAddress(defaultAddressBuilder().country("Не Россия").settlement("Фактическое село").build())
                    .build()
            )
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testDisableRelationEnabled() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partnerRelation/switchOff?from=1&to=2")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        PartnerRelation pr = partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L).orElse(null);
        softly.assertThat(pr)
            .as("Partner relation found").isNotNull()
            .extracting(PartnerRelation::getEnabled)
            .as("Partner relation should be disabled").isEqualTo(false);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @DatabaseSetup(
        value = "/data/controller/externalApi/partner_relation_disabled.xml",
        type = DatabaseOperation.REFRESH
    )
    void testEnableRelationDisabled() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partnerRelation/switchOn?from=1&to=2")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        PartnerRelation pr = partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L).orElse(null);
        softly.assertThat(pr)
            .as("Partner relation found").isNotNull()
            .extracting(PartnerRelation::getEnabled)
            .as("Partner relation should be enabled").isEqualTo(true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testPartnerRelationAlreadyEnabled() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partnerRelation/switchOn?from=1&to=2")
        )
            .andExpect(MockMvcResultMatchers.status().isConflict());

        PartnerRelation pr = partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L).orElse(null);

        softly.assertThat(pr)
            .as("Partner relation found").isNotNull()
            .extracting(PartnerRelation::getEnabled)
            .as("Partner relation should be enabled").isEqualTo(true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testPartnerRelationNotFound() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partnerRelation/switchOn?from=1&to=153")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound());

        PartnerRelation pr = partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L).orElse(null);

        softly.assertThat(pr)
            .as("Partner relation found").isNotNull()
            .extracting(PartnerRelation::getEnabled)
            .as("Partner relation should be enabled").isEqualTo(true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testFindAllCargoTypes() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/cargo-types")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/cargoTypes.json"));
    }

    @Test
    void testFindAllCargoTypesEmpty() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/cargo-types")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/empty_entities.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void getPlatformClientOptions() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/platformClientOptions")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(content().json(pathToJson("data/controller/platform_client_options.json")));
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testStockSyncSwitchOn() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner/1/change-stock-sync?enabled=true")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Partner partner = partnerRepository.findById(1L).orElseThrow(EntityNotFoundException::new);
        softly.assertThat(partner.getStockSyncEnabled()).as("Stock sync should be enabled")
            .isTrue();

        softly.assertThat(partner.getStockSyncSwitchReason()).as("Unknown reason should be set")
            .isEqualTo(StockSyncSwitchReason.UNKNOWN);

        checkLogbrokerEvent(
            "data/controller/logbrokerEvent/test_stock_sync_switch_on.json"
        );
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @DatabaseSetup(
        value = "/data/controller/externalApi/partner_1_stock_sync_enabled.xml",
        type = DatabaseOperation.REFRESH
    )
    void testStockSyncSwitchOff() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner/1/change-stock-sync?enabled=false")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Partner partner = partnerRepository.findById(1L).orElseThrow(EntityNotFoundException::new);
        softly.assertThat(partner.getStockSyncEnabled()).as("Stock sync should be disabled")
            .isFalse();

        softly.assertThat(partner.getStockSyncSwitchReason()).as("Unknown reason should be set")
            .isEqualTo(StockSyncSwitchReason.UNKNOWN);

        checkLogbrokerEvent(
            "data/controller/logbrokerEvent/test_stock_sync_switch_off.json"
        );
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    @DatabaseSetup(
        value = "/data/controller/externalApi/partner_1_stock_sync_enabled.xml",
        type = DatabaseOperation.REFRESH
    )
    void testStockSyncSwitchOffWithReason() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner/1/change-stock-sync?enabled=false&reason=AUTO_CHANGED_AFTER_FAIL")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Partner partner = partnerRepository.findById(1L).orElseThrow(EntityNotFoundException::new);
        softly.assertThat(partner.getStockSyncEnabled()).as("Stock sync should be disabled")
            .isFalse();

        softly.assertThat(partner.getStockSyncSwitchReason()).as("Proper reason should be set")
            .isEqualTo(StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);

        checkLogbrokerEvent(
            "data/controller/logbrokerEvent/test_stock_sync_switch_off.json"
        );
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    void testStockSync404WhenPartnerNotExists() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner/3/change-stock-sync?enabled=false")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DatabaseSetup("/data/controller/externalApi/prepare_data_activate_multiple_entities.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalApi/after/multiple_entities_activated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testActivateMultipleEntitiesSuccess() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/activate-multiple-entities")
                .content(pathToJson("data/controller/externalApi/activate_multiple_entities_request.json"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk());
        checkBuildWarehouseSegmentTask(10002L);
    }

    @TestFactory
    @DisplayName("Tariffs get by id tests")
    @DatabaseSetup("/data/controller/externalApi/prepare_data.xml")
    Collection<DynamicTest> cargoTypesTest() {
        return Arrays.asList(
            DynamicTest.dynamicTest("tariff cargo type found, tariff locations empty",
                () -> cargoTypesTestTemplate(11)),

            DynamicTest.dynamicTest("tariff cargo type empty, tariff locations found",
                () -> cargoTypesTestTemplate(12)),

            DynamicTest.dynamicTest("tariff cargo type empty, tariff locations empty",
                () -> cargoTypesTestTemplate(13)),

            DynamicTest.dynamicTest("tariff cargo type found, tariff locations found",
                () -> cargoTypesTestTemplate(14)),

            DynamicTest.dynamicTest("tariff not exist",
                () -> cargoTypesTestTemplate(15))
        );
    }

    void cargoTypesTestTemplate(long tariffId) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(String.format("/externalApi/tariff/%s/cargo-types", tariffId))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson(String.format("data/controller/tariff_%s.json", tariffId)));
    }

    @Nonnull
    private UpdateLegalInfoDto.Builder defaultLegalInfoUpdateDtoBuilder() {
        return UpdateLegalInfoDto.newBuilder()
            .taxationSystem(TaxationSystem.SIMPLIFIED_INCOME)
            .legalAddress(defaultAddressBuilder().build())
            .postAddress(defaultAddressBuilder().settlement("Фактическое село").build())
            .kpp("332211")
            .bik("112233")
            .account("account");
    }

    @Nonnull
    private Address.AddressBuilder defaultAddressBuilder() {
        return Address.newBuilder()
            .locationId(133543)
            .country("Россия")
            .latitude(BigDecimal.valueOf(55.822463))
            .longitude(BigDecimal.valueOf(84.258002))
            .settlement("Юридическое село")
            .postCode("633372")
            .street("Солнечная")
            .house("6")
            .housing("2")
            .building("А")
            .apartment("318")
            .addressString(
                "село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2"
            )
            .shortAddressString("село Зудово, Солнечная улица, 9A, 2")
            .exactLocationId(133543);
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor =
            ArgumentCaptor.forClass(EventDto.class);
        Mockito.verify(logbrokerEventTaskProducer)
            .produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }
}
