package ru.yandex.direct.core.testing.steps.campaign.model0;

import java.time.LocalDate;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;

public interface BaseCampaign {

    Long getId();

    void setId(Long campaignId);

    Long getUid();

    void setUid(Long uid);

    String getName();

    void setName(String name);

    CampaignType getType();

    void setType(CampaignType type);

    Boolean getStatusEmpty();

    void setStatusEmpty(Boolean empty);

    StatusModerate getStatusModerate();

    void setStatusModerate(StatusModerate statusModerate);

    StatusPostModerate getStatusPostModerate();

    void setStatusPostModerate(StatusPostModerate statusPostModerate);

    Boolean getStatusShow();

    void setStatusShow(Boolean statusShow);

    Boolean getStatusActive();

    void setStatusActive(Boolean statusActive);

    Long getOrderId();

    void setOrderId(Long orderId);

    StatusBsSynced getStatusBsSynced();

    void setStatusBsSynced(StatusBsSynced statusBsSynced);

    LocalDate getStartTime();

    void setStartTime(LocalDate startTime);

    LocalDate getFinishTime();

    void setFinishTime(LocalDate finishTime);

    BalanceInfo getBalanceInfo();

    void setBalanceInfo(BalanceInfo balanceInfo);


    BaseCampaign withId(Long id);

    BaseCampaign withUid(Long uid);

    BaseCampaign withName(String name);

    BaseCampaign withType(CampaignType type);

    BaseCampaign withStatusEmpty(Boolean empty);

    BaseCampaign withStatusModerate(StatusModerate statusModerate);

    BaseCampaign withStatusPostModerate(StatusPostModerate statusPostModerate);

    BaseCampaign withStatusShow(Boolean statusShow);

    BaseCampaign withStatusActive(Boolean statusActive);

    BaseCampaign withOrderId(Long orderId);

    BaseCampaign withStatusBsSynced(StatusBsSynced statusBsSynced);

    BaseCampaign withStartTime(LocalDate startTime);

    BaseCampaign withFinishTime(LocalDate finishTime);

    BaseCampaign withBalanceInfo(BalanceInfo balanceInfo);
}
