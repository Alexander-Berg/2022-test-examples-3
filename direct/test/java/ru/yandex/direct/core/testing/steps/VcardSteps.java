package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardIncVcardId.SHARD_INC_VCARD_ID;

public class VcardSteps {

    private final CampaignSteps campaignSteps;
    private final VcardRepository vcardRepository;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public VcardSteps(CampaignSteps campaignSteps,
                      DslContextProvider dslContextProvider,
                      VcardRepository vcardRepository) {
        this.campaignSteps = campaignSteps;
        this.vcardRepository = vcardRepository;
        this.dslContextProvider = dslContextProvider;
    }

    public VcardInfo createFullVcard() {
        return createVcard(fullVcard());
    }

    public VcardInfo createVcard(Vcard vcard) {
        return createVcard(new VcardInfo().withVcard(vcard));
    }

    public VcardInfo createVcard(Vcard vcard, ClientInfo clientInfo) {
        return createVcard(vcard, new CampaignInfo().withClientInfo(clientInfo));
    }

    public VcardInfo createVcard(CampaignInfo campaignInfo) {
        Vcard vcard = fullVcard(campaignInfo.getCampaignId(), campaignInfo.getUid());
        return createVcard(vcard, campaignInfo);
    }

    public VcardInfo createVcard(Vcard vcard, CampaignInfo campaignInfo) {
        return createVcard(new VcardInfo()
                .withCampaignInfo(campaignInfo)
                .withVcard(vcard));
    }

    public VcardInfo createVcard(VcardInfo vcardInfo) {
        if (vcardInfo.getVcard() == null) {
            vcardInfo.withVcard(fullVcard((Long) null, null));
        }
        if (vcardInfo.getVcardId() == null) {
            campaignSteps.createCampaign(vcardInfo.getCampaignInfo());
            vcardInfo.getVcard()
                    .withUid(vcardInfo.getUid())
                    .withCampaignId(vcardInfo.getCampaignId());
            vcardRepository.addVcards(vcardInfo.getShard(), vcardInfo.getUid(),
                    vcardInfo.getClientId(), singletonList(vcardInfo.getVcard()));
        }
        return vcardInfo;
    }

    public Set<Long> getExistingIdsInMetabase(Collection<Long> vcardIds) {
        return dslContextProvider.ppcdict()
                .select(SHARD_INC_VCARD_ID.VCARD_ID)
                .from(SHARD_INC_VCARD_ID)
                .where(SHARD_INC_VCARD_ID.VCARD_ID.in(vcardIds))
                .fetchSet(SHARD_INC_VCARD_ID.VCARD_ID);
    }
}
