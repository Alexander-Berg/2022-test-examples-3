package ru.yandex.market.supportwizard;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.api.FrontController;
import ru.yandex.market.supportwizard.base.PartnerMoney;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.base.ProgramType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.storage.PartnerMoneyRepository;
import ru.yandex.market.supportwizard.storage.TicketRepository;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.supportwizard.TestHelper.getValue;

@DbUnitDataSet
public class FrontControllerTest extends BaseFunctionalTest {

    private static final Long SHOP_A_ID = 100L;
    private static final Long SHOP_A_CAMPAIGN_ID = 100L;
    private static final Long SHOP_A_BUSINESS_ID = 1000L;
    private static final String SHOP_A_NAME = "testShopName1";
    private static final Long SHOP_A_MONEY = 1000L;
    private static final PartnerMoney SHOP_A =
            new PartnerMoney.Builder(SHOP_A_ID, PartnerType.SHOP)
                    .campaignId(SHOP_A_CAMPAIGN_ID)
                    .businessId(SHOP_A_BUSINESS_ID)
                    .partnerName(SHOP_A_NAME)
                    .money(SHOP_A_MONEY)
                    .partnerPlacementProgramTypes(List.of(ProgramType.ADV))
                    .build();

    private Issues issues = Mockito.mock(Issues.class);

    @Autowired
    private TestRestTemplate template;

    @Autowired
    Session startrekSession;

    @Autowired
    PartnerMoneyRepository partnerMoneyRepository;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    FrontController frontController;

    @Test
    void formTest() {
        ResponseEntity<String> response = template.getForEntity("/form", String.class);
        assertTrue(Objects.requireNonNull(response.getBody()).contains("<title>form</title>"));
    }

    @Test
    void indexTest() {
        ResponseEntity<String> response = template.getForEntity("/", String.class);
        assertTrue(Objects.requireNonNull(response.getBody()).contains("<title>Main page</title>"));
    }

    @Test
    void weightTicketTest() {
        Issue issue = spy(new Issue("", URI.create(""), "TEST-1", "", 0, DefaultMapF.wrap(Map.of(
                "partnerIDs", Option.of(Long.toString(SHOP_A_ID)),
                "campaignIDs", Option.of(Long.toString(SHOP_A_CAMPAIGN_ID)),
                "businessIds", Option.of(Long.toString(SHOP_A_BUSINESS_ID)),
                "estimatedImportance", Option.of(1000000L),
                "createdBy", new SimpleUserRef("pupkin"))),
                null));
        doAnswer(invocation -> issue).when(issue).update(any());

        Mockito.when(startrekSession.issues()).thenReturn(issues);
        when(startrekSession.issues().get(anyString())).thenReturn(issue);
        assertNull(ticketRepository.findById("TEST-1"));
        partnerMoneyRepository.save(SHOP_A.toPartnerMoney());
        template.postForObject(
                "/ticket-weight?ticket=TEST-1&partnerIds=100&campaignIds=100&agencies=",
                null,
                String.class);
        assertNotNull(ticketRepository.findById("TEST-1"));
        ArgumentCaptor<IssueUpdate> issueUpdateArgumentCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issue).update(issueUpdateArgumentCaptor.capture());

        IssueUpdate issueUpdate = issueUpdateArgumentCaptor.getValue();

        assertEquals(((Long) getValue(issueUpdate, "estimatedImportance")), 1000L);
    }
}
