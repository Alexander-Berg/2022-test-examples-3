package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.Set;

import junitparams.converters.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.MetrikaClient;

import static java.util.Collections.emptyMap;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultContentPromotionCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalAutobudgetCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalDistribCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalFreeCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultSmartCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTouchCampaign;

public class TypedCampaignStepsUnstubbed {
    final CampaignModifyRepository campaignModifyRepository;
    final CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    final DslContextProvider dslContextProvider;
    final MetrikaClient metrikaClient;

    @Autowired
    public TypedCampaignStepsUnstubbed(CampaignModifyRepository campaignModifyRepository,
                                       CampaignAddOperationSupportFacade campaignAddOperationSupportFacade,
                                       DslContextProvider dslContextProvider,
                                       MetrikaClient metrikaClient) {
        this.campaignModifyRepository = campaignModifyRepository;
        this.campaignAddOperationSupportFacade = campaignAddOperationSupportFacade;
        this.dslContextProvider = dslContextProvider;
        this.metrikaClient = metrikaClient;
    }

    public TypedCampaignInfo createDefaultCpmBannerCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createCpmBannerCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createCpmBannerCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                     CpmBannerCampaign campaign) {
        return createCpmBannerCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign));
    }

    private TypedCampaignInfo createCpmBannerCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultCpmBannerCampaignWithSystemFields());
    }

    public TypedCampaignInfo createDefaultTextCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createTextCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createTextCampaign(UserInfo operatorInfo, ClientInfo clientInfo, TextCampaign campaign) {
        return createTextCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign));
    }

    protected TypedCampaignInfo createTextCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultTextCampaignWithSystemFields());
    }

    public TypedCampaignInfo createDefaultDynamicCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createDynamicCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createDynamicCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                   DynamicCampaign campaign) {
        return createDynamicCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign));
    }

    private TypedCampaignInfo createDynamicCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultDynamicCampaignWithSystemFields());
    }

    public TypedCampaignInfo createDefaultSmartCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createSmartCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createSmartCampaign(UserInfo operatorInfo, ClientInfo clientInfo, SmartCampaign campaign) {
        return createSmartCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign));
    }

    private TypedCampaignInfo createSmartCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultSmartCampaignWithSystemFields());
    }

    public TypedCampaignInfo createDefaultContentPromotionCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null),
                defaultContentPromotionCampaignWithSystemFields());
    }

    public TypedCampaignInfo createCpmPriceCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                    CpmPriceCampaign campaign) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign), null);
    }

    public TypedCampaignInfo createDefaultMobileContentCampaign() {
        return createDefaultMobileContentCampaign(null, null);
    }

    public TypedCampaignInfo createDefaultMobileContentCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createMobileContentCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createMobileContentCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                         MobileContentCampaign campaign) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, campaign), null);
    }

    private TypedCampaignInfo createMobileContentCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultMobileContentCampaignWithSystemFields());
    }

    public TypedCampaignInfo createDefaultMcBannerCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createMcBannerCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null));
    }

    public TypedCampaignInfo createMcBannerCampaign(UserInfo userInfo, ClientInfo clientInfo,
                                                    McBannerCampaign campaign) {
        return createTypedCampaign(new TypedCampaignInfo(userInfo, clientInfo, null), campaign);
    }

    protected TypedCampaignInfo createMcBannerCampaign(TypedCampaignInfo campaignInfo) {
        return createTypedCampaign(campaignInfo, defaultMcBannerCampaignWithSystemFields());
    }

    protected TypedCampaignInfo createTypedCampaign(TypedCampaignInfo campaignInfo,
                                                    @Nullable CommonCampaign defaultCampaign) {
        if (campaignInfo.getClientInfo() == null) {
            throw new IllegalArgumentException("use stubbed TypedCampaignSteps");
        }

        if (campaignInfo.getCampaign() == null) {
            defaultCampaign.withUid(campaignInfo.getUid())
                    .withClientId(campaignInfo.getClientId().asLong());
            campaignInfo.setCampaign(defaultCampaign);
        }

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient,
                List.of(campaignInfo.getOperatorUid()), Set.of());
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                new RestrictedCampaignsAddOperationContainerImpl(campaignInfo.getShard(), campaignInfo.getOperatorUid(),
                        campaignInfo.getClientId(), campaignInfo.getUid(), campaignInfo.getUid(),
                        null, new CampaignOptions(), metrikaClientAdapter, emptyMap()
                );
        campaignModifyRepository.addCampaigns(dslContextProvider.ppc(campaignInfo.getShard()),
                addCampaignParametersContainer,
                List.of(campaignInfo.getCampaign()));
        campaignAddOperationSupportFacade
                .updateRelatedEntitiesInTransaction(dslContextProvider.ppc(campaignInfo.getShard()),
                        addCampaignParametersContainer, List.of(campaignInfo.getCampaign()));

        return campaignInfo;
    }

    public TypedCampaignInfo createDefaultTouchCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, null), defaultTouchCampaign());
    }

    public TypedCampaignInfo creatDefaultInternalDistribCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return creatDefaultInternalDistribCampaign(operatorInfo, clientInfo,
                defaultInternalDistribCampaignWithSystemFields(clientInfo));
    }

    public TypedCampaignInfo creatDefaultInternalDistribCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                                 InternalDistribCampaign internalDistribCampaign) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, internalDistribCampaign), null);
    }

    public TypedCampaignInfo creatDefaultInternalFreeCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return creatDefaultInternalFreeCampaign(operatorInfo, clientInfo,
                defaultInternalFreeCampaignWithSystemFields(clientInfo));
    }

    public TypedCampaignInfo creatDefaultInternalFreeCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                              InternalFreeCampaign internalFreeCampaign) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, internalFreeCampaign), null);
    }

    public TypedCampaignInfo creatDefaultInternalAutobudgetCampaign(UserInfo operatorInfo, ClientInfo clientInfo) {
        return creatDefaultInternalAutobudgetCampaign(operatorInfo, clientInfo,
                defaultInternalAutobudgetCampaignWithSystemFields(clientInfo));
    }

    public TypedCampaignInfo creatDefaultInternalAutobudgetCampaign(UserInfo operatorInfo, ClientInfo clientInfo,
                                                                    InternalAutobudgetCampaign internalAutobudgetCampaign) {
        return createTypedCampaign(new TypedCampaignInfo(operatorInfo, clientInfo, internalAutobudgetCampaign), null);
    }

}
