package ru.yandex.market.crm.core.services.sms;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.exceptions.UidIsNotAllowedException;
import ru.yandex.market.crm.core.services.communication.CommunicationConfig;
import ru.yandex.market.crm.core.services.communication.CommunicationRestrictionService;
import ru.yandex.market.crm.core.services.communication.CommunicationRestrictionStrategy;
import ru.yandex.market.crm.core.services.external.smspassport.SmsPassportClient;
import ru.yandex.market.crm.core.services.external.smspassport.domain.SendSmsRequestProperties;
import ru.yandex.market.crm.core.services.logging.SentLogService;
import ru.yandex.market.crm.core.services.messages.StringTemplator;
import ru.yandex.market.crm.core.services.messages.TextMessageTransformer;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.core.services.phone.PhoneConfig;
import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.core.services.staff.StaffConfig;
import ru.yandex.market.crm.core.services.staff.StaffService;
import ru.yandex.market.crm.core.services.staff.StaffTvmTicketProvider;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.mcrm.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SmsSendingServiceTest.TestConfiguration.class)
@TestPropertySource("/core_test.properties")
public class SmsSendingServiceTest {

    private static final long STAFF_PUID = 1L;
    private static final long NOT_STAFF_PUID = 2L;
    private static final String STAFF_PHONE = "79990000001";
    private static final String NOT_STAFF_PHONE = "79990000002";
    private static final String TEST_PHONE = "7 (000) 000-0000";

    @Inject
    private StaffService staffService;

    @Inject
    private EnvironmentResolver environmentResolver;

    @Inject
    private SmsPassportClient smsPassportClient;

    @Inject
    private SentLogService sentLogService;

    @Inject
    private PhoneService phoneService;

    @Inject
    private List<CommunicationRestrictionStrategy> strategies;

    @Inject
    private StringTemplator stringTemplator;

    @Inject
    private TextMessageTransformer textMessageTransformer;

