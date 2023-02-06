package ru.yandex.market.logistics.nesu.controller.shipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CarDto;
import ru.yandex.market.logistics.lom.model.dto.ContactDto;
import ru.yandex.market.logistics.lom.model.dto.CourierDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.CourierType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты АПИ search ShipmentController")
@DatabaseSetup("/repository/shipments/database_prepare.xml")
class ShipmentSearchTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск заявок на отгрузку (самопривоз)")
    void searchImportShipment() throws Exception {
        List<ShipmentSearchDto> response = List.of(
            createShipment(),
            createShipment(3L)
        );

        mockSearchShipments(response, refEq(defaultLomFilter().build()), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L, 3L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_import.json"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку (забор)")
    void searchWithdrawShipment() throws Exception {
        List<ShipmentSearchDto> searchResponse = List.of(createWithdrawShipment());

        mockSearchShipments(searchResponse, refEq(defaultLomFilter().build()), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_withdraw.json"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку с неизвестным складом")
    void searchShipmentWarehouseNotFound() throws Exception {
        List<ShipmentSearchDto> response = List.of(createShipment(2L));

        mockSearchShipments(response, refEq(defaultLomFilter().build()), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(
            Set.of(1L, 2L),
            List.of(createLogisticsPointResponse(1L, 1L, "", null)), lmsClient
        );
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [WAREHOUSE] with ids [2]"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку с неизвестным партнером")
    void searchShipmentPartnerNotFound() throws Exception {
        List<ShipmentSearchDto> response = List.of(createShipment(2L));

        mockSearchShipments(response, refEq(defaultLomFilter().build()), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(1L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [2]"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку пустой результат")
    void searchShipmentPartnerEmpty() throws Exception {
        mockSearchShipments(List.of(), refEq(defaultLomFilter().build()), refEq(PAGE_DEFAULTS));

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_empty.json"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку. Кейс с отсутствием параметра shopId")
    void searchShipmentPartnerWithoutShopId() throws Exception {
        mockMvc.perform(
            put("/back-office/shipments/search")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "shopId",
                "Failed to convert value of type 'null' to required type 'long'",
                "shopIdHolder",
                "typeMismatch"
            )));

        verifyZeroInteractions(lomClient);
    }

    @Test
    @DisplayName("Валидация фильтра поиска заявок на отгрузку")
    void searchShipmentValidation() throws Exception {
        search("controller/shipment/request/search_shipment_javax_request.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError("orderStatuses[]", "must not be null", "shipmentSearchFilter", "NotNull"),
                fieldError("partnerIdsTo[]", "must not be null", "shipmentSearchFilter", "NotNull"),
                fieldError("statuses[]", "must not be null", "shipmentSearchFilter", "NotNull"),
                fieldError("warehousesFrom[]", "must not be null", "shipmentSearchFilter", "NotNull")
            )));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск заявок на отгрузку с полным списком параметров")
    void searchShipmentPartnerAllFields(
        @SuppressWarnings("unused") String caseName,
        String requestPath
    ) throws Exception {
        mockSearchShipments(
            List.of(
                createShipment(),
                createShipment(3L)
            ),
            refEq(allFieldsFilter()),
            refEq(PAGE_DEFAULTS)
        );
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L, 3L), lmsClient);

        search(requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_import.json"));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск по warehouseFrom",
                "controller/shipment/request/search_all_fields_shipment_request_warehouse_from.json"
            ),
            Arguments.of(
                "Поиск по warehousesFrom",
                "controller/shipment/request/search_all_fields_shipment_request.json"
            )
        );
    }

    @Test
    @DisplayName("Поиск отгрузок без заявок")
    void searchShipmentWithoutApplication() throws Exception {
        LocalDate date = LocalDate.of(2019, 8, 17);
        List<ShipmentSearchDto> searchResponse = List.of(
            ShipmentSearchDto.builder()
                .created(date.atStartOfDay().toInstant(ZoneOffset.UTC))
                .marketIdFrom(2L)
                .partnerIdTo(3L)
                .shipmentDate(date)
                .shipmentType(ShipmentType.WITHDRAW)
                .warehouseFrom(1L)
                .build()
        );

        ShipmentTestUtils.mockGetPartnersById(2L, lmsClient);
        mockSearchShipments(searchResponse, any(), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(3L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_without_application.json"));
    }

    @Test
    @DisplayName("Поиск отгрузок c пешим курьером")
    void searchShipmentWithoutCar() throws Exception {
        CourierDto courier = createCourier(null);
        List<ShipmentSearchDto> searchResponse = List.of(ShipmentTestUtils.createShipment(2L, courier));

        ShipmentTestUtils.mockGetPartnersById(2L, lmsClient);
        mockSearchShipments(searchResponse, any(), refEq(PAGE_DEFAULTS));
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L), lmsClient);

        search("controller/shipment/request/search_shipment_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_without_car.json"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку по несуществующему партнеру")
    void searchShipmentPartnerAllFieldsNotFoundPartner() throws Exception {
        mockSearchShipments(
            List.of(createShipment(2L)),
            refEq(allFieldsFilter()),
            refEq(PAGE_DEFAULTS)
        );

        doReturn(List.of())
            .when(lmsClient)
            .searchPartners(any());

        search("controller/shipment/request/search_all_fields_shipment_request.json")
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [2]"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку по пустому списку партнеров")
    void searchShipmentPartnerAllFieldsEmptyPartners() throws Exception {
        search("controller/shipment/request/search_empty_partners_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/search/response/empty.json"));

        verifyZeroInteractions(lomClient);
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку с сортировкой")
    void searchShipmentPartnerWithSort() throws Exception {
        List<ShipmentSearchDto> response = List.of(
            createShipment(),
            createShipment(3L)
        );

        ShipmentTestUtils.mockGetPartnersById(2L, lmsClient);
        mockSearchShipments(
            response,
            refEq(defaultLomFilter().build()),
            ArgumentMatchers.argThat(pageable -> {
                Map<String, Set<String>> expected =
                    new Pageable(0, 10, new Sort(Direction.DESC, "shipmentDate")).toUriParams();
                return pageable.toUriParams().equals(expected);
            })
        );
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L, 3L), lmsClient);

        MultiValueMap<String, String> sortingParam = new LinkedMultiValueMap<>();
        sortingParam.put("sort", List.of("shipmentDate,desc"));
        search("controller/shipment/request/search_shipment_request.json", sortingParam)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_import.json"));
    }

    @Test
    @DisplayName("Поиск заявок на отгрузку с сортировкой без указания направления")
    void searchShipmentPartnerWithSortWithoutDirection() throws Exception {
        List<ShipmentSearchDto> response = List.of(
            createShipment(),
            createShipment(3L)
        );

        ShipmentTestUtils.mockGetPartnersById(2L, lmsClient);
        mockSearchShipments(
            response,
            refEq(defaultLomFilter().build()),
            ArgumentMatchers.argThat(pageable -> {
                Map<String, Set<String>> expected =
                    new Pageable(0, 10, new Sort(Direction.ASC, "shipmentDate")).toUriParams();
                return pageable.toUriParams().equals(expected);
            })
        );

        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(2L, 3L), lmsClient);

        MultiValueMap<String, String> sortingParam = new LinkedMultiValueMap<>();
        sortingParam.put("sort", List.of("shipmentDate"));
        search("controller/shipment/request/search_shipment_request.json", sortingParam)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/search_shipment_import.json"));
    }

    @Nonnull
    private PageResult<ShipmentSearchDto> createPageResult(List<ShipmentSearchDto> values, int totalElements) {
        return new PageResult<ShipmentSearchDto>()
            .setData(values)
            .setTotalPages(1)
            .setPageNumber(0)
            .setTotalElements(totalElements)
            .setSize(10);
    }

    @Nonnull
    private ResultActions search(String requestPath) throws Exception {
        return search(requestPath, new LinkedMultiValueMap<>());
    }

    @Nonnull
    private ResultActions search(String requestPath, MultiValueMap<String, String> additionalParam) throws Exception {
        return mockMvc.perform(
            put("/back-office/shipments/search")
                .param("shopId", "1")
                .param("userId", "1")
                .params(additionalParam)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    private void mockSearchShipments(List<ShipmentSearchDto> result, ShipmentSearchFilter filter, Pageable pageable) {
        when(lomClient.searchShipments(filter, pageable))
            .thenReturn(createPageResult(result, result.size()));
    }

    @Nonnull
    private ShipmentSearchFilter.ShipmentSearchFilterBuilder defaultLomFilter() {
        return ShipmentSearchFilter.builder().marketIdFrom(100L);
    }

    @Nonnull
    private ShipmentSearchFilter allFieldsFilter() {
        return defaultLomFilter()
            .partnerIdsTo(Set.of(2L))
            .fromDate(LocalDate.of(2019, 5, 1))
            .toDate(LocalDate.of(2019, 6, 20))
            .fromTime(LocalTime.of(8, 0))
            .toTime(LocalTime.of(20, 0))
            .shipmentType(ShipmentType.WITHDRAW)
            .withApplication(true)
            .statuses(Set.of(
                ShipmentApplicationStatus.NEW,
                ShipmentApplicationStatus.DELIVERY_SERVICE_PROCESSING
            ))
            .warehousesFrom(Set.of(1L))
            .warehouseTo(2L)
            .orderStatuses(Set.of(OrderStatus.DRAFT))
            .segmentStatuses(Map.of(PartnerType.DELIVERY, Set.of(SegmentStatus.PENDING)))
            .build();
    }

    @Nonnull
    private ShipmentSearchDto createShipment(long partnerIdTo) {
        return ShipmentTestUtils.createShipment(partnerIdTo, createCourier(createCar()));
    }

    @Nonnull
    private ShipmentSearchDto createShipment() {
        return ShipmentTestUtils.createShipment(2, createCourier(createCar()), true);
    }

    @Nonnull
    private CarDto createCar() {
        return CarDto.builder()
            .number("a348ee54rus")
            .brand("toyota")
            .build();
    }

    @Nonnull
    private ShipmentSearchDto createWithdrawShipment() {
        LocalDate date = LocalDate.of(2019, 6, 8);
        CourierDto courier = createCourier(createCar());
        return ShipmentSearchDto.builder()
            .marketIdFrom(1L)
            .shipmentDate(date)
            .shipmentType(ShipmentType.WITHDRAW)
            .warehouseFrom(1L)
            .partnerIdTo(2L)
            .korobyteDto(new KorobyteDto(10, 15, 40, new BigDecimal("5.5")))
            .interval(new TimeIntervalDto(LocalTime.of(12, 0), LocalTime.of(13, 0)))
            .status(ShipmentApplicationStatus.NEW)
            .cost(new BigDecimal(300))
            .applicationId(5L)
            .courier(courier)
            .created(date.atStartOfDay().toInstant(ZoneOffset.UTC))
            .build();
    }

    @Nonnull
    private CourierDto createCourier(@Nullable CarDto car) {
        return CourierDto.builder()
            .car(car)
            .type(car == null ? CourierType.COURIER : CourierType.CAR)
            .contact(createContact())
            .build();
    }

    @Nonnull
    private ContactDto createContact() {
        return ContactDto.builder()
            .firstName("fname")
            .lastName("lname")
            .phone("+79998884433")
            .build();
    }
}
