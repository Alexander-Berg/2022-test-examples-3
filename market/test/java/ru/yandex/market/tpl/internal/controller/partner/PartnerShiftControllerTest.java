package ru.yandex.market.tpl.internal.controller.partner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDetailsDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerRoutingListDataDto;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandEvent;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.partner.PartnerShiftService;
import ru.yandex.market.tpl.core.domain.usershift.partner.equeue.PartnerEqueueService;
import ru.yandex.market.tpl.core.domain.usershift.partner.usershift.PartnerUserShiftParamsDtoValidator;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.core.service.usershift.ElectronicQueueService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.report.QrCodeGenerator;
import ru.yandex.market.tpl.internal.service.report.RoutingListReportService;
import ru.yandex.market.tpl.internal.service.report.ShiftReportService;
import ru.yandex.market.tpl.internal.service.report.UserShiftReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@WebLayerTest(PartnerShiftController.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerShiftControllerTest extends BaseShallowTest {

    @SpyBean
    private PartnerkaCommandService commandService;

    @MockBean
    private PartnerkaCommandRepository commandRepository;
    @MockBean
    private PartnerShiftService partnerShiftService;
    @MockBean
    private PartnerUserShiftParamsDtoValidator paramsDtoValidator;
    @MockBean
    private UserShiftReassignManager userShiftReassignManager;
    @MockBean
    private OrderManager orderManager;
    @MockBean
    private UserShiftReportService userShiftReportService;
    @MockBean
    private RoutingListReportService routingListReportService;
    @MockBean
    private ShiftReportService shiftReportService;
    @MockBean
    private QrCodeGenerator qrCodeGenerator;
    @MockBean
    private ElectronicQueueService electronicQueueService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    private PartnerEqueueService partnerEqueueService;

    @BeforeEach
    void init() {
        when(paramsDtoValidator.supports(any())).thenReturn(true);
    }

    @Test
    void shouldPerformUnassignOrders() throws Exception {
        Long userShiftId = 123L;
        long orderId1 = 123L;
        long orderId2 = 456L;
        List<Long> orderIds = List.of(orderId1, orderId2);

        when(commandRepository.save(any())).thenReturn(new PartnerkaCommandEvent());

        mockMvc.perform(patch("/internal/partner/user-shifts/{userShiftId}", userShiftId)
                        .param("orderId", String.valueOf(orderId1))
                        .param("orderId", String.valueOf(orderId2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful());

        verify(userShiftReassignManager).assignOrders(userShiftId, orderIds);
    }

    @Test
    void shouldReturnRoutingListData() throws Exception {
        when(partnerShiftService.findRoutingListData(any(), any())).thenAnswer(invocation ->
                List.of(
                        PartnerRoutingListDataDto.builder()
                                .orders(new ArrayList<>())
                                .courierFullName("Test1 Test1")
                                .courierUid(1L)
                                .userShiftId(1L)
                                .build(),
                        PartnerRoutingListDataDto.builder()
                                .orders(List.of(
                                        PartnerOrderDetailsDto.builder()
                                                .id(1L)
                                                .orderType(PartnerOrderType.LOCKER)
                                                .build()
                                ))
                                .courierFullName("Test2 Test2")
                                .courierUid(2L)
                                .userShiftId(2L)
                                .build())
        );

        String userShiftIds = List.of(1, 2).stream().map(String::valueOf).collect(Collectors.joining(","));

        mockMvc.perform(get("/internal/partner/user-shifts/routing-list-data")
                        .param("userShiftIds", userShiftIds)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_routing_list_data.json")));
    }

    @Test
    void shouldReturnRoutingListDataByUid() throws Exception {
        when(partnerShiftService.findRoutingListData(
                eq(1L), eq(LocalDate.parse("2020-10-27")), eq(List.of(1L, 2L))))
                .thenAnswer(invocation ->
                        List.of(
                                PartnerRoutingListDataDto.builder()
                                        .orders(new ArrayList<>())
                                        .courierFullName("Test1 Test1")
                                        .courierUid(1L)
                                        .userShiftId(1L)
                                        .build(),
                                PartnerRoutingListDataDto.builder()
                                        .orders(List.of(
                                                PartnerOrderDetailsDto.builder()
                                                        .id(1L)
                                                        .orderType(PartnerOrderType.LOCKER)
                                                        .build()
                                        ))
                                        .courierFullName("Test2 Test2")
                                        .courierUid(2L)
                                        .userShiftId(2L)
                                        .build())
                );

        mockMvc.perform(get("/internal/partner/user-shifts/routing-list-data/byUid")
                        .param("sortingCenterId", "1")
                        .param("date", "2020-10-27")
                        .param("uid", "1")
                        .param("uid", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_routing_list_data.json")));
    }


    @Test
    void shouldReturnRoutingListDataByUidV3() throws Exception {
        when(partnerShiftService.findRoutingListDataV3(
                eq("55555"), eq(LocalDate.parse("2020-10-27")), eq(List.of(1L, 2L))))
                .thenAnswer(invocation ->
                        List.of(
                                PartnerRoutingListDataDto.builder()
                                        .orders(new ArrayList<>())
                                        .courierFullName("Test1 Test1")
                                        .courierUid(1L)
                                        .userShiftId(1L)
                                        .build(),
                                PartnerRoutingListDataDto.builder()
                                        .orders(List.of(
                                                PartnerOrderDetailsDto.builder()
                                                        .id(1L)
                                                        .orderType(PartnerOrderType.LOCKER)
                                                        .build()
                                        ))
                                        .courierFullName("Test2 Test2")
                                        .courierUid(2L)
                                        .userShiftId(2L)
                                        .build())
                );

        mockMvc.perform(get("/internal/partner//user-shifts/routing-list-data/v3/byUid")
                        .param("sortingCenterToken", "55555")
                        .param("date", "2020-10-27")
                        .param("uid", "1")
                        .param("uid", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_routing_list_data.json")));
    }

}
