package ru.yandex.market.tpl.api.controller.api;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.notification.PushRedirect;
import ru.yandex.market.tpl.api.model.notification.PushRedirectType;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.push.subscription.PushSubscriptionServiceImpl;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.query.usershift.mapper.UserShiftDtoMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@WebLayerTest(PushController.class)
class PushControllerTest extends BaseShallowTest {

    @MockBean
    private PushNotificationRepository pushNotificationRepository;

    @MockBean(name = "pushSubscriptionServiceImpl")
    private PushSubscriptionServiceImpl pushSubscriptionServiceImpl;

    private UserShiftDtoMapper userShiftDtoMapper = new UserShiftDtoMapper();

    @Test
    void messages() throws Exception {
        long uid = 12312L;
        long userShiftId = 984753L;
        doReturn(userShiftDto(userShiftId)).when(queryService)
                .getCurrentShiftDto(any());
        doReturn(List.of(pushNotification(uid, userShiftId))).when(pushNotificationRepository)
                .findByXivaUserIdAndUserShiftIdOrderByIdDesc(any(), anyLong());
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "OAuth uid-" + uid)
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json("" +
                        "{\n" +
                        "  \"messages\": [\n" +
                        "    {\n" +
                        "      \"id\": 0,\n" +
                        "      \"text\": \"my_push\",\n" +
                        "      \"redirect\": {\n" +
                        "        \"type\": \"ROUTE_POINT_PAGE\",\n" +
                        "        \"routePointId\": 1,\n" +
                        "        \"taskId\": 1\n" +
                        "      },\n" +
                        "      \"time\": \"1970-01-01T00:00:00Z\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
                ));
    }

    @Test
    void noShift() throws Exception {
        doReturn(userShiftDtoMapper.noShiftDto()).when(queryService).getCurrentShiftDto(any());
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "OAuth uid-12312")
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json("" +
                        "{\n" +
                        "  \"messages\": []\n" +
                        "}"
                ));
    }

    private UserShiftDto userShiftDto(long userShiftId) {
        return UserShiftDto.builder()
                .id(userShiftId)
                .status(UserShiftStatus.ON_TASK)
                .startDate(Instant.ofEpochMilli(0))
                .endDate(Instant.ofEpochMilli(0))
                .currentRoutePointId(1L)
                .active(true)
                .demoEnabled(false)
                .build();
    }

    private PushNotification pushNotification(long uid, long userShiftId) {
        var notification = new PushNotification(
                String.valueOf(uid),
                PushEvent.SYSTEM,
                null,
                "my_push",
                1,
                new PushNotificationPayload(new PushRedirect(PushRedirectType.ROUTE_POINT_PAGE, 1L, 1L, null)),
                null, null, null, userShiftId, null, null
        );
        notification.setId(0L);
        notification.setCreatedAt(Instant.ofEpochMilli(0));
        return notification;
    }

}
