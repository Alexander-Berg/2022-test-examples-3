package ru.yandex.direct.grid.processing.service.campaign.cpmbanner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;

public class DefaultGdAddCpmBannerCampaign {
    public static GdAddCmpBannerCampaign defaultGdAddCpmBannerCampaign(CampaignAttributionModel defaultAttributionModel) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddCmpBannerCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(null)
                        .withStrategyName(GdCampaignStrategyName.CPM_DEFAULT)
                        .withStrategyData(new GdCampaignStrategyData())
                )
                .withMetrikaCounters(emptyList())
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.STRETCHED)
                .withDisabledPlaces(emptyList());
    }
}
