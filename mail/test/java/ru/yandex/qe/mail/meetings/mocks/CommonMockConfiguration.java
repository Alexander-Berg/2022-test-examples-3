package ru.yandex.qe.mail.meetings.mocks;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.codahale.metrics.MetricRegistry;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.sun.mail.smtp.SMTPMessage;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import ru.yandex.qe.mail.meetings.rooms.dao.SentEmailsDao;
import ru.yandex.qe.mail.meetings.services.gaps.GapApi;
import ru.yandex.qe.mail.meetings.services.staff.MockStaff;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
@ImportResource("classpath*:spring/template-ctx.xml")
public class CommonMockConfiguration {

    public static final SendAnswer SEND_ANSWER = new SendAnswer();

    @Bean
    public StaffClient staffClient() {
        return new StaffClient(new MockStaff(MockStaff.DISMISSED));
    }

    @Bean
    public GapApi gapApi() {
        return mock(GapApi.class);
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return mock(MetricRegistry.class);
    }

    @Bean
    public JavaMailSender mailSender(SendAnswer sendAnswer) {
        JavaMailSender sender = mock(JavaMailSender.class);
        doAnswer(sendAnswer).when(sender).send(any(MimeMessagePreparator.class));
        doAnswer(sendAnswer).when(sender).send(any(MimeMessagePreparator.class), any(MimeMessagePreparator.class));
        return sender;
    }

    @Bean
    public SendAnswer sendAnswer() {
        return SEND_ANSWER;
    }

    public static class SendAnswer implements Answer<Void> {
        public MimeMessage message;
        public int invocations;
        @Override
        public Void answer(InvocationOnMock invocation) {
            message = new SMTPMessage((Session) null);
            invocations++;
            try {
                ((MimeMessagePreparator) invocation.getArguments()[0]).prepare(message);
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                fail(e.getMessage() + "\n" + writer.toString());
            }
            return null;
        }
        public void clear() {
            message = null;
            invocations = 0;
        }
    }

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres postgres() throws Exception {
        return EmbeddedPostgres.start();
    }

    @Bean
    public SentEmailsDao sentEmailsDao(EmbeddedPostgres ps) throws Exception {
        FlywayPreparer flywayPreparer = FlywayPreparer.forClasspathLocation("db/migration");
        flywayPreparer.prepare(ps.getPostgresDatabase());
        return new SentEmailsDao(ps.getPostgresDatabase());
    }
}
