package ru.yandex.market.global.partner;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import retrofit2.mock.Calls;

import ru.yandex.market.common.retrofit.RetrofitUtils;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.dto.CreatePartnerResponse;
import ru.yandex.market.global.common.trust.client.dto.CreateProductResponse;
import ru.yandex.market.global.partner.config.InternalConfig;
import ru.yandex.market.global.partner.test.config.TestsConfig;
import ru.yandex.market.global.partner.test.config.TestsExternalConfig;
import ru.yandex.market.global.partner.util.TestDataService;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.mj.generated.client.ezcount.api.EzcountApiClient;
import ru.yandex.mj.generated.client.ezcount.model.CreateUserResponse;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.BusinessRegistrationApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.PartnerRegistrationApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.BusinessRegistration;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.BusinessRegistrationResponse;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerRegistration;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerRegistrationResponse;

import static ru.yandex.market.request.trace.Module.GLOBAL_MARKET_PARTNER;
import static ru.yandex.market.request.trace.Module.MBI_PARTNER_REGISTRATION;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                InternalConfig.class,
                SpringApplicationConfig.class,
                TestsConfig.class,
                TestsExternalConfig.class
        }
)
@TestPropertySource({"classpath:test_properties/test.properties"})
@ActiveProfiles("functionalTest")
@Slf4j
public abstract class BaseFunctionalTest {
    private static final long PARTNER_ID = 5L;
    private static final long BUSINESS_ID = 6L;
    private static final long CAMPAIGN_ID = 123L;
    public static final String EZCOUNT_MOCKED_API_KEY = "ezcount-some-api-key";
    public static final String EZCOUNT_MOCKED_REF_TOKEN = "ezcount-some-refresh-token";
    public static final String TRUST_MOCKED_PARTNER_ID = "trust_partner_id";

    @Autowired
    protected PartnerRegistrationApiClient partnerRegistrationApiClient;

    @Autowired
    protected BusinessRegistrationApiClient businessRegistrationApiClient;

    @Autowired
    protected EzcountApiClient ezcountApiClient;

    @Autowired
    protected TrustClient trustClient;

    @Autowired
    protected TestDataService testDataService;

    protected TestDataService.TestData testData;

    @BeforeEach
    public void setup() {
        prepareTestData();
        mockExternalCalls();
    }

    private void prepareTestData() {
        testDataService.cleanTestData();
        testData = testDataService.saveTestData();
    }


    @SneakyThrows
    private void mockExternalCalls() {
        Mockito.when(partnerRegistrationApiClient.registerPartner(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(RetrofitUtils.caller(Calls.response(new PartnerRegistrationResponse()
                        .result(new PartnerRegistration()
                                .partnerId(PARTNER_ID)
                                .businessId(BUSINESS_ID)
                                .campaignId(CAMPAIGN_ID)
                        )
                ), log, RetryStrategy.NO_RETRY_STRATEGY, GLOBAL_MARKET_PARTNER, MBI_PARTNER_REGISTRATION));

        Mockito.when(businessRegistrationApiClient.registerBusiness(Mockito.anyLong(), Mockito.any()))
                .thenReturn(RetrofitUtils.caller(Calls.response(new BusinessRegistrationResponse()
                        .result(new BusinessRegistration()
                                .businessId(BUSINESS_ID)
                        )
                ), log, RetryStrategy.NO_RETRY_STRATEGY, GLOBAL_MARKET_PARTNER, MBI_PARTNER_REGISTRATION));

        Mockito.when(ezcountApiClient.apiUserCreatePost(Mockito.any()))
                .thenReturn(RetrofitUtils.caller(Calls.response(new CreateUserResponse()
                        .uApiKey(EZCOUNT_MOCKED_API_KEY)
                        .refreshToken(EZCOUNT_MOCKED_REF_TOKEN)
                        .success(true)
                ), log, RetryStrategy.NO_RETRY_STRATEGY, GLOBAL_MARKET_PARTNER, MBI_PARTNER_REGISTRATION));

        Mockito.when(trustClient.createPartner(Mockito.any()))
                .thenReturn(CreatePartnerResponse.builder().partnerId(TRUST_MOCKED_PARTNER_ID).build());

        Mockito.when(trustClient.createProduct(Mockito.any()))
                .thenReturn(CreateProductResponse.builder().build());

    }
}
