package ru.yandex.market.global.index.config;

import java.time.Instant;

import com.amazonaws.services.s3.AmazonS3;
import org.mockito.Answers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.market.global.common.datacamp.DataCampClient;
import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.index.domain.dixtionary.AttributesDictionary;
import ru.yandex.market.global.index.domain.dixtionary.CategoryEnrichmentsDictionary;
import ru.yandex.market.global.index.domain.dixtionary.MarketCategoriesDictionary;
import ru.yandex.market.global.index.domain.dixtionary.OffersDictionary;
import ru.yandex.market.global.index.domain.dixtionary.ShopEnrichmentsDictionary;
import ru.yandex.market.global.index.domain.dixtionary.ShopsDictionary;
import ru.yandex.market.starter.logbroker.config.LogbrokerInstallationsAutoConfiguration;
import ru.yandex.mj.generated.client.partner.api.ShopExportApiClient;
import ru.yandex.mj.generated.client.pim.api.PimApiClient;

@Configuration
@Profile({"functionalTest", "functionalTestRecipe"})
@EnableAutoConfiguration(exclude = {
        LogbrokerInstallationsAutoConfiguration.class
})
public class TestsExternalConfig {
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    AmazonS3 marketFrontS3Client;

    @MockBean
    // Мокаем IndexingService а не RestHighLevelClient
    // потому что, в RestHighLevelClient final методы замокать нельзя
    IndexingService indexingService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    DataCampClient dataCampClient;

    @Bean
    TestClock clock() {
        return new TestClock(Instant.parse("2021-09-22T14:14:00.00Z"));
    }

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "datacampStreamConsumer")
    StreamConsumer datacampConsumerStarter;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    AsyncProducer cleanWebLogbrokerProducer;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "cleanWebStreamConsumer")
    StreamConsumer cleanWebStreamConsumer;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    AttributesDictionary attributesDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    ShopsDictionary shopsDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    MarketCategoriesDictionary marketCategoriesDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    ShopEnrichmentsDictionary shopEnrichmentsDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    OffersDictionary offersDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "categoryEnrichmentCacheDictionary")
    CategoryEnrichmentsDictionary categoryEnrichmentCacheDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "pimCategoryEnrichmentCacheDictionary")
    CategoryEnrichmentsDictionary pimCategoryEnrichmentCacheDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    ShopExportApiClient shopExportApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    PimApiClient pimApiClient;
}
