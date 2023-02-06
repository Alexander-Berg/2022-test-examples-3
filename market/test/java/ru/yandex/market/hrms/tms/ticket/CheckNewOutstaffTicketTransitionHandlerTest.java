package ru.yandex.market.hrms.tms.ticket;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.config.TestMockConfig;
import ru.yandex.market.hrms.core.domain.outstaff.ticket.CheckNewOutstaffTicketStatus;
import ru.yandex.market.hrms.core.domain.outstaff.ticket.CheckNewOutstaffTicketTag;
import ru.yandex.market.hrms.core.domain.outstaff.ticket.CheckNewOutstaffTicketTransitionHandler;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffService;
import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.outstaff.enums.OutstaffFormEnum;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffYqlRepoStub;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckNewOutstaffTicketTransitionHandlerTest extends AbstractCoreTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Session trackerSession;

    @Autowired
    private CheckNewOutstaffTicketTransitionHandler handler;

    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;

    @Autowired
    private OutstaffService service;

    @MockBean(name = "hrmsRestTemplate")
    private RestTemplate hrmsRestTemplate;

    @MockBean
    YaDiskClient yaDiskClient;

    private String getStringFromResource(String resource) throws Exception {
        return IOUtils.toString(TestMockConfig.class.getResourceAsStream(resource),
                StandardCharsets.UTF_8);
    }

    private void configureOutstaffYqlRepo(String ytTable, String resource, long ytId, long ytUid,
                                          Instant createdDate) throws Exception {
        String json = getStringFromResource(resource);

        List<YtTableDto> testDtos = new ArrayList<>();
        testDtos.add(new YtTableDto(ytId, ytUid, json, createdDate));

        var stub = (OutstaffYqlRepoStub) context.getBean("outstaffYqlRepo");
        stub.withData(ytTable, testDtos);
    }

    //проверяем что тикеты генерятся в OutstaffService
    @Test
    @DbUnitDataSet(
            before = "CheckNewOutstaffTicketTransitionHandlerTest.1row_active_outstaff.table.csv",
            after = "CheckNewOutstaffTicketTransitionHandlerTest.2rows_outstaff.table.no_error.csv"
    )

    @Disabled("ломается в CI из-за RestTemplate. Будет не нужен после выхода 2 фазу ЛК аутов")
    void shouldLoad1RowInOutstaffTableAndReturnNoDupWarning() throws Exception {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String inputJson = "/inputs/outstaff_yt_single_record.json";
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), inputJson, 129205766,
                326611127, Instant.now());

        when(yaDiskClient.downloadFile(any())).thenReturn(Optional.of(new byte[100]));

        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", List.of(MediaType.IMAGE_JPEG_VALUE));

        Mockito.when(hrmsRestTemplate.headForHeaders(any())).thenReturn(headers);

        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, "HRMSTESTGUESTS"));

        var warnings = service.loadOutstaffFromYt();

        Assertions.assertEquals(0, warnings.size());
    }

    //проверяем обработку сгенерированных тикетов в хендлере
    @Test
    @DbUnitDataSet(
            before = "CheckNewOutstaffTicketTransitionHandlerTest.updateTickets.before.csv",
            after = "CheckNewOutstaffTicketTransitionHandlerTest.updateTickets.after.csv"
    )
    void shouldSetDeactivationDateInTicketsToCancelledOutstaff() {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        StartrekTicket startrekTicket = mock(StartrekTicket.class);
        Mockito.when(startrekTicket.getKey()).thenReturn("HRMSTESTGUESTS-1");

        Mockito.when(startrekTicket.getStatus(CheckNewOutstaffTicketStatus.class)).thenReturn(CheckNewOutstaffTicketStatus.REFUSED);
        Mockito.when(startrekTicket.hasTag(CheckNewOutstaffTicketTag.SEEN)).thenReturn(false);

        handler.handleTransition(startrekTicket);
    }

    //проверяем деактивацию аутстафферов по результатам проверки
    @Test
    @DbUnitDataSet(
            before = "CheckNewOutstaffTicketTransitionHandlerTest.deactivate.before.csv",
            after = "CheckNewOutstaffTicketTransitionHandlerTest.deactivate.after.csv"
    )
    void shouldDeactivateOutstaffAfterFailingSecurityCheck() {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        service.deactivatePersons();
    }
}
