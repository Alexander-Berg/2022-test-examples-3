package ru.yandex.market.tpl.api.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.facade.LockerDeliveryTaskFacade;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(LockerDeliveryTaskController.class)
class LockerDeliveryTaskControllerTest extends BaseShallowTest {

    public static final long UID = 8347L;

    @MockBean
    private UserPropertyService userPropertyService;
    @MockBean
    private LockerDeliveryTaskFacade lockerDeliveryTaskFacade;


    @Test
    void finishLoad() throws Exception {
        long routePointId = 199;
        long taskId = 4765;
        mockMvc.perform(
                        post("/api/route-points/" + routePointId + "/tasks/locker-delivery/" + taskId + "/finish-load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "OAuth uid-" + LockerDeliveryTaskControllerTest.UID)
                                .content("{\n" +
                                        "  \"completedOrders\": [\n" +
                                        "    {\n" +
                                        "      \"externalOrderId\": \"83475\"\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "      \"externalOrderId\": \"92342\"\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"comment\": \"Не вышло\"\n" +
                                        "}")
                )
                .andExpect(status().is2xxSuccessful());

        verify(lockerDeliveryTaskFacade).finishLoadingLocker(eq(routePointId), eq(taskId), any(), any());
    }

    @Test
    void finishUnload() throws Exception {
        long routePointId = 199;
        long taskId = 4765;
        mockMvc.perform(
                        post("/api/route-points/" + routePointId + "/tasks/locker-delivery/" + taskId + "/finish" +
                                "-unload")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "OAuth uid-" + UID)
                                .content("{\n" +
                                        "  \"completedOrders\": [\n" +
                                        "    {\n" +
                                        "      \"externalOrderId\": \"123\"\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "      \"externalOrderId\": \"456\"\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}")
                )
                .andExpect(status().is2xxSuccessful());

        verify(lockerDeliveryTaskFacade).finishUnloadingLocker(eq(routePointId), eq(taskId), any(), any());
    }

}
