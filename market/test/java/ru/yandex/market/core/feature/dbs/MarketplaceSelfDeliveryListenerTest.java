package ru.yandex.market.core.feature.dbs;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.stocks.FF4ShopsClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "marketplaceSelfDeliveryListenerTest.before.csv")
public class MarketplaceSelfDeliveryListenerTest extends FunctionalTest {

    private static final long ACTION_ID = 1;
    private static final long SHOP_ID = 10;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }


    @ParameterizedTest
    @EnumSource(value = ParamCheckStatus.class, names = {"NEW","FAIL","SUCCESS"})
    void name(ParamCheckStatus status) {
        changeStatus(status);

        verify(ff4ShopsClient, times(1)).updatePartnerState(
                eq(FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(SHOP_ID)
                        .withBusinessId(1000L)
                        .withFeatureStatus(status)
                        .withCpaIsPartnerInterface(true)
                        .withFeatureType(FeatureType.MARKETPLACE_SELF_DELIVERY)
                        .withPushStocksIsEnabled(false)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                .withServiceId(1001L)
                                .withDeliveryServiceType(DeliveryServiceType.DROPSHIP_BY_SELLER)
                                .build()
                        ))
                        .build()));
    }


    private void changeStatus(ParamCheckStatus newStatus) {
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.CHANGE_PARAM, ACTION_ID),
                (transaction, actionId) -> {
                    featureService.changeStatus(
                            actionId,
                            ShopFeature.of(SHOP_ID, FeatureType.MARKETPLACE_SELF_DELIVERY, newStatus)
                    );
                    return null;
                }
        );
    }
}
