package ru.yandex.market.tpl.core.domain.tracker;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import ru.yandex.bolts.collection.impl.EmptyIterator;
import ru.yandex.market.tpl.common.util.TestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelPayload;
import ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketParams;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.startrek.client.AttachmentsClient;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketType.ORDER;
import static ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketType.TASK;

public class TrackerServiceTest extends TplAbstractTest {

    @Captor
    private ArgumentCaptor<IssueCreate> issueCaptor;

    private final TrackerService trackerService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Session trackerSession;
    private final Issues trackerIssues;
    private final Environment environment;
    private final ClientReturnGenerator clientReturnGenerator;

    @Autowired
    public TrackerServiceTest(@Qualifier("trackerService") TrackerService trackerService,
                              ConfigurationServiceAdapter configurationServiceAdapter,
                              Session trackerSession, Issues trackerIssues,
                              Environment environment,
                              ClientReturnGenerator clientReturnGenerator) {
        this.trackerService = trackerService;
        this.configurationServiceAdapter = configurationServiceAdapter;
        this.trackerSession = trackerSession;
        this.trackerIssues = trackerIssues;
        this.environment = environment;
        this.clientReturnGenerator = clientReturnGenerator;
    }

    @BeforeEach
    void init() {
        configureMockTrackingSession(null);

        configurationServiceAdapter.mergeValue(ConfigurationProperties.LOCKER_CANCEL_TICKET_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.PVZ_CANCEL_TICKET_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.ORDER_CANCEL_TICKET_ENABLED, true);
    }

    @AfterEach
    void after() {
        clearInvocations(trackerIssues);
        clearInvocations(trackerSession);
    }

    @Test
    void cancelTaskTest() {
        var payload = new LockerCancelPayload("1", "PVZ",
                789L, List.of("789", "123"), TASK, "?????????? ?????????????? ??????????????", "comment", List.of("https://ya.ru"), null);

        var params = new LockerCancelTicketParams(payload);
        trackerService.createLavkaOrderCancelledTicket(params);

        verify(trackerIssues).create(issueCaptor.capture());

        IssueCreate issue = issueCaptor.getValue();
        assertThat(issue).extracting("values")
                .extracting("summary", "components", "description",
                        "deliveryName", "customerOrderNumber", "queue", "tags")
                .containsOnly("???????????? ?????????????? 789 ???? ?????????????? ?????????? ?????????????? ??????????????", new long[]{100088L},
                        "ID ??????????????: 789\n" +
                                "ID ??????????????: 789, 123\n" +
                                "???????????? ???? ?????????? ?? ????:\n" +
                                "???? ??????????????  ?????????? super company\n" +
                                "???? ??????????????  ?????????? super company\n" +
                                "??????????????????????: comment\n" +
                                "????????:\n" +
                                "((https://ya.ru/orig ????????_1))\n",
                        "",
                        "789, 123",
                        "PARTNERPICKUP",
                        new String[]{"??????????_??????????????_??????????????"});
    }

    @Test
    void cancelOrderTest() {
        var payload = new LockerCancelPayload("1", "LOCKER",
                null, List.of("456", "123"), ORDER, "?????? ?????????????? ?? ??????????????????", "comment", List.of("https://ya.ru",
                "https://yandex.ru"), null);
        var params = new LockerCancelTicketParams(payload);
        trackerService.createLavkaOrderCancelledTicket(params);

        verify(trackerIssues).create(issueCaptor.capture());

        IssueCreate issue = issueCaptor.getValue();
        assertThat(issue).extracting("values")
                .extracting("summary", "components", "description",
                        "deliveryName", "customerOrderNumber", "queue", "tags")
                .containsOnly("???????????? ???????????? 456 ???? ?????????????? ?????? ?????????????? ?? ??????????????????", new long[]{100087},
                        "ID ????????????: 456\n" +
                                "???????????? ???? ?????????? ?? ????: ???? ??????????????  ?????????? super company\n" +
                                "??????????????????????: comment\n" +
                                "????????:\n" +
                                "((https://ya.ru/orig ????????_1))\n" +
                                "((https://yandex.ru/orig ????????_2))\n",
                        "",
                        "456, 123",
                        "POSTAMATSUP",
                        new String[]{"??????_??????????????_??_??????????????????"});
    }

