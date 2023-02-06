package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.sitelink.turbolanding.model.SitelinkTurboLanding;
import ru.yandex.direct.core.entity.turbolanding.model.StatusPostModerate;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaCounter;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaCounterContainer;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaGoal;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaGoalContainer;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingWithCountersAndGoals;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.testing.info.BannerTurboLandingInfo;
import ru.yandex.direct.core.testing.repository.TestTurboLandingRepository;
import ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset;
import ru.yandex.direct.dbschema.ppc.enums.TurbolandingsStatusmoderateforcpa;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;

public class TurboLandingSteps {

    private static AtomicLong turboLandingId = new AtomicLong(7777L);

    @Autowired
    private TurboLandingRepository turboLandingRepository;

    @Autowired
    private TestTurboLandingRepository testTurboLandingRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private DslContextProvider dslContextProvider;

    public static final Long COUNTER_ID_1 = 50001L;
    public static final Long USER_COUNTER_ID = 70001L;
    public static final Long GOAL_ID_1 = 1234L;
    public static final Long GOAL_ID_2 = 5678L;


    public static TurboLanding defaultTurboLanding(ClientId clientId) {
        TurboLanding turboLanding = new TurboLanding();

        turboLanding.setId(turboLandingId.incrementAndGet());
        turboLanding.setClientId(clientId.asLong());
        turboLanding.setMetrikaCounters("["
                + "{\"id\":77771,\"goals\":[777711,777712]},"
                + "{\"id\":77772,\"goals\":[777721,777722],\"isUserCounter\":true}"
                + "]");
        turboLanding.setName("тестовый турболендинг");
        turboLanding.setUrl("https://yandex.ru/turbo?text=vkbn&test=" + turboLandingId.get());
        turboLanding.setTurboSiteHref("project000.turbo.site");
        turboLanding.setIsChanged(false);
        turboLanding.setVersion(1L);
        turboLanding.setLastModeratedVersion(0L);
        turboLanding.setIsCpaModerationRequired(false);
        turboLanding.setPreviewHref("https://yandex.ru/turbo?text=vkbn&preview=" + turboLandingId.get());
        turboLanding.setStatusPostModerate(StatusPostModerate.YES);
        turboLanding.setPreset(TurbolandingsPreset.short_preset);
        return turboLanding;
    }

    private static TurboLandingWithCountersAndGoals defaultTurboLandingWithCountersAndGoals(ClientId clientId,
                                                                                            Set<TurboLandingMetrikaCounter> counters, Set<TurboLandingMetrikaGoal> goals) {
        return new TurboLandingWithCountersAndGoals(defaultTurboLanding(clientId))
                .withCounters(counters)
                .withGoals(goals);
    }

    private static TurboLandingMetrikaCounter turboLandingCounter(Long counterId, Boolean userCounter) {
        TurboLandingMetrikaCounter counter = new TurboLandingMetrikaCounter();

        counter.setId(counterId);
        counter.setIsUserCounter(userCounter);

        return counter;
    }

    private TurboLandingMetrikaGoal turboLandingGoal(Long goalId, Boolean conversionGoal) {
        TurboLandingMetrikaGoal goal = new TurboLandingMetrikaGoal();

        goal.setId(goalId);
        goal.setIsConversionGoal(conversionGoal);

        return goal;
    }

    public TurboLandingWithCountersAndGoals createDefaultTurbolandingWithCounterAndGoals(ClientId clientId) {

        TurboLandingWithCountersAndGoals turboLandingWithCountersAndGoals = defaultTurboLandingWithCountersAndGoals(
                clientId,
                asSet(
                        turboLandingCounter(COUNTER_ID_1, false),
                        turboLandingCounter(USER_COUNTER_ID, true)),
                asSet(
                        turboLandingGoal(GOAL_ID_1, false),
                        turboLandingGoal(GOAL_ID_2, true)
                )
        );

        return createTurboLandingWithCountersAndGoals(clientId, turboLandingWithCountersAndGoals);
    }

