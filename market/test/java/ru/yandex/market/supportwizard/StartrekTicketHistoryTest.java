package ru.yandex.market.supportwizard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.bolts.collection.impl.DefaultListF;
import ru.yandex.market.supportwizard.base.PartnerMoney;
import ru.yandex.market.supportwizard.base.PartnerMoneyCollection;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.PartnerMoneyCollectionService;
import ru.yandex.market.supportwizard.service.StartrekService;
import ru.yandex.market.supportwizard.service.tasks.StartrekTicketHistoryManager;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.supportwizard.TestHelper.getCollection;
import static ru.yandex.market.supportwizard.TestHelper.getComment;

/**
 * Тесты для {@link StartrekTicketHistoryManager}
 */
public class StartrekTicketHistoryTest extends BaseFunctionalTest {
    private static final PartnerMoneyCollection PARTNER_MONEY_COLLECTION =
            new PartnerMoneyCollection(List.of(
                    new PartnerMoney.Builder(100L, PartnerType.SHOP)
                            .campaignId(101L)
                            .partnerName("TestName1")
                            .money(10)
                            .hasManager(false)
                            .build(),
                    new PartnerMoney.Builder(200L, PartnerType.SHOP)
                            .campaignId(201L)
                            .partnerName("TestName2")
                            .money(10)
                            .hasManager(false)
                            .build()));

    private static StartrekTicketHistoryManager startrekTicketHistoryManager;
    private static Issue targetIssue;

    @Value("${startrek.filter.plsupport}")
    private String filter;

    @BeforeEach
    void init() {
        PartnerMoneyCollectionService partnerMoneyCollectionService = mock(PartnerMoneyCollectionService.class);
        Session session = mock(Session.class);
        Issues issues = mock(Issues.class);
        targetIssue = mock(Issue.class);
        Issue firstFoundIssue = mock(Issue.class);

        when(partnerMoneyCollectionService.loadPartnerMoneyCollection(any(), any(), any()))
                .thenReturn(PARTNER_MONEY_COLLECTION);

        when(session.issues()).thenReturn(issues);
        when(issues.find(ArgumentMatchers.matches(".*MBISupportWizardFilterForPlSupport.*")))
                .thenAnswer(invocation -> DefaultIteratorF.wrap(Collections.singleton(targetIssue).iterator()));
        when(targetIssue.getO(eq("partnerIDs"))).thenReturn(Option.of(Option.of("100 200")));
        when(targetIssue.getO(eq("campaignIDs"))).thenReturn(Option.of(Option.of("101 201")));
        when(targetIssue.getO(eq("businessIds"))).thenReturn(Option.of(Option.of("1001")));
        when(targetIssue.getO(eq("estimatedImportance"))).thenReturn(Option.of(Option.of(0L)));
        when(targetIssue.getO(eq("weight"))).thenReturn(Option.of(Option.of(0.0D)));
        when(targetIssue.getCreatedBy()).thenReturn(new SimpleUserRef("pupkin"));

        when(firstFoundIssue.getO(eq("partnerIDs"))).thenReturn(Option.of(Option.of("100 200")));
        when(firstFoundIssue.getO(eq("campaignIDs"))).thenReturn(Option.of(Option.of("101 201")));
        when(firstFoundIssue.getO(eq("businessIds"))).thenReturn(Option.of(Option.of("1001")));
        when(firstFoundIssue.getO(eq("estimatedImportance"))).thenReturn(Option.of(Option.of(0L)));

        when(targetIssue.getKey()).thenReturn("PLSUPPORT-1");

        when(issues.find(ArgumentMatchers.matches(".*Partner IDs.*")))
                .thenAnswer(invocation -> DefaultIteratorF
                        .wrap(Arrays.asList(targetIssue, firstFoundIssue).iterator()));
        when(firstFoundIssue.getKey()).thenReturn("PLSUPPORT-2");

        when(issues.get(anyString())).thenReturn(targetIssue);
        when(targetIssue.getTags()).thenReturn(DefaultListF.wrap(Collections.emptyList()));

        StartrekService startrekService = new StartrekService(session);

        startrekTicketHistoryManager = new StartrekTicketHistoryManager(startrekService, partnerMoneyCollectionService, filter);
    }


    @Test
    @DisplayName("Add history for tickets without history")
    void testHistoryAdding() {
        startrekTicketHistoryManager.doJob(null);

        ArgumentCaptor<IssueUpdate> issueUpdateArgumentCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(targetIssue).update(issueUpdateArgumentCaptor.capture());

        IssueUpdate issueUpdate = issueUpdateArgumentCaptor.getValue();

        String comment = getComment(issueUpdate);
        String tag = (String) getCollection(issueUpdate, "tags").getO(0).get();

        assertEquals("support_history", tag);
        assertEquals("История тикетов:\n"
                + "<{100\n"
                + "https://st.yandex-team.ru/PLSUPPORT-2\n"
                + "}>\n"
                + "<{200\n"
                + "https://st.yandex-team.ru/PLSUPPORT-2\n"
                + "}>\n", comment);
    }
}
