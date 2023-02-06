package ru.yandex.direct.core.entity.relevancematch.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchModification;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchUpdateContainer;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.testing.steps.RelevanceMatchSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchMapping.relevanceMatchesToCoreModelChanges;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

public abstract class RelevanceMatchModificationBaseTest extends RelevanceMatchOperationBaseTest {
    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    protected RelevanceMatchSteps relevanceMatchSteps;
    protected Map<Long, Long> adGroupIdsByRelevanceMatchIds;
    protected Map<Long, RelevanceMatch> relevanceMatchByIds;
    protected Set<Long> adGroupIds;
    @Autowired
    private DslContextProvider dslContextProvider;
    private RelevanceMatch savedRelevanceMatch;

    @Override
    public void before() {
        super.before();
        RelevanceMatch relevanceMatch = getValidRelevanceMatch()
                .withIsSuspended(false)
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withCampaignId(defaultAdGroup.getCampaignId())
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(now());

        List<Long> relevanceMatchIds = relevanceMatchSteps
                .addRelevanceMatchToAdGroup(Collections.singletonList(relevanceMatch), defaultAdGroup);

        this.savedRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(relevanceMatchIds.get(0)))
                .get(relevanceMatchIds.get(0));

        relevanceMatchByIds = new HashMap<>();
        relevanceMatchByIds.put(savedRelevanceMatch.getId(), savedRelevanceMatch);

        adGroupIdsByRelevanceMatchIds = new HashMap<>();
        adGroupIdsByRelevanceMatchIds.put(relevanceMatch.getId(), relevanceMatch.getAdGroupId());

        adGroupIds = new HashSet<>(adGroupByIds.keySet());
    }

    public RelevanceMatch getSavedRelevanceMatch() {
        return savedRelevanceMatch;
    }

    protected AdGroup getAdGroup() {
        return defaultTextAdGroup(activeCampaign.getCampaignId());
    }

    protected RelevanceMatchUpdateOperation getFullUpdateOperation(RelevanceMatch relevanceMatch) {
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                RelevanceMatchUpdateContainer.createRelevanceMatchUpdateOperationContainer(
                        getOperatorUid(), getClientId(), campaignsByIds,
                        campaignIdsByAdGroupIds, adGroupIdsByRelevanceMatchIds, relevanceMatchByIds
                );
        return relevanceMatchService
                .createFullUpdateOperation(defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getClientInfo().getClient().getChiefUid(),
                        defaultUser.getUid(),
                        relevanceMatchesToCoreModelChanges(Collections.singletonList((relevanceMatch))),
                        relevanceMatchUpdateOperationContainer, false, null);
    }

    protected RelevanceMatchDeleteOperation getFullDeleteOperation(List<Long> ids) {
        return relevanceMatchService.createFullDeleteOperation(
                getClientId(), getOperatorUid(), ids, relevanceMatchByIds);
    }

    protected RelevanceMatchModifyOperation getFullModifyOperation(
            RelevanceMatchModification relevanceMatchModification) {
        return relevanceMatchService.createFullModifyOperation(
                getClientId(), defaultUser.getUid(), getOperatorUid(), relevanceMatchModification, false, null);
    }

    protected void setSuspended(long id, boolean isSuspended) {
        ModelChanges<RelevanceMatch> modelChanges = new ModelChanges<>(id, RelevanceMatch.class);
        modelChanges.process(isSuspended, RelevanceMatch.IS_SUSPENDED);
        relevanceMatchRepository
                .update(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        Collections.singletonList(modelChanges.applyTo(savedRelevanceMatch)));
    }

}
