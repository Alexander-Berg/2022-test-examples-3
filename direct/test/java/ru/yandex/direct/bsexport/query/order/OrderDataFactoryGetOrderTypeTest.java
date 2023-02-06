package ru.yandex.direct.bsexport.query.order;

import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.bsexport.snapshot.model.ExportedClient;
import ru.yandex.direct.bsexport.snapshot.model.ExportedUser;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class OrderDataFactoryGetOrderTypeTest extends BsExportSnapshotTestBase {
    private static final String TYPE_TEST_NAME = "campaignType = {0}";
    private OrderDataFactory orderDataFactory;

    private CommonCampaign campaign;
    private WalletTypedCampaign wallet;
    private ExportedClient client;
    private ExportedUser user;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);

        client = new ExportedClient()
                .withId(nextPositiveLong())
                .withIsBusinessUnit(false);
        putClientToSnapshot(client);

        user = new ExportedUser()
                .withId(nextPositiveLong())
                .withStatusYandexAdv(false);
        putUserToSnapshot(user);

        campaign = new TextCampaign().withType(CampaignType.TEXT);
        fillCampaign(campaign);
        putCampaignToSnapshot(campaign);

        wallet = new WalletTypedCampaign().withType(CampaignType.WALLET);
        fillCampaign(wallet);
        putCampaignToSnapshot(wallet);
    }

    private CommonCampaign fillCampaign(CommonCampaign campaign) {
        long campaignId = nextPositiveLong();
        if (campaignId == 2834265) {
            // это особая кампания, если random выбрал его - не хотим чтобы тесты падали, меняем
            campaignId = nextPositiveLong(Short.MAX_VALUE);
        }
        return campaign.withId(campaignId)
                .withUid(user.getId())
                .withClientId(client.getId())
                .withPaidByCertificate(false);
    }

    //----------- 7 ------------

    @Test
    void geoCampaign() {
        campaign.setType(CampaignType.GEO);
        expect(campaign, 7);
    }

    @Test
    void geoIsTheMostPriorityFeature() {
        campaign.setType(CampaignType.GEO);
        campaign.setAgencyId(1647047L);
        campaign.setPaidByCertificate(true);
        user.setStatusYandexAdv(true);
        client.setIsBusinessUnit(true);

        expect(campaign, 7);
    }

    //----------- 9 ------------

    @Test
    void businessUnitClient() {
        client.setIsBusinessUnit(true);
        expect(campaign, 9);
    }

    @Test
    void businessUnitFeatureIsMoreImportantThanTypeAndYandexAdvAndBegun() {
        client.setIsBusinessUnit(true);
        campaign.setType(CampaignType.INTERNAL_DISTRIB);
        campaign.setAgencyId(1647047L);
        campaign.setPaidByCertificate(true);
        user.setStatusYandexAdv(true);
        expect(campaign, 9);
    }


    //----------- 6 ------------

    @ParameterizedTest(name = TYPE_TEST_NAME)
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.MATCH_ALL, names = "^INTERNAL_.*")
    void internalCampaign(CampaignType campaignType) {
        campaign.setType(campaignType);
        expect(campaign, 6);
    }

    @Test
    void yandexAdvLogin() {
        user.setStatusYandexAdv(true);
        expect(campaign, 6);
    }

    @Test
    void campaignPaidByCertificate() {
        campaign.setPaidByCertificate(true);
        expect(campaign, 6);
    }

    @Test
    void walletPaidByCertificate() {
        campaign.setWalletId(wallet.getId());
        wallet.setPaidByCertificate(true);
        expect(campaign, 6);
    }

    @Test
    void yandexAdvFeatureIsMoreImportantThanBegun() {
        campaign.setAgencyId(1647047L);
        user.setStatusYandexAdv(true);
        expect(campaign, 6);
    }

    @Test
    void paidByCertificateIsMoreImportantThanBegun() {
        campaign.setAgencyId(1647047L);
        campaign.setPaidByCertificate(true);
        expect(campaign, 6);
    }

    @Test
    void walletPaidByCertificateIsMoreImportantThanBegun() {
        wallet.setPaidByCertificate(true);
        campaign.setAgencyId(1647047L);
        campaign.setWalletId(wallet.getId());
        expect(campaign, 6);
    }

    //----------- 8 ------------

    @Test
    void begun2011Agency() {
        campaign.setAgencyId(1647047L);
        expect(campaign, 8);
    }

    @Test
    void begunTestAgency() {
        campaign.setAgencyId(1618235L);
        expect(campaign, 8);
    }

    @Test
    void begunTestCampaign() {
        campaign.setId(2834265L);
        putCampaignToSnapshot(campaign);
        expect(campaign, 8);
    }

    //----------- 1 ------------
    static Stream<CampaignType> usualExportedTypes() {
        return StreamEx.of(CampaignTypeKinds.BS_EXPORT)
                .remove(CampaignTypeKinds.GEO::contains)
                .remove(CampaignTypeKinds.INTERNAL::contains)
                .remove(CampaignTypeKinds.WALLET::contains);
    }

    @ParameterizedTest(name = TYPE_TEST_NAME)
    @MethodSource("usualExportedTypes")
    void usualDirect(CampaignType campaignType) {
        campaign.setType(campaignType);
        expect(campaign, 1);
    }

    @Test
    void walletCampaign() {
        expect(wallet, 1);
    }

    @ParameterizedTest(name = TYPE_TEST_NAME)
    @MethodSource("usualExportedTypes")
    void usualDirectUnderWallet(CampaignType campaignType) {
        campaign.setType(campaignType);
        campaign.setWalletId(wallet.getId());
        expect(campaign, 1);
    }

    @Test
    void agencyCampaign() {
        long agencyID = RandomUtils.nextLong(1, 1_500_000);
        campaign.setAgencyId(agencyID);
        expect(campaign, 1);
    }

    //----------- - ------------

    private void expect(CommonCampaign campaign, int orderType) {
        assertThat(orderDataFactory.getOrderType(campaign))
                .describedAs("вычисленный OrderType")
                .isEqualTo(orderType);
    }
}
