package ru.yandex.market.logistics.management.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Операции с бизнес-складами")
public class LmsClientBusinessWarehouseTest extends AbstractClientTest {
    @Test
    @DisplayName("Создать бизнес-склад")
    void createBusinessWarehouse() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/business-warehouse").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource(
                "data/controller/businessWarehouse/create_business_warehouse_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/create_business_warehouse_response.json")));

        BusinessWarehouseResponse warehouse = client.createBusinessWarehouse(createBusinessWarehouseDto());

        softly.assertThat(warehouse).usingRecursiveComparison().isEqualTo(businessWarehouseResponse());
    }

    @Test
    @DisplayName("Получить бизнес-склад")
    void getBusinessWarehouse() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/business-warehouse/1").toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/create_business_warehouse_response.json")));

        Optional<BusinessWarehouseResponse> warehouse = client.getBusinessWarehouseForPartner(1L);

        softly.assertThat(warehouse).isPresent();
        softly.assertThat(warehouse.get()).usingRecursiveComparison().isEqualTo(businessWarehouseResponse());
    }

    @Test
    @DisplayName("Обновить бизнес-склад")
    void updateBusinessWarehouse() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/business-warehouse/1").toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/businessWarehouse/update_business_warehouse_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/update_business_warehouse_response.json")));

        BusinessWarehouseResponse warehouse = client.updateBusinessWarehouse(1L, updateBusinessWarehouseDto());

        softly.assertThat(warehouse).usingRecursiveComparison().isEqualTo(businessWarehouseResponse());
    }

    @Test
    @DisplayName("Получить страницу бизнес-складов")
    void getBusinessWarehousesPage() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse?size=2&page=0"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/businessWarehouse/search_business_warehouse_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/search_business_warehouse_response.json")));

        PageResult<BusinessWarehouseResponse> warehouses =
            client.getBusinessWarehouses(businessWarehouseFilter(), 0, 2);

        softly.assertThat(warehouses).isEqualTo(
            new PageResult<>()
                .setData(List.of(businessWarehouseResponse()))
                .setTotalElements(1)
                .setPage(0)
                .setSize(2)
                .setTotalPages(1)
                .setTotalElements(1)
        );
    }

    @DisplayName("Копирование бизнес-склада и партнера")
    @Test
    void copyBusinessWarehouseAndPartner() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse/copy/" + 1))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/create_business_warehouse_response.json")));

        BusinessWarehouseResponse response = client.copyBusinessWarehouseAndPartner(1L);

        softly.assertThat(response).isEqualTo(businessWarehouseResponse());
    }

    @DisplayName("Деактивация бизнес-склада после копирования")
    @Test
    void deactivateBusinessWarehouseAfterCopy() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse/deactivate-after-copy/3"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deactivateBusinessWarehouseAfterCopy(3L)).doesNotThrowAnyException();
    }

    @DisplayName("Деактивация бизнес-склада")
    @Test
    void deactivateBusinessWarehouse() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse/3/deactivate"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deactivateBusinessWarehouse(3L)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Получить страницу бизнес-складов без указания параметров")
    void getBusinessWarehousesPageWithoutParams(String name, @Nullable Integer page, @Nullable Integer size) {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/businessWarehouse/search_business_warehouse_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/businessWarehouse/search_business_warehouse_response.json")));

        PageResult<BusinessWarehouseResponse> warehouses =
            client.getBusinessWarehouses(businessWarehouseFilter(), page, size);

        softly.assertThat(warehouses).isEqualTo(
            new PageResult<>()
                .setData(List.of(businessWarehouseResponse()))
                .setTotalElements(1)
                .setPage(0)
                .setSize(2)
                .setTotalPages(1)
                .setTotalElements(1)
        );
    }

    @Nonnull
    private static Stream<Arguments> getBusinessWarehousesPageWithoutParams() {
        return Stream.of(
            Arguments.of("Без номера страницы", null, 1),
            Arguments.of("Без размера страницы", 1, null),
            Arguments.of("Без номера и размера страницы", null, null)
        );
    }

    @Nonnull
    private BusinessWarehouseResponse businessWarehouseResponse() {
        return BusinessWarehouseResponse.newBuilder()
            .logisticsPointId(1L)
            .businessId(100L)
            .marketId(200L)
            .partnerId(1L)
            .externalId("ext-id")
            .name("business warehouse")
            .readableName("бизнес склад")
            .partnerType(PartnerType.DROPSHIP)
            .address(Address.newBuilder()
                .locationId(1)
                .country("Россия")
                .settlement("Новосибирск")
                .postCode("630111")
                .latitude(BigDecimal.valueOf(0.11))
                .longitude(BigDecimal.valueOf(0.12))
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
            .partnerStatus(PartnerStatus.INACTIVE)
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+78005553535")
                    .internalNumber("1222")
                    .comment("number")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.NOON, true)))
            .contact(new Contact("Имя", "Фамилия", "Отчество"))
            .logoUrl("http://partner.test/avatarka1.jpg")
            .domain("first.ff.example1.com")
            .billingClientId(11L)
            .rating(21)
            .locationId(215)
            .shipmentType(ExtendedShipmentType.EXPRESS)
            .build();
    }

    @Nonnull
    private CreateBusinessWarehouseDto createBusinessWarehouseDto() {
        return CreateBusinessWarehouseDto.newBuilder()
            .partnerType(PartnerType.DROPSHIP)
            .name("business warehouse")
            .readableName("бизнес склад")
            .businessId(100L)
            .marketId(200L)
            .externalId("ext-id")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .country("Россия")
                    .settlement("Новосибирск")
                    .postCode("630111")
                    .latitude(BigDecimal.valueOf(0.11))
                    .longitude(BigDecimal.valueOf(0.12))
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
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+78005553535")
                    .internalNumber("1222")
                    .comment("number")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.NOON, true)))
            .contact(new Contact("Имя", "Фамилия", "Отчество"))
            .handlingTime(Duration.ofDays(10))
            .build();
    }

    @Nonnull
    private UpdateBusinessWarehouseDto updateBusinessWarehouseDto() {
        return UpdateBusinessWarehouseDto.newBuilder()
            .name("business warehouse")
            .readableName("бизнес склад")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .country("Россия")
                    .settlement("Новосибирск")
                    .postCode("630111")
                    .latitude(BigDecimal.valueOf(0.11))
                    .longitude(BigDecimal.valueOf(0.12))
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
            .phones(Set.of(
                Phone.newBuilder()
                    .number("+78005553535")
                    .internalNumber("1222")
                    .comment("number")
                    .type(PhoneType.PRIMARY)
                    .build()
            ))
            .schedule(Set.of(new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.NOON, true)))
            .contact(new Contact("Имя", "Фамилия", "Отчество"))
            .externalId("new-ext-id")
            .build();
    }

    @Nonnull
    private BusinessWarehouseFilter businessWarehouseFilter() {
        return BusinessWarehouseFilter.newBuilder()
            .businessIds(Set.of(41L, 42L))
            .marketIds(Set.of(100L, 200L))
            .ids(Set.of(1L, 2L))
            .platformClientIds(Set.of(1L, 3L))
            .platformClientStatuses(Set.of(PartnerStatus.ACTIVE))
            .statuses(Set.of(PartnerStatus.TESTING))
            .types(Set.of(PartnerType.DROPSHIP))
            .build();
    }
}
