package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupIdsBySelectionCriteriaWithNegativeKeywordsTest {
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private AdGroupInfo adGroup;
    private MinusKeywordsPackInfo libraryMinusKeywordsPack;
    private AdGroupInfo adGroupWithNegativeKeyword;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo);
        adGroupWithNegativeKeyword = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                clientInfo);
    }

    @Test
    public void getAdGroupIdsByNegativeKeywordsIds_OneAdGroupFiltered() {
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(adGroup.getAdGroupId(), adGroupWithNegativeKeyword.getAdGroupId())
                .withNegativeKeywordSharedSetIds(libraryMinusKeywordsPack.getMinusKeywordPackId());
        List<Long> adGroupIdsBySelectionCriteria =
                adGroupRepository.getAdGroupIdsBySelectionCriteria(clientInfo.getShard(),
                        selectionCriteria,
                        LimitOffset.maxLimited());
        assertThat(adGroupIdsBySelectionCriteria, contains(adGroupWithNegativeKeyword.getAdGroupId()));
    }

    @Test
    public void getAdGroupIdsByNegativeKeywordsIds_AllAdGroupsFiltered() {
        MinusKeywordsPackInfo libraryMinusKeywordsPack2 =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(adGroup.getAdGroupId(), adGroupWithNegativeKeyword.getAdGroupId())
                .withNegativeKeywordSharedSetIds(libraryMinusKeywordsPack2.getMinusKeywordPackId());
        List<Long> adGroupIdsBySelectionCriteria =
                adGroupRepository.getAdGroupIdsBySelectionCriteria(clientInfo.getShard(),
                        selectionCriteria,
                        LimitOffset.maxLimited());
        assertThat(adGroupIdsBySelectionCriteria, hasSize(0));
    }

    @Test
    public void getAdGroupIdsByNegativeKeywordsIds_SeveralNegativeSets() {
        MinusKeywordsPackInfo libraryMinusKeywordsPack2 =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo);

        AdGroupInfo adGroupWithNegativeKeyword2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack2.getMinusKeywordPackId())),
                clientInfo);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(adGroup.getAdGroupId(), adGroupWithNegativeKeyword.getAdGroupId(),
                        adGroupWithNegativeKeyword2.getAdGroupId())
                .withNegativeKeywordSharedSetIds(libraryMinusKeywordsPack.getMinusKeywordPackId(),
                        libraryMinusKeywordsPack2.getMinusKeywordPackId());
        List<Long> adGroupIdsBySelectionCriteria =
                adGroupRepository.getAdGroupIdsBySelectionCriteria(clientInfo.getShard(),
                        selectionCriteria,
                        LimitOffset.maxLimited());
        assertThat(adGroupIdsBySelectionCriteria, containsInAnyOrder(adGroupWithNegativeKeyword.getAdGroupId(),
                adGroupWithNegativeKeyword2.getAdGroupId()));
    }
}
