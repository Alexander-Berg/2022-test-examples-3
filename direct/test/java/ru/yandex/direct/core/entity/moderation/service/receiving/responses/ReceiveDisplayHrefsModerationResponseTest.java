package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.DisplayHrefsModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerDisplayHrefsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerDisplayHrefsRecord;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.DISPLAYHREFS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveDisplayHrefsModerationResponseTest extends AbstractModerationResponseTest<DisplayHrefsModerationMeta,
        Verdict,
        DisplayHrefsModerationResponse> {

    private static final long DEFAULT_VERSION = 30000L;
    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    OldBannerRepository bannerRepository;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    @Autowired
    DisplayHrefsModerationReceivingService displayHrefsModerationReceivingService;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withDisplayHref("New displayhref")
                                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.SENT).withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createDisplayHrefsVersion(shard, banner.getId(), DEFAULT_VERSION);
    }

    private BannerDisplayHrefsRecord getExpectedDisplayHrefs(DisplayHrefsModerationResponse response) {
        String verdict = StringUtils.capitalize(response.getResult().getVerdict().toString().toLowerCase());

        BannerDisplayHrefsRecord record = new BannerDisplayHrefsRecord();

        record.setBid(response.getMeta().getBannerId());
        record.setDisplayHref("New displayhref");
        record.setStatusmoderate(BannerDisplayHrefsStatusmoderate.valueOf(verdict));

        return record;
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, DisplayHrefsModerationResponse response) {
        List<BannerDisplayHrefsRecord> dbDisplayhrefs = testModerationRepository.getBannerDisplayHrefs(shard,
                Collections.singleton(bid));

        assumeThat(dbDisplayhrefs, not(empty()));

        BannerDisplayHrefsRecord dbRecord = dbDisplayhrefs.get(0);
        BannerDisplayHrefsRecord expectedDisplayHrefs = getExpectedDisplayHrefs(response);

        assertEquals(dbRecord.getStatusmoderate(), expectedDisplayHrefs.getStatusmoderate());
    }

    @Override
    protected ModerationReceivingService<DisplayHrefsModerationResponse> getReceivingService() {
        return displayHrefsModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withDisplayHref("New displayhref")
                                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.SENT).withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createDisplayHrefsVersion(shard, banner.getId(), version);
        return banner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return DISPLAYHREFS;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected DisplayHrefsModerationResponse createResponse(long bid, ModerationDecision status,
                                                            @Nullable String language, long version, Map<String,
            String> flags, List<Long> minusRegions, ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        DisplayHrefsModerationResponse response = new DisplayHrefsModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(DISPLAYHREFS);

        DisplayHrefsModerationMeta meta = new DisplayHrefsModerationMeta();
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
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteDisplayhrefsVersion(shard, banner.getId());
    }
}
