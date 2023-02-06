package ru.yandex.market.global.partner.test.config;

import java.time.Instant;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Answers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.global.common.balance.BalanceService;
import ru.yandex.market.global.common.datacamp.DataCampClient;
import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.TrustClientProperties;
import ru.yandex.market.global.partner.domain.clients.BlackboxService;
import ru.yandex.mj.generated.client.ezcount.api.EzcountApiClient;
import ru.yandex.mj.generated.client.index.api.IndexApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.BusinessRegistrationApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.PartnerRegistrationApiClient;
import ru.yandex.startrek.client.Session;

@Slf4j
@Configuration
@Profile({"functionalTest", "functionalTestRecipe"})
public class TestsExternalConfig {
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    PartnerRegistrationApiClient partnerRegistrationApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    BusinessRegistrationApiClient businessRegistrationApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    EzcountApiClient ezcountApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    IndexApiClient indexApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    AmazonS3 marketFrontS3Client;

    @MockBean
    // Мокаем IndexingService а не RestHighLevelClient
    // потому что, в RestHighLevelClient final методы замокать нельзя
    IndexingService indexingService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TrustClient trustClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    DataCampClient dataCampClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    Session internalTrackerSession;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    BalanceService balanceService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    BlackboxService blackboxService;

    @Bean
    TrustClientProperties trustClientProperties() {
        return new TrustClientProperties();
    }

    @Bean
    TestClock clock() {
        return new TestClock(Instant.parse("2021-09-22T14:14:00.00Z"));
    }

}
