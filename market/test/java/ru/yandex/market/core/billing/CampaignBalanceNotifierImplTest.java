package ru.yandex.market.core.billing;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "CampaignBalanceNotifierImplTest.before.csv")
class CampaignBalanceNotifierImplTest extends FunctionalTest {
    @Autowired
    private CampaignBalanceNotifierImpl notifier;

    @Test
    void getCampaignInfosTest() {
        List<CampaignBalanceNotifierImpl.AlertInfo> actualResult1 = notifier.getCampaignInfos(111);
        assertThat(actualResult1).singleElement().satisfies(transaction(
                0L,
                100L,
                111L,
                BigDecimal.valueOf(42.42),
                false,
                null,
                15L,
                null,
                false,
                "clearspb.ru",
                null,
                null,
                BigDecimal.valueOf(12.34)
        ));

        List<CampaignBalanceNotifierImpl.AlertInfo> actualResult2 = notifier.getCampaignInfos(222);
        assertThat(actualResult2).singleElement().satisfies(transaction(
                0L,
                200L,
                222L,
                BigDecimal.valueOf(-3),
                false,
                null,
                15L,
                null,
                false,
                "test.ru",
                null,
                null,
                null
        ));

        List<CampaignBalanceNotifierImpl.AlertInfo> actualResult3 = notifier.getCampaignInfos(333);
        assertThat(actualResult3).singleElement().satisfies(transaction(
                0L,
                300L,
                333L,
                BigDecimal.valueOf(5),
                false,
                null,
                15L,
                null,
                false,
                "bulka.com",
                null,
                null,
                BigDecimal.valueOf(4.56)
        ));
    }

    private static Condition<CampaignBalanceNotifierImpl.AlertInfo> transaction(
            Long templateId,
            Long datasourceId,
            Long campaignId,
            BigDecimal balance,
            Boolean isBalanceLow,
            BigDecimal avgDailySpendingUsd,
            Long daysCountForAvg,
            Long daysToSpendRemainder,
            Boolean isOff,
            String shopName,
            String manager,
            Long oldDaysToSpendRemainder,
            BigDecimal lastPayment
    ) {
        return HamcrestCondition.matching(MbiMatchers.<CampaignBalanceNotifierImpl.AlertInfo>newAllOfBuilder()
                .add(CampaignBalanceNotifierImpl.AlertInfo::getTemplateId, templateId, "templateId")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getDatasourceId, datasourceId, "datasourceId")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getCampaignId, campaignId, "campaignId")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getBalance, balance, "balance")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getBalanceLow, isBalanceLow, "isBalanceLow")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getAvgDailySpendingUsd, avgDailySpendingUsd,
                        "avgDailySpendingUsd")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getDaysCountForAvg, daysCountForAvg, "daysCountForAvg")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getDaysToSpendRemainder, daysToSpendRemainder,
                        "daysToSpendRemainder")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getOff, isOff, "isOff")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getShopName, shopName, "shopName")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getManager, manager, "manager")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getOldDaysToSpendRemainder, oldDaysToSpendRemainder,
                        "oldDaysToSpendRemainder")
                .add(CampaignBalanceNotifierImpl.AlertInfo::getLastPayment, lastPayment, "lastPayment")
                .build());
    }

    @Test
    void notifyCampaignPaymentTest() {
        notifier.notifyCampaignPayment(111);

        verifySentNotificationType(partnerNotificationClient, 1, 63);
    }

    @Test
    void notifySupplierCampaignPaymentTest() {
        notifier.notifyCampaignPayment(333);

        verifySentNotificationType(partnerNotificationClient, 1, 1602519581L);
    }

    @Test
    @DbUnitDataSet(before = "CampaignBalanceNotifierImplTest.notifyLowBalanceSuppliersTest.before.csv")
    void notifyLowBalanceSuppliersTest() {
        notifier.notifyLowBalanceSuppliers();

        verifySentNotificationType(partnerNotificationClient, 1, 1604060582L);
    }

    @Test
    @DbUnitDataSet(before = "CampaignBalanceNotifierImplTest.notifyLowBalanceSuppliersWithAutopaymentTest.before.csv")
    @DisplayName("Testing notifications of suppliers with enabled autopayment")
    void notifyLowBalanceSuppliersWithAutopaymentTest() {
        notifier.notifyLowBalanceSuppliers();

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "CampaignBalanceNotifierImplTest.notifyLowBalanceSuppliersMixedAutopaymentTest.before.csv")
    @DisplayName("Testing notifications of one supplier with disabled autopayment and other with enabled")
    void notifyLowBalanceSuppliersWithMixedAutopaymentTest() {
        notifier.notifyLowBalanceSuppliers();

        verifySentNotificationType(partnerNotificationClient, 1, 1604060582L);
    }
}
