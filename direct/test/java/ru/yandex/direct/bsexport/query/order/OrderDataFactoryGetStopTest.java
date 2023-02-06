package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getStop;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class OrderDataFactoryGetStopTest {

    private CommonCampaign campaign;

    @BeforeEach
    void prepare() {
        campaign = new TextCampaign().withId(nextPositiveLong());
    }

    @Test
    void statusShowIsNull_throwsException() {
        assertThrows(NullPointerException.class, () -> getStop(campaign));
    }

    @Test
    void statusShowTrue_StopIs0() {
        campaign.setStatusShow(true);
        assertThat(getStop(campaign)).isEqualTo(0);
    }

    @Test
    void statusShowFalse_StopIs1() {
        campaign.setStatusShow(false);
        assertThat(getStop(campaign)).isEqualTo(1);
    }
}
