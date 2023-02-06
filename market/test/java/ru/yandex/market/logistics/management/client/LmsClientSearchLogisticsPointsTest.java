package ru.yandex.market.logistics.management.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientSearchLogisticsPointsTest extends AbstractClientTest {

    @Test
    void searchFilterSerialization() {
        LogisticsPointFilter filter = filter();

        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/logistics_points/search_filter.json"), true))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_points/search_result.json")));

        client.getLogisticsPoints(filter);
    }

    @Test
    void searchFilterPageSerialization() {
        LogisticsPointFilter filter = filter();

        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/page?size=10&page=1"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/logistics_points/search_filter.json"), true))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_points/search_result_page.json")));

        client.getLogisticsPoints(filter, new PageRequest(1, 10));
    }

    @Test
    void searchEmptyResult() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json")));

        List<LogisticsPointResponse> result = client.getLogisticsPoints(LogisticsPointFilter.newBuilder().build());

        softly.assertThat(result).isEmpty();
    }

    @Test
    void searchEmptyPageResult() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/page?size=2&page=1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_points/empty_page.json")));

        PageResult<LogisticsPointResponse> result = client.getLogisticsPoints(
            LogisticsPointFilter.newBuilder().build(),
            new PageRequest(1, 2)
        );

        softly.assertThat(result.getData()).isEmpty();
        softly.assertThat(result.getPage()).isEqualTo(1);
        softly.assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    void searchResultDeserialization() {
        LogisticsPointResponse first = warehouse();
        LogisticsPointResponse second = pickupPoint();

        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_points/search_result.json")));

        List<LogisticsPointResponse> result = client.getLogisticsPoints(LogisticsPointFilter.newBuilder().build());

        softly.assertThat(result).isNotEmpty();
        softly.assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(first, second);
    }

    @Test
    void searchResultPageDeserialization() {
        LogisticsPointResponse first = warehouse();
        LogisticsPointResponse second = pickupPoint();

        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/page?size=10&page=1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/logistics_points/search_result_page.json")));

        PageResult<LogisticsPointResponse> result = client.getLogisticsPoints(
            LogisticsPointFilter.newBuilder().build(),
            new PageRequest(1, 10)
        );

        softly.assertThat(result.getData()).isNotEmpty();
        softly.assertThat(result.getData()).usingRecursiveFieldByFieldElementComparator().containsOnly(first, second);
    }

    @Nonnull
    private LogisticsPointFilter filter() {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(ImmutableSet.of(456L, 567L))
            .businessIds(ImmutableSet.of(241L, 984L))
            .platformClientId(3L)
            .type(PointType.PICKUP_POINT)
            .active(true)
            .externalIds(ImmutableSet.of("678", "789"))
            .ids(ImmutableSet.of(890L))
            .latitudeFrom(new BigDecimal("1.2"))
            .latitudeTo(new BigDecimal("2.3"))
            .longitudeFrom(new BigDecimal("3.4"))
            .longitudeTo(new BigDecimal("4.5"))
            .locationId(90)
            .pickupPointType(PickupPointType.POST_OFFICE)
            .orderLength(11)
            .orderWidth(12)
            .orderHeight(13)
            .orderWeight(14.5)
            .marketBranded(false)
            .hasPartner(true)
            .hasBusinessId(true)
            .partnerTypes(Set.of(PartnerType.OWN_DELIVERY))
            .partnerSubtypesToExclude(Set.of(1L))
            .build();
    }

    @Nonnull
    private LogisticsPointResponse pickupPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(2L)
            .partnerId(1L)
            .businessId(21L)
            .externalId("CODE_2")
            .type(PointType.WAREHOUSE)
            .pickupPointType(null)
            .name("NEW_POINT_2")
            .address(getAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(false)
            .schedule(Collections.emptySet())
            .contact(getContactDto())
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .photos(null)
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
            .availableForOnDemand(false)
            .deferredCourierAvailable(false)
            .darkStore(false)
            .availableForC2C(false)
            .handlingTime(Duration.ofDays(1))
            .build();
    }

    @Nonnull
    private LogisticsPointResponse warehouse() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(0L)
            .externalId("CODE_1")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("NEW_POINT_1")
            .address(getAddressDto())
            .phones(ImmutableSet.of(getPhoneDto()))
            .active(true)
            .schedule(Collections.emptySet())
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
            .availableForOnDemand(false)
            .deferredCourierAvailable(false)
            .darkStore(false)
            .availableForC2C(false)
            .build();
    }

    @Nonnull
    private Address getAddressDto() {
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
            .comment("comment")
            .region("region")
            .subRegion("subRegion")
            .addressString("Строка адреса")
            .shortAddressString("Короткая строка адреса")
            .build();
    }

    @Nonnull
    private Contact getContactDto() {
        return new Contact(
            "Арсений",
            "Петров",
            "Сергеевич"
        );
    }

    @Nonnull
    private Phone getPhoneDto() {
        return Phone.newBuilder()
            .number("+78005553535")
            .internalNumber("")
            .comment("number")
            .type(PhoneType.PRIMARY)
            .build();
    }

    @Nonnull
    private Service getServiceDto() {
        return new Service(
            ServiceCodeName.CHECK,
            false,
            "Проверка заказа перед оплатой",
            null
        );
    }
}
