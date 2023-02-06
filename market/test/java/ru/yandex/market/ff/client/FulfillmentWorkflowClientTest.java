package ru.yandex.market.ff.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpServerErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.dto.CalendaringIntervalByRatingDto;
import ru.yandex.market.ff.client.dto.CargoUnitCountRequestDTO;
import ru.yandex.market.ff.client.dto.CreateRegistryDTO;
import ru.yandex.market.ff.client.dto.CreateSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.CreateTransferForm;
import ru.yandex.market.ff.client.dto.CreateTransferItemForm;
import ru.yandex.market.ff.client.dto.CustomerReturnInfoDTO;
import ru.yandex.market.ff.client.dto.CustomerReturnItemDTO;
import ru.yandex.market.ff.client.dto.DataEnteredByMerchantDTO;
import ru.yandex.market.ff.client.dto.InboundPrimaryDocumentDTO;
import ru.yandex.market.ff.client.dto.ItemCountDTO;
import ru.yandex.market.ff.client.dto.ItemCountUnitDTO;
import ru.yandex.market.ff.client.dto.LastShopRequestFilterDTO;
import ru.yandex.market.ff.client.dto.LegalInfoDTO;
import ru.yandex.market.ff.client.dto.LogisticUnitDTO;
import ru.yandex.market.ff.client.dto.LogisticUnitMetaDTO;
import ru.yandex.market.ff.client.dto.PrimaryDivergenceActDto;
import ru.yandex.market.ff.client.dto.PutSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.PutWithdrawRequestDTO;
import ru.yandex.market.ff.client.dto.RegistryDTO;
import ru.yandex.market.ff.client.dto.RegistryDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryRestrictedDataDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitCountDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitCountsInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitsFilterDTO;
import ru.yandex.market.ff.client.dto.RejectionDto;
import ru.yandex.market.ff.client.dto.RejectionReasonDto;
import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTOContainer;
import ru.yandex.market.ff.client.dto.RequestItemFilterDTO;
import ru.yandex.market.ff.client.dto.RequestRejectDTO;
import ru.yandex.market.ff.client.dto.RequestStatusHistoryDTO;
import ru.yandex.market.ff.client.dto.ShopRequestAdditionalFieldsConfigDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDocumentDTO;
import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.dto.ShopRequestYardFilterDTO;
import ru.yandex.market.ff.client.dto.SupplierRatingIntervalConfigResponse;
import ru.yandex.market.ff.client.dto.SupplierRatingIntervalDto;
import ru.yandex.market.ff.client.dto.SupplierSkuDto;
import ru.yandex.market.ff.client.dto.SupplierWithFirstFinishedSupplyInfo;
import ru.yandex.market.ff.client.dto.TransferDetailsDTO;
import ru.yandex.market.ff.client.dto.UtilizationItemCountRequestDto;
import ru.yandex.market.ff.client.dto.XDocFinalInboundDateDTO;
import ru.yandex.market.ff.client.dto.quota.AvailableQuotaForServiceAndDestinationDto;
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForDateDto;
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForServiceDto;
import ru.yandex.market.ff.client.dto.quota.GetQuotaFilterDto;
import ru.yandex.market.ff.client.dto.quota.GetQuotaFilterDtov2;
import ru.yandex.market.ff.client.dto.quota.GetQuotaResponseDto;
import ru.yandex.market.ff.client.dto.quota.GetQuotaWithDestinationsDto;
import ru.yandex.market.ff.client.dto.quota.GetQuotaWithDestinationsResponseDto;
import ru.yandex.market.ff.client.dto.quota.QuotaInfosDTO;
import ru.yandex.market.ff.client.dto.quota.TakeQuotaBookingDto;
import ru.yandex.market.ff.client.dto.quota.TakeQuotaDto;
import ru.yandex.market.ff.client.dto.quota.TakeQuotaResponseDto;
import ru.yandex.market.ff.client.dto.quota.TakeQuotasManyBookingsRequestDto;
import ru.yandex.market.ff.client.dto.quota.TakeQuotasManyBookingsResponseDto;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.DailyLimitsType;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.RequestDocumentType;
import ru.yandex.market.ff.client.enums.RequestItemAttribute;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.TransferCreationType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.ff.client.FulfillmentWorkflowReturnRegistryClientTest.assertCourierIsDeserializedCorrectly;
import static ru.yandex.market.ff.client.FulfillmentWorkflowReturnRegistryClientTest.createCourierDTO;
import static ru.yandex.market.ff.client.FulfillmentWorkflowReturnRegistryClientTest.createSupplyWithRegistryDTO;
import static ru.yandex.market.ff.client.enums.RequestType.WITHDRAW;

