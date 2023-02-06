package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.dbutil.model.ClientId;

public class RetargetingInfo {

    private AdGroupInfo adGroupInfo;
    private RetConditionInfo retConditionInfo;
    private Retargeting retargeting;

    public RetargetingInfo() {
        adGroupInfo = new AdGroupInfo();
        retConditionInfo = new RetConditionInfo();
        setCommonClientInfo(adGroupInfo.getClientInfo());
    }

    /**
     * Этот метод следит за тем, чтобы у создателя условия ретаргетинга
     * и создателя баннера был общий создатель клиента.
     * <p>
     * Метод необходимо вызывать при любом, прямом или косвенном изменении
     * создателя клиента у создателя условия ретаргетинга или
     * у создателя баннера.
     *
     * @param clientInfo общий создатель клиента
     */
    protected void setCommonClientInfo(ClientInfo clientInfo) {
        adGroupInfo.withClientInfo(clientInfo);
        retConditionInfo.withClientInfo(clientInfo);
    }

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public RetargetingInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        setCommonClientInfo(adGroupInfo.getClientInfo());
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getAdGroupInfo().getCampaignInfo();
    }

    public RetargetingInfo withCampaignInfo(CampaignInfo campaignInfo) {
        getAdGroupInfo().withCampaignInfo(campaignInfo);
        setCommonClientInfo(campaignInfo.getClientInfo());
        return this;
    }

    public ClientInfo getClientInfo() {
        return adGroupInfo.getClientInfo();
    }

    public RetargetingInfo withClientInfo(ClientInfo clientInfo) {
        setCommonClientInfo(clientInfo);
        return this;
    }

    public RetConditionInfo getRetConditionInfo() {
        return retConditionInfo;
    }

    public RetargetingInfo withRetConditionInfo(
            RetConditionInfo retConditionInfo) {
        this.retConditionInfo = retConditionInfo;
        setCommonClientInfo(retConditionInfo.getClientInfo());
        return this;
    }

    public RetargetingInfo useCommonAdGroupInfo(RetargetingInfo retargetingInfo) {
        return withAdGroupInfo(retargetingInfo.getAdGroupInfo());
    }

    public RetargetingInfo useCommonCampaignInfo(RetargetingInfo retargetingInfo) {
        return withCampaignInfo(retargetingInfo.getCampaignInfo());
    }

    public RetargetingInfo useCommonClientInfo(RetargetingInfo retargetingInfo) {
        return withClientInfo(retargetingInfo.getClientInfo());
    }

    public RetargetingInfo useCommonRetConditionInfo(RetargetingInfo retargetingInfo) {
        return withRetConditionInfo(retargetingInfo.getRetConditionInfo());
    }

    public Retargeting getRetargeting() {
        return retargeting;
    }

    public RetargetingInfo withRetargeting(Retargeting retargeting) {
        this.retargeting = retargeting;
        return this;
    }

    public Long getRetargetingId() {
        return retargeting.getId();
    }

    public Long getAdGroupId() {
        return getAdGroupInfo().getAdGroupId();
    }

    public Long getCampaignId() {
        return getAdGroupInfo().getCampaignId();
    }

    public Long getUid() {
        return getAdGroupInfo().getUid();
    }

    public ClientId getClientId() {
        return getAdGroupInfo().getClientId();
    }

    public Long getRetConditionId() {
        return getRetConditionInfo().getRetConditionId();
    }

    public Integer getShard() {
        return getAdGroupInfo().getShard();
    }
}
