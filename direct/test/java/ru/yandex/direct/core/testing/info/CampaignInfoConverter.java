package ru.yandex.direct.core.testing.info;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedPageIds;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDialog;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledDomainsAndSsp;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledVideoPlacements;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithNetworkSettings;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalAddMetrikaTagToUrl;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOrganization;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.BroadmatchFlag;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusPostModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository.contextLimitTypeFromDb;

@ParametersAreNonnullByDefault
public class CampaignInfoConverter {

    public static CampaignInfo toCampaignInfo(ClientInfo clientInfo, CommonCampaign campaign) {
        Campaign resultCampaign = new Campaign()
                .withId(campaign.getId())
                .withType(campaign.getType())
                .withClientId(clientInfo.getClientId().asLong())
                .withWalletId(campaign.getWalletId())
                .withUid(campaign.getUid())
                .withAgencyUid(campaign.getAgencyUid())
                .withAgencyId(campaign.getAgencyId())
                .withManagerUid(campaign.getManagerUid())
                .withName(campaign.getName())
                .withType(campaign.getType())
                .withStatusEmpty(campaign.getStatusEmpty())
                .withStatusModerate(getStatusModerate(campaign.getStatusModerate()))
                .withStatusPostModerate(getStatusPostModerate(campaign.getStatusPostModerate()))
                .withStatusShow(campaign.getStatusShow())
                .withStatusActive(campaign.getStatusActive())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withTimezoneId(campaign.getTimeZoneId())
                .withOrderId(campaign.getOrderId())
                .withStatusBsSynced(getStatusBsSynced(campaign.getStatusBsSynced()))
                .withStartTime(campaign.getStartDate())
                .withFinishTime(campaign.getEndDate())
                .withLastChange(campaign.getLastChange())
                .withAutobudgetForecastDate(campaign.getAutobudgetForecastDate())
                .withEmail(campaign.getEmail())
                .withArchived(false)
                .withStatusMetricaControl(false)
                .withLastShowTime(campaign.getLastChange());

        if (campaign instanceof CampaignWithOptionalAddMetrikaTagToUrl) {
            resultCampaign.setHasAddMetrikaTagToUrl(((CampaignWithOptionalAddMetrikaTagToUrl) campaign)
                    .getHasAddMetrikaTagToUrl());
        }

        if (campaign instanceof CampaignWithOrganization) {
            resultCampaign.setDefaultPermalink(((CampaignWithOrganization) campaign).getDefaultPermalinkId());
        }

        if (campaign instanceof CampaignWithDialog) {
            resultCampaign.setClientDialogId(((CampaignWithDialog) campaign).getClientDialogId());
        }

        if (campaign instanceof CampaignWithDisabledDomainsAndSsp) {
            List<String> disabledDomains = ((CampaignWithDisabledDomainsAndSsp) campaign).getDisabledDomains();
            resultCampaign.setDisabledDomains(disabledDomains != null ? Set.copyOf(disabledDomains) : null);
            resultCampaign.setDisabledSsp(((CampaignWithDisabledDomainsAndSsp) campaign).getDisabledSsp());
        }

        if (campaign instanceof CampaignWithDisabledVideoPlacements) {
            resultCampaign.setDisabledVideoPlacements(((CampaignWithDisabledVideoPlacements) campaign)
                    .getDisabledVideoPlacements());
        }

        if (campaign instanceof CampaignWithBroadMatch) {
            resultCampaign.setBroadMatchFlag(toBroadMatchFlag(((CampaignWithBroadMatch) campaign).getBroadMatch()));
            resultCampaign.setBroadMatchLimit(((CampaignWithBroadMatch) campaign).getBroadMatch().getBroadMatchLimit());
        }

        if (campaign instanceof CampaignWithBrandLift) {
            resultCampaign.setBrandSurveyId(((CampaignWithBrandLift) campaign).getBrandSurveyId());
        }

        if (campaign instanceof CampaignWithMetrikaCounters) {
            resultCampaign.setMetrikaCounters(((CampaignWithMetrikaCounters) campaign).getMetrikaCounters());
        }

        if (campaign instanceof CampaignWithMinusKeywords) {
            resultCampaign.setMinusKeywords(((CampaignWithMinusKeywords) campaign).getMinusKeywords());
        }

        if (campaign instanceof CampaignWithAllowedPageIds) {
            resultCampaign.setAllowedPageIds(((CampaignWithAllowedPageIds) campaign).getAllowedPageIds());
        }

        if (campaign instanceof CampaignWithStrategy) {
            // TODO: конвертация стратегии
        }

        if (campaign instanceof CampaignWithNetworkSettings) {
            ContextSettings contextSettings = new ContextSettings()
                    .withLimit(((CampaignWithNetworkSettings) campaign).getContextLimit())
                    .withLimitType(contextLimitTypeFromDb(((CampaignWithNetworkSettings) campaign)
                            .getContextLimit().longValue()))
                    .withPriceCoeff(((CampaignWithNetworkSettings) campaign).getContextPriceCoef())
                    .withEnableCpcHold(campaign.getEnableCpcHold());

            resultCampaign.setContextSettings(contextSettings);
        }

        return new CampaignInfo()
                .withCampaign(resultCampaign)
                .withClientInfo(clientInfo);
    }

    @Nullable
    private static StatusBsSynced getStatusBsSynced(@Nullable CampaignStatusBsSynced status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case YES:
                return StatusBsSynced.YES;
            case NO:
                return StatusBsSynced.NO;
            case SENDING:
                return StatusBsSynced.SENDING;
            default:
                throw new IllegalStateException("unsupported bas synced status");
        }
    }

    @Nullable
    private static StatusPostModerate getStatusPostModerate(@Nullable CampaignStatusPostmoderate status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case NO:
                return StatusPostModerate.NO;
            case NEW:
                return StatusPostModerate.NEW;
            case YES:
                return StatusPostModerate.YES;
            case ACCEPTED:
                return StatusPostModerate.ACCEPTED;
            default:
                throw new IllegalStateException("unsupported post moderate status");
        }

    }

    @Nullable
    private static StatusModerate getStatusModerate(@Nullable CampaignStatusModerate status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case YES:
                return StatusModerate.YES;
            case NEW:
                return StatusModerate.NEW;
            case READY:
                return StatusModerate.READY;
            case SENT:
                return StatusModerate.SENT;
            case NO:
                return StatusModerate.NO;
            case MEDIAPLAN:
                return StatusModerate.MEDIAPLAN;
            default:
                throw new IllegalStateException("unsupported moderate status");
        }
    }

    @Nullable
    private static BroadmatchFlag toBroadMatchFlag(@Nullable BroadMatch broadMatch) {
        if (broadMatch == null) {
            return null;
        }
        return broadMatch.getBroadMatchFlag() ? BroadmatchFlag.YES :
                BroadmatchFlag.NO;
    }
}