/**
 * Функциональные тесты для  {@link FulfillmentWorkflowClient}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class FulfillmentWorkflowClientTest {

    private static final long REQUEST_ID = 1L;
    private static final String SERVICE_REQUEST_ID = "abc123";
    private static final String RETURN_ID = "return1";
    private static final String RETURN_ITEMS_JSON = "{\"items\":[{\"supplierId\":1,\"article\":\"art1\"}," +
            "{\"supplierId\":2,\"article\":\"art2\"}],\"serviceId\":100}";
    private static final String RETURN_ITEMS_WITH_UPLOADING_FINISH_DATE_JSON =
            "{\"items\":[{\"supplierId\":1,\"article\":\"art1\"},{\"supplierId\":2,\"article\":\"art2\"}]," +
                    "\"serviceId\":100,\"uploadingFinishDate\":\"2020-09-12T09:00:00+03:00\"}";
    private static final String ACCEPT_REQUEST_JSON = "{\"serviceRequestId\":\"abc123\"}";
    private static final String CREATE_REQUEST_JSON = "{\"calendaringMode\":3}";
    private static final long SERVICE_ID = 100L;

    @Autowired
    private FulfillmentWorkflowClientApi clientApi;

    @Autowired
    private MockRestServiceServer mockServer;

    @Value("${fulfillment.workflow.api.host}")
    private String host;

    @AfterEach
    void resetMocks() {
        mockServer.reset();
    }

    @Test
    void sendCustomerReturnsSuccessfully() {
        assertCustomerReturnCreation(true, OK, false);
    }

    @Test
    void sendCustomerReturnsSuccessfullyWithUploadingFinishDate() {
        assertCustomerReturnCreation(true, OK, true);
    }

    @Test
    void sendCustomerReturnsAlreadyExists() {
        assertCustomerReturnCreation(false, ALREADY_REPORTED, false);
    }

    @Test
    void sendCustomerReturnsError() {
        Assertions.assertThrows(HttpServerErrorException.class, () ->
                assertCustomerReturnCreation(false, INTERNAL_SERVER_ERROR, false));
    }

    @Test
    void sendCustomerReturnsBadRequest() {
        Assertions.assertThrows(HttpTemplateException.class, () ->
                assertCustomerReturnCreation(false, BAD_REQUEST, false));
    }

    @Test
    void getCustomerReturn() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("customer_return.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/returns/" + RETURN_ID))
                .andRespond(returnResponseCreator);

        CustomerReturnInfoDTO returnInfo = clientApi.getCustomerReturn(RETURN_ID);
        assertThat(returnInfo, notNullValue());
        assertThat(returnInfo.getId(), equalTo(RETURN_ID));
        assertThat(returnInfo.getCreatedAt(),
                equalTo(LocalDateTime.of(2018, 1, 1, 10, 10, 10)));
        assertThat(returnInfo.getRequestId(), equalTo(10L));
        assertThat(returnInfo.getServiceId(), equalTo(555L));
        assertThat(returnInfo.getUploadingFinishDate(), equalTo(Instant.parse("2020-09-12T06:00:00Z")));
    }

    @Test
    void findItemsCountInTransit() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("items_count.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/requests/items/in-transit-count"))
                .andRespond(returnResponseCreator);

        ItemCountDTO dto = clientApi.findItemsCountInTransit(null);

        assertThat(dto, contains(samePropertyValuesAs(new ItemCountUnitDTO(1, 1, 100, 3)),
                samePropertyValuesAs(new ItemCountUnitDTO(1, 2, 101, 5))));
    }

    @Test
    void getRequests() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_requests.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/requests")))
                .andExpect(queryParam("requestIds", "ID123"))
                .andExpect(queryParam("types", "1"))
                .andExpect(queryParam("statuses", "0", "7"))
                .andExpect(queryParam("article", "shop_sku1"))
                .andExpect(queryParam("hasShortage", "false"))
                .andExpect(queryParam("hasAnomaly", "false"))
                .andExpect(queryParam("stockType", "1"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("size", "15"))
                .andRespond(returnResponseCreator);

        ShopRequestFilterDTO filterDTO = new ShopRequestFilterDTO();
        filterDTO.setArticle("shop_sku1");
        filterDTO.setCreationDateFrom(LocalDate.of(2018, 1, 1));
        filterDTO.setRequestDateTo(LocalDate.of(2019, 1, 1));
        filterDTO.setRequestIds(Arrays.asList("ID123", "ID456"));
        filterDTO.setStockType(StockType.EXPIRED);
        filterDTO.setStatuses(Arrays.asList(RequestStatus.PROCESSED, RequestStatus.CREATED));
        filterDTO.setTypes(singletonList(1));
        filterDTO.setPage(2);
        filterDTO.setSize(15);

        ShopRequestDTOContainer shopRequestDTOContainer = clientApi.getRequests(filterDTO);

        mockServer.verify();

        LocalDateTime dateTime = LocalDateTime.of(1999, 9, 9, 9, 9, 9);

        ShopRequestDTO item1 = new ShopRequestDTO();
        item1.setId(1L);
        item1.setServiceId(1L);
        item1.setRequestedDate(dateTime);
        item1.setShopId(1L);
        item1.setShopName("some name");
        item1.setType(RequestType.SUPPLY.getId());
        item1.setCreatedAt(dateTime);
        item1.setUpdatedAt(dateTime);
        item1.setStatus(RequestStatus.SENT_TO_SERVICE);
        item1.setItemsTotalCount(25L);
        item1.setItemsTotalDefectCount(0L);
        item1.setItemsTotalFactCount(25L);
        item1.setHasDefects(false);
        item1.setHasShortage(false);
        item1.setStockType(StockType.EXPIRED);
        item1.setDocuments(singletonList(
                new ShopRequestDocumentDTO(1L, 1L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));
        Map<RequestItemErrorType, Long> errors = new HashMap<>();
        errors.put(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 2L);
        item1.setErrors(errors);

        ShopRequestDTO item2 = new ShopRequestDTO();
        item2.setId(2L);
        item2.setServiceId(1L);
        item2.setRequestedDate(dateTime);
        item2.setShopId(1L);
        item2.setShopName("some name");
        item2.setType(RequestType.SUPPLY.getId());
        item2.setCreatedAt(dateTime);
        item2.setUpdatedAt(dateTime);
        item2.setStatus(RequestStatus.CREATED);
        item2.setItemsTotalCount(25L);
        item2.setItemsTotalDefectCount(3L);
        item2.setItemsTotalFactCount(25L);
        item2.setHasDefects(true);
        item2.setHasShortage(false);
        item2.setDocuments(singletonList(
                new ShopRequestDocumentDTO(2L, 2L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));

        ShopRequestDTOContainer expected = new ShopRequestDTOContainer();
        expected.setTotalElements(2L);
        expected.setPageNumber(0);
        expected.setTotalPages(1);
        expected.addRequest(item1);
        expected.addRequest(item2);

        ReflectionAssert.assertReflectionEquals(expected, shopRequestDTOContainer);
    }

    @Test
    void getRequestsAndCalculateItemsTotalPrice() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_requests_with_additional_field.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/requests")))
                .andExpect(queryParam("requestIds", "ID123"))
                .andExpect(queryParam("types", "1"))
                .andExpect(queryParam("statuses", "0", "7"))
                .andExpect(queryParam("article", "shop_sku1"))
                .andExpect(queryParam("hasShortage", "false"))
                .andExpect(queryParam("stockType", "1"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("size", "15"))
                .andExpect(queryParam("calculateItemsTotalPrice", "true"))
                .andRespond(returnResponseCreator);

        ShopRequestFilterDTO filterDTO = new ShopRequestFilterDTO();
        filterDTO.setArticle("shop_sku1");
        filterDTO.setCreationDateFrom(LocalDate.of(2018, 1, 1));
        filterDTO.setRequestDateTo(LocalDate.of(2019, 1, 1));
        filterDTO.setRequestIds(Arrays.asList("ID123", "ID456"));
        filterDTO.setStockType(StockType.EXPIRED);
        filterDTO.setStatuses(Arrays.asList(RequestStatus.PROCESSED, RequestStatus.CREATED));
        filterDTO.setTypes(singletonList(1));
        filterDTO.setPage(2);
        filterDTO.setSize(15);

        ShopRequestAdditionalFieldsConfigDTO additionalFieldsConfigDTO = new ShopRequestAdditionalFieldsConfigDTO();
        additionalFieldsConfigDTO.setCalculateItemsTotalPrice(true);

        ShopRequestDTOContainer shopRequestDTOContainer = clientApi.getRequests(filterDTO, additionalFieldsConfigDTO);

        mockServer.verify();

        LocalDateTime dateTime = LocalDateTime.of(1999, 9, 9, 9, 9, 9);

        ShopRequestDTO item1 = new ShopRequestDTO();
        item1.setId(1L);
        item1.setServiceId(1L);
        item1.setRequestedDate(dateTime);
        item1.setShopId(1L);
        item1.setShopName("some name");
        item1.setType(RequestType.SUPPLY.getId());
        item1.setCreatedAt(dateTime);
        item1.setUpdatedAt(dateTime);
        item1.setStatus(RequestStatus.SENT_TO_SERVICE);
        item1.setItemsTotalCount(25L);
        item1.setItemsTotalDefectCount(0L);
        item1.setItemsTotalFactCount(25L);
        item1.setHasDefects(false);
        item1.setHasShortage(false);
        item1.setStockType(StockType.EXPIRED);
        item1.setDocuments(singletonList(
                new ShopRequestDocumentDTO(1L, 1L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));
        Map<RequestItemErrorType, Long> errors = new HashMap<>();
        errors.put(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 2L);
        item1.setErrors(errors);
        item1.setItemsTotalPrice(BigDecimal.ZERO);

        ShopRequestDTO item2 = new ShopRequestDTO();
        item2.setId(2L);
        item2.setServiceId(1L);
        item2.setRequestedDate(dateTime);
        item2.setShopId(1L);
        item2.setShopName("some name");
        item2.setType(RequestType.SUPPLY.getId());
        item2.setCreatedAt(dateTime);
        item2.setUpdatedAt(dateTime);
        item2.setStatus(RequestStatus.CREATED);
        item2.setItemsTotalCount(25L);
        item2.setItemsTotalDefectCount(3L);
        item2.setItemsTotalFactCount(25L);
        item2.setHasDefects(true);
        item2.setHasShortage(false);
        item2.setDocuments(singletonList(
                new ShopRequestDocumentDTO(2L, 2L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));
        item2.setItemsTotalPrice(new BigDecimal("5050.5"));
        item2.setUploadingFinishDate(Instant.parse("2020-10-12T06:00:00Z"));

        ShopRequestDTOContainer expected = new ShopRequestDTOContainer();
        expected.setTotalElements(2L);
        expected.setPageNumber(0);
        expected.setTotalPages(1);
        expected.addRequest(item1);
        expected.addRequest(item2);

        ReflectionAssert.assertReflectionEquals(expected, shopRequestDTOContainer);

    }

    @Test
    void getRequest() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_request.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID))
                .andRespond(returnResponseCreator);
        ShopRequestDetailsDTO actual = clientApi.getRequest(REQUEST_ID);
        final LocalDateTime dateTime = LocalDateTime.of(1999, 9, 9, 9, 9, 9);

        ShopRequestDetailsDTO expected = new ShopRequestDetailsDTO();
        expected.setId(1L);
        expected.setServiceId(1L);
        expected.setRequestedDate(dateTime);
        expected.setShopId(1L);
        expected.setShopName("some name");
        expected.setType(0);
        expected.setCreatedAt(dateTime);
        expected.setUpdatedAt(dateTime);
        expected.setStatus(RequestStatus.SENT_TO_SERVICE);
        expected.setItemsTotalCount(25L);
        expected.setItemsTotalDefectCount(0L);
        expected.setItemsTotalFactCount(25L);
        expected.setHasDefects(false);
        expected.setHasShortage(false);
        expected.setStockType(StockType.EXPIRED);
        expected.setComment("my comment");
        expected.setItemsWithDefects(1);
        expected.setItemsWithShortage(0);
        RequestStatusHistoryDTO hist = new RequestStatusHistoryDTO();
        hist.setStatus(RequestStatus.SENT_TO_SERVICE);
        hist.setDate(dateTime);
        expected.setStatusHistory(singletonList(hist));
        expected.setLegalInfo(new LegalInfoDTO("Some consignee", "Some name", "Some surname", "79232435555"));
        expected.setDocuments(singletonList(
                new ShopRequestDocumentDTO(1L, 1L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));
        Map<RequestItemErrorType, Long> errors = new HashMap<>();
        errors.put(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 2L);
        expected.setErrors(errors);
        expected.setCalendaringMode(CalendaringMode.NOT_REQUIRED);
        expected.setUploadingFinishDate(Instant.parse("2020-10-12T06:00:00Z"));
        expected.setSupplyRequestId(123L);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    void getFinishedWithdraws() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_requests.json")), StandardCharsets.UTF_8));

        final LocalDateTime dateTime = LocalDateTime.of(1999, 9, 9, 9, 9, 9);

        LocalDate dateFrom = dateTime.toLocalDate();
        LocalDate dateTo = dateTime.toLocalDate().plusDays(1);

        mockServer.expect(requestTo(startsWith(host + "/requests")))
                .andExpect(queryParam("types", WITHDRAW.getId() + ""))
                .andExpect(queryParam("statuses", RequestStatus.FINISHED.getId() + ""))
                .andExpect(queryParam("updatedDateFrom", dateFrom.format(DateTimeFormatter.ISO_DATE)))
                .andExpect(queryParam("updatedDateTo", dateTo.format(DateTimeFormatter.ISO_DATE)))
                .andRespond(returnResponseCreator);


        ShopRequestDTOContainer dto = clientApi.getFinishedWithdraws(dateFrom, dateTo);

        ShopRequestDTO item1 = new ShopRequestDTO();
        item1.setId(1L);
        item1.setServiceId(1L);
        item1.setRequestedDate(dateTime);
        item1.setShopId(1L);
        item1.setShopName("some name");
        item1.setType(RequestType.SUPPLY.getId());
        item1.setCreatedAt(dateTime);
        item1.setUpdatedAt(dateTime);
        item1.setStatus(RequestStatus.SENT_TO_SERVICE);
        item1.setItemsTotalCount(25L);
        item1.setItemsTotalDefectCount(0L);
        item1.setItemsTotalFactCount(25L);
        item1.setHasDefects(false);
        item1.setHasShortage(false);
        item1.setStockType(StockType.EXPIRED);
        item1.setDocuments(singletonList(
                new ShopRequestDocumentDTO(1L, 1L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));
        Map<RequestItemErrorType, Long> errors = new HashMap<>();
        errors.put(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 2L);
        item1.setErrors(errors);

        ShopRequestDTO item2 = new ShopRequestDTO();
        item2.setId(2L);
        item2.setServiceId(1L);
        item2.setRequestedDate(dateTime);
        item2.setShopId(1L);
        item2.setShopName("some name");
        item2.setType(RequestType.SUPPLY.getId());
        item2.setCreatedAt(dateTime);
        item2.setUpdatedAt(dateTime);
        item2.setStatus(RequestStatus.CREATED);
        item2.setItemsTotalCount(25L);
        item2.setItemsTotalDefectCount(3L);
        item2.setItemsTotalFactCount(25L);
        item2.setHasDefects(true);
        item2.setHasShortage(false);
        item2.setDocuments(singletonList(
                new ShopRequestDocumentDTO(2L, 2L, DocumentType.SUPPLY, dateTime, "FILE_URL")
        ));

        ShopRequestDTOContainer expected = new ShopRequestDTOContainer();
        expected.setTotalElements(2L);
        expected.setPageNumber(0);
        expected.setTotalPages(1);
        expected.addRequest(item1);
        expected.addRequest(item2);

        ReflectionAssert.assertReflectionEquals(expected, dto);
    }

    @Test
    void getRequestItems() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_request_items.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/requests/1/items")))
                .andExpect(queryParam("requestId", "1"))
                .andExpect(queryParam("article", "shop_sku1"))
                .andExpect(queryParam("hasDefects", "true"))
                .andExpect(queryParam("hasSurplus", "false"))
                .andExpect(queryParam("hasShortage", "false"))
                .andExpect(queryParam("size", "10"))
                .andExpect(queryParam("sort", "name"))
                .andRespond(returnResponseCreator);

        final RequestItemFilterDTO filter = RequestItemFilterDTO.builder(1L)
                .setArticle("shop_sku1")
                .setHasDefects(true)
                .setHasSurplus(false)
                .setSize(10)
                .setSort("name")
                .build();
        RequestItemDTOContainer actual = clientApi.getRequestItems(filter);

        RequestItemDTO item1 = new RequestItemDTO();
        item1.setArticle("abc");
        item1.setBarcodes(Arrays.asList("11", "22"));
        item1.setCount(3);
        item1.setFactCount(2);
        item1.setDefectCount(1);
        item1.setName("offer_1");
        item1.setMarketName("market_name_1");
        item1.setSku(2222L);
        item1.setSupplyPrice(BigDecimal.valueOf(5050).movePointLeft(2));
        item1.setVatRate(VatRate.VAT_0);
        item1.setErrors(Arrays.asList(
                RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND,
                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND
        ));
        item1.setLength(BigDecimal.valueOf(10.0));
        item1.setWidth(BigDecimal.valueOf(4.0));
        item1.setHeight(BigDecimal.valueOf(3.0));
        item1.setCargoTypes(List.of(200));

        RequestItemDTO item2 = new RequestItemDTO();
        item2.setArticle("abcd");
        item2.setBarcodes(Arrays.asList("111", "222"));
        item2.setCount(3);
        item2.setName("offer_2");
        item2.setMarketName("market_name_2");
        item2.setSku(3333L);
        item2.setSupplyPrice(BigDecimal.valueOf(5050).movePointLeft(2));
        item2.setVatRate(VatRate.VAT_0);

        RequestItemDTOContainer expected = new RequestItemDTOContainer();
        expected.setTotalCount(6L);
        expected.setTotalFactCount(2L);
        expected.setTotalDefectCount(1L);
        expected.setTotalSupplyPrice(BigDecimal.valueOf(10100).movePointLeft(2));
        expected.setTotalPages(2);
        expected.setPageNumber(1);
        expected.setTotalElements(2);
        expected.addItem(item1);
        expected.addItem(item2);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    void createSupply() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_supply.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/upload-request/supply"))
                .andExpect(content().json(CREATE_REQUEST_JSON))
                .andRespond(returnResponseCreator);

        final ShopRequestDTO supplyRequest = clientApi.createSupplyRequest(createSupplyValidRequest());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.SUPPLY.getId()));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(25L));
    }

    private CreateSupplyRequestDTO createSupplyValidRequest() {
        CreateSupplyRequestDTO dto = new CreateSupplyRequestDTO();
        dto.setCalendaringMode(CalendaringMode.ALREADY_SELECTED);
        return dto;
    }

    @Test
    void createRegistry() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_supply_with_uploading_finish_date.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/upload-request/registry"))
                .andExpect(content().json(IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(
                        "valid-registry-with-cis.json")), StandardCharsets.UTF_8), true))
                .andRespond(returnResponseCreator);

        ShopRequestDTO supplyRequest = clientApi.createRegistryRequest(createRegistryDTO());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.SUPPLY.getId()));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(25L));
        assertThat(supplyRequest.getUploadingFinishDate(), equalTo(Instant.parse("2020-10-12T06:00:00Z")));
    }

    @Test
    void createEmptySupplyRequest() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_empty_supply.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/supplies"))
                .andExpect(content().json(IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(
                        "valid-empty-supply-with-courier.json")), StandardCharsets.UTF_8), true))
                .andRespond(returnResponseCreator);

        ShopRequestDTO supplyRequest = clientApi.createEmptySupplyRequest(createSupplyDTO());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.ORDERS_SUPPLY.getId()));
        assertThat(supplyRequest.getStockType(), equalTo(StockType.DEFECT));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(0L));
        assertCourierIsDeserializedCorrectly(supplyRequest.getCourier(), "Олег");
    }

    @Test
    void createEmptyWithdrawRequest() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_empty_withdraw.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/withdraws"))
                .andExpect(content().json(IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(
                        "valid-empty-withdraw-with-courier.json")), StandardCharsets.UTF_8), true))
                .andRespond(returnResponseCreator);

        ShopRequestDTO supplyRequest = clientApi.createEmptyWithdrawRequest(createWithdrawDTO());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.ORDERS_WITHDRAW.getId()));
        assertThat(supplyRequest.getStockType(), equalTo(StockType.DEFECT));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(0L));
        assertCourierIsDeserializedCorrectly(supplyRequest.getCourier(), "Олег");
        assertThat(supplyRequest.getSupplyRequestId(), equalTo(123L));
    }

    @Test
    void createSupplyRequest() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_supply_with_registry.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/supplies-with-registry"))
                .andExpect(content().json(IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(
                        "deprecated-supply-serialized-without-boxes.json")), StandardCharsets.UTF_8), true))
                .andRespond(returnResponseCreator);

        ShopRequestDTO supplyRequest = clientApi.createRequestAndPutRegistry(createSupplyWithRegistryDTO());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.ORDERS_SUPPLY.getId()));
        assertThat(supplyRequest.getStockType(), equalTo(StockType.DEFECT));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(0L));
        assertCourierIsDeserializedCorrectly(supplyRequest.getCourier(), "Олег");
    }

    @Test
    void createTransfer() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_transfer_response.json")), StandardCharsets.UTF_8));

        String request = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("create_transfer_request.json")), StandardCharsets.UTF_8);

        mockServer.expect(requestTo(host + "/transfer/create"))
                .andExpect(content().json(request))
                .andRespond(returnResponseCreator);

        CreateTransferForm transfer = new CreateTransferForm();
        transfer.setServiceId(172L);
        transfer.setSupplierId(123456);
        transfer.setStockTypeFrom(StockType.DEFECT);
        transfer.setStockTypeTo(StockType.PLAN_UTILIZATION);
        transfer.setComment("Утилизация");
        transfer.setTransferCreationType(TransferCreationType.BY_UTILIZER);
        CreateTransferItemForm item = new CreateTransferItemForm();
        item.setArticle("sku1");
        item.setCount(3);
        transfer.setItems(Collections.singletonList(item));
        TransferDetailsDTO transferDto = clientApi.createTransferRequest(transfer);
        assertThat(transferDto, notNullValue());
        assertThat(transferDto.getTransferId(), equalTo(1L));
        assertThat(transferDto.getServiceId(), equalTo(172L));
        assertThat(transferDto.getServiceTransferId(), nullValue());
        assertThat(transferDto.getSupplierId(), equalTo(123456L));
        assertThat(transferDto.getCreatedAt(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(transferDto.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(transferDto.getMovingFromStockType(), equalTo(StockType.DEFECT));
        assertThat(transferDto.getMovingToStockType(), equalTo(StockType.PLAN_UTILIZATION));
        assertThat(transferDto.getStatus(), equalTo(RequestStatus.PREPARED_FOR_CREATION));
        assertThat(transferDto.getItems().size(), equalTo(1));
        assertThat(transferDto.getTransferCreationType(), equalTo(TransferCreationType.BY_UTILIZER));
    }

    private CreateRegistryDTO createRegistryDTO() {
        CreateRegistryDTO registryDTO = new CreateRegistryDTO();

        registryDTO.setType(RequestType.SUPPLY);
        registryDTO.setExternalRegisterId("2341431");
        registryDTO.setIgnoreShipmentDateValidation(false);

        RegistryUnitPartialIdDTO registryUnitPartialIdDTO =
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.CIS, "010942102361011221dXp");
        RegistryUnitIdDTO registryUnitIdDTO = new RegistryUnitIdDTO(Set.of(registryUnitPartialIdDTO));

        LogisticUnitMetaDTO logisticUnitMetaDTO = new LogisticUnitMetaDTO();
        logisticUnitMetaDTO.setUnitId(registryUnitIdDTO);

        LogisticUnitDTO logisticUnitDTO = new LogisticUnitDTO();
        logisticUnitDTO.setMeta(logisticUnitMetaDTO);

        registryDTO.setLogisticUnits(List.of(logisticUnitDTO));
        return registryDTO;
    }

    private PutSupplyRequestDTO createSupplyDTO() {
        PutSupplyRequestDTO supply = new PutSupplyRequestDTO();
        supply.setDate(OffsetDateTime.parse("2018-01-06T10:10:10+03:00"));
        supply.setSupplierId(1L);
        supply.setComment("some comment");
        supply.setType(RequestType.ORDERS_SUPPLY.getId());
        supply.setStockType(StockType.DEFECT);
        supply.setExternalRequestId("2341431");
        supply.setLogisticsPointId(12341L);
        supply.setCourier(createCourierDTO());
        supply.setTransportationId("TMT123");
        return supply;
    }

    private PutWithdrawRequestDTO createWithdrawDTO() {
        PutWithdrawRequestDTO withdraw = new PutWithdrawRequestDTO();
        withdraw.setDate(OffsetDateTime.parse("2018-01-06T10:10:10+03:00"));
        withdraw.setSupplierId(1L);
        withdraw.setComment("some comment");
        withdraw.setType(11);
        withdraw.setStockType(StockType.DEFECT);
        withdraw.setExternalRequestId("2341431");
        withdraw.setLogisticsPointId(12341L);
        withdraw.setCourier(createCourierDTO());
        withdraw.setTransportationId("TMT456");
        withdraw.setSupplyRequestId(123L);
        return withdraw;
    }

    private RegistryRestrictedDataDTO createRestrictedDataDTO() {
        RegistryRestrictedDataDTO dto = new RegistryRestrictedDataDTO();
        dto.setPrimaryDocument(createPrimaryDocumentDTO());
        return dto;
    }

    private InboundPrimaryDocumentDTO createPrimaryDocumentDTO() {
        InboundPrimaryDocumentDTO dto = new InboundPrimaryDocumentDTO();
        dto.setArrivalDate(OffsetDateTime.parse("2020-10-10T00:00:00+03:00"));
        dto.setDataEnteredByMerchant(createDataEnteredByMerchantDTO());
        return dto;
    }

    private DataEnteredByMerchantDTO createDataEnteredByMerchantDTO() {
        DataEnteredByMerchantDTO dto = new DataEnteredByMerchantDTO();
        dto.setNumber("number-1");
        dto.setDate(OffsetDateTime.parse("2020-10-09T00:00:00+03:00"));
        dto.setPrice(BigDecimal.valueOf(100));
        dto.setTax(BigDecimal.valueOf(20));
        dto.setUntaxedPrice(BigDecimal.valueOf(80));
        return dto;
    }

    private void assertCustomerReturnCreation(boolean created, HttpStatus status, boolean withUploadingFinishDate) {
        ResponseCreator taskResponseCreator = withStatus(status)
                .contentType(APPLICATION_JSON);

        mockServer.expect(requestTo(host + "/returns/" + RETURN_ID))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(withUploadingFinishDate ? RETURN_ITEMS_WITH_UPLOADING_FINISH_DATE_JSON :
                        RETURN_ITEMS_JSON))
                .andRespond(taskResponseCreator);

        boolean result;
        if (withUploadingFinishDate) {
            result = clientApi.sendCustomerReturns(SERVICE_ID, RETURN_ID, Arrays.asList(
                    returnItem(1L, "art1"),
                    returnItem(2L, "art2")),
                    Instant.parse("2020-09-12T06:00:00Z")
            );
        } else {
            result = clientApi.sendCustomerReturns(SERVICE_ID, RETURN_ID, Arrays.asList(
                    returnItem(1L, "art1"),
                    returnItem(2L, "art2")
            ));
        }

        assertThat(result, equalTo(created));
    }

    @Test
    void acceptCancellation() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-cancellation"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(OK));

        clientApi.acceptRequestCancellation(REQUEST_ID);
    }

    @Test
    void rejectCancellation() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/reject-cancellation"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(OK));

        clientApi.rejectRequestCancellation(REQUEST_ID);
    }

    @Test
    void acceptRequestByService() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-by-service"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(ACCEPT_REQUEST_JSON))
                .andRespond(withStatus(OK));

        clientApi.acceptRequestByService(REQUEST_ID, new RequestAcceptDTO(SERVICE_REQUEST_ID));
    }

    @Test
    void rejectRequestByService() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/reject-by-service"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string("{\"error\":\"Error\"}"))
                .andRespond(withStatus(OK));

        clientApi.rejectRequestByService(REQUEST_ID, new RequestRejectDTO("Error"));
    }

    @Test
    void acceptUpdating() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-updating"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(OK));

        clientApi.acceptRequestUpdating(REQUEST_ID);
    }

    @Test
    void rejectUpdating() {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/reject-updating"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(OK));

        clientApi.rejectRequestUpdating(REQUEST_ID);
    }

    @Test
    void sendDivergenceActToEmails() {
        mockServer.expect(requestTo(host + "/unredeemed/send-act"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST)
                        .contentType(APPLICATION_OCTET_STREAM));
        assertThrows(HttpTemplateException.class,
                () -> clientApi.generateDivergenceAct(new PrimaryDivergenceActDto()));
    }

    private CustomerReturnItemDTO returnItem(final long supplierId, final String article) {
        final CustomerReturnItemDTO item = new CustomerReturnItemDTO();
        item.setSupplierId(supplierId);
        item.setArticle(article);
        return item;
    }

    @Test
    void getLastRequestItems() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_request_items.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/suppliers/1/last-items")))
                .andRespond(returnResponseCreator);

        final LastShopRequestFilterDTO filter = LastShopRequestFilterDTO.builder()
                .withShopId(1L)
                .addArticle("abc").addArticle("abcd")
                .addServiceId(100L).addServiceId(200L)
                .build();
        RequestItemDTOContainer actual = clientApi.getLastRequestItems(filter);

        RequestItemDTO item1 = new RequestItemDTO();
        item1.setArticle("abc");
        item1.setBarcodes(Arrays.asList("11", "22"));
        item1.setCount(3);
        item1.setFactCount(2);
        item1.setDefectCount(1);
        item1.setName("offer_1");
        item1.setMarketName("market_name_1");
        item1.setSku(2222L);
        item1.setSupplyPrice(BigDecimal.valueOf(5050).movePointLeft(2));
        item1.setVatRate(VatRate.VAT_0);
        item1.setErrors(Arrays.asList(
                RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND,
                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND
        ));
        item1.setLength(BigDecimal.valueOf(10.0));
        item1.setWidth(BigDecimal.valueOf(4.0));
        item1.setHeight(BigDecimal.valueOf(3.0));
        item1.setCargoTypes(List.of(200));

        RequestItemDTO item2 = new RequestItemDTO();
        item2.setArticle("abcd");
        item2.setBarcodes(Arrays.asList("111", "222"));
        item2.setCount(3);
        item2.setName("offer_2");
        item2.setMarketName("market_name_2");
        item2.setSku(3333L);
        item2.setSupplyPrice(BigDecimal.valueOf(5050).movePointLeft(2));
        item2.setVatRate(VatRate.VAT_0);

        RequestItemDTOContainer expected = new RequestItemDTOContainer();
        expected.setTotalCount(6L);
        expected.setTotalFactCount(2L);
        expected.setTotalDefectCount(1L);
        expected.setTotalSupplyPrice(BigDecimal.valueOf(10100).movePointLeft(2));
        expected.setTotalPages(2);
        expected.setPageNumber(1);
        expected.setTotalElements(2);
        expected.addItem(item1);
        expected.addItem(item2);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    void getSuppliersWithFirstFinishedSupplyInfo() throws IOException {
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("suppliers-with-finished-inbound-info.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/suppliers/suppliers-with-first-finished-inbound-info")))
                .andRespond(responseCreator);

        List<SupplierWithFirstFinishedSupplyInfo> actual = clientApi.getSuppliersWithFirstFinishedSupplyInfo()
                .getSuppliersWithFirstFinishedSupplyInfos();
        SupplierWithFirstFinishedSupplyInfo firstExpected = new SupplierWithFirstFinishedSupplyInfo();
        firstExpected.setSupplierId(1L);
        firstExpected.setFirstFinishedSupplyId(3L);
        firstExpected.setFirstFinishedSupplyUpdatedAt(LocalDateTime.parse("2020-02-25T01:01:01"));
        SupplierWithFirstFinishedSupplyInfo secondExpected = new SupplierWithFirstFinishedSupplyInfo();
        secondExpected.setSupplierId(3L);
        secondExpected.setFirstFinishedSupplyId(5L);
        secondExpected.setFirstFinishedSupplyUpdatedAt(LocalDateTime.parse("2019-02-01T13:00:00"));

        assertThat(actual, hasSize(2));
        ReflectionAssert.assertReflectionEquals(firstExpected, actual.get(0));
        ReflectionAssert.assertReflectionEquals(secondExpected, actual.get(1));
    }

    @Test
    void getSuppliersHavingAtLeastOneSupply() throws IOException {
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("suppliers-having-at-least-one-supply.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/suppliers/suppliers-having-at-least-one-supply")))
                .andRespond(responseCreator);

        List<Long> actual = clientApi.getSuppliersHavingAtLeastOneSupply().getSupplierIds();
        assertThat(actual, hasSize(3));
        assertThat(actual.get(0), equalTo(1L));
        assertThat(actual.get(1), equalTo(3L));
        assertThat(actual.get(2), equalTo(4L));
    }

    @Test
    void getRegistries() throws IOException {
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_registries.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/requests/123/registries")))
                .andRespond(responseCreator);

        RegistryDTOContainer actual = clientApi.getRegistries(123);
        RegistryDTOContainer expected = new RegistryDTOContainer();

        RegistryDTO registry1 = new RegistryDTO();
        registry1.setId(1L);
        registry1.setType(RegistryFlowType.PLAN);
        registry1.setRequestId(123L);
        registry1.setComment("Comment");
        registry1.setCreatedAt(LocalDateTime.parse("2020-10-10T10:00:00.000"));
        registry1.setUpdatedAt(LocalDateTime.parse("2020-10-10T10:00:00.000"));

        RegistryDTO registry2 = new RegistryDTO();
        registry2.setId(2L);
        registry2.setPartnerId("2");
        registry2.setType(RegistryFlowType.FACT);
        registry2.setRequestId(123L);
        registry2.setComment("Comment");
        registry2.setCreatedAt(LocalDateTime.parse("2020-10-10T10:00:00.000"));
        registry2.setUpdatedAt(LocalDateTime.parse("2020-10-10T10:00:00.000"));
        registry2.setPartnerDate(OffsetDateTime.parse("2020-10-08T10:00:00.000Z"));
        registry2.setRestrictedData(createRestrictedDataDTO());

        List<RegistryDTO> registries = new ArrayList<>();
        registries.add(registry1);
        registries.add(registry2);
        expected.setRegistries(registries);
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    void getRegistryUnits() throws IOException {
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_registry_units.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/requests/123/registry-units")))
                .andExpect(queryParam("registryIds", "2", "3"))
                .andExpect(queryParam("registryUnitTypes", "0", "10"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("size", "2"))
                .andRespond(responseCreator);

        RegistryUnitsFilterDTO filter = RegistryUnitsFilterDTO.Builder.builder(123L, 2L, 3L)
                .registryUnitTypes(EnumSet.of(RegistryUnitType.PALLET, RegistryUnitType.BOX))
                .page(1)
                .size(2)
                .build();
        RegistryUnitDTOContainer actual = clientApi.getRegistryUnits(filter);

        RegistryUnitDTOContainer expected = new RegistryUnitDTOContainer(3, 1, 9);
        RegistryUnitDTO unit1 = new RegistryUnitDTO();
        unit1.setRegistryId(2L);
        unit1.setUnitInfo(createInfo(RegistryUnitIdType.PALLET_ID, "PL0002", UnitCountType.FIT, 1));
        unit1.setType(RegistryUnitType.PALLET);
        expected.addUnit(unit1);

        RegistryUnitDTO unit2 = new RegistryUnitDTO();
        unit2.setRegistryId(2L);
        unit2.setUnitInfo(createInfo(
                RegistryUnitIdType.BOX_ID, "P0002",
                UnitCountType.FIT, 1,
                unit1.getUnitInfo().getUnitId())
        );
        unit2.setType(RegistryUnitType.BOX);
        expected.addUnit(unit2);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    void getSupplierRatingConfig() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_supplier_rating_config.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(startsWith(host + "/supplier-rating/config/10")))
                .andRespond(returnResponseCreator);

        SupplierRatingIntervalConfigResponse supplierRatingConfig = clientApi.getSupplierRatingConfig(10);

        SupplierRatingIntervalConfigResponse expected = new SupplierRatingIntervalConfigResponse();
        expected.setSupplierRatingValidationEnabled(true);
        expected.setNewbieInboundsCount(10);
        expected.setIsNewbie(true);

        CalendaringIntervalByRatingDto firstCalendaringInterval =
                getCalendaringIntervalByRating(1, LocalTime.MIDNIGHT, LocalTime.of(6, 0));
        CalendaringIntervalByRatingDto secondCalendaringInterval =
                getCalendaringIntervalByRating(2, LocalTime.MIDNIGHT, LocalTime.of(11, 0));
        CalendaringIntervalByRatingDto thirdCalendaringInterval =
                getCalendaringIntervalByRating(3, LocalTime.of(19, 0), LocalTime.MIDNIGHT);

        SupplierRatingIntervalDto firstInterval =
                getSupplierRatingInterval(1, 0, 45, false, singletonList(firstCalendaringInterval));
        SupplierRatingIntervalDto secondInterval = getSupplierRatingInterval(2, 45, 95, true,
                Arrays.asList(secondCalendaringInterval, thirdCalendaringInterval));
        SupplierRatingIntervalDto thirdInterval = getSupplierRatingInterval(3, 95, 100, true, Collections.emptyList());

        expected.setSupplierRatingIntervals(Arrays.asList(firstInterval, secondInterval, thirdInterval));

        ReflectionAssert.assertReflectionEquals(expected, supplierRatingConfig);
    }

    @Test
    void getQuota() throws IOException {
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("get_quota_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(startsWith(host + "/quota?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("dailyLimitsType", "MOVEMENT_SUPPLY"))
                .andExpect(queryParam("warehouses", "300", "400"))
                .andExpect(queryParam("supplierType", "REAL_SUPPLIER"))
                .andExpect(queryParam("dates", "2020-11-11", "2020-11-12"))
                .andExpect(queryParam("exceptBookings", "1", "2"))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        GetQuotaFilterDto filter = getGetQuotaFilter();
        GetQuotaResponseDto actualResponse = clientApi.getQuota(filter);
        GetQuotaResponseDto expectedResponse = getGetQuotaResponse();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void getQuotav2() throws IOException {
        String expectedRequest = IOUtils.toString(Objects.requireNonNull(
            getSystemResourceAsStream("get_quota_request.json")), StandardCharsets.UTF_8);
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
            getSystemResourceAsStream("get_quota_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(startsWith(host + "/quota/v2")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(expectedRequest))
            .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        GetQuotaFilterDtov2 filter = getGetQuotaFilterv2();
        GetQuotaResponseDto actualResponse = clientApi.getQuotav2(filter);
        GetQuotaResponseDto expectedResponse = getGetQuotaResponse();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void getQuotaWithDestinations() throws IOException {
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("get_quota_with_destinations_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(startsWith(host + "/quota/with-destinations?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("dailyLimitsType", "SUPPLY"))
                .andExpect(queryParam("warehouses", "172"))
                .andExpect(queryParam("destinationWarehouseIds", "147", "171"))
                .andExpect(queryParam("supplierType", "FIRST_PARTY"))
                .andExpect(queryParam("dates", "2020-11-11", "2020-11-12"))
                .andExpect(queryParam("exceptBookings", "10"))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        GetQuotaWithDestinationsDto filter = getGetQuotaWithDestinationsFilter();
        GetQuotaWithDestinationsResponseDto actualResponse = clientApi.getQuotaWithDestinations(filter);
        GetQuotaWithDestinationsResponseDto expectedResponse = getGetQuotaWithDestinationsResponse();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    private GetQuotaFilterDto getGetQuotaFilter() {
        return GetQuotaFilterDto.builder()
                .dailyLimitsType(DailyLimitsType.MOVEMENT_SUPPLY)
                .warehouses(List.of(300L, 400L))
                .supplierType(SupplierType.REAL_SUPPLIER)
                .dates(List.of(LocalDate.of(2020, 11, 11), LocalDate.of(2020, 11, 12)))
                .exceptBookings(List.of(1L, 2L))
                .build();
    }

    private GetQuotaFilterDtov2 getGetQuotaFilterv2() {
        var dates = List.of(LocalDate.of(2020, 11, 11), LocalDate.of(2020, 11, 12));
        return GetQuotaFilterDtov2.builder()
            .dailyLimitsType(DailyLimitsType.MOVEMENT_SUPPLY)
            .warehousesToDates(Map.of(300L, dates, 400L, dates))
            .supplierType(SupplierType.REAL_SUPPLIER)
            .exceptBookings(List.of(1L, 2L))
            .build();
    }

    private GetQuotaResponseDto getGetQuotaResponse() {
        return GetQuotaResponseDto.builder()
                .availableQuotasForServices(List.of(
                        AvailableQuotasForServiceDto.builder()
                                .serviceId(300L)
                                .availableQuotasForDates(List.of(
                                        AvailableQuotasForDateDto.builder()
                                                .date(LocalDate.of(2020, 11, 11))
                                                .items(1000L)
                                                .pallets(100L).build(),
                                        AvailableQuotasForDateDto.builder()
                                                .date(LocalDate.of(2020, 11, 12))
                                                .items(1500L)
                                                .pallets(120L).build()
                                        )
                                ).build(),
                        AvailableQuotasForServiceDto.builder()
                                .serviceId(400)
                                .availableQuotasForDates(List.of(
                                        AvailableQuotasForDateDto.builder()
                                                .date(LocalDate.of(2020, 11, 11))
                                                .items(0L)
                                                .pallets(10L).build(),
                                        AvailableQuotasForDateDto.builder()
                                                .date(LocalDate.of(2020, 11, 12))
                                                .items(300L)
                                                .pallets(0L).build()
                                )).build()
                )).build();
    }

    private GetQuotaWithDestinationsDto getGetQuotaWithDestinationsFilter() {
        return GetQuotaWithDestinationsDto.builder()
                .dailyLimitsType(DailyLimitsType.SUPPLY)
                .warehouses(List.of(172L))
                .destinationWarehouseIds(Set.of(147L, 171L))
                .supplierType(SupplierType.FIRST_PARTY)
                .dates(List.of(LocalDate.of(2020, 11, 11), LocalDate.of(2020, 11, 12)))
                .exceptBookings(List.of(10L))
                .build();
    }

    private GetQuotaWithDestinationsResponseDto getGetQuotaWithDestinationsResponse() {
        return GetQuotaWithDestinationsResponseDto.builder()
                .availableQuotasForServiceAndDestination(
                        List.of(
                                AvailableQuotaForServiceAndDestinationDto.builder()
                                        .serviceId(172L)
                                        .destinationId(171L)
                                        .availableQuotasForDates(List.of(
                                                AvailableQuotasForDateDto.builder()
                                                        .date(LocalDate.of(2020, 11, 11))
                                                        .items(1500L)
                                                        .pallets(120L).build(),
                                                AvailableQuotasForDateDto.builder()
                                                        .date(LocalDate.of(2020, 11, 12))
                                                        .items(500L)
                                                        .pallets(24L).build()
                                        ))
                                        .build(),
                                AvailableQuotaForServiceAndDestinationDto.builder()
                                        .serviceId(172L)
                                        .destinationId(147L)
                                        .availableQuotasForDates(List.of(
                                                AvailableQuotasForDateDto.builder()
                                                        .date(LocalDate.of(2020, 11, 11))
                                                        .items(2000L)
                                                        .pallets(1198L).build(),
                                                AvailableQuotasForDateDto.builder()
                                                        .date(LocalDate.of(2020, 11, 12))
                                                        .items(6000L)
                                                        .pallets(22L).build()
                                        ))
                                        .build()
                        )
                ).build();
    }

    @Test
    void takeQuota() throws IOException {
        String expectedRequest = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("take_quota_request.json")), StandardCharsets.UTF_8);
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("take_quota_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(equalTo(host + "/quota")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedRequest))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        TakeQuotaResponseDto actualResponse = clientApi.takeQuota(getTakeQuotaDto());
        TakeQuotaResponseDto expectedResponse =
                TakeQuotaResponseDto.builder().quotaDate(LocalDate.of(2020, 11, 11)).build();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void takeOrUpdateConsolidatedQuotas() throws IOException {
        String expectedRequest = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("take_quotas_request.json")), StandardCharsets.UTF_8);
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("take_quotas_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(equalTo(host + "/quota/take-or-update-consolidated")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedRequest))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        TakeQuotasManyBookingsResponseDto actualResponse = clientApi.takeOrUpdateConsolidatedQuotas(getTakeQuotasDto());
        TakeQuotasManyBookingsResponseDto expectedResponse =
                TakeQuotasManyBookingsResponseDto.builder()
                        .quotaDate(LocalDate.of(2020, 11, 11))
                        .build();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void updateQuotaQuota() throws IOException {
        String expectedRequest = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("update_quota_request.json")), StandardCharsets.UTF_8);
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("update_quota_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(equalTo(host + "/quota/update")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedRequest))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        TakeQuotaResponseDto actualResponse = clientApi.updateQuota(55, 100, 10, false);
        TakeQuotaResponseDto expectedResponse =
                TakeQuotaResponseDto.builder().quotaDate(LocalDate.of(2020, 11, 11)).build();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void updateBookingId() throws IOException {
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("update_booking_id_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(equalTo(host + "/quota/updateBookingId/55/100")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));
        TakeQuotaResponseDto actualResponse = clientApi.updateBookingId(55, 100);
        TakeQuotaResponseDto expectedResponse =
                TakeQuotaResponseDto.builder().quotaDate(LocalDate.of(2020, 11, 11)).build();
        ReflectionAssert.assertReflectionEquals(expectedResponse, actualResponse);
    }

    @Test
    void commitXDocFinalInboundDate() {
        mockServer
                .expect(requestTo(host + "/xdoc-inbound/final-date"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"date\":\"2021-05-20T10:10:10+03:00\", \"shopRequestId\":1}"))
                .andRespond(withStatus(OK));

        XDocFinalInboundDateDTO dto = new XDocFinalInboundDateDTO()
                .setDate(OffsetDateTime.parse("2021-05-20T10:10:10+03:00"))
                .setShopRequestId(1L);
        clientApi.commitXDocFinalInboundDate(dto);
    }

    @Test
    void getQuotaInfo() throws Exception {
        String mockedResponse = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("get_quota_info_response.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(startsWith(host + "/quota/171/2019-10-21/2019-10-31?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("limitType", "WITHDRAW", "MOVEMENT_SUPPLY"))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));

        QuotaInfosDTO info = clientApi.getQuotaInfo(171, LocalDate.of(2019, 10, 21), LocalDate.of(2019, 10, 31),
                ImmutableList.of(DailyLimitsType.MOVEMENT_SUPPLY, DailyLimitsType.WITHDRAW));
        assertEquals(6, info.getQuotaInfos().size());
    }

    private TakeQuotaDto getTakeQuotaDto() {
        return TakeQuotaDto.builder()
                .bookingId(55L)
                .quotaType(DailyLimitsType.MOVEMENT_SUPPLY)
                .serviceId(300L)
                .supplierType(SupplierType.THIRD_PARTY)
                .items(100L)
                .pallets(10L)
                .measurements(50L)
                .possibleDates(List.of(LocalDate.of(2020, 11, 11), LocalDate.of(2020, 11, 12)))
                .build();
    }

    private TakeQuotasManyBookingsRequestDto getTakeQuotasDto() {
        return TakeQuotasManyBookingsRequestDto.builder()
                .bookingDtos(
                        List.of(TakeQuotaBookingDto.builder().bookingId(55L).items(100L).pallets(10L).measurements(50L)
                                .build()))
                .quotaType(DailyLimitsType.MOVEMENT_SUPPLY)
                .serviceId(300L)
                .possibleDates(List.of(LocalDate.of(2020, 11, 11)))
                .build();

    }

    private RegistryUnitInfoDTO createInfo(RegistryUnitIdType idType, String idValue,
                                           UnitCountType countType, int countValue,
                                           RegistryUnitIdDTO... parents) {
        RegistryUnitInfoDTO info = new RegistryUnitInfoDTO();
        info.setUnitId(RegistryUnitIdDTO.of(idType, idValue));
        RegistryUnitCountsInfoDTO counts = new RegistryUnitCountsInfoDTO();
        RegistryUnitCountDTO count = new RegistryUnitCountDTO(
                countType,
                countValue,
                List.of(RegistryUnitIdDTO.of(RegistryUnitIdType.CIS, "CIS0002")),
                null);
        counts.setUnitCounts(singletonList(count));
        info.setUnitCountsInfo(counts);
        info.setParentUnitIds(Arrays.asList(parents));

        return info;
    }

    private CalendaringIntervalByRatingDto getCalendaringIntervalByRating(long id, LocalTime from, LocalTime to) {
        CalendaringIntervalByRatingDto calendaringInterval = new CalendaringIntervalByRatingDto();
        calendaringInterval.setId(id);
        calendaringInterval.setFromTime(from);
        calendaringInterval.setToTime(to);
        return calendaringInterval;
    }

    private SupplierRatingIntervalDto getSupplierRatingInterval(
            long id, int from, int to, boolean inboundAllowed,
            List<CalendaringIntervalByRatingDto> calendaringIntervals) {
        SupplierRatingIntervalDto interval = new SupplierRatingIntervalDto();
        interval.setId(id);
        interval.setFromRating(from);
        interval.setToRating(to);
        interval.setInboundAllowed(inboundAllowed);
        interval.setCalendaringIntervals(calendaringIntervals);
        return interval;
    }

    @Test
    void releaseQuota() {
        mockServer
                .expect(requestTo(startsWith(host + "/quota")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(queryParam("bookingId", "101", "102"))
                .andRespond(withStatus(OK));

        clientApi.releaseQuota(Set.of(101L, 102L));
    }

    @Test
    void putBookedSlot() throws IOException {
        String expectedRequest = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("put_booked_slot.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(startsWith(host + "/booking/slot")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(expectedRequest));
    }

    @Test
    void deactivateBooking() {
        mockServer
                .expect(requestTo(startsWith(host + "/booking/slot")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(queryParam("bookingId", "101", "103"))
                .andRespond(withStatus(OK));

        clientApi.deactivateBooking(List.of(101L, 103L));
    }

    @Test
    void testCancelRequest() {
        String mockedResponse = "{\"status\": 0}";

        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/cancel"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));

        clientApi.cancelRequest(REQUEST_ID);
    }

    @Test
    void testChangeXDockRequestStatus() {
        String mockedResponse = "{"
                + "  \"requestId\": 1,"
                + "  \"requestType\": 21,"
                + "  \"detailsLoaded\": false,"
                + "  \"changedAt\": \"2021-05-20T10:10:10\","
                + "  \"receivedChangedAt\": \"2021-05-20T10:10:10\","
                + "  \"oldStatus\": \"3\","
                + "  \"newStatus\": \"210\""
                + "}";

        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/xDockStatus"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(mockedResponse));

        clientApi.changeXDockRequestStatus(REQUEST_ID, RequestStatus.ACCEPTED_BY_XDOC_SERVICE);
    }

    @Test
    void findUtilizationTransferItemsCountTest() throws IOException {
        final ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_utilization_items_count_response.json")),
                        StandardCharsets.UTF_8));

        mockServer
                .expect(requestTo(host + "/utilization/get-utilization-items-count"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"supplierSkuKeys\":[{\"supplierId\":7,\"sku\":\"sku7\"}]}"))
                .andRespond(returnResponseCreator);

        UtilizationItemCountRequestDto dto = UtilizationItemCountRequestDto.builder()
                .addSupplierSkuKey(new SupplierSkuDto(7L, "sku7"))
                .build();

        clientApi.findUtilizationTransferItemsCount(dto);
    }

    @Test
    void testGetRequestsForYard() {

        final LocalDateTime dateTime = LocalDateTime.of(1999, 9, 9, 9, 9, 9);

        LocalDate requestDateFrom = dateTime.toLocalDate();
        LocalDate requestDateTo = dateTime.toLocalDate().plusDays(1);
        String requestId = "123";
        long shopId = 344L;
        String realSupplierIds = "0121212";
        long serviceId = 567L;
        boolean vetis = true;
        Set<RequestItemAttribute> attributes = Set.of(RequestItemAttribute.CTM);
        boolean hasUTDDocuments = true;
        boolean hasDocumentTicketUrl = true;
        boolean documentTicketStatusIsOpen = true;
        int page = 1;
        int size = 10;
        String sort = "createdAt,DESC";
        RequestDocumentType documentType = RequestDocumentType.ELECTRONIC;

        mockServer.expect(requestTo(startsWith(host + "/requests/yard-filter")))
                .andExpect(queryParam("requestIds", requestId))
                .andExpect(queryParam("shopIds", shopId + ""))
                .andExpect(queryParam("realSupplierIds", realSupplierIds))
                .andExpect(queryParam("serviceIds", serviceId + ""))
                .andExpect(queryParam("types", WITHDRAW.getId() + ""))
                .andExpect(queryParam("statuses", RequestStatus.FINISHED.getId() + ""))
                .andExpect(queryParam("requestDateFrom", requestDateFrom.format(DateTimeFormatter.ISO_DATE)))
                .andExpect(queryParam("requestDateTo", requestDateTo.format(DateTimeFormatter.ISO_DATE)))
                .andExpect(queryParam("documentTypes", documentType.getId() + ""))
                .andExpect(queryParam("vetis", vetis + ""))
                .andExpect(queryParam("hasUTDDocuments", hasUTDDocuments + ""))
                .andExpect(queryParam("hasDocumentTicketUrl", hasDocumentTicketUrl + ""))
                .andExpect(queryParam("documentTicketStatusIsOpen", documentTicketStatusIsOpen + ""))
                .andExpect(queryParam("attributes", RequestItemAttribute.CTM + ""))
                .andExpect(queryParam("page", page + ""))
                .andExpect(queryParam("size", size + ""))
                .andExpect(queryParam("sort", sort))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body("{}"));

        ShopRequestYardFilterDTO filter = new ShopRequestYardFilterDTO();
        filter.setRequestIds(List.of(requestId));
        filter.setShopIds(List.of(shopId));
        filter.setRealSupplierIds(List.of(realSupplierIds));
        filter.setServiceIds(List.of(serviceId));
        filter.setTypes(List.of(WITHDRAW));
        filter.setStatuses(List.of(RequestStatus.FINISHED));
        filter.setRequestDateFrom(requestDateFrom);
        filter.setRequestDateTo(requestDateTo);
        filter.setVetis(vetis);
        filter.setDocumentTypes(List.of(documentType));
        filter.setHasUTDDocuments(hasUTDDocuments);
        filter.setHasDocumentTicketUrl(hasDocumentTicketUrl);
        filter.setDocumentTicketStatusIsOpen(documentTicketStatusIsOpen);
        filter.setAttributes(attributes);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSort(sort);

        clientApi.getRequestsForYard(filter);

        mockServer.verify();
    }


    @Test
    void testFindRealSupplyInfoByIds() {

        mockServer.expect(requestTo(startsWith(host + "/supplier-info/aggregated-by-first-name")))
                .andExpect(queryParam("supplierId", "a", "b"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body("{}"));

        clientApi.findRealSupplyInfoByIds(List.of("a", "b"));

        mockServer.verify();
    }

    @Test
    void getRequestTypeStatusMapTest() {
        mockServer.expect(requestTo(startsWith(host + "/requests/request-type-status-map")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body("{}"));

        clientApi.getRequestTypeStatusMap();
        mockServer.verify();
    }

    @Test
    void testProcessRejection() {
        String expectedJson =
                "{\"id\":1,\"rejectionComment\":\"test comment\",\"createdAt\":\"2020-10-10T10:00:00\",\n" +
                        "  \"rejectionReasons\":[{\"id\":11,\"title\":\"reason1\"},\n" +
                        "  {\"id\":12,\"title\":\"reason2\"}],\"includedRequests\":[1,2,3],\"active\":true}";

        mockServer
                .expect(requestTo(host + "/yard/rejection"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedJson))
                .andRespond(withStatus(OK));

        clientApi.processRejection(new RejectionDto(
                1L,
                "test comment",
                LocalDateTime.parse("2020-10-10T10:00:00.000"),
                List.of(new RejectionReasonDto(11L, "reason1"), new RejectionReasonDto(12L, "reason2")),
                List.of(1L, 2L, 3L),
                true
        ));
    }

    @Test
    void testGetDocu() throws IOException {
        mockServer
                .expect(requestTo(host +
                        "/reports/request-daily-report/303"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andRespond(
                        withStatus(OK)
                                .contentType(APPLICATION_OCTET_STREAM)
                                .body("myStream".getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals("myStream",
                IOUtils.toString(clientApi.getDocuStream("303"),
                        StandardCharsets.UTF_8.name()));


    }

    @Test
    void pushCargoUnitCount() {
        mockServer
                .expect(requestTo(host + "/requests/1/xdock-cargo-unit-counts"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"pallets\": 10, \"boxes\": 20 }"))
                .andRespond(withStatus(OK));

        CargoUnitCountRequestDTO countsDto = new CargoUnitCountRequestDTO();
        countsDto.setPallets(10);
        countsDto.setBoxes(20);

        clientApi.pushCargoUnitCount(1L, countsDto);
    }
}
