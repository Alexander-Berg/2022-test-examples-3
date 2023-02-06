package ru.yandex.direct.jobs.campaign.paused.daybudget;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jooq.DSLContext;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignEmailNotification;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsDayBudgetNotificationStatus;
import ru.yandex.direct.feature.FeatureName;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class PausedByDayBudgetTestUtils {
    public static class NotificationSendingPermissions {
        final boolean featureEnabled;
        final boolean allowedSendingEmails;
        final boolean allowedSendingSms;

        public NotificationSendingPermissions(boolean featureEnabled, boolean allowedSendingEmails, boolean allowedSendingSms) {
            this.featureEnabled = featureEnabled;
            this.allowedSendingEmails = allowedSendingEmails;
            this.allowedSendingSms = allowedSendingSms;
        }
    }

    public static void enableFeatures(FeatureSteps steps, ClientInfo clientInfo,
                                      NotificationSendingPermissions permissions) {
        steps.addClientFeature(clientInfo.getClientId(), FeatureName.PAUSED_BY_DAY_BUDGET_WARNINGS,
                permissions.featureEnabled);
    }

    private final static String ALLOWED_SENDING_SMS =
            CampaignMappings.smsFlagsToDb(EnumSet.of(SmsFlag.PAUSED_BY_DAY_BUDGET_SMS));
    private final static String ALLOWED_SENDING_MAIL =
            CampaignMappings.emailNotificationsToDb(EnumSet.of(CampaignEmailNotification.PAUSED_BY_DAY_BUDGET));

    public static void setAllowingSendingNotifications(DSLContext context,
                                                       Long campaignInfo,
                                                       NotificationSendingPermissions permissions) {
        setAllowingSendingNotifications(context, List.of(campaignInfo), permissions);
    }

    public static void setAllowingSendingNotifications(DSLContext context, Collection<Long> campaignInfos,
                                                       NotificationSendingPermissions permissions) {
        context.update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.SMS_FLAGS, permissions.allowedSendingSms ?
                        ALLOWED_SENDING_SMS : "")
                .set(CAMP_OPTIONS.EMAIL_NOTIFICATIONS, permissions.allowedSendingEmails ?
                        ALLOWED_SENDING_MAIL : "")
                .where(CAMP_OPTIONS.CID.in(campaignInfos))
                .execute();
    }

    public static Set<PausedByDayBudgetNotificationType> typesByPermissions(NotificationSendingPermissions permissions) {
        Set<PausedByDayBudgetNotificationType> types = new HashSet<>();
        if (permissions.featureEnabled) {
            types.add(PausedByDayBudgetNotificationType.EVENT_LOG);
            if (permissions.allowedSendingEmails) {
                types.add(PausedByDayBudgetNotificationType.EMAIL);
            }
            if (permissions.allowedSendingSms) {
                types.add(PausedByDayBudgetNotificationType.SMS);
            }
        }
        return types;
    }

    public static PausedByDayBudgetNotificationType[] typesByPermissionsAsArray(NotificationSendingPermissions permissions) {
        return typesByPermissions(permissions).toArray(PausedByDayBudgetNotificationType[]::new);
    }

    public static void markCampaignsReadyForNotification(DSLContext context, Collection<Long> ids) {
        context.update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.DAY_BUDGET_STOP_TIME, LocalDateTime.now())
                .set(CAMP_OPTIONS.DAY_BUDGET_NOTIFICATION_STATUS, CampOptionsDayBudgetNotificationStatus.Ready)
                .where(CAMP_OPTIONS.CID.in(ids))
                .execute();
    }

    public static void removeCampOptions(DSLContext context, Collection<Long> ids) {
        context.delete(CAMP_OPTIONS)
                .where(CAMP_OPTIONS.CID.in(ids))
                .execute();
    }

    public static List<Campaign> campaignsByInfos(int shard, CampaignRepository campaignRepository,
                                                  List<CampaignInfo> infos) {
        return campaignRepository.getCampaigns(shard, mapList(infos, CampaignInfo::getCampaignId));
    }

    public static <T, S> BiConsumer<T, S> emptyConsumer(Class<T> unused1, Class<S> unused2) {
        return PausedByDayBudgetTestUtils::EMPTY_BI_CONSUMER;
    }

    private static <T, S> void EMPTY_BI_CONSUMER(T t, S s) {
    }
}
