package ru.yandex.market.supplier.command;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "SynchronizeSupplierStateCommandTest.before.csv")
public class SynchronizeSupplierStateCommandTest extends FunctionalTest {

    @Autowired
    private SynchronizeSupplierStateCommand tested;
    @Mock
    private Terminal terminal;
    @Autowired
    private FF4ShopsClient ff4ShopsClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    void testExecuteCommand() {
        mockMarketId();
        PartnerResponse partnerResponse = createDefaultPartnerResponse(115L);
        when(lmsClient.getPartner(1115L)).thenReturn(Optional.of(partnerResponse));

        CommandInvocation commandInvocation = new CommandInvocation("synchronize-supplier-state",
                new String[]{"115"},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);

        verify(ff4ShopsClient, times(1)).updatePartnerState(
                FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(115L)
                        .withBusinessId(1000L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.NEW)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(singletonList(PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                .withServiceId(1115L)
                                .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                .build()))
                        .withPushStocksIsEnabled(false)
                        .build());
        Mockito.verify(lmsClient, times(1)).updatePartnerSettings(115L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(true)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(partnerResponse.getKorobyteSyncEnabled())
                        .build());
    }

    private PartnerResponse createDefaultPartnerResponse(long partnerId) {
        PartnerResponse.PartnerResponseBuilder responseBuilder
                = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class);
        responseBuilder.id(partnerId);
        responseBuilder.status(PartnerStatus.INACTIVE);

        return responseBuilder.build();
    }

    private void mockMarketId() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(100500L).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }
}
