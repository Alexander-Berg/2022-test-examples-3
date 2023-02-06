package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTestConfig;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.module.comment.CommentsService;
import ru.yandex.market.jmf.module.comment.PublicComment;
import ru.yandex.market.jmf.module.mail.SendMailService;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@B2bTicketTests
@ContextConfiguration(classes = {B2bTicketTestConfig.class, B2bTicketCommentTest.B2bCommentTestConfig.class})
public class B2bTicketCommentTest {
    @Inject
    private CommentsService commentsService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;

    @Inject
    private BcpService bcpService;

    @Inject
    private SendMailService mailService;

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(mailService);
    }

    @Test
    @Description("""
            Если обращение 'содержит приватные данные',
            то при добавлении комментария с темой, новая тема подставляется в title,
            а в ответном письме к новому title добавляется номер обращения
            https://testpalm.yandex-team.ru/testcase/ocrm-1368
            """)
    public void shouldChangeSubjectOnPublicCommentIfTicketContainsPrivateData() {
        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.TITLE, "original title",
                B2bTicket.CONTAINS_PRIVATE_DATA, true
        ));
        createCommentAndCheckSubject(ticket);
    }

    @Test
    @Description("""
            Если обращение находится в очереди, которая позволяет 'изменять тему в ответах на обращения',
            то при добавлении комментария с темой, новая тема подставляется в title,
            а в ответном письме к новому title добавляется номер обращения
            https://testpalm.yandex-team.ru/testcase/ocrm-1368
            """)
    public void shouldChangeSubjectOnPublicCommentIfServiceAllowIt() {
        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.TITLE, "original title"
        ));
        bcpService.edit(ticket.getService(), "allowChangeCommentSubject", true);
        createCommentAndCheckSubject(ticket);
    }

    @Test
    @Description("""
            Изменение темы чз комментарий не влияет на изменение title в обращениях "B2B - Лид"
            https://testpalm.yandex-team.ru/testcase/ocrm-1512
            """)
    public void commentSubjectShouldNotAffectB2bLeadTitle() {
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(
                B2bTicket.TITLE, "original title",
                B2bTicket.CONTAINS_PRIVATE_DATA, true
        ));
        String commentSubject = "updated title";
        createPublicComment(ticket, commentSubject);
        checkMailSentWithSubject(ticket, commentSubject, true);
        EntityAssert.assertThat(ticket)
                .hasAttributes(
                        B2bTicket.TITLE, "original title",
                        B2bTicket.MAIL_SUBJECT, commentSubject
                );
    }

    @Test
    @Description("""
            Если на очереди включена настройа "Скрывать идентификатор обращения в ответном письме"
            то письмо отправляется без номера тикета в теме""")
    public void shouldSendMailWithoutTicketIdInSubject() {
        String originalTitle = "original title";
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(B2bTicket.TITLE, originalTitle));
        bcpService.edit(ticket.getService(), "hideTicketIdInMailSubject", true);
        commentsService.create(ticket.getGid(), Randoms.string(), PublicComment.FQN, Map.of());
        checkMailSentWithSubject(ticket, originalTitle, false);
    }

    @Test
    @Description("""
            Если на очереди включена настройа "Скрывать идентификатор обращения в ответном письме"
            то письмо отправляется без номера тикета в теме
            даже если тема меняется чз внешний комментарий
            """)
    public void shouldSendMailWithoutTicketIdInUpdatedSubject() {
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(
                B2bTicket.TITLE, "original title",
                B2bTicket.CONTAINS_PRIVATE_DATA, true
        ));
        bcpService.edit(ticket.getService(), "hideTicketIdInMailSubject", true);
        String commentSubject = "updated title";
        createPublicComment(ticket, commentSubject);
        checkMailSentWithSubject(ticket, commentSubject, false);
    }

    private void createCommentAndCheckSubject(B2bTicket ticket) {
        String commentSubject = "updated title";
        createPublicComment(ticket, commentSubject);
        checkMailSentWithSubject(ticket, commentSubject, true);
        assertThat(ticket.getTitle()).isEqualTo(commentSubject);
    }

    private void createPublicComment(B2bTicket ticket, String commentSubject) {
        commentsService.create(
                ticket.getGid(),
                Randoms.string(),
                PublicComment.FQN,
                Map.of(PublicComment.SUBJECT, commentSubject)
        );
    }

    private void checkMailSentWithSubject(B2bTicket ticket, String commentSubject, boolean ticketIdInSubject) {
        String expectedMailSubject = ticketIdInSubject
                ? String.format("%s, № %d", commentSubject, ticket.getId())
                : commentSubject;
        Mockito.verify(mailService, Mockito.times(1))
                .send(any(), any(), any(), any(), any(), any(), any(), eq(expectedMailSubject), any(), any(), any());
    }

    @Configuration
    public static class B2bCommentTestConfig {

        @Bean
        @Primary
        public SendMailService mailServiceMock() {
            return Mockito.mock(SendMailService.class);
        }

    }
}
