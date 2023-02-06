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

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertyRepository;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceRepository;
import ru.yandex.market.sc.core.domain.delivery_service.repository.enums.DeliveryServicePropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.lms.ds.LmsDeliveryServicePropertyDetailDto;
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
public class LmsDeliveryServiceControllerTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DeliveryServicePropertyRepository deliveryServicePropertyRepository;

    @Autowired
    DeliveryServiceRepository deliveryServiceRepository;

    @Test
    @Order(1)
    @SneakyThrows
    void getDeliveryServicesTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_ds_response.json", deliveryService), false));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices/" + deliveryService.getId())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_ds_detail_response.json", deliveryService), false));
    }

    @Test
    @SneakyThrows
    void getDeliveryServicePropertiesTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        var properties =
                deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(deliveryService.getYandexId());
        var property = properties.stream().findFirst().get();

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices/properties?id=" + deliveryService.getId())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_list_ds_properties_response.json"),
                                        getKeyOptions(),
                                        property.getId(),
                                        property.getDeliveryServiceYandexId(),
                                        property.getValue(),
                                        property.getKey(),
                                        DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(),
                                        property.getId(),
                                        deliveryService.getYandexId()),
                                false));
    }

    @Test
    @SneakyThrows
    void getEmptyFilteredDeliveryService() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices" + generateFilterDs(0L, null, null, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_ds_empty_response.json", deliveryService), false));
    }

    @Test
    @SneakyThrows
    void getFilteredDeliveryService() {
        var deliveryService1 = testFactory.storedDeliveryService("1", false);
        var deliveryService2 = testFactory.storedDeliveryService("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices"
                                        + generateFilterDs(deliveryService2.getId(), null, null, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_ds_response.json", deliveryService2), false));
    }

    @Test
    @SneakyThrows
    void getFullFilteredDeliveryService() {
        var deliveryService1 = testFactory.storedDeliveryService("1", false);
        var deliveryService2 = testFactory.storedDeliveryService("111", true);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/deliveryServices" +
                                        generateFilterDs(deliveryService2.getId(), deliveryService2.getYandexId(),
                                                deliveryService2.getPartnerId(), deliveryService2.getName()))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(readResponse("lms_list_ds_response.json", deliveryService2), false));
    }

    @Test
    @SneakyThrows
    void getFilteredDeliveryServicePropertiesTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        var properties =
                deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(deliveryService.getYandexId());
        var property = properties.stream().findFirst().get();

        testFactory.setDeliveryServiceProperty(deliveryService, DeliveryServiceProperty.NEED_TRANSPORT_BARCODE,
                "true");

        assertThat(deliveryServicePropertyRepository
                .findAllByDeliveryServiceYandexId(deliveryService.getYandexId()).size()).isEqualTo(2);

        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/deliveryServices/properties?id="
                        + deliveryService.getId() + generateFilterDsProperty(null, null,
                                        DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX, null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_list_ds_properties_response.json"),
                                        getKeyOptions(),
                                        property.getId(),
                                        property.getDeliveryServiceYandexId(),
                                        property.getValue(),
                                        property.getKey(),
                                        DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(),
                                        property.getId(),
                                        "\"" + deliveryService.getYandexId() + "\""),
                                false));
    }

    @Test
    @SneakyThrows
    void addNewDeliveryServicePropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.getTitle(),
                DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.getComment(), "true", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void addExistsDeliveryServicePropertySingleValueTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.getTitle(), "true");

        assert !DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.isMultiValue();

        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.getTitle(),
                DeliveryServicePropertiesKey.NEED_TRANSPORT_BARCODE.getComment(), "111", "false");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void addExistsDeliveryServicePropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(), "1");

        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getComment(), "111", "1");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void deleteDeliveryServicePropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        testFactory.setDeliveryServiceProperty(deliveryService, DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, "1");

        DeliveryServiceProperty property = deliveryServicePropertyRepository
                .findAllByDeliveryServiceYandexId(deliveryService.getYandexId()).stream()
                .filter(p -> DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC.equals(p.getKey())).findFirst().get();

        var dto = new DeleteIdsDto();
        dto.setIds(Arrays.asList(property.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @SneakyThrows
    void changeDeliveryServicePropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        testFactory.setDeliveryServiceProperty(deliveryService, DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, "1");

        DeliveryServiceProperty property = deliveryServicePropertyRepository
                .findAllByDeliveryServiceYandexId(deliveryService.getYandexId()).stream()
                .filter(p -> DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC.equals(p.getKey())).findFirst().get();

        var dto = new LmsDeliveryServicePropertyDetailDto(property.getId(), property.getId(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getComment(),
                "12",
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/deliveryServices/properties/"
                                + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                        readResponse("lms_add_ds_property_response.json"),
                                        property.getId(), property.getId()
                                )
                        )
                );
    }

    @Test
    @SneakyThrows
    void changeOnNullDeliveryServicePropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        testFactory.setDeliveryServiceProperty(deliveryService, DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, "1");

        DeliveryServiceProperty property = deliveryServicePropertyRepository
                .findAllByDeliveryServiceYandexId(deliveryService.getYandexId()).stream()
                .filter(p -> DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC.equals(p.getKey())).findFirst().get();

        var dto = new LmsDeliveryServicePropertyDetailDto(property.getId(), property.getId(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getComment(),
                null,
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/deliveryServices/properties/" + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void getFilteredByKeyPropertiesKeyListTest() {
        String filterValue = "type_on_sc";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/deliveryServices/properties/list"
                        + generateFilterDsPropertiesKey(filterValue, null)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                                readResponse("lms_filter_ds_properties_by_key_response.json"),
                                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.ordinal(),
                                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getTitle(),
                                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment()
                        )
                ));

    }

    @Test
    @SneakyThrows
    void getFilteredByKeyCommentPropertiesKeyListTest() {
        String filterValue = "Включает предсортировку для службы доставки на конкретном СЦ";
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/sortingCenter/deliveryServices/properties/list"
                        + generateFilterDsPropertiesKey(null, filterValue)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        readResponse("lms_filter_ds_properties_by_key_comment_response.json"),
                        DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.ordinal(),
                        DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(),
                        DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getComment()
                        )
                ));

    }

    @Test
    @SneakyThrows
    void addNonExistentSortingCenterPropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getTitle(),
                DeliveryServicePropertiesKey.SORT_IN_ADVANCE_ON_SC.getComment(), "0", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addIncorrectTypeOnScPropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.name(),
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(), "24lastmile", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void addCorrectTypeOnScPropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);
        var sortingCenter = testFactory.storedSortingCenter(24L, "Новый СЦ");

        var dto = new LmsDeliveryServicePropertyDetailDto(null, null,
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.name(),
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(), "24:TRANSIT", "111");
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/sortingCenter/deliveryServices/properties/")
                        .param("parentId", String.valueOf(deliveryService.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void changeIncorrectTypeOnScPropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        var properties =
                deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(deliveryService.getYandexId());
        var property = properties.stream().findFirst().get();

        var dto = new LmsDeliveryServicePropertyDetailDto(property.getId(), property.getId(),
                "TYPE_ON_SC_12",
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(),
                "12:TRANSIT",
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/deliveryServices/properties/"
                                + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void changeCorrectTypeOnScPropertyTest() {
        var deliveryService = testFactory.storedDeliveryService("111", true);

        var properties =
                deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(deliveryService.getYandexId());
        var property = properties.stream().findFirst().get();

        var dto = new LmsDeliveryServicePropertyDetailDto(property.getId(), property.getId(),
                "TYPE_ON_SC_12",
                DeliveryServicePropertiesKey.TYPE_ON_SC_PREFIX.getComment(),
                "TRANSIT",
                "111");
        mockMvc.perform(MockMvcRequestBuilders.put("/LMS/sortingCenter/deliveryServices/properties/"
                                + property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(dto)))
                .andExpect(status().is2xxSuccessful());
    }

    private String generateFilterDsPropertiesKey(String key, String keyComment) {
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

    private String generateFilterDs(Long deliveryServiceId, String yandexId, String partnerId, String deliveryServiceName) {
        String reqDsId = deliveryServiceId == null
                ? null
                : "deliveryServiceId=" + deliveryServiceId;
        String reqYandexId = yandexId == null
                ? null
                : "yandexId=" + yandexId;
        String reqPartnerId = partnerId == null
                ? null
                : "partnerId=" + partnerId;
        String reqDeliveryServiceName = deliveryServiceName == null
                ? null
                : "deliveryServiceName=" + deliveryServiceName;

        return Stream.of(reqDsId, reqYandexId, reqPartnerId, reqDeliveryServiceName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "?", ""));
    }

    private String generateFilterDsProperty(Long dsPropertyId, Long deliveryServiceId, DeliveryServicePropertiesKey key,
                                            String value) {
        String reqDsPropertyId = dsPropertyId == null
                ? null
                : "dsPropertyId=" + dsPropertyId;
        String reqDeliveryServiceId = deliveryServiceId == null
                ? null
                : "deliveryServiceId=" + deliveryServiceId;
        String reqKey = key == null
                ? null
                : "key=" + key;
        String reqValue = value == null
                ? null
                : "value=" + value;

        return Stream.of(reqDsPropertyId, reqDeliveryServiceId, reqKey, reqValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "&", ""));
    }

    private String getKeyOptions() {
        return Arrays.stream(DeliveryServicePropertiesKey.values())
                .map(key -> "{" +
                            "\"id\":\"" + key.name() + "\"," +
                            "\"displayName\":\"" + key.getTitle() + "\"," +
                            "\"openNewTab\":" + false + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    @SneakyThrows
    private String readResponse(String file, DeliveryService deliveryService) {
        return String.format(readResponse(file),
                deliveryService.getId(),
                "[" + deliveryService.getPhones().stream().collect(Collectors.joining("\",\"", "\"", "\"")) + "]",
                deliveryService.getPartnerId(),
                deliveryService.getId(),
                deliveryService.getYandexId(),
                deliveryService.getName(),
                deliveryService.getContract());
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
