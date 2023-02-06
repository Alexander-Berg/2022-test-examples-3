package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getManagerUid;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class OrderDataFactoryGetManagerUidTest {

    private CommonCampaign campaign;

    @Test
    void managerUidIsNull_resultIs0() {
        campaign = new TextCampaign();
        assertThat(getManagerUid(campaign)).isEqualTo(0);
    }

    @Test
    void managerUidIs0_resultIs0() {
        campaign = new TextCampaign().withManagerUid(0L);
        assertThat(getManagerUid(campaign)).isEqualTo(0);
    }

    @Test
    void managerIsRandom_resultIsManagerUid() {
        long managerUid = nextPositiveLong();
        campaign = new TextCampaign().withManagerUid(managerUid);

        assertThat(getManagerUid(campaign)).isEqualTo(managerUid);
    }
}
