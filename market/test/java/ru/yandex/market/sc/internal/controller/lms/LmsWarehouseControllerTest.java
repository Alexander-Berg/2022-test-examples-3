package ru.yandex.market.sc.internal.controller.lms;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehousePropertyRepository;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.domain.warehouse.repository.enums.WarehousePropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.lms.wh.LmsWarehousePropertyDetailDto;
import ru.yandex.market.sc.internal.model.lms.DeleteIdsDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LmsWarehouseControllerTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WarehousePropertyRepository warehousePropertyRepository;

    @Autowired
    WarehouseRepository warehouseRepository;

    @Test
    @Order(1)
    @SneakyThrows
    void getWarehousesTest() {
        var warehouse = testFactory.storedWarehouse("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_wh_response.json", warehouse), false));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses/" + warehouse.getId())
                )
                .andExpect(status().is2xxSuccessful())
                 .andExpect(content()
                        .json(readResponse("lms_wh_detail_response.json", warehouse), false));
    }

    @Test
    @SneakyThrows
    void getWarehousePropertiesTest() {
        var warehouse = testFactory.storedWarehouse("111", true);

        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");

        var properties =
                warehousePropertyRepository.findAllByWarehouseYandexId(warehouse.getYandexId());
        var property = properties.stream().findFirst().get();

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses/properties?id=" + warehouse.getId())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_list_wh_properties_response.json"),
                                        getKeyOptions(),
                                        property.getId(),
                                        property.getValue(),
                                        property.getKey(),
                                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(),
                                        property.getWarehouseYandexId(),
                                        property.getId()),
                                false));
    }

    @Test
    @SneakyThrows
    void getEmptyFilteredWarehouse() {
        var warehouse = testFactory.storedWarehouse("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses" +
                                        generateFilterWh(0L, null, null, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_wh_empty_response.json", warehouse), false));
    }

    @Test
    @SneakyThrows
    void getFilteredWarehouse() {
        var warehouse1 = testFactory.storedWarehouse("1", false);
        var warehouse2 = testFactory.storedWarehouse("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses"
                                        + generateFilterWh(warehouse2.getId(), null, null, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_wh_response.json", warehouse2), false));
    }

    @Test
    @SneakyThrows
    void getFullFilteredWarehouse() {
        var warehouse1 = testFactory.storedWarehouse("1", false);
        var warehouse2 = testFactory.storedWarehouse("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses" +
                                        generateFilterWh(warehouse2.getId(), warehouse2.getYandexId(),
                                                warehouse2.getIncorporation(), warehouse2.getPartnerId()))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_wh_response.json", warehouse2), false));
    }

    @Test
    @SneakyThrows
    void getFilteredByIncorporationWarehouse() {
        var warehouse1 = testFactory.storedWarehouse("1", false);
        var warehouse2 = testFactory.storedWarehouse("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/warehouses" +
                                        generateFilterWh(null, warehouse1.getYandexId(),
                                                "ромашка", null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_wh_response.json", warehouse1), false));
    }

    @Test
    @SneakyThrows
    void getFilteredWarehousePropertiesTest() {
        var warehouse = testFactory.storedWarehouse("111", true);

        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
        var properties =
                warehousePropertyRepository.findAllByWarehouseYandexId(warehouse.getYandexId());
        var property = properties.stream().findFirst().get();
        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.IS_WAREHOUSE,
                "true");

        assertThat(warehousePropertyRepository
                .findAllByWarehouseYandexId(warehouse.getYandexId()).size()).isEqualTo(2);

        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/warehouses/properties?id="
                        + warehouse.getId() + generateFilterWhProperty(null, null,
                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_list_wh_properties_response.json"),
                                        getKeyOptions(),
                                        property.getId(),
                                        property.getValue(),
                                        property.getKey(),
                                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(),
                                        property.getWarehouseYandexId(),
                                        property.getId()),
                                false));
    }

    @Test
    @SneakyThrows
    void addNewWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        var dto = new LmsWarehousePropertyDetailDto(null, null,
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(), "true", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/")
                        .param("parentId", String.valueOf(warehouse.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void addExistsWarehousePropertySingleValueTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        testFactory.setWarehouseProperty(warehouse.getYandexId(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(), "true");

        assert !WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.isMultiValue();

        var dto = new LmsWarehousePropertyDetailDto(null, null,
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(), "111", "false");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/")
                        .param("parentId", String.valueOf(warehouse.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void addExistsWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        testFactory.setWarehouseProperty(warehouse.getYandexId(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(), "true");

        var dto = new LmsWarehousePropertyDetailDto(null, null,
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(), "true", "1");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/")
                        .param("parentId", String.valueOf(warehouse.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void deleteWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "true");

        WarehouseProperty property = warehousePropertyRepository
                .findAllByWarehouseYandexId(warehouse.getYandexId()).stream()
                .filter(p -> WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS.equals(p.getKey())).findFirst().get();

        var dto = new DeleteIdsDto();
        dto.setIds(Arrays.asList(property.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @SneakyThrows
    void changeWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "true");

        WarehouseProperty property = warehousePropertyRepository
                .findAllByWarehouseYandexId(warehouse.getYandexId()).stream()
                .filter(p -> WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS.equals(p.getKey())).findFirst().get();

        var dto = new LmsWarehousePropertyDetailDto(property.getId(), property.getId(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(),
                "false",
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/warehouses/properties/"
                                + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_add_wh_property_response.json"),
                                        property.getId(), property.getId(), property.getWarehouseYandexId()
                                )
                        )
                );
    }

    @Test
    @SneakyThrows
    void changeOnNullWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);
        testFactory.setWarehouseProperty(warehouse.getYandexId(), WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "true");

        WarehouseProperty property = warehousePropertyRepository
                .findAllByWarehouseYandexId(warehouse.getYandexId()).stream()
                .filter(p -> WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS.equals(p.getKey())).findFirst().get();

        var dto = new LmsWarehousePropertyDetailDto(property.getId(), property.getId(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment(),
                null,
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/warehouses/properties/" + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void addInvalidBooleanWarehousePropertyTest() {
        var warehouse = testFactory.storedWarehouse("111", true);

        var dto = new LmsWarehousePropertyDetailDto(null, null,
                WarehousePropertiesKey.CAN_PROCESS_CLIENT_RETURNS.name(),
                WarehousePropertiesKey.CAN_PROCESS_CLIENT_RETURNS.getComment(), "тру", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/warehouses/properties/")
                        .param("parentId", String.valueOf(warehouse.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void getFilteredByKeyPropertiesKeyListTest() {
        String filterValue = "BUFFER_RETURNS";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/warehouses/properties/list"
                        + generateFilterWhPropertiesKey(filterValue, null)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        readResponse("lms_filter_wh_properties_by_key_response.json"),
                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.ordinal(),
                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getTitle(),
                        WarehousePropertiesKey.CAN_PROCESS_BUFFER_RETURNS.getComment()
                        )
                ));

    }

    @Test
    @SneakyThrows
    void getFilteredByKeyCommentPropertiesKeyListTest() {
        String filterValue = "поврежденных заказов";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/warehouses/properties/list"
                        + generateFilterWhPropertiesKey(null, filterValue)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        readResponse("lms_filter_wh_properties_by_key_comment_response.json"),
                        WarehousePropertiesKey.CAN_PROCESS_DAMAGED_ORDERS.ordinal(),
                        WarehousePropertiesKey.CAN_PROCESS_DAMAGED_ORDERS.getTitle(),
                        WarehousePropertiesKey.CAN_PROCESS_DAMAGED_ORDERS.getComment()
                        )
                ));

    }

    private String generateFilterWhPropertiesKey(String key, String keyComment) {
        String reqKey = key == null
                ? null
                : "key=" + key;
        String reqKeyComment = keyComment == null
                ? null
                : "keyComment=" + keyComment;

        return Stream.of(reqKey, reqKeyComment)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "?", ""));
    }


    private String generateFilterWh(Long warehouseId, String yandexId, String incorporation, String partnerId) {
        String reqWhId = warehouseId == null
                ? null
                : "warehouseId=" + warehouseId;
        String reqYandexId = yandexId == null
                ? null
                : "yandexId=" + yandexId;
        String reqIncorporation = incorporation == null
                ? null
                : "incorporation=" + incorporation;
        String reqPartnerId = partnerId == null
                ? null
                : "partnerId=" + partnerId;


        return Stream.of(reqWhId, reqYandexId, reqIncorporation, reqPartnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "?", ""));
    }

    private String generateFilterWhProperty(Long whPropertyId, Long warehouseId, WarehousePropertiesKey key,
                                            String value) {
        String reqWhPropertyId = whPropertyId == null
                ? null
                : "whPropertyId=" + whPropertyId;
        String reqWarehouseId = warehouseId == null
                ? null
                : "warehouseId=" + warehouseId;
        String reqKey = key == null
                ? null
                : "key=" + key;
        String reqValue = value == null
                ? null
                : "value=" + value;

        return Stream.of(reqWhPropertyId, reqWarehouseId, reqKey, reqValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "&", ""));
    }

    private String getKeyOptions() {
        return Arrays.stream(WarehousePropertiesKey.values())
                .map(key -> "{" +
                        "\"id\":\"" + key.name() + "\"," +
                        "\"displayName\":\"" + key.getTitle() + "\"," +
                        "\"openNewTab\":" + false + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String getTypeOptions() {
        return Arrays.stream(WarehouseType.values())
                .map(key -> "{" +
                        "\"id\":\"" + key.name() + "\"," +
                        "\"displayName\":\"" + key.getTitle() + "\"," +
                        "\"openNewTab\":" + false + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    @SneakyThrows
    private String readResponse(String file, Warehouse warehouse) {
        return String.format(readResponse(file),
                getTypeOptions(),
                warehouse.getId(),
                warehouse.getIncorporation(),
                "[" + warehouse.getPhones().stream().collect(Collectors.joining("\",\"", "\"", "\"")) + "]",
                warehouse.getPartnerId(),
                warehouse.getYandexId(),
                warehouse.getId(),
                warehouse.getContact(),
                warehouse.getType().getTitle());
    }

    @SneakyThrows
    private String readResponse(String file) {
        return IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                file
                        )
                ),
                StandardCharsets.UTF_8
        );
    }

}
