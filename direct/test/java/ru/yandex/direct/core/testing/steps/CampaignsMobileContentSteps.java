package ru.yandex.direct.core.testing.steps;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.InsertValuesStep5;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.converter.MobileContentConverter;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsMobileContentRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MOBILE_CONTENT;

@ParametersAreNonnullByDefault
public class CampaignsMobileContentSteps {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public CampaignsMobileContentSteps(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void createCampaignsMobileContent(int shard, Long campaignId,
                                             Long mobileAppId,
                                             Set<MobileAppDeviceTypeTargeting> mobileAppDeviceTypeTargetings,
                                             Set<MobileAppNetworkTargeting> mobileAppNetworkTargetings,
                                             Set<MobileAppAlternativeStore> mobileAppAlternativeStores) {
        InsertValuesStep5<CampaignsMobileContentRecord, Long, Long, String, String, String> step =
                dslContextProvider.ppc(shard)
                .insertInto(CAMPAIGNS_MOBILE_CONTENT)
                .columns(CAMPAIGNS_MOBILE_CONTENT.CID, CAMPAIGNS_MOBILE_CONTENT.MOBILE_APP_ID,
                        CAMPAIGNS_MOBILE_CONTENT.DEVICE_TYPE_TARGETING, CAMPAIGNS_MOBILE_CONTENT.NETWORK_TARGETING,
                        CAMPAIGNS_MOBILE_CONTENT.ALTERNATIVE_APP_STORES);
        step = step.values(campaignId, mobileAppId,
                MobileContentConverter.deviceTypeTargetingToDb(mobileAppDeviceTypeTargetings),
                MobileContentConverter.networkTargetingsToDb(mobileAppNetworkTargetings),
                MobileContentConverter.altAppStoresToDb(mobileAppAlternativeStores));
        step.onDuplicateKeyIgnore().execute();
    }
}
