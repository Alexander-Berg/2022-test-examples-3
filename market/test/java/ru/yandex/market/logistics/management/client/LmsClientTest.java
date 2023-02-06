package ru.yandex.market.logistics.management.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.logistics.management.client.util.DtoFactory;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.MultipleEntitiesActivationRequest;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.geoBase.GeoBaseFilter;
import ru.yandex.market.logistics.management.entity.request.legalInfo.LegalInfoFilter;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerTransportFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partner.UpdatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.LocationResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse;
import ru.yandex.market.logistics.management.entity.response.partner.ContractDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDayOffDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerForbiddenCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerShipmentSettingsDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientPartnerDto;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.XDocPartnerRelationResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.ReturnPointInfoResponse;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeDto;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeRestrictionsDto;
import ru.yandex.market.logistics.management.entity.response.tariff.TariffLocationCargoTypeDto;
import ru.yandex.market.logistics.management.entity.type.AllowedShipmentWay;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ScheduleType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@SuppressWarnings("unchecked")
class LmsClientTest extends AbstractClientTest {

    @Test
    void getRegisterSchedules() {
        mockServer.expect(requestTo(uri + "/export/schedules?from=1&to=2&type=REGISTER_CREATION"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/schedule_response_register.json")));

        Map<Long, List<ScheduleDayResponse>> schedulesMap =
            client.getSchedules(1L, 2L, ScheduleType.REGISTER_CREATION);

        softly.assertThat(schedulesMap.keySet())
            .as("Check keys")
            .containsExactlyInAnyOrder(2L);

        List<ScheduleDayResponse> days = schedulesMap.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        softly.assertThat(days)
            .extracting(
                ScheduleDayResponse::getDay,
                ScheduleDayResponse::getTimeFrom,
                ScheduleDayResponse::getTimeTo
            )
            .as("Check params of schedule days - id, day, time from, time to")
            .containsExactlyInAnyOrder(
                new Tuple(3, LocalTime.of(16, 0), LocalTime.of(18, 0)),
                new Tuple(4, LocalTime.of(6, 0), LocalTime.of(9, 0))
            );
    }

    @Test
    void getIntakeSchedules() {
        mockServer.expect(requestTo(uri + "/export/schedules?from=1&to=2&type=INTAKE_CREATION"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/schedule_response_intake.json")));

        Map<Long, List<ScheduleDayResponse>> schedulesMap =
            client.getSchedules(1L, 2L, ScheduleType.INTAKE_CREATION);

        softly.assertThat(schedulesMap.keySet())
            .as("Check keys")
            .containsExactlyInAnyOrder(1L);

        List<ScheduleDayResponse> days = schedulesMap.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        softly.assertThat(days)
            .extracting(
                ScheduleDayResponse::getDay,
                ScheduleDayResponse::getTimeFrom,
                ScheduleDayResponse::getTimeTo
            )
            .as("Check params of schedule days - id, day, time from, time to")
            .containsExactlyInAnyOrder(
                new Tuple(1, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new Tuple(2, LocalTime.of(12, 0), LocalTime.of(16, 0))
            );
    }

    @Test
    void getEmptySchedules() {
        mockServer.expect(requestTo(uri + "/export/schedules?from=1&to=2&type=REGISTER_CREATION"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_json.json")));

        Map<Long, List<ScheduleDayResponse>> schedules =
            client.getSchedules(1L, 2L, ScheduleType.REGISTER_CREATION);

        softly.assertThat(schedules)
            .as("Should return empty list")
            .hasSize(0);
    }

    @Test
    void getSinglePartnerRelation() {
        mockServer.expect(requestTo(uri + "/export/partnerRelation?from=1&to=2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_relation.json")));

        PartnerRelationResponse partnerRelation = client.getPartnerRelation(1L, 2L)
            .orElse(null);

        softly.assertThat(partnerRelation).isNotNull();
        softly.assertThat(partnerRelation.getId()).isEqualTo(1);
        softly.assertThat(partnerRelation.getFulfillmentId()).isEqualTo(1);
        softly.assertThat(partnerRelation.getDeliveryServiceId()).isEqualTo(2);
        softly.assertThat(partnerRelation.getEnabled()).isTrue();
    }

    @Test
    void getEmptySinglePartnerRelation() {
        mockServer.expect(requestTo(uri + "/export/partnerRelation?from=666&to=777"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(APPLICATION_JSON)
                .body("null"));

        softly.assertThat(client.getPartnerRelation(666L, 777L)).isNotPresent();
    }

    @Test
    void getPartnerRelations() {
        mockServer.expect(requestTo(uri + "/export/partnerRelations"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_relations.json")));

        List<PartnerRelationResponse> partnerRelations = client.getPartnerRelations();

        softly.assertThat(partnerRelations)
            .extracting(
                PartnerRelationResponse::getId,
                PartnerRelationResponse::getFulfillmentId,
                PartnerRelationResponse::getDeliveryServiceId,
                PartnerRelationResponse::getEnabled
            )
            .as("check params of partner relations - id, FF id, DS id, enabled")
            .containsExactlyInAnyOrder(
                new Tuple(1L, 1L, 2L, true),
                new Tuple(2L, 1L, 3L, false)

            );
    }

    @Test
    void getEmptyPartnerRelations() {
        mockServer.expect(requestTo(uri + "/export/partnerRelations"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json")));

        List<PartnerRelationResponse> partnerRelations = client.getPartnerRelations();

        softly.assertThat(partnerRelations)
            .as("Should return empty list")
            .hasSize(0);
    }

    @Test
    void getXDocPartnerRelations() {
        mockServer.expect(requestTo(uri + "/export/xDocPartnerRelations?to=1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_relations_xdoc.json")));

        List<XDocPartnerRelationResponse> partnerRelations = client.getXDocPartnerRelations(1L);

        softly.assertThat(partnerRelations)
            .extracting(
                XDocPartnerRelationResponse::getId,
                XDocPartnerRelationResponse::getFulfillmentId,
                XDocPartnerRelationResponse::getxDocServiceId,
                XDocPartnerRelationResponse::getxDocServiceName,
                XDocPartnerRelationResponse::getHandlingTime,
                XDocPartnerRelationResponse::getEnabled
            )
            .as("check params of partner relations - id, FF id, DS id, capacity, handling time, enabled")
            .containsExactlyInAnyOrder(
                new Tuple(3L, 1L, 4L, "XDoc service 1", "1", true)
            );
    }

    @Test
    void getEmptyXDocPartnerRelations() {
        mockServer.expect(requestTo(uri + "/export/xDocPartnerRelations?to=1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json")));

        List<XDocPartnerRelationResponse> partnerRelations = client.getXDocPartnerRelations(1L);

        softly.assertThat(partnerRelations)
            .as("Should return empty list")
            .hasSize(0);
    }

    @Test
    void getPartnerLegalInfo() {
        mockServer.expect(requestTo(uri + "/externalApi/partner/145/legalInfo"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/legal_info.json"))
            );

        LegalInfoResponse legalInfoResponse = client.getPartnerLegalInfo(145L).orElse(null);

        softly.assertThat(legalInfoResponse).isNotNull();
        softly.assertThat(legalInfoResponse.getId()).isEqualTo(100L);
        softly.assertThat(legalInfoResponse.getPartnerId()).isEqualTo(1L);
        softly.assertThat(legalInfoResponse.getIncorporation()).isEqualTo("ООО ТЕСТ");
        softly.assertThat(legalInfoResponse.getOgrn()).isEqualTo(1000000000000L);
        softly.assertThat(legalInfoResponse.getUrl()).isEqualTo("https://test.ru");
        softly.assertThat(legalInfoResponse.getLegalForm()).isEqualTo("ООО");
        softly.assertThat(legalInfoResponse.getLegalInn()).isEqualTo("7777777777");
        softly.assertThat(legalInfoResponse.getPhone()).isEqualTo("+7(800)700-00-00");
    }

    @Test
    void getNullPartnerLegalInfo() {
        mockServer.expect(requestTo(uri + "/externalApi/partner/666/legalInfo"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body("null")
            );

        client.getPartnerLegalInfo(666L);
    }

    @Test
    void getAllLegalInfo() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/legalInfo"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/legal_info_list.json"))
            );

        List<LegalInfoResponse> legalInfoResponses = client.getAllLegalInfo();

        softly.assertThat(legalInfoResponses.size()).isEqualTo(1);

        LegalInfoResponse legalInfoResponse = legalInfoResponses.get(0);

        softly.assertThat(legalInfoResponse).isNotNull();
        softly.assertThat(legalInfoResponse.getId()).isEqualTo(100L);
        softly.assertThat(legalInfoResponse.getPartnerId()).isEqualTo(1L);
        softly.assertThat(legalInfoResponse.getIncorporation()).isEqualTo("ООО ТЕСТ");
        softly.assertThat(legalInfoResponse.getOgrn()).isEqualTo(1000000000000L);
        softly.assertThat(legalInfoResponse.getUrl()).isEqualTo("https://test.ru");
        softly.assertThat(legalInfoResponse.getLegalForm()).isEqualTo("ООО");
        softly.assertThat(legalInfoResponse.getLegalInn()).isEqualTo("7777777777");
        softly.assertThat(legalInfoResponse.getPhone()).isEqualTo("+7(800)700-00-00");
    }

    @Test
    void getAllNullLegalInfo() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/legalInfo"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<LegalInfoResponse> allLegalInfo = client.getAllLegalInfo();
        softly.assertThat(allLegalInfo)
            .as("Should return empty list").isEmpty();
    }

    @Test
    void searchLegalInfo() {
        mockServer.expect(requestTo(uri + "/externalApi/legal-info/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/legal_info_search_request.json"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/legal_info_search.json"))
            );

        List<LegalInfoResponse> legalInfoResponses = client.searchLegalInfo(
            LegalInfoFilter.builder()
                .setSearchQuery("100")
                .build()
        );

        softly.assertThat(legalInfoResponses.size()).isEqualTo(1);

        LegalInfoResponse legalInfoResponse = legalInfoResponses.get(0);

        softly.assertThat(legalInfoResponse).isNotNull();
        softly.assertThat(legalInfoResponse.getId()).isEqualTo(1);
        softly.assertThat(legalInfoResponse.getPartnerId()).isNull();
        softly.assertThat(legalInfoResponse.getIncorporation()).isEqualTo("ООО ТЕСТ");
        softly.assertThat(legalInfoResponse.getOgrn()).isEqualTo(1000000000000L);
        softly.assertThat(legalInfoResponse.getUrl()).isEqualTo("https://test.ru");
        softly.assertThat(legalInfoResponse.getLegalForm()).isEqualTo("ООО");
        softly.assertThat(legalInfoResponse.getLegalInn()).isEqualTo("7777777777");
        softly.assertThat(legalInfoResponse.getPhone()).isEqualTo("+7(800)700-00-00");
    }

    @Test
    void searchLegalInfoPaged() {
        mockServer.expect(requestTo(startsWith(uri + "/externalApi/legal-info/search-paged")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("size", "2"))
            .andExpect(queryParam("page", "4"))
            .andExpect(jsonContent("data/controller/legal_info_search_request_empty.json"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/legal_info_search_paged.json"))
            );

        PageResult<LegalInfoResponse> legalInfoResponses = client.searchLegalInfo(
            LegalInfoFilter.builder().build(),
            new PageRequest(4, 2)
        );

        softly.assertThat(legalInfoResponses.getPage()).isEqualTo(4);
        softly.assertThat(legalInfoResponses.getSize()).isEqualTo(2);
        softly.assertThat(legalInfoResponses.getTotalElements()).isEqualTo(9);
        softly.assertThat(legalInfoResponses.getTotalPages()).isEqualTo(5);
        softly.assertThat(legalInfoResponses.getData().size()).isEqualTo(1);

        LegalInfoResponse legalInfoResponse = legalInfoResponses.getData().get(0);

        softly.assertThat(legalInfoResponse).isNotNull();
        softly.assertThat(legalInfoResponse.getId()).isEqualTo(1);
        softly.assertThat(legalInfoResponse.getPartnerId()).isNull();
        softly.assertThat(legalInfoResponse.getIncorporation()).isEqualTo("ООО ТЕСТ");
        softly.assertThat(legalInfoResponse.getOgrn()).isEqualTo(1000000000000L);
        softly.assertThat(legalInfoResponse.getUrl()).isEqualTo("https://test.ru");
        softly.assertThat(legalInfoResponse.getLegalForm()).isEqualTo("ООО");
        softly.assertThat(legalInfoResponse.getLegalInn()).isEqualTo("7777777777");
        softly.assertThat(legalInfoResponse.getPhone()).isEqualTo("+7(800)700-00-00");
    }

    @Test
    void getAllCargoTypes() {
        mockServer.expect(requestTo(uri + "/externalApi/cargo-types"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/cargoTypes.json"))
            );

        List<CargoTypeDto> cargoTypes = client.getAllCargoTypes();

        softly.assertThat(cargoTypes)
            .extracting(CargoTypeDto::getId, CargoTypeDto::getCargoType, CargoTypeDto::getDescription)
            .as("Should return 3 entities with exact fields")
            .containsExactlyInAnyOrder(
                new Tuple(1L, 123, "first cargo type"),
                new Tuple(2L, 456, "second cargo type"),
                new Tuple(3L, 789, "third cargo type")
            );
    }

    @Test
    void getAllCargoTypesEmpty() {
        mockServer.expect(requestTo(uri + "/externalApi/cargo-types"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<CargoTypeDto> cargoTypes = client.getAllCargoTypes();
        softly.assertThat(cargoTypes)
            .as("Should return empty list").isEmpty();
    }

    @Test
    void createLogisticsPointSuccessful() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_logistics_point_successful.json")))
            .andRespond(withStatus(CREATED)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/created_logistics_point.json"))
            );

        LogisticsPointResponse response = client.createLogisticsPoint(getPointRequestBuilder().build());

        softly.assertThat(response).isNotNull();
        softly.assertThat(response)
            .isEqualToComparingFieldByFieldRecursively(getLogisticsPointResponse());
    }

    @Test
    void createLogisticsPointWithoutAddress() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_logistics_point_without_address.json")))
            .andRespond(withStatus(BAD_REQUEST));

        softly.assertThatThrownBy(() -> client.createLogisticsPoint(
                getPointRequestBuilder()
                    .cashAllowed(true)
                    .address(null).build()))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + BAD_REQUEST.value() + ">");
    }

    @Test
    void updateLogisticsPointSuccessful() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/1"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/update_logistics_point_successful.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/updated_logistics_point.json"))
            );

        LogisticsPointResponse logisticsPointResponse = client.updateLogisticsPoint(
            1L, getPointUpdateRequestBuilder().build()
        );

        softly.assertThat(logisticsPointResponse).isNotNull();
        softly.assertThat(logisticsPointResponse)
            .isEqualToComparingFieldByFieldRecursively(getUpdatedLogisticsPointResponse());
    }

    @Test
    void updateNonexistentLogisticsPoint() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/2315"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/update_logistics_point_successful.json")))
            .andRespond(withStatus(NOT_FOUND));

        softly.assertThatThrownBy(() -> client.updateLogisticsPoint(
                2315L,
                getPointUpdateRequestBuilder().build()
            ))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + NOT_FOUND.value() + ">");
    }

