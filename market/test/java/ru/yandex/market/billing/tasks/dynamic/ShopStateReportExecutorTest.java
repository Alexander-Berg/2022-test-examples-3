package ru.yandex.market.billing.tasks.dynamic;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.DirectHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.NamedHistoryMdsS3ClientImpl;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.config.FunctionalTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.delivery.service.billing.DeliveryBalanceOrderService;
import ru.yandex.market.core.util.spring.HideFromComponentScan;
import ru.yandex.market.dynamic.DbMarketDynamicService;
import ru.yandex.market.dynamic.DisabledOffersDataProvider;
import ru.yandex.market.dynamic.DynamicGenerationStatus;
import ru.yandex.market.dynamic.DynamicLogEvent;
import ru.yandex.market.dynamic.DynamicLogEventPublisher;
import ru.yandex.market.dynamic.FileGenerator;
import ru.yandex.market.dynamic.TSVFileGenerator;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.request.trace.Module;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = ShopStateReportExecutorTest.Config.class)
@PreserveDictionariesDbUnitDataSet
class ShopStateReportExecutorTest extends JupiterDbUnitTest {
    @Autowired
    private DbMarketDynamicService marketDynamicService;

    @Autowired
    private LogbrokerEventPublisher<DynamicLogEvent> dynamicLogEventLogbrokerEventPublisher;

    @BeforeEach
    void setUp() {
        when(dynamicLogEventLogbrokerEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    @DbUnitDataSet(
            before = "shopStateReportExecutorTest.before.csv"
    )
    void generateInDb() {
        // when
        marketDynamicService.makeNewGenerationInTransaction();

        // then
        var eventsCaptor = ArgumentCaptor.forClass(DynamicLogEvent.class);
        verify(dynamicLogEventLogbrokerEventPublisher, atLeastOnce()).publishEventAsync(eventsCaptor.capture());
        assertThat(eventsCaptor.getAllValues()).containsExactlyInAnyOrder(
                new DynamicLogEvent(1, DynamicGenerationStatus.PHONE_VISIBILITY_GENERATION, "222666")
        );
    }

    @Configuration
    @HideFromComponentScan
    @PropertySource({
            "classpath:servant.properties",
            "classpath:ru/yandex/market/billing/tasks/dynamic/ShopStateReportExecutorTest.properties"
    })
    @ImportResource({
            "classpath:billing/market-dynamic.xml",
            "classpath:ru/yandex/market/billing/tasks/dynamic/dynamic-test-config.xml"
    })
    @Import({
            EmbeddedPostgresConfig.class,
            FunctionalTestConfig.class,
    })
    static class Config {
        static {
            System.setProperty("simple.host.name", "localhost");
        }

        @Autowired
        private DynamicLogEventPublisher dynamicLogEventPublisher;


        @Bean
        Module sourceModule() {
            return Module.MBI_BILLING;
        }

        @Bean
        public DeliveryBalanceOrderService deliveryBalanceOrderService() {
            return mock(DeliveryBalanceOrderService.class);
        }

        @Bean
        public DataSource shopDataSource(DataSource dataSource) {
            return dataSource;
        }

        @Bean
        public DirectHistoryMdsS3Client aboDirectHistoryMdsS3Client() {
            var client = mock(DirectHistoryMdsS3Client.class);
            when(client.downloadLast(any(), any())).thenAnswer(i -> {
                // скрытые офферы из або (mds)
                var data = "7bfd8496287e1b59bdc4f4eb63efe310\n" +
                        "8b1e197a796ef19803be59574ee51b4d\n" +
                        "#1242557a796ef19803be59574ee51b4d\n" +
                        "#1234\n";
                ContentConsumer<?> destination = i.getArgument(2);
                return destination.consume(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
            });
            return client;
        }

        @Bean
        public FileGenerator disabledOffersFileGenerator(
                TransactionTemplate transactionTemplate
        ) {
            return new TSVFileGenerator(
                    "offer-filter.db",
                    new DisabledOffersDataProvider(
                            transactionTemplate,
                            dynamicLogEventPublisher
                    ));
        }

        @Bean
        public NamedHistoryMdsS3Client namedHistoryMdsS3Client() throws MalformedURLException {
            NamedHistoryMdsS3Client mock = mock(NamedHistoryMdsS3ClientImpl.class);
            var fakeLocation = ResourceLocation.create("fake_bucket", "fake_key");
            when(mock.upload(anyString(), any())).thenReturn(fakeLocation);
            when(mock.getUrl(fakeLocation)).thenReturn(new URL("http://fake.url"));
            return mock;
        }

        @Bean("ticketParserTvmClient")
        public TvmClient tvmClient() {
            return mock(TvmClient.class);
        }
    }
}
