package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.time.model.TimeInterval;

@ParametersAreNonnullByDefault
public class AddCampaignDelegateConstants {

    public static final TimeInterval DEFAULT_SMS_INTERVAL = new TimeInterval()
            .withStartHour(9)
            .withStartMinute(0)
            .withEndHour(21)
            .withEndMinute(0);

    public static final CampaignWarnPlaceInterval DEFAULT_CHECK_POSITION_INTERVAL_EVENT = CampaignWarnPlaceInterval._60;

    public static final Boolean DEFAULT_ENABLE_SEND_ACCOUNT_NEWS = Boolean.FALSE;

    public static final Boolean DEFAULT_ENABLE_CHECK_POSITION_EVENT = Boolean.FALSE;

    public static final int DEFAULT_WARNING_BALANCE = 20;

}
