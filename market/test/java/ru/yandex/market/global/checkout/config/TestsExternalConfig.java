package ru.yandex.market.global.checkout.config;

import java.time.Instant;
import java.util.List;

import javax.annotation.PostConstruct;

import com.amazonaws.services.s3.AmazonS3;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.global.checkout.domain.client.trust.TrustPlusClient;
import ru.yandex.market.global.checkout.domain.delivery.DeliveryTariffService;
import ru.yandex.market.global.checkout.domain.shop.ShopQueryService;
import ru.yandex.market.global.common.datacamp.DataCampClient;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.TrustClientProperties;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.mj.generated.client.blackbox.api.BlackboxApiClient;
import ru.yandex.mj.generated.client.ezcount.api.EzcountApiClient;
import ru.yandex.mj.generated.client.sms.api.SmsApiClient;
import ru.yandex.mj.generated.client.taxi_communications_scenario.api.TaxiCommunicationsScenarioApiClient;
import ru.yandex.mj.generated.client.taxi_ucommunications_bulk_push.api.TaxiUcommunicationsBulkPushApiClient;
import ru.yandex.mj.generated.client.taxi_ucommunications_push.api.TaxiUcommunicationsPushApiClient;
import ru.yandex.mj.generated.client.taxi_v1_intergration.api.EstimateApiClient;
import ru.yandex.mj.generated.client.taxi_v1_intergration.api.InternalApiClient;
import ru.yandex.mj.generated.client.taxi_v1_intergration.api.PerformerApiClient;
import ru.yandex.mj.generated.client.trust_rest_api.api.TrustRestApiApiClient;
import ru.yandex.mj.generated.client.yacc.api.YaccApiClient;
import ru.yandex.mj.generated.server.model.DeliveryTariffDto;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.startrek.client.Session;

@Configuration
@Profile({"functionalTest", "functionalTestRecipe"})
public class TestsExternalConfig {
    @Bean
    TrustClientProperties trustClientProperties() {
        return new TrustClientProperties();
    }

    @Bean
    TestClock clock() {
        return new TestClock(Instant.parse("2021-09-01T14:14:00.00Z"));
    }

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TvmClient tvmClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    DeliveryServiceClient deliveryServiceClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TrustClient trustClient;

    // Мокаем ShopQueryService а не RestHighLevelClient
    // потому что, в ShopQueryService final методы замокать нельзя
    @MockBean(reset = MockReset.NONE)
    ShopQueryService shopQueryService;

    @MockBean(reset = MockReset.NONE)
    DeliveryTariffService deliveryTariffService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    DataCampClient dataCampClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    AmazonS3 amazonS3;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    EzcountApiClient ezcountApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    EstimateApiClient estimateApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    PerformerApiClient performerApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    BlackboxApiClient blackboxApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    SmsApiClient smsApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TaxiCommunicationsScenarioApiClient taxiCommunicationsScenarioApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TaxiUcommunicationsBulkPushApiClient taxiUcommunicationsBulkPushApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TaxiUcommunicationsPushApiClient taxiUcommunicationsPushApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    DictionaryQueryService<OfferDto> offersDictionary;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    InternalApiClient internalApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    ru.yandex.mj.generated.client.taxi_v1_intergration.api.ClaimsApiClient claimsApiV1Client;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    ru.yandex.mj.generated.client.taxi_v2_intergration.api.ClaimsApiClient claimsApiV2Client;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    TrustRestApiApiClient trustRestApiApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    YaccApiClient yaccApiClient;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "startrekSession")
    Session startrekSession;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "internalTrackerSession")
    Session internalTrackerSession;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS, name = "externalTrackerSession")
    Session externalTrackerSession;

    @PostConstruct
    private void setup() {
        Mockito.when(deliveryTariffService.getTariffs())
                .thenReturn(List.of(
                        new DeliveryTariffDto().id(1L)
                                .defaultDistance(1000L)
                                .cost(1000L),
                        new DeliveryTariffDto().id(2L)
                                .defaultDistance(1000000L)
                                .cost(1600L)
                ));
    }

    @Bean
    TrustPlusClient trustPlusClient() {
        return Mockito.mock(TrustPlusClient.class, Mockito.RETURNS_DEEP_STUBS);
    }

}
