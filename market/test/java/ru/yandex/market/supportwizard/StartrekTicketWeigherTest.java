package ru.yandex.market.supportwizard;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.PartnerMoney;
import ru.yandex.market.supportwizard.base.PartnerMoneyCollection;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.PartnerMoneyCollectionService;
import ru.yandex.market.supportwizard.service.StartrekService;
import ru.yandex.market.supportwizard.service.WeighterService;
import ru.yandex.market.supportwizard.service.tasks.StartrekTicketWeigher;
import ru.yandex.market.supportwizard.storage.PartnerMoneyRepository;
import ru.yandex.market.supportwizard.storage.TicketEntity;
import ru.yandex.market.supportwizard.storage.TicketRepository;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.supportwizard.TestHelper.getComment;
import static ru.yandex.market.supportwizard.TestHelper.getValue;

/**
 * Тесты для {@link StartrekTicketWeigher}
 */
@DbUnitDataSet
public class StartrekTicketWeigherTest extends BaseFunctionalTest {
    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    Session startrekSession;

    @Autowired
    WeighterService weighterService;

    private Issues issues = Mockito.mock(Issues.class);

    StartrekTicketWeigher startrekTicketWeigherSpy;

    PartnerMoneyCollectionService partnerMoneyCollectionService;

    private static final ArgumentCaptor<IssueUpdate> ISSUE_UPDATE_ARGUMENT_CAPTOR =
            ArgumentCaptor.forClass(IssueUpdate.class);
    private static final String TICKET_KEY = "TEST-1";
    private static final String COMMENT =
            "Данная задача затрагивает:\n" +
                    "<{ Партнеры:\n" +
                    "#|\n" +
                    "||**Name**|**Partner ID**|**Campaign ID**|**Business ID**|**Value estimate**|**Newbie status**||\n" +
                    "||TestName|100|100|1000|10|старенький||\n" +
                    "|#}>\n" +
                    "**Оценка важности: 10**\n" +
                    "**Итоговый вес: 718**\n";
    private static final String AUTO_COMMENT = "Тикет был развешен автоматически.";
    private static final String MANUAL_COMMENT = "Тикет был развешен вручную.";
    private static final Long PARTNER_ID = 100L;
    private static final Long CAMPAIGN_ID = 100L;
    private static final Long BUSINESS_ID = 1000L;
    private static final TicketEntity TICKET_ENTITY = new TicketEntity(TICKET_KEY);
    private static final PartnerMoneyCollection PARTNER_MONEY_COLLECTION =
            new PartnerMoneyCollection(List.of(
                    new PartnerMoney.Builder(PARTNER_ID, PartnerType.SHOP)
                            .campaignId(CAMPAIGN_ID)
                            .businessId(BUSINESS_ID)
                            .partnerName("TestName")
                            .money(10)
                            .hasManager(false)
                            .build()
            ));

    private Issue issue = spy(new Issue("", URI.create(""), TICKET_KEY, "", 0, DefaultMapF.wrap(Map .of(
            "partnerIDs", Option.of(Long.toString(PARTNER_ID)),
            "campaignIDs", Option.of(Long.toString(CAMPAIGN_ID)),
            "estimatedImportance", Option.of(1000000L),
            "createdBy", new SimpleUserRef("pupkin"))),
            null));
    private Issue weightedIssue = spy(new Issue("", URI.create(""), TICKET_KEY, "", 0, DefaultMapF.wrap(Map.of(
            "partnerIDs", Option.of(Long.toString(PARTNER_ID)),
            "campaignIDs", Option.of(Long.toString(CAMPAIGN_ID)),
            "estimatedImportance", Option.of(10L),
            "createdBy", new SimpleUserRef("pupkin"))),
            null));
    private Issue issueWithNullValue = spy(new Issue("", URI.create(""), TICKET_KEY, "", 0, DefaultMapF.wrap(Map.of(
            "partnerIDs", Option.of(Long.toString(PARTNER_ID)),
            "campaignIDs", Option.of(Long.toString(CAMPAIGN_ID)),
            "estimatedImportance", Option.empty(),
            "createdBy", new SimpleUserRef("pupkin"))),
            null));

    @Value("${startrek.filter}")
    private String filter;

    @BeforeEach
    void init() {
        partnerMoneyCollectionService = mock(PartnerMoneyCollectionService.class);
        var partnerMoneyRepository = mock(PartnerMoneyRepository.class);
        when(partnerMoneyRepository.getWeightStatsByType())
                .thenReturn(Map.of(PartnerType.SHOP, TestHelper.shopStatisticsEntity()));
        when(partnerMoneyCollectionService.loadPartnerMoneyCollection(any(), any(), any()))
                .thenReturn(PARTNER_MONEY_COLLECTION);
        Mockito.when(startrekSession.issues()).thenReturn(issues);

        /*
        TODO: использование мока здесь ненужно. Переделать на использование нормальных постгревых
        табличек.
         */

        startrekTicketWeigherSpy = new StartrekTicketWeigher(
                new StartrekService(startrekSession),
                weighterService,
                ticketRepository,
                partnerMoneyCollectionService,
                partnerMoneyRepository, filter
        );

        doAnswer(invocation -> issue).when(issue).update(any());
        doAnswer(invocation -> issueWithNullValue).when(issueWithNullValue).update(any());
    }

