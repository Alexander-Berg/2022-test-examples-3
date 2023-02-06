package ru.yandex.market.delivery.mdbapp.components.queue.startrek;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.queue.startrek.dto.CreateIssueForEventsDto;
import ru.yandex.market.delivery.mdbapp.configuration.StartrekConfiguration;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

public class CreateIssueForEventsQueueConsumerTest {
    public static final List<Long> EVENT_IDS = List.of(100L, 200L, 300L);
    private final StartrekConfiguration startrekConfiguration = Mockito.mock(StartrekConfiguration.class);
    private final StartrekClient startrekClient = Mockito.mock(StartrekClient.class);
    private final OrderEventFailoverableService failoverableService = Mockito.mock(OrderEventFailoverableService.class);
    private final Issues issues = Mockito.mock(Issues.class);

    private final CreateIssueForEventsQueueConsumer consumer = new CreateIssueForEventsQueueConsumer(
        startrekConfiguration,
        startrekClient,
        failoverableService
    );

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        Mockito.reset(startrekConfiguration, startrekClient, failoverableService, issues);
    }

    @Test
    @DisplayName("Создание тикета в стартреке")
    public void createIssue() {
        CreateIssueForEventsDto dto = new CreateIssueForEventsDto(EVENT_IDS, "title", "body");
        Mockito.when(startrekClient.issues(Mockito.any())).thenReturn(issues);
        Mockito.when(issues.create(Mockito.any())).thenAnswer((Answer<Issue>) invocation -> {
            IssueCreate issue = invocation.getArgument(0);
            softly
                .assertThat(issue)
                .extracting(i -> i.getValues().getOptional("summary"))
                .isEqualTo(Optional.of("title"));
            softly
                .assertThat(issue)
                .extracting(i -> i.getValues().getOptional("description"))
                .isEqualTo(Optional.of("body"));
            String id = "MDBFAILEDORDERS-1";
            return new Issue(
                id,
                URI.create("http://st.yandex-team.ru/" + id),
                id,
                "",
                1,
                new EmptyMap<>(),
                null
            );
        });

        consumer.execute(new Task<>(new QueueShardId("1"), dto, 0, ZonedDateTime.now(), null, null));

        Mockito.verify(issues).create(Mockito.any());
        Mockito.verify(failoverableService).setTicketCreated(EVENT_IDS);
        Mockito.verifyNoMoreInteractions(failoverableService, issues);
    }
}
