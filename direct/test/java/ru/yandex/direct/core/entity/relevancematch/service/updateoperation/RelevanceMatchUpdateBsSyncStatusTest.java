package ru.yandex.direct.core.entity.relevancematch.service.updateoperation;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchUpdateOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateBsSyncStatusTest extends RelevanceMatchModificationBaseTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void before() {
        super.before();
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(StatusBsSynced.YES, RelevanceMatch.STATUS_BS_SYNCED);
        relevanceMatchRepository.update(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                Collections.singletonList(modelChanges.applyTo(getSavedRelevanceMatch())));
    }

    @Test
    public void prepareAndApply_ChangeAutoBudgetPriority_StatusBsSyncReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusBsSynced statusBsSynced = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(getSavedRelevanceMatch().getId()))
                .get(getSavedRelevanceMatch().getId()).getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.NO));
    }


    @Test
    public void prepareAndApply_ChangePrice_StatusBsSyncReset() {
        campaignsByIds.get(activeCampaign.getCampaignId()).withAutobudget(false);
        campaignsByIds.get(activeCampaign.getCampaignId()).getStrategy().setAutobudget(CampaignsAutobudget.NO);
        campaignsByIds.get(activeCampaign.getCampaignId()).getStrategy().setPlatform(CampaignsPlatform.BOTH);

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withPrice(BigDecimal.TEN);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusBsSynced statusBsSynced = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(getSavedRelevanceMatch().getId()))
                .get(getSavedRelevanceMatch().getId()).getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_ChangeHrefParam_StatusBsSyncNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(3)
                .withHrefParam1("123");

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusBsSynced statusBsSynced = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(getSavedRelevanceMatch().getId()))
                .get(getSavedRelevanceMatch().getId()).getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_ChangeRelevanceMatchCategories_StatusBsSyncNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(3)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusBsSynced statusBsSynced = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(getSavedRelevanceMatch().getId()))
                .get(getSavedRelevanceMatch().getId()).getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }
}
