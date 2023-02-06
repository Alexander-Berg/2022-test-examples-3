package ru.yandex.market.crm.core.services.email;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.core.domain.messages.CampaignInfo;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.sending.conf.InfoBlockConf;
import ru.yandex.market.crm.core.exceptions.UidIsNotAllowedException;
import ru.yandex.market.crm.core.services.communication.CommunicationRestrictionService;
import ru.yandex.market.crm.core.services.communication.CommunicationRestrictionStrategy;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderApiClient;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderResponse;
import ru.yandex.market.crm.core.services.logging.LogSource;
import ru.yandex.market.crm.core.services.logging.SentLogService;
import ru.yandex.market.crm.core.services.messages.ParametrizedBlockDataFactory;
import ru.yandex.market.crm.core.services.messages.StringTemplator;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.core.services.phone.PhoneConfig;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.core.services.sending.bannerstrategy.BannerEnricher;
import ru.yandex.market.crm.core.services.staff.StaffConfig;
import ru.yandex.market.crm.core.services.staff.StaffService;
import ru.yandex.market.crm.core.services.staff.StaffTvmTicketProvider;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockState;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.InfoBlockData;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmailMessageSenderTest.TestConfiguration.class)
@TestPropertySource("/core_test.properties")
public class EmailMessageSenderTest {

    private static final String STAFF_EMAIL = "staff-mcrm-test@yandex.ru";
    private static final String NOT_STAFF_EMAIL = "not-staff-mcrm-test@yandex.ru";

    @Inject
    private StaffService staffService;

    @Inject
    private EnvironmentResolver environmentResolver;

    @Inject
    private BannerEnricher bannerEnricher;

    @Inject
    private ParametrizedBlockDataFactory blockDataFactory;

    @Inject
    private StringTemplator templator;

    @Inject
    private YaSenderApiClient yaSenderApiClient;

    @Inject
    private SentLogService sentLogService;

    @Inject
    private List<CommunicationRestrictionStrategy> strategies;

    private static Stream<Arguments> dataForTest() {
        return Stream.of(
                arguments("sendEmailForStaffOnTesting", Environment.TESTING, STAFF_EMAIL, true),
                arguments("sendEmailForNotStaffOnTesting", Environment.TESTING, NOT_STAFF_EMAIL, false),
                arguments("sendEmailForStaffOnProd", Environment.PRODUCTION, STAFF_EMAIL, true),
                arguments("sendEmailForNotStaffOnProd", Environment.PRODUCTION, NOT_STAFF_EMAIL, true)
        );
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(staffService, yaSenderApiClient, environmentResolver);
        when(staffService.hasEmail(STAFF_EMAIL)).thenReturn(true);
        when(staffService.hasEmail(NOT_STAFF_EMAIL)).thenReturn(false);
        when(blockDataFactory.create(any(), any())).thenReturn(createInfoBlockData());
        when(yaSenderApiClient.sendTransactional(any(), any(), any(), eq(false), any()))
                .thenReturn(createYaSenderResponse());
    }

    @MethodSource("dataForTest")
    @ParameterizedTest(name = "{0}")
    public void testSendEmail(String name,
                              Environment environment,
                              String email,
                              boolean success) {
        when(environmentResolver.get()).thenReturn(environment);
        var communicationRestrictionService = new CommunicationRestrictionService(environmentResolver, strategies);
        var emailMessageSender = new EmailMessageSender(
                bannerEnricher,
                blockDataFactory,
                templator,
                yaSenderApiClient,
                sentLogService,
                communicationRestrictionService
        );

        if (success) {
            send(emailMessageSender, email);
            verify(yaSenderApiClient, times(1)).sendTransactional(any(), eq(email), any(), eq(false), any());
        } else {
            assertThrows(
                    UidIsNotAllowedException.class,
                    () -> send(emailMessageSender, email)
            );
            verify(yaSenderApiClient, never()).sendTransactional(any(), any(), any(), eq(false), any());
        }
    }

    private void send(EmailMessageSender emailMessageSender, String email) {
        emailMessageSender.send(
                createEmailMessageConf(),
                new CampaignInfo(),
                Map.of(),
                List.of(),
                email,
                "",
                UtmLinks.forEmailTrigger("send-test"),
                LogSource.NOOP
        );
    }

    private EmailMessageConf createEmailMessageConf() {
        var emailMessageConf = new EmailMessageConf();
        var blockConf = new InfoBlockConf();
        emailMessageConf.setBlocks(List.of(blockConf));
        return emailMessageConf;
    }

    private BlockData createInfoBlockData() {
        var blockData = new InfoBlockData();
        blockData.setState(BlockState.COMPLETED);
        return blockData;
    }

    private YaSenderResponse createYaSenderResponse() {
        var yaSenderResponse = new YaSenderResponse();
        var result = new YaSenderResponse.Result();
        result.setMessageId(Randoms.string());
        yaSenderResponse.setResult(result);
        return yaSenderResponse;
    }

    @Import({
            StaffConfig.class,
            PhoneConfig.class
    })
    static class TestConfiguration {

        @Bean
        @Primary
        public StaffService staffService() {
            return Mockito.mock(StaffService.class);
        }

        @Bean
        public BannerEnricher bannerEnricher() {
            return Mockito.mock(BannerEnricher.class);
        }

        @Bean
        public ParametrizedBlockDataFactory parametrizedBlockDataFactory() {
            return Mockito.mock(ParametrizedBlockDataFactory.class);
        }

        @Bean
        public StringTemplator stringTemplator() {
            return Mockito.mock(StringTemplator.class);
        }

        @Bean
        public YaSenderApiClient yaSenderApiClient() {
            return Mockito.mock(YaSenderApiClient.class);
        }

        @Bean
        public SentLogService sentLogService() {
            return Mockito.mock(SentLogService.class);
        }

        @Bean
        public EnvironmentResolver environmentResolver() {
            return Mockito.mock(EnvironmentResolver.class);
        }

        @Bean
        public StaffTvmTicketProvider staffTvmTicketProvider() {
            return Mockito.mock(StaffTvmTicketProvider.class);
        }

        @Bean
        public TemplateService templateService() {
            return new TemplateService(
                    new Properties(),
                    Collections.emptyMap(),
                    1
            );
        }

        @Bean
        public PersonalService personalService() {
            return Mockito.mock(PersonalService.class);
        }
    }
}
