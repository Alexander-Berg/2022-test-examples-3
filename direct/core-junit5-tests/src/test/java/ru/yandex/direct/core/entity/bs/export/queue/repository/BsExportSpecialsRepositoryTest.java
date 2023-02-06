package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.dbschema.ppc.tables.BsExportSpecials.BS_EXPORT_SPECIALS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@ExtendWith(SpringExtension.class)
@ParametersAreNonnullByDefault
class BsExportSpecialsRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BsExportSpecialsRepository bsExportSpecialsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private long campaignId1;
    private long campaignId2;
    private int shard;

    @BeforeEach
    void before() {
        CampaignInfo campaignInfo1 = steps.campaignSteps().createActiveTextCampaign();
        campaignId1 = campaignInfo1.getCampaignId();
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActiveTextCampaign(campaignInfo1.getClientInfo());
        campaignId2 = campaignInfo2.getCampaignId();
        shard = campaignInfo1.getShard();
    }

    @AfterEach
    @QueryWithoutIndex("Чистка тестового репа")
    void cleanup() {
        dslContextProvider.ppc(shard).delete(BS_EXPORT_SPECIALS).execute();
    }

    @Test
    void add_OneItem_SavedInQueue() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.DEV1);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(item));

        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assertThat(actual).contains(item);
    }

    @Test
    void addToShard_OneItem_SavedInQueue() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.DEV1);
        bsExportSpecialsRepository.add(shard, singletonList(item));

        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assertThat(actual).contains(item);
    }

    @Test
    void add_OneDuplicatedCampaignItem_UpdatedParType() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.DEV1);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(item));

        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assumeThat(actual, contains(item));

        BsExportSpecials duplicatedCampaignItem = buildItem(campaignId1, QueueType.DEV2);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(duplicatedCampaignItem));

        actual = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assertThat(actual).contains(duplicatedCampaignItem);

    }

    @Test
    void add_TwoItemsWithDifferentParTypes_SavedInQueue() {
        BsExportSpecials item1 = buildItem(campaignId1, QueueType.FAST);
        BsExportSpecials item2 = buildItem(campaignId2, QueueType.HEAVY);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), asList(item1, item2));

        List<BsExportSpecials> actual =
                bsExportSpecialsRepository.getByCampaignIds(shard, asList(campaignId1, campaignId2));

        assertThat(actual).containsExactlyInAnyOrder(item1, item2);
    }

    @Test
    void campaignsByTypeSizInQueue_EmptyQueue_Zero() {
        int size = bsExportSpecialsRepository.campaignsByTypeSizeInQueue(shard, QueueType.PREPROD, CampaignType.TEXT);
        assertThat(size).isEqualTo(0);
    }

    @Test
    void campaignsByTypeSizeInQueue_AddItemToQueue_One() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.CAMPS_ONLY);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(item));

        int size = bsExportSpecialsRepository
                .campaignsByTypeSizeInQueue(shard, QueueType.CAMPS_ONLY, CampaignType.TEXT);
        assertThat(size).isEqualTo(1);
    }

    @Test
    void campaignsByTypeSizeInQueue_AddTextItemToQueueCheckWallet_Zero() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.NOSEND);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(item));

        int size = bsExportSpecialsRepository.campaignsByTypeSizeInQueue(shard, QueueType.NOSEND, CampaignType.WALLET);
        assertThat(size).isEqualTo(0);
    }

    @Test
    void getQueueSize_DifferentQueues_GotSpecificQueueSize() {
        long campaignId3 = steps.campaignSteps().createActiveTextCampaign().getCampaignId();
        long campaignId4 = steps.campaignSteps().createActiveTextCampaign().getCampaignId();
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), Arrays.asList(
                buildItem(campaignId1, QueueType.NOSEND),
                buildItem(campaignId2, QueueType.DEV2),
                buildItem(campaignId3, QueueType.CAMPS_ONLY),
                buildItem(campaignId4, QueueType.CAMPS_ONLY)
        ));

        int size = bsExportSpecialsRepository.getQueueSize(shard, QueueType.CAMPS_ONLY);
        assertThat(size).isEqualTo(2);
    }

    @Test
    void remove_OneItem_RemovedFromQueue() {
        BsExportSpecials item = buildItem(campaignId1, QueueType.DEV1);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), singletonList(item));

        List<BsExportSpecials> current = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assumeThat(current, contains(item));

        bsExportSpecialsRepository.remove(shard, singletonList(campaignId1));
        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, singletonList(campaignId1));
        assertThat(actual).isEmpty();
    }

    @Test
    void remove_TwoItems_RemovedFromQueue() {
        BsExportSpecials item1 = buildItem(campaignId1, QueueType.FAST);
        BsExportSpecials item2 = buildItem(campaignId2, QueueType.HEAVY);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), asList(item1, item2));

        List<BsExportSpecials> current = bsExportSpecialsRepository.getByCampaignIds(shard, asList(campaignId1, campaignId2));
        assumeThat(current, containsInAnyOrder(item1, item2));

        bsExportSpecialsRepository.remove(shard, asList(campaignId1, campaignId2));
        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, asList(campaignId1, campaignId2));
        assertThat(actual).isEmpty();
    }

    @Test
    void remove_OneItem_RemovedFromQueueAndOtherKept() {
        BsExportSpecials item1 = buildItem(campaignId1, QueueType.FAST);
        BsExportSpecials item2 = buildItem(campaignId2, QueueType.HEAVY);
        bsExportSpecialsRepository.add(dslContextProvider.ppc(shard), asList(item1, item2));

        List<BsExportSpecials> current = bsExportSpecialsRepository.getByCampaignIds(shard, asList(campaignId1, campaignId2));
        assumeThat(current, containsInAnyOrder(item1, item2));

        bsExportSpecialsRepository.remove(shard, singletonList(campaignId2));
        List<BsExportSpecials> actual = bsExportSpecialsRepository.getByCampaignIds(shard, asList(campaignId1, campaignId2));
        assertThat(actual).contains(item1);
    }

    private BsExportSpecials buildItem(long campaignId, QueueType type) {
        return new BsExportSpecials()
                .withCampaignId(campaignId)
                .withType(type);
    }

}
