package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.sc.core.domain.inbound.model.InboundAvailableAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistory;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.PLANNED;
import static ru.yandex.market.sc.internal.test.Template.fromFile;
import static ru.yandex.market.sc.internal.util.ScIntControllerCaller.PUT_CAR_INFO_REQUEST;


@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class FFApiControllerGetInboundTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final XDocFlow flow;
    private final ScIntControllerCaller caller;
    private final InboundRepository inboundRepository;
    private final InboundStatusHistoryRepository inboundStatusHistoryRepository;
    private SortingCenter sortingCenter;
    private String requestUID;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        requestUID = UUID.randomUUID().toString().replace("-", "");
        testFactory.setupMockClock(clock);
    }

    @Test
    @DisplayName("getInbound для не xDoc поставки")
    @SneakyThrows
    void getInboundNonXDoc() {
        var inbound = createdInbound("inbound-1", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-1", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-1", "palletId-1");

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        var expectedResponse = fromFile("ffapi/inbound/getInboundResponseTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .setValue("registryId.yandexId", registry.getExternalId())
                .setValue("registryId.partnerId", registry.getExternalId())
                .setValue("placeId", "placeId-1")
                .setValue("palletId", "palletId-1")
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getInboundStatus для не xDoc поставки")
    @SneakyThrows
    void getInboundStatusNonXDoc() {
        var inbound = createdInbound("inbound-2", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-2", "palletId-2");

        var request = fromFile("ffapi/inbound/getInboundStatusRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatus/inboundId/partnerId").string(inbound.getId().toString()))
                .andExpect(xpath("//inboundStatus/status/statusCode")
                        .string(Integer.toString(inbound.getInboundStatus().getCode())));
    }

    @Test
    @DisplayName("getInboundStatus для не xDoc поставки. Проверка времени выставления статуса.")
    @SneakyThrows
    void getInboundStatusNonXDocUpdatedAt() {
        var inbound = createdInbound("inbound-2", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-2", "palletId-2");

        var request = fromFile("ffapi/inbound/getInboundStatusRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        String historyRequest = fromFile("ffapi/inbound/getInboundStatusHistoryRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        String getStatusResponse = ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andReturn().getResponse().getContentAsString();
        String getStatusHistoryResponse = ScTestUtils.ffApiSuccessfulCall(mockMvc, historyRequest)
                .andReturn().getResponse().getContentAsString();
        String setDateStatus = StringUtils
                .substringBetween(getStatusResponse, "setDate", "</setDate>");
        String setDateStatusHistory = StringUtils
                .substringBetween(getStatusHistoryResponse, "setDate", "</setDate>");
        assertThat(setDateStatus).isEqualTo(setDateStatusHistory);
    }

    @Test
    @DisplayName("getInboundStatus для не xDoc поставки. В запросе несколько Id")
    @SneakyThrows
    void getInboundStatusNonXDocSeveralIds() {
        var inbound1 = createdInbound("inbound-1", InboundType.DEFAULT);
        var registry1 = testFactory.bindRegistry(inbound1, "registryId-1", PLANNED);
        testFactory.bindInboundOrder(inbound1, registry1, "placeId-1", "palletId-1");
        var inbound2 = createdInbound("inbound-2", InboundType.DEFAULT);
        var registry2 = testFactory.bindRegistry(inbound2, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound2, registry2, "placeId-2", "palletId-12");
        String unknown = "unknown";

        var request = fromFile("ffapi/inbound/getInboundStatusRequestSeveralIdsTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId1", inbound1.getExternalId())
                .setValue("inboundId.partnerId1", inbound1.getExternalId())
                .setValue("inboundId.yandexId2", inbound2.getExternalId())
                .setValue("inboundId.partnerId2", inbound2.getExternalId())
                .setValue("inboundId.yandexId3", unknown)
                .setValue("inboundId.partnerId3", unknown)
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus[1]/inboundId/yandexId").string(inbound1.getExternalId()))
                .andExpect(xpath("//inboundStatus[1]/inboundId/partnerId").string(inbound1.getExternalId()))
                .andExpect(xpath("//inboundStatus[2]/inboundId/yandexId").string(inbound2.getExternalId()))
                .andExpect(xpath("//inboundStatus[2]/inboundId/partnerId").string(inbound2.getExternalId()))
                .andExpect(xpath("//inboundStatus[3]/inboundId/yandexId").string(unknown))
                .andExpect(xpath("//inboundStatus[3]/inboundId/partnerId").string(unknown))
                .andExpect(xpath("//inboundStatus/status/statusCode")
                        .string(Integer.toString(inbound1.getInboundStatus().getCode())));
    }

    @Test
    @DisplayName("getInboundStatusHistory для не xDoc поставки")
    @SneakyThrows
    void getInboundStatusHistoryNonXDoc() {
        Inbound inbound = createdInbound("inbound-3", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-3", "palletId-3");

        String request = fromFile("ffapi/inbound/getInboundStatusHistoryRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatusHistory/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/inboundId/partnerId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/history/status").nodeCount(1))
                .andExpect(xpath("//inboundStatusHistory/history/status/statusCode")
                        .string(Integer.toString(inbound.getInboundStatus().getCode())));
    }

    @Test
    @DisplayName("getInbound для xDox поствки")
    @SneakyThrows
    void getInboundXDoc() {
        var inbound = createdInbound("inbound-4", InboundType.XDOC_TRANSIT);
        var registry = testFactory.bindRegistry(inbound, "registryId-4", FACTUAL);
        testFactory.bindRegistrySortable(registry, "XDOC-41", SortableType.XDOC_BOX);
        testFactory.bindRegistrySortable(registry, "XDOC-42", SortableType.XDOC_PALLET);

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        var expectedResponse = fromFile("ffapi/inbound/getInboundResponseXDocTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .setValue("registryId.yandexId", registry.getExternalId())
                .setValue("registryId.partnerId", registry.getExternalId())
                .setValue("boxId", "XDOC-41")
                .setValue("palletId", "XDOC-42")
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getInbound для xDox поставки без коробок")
    @SneakyThrows
    void getInboundXDocWithoutBoxes() {
        var inbound = createdInbound("inbound-4", InboundType.XDOC_TRANSIT);
        var registry = testFactory.bindRegistry(inbound, "registryId-4", FACTUAL);
        testFactory.bindRegistrySortable(registry, "XDOC-42", SortableType.XDOC_PALLET);

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        var expectedResponse = fromFile("ffapi/inbound/getInboundResponseXDocWithoutBoxesTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .setValue("registryId.yandexId", registry.getExternalId())
                .setValue("registryId.partnerId", registry.getExternalId())
                .setValue("palletId", "XDOC-42")
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(content().xml(expectedResponse));
    }


    @Test
    @DisplayName("getInbound для xDoc поставки, отсутствует первичный документ")
    @SneakyThrows
    void getInboundWithoutInboundPrimaryDocument() {
        Inbound inbound = createInboundWithDocument(null);

        var expectedResponse = fromFile("ffapi/inbound/primary_document/noFieldsArePresent.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, getInboundRequest(inbound))
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getInbound для xDoc поставки, в первичном документе указана только дата поставки")
    @SneakyThrows
    void getInboundOnlyInboundDateInPrimaryDocument() {
        Inbound inbound = createInboundWithDocument("{\"inboundDate\": \"2000-11-01\"}");

        var expectedResponse = fromFile("ffapi/inbound/primary_document/onlyInboundArrivalDateIsPresent.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, getInboundRequest(inbound))
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getInbound для xDoc поставки, первичный документ полностью заполнен")
    @SneakyThrows
    void getInboundAllFieldsInPrimaryDocument() {
        Inbound inbound = createInboundWithDocument("""
                {
                    "docNumber": "number-1",
                    "docDate": "2000-10-31",
                    "inboundDate": "2000-11-01",
                    "price": 100.0,
                    "tax": 20.0,
                    "untaxedPrice": 80.0
                  }
                """);
        var expectedResponse = fromFile("ffapi/inbound/primary_document/allFieldsArePresent.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, getInboundRequest(inbound))
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getInbound для xDoc поставки, в первичном документе не заполнены какие либо поля")
    @SneakyThrows
    void getInboundInvalidFieldsInPrimaryDocument() {
        var inbound = createInboundWithDocument("""
                {
                    "docNumber": "number-1",
                    "docDate": "2000-10-31",
                    "inboundDate": "2000-11-01",
                    "price": 100.0,
                    "tax": 20.0,
                    "untaxedPrice": 80.0
                  }
                """);

        // приведем в невалидное состояние
        var inboundInfo = inbound.getInboundInfo();
        inboundInfo.getDocumentInfo().setDocNumber(null);
        inbound.setInboundInfo(inboundInfo);
        inboundRepository.save(inbound);

        ScTestUtils.ffApiErrorCall(
            mockMvc,
            getInboundRequest(inbound),
            "Unexpected technical error. Please check logs for details."
        );
    }


    @Test
    @DisplayName("getInboundStatus для xDoc поставки")
    @SneakyThrows
    void getInboundStatusXDoc() {
        var inbound = createdInbound("inbound-5", InboundType.XDOC_TRANSIT);
        var registry = testFactory.bindRegistry(inbound, "registryId-5", FACTUAL);
        testFactory.bindRegistrySortable(registry, "XDOC-5", SortableType.XDOC_BOX);

        var request = fromFile("ffapi/inbound/getInboundStatusRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatus/inboundId/partnerId").string(inbound.getId().toString()))
                .andExpect(xpath("//inboundStatus/status/statusCode")
                        .string(Integer.toString(inbound.getInboundStatus().getCode())));
    }

    @Test
    @DisplayName("getInboundStatusHistory для xDoc поставки")
    @SneakyThrows
    void getInboundStatusHistoryXDoc() {
        Inbound inbound = createdInbound("inbound-6", InboundType.XDOC_TRANSIT);
        var registry = testFactory.bindRegistry(inbound, "registryId-6", FACTUAL);
        testFactory.bindRegistrySortable(registry, "XDOC-6", SortableType.XDOC_BOX);

        String request = fromFile("ffapi/inbound/getInboundStatusHistoryRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatusHistory/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/inboundId/partnerId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/history/status").nodeCount(1))
                .andExpect(xpath("//inboundStatusHistory/history/status/statusCode")
                        .string(Integer.toString(inbound.getInboundStatus().getCode())));
    }

    @Test
    @DisplayName("getInboundStatus CREATED, CONFIRMED, CAR_ARRIVED, READY_TO_RECEIVE через ff api возвращается как " +
            "CREATE")
    @SneakyThrows
    void getInboundStatusXDocReadyToReceiveStatus() {
        var inbound = flow.inboundBuilder("inbound-5")
                .confirm(false)
                .informationListBarcode("0000001001")
                .build()
                .getInbound();

        var request = fromFile("ffapi/inbound/getInboundStatusRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        // Только что созданный и не подтвержденный, возвращается в статусе CREATE
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatus/inboundId/partnerId").string(inbound.getId().toString()))
                .andExpect(xpath("//inboundStatus/status/statusCode").string(Integer.toString(StatusCode.CREATED.getCode())));


        // После update с подтверждением, возвращается с внутренним статусом CONFIRMED и внешним статусом CREATE
        testFactory.confirmInbound(inbound.getExternalId());
        inbound = inboundRepository.findByIdOrThrow(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CONFIRMED);

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatus/inboundId/partnerId").string(inbound.getId().toString()))
                .andExpect(xpath("//inboundStatus/status/statusCode").string(Integer.toString(StatusCode.CREATED.getCode())));

        caller.inboundCarArrived(inbound.getExternalId(), PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        // Подтвержден диспетчером на РЦ, возвращается с внутренним статусом READY_TO_RECEIVE и внешним статусом CREATE
        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk());
        inbound = inboundRepository.findByIdOrThrow(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.READY_TO_RECEIVE);

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatus/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatus/inboundId/partnerId").string(inbound.getId().toString()))
                .andExpect(xpath("//inboundStatus/status/statusCode").string(Integer.toString(StatusCode.CREATED.getCode())));
    }

    @Test
    @DisplayName("getInboundStatusHistory множество разных внутренних статусов, которые мапятся на один и тот же " +
            "внешний вернут одну запись")
    @SneakyThrows
    void multipleInternalStatusesThatMapToSameExternalStatusWillReturnSingleHistoryRecord() {
        var inbound = flow.inboundBuilder("inbound-6")
                .confirm(false)
                .informationListBarcode("0000001002")
                .build()
                .getInbound();

        testFactory.confirmInbound(inbound.getExternalId());

        caller.inboundCarArrived(inbound.getExternalId(), PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk());

        flow.toArrival("inbound-6")
                .linkPallets("XDOC-1");

        // множество статусов присутствует в истории
        assertThat(inboundStatusHistoryRepository.findAllByInboundExternalIdIn(List.of(inbound.getExternalId())))
                .map(InboundStatusHistory::getInboundStatus)
                .containsExactlyInAnyOrder(
                        InboundStatus.CREATED,
                        InboundStatus.CONFIRMED,
                        InboundStatus.CAR_ARRIVED,
                        InboundStatus.READY_TO_RECEIVE,
                        InboundStatus.ARRIVED
                );

        String request = fromFile("ffapi/inbound/getInboundStatusHistoryRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(xpath("//inboundStatusHistory/inboundId/yandexId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/inboundId/partnerId").string(inbound.getExternalId()))
                .andExpect(xpath("//inboundStatusHistory/history/status").nodeCount(2))
                .andExpect(xpath("//inboundStatusHistory/history/status[1]/statusCode").string(Integer.toString(StatusCode.CREATED.getCode())))
                .andExpect(xpath("//inboundStatusHistory/history/status[2]/statusCode").string(Integer.toString(StatusCode.ARRIVED.getCode())));
    }

    private Inbound createdInbound(String externalId, InboundType inboundType) {
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        return testFactory.createInbound(params);
    }

    private String getInboundRequest(Inbound inbound) {
        return fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .resolve();
    }

    private Inbound createInboundWithDocument(String documentRequest) {
        Warehouse warehouse = testFactory.storedWarehouse("samara-wh");

        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(warehouse.getYandexId())
                .informationListBarcode("000001317")
                .build()
                .getInbound();

        caller.inboundCarArrived(inbound.getInformationListCode(), PUT_CAR_INFO_REQUEST)
                .andExpect(status().isOk());

        if (documentRequest != null) {
            caller.putDocInfo(
                    inbound.getExternalId(),
                    documentRequest
            ).andExpect(status().isOk());
        }

        // формируем факт реестр
        flow.toArrival(inbound.getExternalId())
                .linkPallets("XDOC-1")
                .fixInbound();

        inbound = flow.getInbound(inbound.getExternalId());

        return inbound;
    }

}
