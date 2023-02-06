package ru.yandex.market.abo.core.ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.forecast.ShopForecastManager;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemType;
import ru.yandex.market.abo.core.ticket.flag.TicketFlagService;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.cpa.MbiApiService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 05.06.17.
 */
class ProblemMailSenderTest extends EmptyTestWithTransactionTemplate {

    @InjectMocks
    ProblemMailSender problemMailSender;
    @Mock
    OfferDbService offerDbService;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    TicketService ticketService;
    @Mock
    TicketFlagService ticketFlagService;
    @Mock
    ShopForecastManager shopForecastManager;

    private static final String XML = "" +
            "<abo-info>" +
            "    <offer>" +
            "        <id>offer-id</id>" +
            "        <title>title</title>" +
            "        <url>http://www.ya.ru</url>" +
            "    </offer>" +
            "    <problems>" +
            "        <problem>" +
            "            <description>some description</description>" +
            "            <comment>some comment</comment>" +
            "        </problem>" +
            "        <problem>" +
            "            <description>another description</description>" +
            "            <comment>another comment</comment>" +
            "        </problem>" +
            "    </problems>" +
            "</abo-info>";


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void buildXmlTest() {
        var offer = createOffer("offer-id", "title", "http://www.ya.ru");
        var problems = List.of(
                createProblem("some description", "some comment"),
                createProblem("another description", "another comment")
        );
        assertEquals(XML.replaceAll("[ \n]", ""), problemMailSender.buildXml(offer, problems).replaceAll("[ \n]", ""));
    }

    @Test
    void problemOrderTest() {
        var problems = new ArrayList<>(List.of(
                createProblem(false, false, ProblemStatus.APPROVED),
                createProblem(false, true, ProblemStatus.APPROVED),
                createProblem(true, true, ProblemStatus.APPROVED),
                createProblem(true, true, ProblemStatus.NEW)
        ));
        var ticket = mock(Ticket.class);
        when(ticket.getProblems()).thenReturn(problems);
        for (int i = 0; i < 100; ++i) {
            var sortedProblems = problemMailSender.getApprovedProblemsSorted(ticket);

            assertTrue(sortedProblems.get(0).isCritical());
            assertTrue(sortedProblems.get(0).getProblemType().isCritical());
            assertFalse(sortedProblems.get(1).isCritical());
            assertTrue(sortedProblems.get(1).getProblemType().isCritical());
            assertFalse(sortedProblems.get(2).isCritical());
            assertFalse(sortedProblems.get(2).getProblemType().isCritical());

            Collections.shuffle(problems);
        }
    }

    private Offer createOffer(String id, String title, String url) {
        var offer = new Offer();
        offer.setShopOfferId(id);
        offer.setName(title);
        offer.setUrl(url);
        return offer;
    }

    private Problem createProblem(boolean critical, boolean criticalType, ProblemStatus status) {
        return createProblem(critical, criticalType, null, null, status);
    }

    private Problem createProblem(String description, String comment) {
        return createProblem(false, false, description, comment, ProblemStatus.APPROVED);
    }

    private Problem createProblem(boolean critical, boolean criticalType, String description, String comment,
                                  ProblemStatus status) {
        var problemType = mock(ProblemType.class);
        when(problemType.getDescription()).thenReturn(description);
        when(problemType.isCritical()).thenReturn(criticalType);
        when(problemType.isSendingMessage()).thenReturn(true);

        var problem = mock(Problem.class);
        when(problem.getProblemType()).thenReturn(problemType);
        when(problem.getPublicComment()).thenReturn(comment);
        when(problem.isCritical()).thenReturn(critical);
        when(problem.getStatus()).thenReturn(status);
        return problem;
    }
}