    @Test
    @DisplayName("Auto weighing updates ticket if the weight has changed")
    void testAutoWeighWeighedUpdate() {
        setIssue(issue);
        ticketRepository.save(TICKET_ENTITY);
        startrekTicketWeigherSpy.doJob(null);

        verify(issue).update(ISSUE_UPDATE_ARGUMENT_CAPTOR.capture());
        IssueUpdate issueUpdate = ISSUE_UPDATE_ARGUMENT_CAPTOR.getValue();

        assertEquals(10L, ((Long) getValue(issueUpdate, "estimatedImportance")));
        assertThrows(NoSuchElementException.class, () -> getComment(issueUpdate));
    }

    @Test
    @DisplayName("Auto weighing updates ticket if the weight has changed to null")
    void testAutoWeighWeighedUpdateNull() {
        setIssue(issueWithNullValue);
        startrekTicketWeigherSpy.doJob(null);

        verify(issueWithNullValue).update(ISSUE_UPDATE_ARGUMENT_CAPTOR.capture());
        IssueUpdate issueUpdate = ISSUE_UPDATE_ARGUMENT_CAPTOR.getValue();

        assertEquals(10L, ((Long) getValue(issueUpdate, "estimatedImportance")));
    }

    @Test
    @DisplayName("Auto weighing weighs not weighed ticket")
    void testAutoWeighNotWeighed() {
        assertNull(ticketRepository.findById(TICKET_KEY));

        setIssue(issue);
        startrekTicketWeigherSpy.doJob(null);

        verify(issue).update(ISSUE_UPDATE_ARGUMENT_CAPTOR.capture());
        IssueUpdate issueUpdate = ISSUE_UPDATE_ARGUMENT_CAPTOR.getValue();

        assertEquals(10L, ((Long) getValue(issueUpdate, "estimatedImportance")));
        assertEquals(ticketRepository.findById(TICKET_KEY), TICKET_ENTITY);

        String comment = getComment(issueUpdate);
        assertTrue(comment.contains(COMMENT));
        assertTrue(comment.contains(AUTO_COMMENT));
    }

    @Test
    @DisplayName("Auto weighing does not weigh already weighed ticket, if the weight hasn't changed")
    void testAutoWeighWeighed() {
        setIssue(weightedIssue);
        ticketRepository.save(TICKET_ENTITY);
        startrekTicketWeigherSpy.doJob(null);

        verify(weightedIssue, never()).update(any());
    }

    @Test
    @DisplayName("Manual weighing weighs not weighed ticket")
    void testManualWeighNotWeighed() {
        when(startrekSession.issues().get(anyString())).thenReturn(issue);
        startrekTicketWeigherSpy.manualWeighTicket(TICKET_KEY, PARTNER_MONEY_COLLECTION);

        verify(issue).update(ISSUE_UPDATE_ARGUMENT_CAPTOR.capture());
        IssueUpdate issueUpdate = ISSUE_UPDATE_ARGUMENT_CAPTOR.getValue();

        assertEquals(10L, ((Long) getValue(issueUpdate, "estimatedImportance")));
        assertEquals(ticketRepository.findById(TICKET_KEY), TICKET_ENTITY);

        String comment = getComment(issueUpdate);
        assertTrue(comment.contains(COMMENT));
        assertTrue(comment.contains(MANUAL_COMMENT));
    }

    @Test
    @DisplayName("Manual weighing updates already weighed ticket")
    void testManualWeighWeighed() {
        when(startrekSession.issues().get(anyString())).thenReturn(issueWithNullValue);
        ticketRepository.save(TICKET_ENTITY);
        startrekTicketWeigherSpy.manualWeighTicket(TICKET_KEY, PARTNER_MONEY_COLLECTION);

        verify(issueWithNullValue).update(ISSUE_UPDATE_ARGUMENT_CAPTOR.capture());
        IssueUpdate issueUpdate = ISSUE_UPDATE_ARGUMENT_CAPTOR.getValue();

        assertEquals(10L, ((Long) getValue(issueUpdate, "estimatedImportance")));
        Assertions.assertEquals(ticketRepository.findById(TICKET_KEY), TICKET_ENTITY);
    }

    private void setIssue(Issue issue) {
        when(startrekSession.issues().find(anyString()))
                .thenReturn(DefaultIteratorF.wrap(Collections.singleton(issue).iterator()));
    }
}
