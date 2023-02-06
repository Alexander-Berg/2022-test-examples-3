package ru.yandex.market.core.feature.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.model.FeatureDescription;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Проверяем отправку уведомлений по бизнесу.
 */
@DbUnitDataSet(before = "notification/FeatureNotificationServiceImplTest.before.csv")
class FeatureNotificationServiceImplTest extends FunctionalTest {
    @Autowired
    private FeatureNotificationServiceImpl featureNotificationService;
    @Autowired
    private FeatureDescription shopLogo;

    @Test
    void checkSendingNotificationWithoutCampaign() {
        featureNotificationService.sendNotification(shopLogo, null, 1543402172,
                new ShopFeature(1, 10L, FeatureType.SHOP_LOGO, ParamCheckStatus.SUCCESS));

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    void checkBusinessNotification() {
        featureNotificationService.sendNotification(shopLogo, null, 1543402172,
                new ShopFeature(1, 100L, FeatureType.SHOP_LOGO, ParamCheckStatus.SUCCESS));

        verifySentNotificationType(partnerNotificationClient, 1, 1543402172L);
    }
}
