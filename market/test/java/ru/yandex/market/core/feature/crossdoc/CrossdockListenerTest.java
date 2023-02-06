package ru.yandex.market.core.feature.crossdoc;

import java.time.Clock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link CrossdockListener}
 */
@DbUnitDataSet(before = "crossdockListenerTestBefore.csv")
class CrossdockListenerTest extends FunctionalTest {

    private static final long ACTION_ID = 1;
    private static final long SHOP_ID = 10;
    private static final long DROPSHIP_ID = 11;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @BeforeEach
    void init() {
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
    }

    @Test
    @DbUnitDataSet(after = "changeStatusToSuccess.after.csv")
    void moveToSuccessStatusTest() {
        changeStatus(SHOP_ID, ParamCheckStatus.NEW);
    }

    @Test
    @DbUnitDataSet(after = "changeStatusToFail.after.csv")
    void moveFromSuccessToFailTest() {
        changeStatus(SHOP_ID, ParamCheckStatus.FAIL);
    }

    @Test
    @DbUnitDataSet(after = "changeStatusToDontWant.after.csv")
    void moveFromRevokeToDontWantTest() {
        changeStatus(12, ParamCheckStatus.DONT_WANT);
    }

    @Test
    void moveFromRevokeForDropshipException() {
        Assertions.assertThrows(IllegalStateException.class, () -> changeStatus(DROPSHIP_ID, ParamCheckStatus.NEW));
    }

    private void changeStatus(long shopId, ParamCheckStatus newStatus) {
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.CHANGE_PARAM, ACTION_ID),
                (transaction, actionId) -> {
                    featureService.changeStatus(actionId, ShopFeature.of(shopId, FeatureType.CROSSDOCK, newStatus));
                    return null;
                }
        );
    }
}
