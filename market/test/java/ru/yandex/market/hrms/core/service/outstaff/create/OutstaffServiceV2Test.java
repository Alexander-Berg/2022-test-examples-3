package ru.yandex.market.hrms.core.service.outstaff.create;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.documentverification.DocumentVerificationPayload;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffServiceV2;
import ru.yandex.market.hrms.core.service.outstaff.document_verification.OutstaffDocVerificationQueueConsumer;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OutstaffServiceV2Test.before.csv")
public class OutstaffServiceV2Test extends AbstractCoreTest {

    @Autowired
    OutstaffServiceV2 outstaffServiceV2;

    @MockBean
    StartrekService startrekService;

    @Autowired
    OutstaffDocVerificationQueueConsumer outstaffDocVerificationQueueConsumer;

    @Captor
    ArgumentCaptor<IssueCreate> argumentCaptor;

    @AfterEach
    void clean() {
        Mockito.reset(startrekService);
    }

    @Test
    void createTicketForDocumentSc() {
        IssueCreate task = IssueCreate.builder()
                .queue("OUTSTAFFCHECK")
                .summary("жора жорочкин жоревич")
                .set("61fa9c2bb1257406398d663f--RazreseniaNaRabotu", "2022-05-27")
                .type("task")
                .description("https://lms-admin.tst.market.yandex-team" +
                        ".ru/hrms/external/partner/person?partnerId=1&personId=1\n" +
                        "\n" +
                        "Тип документа: Документ о разрешении на работу\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n")
                .set("61fa9c2bb1257406398d663f--autsorsKompania", "Company #test-login")
                .set("61fa9c2bb1257406398d663f--region", "регион")
                .build();

        Issue issue = new Issue(null, null, "123", "", 1, DefaultMapF.wrap(Map.of("", "")), null);

        StartrekTicket startrekTicket = new StartrekTicket(issue, null,
                null);
        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(startrekTicket);

        outstaffDocVerificationQueueConsumer.processPayload(
                DocumentVerificationPayload.builder()
                        .outstaffV2id(1L)
                        .requestId("123")
                        .build());

        verify(startrekService, times(1)).createTicket(argumentCaptor.capture());

        IssueCreate value = argumentCaptor.getValue();

        assertEquals(task.getValues(), value.getValues());
        assertEquals(task.getAttachments(), value.getAttachments());
        assertEquals(task.getLinks(), value.getLinks());
        assertEquals(task.getComment(), value.getComment());
        assertEquals(task.getValues(), value.getValues());
    }

    @Test
    void createTicketForDocumentFfcOperatorPrt() {
        IssueCreate task = IssueCreate.builder()
                .queue("OUTSTAFFCHECK")
                .summary("жора жорочкин жоревич")
                .set("61fa9c2bb1257406398d663f--dolznost", "Оператор ПРТ")
                .set("61fa9c2bb1257406398d663f--ElektrobezopasnostiDo", "2022-05-27")
                .set("61fa9c2bb1257406398d663f--gigieniceskaaAttestaciaDo", "2022-05-27")
                .type("checkUp")
                .description("https://lms-admin.tst.market.yandex-team" +
                        ".ru/hrms/external/partner/person?partnerId=1&personId=2\n" +
                        "\n" +
                        "Тип документа: Заключение о психоосвидетельствовании\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n" +
                        "\n" +
                        "Тип документа: Заключение о прохождении гигиенической аттестации\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n" +
                        "\n" +
                        "Тип документа: Права на трактор\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n" +
                        "\n" +
                        "Тип документа: Удостоверение электробезопасности\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n" +
                        "\n" +
                        "Тип документа: Удостоверение по охране труда\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n")
                .set("61fa9c2bb1257406398d663f--udostovereniePoOhraneTrudaDo", "2022-05-27")
                .set("61fa9c2bb1257406398d663f--psihOsvidetelstvovanie", "2022-05-27")
                .set("61fa9c2bb1257406398d663f--plosadka", "ФФЦ Софьино")
                .set("61fa9c2bb1257406398d663f--dataOkoncaniaSrokaDejstviaPrav", "2022-05-27")
                .set("61fa9c2bb1257406398d663f--autsorsKompania", "Company #test-login")
                .set("61fa9c2bb1257406398d663f--region", "регион")
                .build();

        Issue issue = new Issue(null, null, "123", "", 1, DefaultMapF.wrap(Map.of("", "")), null);

        StartrekTicket startrekTicket = new StartrekTicket(issue, null,
                null);
        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(startrekTicket);

        outstaffDocVerificationQueueConsumer.processPayload(
                DocumentVerificationPayload.builder()
                        .outstaffV2id(2L)
                        .requestId("123")
                        .build());

        verify(startrekService, times(1)).createTicket(argumentCaptor.capture());

        IssueCreate value = argumentCaptor.getValue();

        assertEquals(task.getAttachments(), value.getAttachments());
        assertEquals(task.getValues(), value.getValues());
        assertEquals(task.getLinks(), value.getLinks());
        assertEquals(task.getComment(), value.getComment());
        assertEquals(task.getWorklog(), value.getWorklog());
    }

    @Test
    void createTicketForDocumentFfcStockMan() {
        IssueCreate task = IssueCreate.builder()
                .queue("OUTSTAFFCHECK")
                .summary("жора жорочкин жоревич")
                .set("61fa9c2bb1257406398d663f--dolznost", "Кладовщик")
                .set("61fa9c2bb1257406398d663f--gigieniceskaaAttestaciaDo", "2022-05-27")
                .type("checkUp")
                .description("https://lms-admin.tst.market.yandex-team" +
                        ".ru/hrms/external/partner/person?partnerId=1&personId=3\n" +
                        "\n" +
                        "Тип документа: Заключение о психоосвидетельствовании\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n" +
                        "\n" +
                        "Тип документа: Заключение о прохождении гигиенической аттестации\n" +
                        "Ссылка: /admin/hrms/external/partner/download-persons-doc?partnerId=1&docLink=www.url.com\n" +
                        "Действителен с 2022-05-27 по 2022-05-27\n")
                .set("61fa9c2bb1257406398d663f--psihOsvidetelstvovanie", "2022-05-27")
                .set("61fa9c2bb1257406398d663f--plosadka", "ФФЦ Софьино")
                .set("61fa9c2bb1257406398d663f--autsorsKompania", "Company #test-login")
                .set("61fa9c2bb1257406398d663f--region", "регион")
                .build();

        Issue issue = new Issue(null, null, "123", "", 1, DefaultMapF.wrap(Map.of("", "")), null);

        StartrekTicket startrekTicket = new StartrekTicket(issue, null,
                null);
        when(startrekService.createTicket(any(IssueCreate.class))).thenReturn(startrekTicket);

        outstaffDocVerificationQueueConsumer.processPayload(
                DocumentVerificationPayload.builder()
                        .outstaffV2id(3L)
                        .requestId("123")
                        .build());

        verify(startrekService, times(1)).createTicket(argumentCaptor.capture());

        IssueCreate value = argumentCaptor.getValue();

        assertEquals(task.getAttachments(), value.getAttachments());
        assertEquals(task.getLinks(), value.getLinks());
        assertEquals(task.getComment(), value.getComment());
        assertEquals(task.getWorklog(), value.getWorklog());
        assertEquals(task.getValues(), value.getValues());
    }
}
