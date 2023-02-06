package ru.yandex.market.pvz.internal.domain.hub;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.logistics.dto.hub.HubFeaturesDto;
import ru.yandex.market.pvz.client.logistics.dto.hub.HubInfoDto;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_ACTIVE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_RETURN_ALLOWED;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class HubServiceTest {

    private final TestPickupPointFactory pickupPointFactory;

    private final HubService hubService;

    @Test
    void getHubInfo() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        HubInfoDto actual = hubService.getHubInfoByCampaignId(pickupPoint.getPvzMarketId());

        HubInfoDto expected = HubInfoDto.builder()
                .id(pickupPoint.getId())
                .mbiCampaignId(pickupPoint.getPvzMarketId())
                .name(DEFAULT_NAME)
                .active(DEFAULT_ACTIVE)
                .features(HubFeaturesDto.builder()
                        .delivery(true)
                        .sorting(false)
                        .refund(DEFAULT_RETURN_ALLOWED)
                        .dropOff(PickupPoint.DEFAULT_DROP_OFF_FEATURE)
                        .build())
                .build();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void tryToGetInfoForNotExistentHub() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        assertThatThrownBy(() -> hubService.getHubInfoByCampaignId(pickupPoint.getPvzMarketId() + 1))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }
}
