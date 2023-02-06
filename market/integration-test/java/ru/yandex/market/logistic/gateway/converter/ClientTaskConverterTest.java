package ru.yandex.market.logistic.gateway.converter;

import java.time.LocalDateTime;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.support.ClientTaskDetailDto;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.service.converter.ClientTaskConverter;
import ru.yandex.market.logistic.gateway.service.converter.util.ExternalReferenceObjectBuilder;
import ru.yandex.market.logistic.gateway.utils.delivery.DtoFactory;
import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;

import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_CREATE_ORDER;

public class ClientTaskConverterTest extends BaseTest {

    @Test
    public void convertToDetailDto() {
        ClientTask clientTask = DtoFactory.createClientTask(1L, DS_CREATE_ORDER, "{ }");

        ClientTaskDetailDto clientTaskDetailDto = new ClientTaskDetailDto()
            .setId(1L)
            .setTitle("Задача")
            .setTaskId(1L)
            .setParentId(0L)
            .setRootId(0L)
            .setRequestFlow(DS_CREATE_ORDER.getFlow())
            .setMessage(FormattedTextObject.of("{ }"))
            .setStatus(TaskStatus.IN_PROGRESS.name())
            .setCountRetry(0)
            .setDelaySeconds(0)
            .setSearchQueryReference(new ExternalReferenceObjectBuilder(
                "Поиск запросов по ID задачи",
                1L,
                LocalDateTime.now(),
                true
            ).build());

        assertions.assertThat(clientTaskDetailDto)
            .as("Dto should be equal to converted entity")
            .isEqualTo(ClientTaskConverter.convertToDetailDto(clientTask));
    }

}
