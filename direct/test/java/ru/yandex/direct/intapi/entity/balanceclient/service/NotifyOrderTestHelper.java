package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.time.LocalDate;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.test.utils.RandomNumberUtils;

public abstract class NotifyOrderTestHelper {

    public static CampaignDataForNotifyOrder generateCampaignDataForNotifyOrder() {
        return new CampaignDataForNotifyOrder()
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withWalletId(RandomNumberUtils.nextPositiveLong())
                .withUid(RandomNumberUtils.nextPositiveLong())
                .withManagerUid(RandomNumberUtils.nextPositiveLong())
                .withAgencyUid(RandomNumberUtils.nextPositiveLong())
                .withClientId(RandomNumberUtils.nextPositiveLong())
                .withAgencyId(RandomNumberUtils.nextPositiveLong())
                .withBalanceTid(RandomNumberUtils.nextPositiveLong())
                .withType(CampaignType.TEXT)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                .withName(RandomStringUtils.randomAlphabetic(10))
                .withEmail(RandomStringUtils.randomAlphanumeric(11))
                .withFio(RandomStringUtils.randomAlphabetic(12))
                .withLogin(RandomStringUtils.randomAlphabetic(13))
                .withPhone(RandomStringUtils.randomNumeric(14))
                .withStartTimeTs(RandomNumberUtils.nextPositiveLong())
                .withStartTimeInFuture(true)
                .withSumUnits(RandomNumberUtils.nextPositiveLong())
                .withSumSpentUnits(RandomNumberUtils.nextPositiveLong())
                .withFinishDate(LocalDate.now())
                .withCurrency(CurrencyCode.RUB)
                .withLang("ru");
    }

    public static NotifyOrderParameters generateNotifyOrderParameters() {
        return new NotifyOrderParameters()
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withServiceId(BalanceClientServiceConstants.DIRECT_SERVICE_ID)
                .withChipsCost(RandomNumberUtils.nextPositiveBigDecimal())
                .withChipsSpent(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumRealMoney(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumUnits(RandomNumberUtils.nextPositiveBigDecimal())
                .withTotalSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withTid(RandomNumberUtils.nextPositiveLong())
                .withPaidByCertificate(0);
    }
}
