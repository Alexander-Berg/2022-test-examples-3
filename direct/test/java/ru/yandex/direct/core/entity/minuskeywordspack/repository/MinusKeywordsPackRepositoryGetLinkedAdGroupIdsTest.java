package ru.yandex.direct.core.entity.minuskeywordspack.repository;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPackRepositoryGetLinkedAdGroupIdsTest {

    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private Long adGroupId1;
    private Long adGroupId2;
    private Long adGroupId3;

    private MinusKeywordsPack pack1;
    private MinusKeywordsPack pack2;
    private MinusKeywordsPack pack3;
    private List<Long> allPackIds;
    private int shard;

    @Before
    public void before() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        clientId = client.getClientId();
        shard = client.getShard();

        pack1 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("pack one"), client).getMinusKeywordsPack();
        pack2 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("pack two"), client).getMinusKeywordsPack();
        pack3 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("pack three"), client).getMinusKeywordsPack();
        allPackIds = asList(pack1.getId(), pack2.getId(), pack3.getId());

        adGroupId1 = steps.adGroupSteps().createActiveTextAdGroup(client).getAdGroupId();
        adGroupId2 = steps.adGroupSteps().createActiveTextAdGroup(client).getAdGroupId();
        adGroupId3 = steps.adGroupSteps().createActiveTextAdGroup(client).getAdGroupId();

        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, pack1.getId(), adGroupId1);
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, pack2.getId(), adGroupId2);
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, pack3.getId(), adGroupId3);
    }

    @Test
    public void getLinkedAdGroupIds_FilteredByIds() {
        Set<Long> linkedAdGroupIds = minusKeywordsPackRepository.getLinkedAdGroupIds(shard, clientId,
                asList(pack1.getId(), pack3.getId()), null, null);
        assertThat(linkedAdGroupIds, contains(adGroupId1, adGroupId3));
    }

    @Test
    public void getLinkedAdGroupIds_FilteredByNameContains() {
        Set<Long> linkedAdGroupIds = minusKeywordsPackRepository.getLinkedAdGroupIds(shard, clientId,
                allPackIds, "t", null);
        assertThat(linkedAdGroupIds, contains(adGroupId2, adGroupId3));
    }

    @Test
    public void getLinkedAdGroupIds_FilteredByNameNotContains() {
        Set<Long> linkedAdGroupIds = minusKeywordsPackRepository.getLinkedAdGroupIds(shard, clientId,
                allPackIds, null, "three");
        assertThat(linkedAdGroupIds, contains(adGroupId1, adGroupId2));
    }

    @Test
    public void getLinkedAdGroupIds_FilteredByNameContainsAndNotContains() {
        Set<Long> linkedAdGroupIds = minusKeywordsPackRepository.getLinkedAdGroupIds(shard, clientId,
                allPackIds, "t", "e");
        assertThat(linkedAdGroupIds, contains(adGroupId2));
    }

    @Test
    public void getLinkedAdGroupIds_FilteredByAllParams() {
        Set<Long> linkedAdGroupIds = minusKeywordsPackRepository.getLinkedAdGroupIds(shard, clientId,
                asList(pack1.getId(), pack2.getId()), "pack", "two");
        assertThat(linkedAdGroupIds, contains(adGroupId1));
    }
}
