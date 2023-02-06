package ru.yandex.market.arbiter.test.config;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.arbiter.api.ArbiterControllerAdvice;
import ru.yandex.market.arbiter.config.ControllersConfig;
import ru.yandex.market.arbiter.config.OpenApiConfig;
import ru.yandex.market.arbiter.config.RepositoryConfig;
import ru.yandex.market.arbiter.config.ServicesConfig;
import ru.yandex.market.arbiter.config.TmsTasksConfig;
import ru.yandex.market.arbiter.jpa.repository.AttachmentRepository;
import ru.yandex.market.arbiter.jpa.repository.AuditRepository;
import ru.yandex.market.arbiter.jpa.repository.ConversationRepository;
import ru.yandex.market.arbiter.jpa.repository.MerchantRepository;
import ru.yandex.market.arbiter.jpa.repository.MessageRepository;
import ru.yandex.market.arbiter.jpa.repository.NotificationChannelRepository;
import ru.yandex.market.arbiter.jpa.repository.NotificationRepository;
import ru.yandex.market.arbiter.jpa.repository.SubjectRepository;
import ru.yandex.market.arbiter.jpa.repository.VerdictRepository;
import ru.yandex.market.arbiter.jpa.repository.WaitingRepository;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.TestMapper;
import ru.yandex.market.arbiter.test.util.TestClock;
import ru.yandex.market.arbiter.workflow.Workflow;

/**
 * @author moskovkin@yandex-team.ru
 * @since 14.05.2020
 */
@Import({
        MockConfig.class,
        ServicesConfig.class,
        RepositoryConfig.class,
        TmsTasksConfig.class,
        ControllersConfig.class,
        ArbiterControllerAdvice.class,
        ApiClientConfig.class,
        OpenApiConfig.class
})
@SpringBootApplication(exclude={
        MongoAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        XADataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@WebAppConfiguration
@PropertySource("classpath:test.properties")
public class TestApplicationConfig {
    @Bean
    public JettyServletWebServerFactory testJettyServletWebServerFactory() {
        return new JettyServletWebServerFactory();
    }

    @Bean
    public TestClock clock() {
        return TestClock.INSTANCE;
    }

    @Bean
    public TestDataService testDataService(
            AttachmentRepository attachmentRepository,
            MessageRepository messageRepository,
            SubjectRepository subjectRepository,
            MerchantRepository merchantRepository,
            ConversationRepository conversationRepository,
            WaitingRepository waitingRepository,
            VerdictRepository verdictRepository,
            NotificationChannelRepository notificationChannelRepository,
            NotificationRepository notificationRepository,
            AuditRepository auditRepository,
            Workflow workflow
    ) {
        return new TestDataService(
                attachmentRepository,
                messageRepository,
                waitingRepository,
                verdictRepository,
                subjectRepository,
                merchantRepository,
                conversationRepository,
                notificationChannelRepository,
                notificationRepository,
                auditRepository,
                workflow
        );
    }

    @Bean
    TestMapper testMapper() {
        return Mappers.getMapper(TestMapper.class);
    }
}
