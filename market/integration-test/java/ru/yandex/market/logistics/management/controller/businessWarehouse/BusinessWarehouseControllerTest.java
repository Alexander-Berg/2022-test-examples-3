package ru.yandex.market.logistics.management.controller.businessWarehouse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.controller.BusinessWarehouseController;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientStatusDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

/**
 * ???????????????????????????? ???????? {@link BusinessWarehouseController}.
 */
@ParametersAreNonnullByDefault
@DatabaseSetup("/data/controller/businessWarehouse/prepare.xml")
class BusinessWarehouseControllerTest extends AbstractContextualAspectValidationTest {

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private TestableClock clock;

    @Autowired
    FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        doNothing().when(logbrokerEventTaskProducer).produceTask(any());
        clock.setFixed(Instant.parse("2021-08-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        verifyNoMoreInteractions(logbrokerEventTaskProducer);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ????????????-????????????")
    void createBusinessWarehouse() throws Exception {
        performCreate(createBusinessWarehouseDtoBuilder().build())
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/create_business_warehouse_response.json")
            ));

        checkLogbrokerEvent(
            "data/controller/businessWarehouse/logbrokerEvent/create_warehouse.json"
        );
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ????????????-????????????, locationId ???? ???????????????? ????????????????")
    void createBusinessWarehouseLocationIdFromPartnerSettings() throws Exception {
        performCreate(createBusinessWarehouseDtoBuilder().partnerSettingDto(createPartnerSettingDto(100500)).build())
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson(
                    "data/controller/businessWarehouse/response/create_business_warehouse_response_location_id.json"
                )
            ));

        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/create_warehouse_location_id.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_no_settings_no_clients.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ????????????-???????????? ?????? ???????????????? ????????????????")
    void createBusinessWarehouseNullClientsAndSettings() throws Exception {
        performCreate(createBusinessWarehouseDtoBuilder().partnerSettingDto(null).build())
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson(
                    "data/controller/businessWarehouse/response/create_business_warehouse_response_no_settings.json"
                )
            ));
        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/create_warehouse.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_copy.xml")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_platform_client.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_capacity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_radial_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("?????????????????????? ????????????-????????????")
    void copyBusinessWarehouse() throws Exception {
        performCopy(3L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/copy_business_warehouse_response.json")
            ));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_copy.xml")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_without_register_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_platform_client.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_capacity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_copy_radial_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("?????????????????????? ????????????-???????????? ?????? ???????????????????? ")
    void copyBusinessWarehouseWithDisabledRegisterSchedule() throws Exception {
        featureProperties.setRegisterScheduleDisabled(true);
        performCopy(3L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/copy_business_warehouse_response.json")
            ));
        checkBuildWarehouseSegmentTask(2L);
        featureProperties.setRegisterScheduleDisabled(false);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/after_copy.xml")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_deactivate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ?????????????????????? ????????????-???????????? ?????????? ??????????????????????")
    void deactivateBusinessWarehouseAfterCopy() throws Exception {
        performDeactivateAfterCopy(3L).andExpect(status().isOk());
        checkLogbrokerEvent(
            "data/controller/businessWarehouse/logbrokerEvent/deactivate_warehouse_after_copying.json"
        );
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/data/controller/businessWarehouse/after_copy.xml",
        "/data/controller/businessWarehouse/after_copy_add_parameter.xml"
    })
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_deactivate_with_parameter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ?????????????????????? ????????????-???????????? c KOROBYTE_SYNC_ENABLED=true")
    void deactivateBusinessWarehouseAfterCopyWithTrueParameter() throws Exception {
        performDeactivateAfterCopy(3L).andExpect(status().isOk());
        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/deactivate_warehouse_KSE_true.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/data/controller/businessWarehouse/after_copy.xml",
        "/data/controller/businessWarehouse/after_copy_add_parameter_false.xml"
    })
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_deactivate_with_parameter_false.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ?????????????????????? ????????????-???????????? c KOROBYTE_SYNC_ENABLED=false")
    void deactivateBusinessWarehouseAfterCopyWithFalseParameter() throws Exception {
        performDeactivateAfterCopy(3L).andExpect(status().isOk());
        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/deactivate_warehouse_KSE_false.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/data/controller/businessWarehouse/fault_copy.xml")
    @MethodSource("faultDeactivateSources")
    void faultDeactivateBusinessWarehouse(
        @SuppressWarnings("unused") String name,
        Long partnerId,
        HttpStatus statusCode,
        String reason
    ) throws Exception {
        performDeactivateAfterCopy(partnerId)
            .andExpect(status().is(statusCode.value()))
            .andExpect(status().reason(reason));
    }

    @Test
    @DisplayName("???????????? ???????????????? ????????????-???????????? - ???????????????????? ?????? ????????????????")
    void createBusinessWarehouseNotDropship() throws Exception {
        performCreate(createBusinessWarehouseDtoBuilder().partnerType(PartnerType.DELIVERY).build())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Cannot create business warehouse for partner with type DELIVERY"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_create_no_market_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????? ????????????-???????????? ?????? ???????????????? marketId")
    void createBusinessWarehouseNoMarketId() throws Exception {
        performCreate(createBusinessWarehouseDtoBuilder().marketId(null).build())
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/businessWarehouse/response/create_business_warehouse_no_market_id_response.json"
            )));

        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/create_warehouse_no_market_id.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("???????????????? ?????????????????? ???????????????? ????????????-????????????")
    @MethodSource("validationArgs")
    void validation(
        @SuppressWarnings("unused") String name,
        CreateBusinessWarehouseDto createBusinessWarehouseDto,
        String responsePath
    ) throws Exception {
        performCreate(createBusinessWarehouseDto)
            .andExpect(status().isBadRequest())
            .andExpect(testJson(responsePath, Option.IGNORING_EXTRA_FIELDS));
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_get.xml")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????????? ????????????-????????????")
    void updateBusinessWarehouse() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/update_business_warehouse_response.json")
            ));

        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/update_warehouse.json");
        checkBuildWarehouseSegmentTask(1L, 2L, 9L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_get.xml")
    @DisplayName("???????????????????? ????????????-???????????? ?? ???????????? ????????????")
    void updateBusinessWarehouseWithEmptyDiff() throws Exception {
        performUpdate(updateBusinessWarehouseWithEmptyDiffBuilder().build(), 1L)
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(1L, 2L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_get.xml")
    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/partner_with_invalid_type.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("???????????????????? ????????????-???????????? - ???????????????????? ??????")
    void updateBusinessWarehouseInvalidType() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 300L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=300"));
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/partner.xml")
    @DisplayName("???????????????????? ????????????-???????????? - ?? ???????????????? ?????? ????????????")
    void updateBusinessWarehouseNoWarehouse() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can not find warehouse for partner 1"));
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_get.xml")
    @DatabaseSetup(value = "/data/controller/businessWarehouse/warehouse.xml", type = DatabaseOperation.INSERT)
    @DisplayName("???????????????????? ????????????-???????????? - ?? ???????????????? ???????????? ???????????? ?????????????????? ????????????")
    void updateBusinessWarehouseMoreThanOneWarehouses() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Partner 1 has more than one active warehouse"));
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/update/before/prepare_update_partner_location_id.xml")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/update/after/update_partner_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("???????????????????? ????????????-???????????? - ?????????????????? locationId ????????????????")
    void updateBusinessWarehouseWithPartnerLocationIdUpdate() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/update/response/update_partner_location_id.json")
            ));

        checkLogbrokerEvent("data/controller/businessWarehouse/update/event/update_partner_location_id.json");
        checkBuildWarehouseSegmentTask(1L, 2L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/update/before/prepare_update_partner_location_id.xml")
    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/update/before/partner_type_is_not_dropship.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/update/after/partner_is_not_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("locationId ???????????????? ???? ?????????????????? - ?????????????? ???? DROPSHIP")
    void updateBusinessWarehouseWithPartnerNotDropshipLocationIdNotUpdate() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/update/response/partner_is_not_dropship.json")
            ));

        checkLogbrokerEvent("data/controller/businessWarehouse/update/event/partner_is_not_dropship.json");
        checkBuildWarehouseSegmentTask(1L, 2L);
    }

    @Test
    @DatabaseSetup("/data/controller/businessWarehouse/update/before/prepare_update_partner_location_id.xml")
    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/update/before/partner_has_partner_relation.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/update/after/partner_has_partner_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("locationId ???????????????? ???? ?????????????????? - ?? ???????????????? ???????? ???????????????? ????????????")
    void updateBusinessWarehouseWithPartnerWithActiveRelationLocationIdNotUpdate() throws Exception {
        performUpdate(updateBusinessWarehouseDtoBuilder().build(), 1L)
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/businessWarehouse/update/response/partner_has_active_partner_relation.json"
            )));

        checkLogbrokerEvent("data/controller/businessWarehouse/update/event/partner_has_active_partner_relation.json");
        checkBuildWarehouseSegmentTask(1L, 2L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getWarehouses")
    @DisplayName("?????????????????? ????????????-??????????????")
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_search.xml")
    void getBusinessWarehouses(
        @SuppressWarnings("unused") String name,
        BusinessWarehouseFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/externalApi/business-warehouse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(filter))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("???????????????? ?????????????????? ???????????? ????????????-??????????????")
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_search.xml")
    void filterValidation(
        @SuppressWarnings("unused") String name,
        BusinessWarehouseFilter filter,
        String fieldName
    ) throws Exception {
        getWarehouses(filter, null)
            .andExpect(status().isBadRequest())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/filter_validation_error.json")
                    .replaceAll("\\[field]", fieldName)
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("?????????????????? ?????????????????????????? ????????????")
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_search.xml")
    void withPaging(
        @SuppressWarnings("unused") String displayName,
        Pageable pageable,
        String resultPath
    ) throws Exception {
        getWarehouses(BusinessWarehouseFilter.newBuilder().build(), pageable)
            .andExpect(IntegrationTestUtils.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent(resultPath));
    }

    @Test
    @DisplayName("?????????????????? ????????????-??????????????, ?? ???????????????? ?????????????????? ???????????????? ????????????")
    @DatabaseSetup("/data/controller/businessWarehouse/prepare_search.xml")
    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/additional_partner_relation.xml",
        type = DatabaseOperation.INSERT
    )
    void getBusinessWarehousesWithMultipleRelations() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/externalApi/business-warehouse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        BusinessWarehouseFilter.newBuilder()
                            .ids(Set.of(2L))
                            .build()
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/businessWarehouse/response/business_2.json"));
    }

    @Test
    @DisplayName("???????????????? ???????????? ?????? RETAIL ????????????????")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_retail_warehouse_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRetailWarehouse() throws Exception {
        performCreate(createRetailBusinessWarehouseDtoBuilder().build())
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/businessWarehouse/response/create_business_warehouse_for_retail.json"
            )));

        verifyNoInteractions(logbrokerEventTaskProducer);
        checkBuildWarehouseSegmentTask(1L);
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor = ArgumentCaptor.forClass(EventDto.class);
        verify(logbrokerEventTaskProducer).produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }

    @SneakyThrows
    @Nonnull
    private ResultActions getWarehouses(BusinessWarehouseFilter filter, @Nullable Pageable pageable) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/externalApi/business-warehouse")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(filter));

        if (pageable != null) {
            request
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()));
        }
        return mockMvc.perform(request);
    }

    @Nonnull
    private static Stream<Arguments> withPaging() {
        return Stream.of(
            Arguments.of(
                "???????????? ????????????????",
                PageRequest.of(0, 50),
                "data/controller/businessWarehouse/response/page_size.json"
            ),
            Arguments.of(
                "?????????????????? ???????????? ????????????????",
                PageRequest.of(0, 2),
                "data/controller/businessWarehouse/response/small_page_size.json"
            ),
            Arguments.of(
                "?????????????????? ????????????????",
                PageRequest.of(2, 1),
                "data/controller/businessWarehouse/response/last_page.json"
            ),
            Arguments.of(
                "?????????????? ?????????????? ????????????????",
                PageRequest.of(5, 10),
                "data/controller/businessWarehouse/response/empty_page_size_10.json"
            )
        );
    }

    @Nonnull
    private ResultActions performCreate(CreateBusinessWarehouseDto createBusinessWarehouseDto) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/business-warehouse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBusinessWarehouseDto))
        );
    }

    @Nonnull
    private ResultActions performDeactivateAfterCopy(Long partnerId) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders.post("/externalApi/business-warehouse/deactivate-after-copy/" + partnerId)
        );
    }

    @Nonnull
    @SuppressWarnings("SameParameterValue")
    private ResultActions performCopy(Long businessWarehouseId) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders.post("/externalApi/business-warehouse/copy/" + businessWarehouseId)
        );
    }

    @Nonnull
    private ResultActions performUpdate(
        UpdateBusinessWarehouseDto updateBusinessWarehouseDto,
        Long partnerId
    ) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/business-warehouse/" + partnerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBusinessWarehouseDto))
        );
    }

    @Nonnull
    private static Stream<Arguments> faultDeactivateSources() {
        return Stream.of(
            Arguments.of(
                "?????????? ???? ????????????????????????",
                2L,
                HttpStatus.BAD_REQUEST,
                "BusinessWarehouse is not copied"
            ),
            Arguments.of(
                "???????????????? ???? ????????????????????",
                3L,
                HttpStatus.NOT_FOUND,
                "Can't find Partner with id=3"
            ),
            Arguments.of(
                "?????????????? ?????????????????????????? ????????",
                6L,
                HttpStatus.BAD_REQUEST,
                "Invalid type of partner with id 6"
            ),
            Arguments.of(
                "???? ???????????????????? ????????????",
                4L,
                HttpStatus.NOT_FOUND,
                "Can not find warehouse for partner 4"
            ),
            Arguments.of(
                "???????????? ???????????? ????????????",
                5L,
                HttpStatus.BAD_REQUEST,
                "Partner 5 has more than one active warehouse"
            ),
            Arguments.of(
                "?????????? ?? businessId=null, ???? ?????????? ???? ????????????????????",
                9L,
                HttpStatus.NOT_FOUND,
                "Can not find copied business-warehouse with external id ext-iddddd"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> validationArgs() {
        return Stream.of(
            Arguments.of(
                "???? ???????????? ?????? ????????????????",
                createBusinessWarehouseDtoBuilder().partnerType(null).build(),
                "data/controller/businessWarehouse/response/partner_type.json"
            ),
            Arguments.of(
                "???? ???????????? ?????????????????????????? ??????????????",
                createBusinessWarehouseDtoBuilder().businessId(null).build(),
                "data/controller/businessWarehouse/response/business_id.json"
            ),
            Arguments.of(
                "???? ???????????? ?????????????? ?????????????????????????? ????????????",
                createBusinessWarehouseDtoBuilder().externalId(null).build(),
                "data/controller/businessWarehouse/response/external_id.json"
            ),
            Arguments.of(
                "???????????? ?????????????? ?????????????????????????? ????????????",
                createBusinessWarehouseDtoBuilder().externalId("").build(),
                "data/controller/businessWarehouse/response/external_id_empty.json"
            ),
            Arguments.of(
                "???? ???????????? ??????????",
                createBusinessWarehouseDtoBuilder().address(null).build(),
                "data/controller/businessWarehouse/response/address.json"
            ),
            Arguments.of(
                "???? ???????????? ?????????????????????????? ???????????????????? ??????????????????",
                createBusinessWarehouseDtoBuilder().platformClients(Set.of(
                    createPlatformClientStatusDto(null, PartnerStatus.ACTIVE)
                )).build(),
                "data/controller/businessWarehouse/response/platform_client_id.json"
            ),
            Arguments.of(
                "???? ???????????? ???????????????????? ??????????????????",
                createBusinessWarehouseDtoBuilder().platformClients(Set.of(
                    createPlatformClientStatusDto(2L, null)
                )).build(),
                "data/controller/businessWarehouse/response/platform_client_status.json"
            )
        );
    }

    @Nonnull
    private static CreateBusinessWarehouseDto.Builder createBusinessWarehouseDtoBuilder() {
        return CreateBusinessWarehouseDto.newBuilder()
            .partnerType(PartnerType.DROPSHIP)
            .name("business warehouse")
            .readableName("???????????? ??????????")
            .businessId(100L)
            .marketId(200L)
            .externalId("ext-id")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .country("????????????")
                    .settlement("??????????????????????")
                    .postCode("630111")
                    .latitude(BigDecimal.valueOf(0.123456789))
                    .longitude(BigDecimal.valueOf(0.987654321))
                    .street("??????????????????")
                    .house("11")
                    .housing("1")
                    .building("1")
                    .apartment("1")
                    .comment("comment")
                    .region("?????????????????????????? ??????????????")
                    .subRegion("??????????????????????????")
                    .addressString("????????????, ??????????????????????, ??????????????????")
                    .shortAddressString("????????????, ??????????????????????")
                    .exactLocationId(2)
                    .build()
            )
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+78005553535")
                    .internalNumber("1222")
                    .comment("number")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.NOON, true)))
            .contact(new Contact("??????", "??????????????", "????????????????"))
            .partnerSettingDto(createPartnerSettingDto(null))
            .platformClients(Set.of(createPlatformClientStatusDto(2L, PartnerStatus.ACTIVE)))
            .handlingTime(Duration.ofDays(10));
    }

    @Nonnull
    private static CreateBusinessWarehouseDto.Builder createRetailBusinessWarehouseDtoBuilder() {
        return CreateBusinessWarehouseDto.newBuilder()
            .partnerType(PartnerType.RETAIL)
            .name("retail warehouse")
            .businessId(100L)
            .marketId(null)
            .externalId("some-generated-value")
            .address(
                Address.newBuilder()
                    .locationId(213)
                    .settlement("????????????")
                    .region("????????????")
                    .build()
            );
    }

    @Nonnull
    private static PartnerSettingDto createPartnerSettingDto(@Nullable Integer locationId) {
        return PartnerSettingDto.newBuilder()
            .trackingType("post_reg")
            .updateCourierNeeded(true)
            .stockSyncEnabled(true)
            .locationId(locationId)
            .build();
    }

    @Nonnull
    private static PlatformClientStatusDto createPlatformClientStatusDto(
        @Nullable Long id,
        @Nullable PartnerStatus status
    ) {
        return PlatformClientStatusDto.newBuilder().platformClientId(id).status(status).build();
    }

    @Nonnull
    private static UpdateBusinessWarehouseDto.Builder updateBusinessWarehouseDtoBuilder() {
        return UpdateBusinessWarehouseDto.newBuilder()
            .name("new business warehouse")
            .readableName("?????????? ???????????? ??????????")
            .address(
                Address.newBuilder()
                    .locationId(2)
                    .country("????????????-2")
                    .settlement("??????????????????????-2")
                    .postCode("630112")
                    .latitude(BigDecimal.valueOf(0.23456789))
                    .longitude(BigDecimal.valueOf(0.87654321))
                    .street("??????????????????-2")
                    .house("12")
                    .housing("2")
                    .building("2")
                    .apartment("2")
                    .comment("comment-2")
                    .region("?????????????????????????? ??????????????-2")
                    .subRegion("??????????????????????????-2")
                    .addressString("????????????, ??????????????????????, ??????????????????-2")
                    .shortAddressString("????????????, ??????????????????????-2")
                    .exactLocationId(3)
                    .build()
            )
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+788888888")
                    .internalNumber("1333")
                    .comment("number-2")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(13, 0), LocalTime.of(15, 0), true)))
            .contact(new Contact("??????2", "??????????????2", "????????????????2"))
            .externalId("new-ext-id");
    }

    @Nonnull
    private static UpdateBusinessWarehouseDto.Builder updateBusinessWarehouseWithEmptyDiffBuilder() {
        return UpdateBusinessWarehouseDto.newBuilder()
            .name("business warehouse")
            .readableName("???????????? ??????????")
            .externalId("ext-id")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .country("????????????")
                    .settlement("??????????????????????")
                    .postCode("630111")
                    .latitude(BigDecimal.valueOf(0.123456789))
                    .longitude(BigDecimal.valueOf(0.987654321))
                    .street("??????????????????")
                    .house("11")
                    .housing("1")
                    .building("1")
                    .apartment("1")
                    .comment("comment")
                    .region("?????????????????????????? ??????????????")
                    .subRegion("??????????????????????????")
                    .addressString("????????????, ??????????????????????, ??????????????????")
                    .shortAddressString("????????????, ??????????????????????")
                    .exactLocationId(2)
                    .build()
            )
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+78005553535")
                    .internalNumber("1222")
                    .comment("number")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.NOON, true)))
            .contact(new Contact("??????", "??????????????", "????????????????"));
    }

    @Nonnull
    private static Stream<Arguments> getWarehouses() {
        return Stream.of(
            Arguments.of(
                "???? ?????????? ????????????????",
                BusinessWarehouseFilter.newBuilder().types(Set.of(PartnerType.SUPPLIER)).build(),
                "data/controller/businessWarehouse/response/business_2.json"
            ),
            Arguments.of(
                "???? ???????????????????????????? ??????????????????",
                BusinessWarehouseFilter.newBuilder().platformClientIds(Set.of(3L)).build(),
                "data/controller/businessWarehouse/response/business_23.json"
            ),
            Arguments.of(
                "???? ???????????????? ????????????????",
                BusinessWarehouseFilter.newBuilder().statuses(Set.of(PartnerStatus.ACTIVE)).build(),
                "data/controller/businessWarehouse/response/business_active_partners.json"
            ),
            Arguments.of(
                "???? ???????????????? ???????????????? ?? ????????????",
                BusinessWarehouseFilter.newBuilder()
                    .platformClientStatuses(Set.of(PartnerStatus.TESTING))
                    .platformClientIds(Set.of(3L))
                    .build(),
                "data/controller/businessWarehouse/response/business_2.json"
            ),
            Arguments.of(
                "???? ???????????? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().businessIds(Set.of(41L, 42L)).build(),
                "data/controller/businessWarehouse/response/business_12.json"
            ),
            Arguments.of(
                "???? ?????????????????? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().marketIds(Set.of(100L, 200L)).build(),
                "data/controller/businessWarehouse/response/business_12.json"
            ),
            Arguments.of(
                "???? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().ids(Set.of(1L, 2L)).build(),
                "data/controller/businessWarehouse/response/business_12.json"
            ),
            Arguments.of(
                "???? ?????????????????????????? ????????",
                BusinessWarehouseFilter.newBuilder().types(Set.of(PartnerType.OWN_DELIVERY)).build(),
                "data/controller/businessWarehouse/response/empty_page_size_0.json"
            ),
            Arguments.of(
                "???? ??????????????????, ?? ?????????????? ?????? ???????????????? ?????????????????????????? ??????????",
                BusinessWarehouseFilter.newBuilder().ids(Set.of(4L, 6L)).build(),
                "data/controller/businessWarehouse/response/empty_page_size_20.json"
            ),
            Arguments.of(
                "???? ?????????????????? ?? ?????????? RETAIL",
                BusinessWarehouseFilter.newBuilder().types(Set.of(PartnerType.RETAIL)).build(),
                "data/controller/businessWarehouse/response/business_retail.json"
            ),
            Arguments.of(
                "???? ???????????????????????????? ???????????? RETAIL ????????????????",
                BusinessWarehouseFilter.newBuilder().ids(Set.of(1000L)).build(),
                "data/controller/businessWarehouse/response/business_retail.json"
            )
        );
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> filterValidation() {
        return Stream.of(
            Arguments.of(
                "null ?? ?????????? ????????????????",
                BusinessWarehouseFilter.newBuilder().types(Collections.singleton(null)).build(),
                "types"
            ),
            Arguments.of(
                "null ?? ?????????????????????????????? ??????????????????",
                BusinessWarehouseFilter.newBuilder().platformClientIds(Collections.singleton(null)).build(),
                "platformClientIds"
            ),
            Arguments.of(
                "null ?? ???????????????? ????????????????",
                BusinessWarehouseFilter.newBuilder().statuses(Collections.singleton(null)).build(),
                "statuses"
            ),
            Arguments.of(
                "null ?? ???????????????? ???????????????? ?? ????????????",
                BusinessWarehouseFilter.newBuilder().platformClientStatuses(Collections.singleton(null)).build(),
                "platformClientStatuses"
            ),
            Arguments.of(
                "null ?? ???????????? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().businessIds(Collections.singleton(null)).build(),
                "businessIds"
            ),
            Arguments.of(
                "null ?? ?????????????????? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().marketIds(Collections.singleton(null)).build(),
                "marketIds"
            ),
            Arguments.of(
                "null ?? ??????????????????????????????",
                BusinessWarehouseFilter.newBuilder().ids(Collections.singleton(null)).build(),
                "ids"
            )
        );
    }
}
