package ru.yandex.market.sc.api.controller.v2;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.flow.repository.Flow;
import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName.COMMON;
import static ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName.DRIVER_INITIAL_ACCEPTANCE;
import static ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName.FLOW_CHECK_IN;
import static ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName.SORTABLE_ACCEPT_SIMPLE;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.FINISH_SUFFIX;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.PREACCEPT_SUFFIX;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_FLOW_TRACE_ID_HEADER;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_ZONE;

@ScApiControllerTest
public class FlowTraceIdHeaderFilterTest {

    private static final Long UID = 1251L;

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private MockMvc mockMvc;

    private SortingCenter sortingCenter;

    private Zone zone;
    private Flow acceptFlow;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);

        zone = testFactory.storedZone(sortingCenter, "zone-accept", Collections.emptyList());

        var checkInOperation = testFactory.storedOperation(FLOW_CHECK_IN.name(), FLOW_CHECK_IN.name());
        var commonFlow = testFactory.storedFlow(COMMON.name(), COMMON.name(), List.of(checkInOperation));
        testFactory.storedProcess(COMMON.name(), COMMON.name(), List.of(commonFlow));

        var operation = testFactory.storedOperation(SORTABLE_ACCEPT_SIMPLE.name(), SORTABLE_ACCEPT_SIMPLE.name());
        acceptFlow = testFactory.storedFlow(DRIVER_INITIAL_ACCEPTANCE.name(),
                DRIVER_INITIAL_ACCEPTANCE.name(), List.of(operation));
        var process = testFactory.storedProcess(DRIVER_INITIAL_ACCEPTANCE.name(), DRIVER_INITIAL_ACCEPTANCE.name(),
                List.of(acceptFlow));
    }

    @Test
    public void doNotReturnFlowTraceIdWithoutFlowTraceIdInRequestTest() throws Exception {
        var headers = new HttpHeaders();
        headers.set("Authorization", "OAuth uid-" + UID);
        var response = mockMvc.perform(post("/apiV2/any_request").headers(headers)).andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isFalse();
    }

    @Test
    public void returnFlowTraceIdFromRequestTest() throws Exception {
        var headers = new HttpHeaders();
        var flowTraceId = UUID.randomUUID().toString();
        headers.set(SC_FLOW_TRACE_ID_HEADER, flowTraceId);
        headers.set("Authorization", "OAuth uid-" + UID);
        var response = mockMvc.perform(post("/apiV2/any_request").headers(headers)).andReturn();
        assertThat(response.getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER)).isEqualTo(flowTraceId);
    }

    @Test
    public void doNotReturnFlowTraceIdFromRequestToApiV1Test() throws Exception {
        var headers = new HttpHeaders();
        var flowTraceId = UUID.randomUUID().toString();
        headers.set(SC_FLOW_TRACE_ID_HEADER, flowTraceId);
        headers.set("Authorization", "OAuth uid-" + UID);
        var response = mockMvc.perform(post("/apiV1/any_request").headers(headers)).andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isFalse();
    }

    @Test
    public void doNotFilterNonApiV2PrefixedTest() throws Exception {
        var headers = new HttpHeaders();
        headers.set("Authorization", "OAuth uid-" + UID);
        var response = mockMvc.perform(get("/api/sortingCenters/list/").headers(headers))
                .andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isFalse();
    }

    @Test
    public void doNotReturnFlowTraceIdOnZoneCheckInTest() throws Exception {
        var headers = new HttpHeaders();
        headers.set("Authorization", "OAuth uid-" + UID);
        var response = mockMvc.perform(
                post(zoneCheckInUrlTemplate(sortingCenter.getId())).headers(headers)).andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isFalse();
    }

    @Test
    public void returnFlowTraceIdOnFlowCheckInTest() throws Exception {
        var headers = new HttpHeaders();
        headers.set("Authorization", "OAuth uid-" + UID);
        headers.set(SC_ZONE, "1");
        var flowCheckInUrlTemplate = flowCheckInUrlTemplate(sortingCenter.getId(), "any");
        var response = mockMvc.perform(
                post(flowCheckInUrlTemplate).headers(headers)).andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isTrue();
    }

    @Test
    public void returnNewFlowTraceIdOnFlowCheckInTest() throws Exception {
        var headers = new HttpHeaders();
        headers.set("Authorization", "OAuth uid-" + UID);
        headers.set(SC_ZONE, "1");
        var flowTraceId = UUID.randomUUID().toString();
        headers.set(SC_FLOW_TRACE_ID_HEADER, flowTraceId);
        var flowCheckInUrlTemplate = flowCheckInUrlTemplate(sortingCenter.getId(), "any");
        var response = mockMvc.perform(
                post(flowCheckInUrlTemplate).headers(headers)).andReturn();
        assertThat(response.getResponse().containsHeader(SC_FLOW_TRACE_ID_HEADER)).isTrue();
        assertThat(response.getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER)).isNotEqualTo(flowTraceId);
    }

    @Nested
    class FlowTraceIdUnFinishedFlow {

        @Test
        @SneakyThrows
        @DisplayName("Получить traceId не завершенной операции в зоне")
        void flowTraceIdUnfinishedFlow() {
            var order = testFactory.createForToday(order(sortingCenter, "o1")
                            .places("o1-1", "o1-2")
                            .build())
                    .get();

            var headers = new HttpHeaders();
            headers.set("Authorization", "OAuth uid-" + UID);
            headers.set(SC_ZONE, String.valueOf(zone.getId()));

            var response = mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() +
                                    "/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/" + acceptFlow.getSystemName())
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, UUID.randomUUID().toString())
                                    .headers(headers)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();


            response = mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() + "/process/INITIAL_ACCEPTANCE/flow" +
                                    "/DRIVER_INITIAL_ACCEPTANCE/operation" +
                                    "/SORTABLE_ACCEPT_SIMPLE" +
                                    "/" + PREACCEPT_SUFFIX)
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JacksonUtil.toString(new AcceptSortableRequestDto(List.of("o1-1"),
                                            order.getWarehouseFrom().getId(), null)))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() + "/process/INITIAL_ACCEPTANCE/flow" +
                                    "/DRIVER_INITIAL_ACCEPTANCE/operation" +
                                    "/SORTABLE_ACCEPT_SIMPLE" +
                                    "/" + PREACCEPT_SUFFIX)
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JacksonUtil.toString(new AcceptSortableRequestDto(List.of("o1-2"),
                                            order.getWarehouseFrom().getId(), null)))
                    )
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() +
                                    "/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/" + acceptFlow.getSystemName())
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string(SC_FLOW_TRACE_ID_HEADER,
                            Objects.requireNonNull(response.getHeader(SC_FLOW_TRACE_ID_HEADER))));
        }

        @Test
        @SneakyThrows
        @DisplayName("Получение нового traceId после завершенной операции в зоне")
        void newFlowTraceIdWhenExistFinishedFlow() {
            var order = testFactory.createForToday(order(sortingCenter, "o1")
                            .places("o1-1", "o1-2")
                            .build())
                    .get();

            var headers = new HttpHeaders();
            headers.set("Authorization", "OAuth uid-" + UID);
            headers.set(SC_ZONE, String.valueOf(zone.getId()));

            var response = mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() +
                                    "/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/" + acceptFlow.getSystemName())
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, UUID.randomUUID().toString())
                                    .headers(headers)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            response = mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() + "/process/INITIAL_ACCEPTANCE/flow" +
                                    "/DRIVER_INITIAL_ACCEPTANCE/operation" +
                                    "/SORTABLE_ACCEPT_SIMPLE" +
                                    "/" + PREACCEPT_SUFFIX)
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JacksonUtil.toString(new AcceptSortableRequestDto(List.of("o1-1"),
                                            order.getWarehouseFrom().getId(), null)))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() + "/process/INITIAL_ACCEPTANCE/flow" +
                                    "/DRIVER_INITIAL_ACCEPTANCE/operation" +
                                    "/SORTABLE_ACCEPT_SIMPLE" +
                                    "/" + FINISH_SUFFIX)
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            var checkInResponse = mockMvc.perform(
                            post("/apiV2/sc/" + sortingCenter.getId() +
                                    "/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/" + acceptFlow.getSystemName())
                                    .header("Authorization", "OAuth uid-" + UID)
                                    .header(SC_ZONE, zone.getId())
                                    .header(SC_FLOW_TRACE_ID_HEADER, response.getHeader(SC_FLOW_TRACE_ID_HEADER))
                                    .headers(headers)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn().getResponse();
            assertThat(checkInResponse.getHeader(SC_FLOW_TRACE_ID_HEADER))
                    .isNotEqualTo(response.getHeader(SC_FLOW_TRACE_ID_HEADER));
        }
    }

    private static String zoneCheckInUrlTemplate(Long scId) {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/ZONE_CHECK_IN", scId);
    }

    private static String flowCheckInUrlTemplate(Long scId, String flowSystemName) {
        return String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation/FLOW_CHECK_IN/%s", scId,
                flowSystemName);
    }

}
