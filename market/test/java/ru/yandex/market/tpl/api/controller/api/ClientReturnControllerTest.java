package ru.yandex.market.tpl.api.controller.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.order.LocationDetailsDto;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskDto;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@SuppressWarnings("unused")
@WebLayerTest(ClientReturnController.class)
class ClientReturnControllerTest extends BaseShallowTest {

    @MockBean
    private ClientReturnService clientReturnService;
    @MockBean
    private UserShiftCommandService commandService;

    @Test
    void cancelTask() throws Exception {
        long uid = 8347L;
        doReturn(199L).when(queryService).getCurrentShiftId(any());
        var response = taskDto();
        doReturn(response).when(queryService).getTaskInfo(any(), anyLong(), anyLong());
        mockMvc.perform(
                post("/api/tasks/client-return/9999/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "OAuth uid-" + uid)
                        .content("{\n" +
                                "  \"externalReturnId\": \"123\",\n" +
                                "  \"barcodes\": [\n" +
                                "    \"456\"\n" +
                                "  ]\n" +
                                "}")
        )
                .andExpect(status().is2xxSuccessful());

        Mockito.verify(clientReturnService).assignBarcodeAndFinishTask(
                Mockito.eq(List.of("456")),
                Mockito.eq(Map.of()),
                Mockito.eq("123"),
                Mockito.any(),
                Mockito.eq(9999L)
        );
    }

    private CollectDropshipTaskDto taskDto() {
        var taskDto = new CollectDropshipTaskDto();
        taskDto.setId(2562);
        taskDto.setName("Забор дропшипов");
        taskDto.setType(TaskType.COLLECT_DROPSHIP);
        taskDto.setStatus(CollectDropshipTaskStatus.CANCELLED);

        var locationDetailsDto = new LocationDetailsDto();
        locationDetailsDto.setContact("Иван Дропшипов");
        locationDetailsDto.setAddress("г. Москва, Пушкина, д. Колотушкина, кв. 10, этаж 1");
        locationDetailsDto.setPhones(List.of("223322223322"));
        locationDetailsDto.setDescription("Спросить старшего");
        locationDetailsDto.setWorkingHours("09:27 - 10:27");

        taskDto.setLocationDetails(locationDetailsDto);
        return taskDto;
    }

}