    private static Stream<Arguments> dataForTest() {
        return Stream.of(
                arguments("sendSmsForStaffPuidOnTesting", Environment.TESTING, Uid.asPuid(STAFF_PUID), true),
                arguments("sendSmsForNotStaffPuidOnTesting", Environment.TESTING, Uid.asPuid(NOT_STAFF_PUID), false),
                arguments("sendSmsForStaffPhoneOnTesting", Environment.TESTING, Uid.asPhone(STAFF_PHONE), true),
                arguments("sendSmsForNotStaffPhoneOnTesting", Environment.TESTING, Uid.asPhone(NOT_STAFF_PHONE), false),
                arguments("sendSmsForTestPhoneOnTesting", Environment.TESTING, Uid.asPhone(TEST_PHONE), false),
                arguments("sendSmsForStaffPuidOnProd", Environment.PRODUCTION, Uid.asPuid(STAFF_PUID), true),
                arguments("sendSmsForNotStaffPuidOnProd", Environment.PRODUCTION, Uid.asPuid(NOT_STAFF_PUID), true),
                arguments("sendSmsForStaffPhoneOnProd", Environment.PRODUCTION, Uid.asPhone(STAFF_PHONE), true),
                arguments("sendSmsForNotStaffPhoneOnProd", Environment.PRODUCTION, Uid.asPhone(NOT_STAFF_PHONE), true),
                arguments("sendSmsForTestPhoneOnProd", Environment.PRODUCTION, Uid.asPhone(TEST_PHONE), false)
        );
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(staffService, smsPassportClient, environmentResolver);
        when(staffService.hasPuid(STAFF_PUID)).thenReturn(true);
        when(staffService.hasPuid(NOT_STAFF_PUID)).thenReturn(false);
        when(staffService.hasCommonPhone(STAFF_PHONE)).thenReturn(true);
        when(staffService.hasCommonPhone(NOT_STAFF_PHONE)).thenReturn(false);
        when(smsPassportClient.sendSms(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @MethodSource("dataForTest")
    @ParameterizedTest(name = "{0}")
    public void testSendSms(String name,
                            Environment environment,
                            Uid uid,
                            boolean success) {
        when(environmentResolver.get()).thenReturn(environment);
        var smsSendingService = createSmsSendingService();

        if (success) {
            smsSendingService.sendSms(createSmsSendingRequest(uid));
            verify(smsPassportClient, times(1)).sendSms(eq(uid), any());
        } else {
            assertThrows(
                    UidIsNotAllowedException.class,
                    () -> smsSendingService.sendSms(createSmsSendingRequest(uid))
            );
            verify(smsPassportClient, never()).sendSms(any(), any());
        }
    }

    @Test
    public void testSecretParams() {
        when(environmentResolver.get()).thenReturn(Environment.PRODUCTION);
        var smsSendingService = createSmsSendingService();

        Uid uid = Uid.asPuid(STAFF_PUID);
        smsSendingService.sendSms(new SmsSendingRequest()
                .setTextTemplate("Secret1=${param1}, Param2=${param2}, Secret3=${param3}, NullSecret5=${param5}")
                .setTemplateVariables(List.of(
                        new MessageTemplateVar("param1", MessageTemplateVar.Type.STRING, true),
                        new MessageTemplateVar("param3", MessageTemplateVar.Type.NUMBER, true),
                        new MessageTemplateVar("param2", MessageTemplateVar.Type.STRING, false),
                        new MessageTemplateVar("param4", MessageTemplateVar.Type.STRING, true),
                        new MessageTemplateVar("param5", MessageTemplateVar.Type.STRING, true)
                ))
                .setVariables(Maps.of(
                        "param1", "value1",
                        "param2", "value2",
                        "param3", 777,
                        "param4", "value4",
                        "param5", null
                ))
                .setUid(uid)
                .setRequestPropertiesBuilder(new SendSmsRequestProperties.Builder())
        );
        verify(smsPassportClient, times(1)).sendSms(
                eq(uid),
                argThat(new SendSmsRequestPropertiesMatcher(
                        "Secret1={{ param1 }}, Param2=value2, Secret3={{ param3 }}, NullSecret5=null",
                        Map.of(
                                "param1", "value1",
                                "param3", "777"
                        )
                ))
        );
    }

    private SmsSendingService createSmsSendingService() {
        var communicationRestrictionService = new CommunicationRestrictionService(environmentResolver, strategies);
        return new SmsSendingService(stringTemplator, textMessageTransformer, smsPassportClient,
                sentLogService, communicationRestrictionService, phoneService);
    }

    private SmsSendingRequest createSmsSendingRequest(Uid uid) {
        return new SmsSendingRequest()
                .setUid(uid)
                .setRequestPropertiesBuilder(new SendSmsRequestProperties.Builder());
    }

    private static class SendSmsRequestPropertiesMatcher implements ArgumentMatcher<SendSmsRequestProperties> {

        private final String smsText;
        private final Map<String, String> textTemplateParams;

        public SendSmsRequestPropertiesMatcher(String smsText, Map<String, String> textTemplateParams) {
            this.smsText = smsText;
            this.textTemplateParams = textTemplateParams;
        }

        @Override
        public boolean matches(SendSmsRequestProperties argument) {
            if (null == argument) {
                return false;
            }
            return Objects.equals(smsText, argument.getSmsText())
                    && Objects.equals(textTemplateParams, argument.getTextTemplateParams());
        }
    }

    @Import({
            StaffConfig.class,
            PhoneConfig.class,
            CommunicationConfig.class
    })
    static class TestConfiguration {

        @Bean
        @Primary
        public StaffService staffService() {
            return Mockito.mock(StaffService.class);
        }

        @Bean
        public SmsPassportClient smsPassportClient() {
            return Mockito.mock(SmsPassportClient.class);
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
        public PersonalService personalService() {
            return Mockito.mock(PersonalService.class);
        }

        @Bean
        public StringTemplator stringTemplator() {
            return new StringTemplator(new TemplateService(new Properties(), Map.of(), 256));
        }

        @Bean
        public TextMessageTransformer textMessageTransformer() {
            return Mockito.mock(TextMessageTransformer.class);
        }
    }
}
