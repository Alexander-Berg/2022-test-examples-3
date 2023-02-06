package ru.yandex.direct.core.entity.vcard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.VcardSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VcardRepositoryDeleteTest {

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private VcardSteps vcardSteps;

    private CampaignInfo campaignInfo;

    private int shard;
    private long clientUid;
    private ClientId clientId;
    private Long vcardId1;
    private Long vcardId2;

    @Before
    public void before() {
        campaignInfo = campaignSteps.createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientUid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();

        vcardId1 = null;
        vcardId2 = null;
    }

    @After
    public void cleanup() {
        delete(StreamEx.of(vcardId1, vcardId2).nonNull().toList());
    }

    // удаление из таблицы vcards

    @Test
    public void deleteVcards_OneUnusedVcard_DeletesVcard() {
        vcardId1 = createUnusedVcard1().getId();
        Set<Long> deletedIds = delete(singletonList(vcardId1));
        assertThat("метод должен вернуть id визитки", deletedIds, contains(vcardId1));
        assertThat("после удаления визитка должна отсутствовать в базе",
                getExistingVcards(singletonList(vcardId1)), hasSize(0));
    }

    @Test
    public void deleteVcards_TwoUnusedVcards_DeletesVcards() {
        vcardId1 = createUnusedVcard1().getId();
        vcardId2 = createUnusedVcard2().getId();
        Set<Long> deletedIds = delete(asList(vcardId1, vcardId2));
        assertThat("метод должен вернуть id обеих визиток",
                deletedIds, containsInAnyOrder(vcardId1, vcardId2));
        assertThat("после удаления визитки должны отсутствовать в базе",
                getExistingVcards(asList(vcardId1, vcardId2)), hasSize(0));
    }

    @Test
    public void deleteVcards_OneUsedVcard_DoesNotDeleteVcard() {
        vcardId1 = createUsedVcard1().getId();
        Set<Long> deletedIds = delete(singletonList(vcardId1));
        assertThat("метод должен вернуть пустой сет", deletedIds, hasSize(0));
        assertThat("после попытки удаления визитка должна присутствовать в базе",
                getExistingVcards(singletonList(vcardId1)), contains(vcardId1));
    }

    @Test
    public void deleteVcards_TwoUsedVcards_DeletesVcards() {
        vcardId1 = createUsedVcard1().getId();
        vcardId2 = createUsedVcard2().getId();
        Set<Long> deletedIds = delete(asList(vcardId1, vcardId2));
        assertThat("метод должен вернуть пустой сет", deletedIds, hasSize(0));
        assertThat("после попытки удаления визитки должны присутствовать в базе",
                getExistingVcards(asList(vcardId1, vcardId2)), containsInAnyOrder(vcardId1, vcardId2));
    }

    @Test
    public void deleteVcards_OneUsedAndOneUnusedVcards_DeletesOnlyUnusedVcard() {
        vcardId1 = createUnusedVcard1().getId();
        vcardId2 = createUsedVcard2().getId();
        Set<Long> deletedIds = delete(asList(vcardId1, vcardId2));
        assertThat("метод должен вернуть id удаленной неиспользуемой визитки", deletedIds, contains(vcardId1));
        assertThat("после попытки удаления в базе должна присутствовать только используемая визитка",
                getExistingVcards(asList(vcardId1, vcardId2)), contains(vcardId2));
    }

    @Test
    public void deleteVcards_DeletesOnlySelectedVcards() {
        vcardId1 = createUnusedVcard1().getId();
        vcardId2 = createUnusedVcard2().getId();

        Set<Long> deletedIds = delete(singletonList(vcardId1));
        checkState(!deletedIds.isEmpty(), "метод должен вернуть id");
        checkState(deletedIds.contains(vcardId1), "метод должен вернуть id удалённой визитки");
        checkState(deletedIds.size() == 1, "метод должен вернуть единственный id");

        assertThat("после удаления одной визитки, другая должна остаться в базе",
                getExistingVcards(asList(vcardId1, vcardId2)), contains(vcardId2));
    }

    @Test
    public void deleteVcards_DeletingVcardsOfAnotherUser_DeletesOnlyVcardsOfSelectedUser() {
        Vcard vcardOfUser1 = createUnusedVcard1();
        long vcardOfUserId1 = vcardOfUser1.getId();

        // здесь создается новый пользователь и визитка
        VcardInfo vcardInfoOfUser2 = vcardSteps.createFullVcard();
        long vcardOfUserId2 = vcardInfoOfUser2.getVcardId();

        checkState(shard == vcardInfoOfUser2.getShard(),
                "оба пользователя должны быть в одном шарде");

        Set<Long> deletedIds = delete(asList(vcardOfUserId1, vcardOfUserId2));
        checkState(deletedIds.contains(vcardOfUserId1) && deletedIds.size() == 1,
                "метод должен вернуть только id визитки указанного пользователя");

        List<Long> remainingVcardIdsOfUser2 = vcardRepository.getVcards(shard, vcardInfoOfUser2.getUid())
                .stream()
                .map(Vcard::getId)
                .collect(toList());
        assertThat("у второго пользователя должна остаться визитка",
                remainingVcardIdsOfUser2, contains(vcardOfUserId2));
    }

    private Set<Long> getExistingVcards(Collection<Long> vcardIds) {
        List<Vcard> vcards = vcardRepository.getVcards(shard, clientUid, vcardIds);
        return StreamEx.of(vcards)
                .map(Vcard::getId)
                .toSet();
    }

    private Set<Long> delete(Collection<Long> vcardIds) {
        return vcardRepository.deleteUnusedVcards(shard, clientUid, vcardIds);
    }

    private Vcard createUnusedVcard1() {
        return createVcard("10").getVcard();
    }

    private Vcard createUnusedVcard2() {
        return createVcard("11").getVcard();
    }

    private Vcard createUsedVcard1() {
        VcardInfo vcardInfo = createVcard("12");
        OldTextBanner banner = activeTextBanner(null, null).withVcardId(vcardInfo.getVcardId());
        bannerSteps.createBanner(banner, campaignInfo);
        return vcardInfo.getVcard();
    }

    private Vcard createUsedVcard2() {
        VcardInfo vcardInfo = createVcard("13");
        OldTextBanner banner = activeTextBanner(null, null).withVcardId(vcardInfo.getVcardId());
        bannerSteps.createBanner(banner, campaignInfo);
        return vcardInfo.getVcard();
    }

    /**
     * @param house значение для создания различных визиток.
     */
    private VcardInfo createVcard(String house) {
        VcardInfo vcardInfo = vcardSteps.createVcard(fullVcard().withHouse(house), campaignInfo);
        checkState(vcardInfo.getVcardId() != null);
        return vcardInfo;
    }
}
