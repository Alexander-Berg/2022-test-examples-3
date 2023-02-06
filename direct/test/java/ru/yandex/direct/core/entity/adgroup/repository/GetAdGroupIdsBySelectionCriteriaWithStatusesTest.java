package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupStatus;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupIdsBySelectionCriteriaWithStatusesTest {

    private int shard;

    private Set<Long> campaignIds = new HashSet<>();

    private Long draftAdGroupId;
    private Long moderationAdGroupId1;
    private Long moderationAdGroupId2;
    private Long moderationAdGroupId3;
    private Long moderationAdGroupId4;
    private Long moderationAdGroupId5;
    private Long moderationAdGroupId6;
    private Long moderationAdGroupId7;
    private Long moderationAdGroupId8;
    private Long moderationAdGroupId9;
    private Long moderationAdGroupId10;
    private Long moderationAdGroupId11;
    private Long moderationAdGroupId12;
    private Long moderationAdGroupId13;
    private Long moderationAdGroupId14;
    private Long moderationAdGroupId15;
    private Long preacceptedAdGroupId1;
    private Long preacceptedAdGroupId2;
    private Long preacceptedAdGroupId3;
    private Long acceptedAdGroupId;
    private Long rejectedAdGroupId;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Before
    public void setUp() {

        // NB: все тестовые группы - текстовые, это допустимо, т.к. тип группы не участвует в вычислении статуса. Хотя
        //      для спокойствия после окончания разработки репозитория можно дописать кейсы и для остальных типов групп

        // NB: создание всех групп на этапе подготовки тестовых данных в какой-то мере помогает, имхо, убедиться, что
        // вызов тестируемого метода не возвращает лишних групп.

        // NB: теструемый метод возвращает группы, упорядоченные по возрастанию id - в тестовых ожиданиях id групп
        // упорядоченны так же, т.к. при создании тестовых групп id выдаются им в порядке возрастания

        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign();
        Long campaignId = campaign.getCampaignId();

        campaignIds.add(campaignId);

        shard = campaign.getShard();

        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        // draft
        draftAdGroupId = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.NEW), campaign)
                .getAdGroupId();

        // moderation
        moderationAdGroupId1 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.NEW), campaign)
                .getAdGroupId();
        moderationAdGroupId2 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.NO), campaign)
                .getAdGroupId();
        moderationAdGroupId3 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.READY), campaign)
                .getAdGroupId();
        moderationAdGroupId4 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.SENT), campaign)
                .getAdGroupId();
        moderationAdGroupId5 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.REJECTED), campaign)
                .getAdGroupId();
        moderationAdGroupId6 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.NEW), campaign)
                .getAdGroupId();
        moderationAdGroupId7 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.NO), campaign)
                .getAdGroupId();
        moderationAdGroupId8 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.READY), campaign)
                .getAdGroupId();
        moderationAdGroupId9 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.SENT), campaign)
                .getAdGroupId();
        moderationAdGroupId10 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.REJECTED), campaign)
                .getAdGroupId();
        moderationAdGroupId11 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.NEW), campaign)
                .getAdGroupId();
        moderationAdGroupId12 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.NO), campaign)
                .getAdGroupId();
        moderationAdGroupId13 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.READY), campaign)
                .getAdGroupId();
        moderationAdGroupId14 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.SENT), campaign)
                .getAdGroupId();
        moderationAdGroupId15 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.REJECTED), campaign)
                .getAdGroupId();

        // preaccepted
        preacceptedAdGroupId1 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENT)
                        .withStatusPostModerate(StatusPostModerate.YES), campaign)
                .getAdGroupId();
        preacceptedAdGroupId2 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.SENDING)
                        .withStatusPostModerate(StatusPostModerate.YES), campaign)
                .getAdGroupId();
        preacceptedAdGroupId3 = adGroupSteps
                .createAdGroup(activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.YES), campaign)
                .getAdGroupId();

        // accepted
        acceptedAdGroupId = adGroupSteps.createAdGroup(
                activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.YES)
                        .withStatusPostModerate(StatusPostModerate.YES), campaign).getAdGroupId();

        // rejected
        rejectedAdGroupId = adGroupSteps.createAdGroup(
                activeTextAdGroup(campaignId).withStatusModerate(StatusModerate.NO), campaign).getAdGroupId();
    }

    @Test
    public void getDraftAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupStatuses(AdGroupStatus.DRAFT),
                maxLimited());

        assertThat("вернулись id ожидаемых групп - черновиков", adGroupIds, contains(draftAdGroupId));
    }

    @Test
    public void getModerationAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.MODERATION), maxLimited());

        assertThat("вернулись id ожидаемых групп на модерации", adGroupIds,
                contains(moderationAdGroupId1, moderationAdGroupId2, moderationAdGroupId3, moderationAdGroupId4,
                        moderationAdGroupId5, moderationAdGroupId6, moderationAdGroupId7, moderationAdGroupId8,
                        moderationAdGroupId9, moderationAdGroupId10, moderationAdGroupId11, moderationAdGroupId12,
                        moderationAdGroupId13, moderationAdGroupId14, moderationAdGroupId15));
    }

    @Test
    public void getPreacceptedAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.PREACCEPTED), maxLimited());

        assertThat("вернулись id ожидаемых групп, предварительно одобренных на модерации", adGroupIds,
                contains(preacceptedAdGroupId1, preacceptedAdGroupId2, preacceptedAdGroupId3));
    }

    @Test
    public void getAcceptedAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.ACCEPTED), maxLimited());

        assertThat("вернулись id ожидаемых групп, принятых на модерации", adGroupIds, contains(acceptedAdGroupId));
    }

    @Test
    public void getRejectedAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.REJECTED), maxLimited());

        assertThat("вернулись id ожидаемых групп, отклонённых на модерации", adGroupIds, contains(rejectedAdGroupId));
    }

    @Test
    public void getAdGroupsWithAllStatus() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.DRAFT, AdGroupStatus.MODERATION, AdGroupStatus.PREACCEPTED,
                                AdGroupStatus.ACCEPTED, AdGroupStatus.REJECTED), maxLimited());

        assertThat("вернулись id ожидаемых групп, отклонённых на модерации", adGroupIds,
                contains(draftAdGroupId, moderationAdGroupId1, moderationAdGroupId2, moderationAdGroupId3,
                        moderationAdGroupId4, moderationAdGroupId5, moderationAdGroupId6, moderationAdGroupId7,
                        moderationAdGroupId8, moderationAdGroupId9, moderationAdGroupId10, moderationAdGroupId11,
                        moderationAdGroupId12, moderationAdGroupId13, moderationAdGroupId14, moderationAdGroupId15,
                        preacceptedAdGroupId1, preacceptedAdGroupId2, preacceptedAdGroupId3, acceptedAdGroupId,
                        rejectedAdGroupId));
    }

    @Test
    public void getAdGroupsWithNullAsStatus() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignIds)
                        .withAdGroupStatuses(AdGroupStatus.DRAFT, AdGroupStatus.MODERATION, AdGroupStatus.PREACCEPTED,
                                AdGroupStatus.ACCEPTED, AdGroupStatus.REJECTED), maxLimited());

        assertThat("вернулись id ожидаемых групп, отклонённых на модерации", adGroupIds,
                contains(draftAdGroupId, moderationAdGroupId1, moderationAdGroupId2, moderationAdGroupId3,
                        moderationAdGroupId4, moderationAdGroupId5, moderationAdGroupId6, moderationAdGroupId7,
                        moderationAdGroupId8, moderationAdGroupId9, moderationAdGroupId10, moderationAdGroupId11,
                        moderationAdGroupId12, moderationAdGroupId13, moderationAdGroupId14, moderationAdGroupId15,
                        preacceptedAdGroupId1, preacceptedAdGroupId2, preacceptedAdGroupId3, acceptedAdGroupId,
                        rejectedAdGroupId));
    }
}