    @Test
    void droppedItemsCreationTicketTest() {
        //given
        var mockedEnvironment = mock(Environment.class);
        TestUtil.setPrivateFinalField(trackerService, "environment", mockedEnvironment);
        Mockito.when(mockedEnvironment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        var mockedIssue = mock(Issue.class);
        configureMockTrackingSession(mockedIssue);

        var mockedAttachment = mock(Attachment.class);
        var mockedAttachmentsClient = mock(AttachmentsClient.class);
        when(mockedAttachmentsClient.upload(any(String.class), any(InputStream.class),
                any(ContentType.class))).thenReturn(mockedAttachment);
        when(trackerSession.attachments()).thenReturn(mockedAttachmentsClient);

        String ticketSummary = "summary";
        String ticketDescr = "descr";

        //when
        trackerService.createDroppedItemsTicket(List.of(), ticketSummary, ticketDescr);
        TestUtil.setPrivateFinalField(trackerService, "environment", environment);

        //then
        verify(trackerIssues).create(issueCaptor.capture());
        IssueCreate issue = issueCaptor.getValue();

        assertThat(issue).extracting("values")
                .extracting("summary", "description",
                         "queue", "tags")
                .containsOnly(
                        ticketSummary,
                        ticketDescr,
                        "TPLWRONGADRESS",
                        new String[]{TrackerService.DROPPED_ITEMS_TICKET_TAG}
                );
    }


    @Test
    void createZeroCoordinatesTicketForClientReturnTest(){

        //given
        var mockedEnvironment = mock(Environment.class);
        TestUtil.setPrivateFinalField(trackerService, "environment", mockedEnvironment);
        Mockito.when(mockedEnvironment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        var mockedIssue = mock(Issue.class);
        configureMockTrackingSession(mockedIssue);

        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(2022,4,28,10,0,0),
                LocalDateTime.of(2022,4,28,11,0,0)
        );
        clientReturn.setExternalReturnId("001");
        clientReturn.setExternalOrderId("002");

        when(trackerIssues.find(any(SearchRequest.class))).thenReturn(new EmptyIterator<>());

        //when
        trackerService.createZeroCoordinatesTicket(clientReturn);

        //then
        verify(trackerIssues).create(issueCaptor.capture());
        IssueCreate issue = issueCaptor.getValue();

        assertThat(issue)
                .extracting("values")
                .extracting("summary", "description",
                        "queue", "tags")
                .containsOnly(
                        "28.04.2022 - ?????????????? ???????????????????? ???????????????? ??? 001",
                        "?????????? ????????????????: ((https://yandex.ru/maps/?text=%D0%B3.+Moscow%2C+Tverskaya%2C+14%2C+%D0%BA.+2 ??. Moscow, Tverskaya, ??. 14, ??????. 1, ??. 2, ????. 213)) \n" +
                                "???????????? ???? OW: ((https://ow.market.yandex-team.ru/order/002)) \n" +
                                "???????????? ???? ??????????????????: ???? ?????????????? ?????????? super company \n" +
                                "???????????? ????????????????: - \n" +
                                "???????? - ?????????? ???????????????? ????: 28.04.2022 11:00:00",
                        "TPLWRONGADRESS",
                        new String[]{TrackerService.ZERO_COORDINATES_TAG}
                );

    }

    private void configureMockTrackingSession(Issue mockedIssue) {
        when(trackerSession.issues()).thenReturn(trackerIssues);
        when(trackerIssues.create(any())).thenReturn(mockedIssue);
    }
}
