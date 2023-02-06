package ru.yandex.direct.core.entity.minuskeywordspack.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MinusKeywordsPackRepositoryTest {

    private static final String PACK_LINKED_ERROR_MESSAGE = "linked to adGroup or campaign";
    private static final String PACK_NOT_EXIST_ERROR_MESSAGE =
            "Some minus keyword packs don't exist at the time of taking the lock";

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private MinusKeywordsPackRepository repoUnderTest;

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    private int shard;
    private Configuration jooqConfig;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private MinusKeywordsPack libraryPack;
    private MinusKeywordsPack privatePack;
    private List<Long> libraryPacks;
    private CampaignInfo defaultCampaign;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        MinusKeywordsPackInfo libraryPackInfo = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack();
        shard = libraryPackInfo.getShard();
        libraryPack = libraryPackInfo.getMinusKeywordsPack();
        clientId = libraryPackInfo.getClientId();
        clientInfo = libraryPackInfo.getClientInfo();
        jooqConfig = dslContextProvider.ppc(shard).configuration();

        MinusKeywordsPackInfo privatePackInfo =
                steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo);
        privatePack = privatePackInfo.getMinusKeywordsPack();

        libraryPacks = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPacks(clientInfo, 2);

        defaultCampaign = steps.campaignSteps().createActiveCampaign(clientInfo);
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(defaultCampaign);
    }

    @Test
    public void getLibraryMinusKeywordsPacksByKeywords_DoNotReturnPrivatePacks() {
        ImmutableList<String> mks1 = generateMinusKeywords();
        ImmutableList<String> mks2 = generateMinusKeywords();

        steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(privateMinusKeywordsPack().withMinusKeywords(mks1), clientInfo);
        MinusKeywordsPackInfo pack2 = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(libraryMinusKeywordsPack().withMinusKeywords(mks2), clientInfo);

        Map<ImmutableList<String>, MinusKeywordsPack> packs =
                repoUnderTest.getLibraryMinusKeywordsPacksByKeywords(shard, clientId, asList(mks1, mks2));
        assertSoftly(assertions -> {
            assertions.assertThat(packs).hasSize(1);
            assertions.assertThat(packs.get(mks2).getId()).isEqualTo(pack2.getMinusKeywordPackId());
        });
    }

    @Test
    public void update_MinusKeywordsPackUpdated() {
        List<AppliedChanges<MinusKeywordsPack>> appliedChanges =
                getMinusKeywordsChanges(libraryPack, "new name", singletonList("new"));

        dslContextProvider.ppc(shard).transaction(ctx -> repoUnderTest.update(ctx, appliedChanges));

        Map<Long, MinusKeywordsPack> packs =
                repoUnderTest.getMinusKeywordsPacks(shard, clientId, singletonList(libraryPack.getId()));
        assumeThat("должен вернуться один набор минус слов", packs.keySet(), hasSize(1));
        MinusKeywordsPack pack = packs.get(libraryPack.getId());

        Assertions.assertThat(pack)
                .hasFieldOrPropertyWithValue(MinusKeywordsPack.ID.name(), libraryPack.getId())
                .hasFieldOrPropertyWithValue(MinusKeywordsPack.NAME.name(), "new name")
                .hasFieldOrPropertyWithValue(MinusKeywordsPack.MINUS_KEYWORDS.name(), singletonList("new"))
                .extracting(MinusKeywordsPack.HASH.name()).isNotNull();
    }

    @Test
    public void createPrivateMinusKeywords_EmptyList_NotThrowAnyException() {
        assertThatCode(() -> repoUnderTest.createPrivateMinusKeywords(shard, clientId, emptyList()))
                .doesNotThrowAnyException();
    }

    @Test
    public void createPrivateMinusKeywords_AllMinusWordsIsDuplicates_CreateOnlyOne() {
        MinusKeywordsPack mw1 = generateNewPrivatePack();
        MinusKeywordsPack mw2 = copyPack(mw1);
        MinusKeywordsPack mw3 = copyPack(mw1);

        repoUnderTest.createPrivateMinusKeywords(shard, clientId, asList(mw1, mw2, mw3));

        assertSoftly(assertions -> {
            assertThat(mw1, beanDiffer(mw2));
            assertThat(mw1, beanDiffer(mw3));
        });
    }

    @Test
    public void createPrivateMinusKeywords_AlreadyCreatedMinusWordsPack_UseExistId() {
        MinusKeywordsPack copy1 = copyPack(privatePack);
        MinusKeywordsPack copy2 = copyPack(privatePack);
        repoUnderTest.createPrivateMinusKeywords(shard, clientId, asList(copy1, copy2));

        assertSoftly(assertions -> {
            assertions.assertThat(copy1.getId()).isEqualTo(privatePack.getId());
            assertions.assertThat(copy2.getId()).isEqualTo(privatePack.getId());
        });
    }

    @Test
    public void createPrivateMinusKeywords_UniqueMinusWords_CreateAllAndDoNotChangeOrder() {
        MinusKeywordsPack mw1 = generateNewPrivatePack();
        MinusKeywordsPack mw2 = generateNewPrivatePack();

        List<MinusKeywordsPack> minusWordsList = asList(mw1, mw2);
        List<Long> ids = repoUnderTest.createPrivateMinusKeywords(shard, clientId, minusWordsList);

        assertSoftly(assertions -> {
            assertions.assertThat(mw1.getId()).isNotEqualTo(mw2.getId());
            assertions.assertThat(mapList(minusWordsList, MinusKeywordsPack::getId)).isEqualTo(ids);
        });
    }

    /**
     * Все вместе: уже созданные наборы, несозданные повторяющиеся наборы, уникальные
     */
    @Test
    public void createPrivateMinusKeywords_Complex() {
        MinusKeywordsPack createdBefore1 =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(generateNewPrivatePack(), clientInfo)
                        .getMinusKeywordsPack();
        MinusKeywordsPack createdBefore2 =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(generateNewPrivatePack(), clientInfo)
                        .getMinusKeywordsPack();

        MinusKeywordsPack mw1Copy = copyPack(createdBefore1);
        MinusKeywordsPack mw2Copy = copyPack(createdBefore2);

        MinusKeywordsPack mw3 = generateNewPrivatePack();
        MinusKeywordsPack mw3Copy1 = copyPack(mw3);
        MinusKeywordsPack mw3Copy2 = copyPack(mw3);

        MinusKeywordsPack mw4 = generateNewPrivatePack();
        MinusKeywordsPack mw5 = generateNewPrivatePack();

        List<MinusKeywordsPack> minusKeywordsPacks =
                asList(createdBefore1, mw1Copy, createdBefore2, mw2Copy, mw3, mw3Copy1, mw3Copy2, mw4, mw5);
        List<Long> ids = repoUnderTest.createPrivateMinusKeywords(shard, clientId, minusKeywordsPacks);

        List<Long> uniqueIds = ids.stream().distinct().collect(toList());

        assertSoftly(assertions -> {
            assertThat(createdBefore1, beanDiffer(mw1Copy));
            assertThat(createdBefore2, beanDiffer(mw2Copy));
            assertThat(mw3, beanDiffer(mw3Copy1));
            assertThat(mw3, beanDiffer(mw3Copy2));
            assertions.assertThat(uniqueIds.size()).isEqualTo(5);
            assertions.assertThat(mapList(minusKeywordsPacks, MinusKeywordsPack::getId)).isEqualTo(ids);
        });
    }

    @Test
    public void createPrivateMinusKeywords_DuplicateWithLibraryPack_DoNotDeduplicate() {
        MinusKeywordsPack newPrivatePack = generateNewPrivatePack();
        MinusKeywordsPackInfo minusKeywordsPack = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(
                        libraryMinusKeywordsPack().withMinusKeywords(newPrivatePack.getMinusKeywords()),
                        clientInfo);

        List<Long> ids = repoUnderTest.createPrivateMinusKeywords(shard, clientId, singletonList(newPrivatePack));
        assertThat(ids.get(0), not(minusKeywordsPack.getMinusKeywordPackId()));
    }

    @Test
    public void delete_PackLinkedToAdGroup_ThrowException() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup.getAdGroupId());

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_LINKED_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackLinkedToSeveralAdGroups_ThrowException() {
        AdGroupInfo adGroup1 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo adGroup2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup1.getAdGroupId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup2.getAdGroupId());

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_LINKED_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackLinkedToCampaign_ThrowException() {
        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign.getCampaignId());

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_LINKED_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackLinkedToSeveralCampaigns_ThrowException() {
        CampaignInfo campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign1.getCampaignId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign2.getCampaignId());

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_LINKED_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackLinkedToAdGroupAndCampaign_ThrowException() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup.getAdGroupId());
        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign.getCampaignId());

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_LINKED_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackOfAnotherClient_ThrowException() {
        ClientInfo anotherClient = steps.clientSteps().createDefaultClient();
        assumeThat(anotherClient.getShard(), is(shard));
        MinusKeywordsPackInfo minusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(anotherClient);

        Throwable thrown = catchThrowable(() -> repoUnderTest
                .delete(shard, clientId, asList(minusKeywordsPack.getMinusKeywordPackId(), libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_NOT_EXIST_ERROR_MESSAGE);

        checkClientPacksExist(anotherClient.getClientId(), singletonList(minusKeywordsPack.getMinusKeywordPackId()));
        checkClientPacksExist(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_PackAlreadyDeleted_ThrowException() {
        repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId()));

        Throwable thrown =
                catchThrowable(() -> repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId())));
        assertIllegalStateException(thrown, PACK_NOT_EXIST_ERROR_MESSAGE);

        checkClientPacksDeleted(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_Successful() {
        repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId()));
        checkClientPacksDeleted(clientId, singletonList(libraryPack.getId()));
    }

    @Test
    public void delete_SeveralPacks() {
        repoUnderTest.delete(shard, clientId, singletonList(libraryPack.getId()));

        MinusKeywordsPackInfo packLinkedToAdGroup =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, packLinkedToAdGroup.getMinusKeywordPackId(),
                        adGroup.getAdGroupId());

        MinusKeywordsPackInfo packLinkedToCampaign =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo);
        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packLinkedToCampaign.getMinusKeywordPackId(),
                        campaign.getCampaignId());

        MinusKeywordsPackInfo packToDelete =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo);

        Throwable thrown = catchThrowable(() -> repoUnderTest.delete(shard, clientId,
                asList(libraryPack.getId(), packLinkedToAdGroup.getMinusKeywordPackId(),
                        packLinkedToCampaign.getMinusKeywordPackId(), packToDelete.getMinusKeywordPackId())));
        assertIllegalStateException(thrown, PACK_NOT_EXIST_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(packToDelete.getMinusKeywordPackId()));
        checkClientPacksExist(clientId, asList(packLinkedToAdGroup.getMinusKeywordPackId(),
                packLinkedToCampaign.getMinusKeywordPackId()));
    }

    @Test
    public void delete_PrivatePacks_ThrowException() {
        Throwable thrown = catchThrowable(
                () -> repoUnderTest.delete(shard, clientId, asList(libraryPack.getId(), privatePack.getId())));
        assertIllegalStateException(thrown, PACK_NOT_EXIST_ERROR_MESSAGE);

        checkClientPacksExist(clientId, singletonList(privatePack.getId()));
    }

    @Test
    public void deleteAdGroupToPackLinks_LinksDeleted() {
        Long adGroupId = steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroupId);
        repoUnderTest.deleteAdGroupToPackLinks(shard, singletonList(adGroupId));

        List<Long> adGroupLinkedPacks = repoUnderTest
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(adGroupId))
                .get(adGroupId);
        assertThat(adGroupLinkedPacks, empty());
    }

    @Test
    public void getLinkedAdGroupIdToCampaignIdMap_MwIdLinkedToSeveralAdGroups() {
        AdGroupInfo adGroup1 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo adGroup2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup1.getAdGroupId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, libraryPack.getId(), adGroup2.getAdGroupId());

        Map<Long, Long> linkedAdGroupIdToCampaignIdMap =
                repoUnderTest.getLinkedAdGroupIdToCampaignIdMap(shard, singletonList(libraryPack.getId()));
        assertSoftly(assertions -> {
            assertions.assertThat(linkedAdGroupIdToCampaignIdMap).hasSize(2);
            assertions.assertThat(linkedAdGroupIdToCampaignIdMap.get(adGroup1.getAdGroupId()))
                    .isEqualTo(adGroup1.getCampaignId());
            assertions.assertThat(linkedAdGroupIdToCampaignIdMap.get(adGroup2.getAdGroupId()))
                    .isEqualTo(adGroup2.getCampaignId());
        });
    }

    @Test
    public void getLinkedCampaignIdToTypeMap_MwIdLinkedToSeveralCampaigns() {
        CampaignInfo campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign1.getCampaignId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, libraryPack.getId(), campaign2.getCampaignId());

        Map<Long, CampaignType> linkedCampaignIdToTypeMap =
                repoUnderTest.getLinkedCampaignIdToTypeMap(shard, singletonList(libraryPack.getId()));
        assertSoftly(assertions -> {
            assertions.assertThat(linkedCampaignIdToTypeMap).hasSize(2);
            assertions.assertThat(linkedCampaignIdToTypeMap.get(campaign1.getCampaignId()))
                    .isEqualTo(campaign1.getCampaign().getType());
            assertions.assertThat(linkedCampaignIdToTypeMap.get(campaign2.getCampaignId()))
                    .isEqualTo(campaign2.getCampaign().getType());
        });
    }

    @Test
    public void getAdGroupsLibraryMinusKeywordsPacks_AdGroupHasLibraryPacks() {
        Long adGroupId = createAdGroupWithLibraryMinusKeywordsPacks();
        List<Long> actualMkPackIds = repoUnderTest
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(adGroupId))
                .get(adGroupId);
        MatcherAssert.assertThat(actualMkPackIds, containsInAnyOrder(libraryPacks.toArray()));
    }

    @Test
    public void getAdGroupsLibraryMinusKeywordsPacks_AdGroupHasNoLibraryPacks() {
        List<Long> actualMkPackIds = repoUnderTest
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(defaultAdGroup.getAdGroupId()))
                .get(defaultAdGroup.getAdGroupId());
        MatcherAssert.assertThat(actualMkPackIds, empty());
    }

    @Test
    public void getAdGroupsLibraryMinusKeywordsPacks_RightOrder() {
        Long adGroupId = createAdGroupWithLibraryMinusKeywordsPacks();
        List<Long> actualMkPackIds = repoUnderTest
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(adGroupId))
                .get(adGroupId);
        List<Long> sortedMkPackIds = new ArrayList<>(actualMkPackIds);
        sortedMkPackIds.sort(Comparator.naturalOrder());
        assertThat(actualMkPackIds, contains(sortedMkPackIds.toArray()));
    }

    @Test
    public void addAdGroupToPackLinks_Successful() {
        Map<Long, List<Long>> adGroupMkPacks = singletonMap(defaultAdGroup.getAdGroupId(), libraryPacks);
        repoUnderTest.addAdGroupToPackLinks(jooqConfig, adGroupMkPacks);

        List<Long> actualMkPackIds = repoUnderTest
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(defaultAdGroup.getAdGroupId()))
                .get(defaultAdGroup.getAdGroupId());
        MatcherAssert.assertThat(actualMkPackIds, containsInAnyOrder(libraryPacks.toArray()));
    }

    @Test
    public void getLibraryPacksWithLinksCount_WithoutLibraryPacks_EmptyResult() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        List<Pair<MinusKeywordsPack, Long>> libraryPacksWithLinksCount =
                repoUnderTest.getLibraryPacksWithLinksCount(shard, clientInfo.getClientId(), null, maxLimited());

        assertThat(libraryPacksWithLinksCount, empty());
    }

    @Test
    public void getLibraryPacksWithLinksCount_PackIdFilter_PackFound() {
        MinusKeywordsPack pack =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordsPack();
        linkPackToNewAdGroup(pack.getId());

        List<Pair<MinusKeywordsPack, Long>> packsWithLinkCount =
                repoUnderTest.getLibraryPacksWithLinksCount(shard, clientId, singleton(pack.getId()), maxLimited());

        assertSoftly(assertions -> {
            assertions.assertThat(packsWithLinkCount.size()).isEqualTo(1);
            assertions.assertThat(packsWithLinkCount.get(0).getLeft()).isEqualTo(pack);
            assertions.assertThat(packsWithLinkCount.get(0).getRight()).isEqualTo(1);
        });
    }

    @Test
    public void getLibraryPacksWithLinksCount_LimitOffsetCheck() {
        MinusKeywordsPack pack1 = libraryMinusKeywordsPack().withName("1");
        steps.minusKeywordsPackSteps().createMinusKeywordsPack(pack1, clientInfo);
        linkPackToNewAdGroup(pack1.getId());
        linkPackToNewAdGroup(pack1.getId());

        MinusKeywordsPack pack2 = libraryMinusKeywordsPack().withName("2");
        steps.minusKeywordsPackSteps().createMinusKeywordsPack(pack2, clientInfo);
        LimitOffset limitOffset = new LimitOffset(1, 1);
        HashSet<Long> packIdIn = new HashSet<>(asList(pack1.getId(), pack2.getId()));
        List<Pair<MinusKeywordsPack, Long>> packsWithLinkCount =
                repoUnderTest.getLibraryPacksWithLinksCount(shard, clientId, packIdIn, limitOffset);
        assertSoftly(assertions -> {
            assertions.assertThat(packsWithLinkCount.size()).isEqualTo(1);
            assertions.assertThat(packsWithLinkCount.get(0).getLeft()).isEqualTo(pack2);
            assertions.assertThat(packsWithLinkCount.get(0).getRight()).isEqualTo(0);
        });
    }

    @Test
    public void getLibraryPacksWithLinksCount_AllCasesWithoutFilters() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        Long packWithoutLinks = createNewLibraryPack(clientInfo);

        Long packWithOneLinkedAdGroup = createNewLibraryPack(clientInfo);
        linkPackToNewAdGroup(packWithOneLinkedAdGroup);

        int expectedPackLinksCount = 10;
        Long packWithManyLinkedAdGroups = createNewLibraryPack(clientInfo);
        IntStream.range(0, expectedPackLinksCount)
                .mapToObj(i -> packWithManyLinkedAdGroups)
                .forEach(this::linkPackToNewAdGroup);

        // набор другого клиента
        createNewLibraryPack(new ClientInfo());

        // частный набор
        steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo);

        List<Pair<MinusKeywordsPack, Long>> packsWithLinkCount =
                repoUnderTest.getLibraryPacksWithLinksCount(shard, clientInfo.getClientId(), null, maxLimited());

        Map<Long, Long> packIdToLinksCount = packsWithLinkCount
                .stream()
                .collect(Collectors.toMap(x -> x.getLeft().getId(), Pair::getRight));

        assertSoftly(assertions -> {
            assertions.assertThat(packIdToLinksCount.size()).isEqualTo(3);
            assertions.assertThat(packIdToLinksCount.get(packWithoutLinks)).isEqualTo(0);
            assertions.assertThat(packIdToLinksCount.get(packWithOneLinkedAdGroup)).isEqualTo(1);
            assertions.assertThat(packIdToLinksCount.get(packWithManyLinkedAdGroups)).isEqualTo(expectedPackLinksCount);
        });
    }

    private void linkPackToNewAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, packId, adGroupId);
    }

    private Long createNewLibraryPack(ClientInfo clientInfo) {
        return steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
    }

    private void assertIllegalStateException(Throwable thrown, String errorMessage) {
        Assertions.assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(errorMessage);
    }

    private MinusKeywordsPack copyPack(MinusKeywordsPack pack) {
        return new MinusKeywordsPack()
                .withName(pack.getName())
                .withMinusKeywords(pack.getMinusKeywords())
                .withIsLibrary(pack.getIsLibrary());
    }

    private MinusKeywordsPack generateNewPrivatePack() {
        return privateMinusKeywordsPack().withMinusKeywords(generateMinusKeywords());
    }

    private ImmutableList<String> generateMinusKeywords() {
        return generateMinusKeywordsList(1).get(0);
    }

    private List<ImmutableList<String>> generateMinusKeywordsList(int size) {
        Supplier<ImmutableList<String>> minusKeywordsGenerator = () -> Stream
                .generate(() -> RandomStringUtils.randomAlphanumeric(10))
                .limit(10)
                .collect(toImmutableList());
        return Stream.generate(minusKeywordsGenerator)
                .limit(size)
                .collect(toList());
    }

    private List<AppliedChanges<MinusKeywordsPack>> getMinusKeywordsChanges(MinusKeywordsPack pack, String name,
                                                                            List<String> minusKeywords) {
        return singletonList(new ModelChanges<>(pack.getId(), MinusKeywordsPack.class)
                .process(name, MinusKeywordsPack.NAME)
                .process(minusKeywords, MinusKeywordsPack.MINUS_KEYWORDS)
                .applyTo(pack));
    }

    private void checkClientPacksExist(ClientId clientId, List<Long> packIds) {
        Map<Long, MinusKeywordsPack> minusKeywordsPacks =
                repoUnderTest.getMinusKeywordsPacks(shard, clientId, packIds);
        assertThat(minusKeywordsPacks.keySet(), contains(packIds.toArray()));
    }

    private void checkClientPacksDeleted(ClientId clientId, List<Long> packIds) {
        Map<Long, MinusKeywordsPack> minusKeywordsPacks =
                repoUnderTest.getMinusKeywordsPacks(shard, clientId, packIds);
        assertThat(minusKeywordsPacks.keySet(), hasSize(0));
    }

    private Long createAdGroupWithLibraryMinusKeywordsPacks() {
        return steps.adGroupSteps()
                .createAdGroup(activeTextAdGroup().withLibraryMinusKeywordsIds(libraryPacks), defaultCampaign)
                .getAdGroupId();
    }
}
