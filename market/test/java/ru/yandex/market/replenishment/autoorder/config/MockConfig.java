package ru.yandex.market.replenishment.autoorder.config;

import java.time.LocalDateTime;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.deepmind.client.api.AvailabilitiesApi;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.client.FfwfApiClient;
import ru.yandex.market.replenishment.autoorder.service.client.TMClient;
import ru.yandex.market.replenishment.autoorder.utils.OsChecker;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;

@Configuration
public class MockConfig {

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return Mockito.mock(LMSClient.class);
    }

    @Bean
    public static PropertyPlaceholderConfigurer propertyConfigurer() {
        return new UnitTestPlaceholderConfigurer();
    }

    @Bean
    public TmsMonitoringService tmsMonitoringService() {
        return Mockito.mock(TmsMonitoringService.class);
    }

    @Bean
    public AmazonS3 s3Client() {
        return Mockito.mock(AmazonS3.class);
    }

    @Bean
    public MboMappingsService mboMappingsService() {
        return Mockito.mock(MboMappingsService.class);
    }

    @Bean
    @Qualifier("axRestTemplate")
    public RestTemplate axRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    @Primary
    @Bean
    public TimeService timeService() {
        final TimeService timeService = Mockito.mock(TimeService.class);
        TestUtils.mockTimeService(timeService, LocalDateTime.now());
        return timeService;
    }

    @Primary
    @Bean
    public FfwfApiClient ffwfApiClient() {
        return Mockito.mock(FfwfApiClient.class);
    }

    @Primary
    @Bean
    public WorkbookConfiguration workbookConfiguration() {
        final WorkbookConfiguration workbookConfiguration = Mockito.mock(WorkbookConfiguration.class);
        when(workbookConfiguration.getEmptyWorkbook()).thenAnswer(invocation -> (OsChecker.getOsType().equals("linux"))
            ? new XSSFWorkbook()
            : new SXSSFWorkbook()
        );
        Mockito.doAnswer(invocation -> TestUtils.mockWorkbook(invocation.getArgument(0)))
            .when(workbookConfiguration).getWorkbook(Mockito.any());
        Mockito.doAnswer(invocation -> TestUtils.mockWorkbook(invocation.getArgument(0)))
            .when(workbookConfiguration).getWorkbook(Mockito.any(), Mockito.anyInt());
        return workbookConfiguration;
    }

    @Primary
    @Bean
    public JavaMailSender javaMailSender() {
        final JavaMailSenderImpl javaMailSender = Mockito.mock(JavaMailSenderImpl.class);
        when(javaMailSender.createMimeMessage()).thenCallRealMethod();
        Mockito.doNothing().when(javaMailSender).send(Mockito.any(SimpleMailMessage.class));
        return javaMailSender;
    }

    @Primary
    @Bean
    public TMClient tmClient() {
        return Mockito.mock(TMClient.class);
    }

    @Primary
    @Bean
    public AvailabilitiesApi deepmindAvailabilitiesApiClient() {
        return Mockito.mock(AvailabilitiesApi.class);
    }
}
