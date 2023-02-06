package ru.yandex.market.tpl.carrier.planner.controller.manual;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierEvent;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.service.api.push.ManualPushApiMapper;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.mj.generated.server.model.CreatePushDto;
import ru.yandex.mj.generated.server.model.CreatePushDtoPayload;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class ManualPushApiControllerTest extends BasePlannerWebTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DbQueueTestUtil dbQueueTestUtil;
    private final ManualPushApiMapper mapper;

    @SneakyThrows
    @Test
    void shouldEnqueuePush() {
        CreatePushDto body = makeCreatePushDto();

        mockMvc.perform(post("/manual/pushes")
                .content(OBJECT_MAPPER.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);


    }

    private CreatePushDto makeCreatePushDto() {
        CreatePushDtoPayload payload = new CreatePushDtoPayload();
        payload.setTitle("Title");
        payload.setBody("Body");
        payload.setDeepLink("deepLink");
        payload.setEvent(CreatePushDtoPayload.EventEnum.DUTY_RUN_CREATED);
        CreatePushDto createPushDto = new CreatePushDto();
        createPushDto.setAppProvider(CreatePushDto.AppProviderEnum.FLUTTER);
        createPushDto.setTaxiId("taxiId");
        createPushDto.setXivaUserId("xivaUserId");
        createPushDto.setPayload(payload);
        createPushDto.setTtlSec(123);
        return createPushDto;
    }

    @Test
    void testMapper() {
        var dto = new CreatePushDto()
                .payload(new CreatePushDtoPayload()
                        .body("body")
                        .title("title")
                        .deepLink("deeplink")
                        .event(CreatePushDtoPayload.EventEnum.DUTY_RUN_CREATED));

        var command = mapper.create(dto);

        Assertions.assertEquals(command.getEvent(), PushCarrierEvent.DUTY_RUN_CREATED);
        Assertions.assertEquals(command.getPayload().getEvent(), PushCarrierEvent.DUTY_RUN_CREATED);
        Assertions.assertEquals(command.getPayload().getDeepLink(), "deeplink");
        Assertions.assertEquals(command.getPayload().getBody(), "body");
        Assertions.assertEquals(command.getPayload().getTitle(), "title");
    }
}
