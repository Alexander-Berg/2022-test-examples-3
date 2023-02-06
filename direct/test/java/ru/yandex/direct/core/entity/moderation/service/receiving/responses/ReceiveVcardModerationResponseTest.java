package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.sending.ModerationDecisionAdapter;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.VcardModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersPhoneflag;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.BANNER_VCARD;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveVcardModerationResponseTest extends AbstractModerationResponseTest
        <BannerAssetModerationMeta, Verdict, BannerAssetModerationResponse> {

    @Autowired
    private VcardModerationReceivingService receivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private AdGroupInfo adGroupInfo;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private VcardInfo vcardInfo;
    private TextBanner banner;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        vcardInfo = steps.vcardSteps().createVcard(campaignInfo);

        shard = clientInfo.getShard();

        banner = fullTextBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withLanguage(Language.RU_)
                .withVcardId(vcardInfo.getVcardId())
                .withVcardStatusModerate(BannerVcardStatusModerate.SENT);

        steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));

        testModerationRepository.createBannerVersion(shard, banner.getId(), getDefaultVersion());
        testModerationRepository.createVcardVersion(shard, banner.getId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long id, BannerAssetModerationResponse response) {
        List<TextBanner> banners = bannerTypedRepository.getStrictly(shard, Collections.singletonList(id),
                TextBanner.class);
        assertEquals(1, banners.size());
        BannersPhoneflag phoneFlag = ModerationDecisionAdapter.toBannersPhoneflag(response.getResult().getVerdict());
        assertEquals(BannerVcardStatusModerate.fromSource(phoneFlag), banners.get(0).getVcardStatusModerate());
    }

    @Override
    protected ModerationReceivingService<BannerAssetModerationResponse> getReceivingService() {
        return receivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        TextBanner newBanner = fullTextBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(BannerStatusModerate.SENT)
                .withVcardStatusModerate(BannerVcardStatusModerate.SENT);

        TextBanner anotherBanner =
                steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(newBanner))
                        .withVcardInfo(vcardInfo)
                        .getBanner();

        testModerationRepository.createBannerVersion(shard, anotherBanner.getId(), version);
        testModerationRepository.createVcardVersion(shard, anotherBanner.getId(), version);
        return newBanner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return BANNER_VCARD;
    }

    @Override
    protected long getDefaultVersion() {
        return 75000L;
    }

    @Override
    protected BannerAssetModerationResponse createResponse(long bid, ModerationDecision status,
                                                           @Nullable String language, long version, Map<String,
            String> flags, List<Long> minusRegions, ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        BannerAssetModerationResponse response = new BannerAssetModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(BANNER_VCARD);

        BannerAssetModerationMeta meta = new BannerAssetModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == No) {
            v.setReasons(DEFAULT_REASONS.stream().map(ModerationReasonDetailed::getId).collect(Collectors.toList()));
            v.setDetailedReasons(DEFAULT_REASONS);
        }

        response.setResult(v);

        return response;
    }

    @Override
    protected long getDefaultObjectId() {
        return banner.getId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected PpcPropertyName<Boolean> getRestrictedModePropertyName() {
        return PpcPropertyNames.RESTRICTED_VCARD_TRANSPORT_NEW_MODERATION;
    }

    @Override
    protected void checkStatusModerateNotChanged(long id) {
        List<TextBanner> banners = bannerTypedRepository.getStrictly(shard, Collections.singletonList(id),
                TextBanner.class);
        assertEquals(1, banners.size());
        assertEquals(BannerVcardStatusModerate.SENT, banners.get(0).getVcardStatusModerate());
    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteVcardsVersion(shard, getDefaultObjectId());
    }
}
