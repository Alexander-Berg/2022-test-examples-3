package ru.yandex.calendar.frontend.worker.task;

import java.util.List;
import java.util.Optional;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.frontend.ews.exp.ResourceParticipantBriefInfo;
import ru.yandex.calendar.frontend.ews.exp.ResourceParticipantChangesInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.commune.bazinga.BazingaBender;

import static org.assertj.core.api.Assertions.assertThat;

public class EwsExportEventResourceChangesOnUpdateTaskTest {
    @Test
    public void parametersSerialization() {
        val resourceParticipants = new ResourceParticipantChangesInfo(
                List.of(resourceParticipantShortInfo(1)),
                List.of(resourceParticipantShortInfo(2)),
                List.of(resourceParticipantShortInfo(3), resourceParticipantShortInfo(4)));
        val expectedJson = "{" +
                "\"eventId\":42," +
                    "\"resourceParticipantChangesInfo\":{" +
                        "\"newResources\":[{\"exchangeEmail\":\"1@exchange\",\"resourceEmail\":\"1@yandex-team.ru\",\"resourceId\":1," +
                                "\"eventId\":1,\"asyncWithExchange\":true,\"yandexTeam\":true,\"syncWithExchange\":true}]," +
                        "\"removedResources\":[{\"exchangeEmail\":\"2@exchange\",\"resourceEmail\":\"2@yandex-team.ru\",\"resourceId\":2," +
                                "\"eventId\":2,\"asyncWithExchange\":true,\"yandexTeam\":true,\"syncWithExchange\":true}]," +
                        "\"notChangedResources\":[{\"exchangeEmail\":\"3@exchange\",\"resourceEmail\":\"3@yandex-team.ru\",\"resourceId\":3," +
                               "\"eventId\":3,\"asyncWithExchange\":true,\"yandexTeam\":true,\"syncWithExchange\":true}," +
                            "{\"exchangeEmail\":\"4@exchange\",\"resourceEmail\":\"4@yandex-team.ru\",\"resourceId\":4," +
                               "\"eventId\":4,\"asyncWithExchange\":true,\"yandexTeam\":true,\"syncWithExchange\":true}]}," +
                    "\"source\":\"web-maya\"," +
                    "\"rid\":\"rid\"" +
                "}";
        val parameters = new EwsExportEventResourceChangesOnUpdateTask
                .ExportToExchangeTaskParameters(42, resourceParticipants, Optional.empty(), ActionSource.WEB_MAYA, "rid");
        val mapper = BazingaBender.mapper;

        val json = new String(mapper.serializeJson(parameters));
        assertThat(json).isEqualTo(expectedJson);

        val parametersFromJson = mapper.parseJson(EwsExportEventResourceChangesOnUpdateTask.ExportToExchangeTaskParameters.class, json);
        assertThat(parametersFromJson).isEqualTo(parameters);
    }

    private ResourceParticipantBriefInfo resourceParticipantShortInfo(long id) {
        return new ResourceParticipantBriefInfo(id + "@exchange", id + "@yandex-team.ru", id, id, true, Optional.empty(), true, true);
    }
}
