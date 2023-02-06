package ru.yandex.market.pers.tms.yt.feedback;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.service.common.util.PersUtils;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;
import ru.yandex.market.pers.tms.yt.feedback.TicketFromFeedbackCreatorExecutor.TicketData;
import ru.yandex.market.util.ListUtils;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.QueueRef;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.yt.feedback.TicketFromFeedbackCreatorExecutor.DAY_IN_MS;
import static ru.yandex.market.pers.tms.yt.feedback.TicketFromFeedbackCreatorExecutor.LAST_EXECUTED_KEY;

public class TicketFromFeedbackCreatorExecutorTest extends MockedPersTmsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Long UPDATED_AT = TicketFromFeedbackCreatorExecutor.DEF_LAST_EXECUTED + 10101L;

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private TicketFromFeedbackCreatorExecutor executor;
    @Autowired
    private YtExportHelper ytExportHelper;
    @Autowired
    private StartrekService startrekService;
    @Autowired
    private ComplexMonitoring complexMonitoring;

    @Test
    public void testTicketsFromFeedbackCreation() throws Exception {
        long currentTime = System.currentTimeMillis();
        long updatedAt = currentTime + 10101L;
        List<String> ticketNames = List.of("TICKET_KEY1", "TICKET_KEY2", "TICKET_KEY3");
        configurationService.mergeValue(LAST_EXECUTED_KEY, currentTime);
        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(
            new Issue("1", null, ticketNames.get(0), null, 1, new EmptyMap<>(), null),
            new Issue("2", null, ticketNames.get(1), null, 1, new EmptyMap<>(), null),
            new Issue("3", null, ticketNames.get(2), null, 1, new EmptyMap<>(), null)
        );

        int cases = 3;
        List<TicketData> ticketDatas = IntStream.range(0, cases)
            .mapToObj(x -> {
                TicketData result = buildBasicFeedback();
                result.orderId = result.orderId + x;
                result.comment = result.comment + " zakaz " + result.orderId;
                result.crTime = Timestamp.from(Instant.ofEpochMilli(updatedAt).plus(x, ChronoUnit.MILLIS));
                return result;
            })
            .collect(Collectors.toList());

        mockMainQuery(ticketDatas);

        executor.runTmsJob();

        assertEquals((Long)(updatedAt + 2L), configurationService.getValueAsLong(LAST_EXECUTED_KEY));
        assertEquals(ticketNames, pgJdbcTemplate.queryForList("SELECT ticket_key from created_ticket_log order by CR_TIME", String.class));
    }

    @Test
    public void testTicketsFromFeedbackFireMonitoring() throws Exception {
        mockMainQuery(List.of());

        configurationService.mergeValue(LAST_EXECUTED_KEY, System.currentTimeMillis() - 2 * DAY_IN_MS);
        executor.runTmsJob();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());

        configurationService.mergeValue(LAST_EXECUTED_KEY, System.currentTimeMillis() - 4 * DAY_IN_MS);
        try {
            executor.runTmsJob();
            Assert.fail();
        } catch (Exception ex) {
            assertEquals(
                "TicketFromFeedbackCreatorExecutor: Long time no feedback tickets",
                ex.getMessage()
            );
        }
    }

    @Test
    public void testTicketsFromFeedbackComment() throws Exception {
        long currentTime = System.currentTimeMillis();
        long updatedAt = currentTime + 10101L;
        List<String> ticketNames = List.of("TICKET_KEY1", "TICKET_KEY2");
        configurationService.mergeValue(LAST_EXECUTED_KEY, currentTime);
        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(
            new Issue("1", null, ticketNames.get(0), null, 1, new EmptyMap<>(), null),
            new Issue("2", null, ticketNames.get(1), null, 1, new EmptyMap<>(), null)
        );

        when(startrekService.tryGetTicket("TICKET_KEY1")).then(invocation -> {
            Issue issue = mockIssueBasic("Номер заказа");
            return Optional.of(issue);
        });

        int cases = 3;
        List<TicketData> ticketDatas = IntStream.range(0, cases)
            .mapToObj(x -> {
                TicketData result = buildBasicFeedback();
                if (x == 1) {
                    result.orderId = result.orderId + x;
                }
                result.comment = result.comment + " zakaz " + result.orderId;
                result.crTime = Timestamp.from(Instant.ofEpochMilli(updatedAt).plus(x, ChronoUnit.MILLIS));
                return result;
            })
            .collect(Collectors.toList());

        mockMainQuery(ticketDatas);

        executor.runTmsJob();

        verify(startrekService, times(1)).createComment(any(), eq(getCommentText()));
        assertEquals((Long)(updatedAt + 2L), configurationService.getValueAsLong(LAST_EXECUTED_KEY));
        assertEquals(ticketNames, pgJdbcTemplate.queryForList("SELECT ticket_key from created_ticket_log order by CR_TIME", String.class));
    }

    private String getCommentText() {
        return "Номер заказа: 777\n" +
            "\n" +
            "Причина: Товар был в употреблении\n" +
            "Наименование товара: Карамель Тульский леденец на палочке Петушок из Тулы\n" +
            "\n" +
            "Причина: Товар оказался подделкой\n" +
            "Наименование товара: Карамель Тульский леденец на палочке Петушок из Тулы\n" +
            "\n" +
            "Комментарий пользователя: Лизали до меня zakaz 777";
    }

    private TicketData buildBasicFeedback() {
        TicketData result = new TicketData();
        result.orderId = 777;
        result.crTime = Timestamp.from(Instant.ofEpochMilli(UPDATED_AT));
        result.comment = "Лизали до меня";
        result.questionWithSkuName = Map.of(
            "Товар был в употреблении",
            "Карамель Тульский леденец на палочке Петушок из Тулы",
            "Товар оказался подделкой",
            "Карамель Тульский леденец на палочке Петушок из Тулы");
        return result;
    }

    private void mockMainQuery(TicketData ticketDatas) {
        mockMainQuery(List.of(ticketDatas));
    }

    private void mockMainQuery(List<TicketData> ticketDatas) {
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == null) {
                return null;
            }
            Function<JsonNode, TicketData> parser = invocation.getArgument(2);
            Consumer<List<TicketData>> consumer = invocation.getArgument(3);

            consumer.accept(ListUtils.toList(prepareFeedbackNode(ticketDatas), parser));
            return null;
        }).when(ytExportHelper.getHahnYtClient()).consumeTableBatched(
            ArgumentMatchers.argThat(argument -> argument.toString().contains("feedback_to_ticket")),
            anyInt(),
            any(Function.class),
            any(Consumer.class)
        );
    }

    private List<JsonNode> prepareFeedbackNode(List<TicketData> ticketDatas) {
        return toNodes(ListUtils.toList(ticketDatas, this::toJsonMap));
    }

    private <T> List<JsonNode> toNodes(List<T> items) {
        try {
            String json = mapper.writeValueAsString(items);
            JsonNode node = mapper.readTree(json);
            List<JsonNode> result = new ArrayList<>();
            node.iterator().forEachRemaining(result::add);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> toJsonMap(TicketData data) {
        List<Map<String, Object>> questionMapToJson = data.getQuestionWithSkuName() == null ? null :
            data.getQuestionWithSkuName().entrySet().stream()
                .map(x -> PersUtils.buildMap("question_title", x.getKey(), "sku_name", x.getValue()))
                .collect(Collectors.toList());

        return PersUtils.buildMap(
            "comment", data.comment,
            "cr_time_ms", data.crTime.getTime(),
            "order_id", data.orderId,
            "questions", questionMapToJson
        );
    }

    @NotNull
    private Issue mockIssueBasic(String text) {
        Issue issue = mock(Issue.class);
        when(issue.getSummary()).thenReturn(text);
        when(issue.getVotedBy()).thenReturn(new ArrayListF<>());
        when(issue.getResolution()).thenReturn(Option.empty());
        QueueRef queue = queueRef("TESTMOCKQUALITY");
        when(issue.getQueue()).thenReturn(queue);
        return issue;
    }

    private QueueRef queueRef(String name) {
        QueueRef res = mock(QueueRef.class);
        when(res.getKey()).thenReturn(name);
        return res;
    }
}
