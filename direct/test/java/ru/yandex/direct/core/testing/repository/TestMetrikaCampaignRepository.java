package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_METRIKA_GOALS;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;

@Repository
public class TestMetrikaCampaignRepository {
    private final DslContextProvider ppcDslContextProvider;
    private final JooqMapperWithSupplier<CampMetrikaGoal> campMetrikaGoalMapper;

    @Autowired
    public TestMetrikaCampaignRepository(DslContextProvider ppcDslContextProvider) {
        this.ppcDslContextProvider = ppcDslContextProvider;
        this.campMetrikaGoalMapper = MetrikaCampaignRepository.createCampMetrikaGoalsMapper();
    }

    /**
     * Получает список целей метрики на кампании со статистикой по идентификаторам кампаний
     */
    public List<CampMetrikaGoal> getCampMetrikaGoalsByCampaignIds(
            int shard, ClientId clientId, Collection<Long> campaignIds) {
        if (campaignIds.isEmpty()) {
            return List.of();
        }

        var goals = ppcDslContextProvider.ppc(shard)
                .select(campMetrikaGoalMapper.getFieldsToRead())
                .from(CAMP_METRIKA_GOALS)
                .join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(CAMP_METRIKA_GOALS.CID))
                .where(CAMPAIGNS.CLIENT_ID.eq(clientId.asLong()))
                .and(CAMP_METRIKA_GOALS.CID.in(campaignIds))
                .fetch(campMetrikaGoalMapper::fromDb);

        return StreamEx.of(goals)
                .map(goal -> goal.withId(new CampMetrikaGoalId()
                        .withCampaignId(goal.getCampaignId())
                        .withGoalId(goal.getGoalId())))
                .toList();
    }
}