    public TurboLanding createDefaultTurboLanding(ClientId clientId) {
        int shard = shardHelper.getShardByClientId(clientId);
        return createDefaultTurboLanding(shard, clientId);
    }

    public TurboLanding createDefaultTurboLanding(int shard, ClientId clientId) {
        TurboLanding turboLanding = defaultTurboLanding(clientId);
        turboLandingRepository.add(shard, asList(turboLanding));

        return turboLanding;
    }

    public TurboLanding createTurboLanding(ClientId clientId, TurboLanding turboLanding) {
        int shard = shardHelper.getShardByClientId(clientId);
        turboLandingRepository.add(shard, asList(turboLanding));
        return turboLanding;
    }

    public TurboLandingWithCountersAndGoals createTurboLandingWithCountersAndGoals(
            ClientId clientId,
            TurboLandingWithCountersAndGoals turboLanding
    ) {
        int shard = shardHelper.getShardByClientId(clientId);
        turboLandingRepository.addOrUpdateTurboLandings(shard, asList(turboLanding));
        return turboLanding;
    }

    public SitelinkTurboLanding defaultSitelinkTurboLanding(ClientId clientId) {
        TurboLanding defaultTurboLanding = defaultTurboLanding(clientId);

        SitelinkTurboLanding sitelinkTurboLanding = new SitelinkTurboLanding();

        sitelinkTurboLanding.setId(defaultTurboLanding.getId());
        sitelinkTurboLanding.setClientId(defaultTurboLanding.getClientId());
        sitelinkTurboLanding.setMetrikaCounters(defaultTurboLanding.getMetrikaCounters());
        sitelinkTurboLanding.setName(defaultTurboLanding.getName());
        sitelinkTurboLanding.setUrl(defaultTurboLanding.getUrl());
        sitelinkTurboLanding.setIsChanged(defaultTurboLanding.getIsChanged());
        sitelinkTurboLanding.setVersion(defaultTurboLanding.getVersion());
        sitelinkTurboLanding.setIsCpaModerationRequired(defaultTurboLanding.getIsCpaModerationRequired());
        sitelinkTurboLanding.setLastModeratedVersion(defaultTurboLanding.getLastModeratedVersion());
        sitelinkTurboLanding.setPreviewHref(defaultTurboLanding.getPreviewHref());
        sitelinkTurboLanding.setStatusPostModerate(defaultTurboLanding.getStatusPostModerate());

        return sitelinkTurboLanding;
    }

    public SitelinkTurboLanding createDefaultSitelinkTurboLanding(ClientId clientId) {
        int shard = shardHelper.getShardByClientId(clientId);

        SitelinkTurboLanding sitelinkTurboLanding = defaultSitelinkTurboLanding(clientId);
        turboLandingRepository.add(shard, singletonList(sitelinkTurboLanding));

        return sitelinkTurboLanding;
    }

    public static OldBannerTurboLanding defaultBannerTurboLanding(ClientId clientId) {
        TurboLanding defaultTurboLanding = defaultTurboLanding(clientId);

        OldBannerTurboLanding bannerTurboLanding = new OldBannerTurboLanding();

        bannerTurboLanding.setId(defaultTurboLanding.getId());
        bannerTurboLanding.setClientId(defaultTurboLanding.getClientId());
        bannerTurboLanding.setMetrikaCounters(defaultTurboLanding.getMetrikaCounters());
        bannerTurboLanding.setName(defaultTurboLanding.getName());
        bannerTurboLanding.setUrl(defaultTurboLanding.getUrl());
        bannerTurboLanding.setIsChanged(defaultTurboLanding.getIsChanged());
        bannerTurboLanding.setIsCpaModerationRequired(defaultTurboLanding.getIsCpaModerationRequired());
        bannerTurboLanding.setVersion(defaultTurboLanding.getVersion());
        bannerTurboLanding.setLastModeratedVersion(defaultTurboLanding.getLastModeratedVersion());
        bannerTurboLanding.setPreviewHref(defaultTurboLanding.getPreviewHref());
        bannerTurboLanding.setStatusPostModerate(defaultTurboLanding.getStatusPostModerate());

        bannerTurboLanding.setStatusModerate(OldBannerTurboLandingStatusModerate.NEW);

        return bannerTurboLanding;
    }

