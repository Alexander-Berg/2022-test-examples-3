package ru.yandex.direct.bsexport.snapshot;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.exception.BsExportProcessingException;
import ru.yandex.direct.bsexport.exception.ObjectNotFoundInSnapshotException;
import ru.yandex.direct.bsexport.snapshot.model.ExportedCampaign;
import ru.yandex.direct.bsexport.snapshot.model.ExportedClient;
import ru.yandex.direct.bsexport.snapshot.model.ExportedUser;
import ru.yandex.direct.bsexport.snapshot.model.QueuedCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class BsExportSnapshotGetObjectMethodsTests extends BsExportSnapshotTestBase {

    @Test
    void strictlyGetQueuedCampaign_getCampaignFromQueuedCampaignsHolderById() {
        long campaignId = nextPositiveLong();
        QueuedCampaign campaign = new QueuedCampaign().withId(campaignId);
        putQueuedCampaignToSnapshot(campaign);

        assertThat(snapshot.strictlyGetQueuedCampaign(campaignId)).isEqualTo(campaign);
    }

    @Test
    void strictlyGetQueuedCampaign_throwsExceptionIfNotFound() {
        long campaignId = nextPositiveLong();

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetQueuedCampaign(campaignId));
    }

    @Test
    void strictlyGetCampaign_getCampaignFromCampaignsHolderById() {
        CommonCampaign campaign = createCampaign();
        putCampaignToSnapshot(campaign);

        assertThat(snapshot.strictlyGetCampaign(campaign.getId())).isEqualTo(campaign);
    }

    @Test
    void strictlyGetCampaign_throwsExceptionIfNotFound() {
        long campaignId = nextPositiveLong();

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetCampaign(campaignId));
    }

    @Test
    void strictlyGetExportedCampaign_getCampaignFromExportedCampaignsHolderById() {
        long campaignId = nextPositiveLong();
        ExportedCampaign exportedCampaign = new ExportedCampaign().withId(campaignId);
        putExportedCampaignToSnapshot(exportedCampaign);

        assertThat(snapshot.strictlyGetExportedCampaign(campaignId)).isEqualTo(exportedCampaign);
    }

    @Test
    void strictlyGetExportedCampaign_throwsExceptionIfNotFound() {
        long campaignId = nextPositiveLong();

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetExportedCampaign(campaignId));
    }

    @Test
    void strictlyGetClientByCampaign_getClientFromClientsHolderByIdFromCampaign() {
        long clientId = nextPositiveLong();
        ExportedClient client = new ExportedClient().withId(clientId);
        CommonCampaign campaign = createCampaign().withClientId(clientId);
        putClientToSnapshot(client);

        assertThat(snapshot.strictlyGetClientByCampaign(campaign)).isEqualTo(client);
    }

    @Test
    void strictlyGetClientByCampaign_throwsExceptionIfNotFound() {
        long clientId = nextPositiveLong();
        CommonCampaign campaign = createCampaign().withClientId(clientId);

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetClientByCampaign(campaign));
    }

    @Test
    void strictlyGetUserByCampaign_getUserFromUsersHolderByIdFromCampaign() {
        long uid = nextPositiveLong();
        ExportedUser user = new ExportedUser().withId(uid);
        CommonCampaign campaign = createCampaign().withUid(uid);
        putUserToSnapshot(user);

        assertThat(snapshot.strictlyGetUserByCampaign(campaign)).isEqualTo(user);
    }

    @Test
    void strictlyGetUserByCampaign_throwsExceptionIfNotFound() {
        long uid = nextPositiveLong();
        CommonCampaign campaign = createCampaign().withUid(uid);

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetUserByCampaign(campaign));
    }

    @Test
    void strictlyGetWalletByCampaign_getWalletFromCampaignsHolderByWalletIdFromCampaig() {
        long walletId = nextPositiveLong();
        WalletTypedCampaign wallet = new WalletTypedCampaign().withId(walletId);
        CommonCampaign campaign = createCampaign().withWalletId(walletId);
        putCampaignToSnapshot(wallet);

        assertThat(snapshot.strictlyGetWalletByCampaign(campaign)).isEqualTo(wallet);
    }

    @Test
    void strictlyGetWalletByCampaign_throwsExceptionIfWalletNotFound() {
        long walletId = nextPositiveLong();
        CommonCampaign campaign = createCampaign().withWalletId(walletId);

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> snapshot.strictlyGetWalletByCampaign(campaign));
    }

    @Test
    void strictlyGetWalletByCampaign_throwsExceptionIfWalletIsNotWalletTypedCampaign() {
        CommonCampaign notWallet = createCampaign();
        CommonCampaign campaign = createCampaign().withWalletId(notWallet.getId());
        putCampaignToSnapshot(notWallet);

        assertThrows(BsExportProcessingException.class, () -> snapshot.strictlyGetWalletByCampaign(campaign));
    }



}
