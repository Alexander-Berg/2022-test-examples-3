package ru.yandex.market.pers.qa.tms.startrek;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.exception.QaRuntimeException;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;


/**
 * @author grigor-vlad
 * 12.04.2022
 */
public class CreateCtmModelQuestionTicketExecutorTest extends PersQaTmsTest {
    private static final String QUESTION_TEXT = "Это хороший текст для вопроса?";
    private static final Long MODEL_ID = 100L;
    private static final Long VENDOR_ID = 100L;
    private static final String TICKET_QUEUE = "QASTMTEST";
    private static final String TICKET_KEY = TICKET_QUEUE + "-1";

    @Autowired
    private StartrekService startrekService;

    @Autowired
    private PersNotifyClient persNotifyClient;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    @Qualifier("createCtmModelQuestionTicketExecutor")
    private CreateCtmModelQuestionTicketExecutor executor;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void initTestEnvironment() {
        //add model to database with ticket_models
        pgJdbcTemplate.update(
            "insert into qa.ext_models_for_tickets (model_id, vendor_id) values (?, ?) on conflict do nothing",
            MODEL_ID, VENDOR_ID
        );
        pgJdbcTemplate.update(
            "insert into qa.vendor_ticket_mapping (vendor_id, ticket_queue) values (?, ?) on conflict do nothing",
            VENDOR_ID, TICKET_QUEUE
        );
        executor.setLastQuestionId(0L);
    }

    @Test
    public void testTicketCreationExecutor() throws Exception {
        long questionId1 = createQuestion(1L, MODEL_ID);
        long questionId2 = createQuestion(2L, MODEL_ID);
        //one question for another model (ticket won't be created)
        long questionId3 = createQuestion(3L, MODEL_ID + 1);

        mockCommonServices();
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return (issueCreate != null) &&
                ((String) issueCreate.getValues().getOrElse("description", ""))
                    .contains("Вопрос " + questionId1);
        }))).thenReturn(new Issue("1", null, TICKET_KEY, null, 1, new EmptyMap<>(), null));
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return (issueCreate != null) &&
                ((String) issueCreate.getValues().getOrElse("description", ""))
                    .contains("Вопрос " + questionId2);
        }))).thenThrow(new RuntimeException("Unable create issue"));

        try {
            executor.createTicketByQuestionCtm();
            Assertions.fail();
        } catch (QaRuntimeException ex) {
            Assertions.assertEquals(
                "1 exception(s) found. First exception: Unable create issue",
                ex.getMessage()
            );
        }
        checkTicketQuestions(Map.of(TICKET_KEY, questionId1));
    }

    @Test
    public void testAnotherVendorTicketCreation() throws Exception {
        //add new vendor
        long anotherModelId = 300L;
        long anotherVendorId = 200L;
        String anotherTicketQueue = "COMMOTEST";
        String anotherTicketKey = anotherTicketQueue + "-1";
        pgJdbcTemplate.update(
            "insert into qa.ext_models_for_tickets (model_id, vendor_id) values (?, ?) on conflict do nothing",
            anotherModelId, anotherVendorId
        );
        pgJdbcTemplate.update(
            "insert into qa.vendor_ticket_mapping (vendor_id, ticket_queue) values (?, ?) on conflict do nothing",
            anotherVendorId, anotherTicketQueue
        );

        long questionId = createQuestion(1L, MODEL_ID);
        long anotherQuestionId = createQuestion(1L, anotherModelId);

        mockCommonServices();
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return (issueCreate != null) &&
                ((String) issueCreate.getValues().getOrElse("queue", ""))
                    .equals(TICKET_QUEUE);
        }))).thenReturn(new Issue("1", null, TICKET_KEY, null, 1, new EmptyMap<>(), null));
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return (issueCreate != null) &&
                ((String) issueCreate.getValues().getOrElse("queue", ""))
                    .equals(anotherTicketQueue);
        }))).thenReturn(new Issue("1", null, anotherTicketKey, null, 1, new EmptyMap<>(), null));

        executor.createTicketByQuestionCtm();
        checkTicketQuestions(Map.of(TICKET_KEY, questionId, anotherTicketKey, anotherQuestionId));
    }


    private long createQuestion(Long userId, Long modelId) {
        Question question = Question.buildModelQuestion(userId, QUESTION_TEXT, modelId);
        return questionService.createQuestionGetId(question, new SecurityData());
    }

    private void mockCommonServices() throws Exception {
        Email activeEmail = new Email("active@yandex.ru", true);
        Mockito.when(persNotifyClient.getEmails(anyLong())).thenReturn(Set.of(activeEmail));
        Model model = new Model();
        model.setName("Model");
        Mockito.when(reportService.getModelById(anyLong())).thenReturn(Optional.of(model));
    }

    private void checkTicketQuestions(Map<String, Long> expectedTicketKeyQuestionIdMap) {
        Map<String, Long> ticketKeyQuestionIdMap = pgJdbcTemplate.query(
            "select ticket_key, qa_entity_id " +
                "from qa.ticket " +
                "where qa_entity_type = ?",
            (rs) -> {
                Map<String, Long> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getString("ticket_key"), rs.getLong("qa_entity_id"));
                }
                return result;
            },
            QaEntityType.QUESTION.getValue()
        );

        Assertions.assertEquals(expectedTicketKeyQuestionIdMap, ticketKeyQuestionIdMap);
    }
}
