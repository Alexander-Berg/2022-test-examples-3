package ru.yandex.market.pers.tms.timer.startrek;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * @author grigor-vlad
 * 29.03.2022
 */
public class CreateCtmModelGradeTicketExecutorTest extends MockedPersTmsTest {
    private static final Long TICKET_MODEL_ID = 1L;
    private static final Long TICKET_VENDOR_ID = 1L;
    private static final String TICKET_QUEUE = "QASTMTEST";
    private static final Long ANOTHER_TICKET_VENDOR_ID = 2L;
    private static final Long ANOTHER_VENDOR_TICKET_MODEL_ID = 2L;
    private static final String ANOTHER_TICKET_QUEUE = "COMMOTEST";
    private static final Long AUTHOR_ID = 123L;
    private static final String NORMAL_TEXT = "Хороший текст отзыва";

    private static final String TICKET_KEY = TICKET_QUEUE + "-1";
    private static final Issue ISSUE = new Issue(
        "1", null, TICKET_KEY, null, 1, new EmptyMap<>(), null
    );

    @Autowired
    private StartrekService startrekService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PersNotifyClient persNotifyClient;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private CreateCtmModelGradeTicketExecutor createCtmModelGradeTicketExecutor;

    @Before
    public void initTestEnvironment() {
        createCtmModelGradeTicketExecutor.setLastGradeId(0L);
        pgJdbcTemplate.update(
            "insert into vendor_ticket_mapping (vendor_id, ticket_queue) values (?, ?) on conflict do nothing",
            TICKET_VENDOR_ID, TICKET_QUEUE
        );
        pgJdbcTemplate.update(
            "insert into ext_models_for_tickets (model_id, vendor_id) values (?, ?) on conflict do nothing",
            TICKET_MODEL_ID, TICKET_VENDOR_ID
        );
    }

    @Test
    public void testSuccessfulTicketCreation() throws Exception {
        long gradeIdForTicket = createModelGrade(TICKET_MODEL_ID, AUTHOR_ID);
        long gradeIdNotForTicket = createModelGrade(TICKET_MODEL_ID + 1, AUTHOR_ID);

        mockCommonServices();
        Mockito.when(startrekService.createTicket(any())).thenReturn(ISSUE);

        createCtmModelGradeTicketExecutor.runTicketCreation();
        //Тикет должен создаться только по первому отзыву.
        checkTicketsAndGrades(Map.of(gradeIdForTicket, TICKET_KEY));

        createCtmModelGradeTicketExecutor.runTicketCreation();
        //После второй работы джобы новый тикет не должен быть создан
        checkTicketsAndGrades(Map.of(gradeIdForTicket, TICKET_KEY));
    }

    @Test
    public void testUnsuccessfulTicketCreation() throws Exception {
        long successfulGradeId = createModelGrade(TICKET_MODEL_ID, AUTHOR_ID);
        long unsuccessfulGradeId = createModelGrade(TICKET_MODEL_ID, AUTHOR_ID + 1);

        mockCommonServices();
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return issueCreate != null &&
                ((String) issueCreate.getValues().getOrElse("description", ""))
                    .contains("отзыв " + successfulGradeId);
        }))).thenReturn(ISSUE);
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return issueCreate != null &&
                ((String) issueCreate.getValues().getOrElse("description", ""))
                    .contains("отзыв " + unsuccessfulGradeId);
        }))).thenThrow(new RuntimeException("Unable create ticket"));

        try {
            createCtmModelGradeTicketExecutor.runTicketCreation();
            Assert.fail();
        } catch (RuntimeException ex) {
            assertEquals("1 exception(s) found. First exception: Unable create ticket", ex.getMessage());
        }

        checkTicketsAndGrades(Map.of(successfulGradeId, TICKET_KEY));
    }

    @Test
    public void testAnotherVendorTicketCreation() throws Exception {
        //initialize another vendor and queue
        pgJdbcTemplate.update(
            "insert into vendor_ticket_mapping (vendor_id, ticket_queue) values (?, ?) on conflict do nothing",
            ANOTHER_TICKET_VENDOR_ID, ANOTHER_TICKET_QUEUE
        );
        pgJdbcTemplate.update(
            "insert into ext_models_for_tickets (model_id, vendor_id) values (?, ?) on conflict do nothing",
            ANOTHER_VENDOR_TICKET_MODEL_ID, ANOTHER_TICKET_VENDOR_ID
        );
        String anotherTicketQueue = ANOTHER_TICKET_QUEUE + "-1";
        Issue anotherIssue = new Issue(
            "1", null, anotherTicketQueue, null, 1, new EmptyMap<>(), null
        );

        long gradeId = createModelGrade(TICKET_MODEL_ID, AUTHOR_ID);
        long anotherVendorGradeId = createModelGrade(ANOTHER_VENDOR_TICKET_MODEL_ID, AUTHOR_ID);

        mockCommonServices();
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return issueCreate != null &&
                ((String) issueCreate.getValues().getOrElse("queue", ""))
                    .equals(TICKET_QUEUE);
        }))).thenReturn(ISSUE);
        Mockito.when(startrekService.createTicket(argThat((issueCreate) -> {
            return issueCreate != null &&
                ((String) issueCreate.getValues().getOrElse("queue", ""))
                    .contains(ANOTHER_TICKET_QUEUE);
        }))).thenReturn(anotherIssue);

        createCtmModelGradeTicketExecutor.runTicketCreation();
        //Должны создаться разные тикеты в разных очередях
        checkTicketsAndGrades(Map.of(gradeId, TICKET_KEY, anotherVendorGradeId, anotherTicketQueue));
    }


    private void mockCommonServices() throws Exception {
        //mock getEmails
        Email activeEmail = new Email("active@yandex.ru", true);
        Mockito.when(persNotifyClient.getEmails(anyLong())).thenReturn(Set.of(activeEmail));

        //mock getModelById
        Model model = new Model();
        model.setName("Модель");
        Mockito.when(reportService.getModelById(anyLong())).thenReturn(Optional.of(model));
    }

    private long createModelGrade(Long modelId, Long authorId) {
        return gradeCreator.createModelGrade(modelId, authorId, ModState.APPROVED, NORMAL_TEXT);
    }

    private void checkTicketsAndGrades(Map<Long, String> gradesAndTickets) {
        Map<Long, String> result = pgJdbcTemplate.query("select * from grade_ticket",
            rs -> {
                Map<Long, String> gradeIdTicketKeyMap = new HashMap<>();
                while (rs.next()) {
                    gradeIdTicketKeyMap.put(rs.getLong("grade_fix_id"),
                        rs.getString("ticket_key"));
                }
                return gradeIdTicketKeyMap;
            });
        assertEquals(gradesAndTickets, result);
    }

}
