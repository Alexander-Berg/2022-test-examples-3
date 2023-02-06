package ru.yandex.market.sc.api;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.FinishAcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.TicketScanRequestDto;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_FLOW_TRACE_ID_HEADER;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_ZONE;

public class FlowOperationTestControllerCaller {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private final Long scId;
    private final Long uid;
    private final Long zoneId;

    public FlowOperationTestControllerCaller(MockMvc mockMvc, long uid, long zoneId, long scId) {
        this.mockMvc = mockMvc;
        this.scId = scId;
        this.uid = uid;
        this.zoneId = zoneId;
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows
    private <T> T executeAsync(Callable<T> callable) {
        return Executors.newSingleThreadExecutor().submit(callable).get();
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#checkInFlow}
     */
    @SneakyThrows
    public ResultActions checkInFlow(String flowSystemName) {
        return checkInFlow(flowSystemName, this.zoneId);
    }

    @SneakyThrows
    public String checkInFlowAndGetFlowTraceId(String flowSystemName) {
        return checkInFlow(flowSystemName, this.zoneId)
                .andReturn()
                .getResponse()
                .getHeader(SC_FLOW_TRACE_ID_HEADER);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#checkInFlow}
     */
    @SneakyThrows
    public ResultActions checkInFlow(String flowSystemName, long zoneId) {
        return executeAsync(
                () -> mockMvc.perform(
                        MockMvcRequestBuilders.post(flowCheckInUrl(flowSystemName))
                                .header(SC_ZONE, String.valueOf(zoneId))
                                .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
                ).andDo(print())
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#leaveFlow}
     */
    @SneakyThrows
    public ResultActions leaveFlow(String flowTraceId) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(flowLeaveUrl())
                        .header(SC_ZONE, String.valueOf(zoneId))
                        .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
                        .header(SC_FLOW_TRACE_ID_HEADER, flowTraceId)
        ).andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#checkInZone}
     */
    @SneakyThrows
    public ResultActions checkInZone() {
        return checkInZone(this.zoneId);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#checkInZone}
     */
    @SneakyThrows
    public ResultActions checkInZone(Long zoneId) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(zoneCheckInUrl())
                        .header(SC_ZONE, String.valueOf(zoneId))
                        .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
        ).andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#leaveZone}
     */
    @SneakyThrows
    public ResultActions leaveZone(Long zoneId) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(zoneLeaveUrl())
                        .header(SC_ZONE, String.valueOf(zoneId))
                        .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
        ).andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.CommonOperationsController#getNextOperation}
     */
    @SneakyThrows
    public ResultActions nextOperation(String flowTraceId) {
        return mockMvc.perform(
                        MockMvcRequestBuilders.get(nextOperationUrl())
                                .header(SC_ZONE, String.valueOf(zoneId))
                                .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
                                .header(SC_FLOW_TRACE_ID_HEADER, flowTraceId)
                ).andDo(print());
    }

    @SneakyThrows
    public ResultActions doPost(String url, Object content, String flowTraceId) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(url)
                        .header(SC_ZONE, String.valueOf(zoneId))
                        .header(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid)
                        .header(SC_FLOW_TRACE_ID_HEADER, flowTraceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(content))
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.SortableAcceptSimpleController#sortablePreAccept}
     */
    public ResultActions preAccept(String processName,
                                   String flowName,
                                   AcceptSortableRequestDto content,
                                   String flowTraceId) {
        return doPost(preAcceptUrl(processName, flowName), content, flowTraceId);
    }


    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.SortableAcceptSimpleController#finishAccept}
     */
    public ResultActions finishAccept(String processName,
                                      String flowName,
                                      FinishAcceptSortableRequestDto content,
                                      String flowTraceId) {
        return doPost(finishAcceptUrl(processName, flowName), content, flowTraceId);
    }


    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.SortableAcceptSimpleController#sortableAcceptRevert}
     */
    public ResultActions revertAccept(String processName,
                                      String flowName,
                                      String flowTraceId) {
        return doPost(revertAcceptUrl(processName, flowName), null, flowTraceId);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.v2.TicketScanController#finishTicketScan}
     */
    public ResultActions finishTicketScan(String processName,
                                          String flowName,
                                          TicketScanRequestDto content,
                                          String flowTraceId) {
        return doPost(scanTicketUrl(processName, flowName), content, flowTraceId);
    }


    private String flowCheckInUrl(String flowSystemName) {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/%s", scId,
                flowSystemName);
    }

    private String flowLeaveUrl() {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/FLOW_LEAVE", scId);
    }

    private String zoneCheckInUrl() {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/ZONE_CHECK_IN", scId);
    }

    private String zoneLeaveUrl() {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/ZONE_LEAVE", scId);
    }

    private String nextOperationUrl() {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/NEXT_OPERATION", scId);
    }

    private String preAcceptUrl(String processName, String flowName) {
        return String.format("/apiV2/sc/%s/process/%s/flow/%s/operation/SORTABLE_ACCEPT_SIMPLE/preaccept", scId,
                processName, flowName);
    }

    private String finishAcceptUrl(String processName, String flowName) {
        return String.format("/apiV2/sc/%s/process/%s/flow/%s/operation/SORTABLE_ACCEPT_SIMPLE/finish", scId,
                processName, flowName);
    }

    private String revertAcceptUrl(String processName, String flowName) {
        return String.format("/apiV2/sc/%s/process/%s/flow/%s/operation/SORTABLE_ACCEPT_SIMPLE/revert", scId,
                processName, flowName);
    }

    private String scanTicketUrl(String processName, String flowName) {
        return String.format("/apiV2/sc/%s/process/%s/flow/%s/operation/TICKET_SCAN/finish", scId, processName, flowName);
    }
}
