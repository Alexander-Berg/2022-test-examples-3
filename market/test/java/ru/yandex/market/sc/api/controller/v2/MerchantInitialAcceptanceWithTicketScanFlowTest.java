package ru.yandex.market.sc.api.controller.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.FlowOperationTestControllerCaller;
import ru.yandex.market.sc.api.domain.operation.ApiNextOperationDto;
import ru.yandex.market.sc.api.domain.operation.ApiOperationDto;
import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.SortableAcceptSimpleFlowOperationContextData;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.TicketScanFlowOperationContextData;
import ru.yandex.market.sc.core.domain.logbroker.publisher.YardLogbrokerEventPublisher;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.FinishAcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.TicketScanRequestDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_FLOW_TRACE_ID_HEADER;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MerchantInitialAcceptanceWithTicketScanFlowTest {
    private static final long UID = 124L;
    private static final String PROCESS_NAME = "MERCHANT_INITIAL_ACCEPTANCE";
    private static final String SORTABLE_ACCEPT_SIMPLE_CONFIG = "{\"validateMerchant\": true}";
    private static final String TICKET_ID = "TEST_TICKET_ID_123";
    private static final String FLOW_NAME = FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE_WITH_TICKET_SCAN.name();

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final OperationLogRepository operationLogRepository;

    private SortingCenter sortingCenter;
    private FlowOperationTestControllerCaller flowOperationControllerCaller;

    @MockBean
    private YardLogbrokerEventPublisher yardLogbrokerEventPublisher;
    @Qualifier("scApiJacksonObjectMapper")
    @Autowired
    private ObjectMapper jacksonObjectMapper;
    private Operation ticketScanOperation;
    private Operation sortableAcceptSimpleOperation;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);
        testFactory.storedCommonProcess();
        var zone = testFactory.storedZone(sortingCenter, "zone-0");
        flowOperationControllerCaller = new FlowOperationTestControllerCaller(mockMvc, UID, zone.getId(),
                sortingCenter.getId());

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.TICKET_SYSTEM_FLOW_ENABLED,
                true);
        ticketScanOperation =
                testFactory.storedOperation(OperationSystemName.TICKET_SCAN.name(), "Сканирование талона ЭО");
        sortableAcceptSimpleOperation =
                testFactory.storedOperation(OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(), "Сканирование сортабла");
        var flowOperationConfigMap = new HashMap<Operation, JsonNode>();
        flowOperationConfigMap.put(sortableAcceptSimpleOperation,
                testFactory.toJsonNode(SORTABLE_ACCEPT_SIMPLE_CONFIG));
        flowOperationConfigMap.put(ticketScanOperation, null);

        var flow = testFactory.storedFlow(
                FLOW_NAME,
                "Приёмка посылок водителя",
                flowOperationConfigMap
        );

        testFactory.storedProcess(PROCESS_NAME, PROCESS_NAME, List.of(flow));
    }

    @DisplayName("Полный проход по флоу приемки от мерча со сканированием талона ЭО")
    @Test
    @SneakyThrows
    void mainScenarioTest() {
        var flowTraceId = flowOperationControllerCaller.checkInFlow(FLOW_NAME)
                .andExpect(status().isOk())
                .andExpect(content().json(emptyTicketScanOperationContent(), true))
                .andReturn().getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER);

        flowOperationControllerCaller.finishTicketScan(
                PROCESS_NAME,
                FLOW_NAME,
                new TicketScanRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json(sortableAcceptContent(), true));

        var place = testFactory.createForToday(
                TestFactory.order(sortingCenter).externalId("1").places("1-0").build()
        ).getPlace();

        preAccept(flowTraceId, place);

        flowOperationControllerCaller.finishAccept(
                PROCESS_NAME,
                FLOW_NAME,
                new FinishAcceptSortableRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}", true));

        flowOperationControllerCaller.leaveFlow(flowTraceId).andExpect(status().isOk());

        assertThat(operationLogRepository.findAll().size()).isEqualTo(8);
    }

    @DisplayName("Выход из флоу и создание новой сессии при чекине")
    @Test
    @SneakyThrows
    void leaveFlowAndCheckInTest() {
        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(FLOW_NAME);

        flowOperationControllerCaller.finishTicketScan(
                PROCESS_NAME,
                FLOW_NAME,
                new TicketScanRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId);
        flowOperationControllerCaller.leaveFlow(flowTraceId);

        var ticketScanExpectedContent = emptyTicketScanOperationContent();
        var newFlowTraceId = flowOperationControllerCaller.checkInFlow(FLOW_NAME)
                .andExpect(content().json(ticketScanExpectedContent, true))
                .andReturn().getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER);
        assertThat(flowTraceId).isNotEqualTo(newFlowTraceId);

        assertThat(operationLogRepository.findAll().size()).isEqualTo(5);
    }

    @DisplayName("Принудительный выход из флоу на \"Сканирование талона ЭО\", восстановление сессии и проход до конца")
    @Test
    @SneakyThrows
    void forceFlowLeaveAndRestoreOnTicketScanOperationTest() {
        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(FLOW_NAME);

        // Потеряли сессию
        var newFlowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(FLOW_NAME);
        assertThat(flowTraceId).isEqualTo(newFlowTraceId);

        flowOperationControllerCaller.finishTicketScan(
                PROCESS_NAME,
                FLOW_NAME,
                new TicketScanRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json(sortableAcceptContent(), true))
                .andReturn();

        flowOperationControllerCaller.finishAccept(
                PROCESS_NAME,
                FLOW_NAME,
                new FinishAcceptSortableRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}", true))
                .andReturn();

        flowOperationControllerCaller.leaveFlow(flowTraceId)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(operationLogRepository.findAll().size()).isEqualTo(7);
    }

    @DisplayName("Принудительный выход из флоу на \"Сканирование сортабла\", восстановление сессии и проход до конца")
    @Test
    @SneakyThrows
    void forceFlowLeaveAndRestoreOnSortableAcceptSimpleOperationTest() {
        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(FLOW_NAME);

        flowOperationControllerCaller.finishTicketScan(
                PROCESS_NAME,
                FLOW_NAME,
                new TicketScanRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId);

        // Потеряли сессию
        var newFlowTraceId = flowOperationControllerCaller.checkInFlow(FLOW_NAME)
                .andExpect(content().json(sortableAcceptContent(), true))
                .andReturn()
                .getResponse()
                .getHeader(SC_FLOW_TRACE_ID_HEADER);
        assertThat(flowTraceId).isEqualTo(newFlowTraceId);

        var place = testFactory.createForToday(
                TestFactory.order(sortingCenter).externalId("1").places("1-0").build()
        ).getPlace();

        preAccept(flowTraceId, place);

        // Потеряли сессию
        var restoredSortableAcceptContextAfterPreaccept = new ApiNextOperationDto(
                new ApiOperationDto(
                        sortableAcceptSimpleOperation.getSystemName(),
                        sortableAcceptSimpleOperation.getDisplayName(),
                        testFactory.toJsonNode(SORTABLE_ACCEPT_SIMPLE_CONFIG),
                        new SortableAcceptSimpleFlowOperationContextData(
                                List.of(place.getMainPartnerCode()),
                                TICKET_ID,
                                Objects.requireNonNull(place.getWarehouseFrom()).getId(),
                                Objects.requireNonNull(place.getWarehouseFrom()).getIncorporation()
                        )
                )
        );
        newFlowTraceId = flowOperationControllerCaller.checkInFlow(FLOW_NAME)
                        .andExpect(status().isOk())
                        .andExpect(content()
                                .json(toJson(restoredSortableAcceptContextAfterPreaccept), true))
                        .andReturn()
                        .getResponse()
                        .getHeader(SC_FLOW_TRACE_ID_HEADER);
        assertThat(flowTraceId).isEqualTo(newFlowTraceId);

        flowOperationControllerCaller.finishAccept(
                PROCESS_NAME,
                FLOW_NAME,
                new FinishAcceptSortableRequestDto(TICKET_ID),
                flowTraceId
        );

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(content().json("{}", true));

        flowOperationControllerCaller.leaveFlow(flowTraceId)
                .andExpect(status().isOk());

        place = testFactory.getPlace(place.getId());
        assertThat(place.getStageId()).isEqualTo(Stages.FINAL_ACCEPT_DIRECT.getId());

        assertThat(operationLogRepository.findAll().size()).isEqualTo(10);
    }

    @SneakyThrows
    private void preAccept(String flowTraceId, Place place) {
        flowOperationControllerCaller.preAccept(
                        PROCESS_NAME,
                        FLOW_NAME,
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                Objects.requireNonNull(place.getWarehouseFrom()).getId(),
                                null
                        ),
                        flowTraceId
                ).andExpect(status().isOk());
    }

    private String emptyTicketScanOperationContent() {
        return toJson(
                new ApiNextOperationDto(
                        new ApiOperationDto(
                                ticketScanOperation.getSystemName(),
                                ticketScanOperation.getDisplayName(),
                                null,
                                new TicketScanFlowOperationContextData()
                        )
                )
        );
    }

    private String sortableAcceptContent() {
        return toJson(
                new ApiNextOperationDto(
                        new ApiOperationDto(
                                sortableAcceptSimpleOperation.getSystemName(),
                                sortableAcceptSimpleOperation.getDisplayName(),
                                testFactory.toJsonNode(SORTABLE_ACCEPT_SIMPLE_CONFIG),
                                new SortableAcceptSimpleFlowOperationContextData(TICKET_ID)
                        )
                )
        );
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return jacksonObjectMapper.writeValueAsString(obj);
    }
}
