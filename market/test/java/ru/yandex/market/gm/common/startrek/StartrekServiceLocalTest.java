package ru.yandex.market.gm.common.startrek;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import ru.yandex.market.gm.common.startrek.ticket.StartrekTicket;
import ru.yandex.market.gm.common.startrek.ticket.StartrekTicketData;
import ru.yandex.market.gm.common.startrek.ticket.TicketField;
import ru.yandex.market.gm.common.startrek.ticket.TicketLinkType;
import ru.yandex.market.gm.common.startrek.ticket.TicketQueueType;
import ru.yandex.market.gm.common.startrek.util.StartekUtil;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;

import static com.google.common.hash.Hashing.sha256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gm.common.startrek.QuickStartWorkflowResolution.RESOLVED;
import static ru.yandex.market.gm.common.startrek.ticket.TicketField.ORDER_ID;
import static ru.yandex.market.gm.common.startrek.ticket.TicketQueueType.TESTPARENT;

@Slf4j
@Disabled("Only for local testing")
class StartrekServiceLocalTest {

    private StartrekService startrekService;

    @BeforeEach
    void prepare() {
        ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
        when(beanFactory.resolveEmbeddedValue(anyString())).thenAnswer(v -> v.getArgument(0));

        String oAuthToken = System.getenv("STARTREK_TOKEN");
        QueueResolver queueResolver = new QueueResolver(beanFactory);

        Session trackerSession = StartekUtil.createSession(
                "https://st-api.yandex-team.ru",
                oAuthToken, 10, 10, null
        );

        startrekService = new StartrekService(trackerSession, queueResolver);
    }

    @Test
    void testCreateAll() {
        StartrekTicket parent = startrekService.createTicket(
                TESTPARENT, "Test parent", "ticket body" + Instant.now(), System.nanoTime() + "");

        StartrekTicket child = startrekService.createTicket(
                TicketQueueType.TESTCHILD, "Test child", "child body" + Instant.now(), System.nanoTime() + "");

        parent.comment("Ticket created");
        parent.setField(TicketField.AGREEMENT_NUMBER, "123");
        parent.link(TicketLinkType.IS_PARENT_TASK_FOR, child);
        parent.doUpdate();

        assertThat(parent.getQueue()).isEqualTo(TESTPARENT);
        assertThat(parent.getLinkedSingle(TicketLinkType.IS_PARENT_TASK_FOR, TicketQueueType.TESTCHILD)).isNotNull();
        assertThat(parent.getStatus(QuickStartWorkflowStatus.class)).isEqualTo(QuickStartWorkflowStatus.OPENED);

        assertThat(parent.isFieldSet(TicketField.AGREEMENT_NUMBER)).isTrue();
        assertThat(parent.<String>getField(TicketField.AGREEMENT_NUMBER)).isEqualTo("123");

        assertThat(parent.isFieldSet(TicketField.REQUEST_REFUSAL_REASON)).isFalse();
        assertThat(parent.<String>getField(TicketField.REQUEST_REFUSAL_REASON)).isNull();

        assertThat(child.getQueue()).isEqualTo(TicketQueueType.TESTCHILD);
    }

    @Test
    void testGetField() {
        var parent = startrekService.getTicket("TSTPARENT-9");
        assertThat(parent.<String>getField(TicketField.AGREEMENT_NUMBER)).isEqualTo("123");
    }

    @Test
    void createTicketWithAllTicketLinkTypes() {
        StartrekTicket parent = startrekService.createTicket(
                TESTPARENT, "Parent w/all link types", "All link types here" + Instant.now(),
                System.nanoTime() + "");

        for (TicketLinkType rel : TicketLinkType.values()) {
            StartrekTicket child = startrekService.createTicket(
                    TicketQueueType.TESTCHILD, "Child " + rel.name(), "" + Instant.now(), System.nanoTime() + "");
            parent.link(rel, child);
        }

        parent.doUpdate();

        for (TicketLinkType rel : TicketLinkType.values()) {
            assertThat(parent.getLinked(rel, TicketQueueType.TESTCHILD)).hasSize(1);
            assertThat(parent.getLinked(rel)).hasSize(1);
        }
    }

    @Test
    void testTransit() {
        StartrekTicket parent = startrekService.createTicket(
                TESTPARENT, "Test parent", "ticket body" + Instant.now(), System.nanoTime() + "");

        parent.doTransitWithUpdate(QuickStartWorkflowStatus.IN_PROGRESS);
        parent.doTransitWithUpdate(QuickStartWorkflowStatus.NEED_INFO);
        parent.doTransitWithUpdate(QuickStartWorkflowStatus.OPENED);
        assertThat(parent.isOpened()).isTrue();

        parent.setResolution(RESOLVED);
        parent.doTransitWithUpdate(QuickStartWorkflowStatus.CLOSED);

        assertThat(parent.getStatus(QuickStartWorkflowStatus.class)).isEqualTo(QuickStartWorkflowStatus.CLOSED);
        assertThat(parent.getResolution(QuickStartWorkflowResolution.class)).isPresent();
        //noinspection OptionalGetWithoutIsPresent
        assertThat(parent.getResolution(QuickStartWorkflowResolution.class).get()).isEqualTo(RESOLVED);
        assertThat(parent.isOpened()).isFalse();
    }

    @Test
    void testNotPostingTheSameTicketTwice() {
        String uniqueKey = "unique";

        StartrekTicket parentFirstAttempt = startrekService.createTicket(
                TESTPARENT, "Test parent", "ticket body" + Instant.now(), uniqueKey);

        StartrekTicket parentSecondAttempt = startrekService.createTicket(
                TESTPARENT, "Test parent", "ticket body" + Instant.now(), uniqueKey);

        assertThat(parentFirstAttempt.getKey()).isEqualTo(parentSecondAttempt.getKey());
    }

    @Test
    void testCheckList() {
        startrekService.createTicket(
                new StartrekTicketData(TESTPARENT, "Test parent", "ticket body", System.nanoTime() + "")
                        .setChecklistItems(List.of("hello", "world", "from", "checklist"))
        );
    }

    @Test
    void testComponent() {
        startrekService.createTicket(
                new StartrekTicketData(TESTPARENT, "Test parent", "ticket body", System.nanoTime() + "")
                        .setComponentIds(List.of(90457L))
        );
    }

    @Test
    void testFields() {
        //noinspection UnstableApiUsage
        StartrekTicket ticket = startrekService.createTicket(IssueCreate.builder()
                .queue("GLOBALMARKETSUP")
                .summary("Title")
                .description("Body")
                .unique(sha256().hashString(System.nanoTime() + "", StandardCharsets.UTF_8).toString())
                .set(ORDER_ID.getFieldKey(), 123)
                .type(53)
                .build()
        );

        System.out.println(ticket.getKey());
    }

    @Test
    void testCreateWithFields() {
        startrekService.createTicket(
                new StartrekTicketData(TESTPARENT, "Test parent", "ticket body", System.nanoTime() + "")
                        .setField(TicketField.INN, "12345")
                        .setField(TicketField.OGRN, "67890")
        );
    }

}
