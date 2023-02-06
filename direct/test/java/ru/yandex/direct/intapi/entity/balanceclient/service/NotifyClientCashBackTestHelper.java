package ru.yandex.direct.intapi.entity.balanceclient.service;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientCashBackParameters;
import ru.yandex.direct.test.utils.RandomNumberUtils;

@ParametersAreNonnullByDefault
public abstract class NotifyClientCashBackTestHelper {

    public static NotifyClientCashBackParameters generateNotifyClientCashBackParameters() {
        return new NotifyClientCashBackParameters()
                .withServiceId(BalanceClientServiceConstants.DIRECT_SERVICE_ID)
                .withBalanceCurrency(CurrencyCode.RUB.name())
                .withClientId(RandomNumberUtils.nextPositiveLong())
                .withCashBackBonus(RandomNumberUtils.nextPositiveBigDecimal())
                .withCashbackConsumedBonus(RandomNumberUtils.nextPositiveBigDecimal());
    }
}
