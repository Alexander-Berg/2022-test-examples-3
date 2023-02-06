package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat;
import static ru.yandex.direct.dbschema.ppc.enums.ModReasonsType.banner;
import static ru.yandex.direct.utils.StringUtils.joinLongsToString;

@ParametersAreNonnullByDefault
public class ReceiveModerationResponseBaseTest {

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    protected static BannerModerationResponse createResponse(long bid,
                                                             ModerationObjectType type,
                                                             ModerationDecision status,
                                                             @Nullable String language,
                                                             long version,
                                                             Map<String, String> flags,
                                                             List<Long> minusRegions,
                                                             ClientInfo clientInfo,
                                                             List<ModerationReasonDetailed> reasons) {
        BannerModerationResponse r = new BannerModerationResponse();

        r.setService(ModerationServiceNames.DIRECT_SERVICE);
        r.setType(type);

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);

        r.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);
        v.setMinusRegions(minusRegions);

        if (status == No) {
            v.setReasons(reasons.stream().map(ModerationReasonDetailed::getId).collect(Collectors.toList()));
            v.setDetailedReasons(reasons);
        }
        v.setFlags(flags);
        v.setLang(language);

        r.setResult(v);

        return r;
    }

    protected Language defaultLanguage() {
        return Language.RU_;
    }


    protected OldBanner checkInDb(int shard, long bid, BannerModerationResponse response,
                                  List<Long> expectedMinusRegions,
                                  List<ModerationReasonDetailed> expectedReasons) {
        List<OldBanner> dbBanners = bannerRepository.getBanners(shard, singleton(bid));

        assertNotNull(dbBanners);
        assertEquals(1, dbBanners.size());

        OldBanner b = dbBanners.get(0);
        BannersPerformanceStatusmoderate bannerPerformanceStatus =
                testBannerCreativeRepository.getBannerPerformanceStatus(shard, bid);

        String minusGeo = testBannerRepository.getMinusGeo(shard, bid);

        List<String> modReasons = testBannerRepository.getModReasons(shard, bid, banner);

        assertNotNull(bannerPerformanceStatus);

        assertEquals(StatusBsSynced.NO, b.getStatusBsSynced());

        if (expectedMinusRegions.isEmpty()) {
            assertNull(minusGeo);
        } else {
            assertEquals(joinLongsToString(expectedMinusRegions), minusGeo);
        }

        if (response.getResult().getVerdict() == ModerationDecision.Yes) {
            assertEquals(OldBannerStatusModerate.YES, b.getStatusModerate());
            assertEquals(OldBannerStatusPostModerate.YES, b.getStatusPostModerate());
            assertEquals(BannersPerformanceStatusmoderate.Yes, bannerPerformanceStatus);
            assertEquals(reasonsToDbFormat(null), modReasons.get(0));
        } else {
            assertEquals(OldBannerStatusModerate.NO, b.getStatusModerate());

            assertEquals(response.getResult().getMinusRegions().isEmpty() ? OldBannerStatusPostModerate.NO :
                    OldBannerStatusPostModerate.REJECTED, b.getStatusPostModerate());

            assertEquals(BannersPerformanceStatusmoderate.No, bannerPerformanceStatus);
            assertEquals(1, modReasons.size());
            assertEquals(reasonsToDbFormat(expectedReasons), modReasons.get(0));
        }

        return b;
    }

}
