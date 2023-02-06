package ru.yandex.market.core.feature.precondition.listeners;

import java.time.Clock;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "marketplaceWorkflow.before.csv")
class TestMarketplaceWorkflowListenerFunctionalTest extends FunctionalTest {
    private static final long DATASOURCE_ID = 101L;

    @Autowired
    FeatureService featureService;

    @Autowired
    PrepayRequestService prepayRequestService;

    @Autowired
    ProtocolService protocolService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    void testExpirementCutoffWhileChangeRequestStatusWorkflow() {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());

        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());

        protocolService.operationInTransaction(
                ActionType.PREPAY_REQUEST_UPDATE,
                100500,
                "comment",
                (transactionStatus, actionId) -> {
                    ShopFeature feature;

                    //SuperAdminLink superAdminLink = superAdminService.getSuperAdminLinkByClient(campaignInfo.getClientId());
                    prepayRequestService.updateRequestStatus(DATASOURCE_ID, PartnerApplicationStatus.COMPLETED,
                            null, "ololo", null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.SUCCESS);

                    prepayRequestService.updateRequestStatus(DATASOURCE_ID,
                            PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_REQUIRED, null, "ololo",
                            null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.FAIL);

                    prepayRequestService.updateRequestStatus(DATASOURCE_ID, PartnerApplicationStatus.COMPLETED,
                            null, "ololo", null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.SUCCESS);

                    featureService.startExperiment(DATASOURCE_ID, FeatureType.MARKETPLACE, actionId);
                    prepayRequestService.updateRequestStatus(DATASOURCE_ID,
                            PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_REQUIRED, null, "ololo",
                            null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.FAIL);

                    prepayRequestService.updateRequestStatus(DATASOURCE_ID, PartnerApplicationStatus.COMPLETED,
                            null, "ololo", null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.SUCCESS);

                    prepayRequestService.updateRequestStatus(DATASOURCE_ID, PartnerApplicationStatus.NEED_INFO,
                            null, "ololo", null, 123L, actionId);
                    feature = featureService.getFeature(DATASOURCE_ID, FeatureType.MARKETPLACE);
                    assertThat(feature.getStatus()).isEqualTo(ParamCheckStatus.FAIL);
                });
    }
}
