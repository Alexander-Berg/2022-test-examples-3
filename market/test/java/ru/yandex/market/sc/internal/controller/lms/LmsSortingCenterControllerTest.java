package ru.yandex.market.sc.internal.controller.lms;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.lms.DeleteIdsDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
@Slf4j
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LmsSortingCenterControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    JdbcTemplate jdbcTemplate;
    private final PlaceRepository placeRepository;

    @Test
    @Order(1)
    @SneakyThrows
    void getSortingCentersTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_sc_response.json", sortingCenter), false));

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters/" + sortingCenter.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_sc_detail_response.json", sortingCenter), false));
    }

    @Test
    @SneakyThrows
    void getSortingCenterPropertiesTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED,
                "true");
        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters/properties?id=" + sortingCenter.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_list_sc_properties_response.json"),
                                getKeyOptions(),
                                property.getId(),
                                property.getId(),
                                "\"" + sortingCenter.getId() + "\""),
                                false));
    }

    @Test
    @SneakyThrows
    void getEmptyFilteredSortingCenter() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters" + generateFilterSc(0L, null, null, null))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_sc_empty_response.json", sortingCenter), false));
    }

    @Test
    @SneakyThrows
    void getFilteredSortingCenter() {
        var sortingCenter1 = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var sortingCenter2 = testFactory.storedSortingCenter(42L, "Еще более новый СЦ");

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters" + generateFilterSc(42L, null, null, null))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_sc_response.json", sortingCenter2), false));
    }

    @Test
    @SneakyThrows
    void getFullFilteredSortingCenter() {
        var sortingCenter1 = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var sortingCenter2 = testFactory.storedSortingCenter(42L, "Еще более новый СЦ");

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters" +
                                generateFilterSc(sortingCenter2.getId(), sortingCenter2.getPartnerId(),
                                        sortingCenter2.getPartnerName(), sortingCenter2.getScName()))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_sc_response.json", sortingCenter2), false));
    }

    @Test
    @SneakyThrows
    void getFilteredByPartnerNameSortingCenter() {
        var sortingCenter1 = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var sortingCenter2 = testFactory.storedSortingCenter(42L, "Старый СЦ");

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/sortingCenters" +
                                        generateFilterSc(null, null,
                                               "новЫЙ", null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_sc_response.json", sortingCenter1), false));
    }

    @Test
    @SneakyThrows
    void getFilteredSortingCenterPropertiesTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS,
                "true");
        var property2 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED,
                "true");
        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortingCenters/properties?id=" + sortingCenter.getId()
                                + generateFilterScProperty(null, null,
                                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, null))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_list_sc_properties_response.json"),
                                getKeyOptions(),
                                property2.getId(),
                                property2.getId(),
                                "\"" + sortingCenter.getId() + "\""),
                                false));
    }

    @Test
    @SneakyThrows
    void addNewSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var dto = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.XDOC_ENABLED.name(),
                SortingCenterPropertiesKey.XDOC_ENABLED.getComment(), "12", "false");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                .param("parentId", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void addExistsSortingCenterPropertySingleValueTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");

        assert !SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.isMultiValue();

        var dto = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.name(),
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment(), "12", "false");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                .param("parentId", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void addExistsSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");

        var property2 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "true");

        var dto = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.name(),
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment(), "12", "false");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                .param("parentId", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addSortingCenterPropertyTest() {
        var sortingCenter1 = testFactory.storedSortingCenter(12L, "Новый СЦ1");
        var sortingCenter2 = testFactory.storedSortingCenter(24L, "Новый СЦ2");

        var property1_1 = testFactory.setSortingCenterProperty(sortingCenter1,
                SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "true");

        var property1_2 = testFactory.setSortingCenterProperty(sortingCenter1,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");

        var property2_1 = testFactory.setSortingCenterProperty(sortingCenter2,
                SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "true");

        var dto1 = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.name(),
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment(), "12", "false");

        var dto2 = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.name(),
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment(), "24", "false");

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                .param("parentId", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(dto1)))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                        .param("parentId", "24")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto2)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void addInvalidBooleanSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");

        var dto = new LmsSortingCenterPropertyDetailDto(null, null,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.name(),
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment(), "12", "фолс123");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/")
                        .param("parentId", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void deleteSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");

        var dto = new DeleteIdsDto();
        dto.setIds(Arrays.asList(property1.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/sortingCenters/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @SneakyThrows
    void changeSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        var dto = new LmsSortingCenterPropertyDetailDto(property1.getId(), property1.getId(),
                property1.getKey().name(),
                property1.getKey().getComment(),
                String.valueOf(property1.getSortingCenterId()),
                "false");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/sortingCenters/properties/" + property1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(dto)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_add_sc_property_response.json"),
                                getKeyOptions(),
                                property1.getId(), property1.getId()
                                )
                        )
                );
    }

    @Test
    @SneakyThrows
    void changeOnNullSortingCenterPropertyTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var property1 = testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        LmsSortingCenterPropertyDetailDto dto = new LmsSortingCenterPropertyDetailDto(null, property1.getId(),
                property1.getKey().name(),
                property1.getKey().getComment(),
                String.valueOf(property1.getSortingCenterId()),
                null);
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/sortingCenters/properties/" + property1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void getFilteredByKeyPropertiesKeyListTest() {
        String filterValue = "courier_route_sheet_v2";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/sortingCenters/properties/list"
                        + generateFilterScPropertiesKey(filterValue, null)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_filter_properties_by_key_response.json"),
                                SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V2_ENABLED.ordinal(),
                                SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V2_ENABLED.getTitle(),
                                SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V2_ENABLED.getComment()),
                                false));

    }

    @Test
    @SneakyThrows
    void getFilteredByKeyCommentPropertiesKeyListTest() {
        String filterValue = "поддерживаются ли поврежденные заказы";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/sortingCenters/properties/list"
                        + generateFilterScPropertiesKey(null, filterValue)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_filter_properties_by_key_comment_response.json"),
                                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.ordinal(),
                                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getTitle(),
                                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED.getComment()),
                                false));

    }

    private String generateFilterSc(Long sortingCenterId, String partnerId, String partnerName,
                                    String sortingCenterName) {
        String reqScId = sortingCenterId == null
                ? null
                : "sortingCenterId=" + sortingCenterId;
        String reqPartnerId = partnerId == null
                ? null
                : "partnerId=" + partnerId;
        String reqPartnerName = partnerName == null
                ? null
                : "partnerName=" + partnerName;
        String reqSortingCenterName = sortingCenterName == null
                ? null
                : "sortingCenterName=" + sortingCenterName;

        return Stream.of(reqScId, reqPartnerId, reqPartnerName, reqSortingCenterName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "?", ""));
    }

    private String generateFilterScProperty(Long scPropertyId, Long sortingCenterId, SortingCenterPropertiesKey key,
                                            String value) {
        String reqScPropertyId = scPropertyId == null
                ? null
                : "scPropertyId=" + scPropertyId;
        String reqSortingCenterId = sortingCenterId == null
                ? null
                : "sortingCenterId=" + sortingCenterId;
        String reqKey = key == null
                ? null
                : "key=" + key;
        String reqValue = value == null
                ? null
                : "value=" + value;

        return Stream.of(reqScPropertyId, reqSortingCenterId, reqKey, reqValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "&", ""));
    }

    private String generateFilterScPropertiesKey(String key, String keyComment) {
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

    @SneakyThrows
    private String readResponse(String file, SortingCenter sortingCenter) {
        return String.format(readResponse(file),
                sortingCenter.getId(),
                sortingCenter.getAddress(),
                sortingCenter.getPartnerId(),
                sortingCenter.getPartnerName(),
                sortingCenter.getScName(),
                sortingCenter.getId());
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

    private String getKeyOptions() {
        return Arrays.stream(SortingCenterPropertiesKey.values())
                .map(key -> "{" +
                        "\"id\":\"" + key.name() + "\"," +
                        "\"displayName\":\"" + key.getTitle() + "\"," +
                        "\"openNewTab\":" + false + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
