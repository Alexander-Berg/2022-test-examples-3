package ru.yandex.market.api.partner.controllers.order.v2;

import java.util.concurrent.CompletableFuture;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterEdaApi;
import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.label.metrics.LabelGenerationProtoLBEvent;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.personal_market.PersonalMarketService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public abstract class OrderControllerV2TestTemplate extends FunctionalTest implements ResourceUtilitiesMixin {

    protected static final long CAMPAIGN_ID = 10668L;
    protected static final long PARTNER_ID = 668L;
    protected static final long DROPSHIP_CAMPAIGN_ID = 1000571241L;
    protected static final long DROPSHIP_BY_SELLER_PARTNER_ID = 2001L;
    protected static final long DROPSHIP_BY_SELLER_CAMPAIGN_ID = 20001L;
    protected static final long CAMPAIGN_ID_WITH_ARCHIVED_ORDER = 1000571241L;
    protected static final long ORDER_ID = 1L;
    protected static final long ARCHIVED_ORDER_ID = 130L;
    protected static final int CLIENT_ID = 668;

    @Autowired
    protected CommunicationProxyClient communicationProxyClient;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected LogbrokerEventPublisher<LabelGenerationProtoLBEvent> logbrokerLabelGenerateEventPublisher;

    @Autowired
    protected TestableClock clock;

    @Autowired
    protected PapiOrderServiceClient papiOrderServiceClient;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    protected RestTemplate checkouterRestTemplate;

    @Value("${market.checkouter.client.url}")
    protected String checkouterUrl;

    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Autowired
    protected CheckouterEdaApi checkouterEdaApi;

    @Autowired
    protected MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    @Qualifier("ff4ShopsRestTemplate")
    protected RestTemplate ff4ShopsRestTemplate;

    @Value("${ff4shops.client.http.url:}")
    protected String ff4shopsUrl;

    @Autowired
    protected PersonalMarketService personalMarketService;

    protected CheckouterMockHelper checkouterMockHelper;

    @BeforeEach
    void setUp() {
        checkouterMockHelper = new CheckouterMockHelper(
                checkouterRestTemplate,
                checkouterUrl
        );
        when(logbrokerLabelGenerateEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    protected void prepareCheckouterMock(String bodyPath, long orderId, long clientId) {
        checkouterMockHelper.mockGetOrderReturnsBody(orderId, clientId, resourceAsString(bodyPath));
    }

    protected void prepareNotFoundCheckouterMock(long orderId, long clientId) {
        checkouterMockHelper.mockGetOrder(orderId, clientId, withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        //language=json
                        "{\n" +
                                "  \"message\": \"Order not found: " + orderId + "\",\n" +
                                "  \"code\": \"ORDER_NOT_FOUND\",\n" +
                                "  \"status\": 404\n" +
                                "}")
        );
    }

    protected void assertResponse(String analysedBody, String expectedContentFileName, Format format) {
        if (Format.JSON.equals(format)) {
            MbiAsserts.assertJsonEquals(resourceAsString(expectedContentFileName), analysedBody);
        } else {
            MbiAsserts.assertXmlEquals(resourceAsString(expectedContentFileName), analysedBody);
        }
    }

    protected void mockMarketId(long partnerId, String legalName) {
        MarketIdPartner partner = MarketIdPartner.newBuilder()
                .setPartnerId(partnerId)
                .setPartnerType(CampaignType.YADELIVERY.getId())
                .build();
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder().setPartner(partner).build();
        // mock ответа MarketID на запрос о ид поставщика
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(legalName)
                            .build()
            ).build();
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request), any());
    }

}
