package ru.yandex.direct.bsexport.query.order;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.exception.ObjectNotFoundInSnapshotException;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class OrderDataFactoryGetGroupOrderIdTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    private CommonCampaign campaign;
    private WalletTypedCampaign wallet;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);

        wallet = new WalletTypedCampaign()
                .withId(nextPositiveLong())
                .withType(CampaignType.WALLET);
        putCampaignToSnapshot(wallet);

        campaign = new TextCampaign()
                .withId(nextPositiveLong())
                .withWalletId(wallet.getId())
                .withType(CampaignType.TEXT);
        putCampaignToSnapshot(campaign);
    }

    @Test
    void campaignIsNotUnderWallet_groupOrderIdIs0() {
        campaign.setWalletId(0L);
        assertThat(orderDataFactory.getGroupOrderId(campaign))
                .isEqualByComparingTo(0L);
    }

    @Test
    void campaignIsUnderWalletWithoutOrderId_groupOrderIdIs0() {
        longTest(0);
    }

    @Test
    void campaignIsUnderWalletWithRandomUnsignedIntOrderId_groupOrderIdIsExpected() {
        long walletOrderId = RandomUtils.nextLong(1, 4294967296L);
        longTest(walletOrderId);
    }

    @Test
    void campaignIsUnderWalletWithRandomLongOrderId_groupOrderIdIsExpected() {
        long walletOrderId = RandomUtils.nextLong(4294967296L, Long.MAX_VALUE);
        longTest(walletOrderId);
    }

    private void longTest(long walletOrderId) {
        wallet.setOrderId(walletOrderId);

        assertThat(orderDataFactory.getGroupOrderId(campaign))
                .isEqualByComparingTo(walletOrderId);
    }

    @Test
    void campaignIsUnderWalletWhichNotFoundInSnapshot_throwsException() {
        removeCampaignFromSnapshot(wallet.getId());

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> orderDataFactory.getGroupOrderId(campaign));
    }
}
