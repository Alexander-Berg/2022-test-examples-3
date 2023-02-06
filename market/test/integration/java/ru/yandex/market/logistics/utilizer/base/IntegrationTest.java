package ru.yandex.market.logistics.utilizer.base;

import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import NMarketIndexer.Common.Common;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.utilizer.client.datacamp.DataCampClient;
import ru.yandex.market.logistics.utilizer.service.StartrekService;
import ru.yandex.market.logistics.utilizer.service.lms.LmsService;
import ru.yandex.market.logistics.utilizer.service.mds.MdsS3Service;
import ru.yandex.market.logistics.utilizer.util.ResettableSequenceStyleGenerator;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;

import static ru.yandex.market.logistics.utilizer.service.datacamp.DataCampService.DATA_CAMP_PRICE_COEFFICIENT;

@WebAppConfiguration
@AutoConfigureMockMvc
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        ResetDatabaseTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class
})
@ActiveProfiles({
        ActivateEmbeddedPg.EMBEDDED_PG,
})
@CleanDatabase
@DbUnitConfiguration(
        dataSetLoader = NullableColumnsDataSetLoader.class
)
@TestPropertySource("classpath:application-test.properties")
public abstract class IntegrationTest extends SoftAssertionSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    @Qualifier("ssLogbrokerReader")
    protected LogbrokerReader logbrokerReader;

    @Autowired
    protected FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected MdsS3Service mdsS3Service;

    @Autowired
    protected DeliveryParams deliveryParams;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected LmsService lmsService;

    @Autowired
    protected DataCampClient dataCampClient;

    @Autowired
    protected StartrekService startrekService;

    @BeforeEach
    public void resetSequences() {
        ResettableSequenceStyleGenerator.resetAllInstances();
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(
                logbrokerReader,
                fulfillmentWorkflowClientApi,
                mbiApiClient,
                deliveryParams,
                lmsClient,
                dataCampClient,
                startrekService
        );

        Mockito.clearInvocations(mdsS3Service);
    }

    @AfterEach
    public void invalidateCache() {
        lmsService.invalidateCache();
    }

    protected void runInExternalTransaction(Runnable runnable, boolean expectException) {
        transactionTemplate.execute(status -> {
            try {
                runnable.run();
                softly.assertThat(expectException).isFalse();
            } catch (Exception e) {
                e.printStackTrace();
                softly.assertThat(expectException).isTrue();
            }
            return null;
        });
    }

    protected SyncChangeOffer.FullOfferResponse createOfferResponse(Map<String, Long> prices) {
        SyncChangeOffer.FullOfferResponse.Builder builder = SyncChangeOffer.FullOfferResponse.newBuilder();
        prices.forEach((sku, price) -> {
            DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder().setOfferId(sku).build())
                    .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                            .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                    .setBinaryPrice(Common.PriceExpression.newBuilder()
                                            .setPrice(price * DATA_CAMP_PRICE_COEFFICIENT).build())
                                    .build())
                            .build())
                    .build();
            builder.addOffer(offer);
        });
        return builder.build();
    }
}