    @Test
    void deactivateLogisticsPointSuccessful() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/deactivate/1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/deactivated_logistics_point.json"))
            );

        LogisticsPointResponse logisticsPointResponse = client.deactivateLogisticsPoint(1L);

        softly.assertThat(logisticsPointResponse).isNotNull();
        softly.assertThat(logisticsPointResponse)
            .isEqualToComparingFieldByFieldRecursively(getDeactivatedLogisticsPointResponse());
    }

    @Test
    void deactivateNonexistentLogisticsPoint() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/deactivate/1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(NOT_FOUND));

        softly.assertThatThrownBy(() -> client.deactivateLogisticsPoint(1L))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + NOT_FOUND.value() + ">");
    }

    @TestFactory
    List<DynamicTest> cargoTypesTest() {
        return ImmutableList.of(
            DynamicTest.dynamicTest(
                "tariff cargo type found, tariff locations empty",
                () -> cargoTypesTestTemplate(11, 36488, 1, 0)
            ),

            DynamicTest.dynamicTest(
                "tariff cargo type empty, tariff locations found",
                () -> cargoTypesTestTemplate(12, 33254, 0, 1)
            ),

            DynamicTest.dynamicTest(
                "tariff cargo type empty, tariff locations empty",
                () -> cargoTypesTestTemplate(13, 31745, 0, 0)
            ),

            DynamicTest.dynamicTest(
                "tariff cargo type found, tariff locations found",
                () -> cargoTypesTestTemplate(14, 10515904, 2, 5)
            ),

            DynamicTest.dynamicTest(
                "tariff not exist",
                () -> cargoTypesTestTemplate(15, 29823, 0, 0)
            )
        );
    }

    void cargoTypesTestTemplate(long tariffId, int hash, int tariffCargoTypesCount, int locationsCargoTypesCount) {
        mockServer.expect(requestTo(String.format("%s/externalApi/tariff/%s/cargo-types", uri, tariffId)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(String.format("data/controller/tariff_%s.json", tariffId)))
            );

        CargoTypeRestrictionsDto cargoTypesByTariffId = client.getCargoTypesByTariffId(tariffId);
        softly.assertThat(cargoTypesByTariffId.getHash()).as("Check hash").isEqualTo(hash);

        softly.assertThat(cargoTypesByTariffId.getCargoTypes())
            .as("Check tariff cargo types count").hasSize(tariffCargoTypesCount);

        int actualLocationCargoTypeCount = cargoTypesByTariffId.getTariffLocationCargoTypes().stream()
            .map(TariffLocationCargoTypeDto::getCargoTypes)
            .mapToInt(List::size)
            .sum();
        softly.assertThat(actualLocationCargoTypeCount)
            .as("Check locations cargo types count").isEqualTo(locationsCargoTypesCount);

        mockServer.verify();
        mockServer.reset();
    }

    @Test
    void getCutoffs() {
        mockServer.expect(requestTo(uri + "/export/cutoffs?from=1&to=2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/cutoffs.json"))
            );

        List<CutoffResponse> cutoffs = client.getCutoffs(1L, 2L);

        softly.assertThat(cutoffs)
            .as("Should return 2 entities with exact fields")
            .containsExactlyInAnyOrder(
                CutoffResponse.newBuilder()
                    .id(1L)
                    .locationId(213)
                    .cutoffTime(LocalTime.of(17, 0))
                    .packagingDuration(Duration.ofMinutes(1))
                    .build(),
                CutoffResponse.newBuilder()
                    .id(2L)
                    .locationId(225)
                    .cutoffTime(LocalTime.of(19, 0))
                    .packagingDuration(Duration.ofMinutes(10))
                    .build()
            );
    }

    @Test
    void getCutoffsPartnerRelationNotExist() {
        mockServer.expect(requestTo(uri + "/export/cutoffs?from=100&to=200"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<CutoffResponse> cutoffs = client.getCutoffs(100L, 200L);

        softly.assertThat(cutoffs)
            .as("Should return empty list")
            .isEmpty();
    }

    @Test
    void getLogisticsPointById() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_point_1.json"))
            );

        LogisticsPointResponse logisticsPointResponse = client.getLogisticsPoint(1L).orElse(null);

        softly.assertThat(logisticsPointResponse).isNotNull();
        softly.assertThat(logisticsPointResponse)
            .isEqualToComparingFieldByFieldRecursively(getLogisticsPointResponse());
    }

    @Test
    void getLogisticsPointByNonexistentId() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/2315"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body("null"));

        softly.assertThat(client.getLogisticsPoint(2315L)).isNotPresent();
    }

    @Test
    void getPartnersBySettingsApiTokenSuccessful() {
        mockServer.expect(requestTo(uri + "/partners?token=token"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partners_by_token.json"))
            );

        softly.assertThat(client.getPartnersByApiSettingsToken("token"))
            .as("There two partners with this token")
            .hasSize(2)
            .as("Partners have correct fields")
            .containsExactlyInAnyOrder(
                new PartnerDto(1L, "fulfillment"),
                new PartnerDto(2L, "delivery")
            );
    }

    @Test
    void searchPartners() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partners.json")));

        List<PartnerResponse> partners = client.searchPartners(SearchPartnerFilter.builder().build());

        softly.assertThat(partners).hasSize(4);

        softly.assertThat(partners.get(0)).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(1L)
                .marketId(829721L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("FulfillmentService1")
                .readableName("Fulfillment service 1")
                .codeName("fulfillment_service_1")
                .abbreviation("ФФ СЦ №1 по РФ")
                .status(PartnerStatus.ACTIVE)
                .locationId(255)
                .trackingType("tt1")
                .billingClientId(123L)
                .rating(1)
                .domain("first.ff.example.com")
                .logoUrl(null)
                .params(ImmutableList.of())
                .intakeSchedule(ImmutableList.of())
                .build()
        );
        softly.assertThat(partners.get(1)).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(2L)
                .marketId(829722L)
                .businessId(2222L)
                .partnerType(PartnerType.DELIVERY)
                .name("DeliveryService1")
                .readableName("Delivery service 1")
                .status(PartnerStatus.ACTIVE)
                .locationId(255)
                .trackingType("tt2")
                .billingClientId(123L)
                .rating(1)
                .domain("first.ds.example.com")
                .build()
        );
        softly.assertThat(partners.get(2)).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(3L)
                .marketId(829723L)
                .businessId(3333L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("FulfillmentService2")
                .readableName("Fulfillment service 2")
                .status(PartnerStatus.INACTIVE)
                .locationId(255)
                .trackingType("tt3")
                .billingClientId(1234L)
                .rating(12)
                .stockSyncEnabled(false)
                .autoSwitchStockSyncEnabled(false)
                .build());

        softly.assertThat(partners.get(3)).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(4L)
                .marketId(829724L)
                .businessId(4444L)
                .partnerType(PartnerType.DELIVERY)
                .name("DeliveryService2")
                .readableName("Delivery service 2")
                .status(PartnerStatus.FROZEN)
                .locationId(255)
                .trackingType("tt4")
                .billingClientId(1234L)
                .rating(12)
                .domain("second.ds.example.com")
                .params(ImmutableList.of(
                    new PartnerExternalParam(
                        PartnerExternalParamType.LOGO.toString(),
                        "Ссылка на логотип службы",
                        "https://second.ds.example.com/logo.png"
                    ),
                    new PartnerExternalParam(
                        PartnerExternalParamType.IS_COMMON.toString(),
                        "Является ли служба обыкновенной",
                        "false"
                    )
                ))
                .intakeSchedule(
                    ImmutableList.of(
                        new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                        new ScheduleDayResponse(2L, 2, LocalTime.of(12, 0), LocalTime.of(16, 0))
                    )
                )
                .build());
    }

    @Test
    void searchPartnersPaged() {
        mockServer.expect(requestTo(startsWith(uri + "/externalApi/partners/search-paged")))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "2"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partners_page.json")));

        PageResult<PartnerResponse> apiResponse = client.searchPartners(
            SearchPartnerFilter.builder().build(),
            new PageRequest(0, 2)
        );

        softly.assertThat(apiResponse)
            .isEqualTo(
                new PageResult<PartnerResponse>()
                    .setData(List.of(
                        PartnerResponse.newBuilder()
                            .id(1L)
                            .marketId(829721L)
                            .partnerType(PartnerType.FULFILLMENT)
                            .name("FulfillmentService1")
                            .readableName("Fulfillment service 1")
                            .codeName("fulfillment_service_1")
                            .abbreviation("ФФ СЦ №1 по РФ")
                            .status(PartnerStatus.ACTIVE)
                            .locationId(255)
                            .trackingType("tt1")
                            .billingClientId(123L)
                            .rating(1)
                            .domain("first.ff.example.com")
                            .logoUrl(null)
                            .params(ImmutableList.of())
                            .intakeSchedule(ImmutableList.of())
                            .build(),
                        PartnerResponse.newBuilder()
                            .id(2L)
                            .marketId(829722L)
                            .businessId(2222L)
                            .partnerType(PartnerType.DELIVERY)
                            .name("DeliveryService1")
                            .readableName("Delivery service 1")
                            .status(PartnerStatus.ACTIVE)
                            .locationId(255)
                            .trackingType("tt2")
                            .billingClientId(123L)
                            .rating(1)
                            .domain("first.ds.example.com")
                            .build()
                    ))
                    .setPage(0)
                    .setSize(2)
                    .setTotalPages(2)
                    .setTotalElements(4)
            );
    }

    @Test
    void getPartnersWithFilter() {
        mockServer.expect(requestTo(startsWith(
                getBuilder(uri, "/externalApi/partners/search").build().toUriString()))
            )
            .andExpect(jsonContent("data/controller/filtered_partners_request.json"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/filtered_partners.json")));

        SearchPartnerFilter filter = SearchPartnerFilter.builder()
            .setTypes(ImmutableSet.of(PartnerType.FULFILLMENT))
            .setStatuses(ImmutableSet.of(PartnerStatus.ACTIVE))
            .setMarketIds(ImmutableSet.of(155L, 523L))
            .setBusinessIds(ImmutableSet.of(2222L))
            .setPlatformClientIds(ImmutableSet.of(632L, 747L, 973L))
            .setIds(ImmutableSet.of(1L))
            .build();
        List<PartnerResponse> partners = client.searchPartners(filter);

        softly.assertThat(partners).hasSize(1);

        softly.assertThat(partners.get(0)).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(1L)
                .marketId(829721L)
                .businessId(2222L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("FulfillmentService1")
                .readableName("Fulfillment service 1")
                .status(PartnerStatus.ACTIVE)
                .locationId(255)
                .trackingType("tt1")
                .billingClientId(123L)
                .rating(1)
                .stockSyncEnabled(false)
                .autoSwitchStockSyncEnabled(false)
                .realSupplierId(null)
                .build());
    }

    @Test
    void getPartner() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partner = client.getPartner(1L).orElse(null);

        softly.assertThat(partner).isNotNull();
        softly.assertThat(partner).isEqualToComparingFieldByFieldRecursively(
            PartnerResponse.newBuilder()
                .id(1L)
                .marketId(829721L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("FulfillmentService1")
                .readableName("Fulfillment service 1")
                .status(PartnerStatus.ACTIVE)
                .locationId(255)
                .trackingType("tt1")
                .billingClientId(123L)
                .rating(1)
                .stockSyncEnabled(false)
                .autoSwitchStockSyncEnabled(false)
                .platformClients(ImmutableList.of(
                    PlatformClientDto.newBuilder()
                        .name("Beru")
                        .id(1L)
                        .status(PartnerStatus.ACTIVE)
                        .build(),
                    PlatformClientDto.newBuilder()
                        .name("Bringly")
                        .id(2L)
                        .status(PartnerStatus.ACTIVE)
                        .build()
                ))
                .balanceContract(
                    ContractDto.newBuilder()
                        .id(1)
                        .serviceContractId(32L)
                        .externalId("EXTERNAL_ID")
                        .build()
                )
                .build());
    }

    @Test
    void addHolidays() {
        mockServer.expect(requestTo(uri + "/externalApi/calendar/1/addHolidays"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/holidays.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/holidays_response.json")));

        client.addHolidays(
            1L,
            Set.of(LocalDate.of(2022, 7, 25), LocalDate.of(2022, 7, 26))
        );
        mockServer.verify();
    }

    @Test
    void removeHolidays() {
        mockServer.expect(requestTo(uri + "/externalApi/calendar/1/removeHolidays"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/holidays.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/holidays_response.json")));

        client.removeHolidays(
            1L,
            Set.of(LocalDate.of(2022, 7, 25), LocalDate.of(2022, 7, 26))
        );
        mockServer.verify();
    }

    @Test
    void getPartnerCapacities() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/capacities_response.json")));

        List<PartnerCapacityDto> partnerCapacities = client.getPartnerCapacities();
        assertCapacities(partnerCapacities);
    }

    @Test
    void getPartnerCapacitiesByPartner() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/1/capacity"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/capacities_response.json")));

        List<PartnerCapacityDto> partnerCapacities = client.getPartnerCapacities(1L);
        assertCapacities(partnerCapacities);
    }

    @Test
    void createCapacity() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/capacity_dto_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/capacity_dto_response.json")));

        PartnerCapacityDto capacity = client.createCapacity(buildPartnerCapacityDto());
        softly.assertThat(capacity).extracting(PartnerCapacityDto::getId).isEqualTo(777L);
    }

    @Test
    void createInvalidCapacity() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/capacity_dto_request.json"), true))
            .andRespond(withStatus(NOT_FOUND));

        try {
            client.createCapacity(buildPartnerCapacityDto());
            softly.failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpTemplateException e) {
            softly.assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND.value());
        }
    }

    @Test
    void updateCapacity() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities/777/value?value=100"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/capacity_dto_response.json")));

        PartnerCapacityDto partnerCapacityDto = client.updateCapacityValue(777L, 100L);

        softly.assertThat(partnerCapacityDto).extracting(PartnerCapacityDto::getId).isEqualTo(777L);
        softly.assertThat(partnerCapacityDto).extracting(PartnerCapacityDto::getValue).isEqualTo(100L);
    }

    @Test
    void updateNonExistedCapacity() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities/10/value?value=20"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(NOT_FOUND));

        try {
            client.updateCapacityValue(10L, 20L);
            softly.failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpTemplateException e) {
            softly.assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND.value());
        }
    }

    @Test
    void searchCapacity() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-capacities/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/capacity_filter.json"), true))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/capacities_response.json")));

        List<PartnerCapacityDto> partnerCapacityDtos = client.searchCapacity(buildPartnerCapacityFilter());
        softly.assertThat(partnerCapacityDtos).hasSize(3);
    }

    @Test
    void createPartnerCapacityDayOff() {
        String expectedDay = "2019-05-04";
        LocalDate day = LocalDate.parse(expectedDay);
        long capacityId = 1;

        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner-capacities/1/days-off")
                    .queryParam("day", expectedDay)
                    .toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/newDayOff_response.json")));

        PartnerCapacityDayOffDto dayOffDto = client.createPartnerCapacityDayOff(capacityId, day);

        softly.assertThat(dayOffDto).isNotNull();
        softly.assertThat(dayOffDto).isEqualToComparingFieldByFieldRecursively(
            new PartnerCapacityDayOffDto(
                7L,
                day,
                capacityId
            )
        );
    }

    @Test
    void deletePartnerCapacityDayOff() {
        String expectedDay = "2019-05-01";
        LocalDate day = LocalDate.parse(expectedDay);
        long capacityId = 1;

        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner-capacities/1/days-off")
                    .queryParam("day", expectedDay)
                    .toUriString()))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(NO_CONTENT));

        client.deletePartnerCapacityDayOff(capacityId, day);
    }

    @Test
    void getPartnerCargoTypes() {
        List<Long> partnersToTest = new ArrayList<>();
        partnersToTest.add(145L);
        partnersToTest.add(147L);
        partnersToTest.add(171L);
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/cargoTypes")
                    .toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_cargo_types_response.json")));
        List<PartnerCargoTypesDto> partnerCargoTypes = client.getPartnerCargoTypes(partnersToTest);

        softly.assertThat(partnerCargoTypes).hasSize(3);
        Map<Long, Set<Integer>> byPartnerMarketId =
            partnerCargoTypes.stream().collect(Collectors.toMap(
                PartnerCargoTypesDto::getPartnerMarketId,
                PartnerCargoTypesDto::getCargoTypes
            ));
        softly.assertThat(byPartnerMarketId).hasSize(3);
        softly.assertThat(byPartnerMarketId.get(145L)).containsExactlyInAnyOrder(300, 320);
        softly.assertThat(byPartnerMarketId.get(147L)).containsExactlyInAnyOrder(300);
        softly.assertThat(byPartnerMarketId.get(171L)).isEmpty();
    }

    @Test
    void getPartnerForbiddenCargoTypes() {
        List<Long> partnersToTest = new ArrayList<>();
        partnersToTest.add(1L);
        partnersToTest.add(2L);
        partnersToTest.add(3L);
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/forbiddenCargoTypes").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_forbidden_cargo_types_response.json"))
            );
        List<PartnerForbiddenCargoTypesDto> partnerForbiddenCargoTypes =
            client.getPartnerForbiddenCargoTypes(partnersToTest);

        softly.assertThat(partnerForbiddenCargoTypes).hasSize(3);
        Map<Long, Set<Integer>> byPartnerMarketId =
            partnerForbiddenCargoTypes.stream().collect(Collectors.toMap(
                PartnerForbiddenCargoTypesDto::getPartnerId,
                PartnerForbiddenCargoTypesDto::getForbiddenCargoTypes
            ));
        softly.assertThat(byPartnerMarketId).hasSize(3);
        softly.assertThat(byPartnerMarketId.get(1L)).containsExactlyInAnyOrder(300, 320);
        softly.assertThat(byPartnerMarketId.get(2L)).containsExactlyInAnyOrder(300);
        softly.assertThat(byPartnerMarketId.get(3L)).isEmpty();
    }

    @Test
    void addPartnerForbiddenCargoTypes() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/666/forbiddenCargoTypes").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonContent("data/controller/add_partner_forbidden_cargo_types_request.json"))
            .andRespond(withStatus(OK));

        client.addPartnerForbiddenCargoTypes(666L, ImmutableSet.of(300, 320));
    }

    @Test
    void removePartnerForbiddenCargoTypes() {
        mockServer.expect(requestTo(getBuilder(
                uri,
                "/externalApi/partners/666/removeForbiddenCargoTypes"
            ).toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonContent("data/controller/add_partner_forbidden_cargo_types_request.json"))
            .andRespond(withStatus(OK));

        client.removePartnerForbiddenCargoTypes(666L, ImmutableSet.of(300L, 320L));
        mockServer.verify();
    }

    @Test
    void changeSyncStocksEnabled() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner/1/change-stock-sync")
                    .queryParam("enabled", true)
                    .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK));
        client.changeStockSync(1L, true, null);
        mockServer.verify();
    }

    @Test
    void changeSyncStocksDisabled() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner/1/change-stock-sync")
                    .queryParam("enabled", false)
                    .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK));
        client.changeStockSync(1L, false, null);
        mockServer.verify();
    }

    @Test
    void changeSyncStocksDisabledWithReason() {
        mockServer.expect(requestTo(startsWith(
                getBuilder(uri, "/externalApi/partner/1/change-stock-sync").toUriString()
            )))
            .andExpect(queryParam("enabled", "false"))
            .andExpect(queryParam("reason", StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL.name()))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK));
        client.changeStockSync(1L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
        mockServer.verify();
    }

    @Test
    void changeSyncStocks404() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner/3/change-stock-sync")
                    .queryParam("enabled", false)
                    .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(NOT_FOUND));

        softly.assertThatThrownBy(() ->
                client.changeStockSync(3L, false, null))
            .isExactlyInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + NOT_FOUND.value() + ">");
        mockServer.verify();
    }

    @Test
    void updatePartnerSetting() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/settings")
                    .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/partner_setting.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_setting.json")));

        PartnerSettingDto response = client.updatePartnerSettings(1L, createPartnerSetting());

        mockServer.verify();
        softly.assertThat(response).as("Should parse response properly").isEqualTo(createPartnerSetting());
    }

    @Test
    void updatePartnerSettingPartnerNotFound() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/100500/settings").toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/partner_setting.json")))
            .andRespond(withStatus(NOT_FOUND));

        softly.assertThatThrownBy(() -> client.updatePartnerSettings(100500L, createPartnerSetting()))
            .isExactlyInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + NOT_FOUND.value() + ">");
        mockServer.verify();
    }

    @Test
    void createPartner() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_partner/create_partner_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/create_partner/create_partner_response.json")));

        PartnerResponse partner = client.createPartner(getCreatePartnerRequest());

        softly.assertThat(partner).as("Proper partner should be returned")
            .isEqualToComparingFieldByField(PartnerResponse.newBuilder()
                .id(47723)
                .marketId(829725L)
                .partnerType(PartnerType.DELIVERY)
                .status(PartnerStatus.INACTIVE)
                .name("partner")
                .readableName("Partner Partner")
                .billingClientId(0L)
                .build()
            );
    }

    @Test
    void updatePartner() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1")
                    .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/update_partner_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partner = client.updatePartner(1L, getUpdatePartnerRequest());

        softly.assertThat(partner).isEqualToComparingFieldByFieldRecursively(
            getPartnerResponse());
    }

    @Nonnull
    private PartnerResponse getPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(1L)
            .marketId(829721L)
            .partnerType(PartnerType.FULFILLMENT)
            .name("FulfillmentService1")
            .readableName("Fulfillment service 1")
            .status(PartnerStatus.ACTIVE)
            .locationId(255)
            .trackingType("tt1")
            .billingClientId(123L)
            .rating(1)
            .stockSyncEnabled(false)
            .autoSwitchStockSyncEnabled(false)
            .platformClients(ImmutableList.of(
                PlatformClientDto.newBuilder()
                    .id(1L)
                    .name("Beru")
                    .status(PartnerStatus.ACTIVE)
                    .build(),
                PlatformClientDto.newBuilder()
                    .id(2L)
                    .name("Bringly")
                    .status(PartnerStatus.ACTIVE)
                    .build()
            ))
            .balanceContract(
                ContractDto.newBuilder()
                    .id(1)
                    .serviceContractId(32L)
                    .externalId("EXTERNAL_ID")
                    .build()
            )
            .build();
    }

    @Test
    void createPartnerNotValid() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(anything())
            .andRespond(withStatus(BAD_REQUEST)
                .contentType(APPLICATION_JSON));

        softly.assertThatThrownBy(() ->
                client.createPartner(getCreatePartnerRequest()))
            .isExactlyInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <" + BAD_REQUEST.value() + ">");
        mockServer.verify();
    }

    @Test
    void getPartnerTypeOptions() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/partnerTypeOptions").toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_type_options.json")));

        List<PartnerType> partnerTypes = client.getPartnerTypeOptions();

        softly.assertThat(partnerTypes).containsExactlyInAnyOrder(PartnerType.DELIVERY, PartnerType.FULFILLMENT);
    }

    @Test
    void getPartnerSubtypeOptions() {
        mockServer.expect(requestTo(startsWith(
                getBuilder(uri, "/externalApi/partners/partnerSubtypeOptions").toUriString()
            )))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("partnerType", "FULFILLMENT"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_subtype_options.json")));

        List<PartnerSubtypeResponse> partnerSubtypes = client.getPartnerSubtypeOptions(PartnerType.FULFILLMENT);

        softly.assertThat(partnerSubtypes).containsExactlyInAnyOrder(
            PartnerSubtypeResponse.newBuilder().id(1).name("Market courier").build(),
            PartnerSubtypeResponse.newBuilder().id(2).name("Market PVZ").build()
        );
    }

    @Test
    void getPlatformClientOptions() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/platformClientOptions").toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/platform_client_options.json")));

        List<PlatformClientResponse> platformClients = client.getPlatformClientOptions();

        softly.assertThat(platformClients).containsExactlyInAnyOrder(
            PlatformClientResponse.builder().id(1L).name("Beru").build(),
            PlatformClientResponse.builder().id(2L).name("Bringly").build(),
            PlatformClientResponse.builder().id(3L).name("Yandex Delivery").build()
        );
    }

    @Test
    void addPlatformClient() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/platform-clients")
                    .queryParam("platformClientId", "1")
                    .queryParam("status", "ACTIVE")
                    .toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partnerResponse = client.setPlatformClient(1L, 1L, PartnerStatus.ACTIVE);

        softly.assertThat(partnerResponse).isEqualToComparingFieldByFieldRecursively(
            getPartnerResponse());
    }

    @Test
    void addPlatformClientWithShipment() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/platform-clients").toUriString()
            ))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/platform_clients_with_shipment.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partnerResponse = client.addOrUpdatePlatformClientPartner(
            PlatformClientPartnerDto.newBuilder()
                .partnerId(8L)
                .platformClientId(3903L)
                .status(PartnerStatus.ACTIVE)
                .shipmentSettings(ImmutableSet.of(
                    PartnerShipmentSettingsDto.newBuilder()
                        .shipmentType(ShipmentType.WITHDRAW)
                        .allowedShipmentWay(AllowedShipmentWay.DIRECTLY)
                        .build()
                ))
                .build()
        );

        softly.assertThat(partnerResponse).isEqualToComparingFieldByFieldRecursively(getPartnerResponse());
    }

    @Test
    void addPlatformClientWithNullStatus() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/platform-clients")
                    .queryParam("platformClientId", "1")
                    .toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partnerResponse = client.setPlatformClient(1L, 1L, null);

        softly.assertThat(partnerResponse).isEqualToComparingFieldByFieldRecursively(
            getPartnerResponse());
    }

    @Test
    void changePartnerStatus() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/changeStatus")
                    .toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/update_partner_status_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner.json")));

        PartnerResponse partner = client.changePartnerStatus(1L, PartnerStatus.INACTIVE);

        softly.assertThat(partner).isEqualToComparingFieldByFieldRecursively(
            getPartnerResponse());
    }

    @Test
    void getPartnerTransport() {
        mockServer.expect(requestTo(
                getBuilder(uri, "externalApi/partnerTransport/search").toUriString()
            ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/partner_transport_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_transport_response.json"))
            );
        List<PartnerTransportDto> partnerTransport = client.getPartnerTransport(PartnerTransportFilter.builder()
            .logisticsPointFrom(1L)
            .build());

        List<PartnerTransportDto> expectedTransports = List.of(PartnerTransportDto.newBuilder()
            .logisticsPointFrom(LogisticsPointResponse.newBuilder().id(1L).build())
            .logisticsPointTo(LogisticsPointResponse.newBuilder().id(2L).build())
            .partner(PartnerResponse.newBuilder().id(5L).build())
            .palletCount(7)
            .price(5L)
            .duration(Duration.ofHours(24))
            .build()
        );

        softly.assertThat(partnerTransport).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedTransports);
    }

    @Test
    void getWarehouseHandlingDuration() {
        String uriString = getBuilder(uri, "externalApi/warehouse-handling-duration/420").toUriString();

        mockServer.expect(requestTo(uriString))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body("\"PT5M\""));

        Duration duration = client.getWarehouseHandlingDuration(420L);
        softly.assertThat(duration).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void updateWarehouseHandlingDuration() {
        String uriString = getBuilder(uri, "externalApi/warehouse-handling-duration/1").toUriString();

        mockServer.expect(requestTo(uriString))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(content().json(jsonResource("data/controller/warehouse_handling_duration_update.json")))
            .andRespond(withStatus(OK));

        client.updateWarehouseHandlingDuration(1L, Duration.ofMinutes(10));

        mockServer.verify();
    }

    @Test
    void searchLocation() {
        mockServer.expect(requestTo(uri + "/geobase/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/location_search_request.json"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/location_search.json"))
            );

        List<LocationResponse> locationResponses = client.searchLocations(
            GeoBaseFilter.builder()
                .setSearchQuery("100")
                .build()
        );

        softly.assertThat(locationResponses.size()).isEqualTo(1);

        LocationResponse locationResponse = locationResponses.get(0);

        softly.assertThat(locationResponse).isNotNull();
        softly.assertThat(locationResponse.getId()).isEqualTo(100L);
        softly.assertThat(locationResponse.getName()).isEqualTo("Санкт-Петербург");
    }

    @Test
    void searchLocationPaged() {
        mockServer.expect(requestTo(startsWith(uri + "/geobase/search-paged")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("size", "2"))
            .andExpect(queryParam("page", "4"))
            .andExpect(jsonContent("data/controller/location_search_request_empty.json"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/location_search_paged.json"))
            );

        PageResult<LocationResponse> locationResponses = client.searchLocations(
            GeoBaseFilter.builder().build(),
            new PageRequest(4, 2)
        );

        softly.assertThat(locationResponses.getPage()).isEqualTo(4);
        softly.assertThat(locationResponses.getSize()).isEqualTo(2);
        softly.assertThat(locationResponses.getTotalElements()).isEqualTo(9);
        softly.assertThat(locationResponses.getTotalPages()).isEqualTo(5);
        softly.assertThat(locationResponses.getData().size()).isEqualTo(1);

        LocationResponse locationResponse = locationResponses.getData().get(0);

        softly.assertThat(locationResponse).isNotNull();
        softly.assertThat(locationResponse.getId()).isEqualTo(100L);
        softly.assertThat(locationResponse.getName()).isEqualTo("Санкт-Петербург");
    }

    @Test
    void activateMultipleEntities() {
        mockServer.expect(requestTo(uri + "/externalApi/activate-multiple-entities"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/activate_multiple_entities_request.json", false, false))
            .andRespond(withStatus(OK));

        MultipleEntitiesActivationRequest request = MultipleEntitiesActivationRequest.newBuilder()
            .partnerIds(Set.of(1L, 3L))
            .partnerIdsForActivationWithAllLogisticPoints(Set.of(2L))
            .logisticPointIds(Set.of(10002L, 10007L))
            .logisticSegmentIds(Set.of(1L, 2L, 8L))
            .partnerRelationIds(Set.of(1L))
            .build();

        client.activateMultipleEntities(request);
    }

    @Test
    void getReturnPointForPartner() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/returnPointForPartner/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/return-point-for-partner.json"))
            );

        ReturnPointInfoResponse returnPointForPartner = client.getReturnPointForPartner(1L).get();
        softly.assertThat(returnPointForPartner.getPartnerId()).isEqualTo(6L);
        softly.assertThat(returnPointForPartner.getId()).isEqualTo(10L);
        softly.assertThat(returnPointForPartner.isDropoff()).isTrue();
        softly.assertThat(returnPointForPartner.getMovementPartnerId()).isEqualTo(4L);
        softly.assertThat(returnPointForPartner.getMovementPartnerName()).isEqualTo("Movement Partner");
    }

    private void assertCapacities(List<PartnerCapacityDto> partnerCapacities) {
        softly.assertThat(partnerCapacities).hasSize(3);

        softly.assertThat(partnerCapacities.get(0)).isEqualToComparingFieldByFieldRecursively(
            PartnerCapacityDto.newBuilder()
                .id(1L)
                .partnerId(1L)
                .locationFrom(11)
                .locationTo(12)
                .deliveryType(DeliveryType.COURIER)
                .type(CapacityType.REGULAR)
                .countingType(CountingType.ORDER)
                .capacityService(CapacityService.DELIVERY)
                .platformClientId(3901L)
                .day(LocalDate.parse("2019-01-01"))
                .value(100L)
                .build()
        );
        softly.assertThat(partnerCapacities.get(1)).isEqualToComparingFieldByFieldRecursively(
            PartnerCapacityDto.newBuilder()
                .id(2L)
                .partnerId(1L)
                .locationFrom(21)
                .locationTo(22)
                .deliveryType(DeliveryType.PICKUP)
                .type(CapacityType.REGULAR)
                .countingType(CountingType.ORDER)
                .capacityService(CapacityService.DELIVERY)
                .platformClientId(3902L)
                .day(LocalDate.parse("2019-01-02"))
                .value(200L)
        );
        softly.assertThat(partnerCapacities.get(2)).isEqualToComparingFieldByFieldRecursively(
            PartnerCapacityDto.newBuilder()
                .id(3L)
                .partnerId(1L)
                .locationFrom(31)
                .locationTo(32)
                .deliveryType(DeliveryType.POST)
                .type(CapacityType.RESERVE)
                .countingType(CountingType.ITEM)
                .capacityService(CapacityService.INBOUND)
                .platformClientId(3903L)
                .day(LocalDate.parse("2019-01-03"))
                .value(300L)
        );
    }

    @Nonnull
    private CreatePartnerDto getCreatePartnerRequest() {
        return CreatePartnerDto.newBuilder()
            .marketId(829725L)
            .name("partner")
            .readableName("Partner Partner")
            .partnerType(PartnerType.DELIVERY)
            .build();
    }

    private UpdatePartnerDto getUpdatePartnerRequest() {
        return UpdatePartnerDto.newBuilder()
            .name("UpdatedName")
            .readableName("UpdatedReadableName")
            .status(PartnerStatus.INACTIVE)
            .billingClientId(999L)
            .rating(100)
            .domain("UpdatedDomain")
            .logoUrl("UpdatedUrl")
            .build();
    }

    private PartnerSettingDto createPartnerSetting() {
        return PartnerSettingDto.newBuilder()
            .trackingType("post_reg")
            .locationId(100500)
            .stockSyncEnabled(true)
            .autoSwitchStockSyncEnabled(true)
            .korobyteSyncEnabled(true)
            .build();
    }

    private PartnerCapacityDto buildPartnerCapacityDto() {
        return PartnerCapacityDto.newBuilder()
            .partnerId(1L)
            .locationFrom(2)
            .locationTo(3)
            .deliveryType(DeliveryType.POST)
            .type(CapacityType.REGULAR)
            .capacityService(CapacityService.DELIVERY)
            .platformClientId(5L)
            .value(100L)
            .build();
    }

    private PartnerCapacityFilter buildPartnerCapacityFilter() {
        return PartnerCapacityFilter.newBuilder()
            .values(Set.of(100L))
            .days(Sets.newHashSet(null, LocalDate.of(2019, 1, 1)))
            .ids(Sets.newHashSet(1L, 2L, 3L))
            .deliveryTypes(Sets.newHashSet(null, DeliveryType.COURIER))
            .locationsFrom(Sets.newHashSet(10L, 20L, 30L))
            .locationsTo(Sets.newHashSet(40L, 50L, 60L))
            .types(Collections.singleton(CapacityType.REGULAR))
            .countingTypes(Collections.singleton(CountingType.ITEM))
            .capacityServices(new LinkedHashSet<>(Arrays.asList(CapacityService.DELIVERY, CapacityService.INBOUND)))
            .platformClientIds(Sets.newHashSet(5L, 6L, 7L))
            .partnerIds(Sets.newHashSet(100L, 200L, 300L))
            .build();
    }

    private LogisticsPointResponse getUpdatedLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(null)
            .businessId(21L)
            .externalId("CODE")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT")
            .address(getUpdatedAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(true)
            .schedule(ImmutableSet.of())
            .contact(getContactDto())
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(ImmutableSet.of(getServiceDto()))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .marketBranded(false)
            .handlingTime(Duration.ofDays(1))
            .build();
    }

    private LogisticsPointResponse getDeactivatedLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(null)
            .externalId("CODE")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT")
            .address(DtoFactory.getAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(false)
            .schedule(ImmutableSet.of())
            .contact(getContactDto())
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(ImmutableSet.of(getServiceDto()))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .locationZoneId(500L)
            .marketBranded(false)
            .build();
    }

    private LogisticsPointResponse getLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(42L)
            .businessId(21L)
            .externalId("CODE")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT")
            .address(DtoFactory.getAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(true)
            .schedule(ImmutableSet.of())
            .contact(getContactDto())
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(ImmutableSet.of(getServiceDto()))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .marketBranded(false)
            .build();
    }

    private Service getServiceDto() {
        return new Service(
            ServiceCodeName.CHECK,
            false,
            "Проверка заказа перед оплатой",
            null
        );
    }

    private Address getUpdatedAddressDto() {
        return Address.newBuilder()
            .locationId(12345)
            .settlement("Москва")
            .postCode("555666")
            .latitude(new BigDecimal("100"))
            .longitude(new BigDecimal("200"))
            .street("Октябрьская")
            .house("5")
            .housing("3")
            .building("2")
            .apartment("1")
            .comment("new comment")
            .region("region")
            .subRegion("subRegion")
            .addressString("Строка адреса")
            .shortAddressString("Строка адреса")
            .build();
    }

    private Contact getContactDto() {
        return new Contact(
            "Арсений",
            "Петров",
            "Сергеевич"
        );
    }

    private Phone getPhoneDto() {
        return Phone.newBuilder()
            .number("+78005553535")
            .internalNumber("")
            .comment("number")
            .type(PhoneType.PRIMARY)
            .build();
    }

    LogisticsPointCreateRequest.Builder getPointRequestBuilder() {
        return LogisticsPointCreateRequest.newBuilder()
            .partnerId(42L)
            .businessId(21L)
            .externalId("CODE")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT")
            .address(DtoFactory.getAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(true)
            .schedule(ImmutableSet.of())
            .contact(getContactDto())
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(ImmutableSet.of(getServiceDto()))
            .storagePeriod(10)
            .maxWidth(15)
            .maxWeight(15d)
            .maxLength(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .marketBranded(false);
    }

    LogisticsPointUpdateRequest.Builder getPointUpdateRequestBuilder() {
        return LogisticsPointUpdateRequest.newBuilder()
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT")
            .businessId(21L)
            .addressComment("new comment")
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(true)
            .schedule(ImmutableSet.of())
            .contact(getContactDto())
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(ImmutableSet.of(getServiceDto()))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .marketBranded(false);
    }
}