    public OldBannerTurboLanding createDefaultBannerTurboLanding(ClientId clientId) {
        int shard = shardHelper.getShardByClientId(clientId);

        OldBannerTurboLanding bannerTurboLanding = defaultBannerTurboLanding(clientId);
        turboLandingRepository.add(shard, asList(bannerTurboLanding));

        return bannerTurboLanding;
    }

    public void setStatusModerateForCpa(ClientId clientId, Collection<Long> turbolandingIds,
                                        TurbolandingsStatusmoderateforcpa statusModerate) {
        int shard = shardHelper.getShardByClientId(clientId);
        testTurboLandingRepository.setTurbolandingStatusModerateForCpa(shard, turbolandingIds, statusModerate);
    }

    public void deleteDefaultTurboLanding(ClientId clientId) {
        int shard = shardHelper.getShardByClientId(clientId);
        testTurboLandingRepository.deleteTurboLandings(shard, asList(defaultTurboLanding(clientId).getId()));
    }

    public void deleteTurboLanding(ClientId clientId, Long turboLandingId) {
        int shard = shardHelper.getShardByClientId(clientId);
        testTurboLandingRepository.deleteTurboLandings(shard, asList(turboLandingId));
    }

    public void deleteTurboLandings(int shard, List<Long> turboLandingIds) {
        testTurboLandingRepository.deleteTurboLandings(shard, turboLandingIds);
    }

    public Collection<Long> getMetrikaCounters(Long cid, Collection<Long> bids) {
        int shard = shardHelper.getShardByCampaignId(cid);
        return testTurboLandingRepository.getCampMetrikaCounters(shard, cid, bids);
    }

    public Map<Long, Long> getMetrikaGoalCounters(Long cid, Collection<Long> goals) {
        int shard = shardHelper.getShardByCampaignId(cid);
        return testTurboLandingRepository
                .getCampMetrikaGoalCounters(shard, cid, goals);
    }

    public List<TurboLanding> getTurbolandingsById(ClientId clientId, @Nonnull Collection<Long> ids) {
        int shard = shardHelper.getShardByClientId(clientId);
        return turboLandingRepository.getClientTurboLandingsbyId(shard, clientId, ids);
    }

    public List<TurboLandingMetrikaCounterContainer> getTurbolandingCountersByTlId(ClientId clientId,
                                                                                   @Nonnull Collection<Long> turboLandingIds) {
        int shard = shardHelper.getShardByClientId(clientId);
        return turboLandingRepository.getTurboLandingMetrikaCounters(shard, turboLandingIds);
    }

    public List<TurboLandingMetrikaGoalContainer> getTurbolandingGoalsByTlId(ClientId clientId,
                                                                             @Nonnull Collection<Long> turboLandingIds) {
        int shard = shardHelper.getShardByClientId(clientId);
        return turboLandingRepository.getTurboLandingMetrikaGoals(shard, turboLandingIds);
    }

    public <T extends OldBannerWithTurboLanding> void addBannerToBannerTurbolandingsTableOrUpdate(Long cid,
                                                                                                  Collection<T> banners) {
        int shard = shardHelper.getShardByCampaignId(cid);
        turboLandingRepository.addBannerToBannerTurbolandingsTableOrUpdate(banners, dslContextProvider.ppc(shard));
    }

    public <T extends BannerWithTurboLanding> void addBannerToBannerTurbolandingsTableOrUpdateNew(Long cid,
                                                                                                  Collection<T> banners) {
        int shard = shardHelper.getShardByCampaignId(cid);
        testTurboLandingRepository.addBannerToBannerTurbolandingsTableOrUpdate(banners,
                dslContextProvider.ppc(shard));
    }

    public void linkBannerWithTurboLanding(
            int shard,
            BannerTurboLandingInfo bannerTurboLandingInfo) {
        testTurboLandingRepository.linkBannerWithTurboLanding(shard, bannerTurboLandingInfo);
    }
}
