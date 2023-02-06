package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getGroupOrder;

class OrderDataFactoryGetGroupOrderTest {

    private CampaignWithCampaignType campaign;

    @BeforeEach
    void prepare() {
        campaign = new CampaignWithTypeImpl();
    }


    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.EXCLUDE, names = "WALLET")
    void anyCampaignIsNotGroupOrder(CampaignType campaignType) {
        campaign.setType(campaignType);

        assertThat(getGroupOrder(campaign)).isEqualTo(0);
    }

    @Test
    void walletCampaignIsGroupOrder() {
        campaign.setType(CampaignType.WALLET);

        assertThat(getGroupOrder(campaign)).isEqualTo(1);
    }
}
