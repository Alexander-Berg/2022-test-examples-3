package ru.yandex.market.jmf.module.toloka;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.toloka.model.TolokaServer;
import ru.yandex.market.jmf.module.toloka.operations.SendTicketToAssessmentOperationHandler;
import ru.yandex.market.jmf.module.toloka.utils.AssessmentTicketUtils;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringJUnitConfig(classes = ModuleTolokaTestConfiguration.class)
public class SendTicketToAssessmentOperationHandlerTest {

    private static final Fqn ASSESSMENT_TICKET_FQN = Fqn.of("ticket$tolokaAssessmentTest");
    public static final String FIRST_COMMENT_TICKET_FORMATTER = "firstComment";
    public static final TolokaServer TOLOKA_SERVER_ID = TolokaServer.YANG;

    @Inject
    TicketTestUtils ticketTestUtils;

    @Inject
    AssessmentTicketUtils assessmentTicketUtils;

    @Inject
    BcpService bcpService;

    private Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
        context.assessmentRule = assessmentTicketUtils.createAssessmentRule(TOLOKA_SERVER_ID);
    }

    @Test
    public void createTicket() {
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Maps.of(
                SendTicketToAssessmentOperationHandler.ID, Map.of(
                        AssessmentTicket.ASSESSMENT_RULE, context.assessmentRule,
                        AssessmentTicket.ASSESSMENT_TICKET_FORMATTER, FIRST_COMMENT_TICKET_FORMATTER
                )
        ));
        assertEquals(AssessmentTicket.STATUS_ASSESSMENT_REQUIRED, ticket.getStatus());
        assertEquals(context.assessmentRule, ticket.getAssessmentRule());
        assertNotNull(ticket.getAssessmentTicketFormatter());
        assertEquals(FIRST_COMMENT_TICKET_FORMATTER, ticket.getAssessmentTicketFormatter().getCode());
    }

    @Test
    public void editTicket() {
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of());
        bcpService.edit(ticket, Maps.of(
                SendTicketToAssessmentOperationHandler.ID, Map.of(
                        AssessmentTicket.ASSESSMENT_RULE, context.assessmentRule,
                        AssessmentTicket.ASSESSMENT_TICKET_FORMATTER, FIRST_COMMENT_TICKET_FORMATTER
                )
        ));
        assertEquals(AssessmentTicket.STATUS_ASSESSMENT_REQUIRED, ticket.getStatus());
        assertEquals(context.assessmentRule, ticket.getAssessmentRule());
        assertNotNull(ticket.getAssessmentTicketFormatter());
        assertEquals(FIRST_COMMENT_TICKET_FORMATTER, ticket.getAssessmentTicketFormatter().getCode());
    }

    @Test
    public void errorWhenTransitionIsAbsence() {
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of());
        bcpService.edit(ticket, Map.of(HasWorkflow.STATUS, AssessmentTicket.STATUS_PROCESSING));
        var thrown = assertThrows(
                ValidationException.class, () ->
                        bcpService.edit(ticket, Maps.of(
                                SendTicketToAssessmentOperationHandler.ID, Map.of(
                                        AssessmentTicket.ASSESSMENT_RULE, context.assessmentRule,
                                        AssessmentTicket.ASSESSMENT_TICKET_FORMATTER, FIRST_COMMENT_TICKET_FORMATTER
                                )
                        ))
        );
        assertEquals(
                "У обращения не доступен переход в статус '%s'".formatted(AssessmentTicket.STATUS_ASSESSMENT_REQUIRED),
                thrown.getMessage()
        );
    }

    @MethodSource("dataForValidation")
    @ParameterizedTest(name = "{0}OnCreate")
    public void testValidationOnCreate(String title, Function<Context, Object> assessmentData, String expectedMessage) {
        var thrown = assertThrows(
                ValidationException.class, () ->
                        ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Maps.of(
                                SendTicketToAssessmentOperationHandler.ID, assessmentData.apply(context)
                        ))
        );
        assertEquals(expectedMessage, thrown.getMessage());
    }

    @MethodSource("dataForValidation")
    @ParameterizedTest(name = "{0}OnEdit")
    public void testValidationOnEdit(String title, Function<Context, Object> assessmentData, String expectedMessage) {
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of());
        var thrown = assertThrows(
                ValidationException.class, () ->
                        bcpService.edit(ticket, Maps.of(
                                SendTicketToAssessmentOperationHandler.ID, assessmentData.apply(context)
                        ))
        );
        assertEquals(expectedMessage, thrown.getMessage());
    }

    private static Stream<Arguments> dataForValidation() {
        return Stream.of(
                Arguments.of(
                        "propsIsNull",
                        (Function<Context, Object>) context -> null,
                        "Нет аргументов для операции отправки обращения в ассессмент"
                ),
                Arguments.of(
                        "invalidDataFormat",
                        (Function<Context, Object>) context -> List.of(),
                        "Wrong @sendTicketToAssessment format"
                ),
                Arguments.of(
                        "assessmentRuleIsNull",
                        (Function<Context, Object>) context -> Maps.of(
                                AssessmentTicket.ASSESSMENT_RULE, null,
                                AssessmentTicket.ASSESSMENT_TICKET_FORMATTER, FIRST_COMMENT_TICKET_FORMATTER
                        ),
                        "Для операции отправки обращения в ассессмент не указан атрибут 'assessmentRule'"
                ),
                Arguments.of(
                        "assessmentTicketFormatterIsNull",
                        (Function<Context, Object>) context -> Maps.of(
                                AssessmentTicket.ASSESSMENT_RULE, context.assessmentRule,
                                AssessmentTicket.ASSESSMENT_TICKET_FORMATTER, null
                        ),
                        "Для операции отправки обращения в ассессмент не указан атрибут 'assessmentTicketFormatter'"
                )
        );
    }

    private static class Context {
        AssessmentRule assessmentRule;
    }
}
