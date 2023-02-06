package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.MetrikaClient;

@Deprecated
// use typed steps, for example TextCampaignSteps
public class TypedCampaignSteps extends TypedCampaignStepsUnstubbed {

    private final Steps steps;

    @Autowired
    public TypedCampaignSteps(CampaignModifyRepository campaignModifyRepository,
                              Steps steps,
                              DslContextProvider dslContextProvider,
                              CampaignAddOperationSupportFacade campaignAddOperationSupportFacade,
                              MetrikaClient metrikaClient) {
        super(campaignModifyRepository, campaignAddOperationSupportFacade, dslContextProvider, metrikaClient);
        this.steps = steps;
    }

    @Override
    protected TypedCampaignInfo createTypedCampaign(TypedCampaignInfo campaignInfo, CommonCampaign defaultCampaign) {
        if (campaignInfo.getClientInfo() == null) {
            UserInfo defaultUser = steps.userSteps().createDefaultUser();
            campaignInfo.setClientInfo(defaultUser.getClientInfo());
            if (campaignInfo.getOperatorInfo() == null) {
                campaignInfo.setOperatorInfo(defaultUser);
            }
        }
        return super.createTypedCampaign(campaignInfo, defaultCampaign);
    }

    public TypedCampaignInfo createDefaultCpmBannerCampaign() {
        return createDefaultCpmBannerCampaign(null, null);
    }

    public TypedCampaignInfo createDefaultDynamicCampaign() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        return createDefaultDynamicCampaign(defaultUser, defaultUser.getClientInfo());
    }

    public TypedCampaignInfo createDefaultInternalDistribCampaign() {
        ClientInfo internalAdProduct = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        return creatDefaultInternalDistribCampaign(internalAdProduct.getChiefUserInfo(), internalAdProduct);
    }

    public TypedCampaignInfo createDefaultInternalFreeCampaign() {
        ClientInfo internalAdProduct = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        return creatDefaultInternalFreeCampaign(internalAdProduct.getChiefUserInfo(), internalAdProduct);
    }

    public TypedCampaignInfo createDefaultInternalAutobudgetCampaign() {
        ClientInfo internalAdProduct = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        return creatDefaultInternalAutobudgetCampaign(internalAdProduct.getChiefUserInfo(), internalAdProduct);
    }

    public TypedCampaignInfo createDefaultTextCampaign() {
        return super.createTextCampaign(new TypedCampaignInfo(null, null, null));
    }

}
